import model.ActionType;
import model.Game;

import static model.TrooperType.*;

import java.util.ArrayList;
import java.util.List;

class MaxHealingPlanComputer {
    static int moveCost;

    final char[][] map;
    final int[] hp;
    final List<MyAction> actions = new ArrayList<>();
    final Utils utils;
    final Game game;
    Cell start;

    List<MyAction> bestActions;
    int bestActionPoints, bestHealedSum;
    boolean bestHoldingFieldRation;
    boolean bestHoldingMedikit;
    int bestMinHp, bestDistSum;
    int[][][] dist;
    Cell[] positions = new Cell[Utils.NUMBER_OF_TROOPER_TYPES];

    int stepCnt;

    MaxHealingPlanComputer(
            int actionPoints,
            char[][] map,
            int[] hp, //TrooperType.ordinal() to hp
            boolean holdingFieldRation,
            boolean holdingMedikit,
            Utils utils
    ) {
        this.map = map;
        this.hp = hp;
        this.utils = utils;
        this.game = utils.getGame();
        moveCost = game.getStandingMoveCost(); //assume that medic is standing

        prepare();
        work(actionPoints, start.x, start.y, 0, holdingFieldRation, holdingMedikit);
        System.out.println(stepCnt);
    }

    private void work(int actionPoints, int x, int y, int healedSum, boolean holdingFieldRation, boolean holdingMedikit) {
        if (holdingFieldRation && actionPoints >= game.getFieldRationEatCost() && actionPoints < utils.getInitialActionPoints(FIELD_MEDIC)) {
            addAction(MyAction.EAT_FIELD_RATION);
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
            addAction(MyAction.USE_MEDIKIT_SELF);
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
                addAction(MyAction.HEAL_SELF);
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
                    if (dist[trooperIndex][i][j] >= 7) {
                        dist[trooperIndex][i][j] = Utils.UNREACHABLE;
                    }
                }
            }
        }
    }

    public List<MyAction> getActions() {
        return bestActions;
    }

    private void rec(int actionPoints, int x, int y, int healedSum, boolean holdingFieldRation, boolean holdingMedikit) {
        stepCnt++;
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
        if (actionPoints >= moveCost) {
            for (MyAction movement : MyAction.movements) {
                int toX = x + movement.getDx();
                int toY = y + movement.getDy();
                if (!inField(toX, toY)) {
                    continue;
                }
                if (map[toX][toY] != '.') {
                    continue;
                }
                addAction(movement);
                rec(
                        actionPoints - moveCost,
                        toX,
                        toY,
                        healedSum,
                        holdingFieldRation,
                        holdingMedikit
                );
                popAction();
            }
        }
    }

    private void tryEatFieldRation(int actionPoints, int x, int y, int healedSum, boolean holdingFieldRation, boolean holdingMedikit) {
        if (holdingFieldRation && actionPoints >= game.getFieldRationEatCost() && actionPoints < utils.getInitialActionPoints(FIELD_MEDIC)) {
            addAction(MyAction.EAT_FIELD_RATION);
            rec(
                    utils.actionPointsAfterEatingFieldRation(FIELD_MEDIC, actionPoints, game),
                    x,
                    y,
                    healedSum,
                    false,
                    holdingMedikit
            );
            popAction();
        }
    }

    private void tryHealTeammates(int actionPoints, int x, int y, int healedSum, boolean holdingFieldRation, boolean holdingMedikit) {
        if (actionPoints >= game.getFieldMedicHealCost()) {
            tryHealTeammates(actionPoints - game.getFieldMedicHealCost(), x, y, healedSum, holdingFieldRation, holdingMedikit, MyAction.directedHeals, game.getFieldMedicHealBonusHitpoints());
        }
        if (holdingMedikit && actionPoints >= game.getMedikitUseCost()) {
            tryHealTeammates(actionPoints - game.getMedikitUseCost(), x, y, healedSum, holdingFieldRation, false, MyAction.directedMedikitUses, game.getMedikitBonusHitpoints());
        }
    }

    private void tryHealTeammates(int actionPoints, int x, int y, int healedSum, boolean holdingFieldRation, boolean holdingMedikit, MyAction[] heals, int healValue) {
        for (MyAction heal : heals) {
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

    private void tryHeal(int actionPoints, int x, int y, int healedSum, MyAction healAction, int targetOrdinal, int healValue, boolean holdingFieldRation, boolean holdingMedikit) {
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

    private boolean inField(int toX, int toY) {
        return toX >= 0 && toX < map.length && toY >= 0 && toY < map[0].length;
    }

    private void updateBest(int actionPoints, int healedSum, boolean holdingFieldRation, boolean holdingMedikit, int dSum) {
        int minHp = getMinHp();
        if (better(actionPoints, healedSum, holdingFieldRation, minHp, holdingMedikit, dSum)) {
            bestActionPoints = actionPoints;
            bestHealedSum = healedSum;
            bestHoldingFieldRation = holdingFieldRation;
            bestMinHp = minHp;
            bestHoldingMedikit = holdingMedikit;
            bestDistSum = dSum;

            bestActions = new ArrayList<>(actions);
        }
    }

    private int getMinHp() {
        int mi = Integer.MAX_VALUE;
        for (int v : hp) {
            mi = Math.min(mi, v);
        }
        return mi;
    }

    private boolean better(int actionPoints, int healedSum, boolean holdingFieldRation, int minHp, boolean holdingMedikit, int dSum) {
        if (bestActions == null) {
            return true;
        }
        if (healedSum != bestHealedSum) {
            return healedSum > bestHealedSum;
        }

        if (minHp != bestMinHp) {
            return minHp > bestMinHp;
        }
        if (holdingMedikit != bestHoldingMedikit) {
            return holdingMedikit;
        }
        if (holdingFieldRation != bestHoldingFieldRation) {
            return holdingFieldRation;
        }
        if (dSum != bestDistSum) {
            return dSum < bestDistSum;
        }
        if (actionPoints != bestActionPoints) {
            return actionPoints > bestActionPoints;
        }
        return false;
    }

    private void addAction(MyAction action) {
        actions.add(action);
    }

    private void popAction() {
        actions.remove(actions.size() - 1);
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
