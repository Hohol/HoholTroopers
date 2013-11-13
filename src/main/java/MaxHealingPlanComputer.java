import model.ActionType;

import static model.TrooperType.*;

import java.util.ArrayList;
import java.util.List;

class MaxHealingPlanComputer extends PlanComputer<HealingState> {
    int moveCost;

    final int[] hp;
    Cell start;

    int[][][] dist;
    Cell[] positions = new Cell[Utils.NUMBER_OF_TROOPER_TYPES];

    MaxHealingPlanComputer(
            int actionPoints,
            char[][] map,
            int[] hp, //TrooperType.ordinal() to hp
            boolean holdingFieldRation,
            boolean holdingMedikit,
            Utils utils
    ) {
        super(map, utils);
        this.hp = hp;
        moveCost = game.getStandingMoveCost(); //assume that medic is standing
        selfType = FIELD_MEDIC;

        prepare();
        cur = new HealingState(new ArrayList<MyMove>(), actionPoints, holdingFieldRation, holdingMedikit, start.x, start.y);
        work(actionPoints, start.x, start.y, 0, holdingFieldRation, holdingMedikit);
    }

    private void work(int actionPoints, int x, int y, int healedSum, boolean holdingFieldRation, boolean holdingMedikit) {
        if (holdingFieldRation && actionPoints >= game.getFieldRationEatCost() && actionPoints < utils.getInitialActionPoints(FIELD_MEDIC)) {
            addAction(MyMove.EAT_FIELD_RATION);
            work(
                    utils.actionPointsAfterEatingFieldRation(FIELD_MEDIC, actionPoints, game),
                    x,
                    y,
                    0,
                    false,
                    holdingMedikit
            );
            popAction();
        }

        int missingHp = Utils.INITIAL_TROOPER_HP - hp[FIELD_MEDIC.ordinal()];

        if (holdingMedikit && actionPoints >= game.getMedikitUseCost() && missingHp >= game.getMedikitHealSelfBonusHitpoints()) {
            int oldHp = hp[FIELD_MEDIC.ordinal()];
            addAction(MyMove.USE_MEDIKIT_SELF);
            work(
                    actionPoints - game.getMedikitUseCost(),
                    x,
                    y,
                    game.getMedikitHealSelfBonusHitpoints(),
                    holdingFieldRation,
                    false
            );
            popAction();
            hp[FIELD_MEDIC.ordinal()] = oldHp;
        }


        int actionsToHeal = Utils.divCeil(missingHp, game.getFieldMedicHealSelfBonusHitpoints());
        int T = Math.min(actionPoints, actionsToHeal);
        int oldHp = hp[FIELD_MEDIC.ordinal()];
        for (int i = 0; i <= T; i++) {
            if (i != 0) {
                hp[FIELD_MEDIC.ordinal()] += game.getFieldMedicHealSelfBonusHitpoints();
                addAction(MyMove.HEAL_SELF);
            }
            rec(
                    actionPoints - i * game.getFieldMedicHealCost(),
                    x,
                    y,
                    healedSum + hp[FIELD_MEDIC.ordinal()] - oldHp,
                    holdingFieldRation,
                    holdingMedikit
            );
        }
        for (int i = 0; i < T; i++) {
            popAction();
        }
    }

    private void prepare() {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (Utils.isLetter(map[i][j])) {
                    if (map[i][j] == Utils.getCharForTrooperType(FIELD_MEDIC)) {
                        start = new Cell(i, j);
                        map[i][j] = '.'; //erase medic
                    } else {
                        positions[Utils.getTrooperTypeByChar(map[i][j]).ordinal()] = new Cell(i, j);
                    }
                }
            }
        }
        dist = new int[Utils.NUMBER_OF_TROOPER_TYPES][][];
        for (int trooperIndex = 0; trooperIndex < Utils.NUMBER_OF_TROOPER_TYPES; trooperIndex++) {
            if (noSuchType(trooperIndex)) {
                if (trooperIndex != FIELD_MEDIC.ordinal()) {
                    hp[trooperIndex] = Integer.MAX_VALUE;
                }
                continue;
            }
            dist[trooperIndex] = Utils.bfsByMap(map, positions[trooperIndex].x, positions[trooperIndex].y);
            for (int i = 0; i < map.length; i++) {
                for (int j = 0; j < map[i].length; j++) {
                    if (dist[trooperIndex][i][j] > MyStrategy.MAX_DISTANCE_MEDIC_SHOULD_HEAL) {
                        dist[trooperIndex][i][j] = Utils.UNREACHABLE;
                    }
                }
            }
        }
    }

    public List<MyMove> getActions() {
        return best.actions;
    }

    private void rec(int actionPoints, int x, int y, int healedSum, boolean holdingFieldRation, boolean holdingMedikit) {
        updateBest(actionPoints, healedSum, holdingFieldRation, holdingMedikit, getDistSum(x, y));

        tryEatFieldRation(actionPoints, x, y, healedSum, holdingFieldRation, holdingMedikit);

        tryHealTeammates(actionPoints, x, y, healedSum, holdingFieldRation, holdingMedikit);

        tryMove(actionPoints, x, y, healedSum, holdingFieldRation, holdingMedikit);
    }

    private int getDistSum(int x, int y) {
        int r = 0;
        int minHp = Integer.MAX_VALUE;
        for (int i = 0; i < Utils.NUMBER_OF_TROOPER_TYPES; i++) {
            if (noSuchType(i)) {
                continue;
            }
            minHp = Math.min(minHp, hp[i]);
        }
        for (int i = 0; i < Utils.NUMBER_OF_TROOPER_TYPES; i++) {
            if (noSuchType(i)) {
                continue;
            }
            int d = dist[i][x][y];
            if (hp[i] == minHp) {
                d *= 100;
            }
            r += d;
        }
        return r;
    }

    private boolean noSuchType(int i) {
        return positions[i] == null;
    }

    private void tryMove(int actionPoints, int x, int y, int healedSum, boolean holdingFieldRation, boolean holdingMedikit) {
        cur.actionPoints = actionPoints;
        cur.x = x;
        cur.y = y;
        cur.healedSum = healedSum;
        cur.holdingFieldRation = holdingFieldRation;
        cur.holdingMedikit = holdingMedikit;
        tryMove();
    }

    @Override
    protected void rec() {
        rec(cur.actionPoints, cur.x, cur.y, cur.healedSum, cur.holdingFieldRation, cur.holdingMedikit);
    }

    @Override
    protected boolean freeCell(int toX, int toY) {
        return map[toX][toY] == '.';
    }

    private void tryEatFieldRation(int actionPoints, int x, int y, int healedSum, boolean holdingFieldRation, boolean holdingMedikit) {
        cur.actionPoints = actionPoints;
        cur.x = x;
        cur.y = y;
        cur.healedSum = healedSum;
        cur.holdingFieldRation = holdingFieldRation;
        cur.holdingMedikit = holdingMedikit;
        tryEatFieldRation();
    }

    private void tryHealTeammates(int actionPoints, int x, int y, int healedSum, boolean holdingFieldRation, boolean holdingMedikit) {
        if (actionPoints >= game.getFieldMedicHealCost()) {
            tryHealTeammates(actionPoints - game.getFieldMedicHealCost(), x, y, healedSum, holdingFieldRation, holdingMedikit, MyMove.directedHeals, game.getFieldMedicHealBonusHitpoints());
        }
        if (holdingMedikit && actionPoints >= game.getMedikitUseCost()) {
            tryHealTeammates(actionPoints - game.getMedikitUseCost(), x, y, healedSum, holdingFieldRation, false, MyMove.directedMedikitUses, game.getMedikitBonusHitpoints());
        }
    }

    private void tryHealTeammates(int actionPoints, int x, int y, int healedSum, boolean holdingFieldRation, boolean holdingMedikit, MyMove[] heals, int healValue) {
        for (MyMove heal : heals) {
            int toX = x + heal.getDx();
            int toY = y + heal.getDy();
            if (!inField(toX, toY)) {
                continue;
            }
            char targetChar = map[toX][toY];
            if (!Utils.isLetter(targetChar)) {
                continue;
            }
            int index = getIndex(targetChar);
            tryHeal(actionPoints, x, y, healedSum, heal, index, healValue, holdingFieldRation, holdingMedikit);
        }
    }

    private void tryHeal(int actionPoints, int x, int y, int healedSum, MyMove healAction, int targetOrdinal, int healValue, boolean holdingFieldRation, boolean holdingMedikit) {
        int oldHp = hp[targetOrdinal];
        if (oldHp >= Utils.INITIAL_TROOPER_HP) {
            return;
        }
        int newHp = Math.min(oldHp + healValue, Utils.INITIAL_TROOPER_HP);
        int hpDiff = newHp - oldHp;
        boolean avoidOverheal = (healAction.getMove().getAction() == ActionType.USE_MEDIKIT);
        if (avoidOverheal && hpDiff < healValue) {
            return;
        }
        hp[targetOrdinal] = newHp;

        addAction(healAction);
        rec(
                actionPoints,
                x,
                y,
                healedSum + hpDiff,
                holdingFieldRation,
                holdingMedikit
        );

        popAction();

        hp[targetOrdinal] = oldHp;
    }

    private void updateBest(int actionPoints, int healedSum, boolean holdingFieldRation, boolean holdingMedikit, int dSum) {
        cur.actionPoints = actionPoints;
        cur.healedSum = healedSum;
        cur.holdingMedikit = holdingMedikit;
        cur.holdingFieldRation = holdingFieldRation;
        cur.distSum = dSum;
        cur.minHp = getMinHp();
        if (cur.better(best)) {
            best = new HealingState(cur);
        }
    }

    private int getMinHp() {
        int mi = Integer.MAX_VALUE;
        for (int v : hp) {
            mi = Math.min(mi, v);
        }
        return mi;
    }

    private int getIndex(char targetChar) {
        switch (targetChar) {
            case 'F':
                return FIELD_MEDIC.ordinal();
            case 'S':
                return SOLDIER.ordinal();
            case 'C':
                return COMMANDER.ordinal();
            case 'R':
                return SNIPER.ordinal();
            case 'T':
                return SCOUT.ordinal();
        }
        throw new RuntimeException();
    }
}
