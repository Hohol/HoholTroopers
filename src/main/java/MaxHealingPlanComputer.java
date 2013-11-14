import static model.TrooperType.*;

import java.util.ArrayList;

class MaxHealingPlanComputer extends PlanComputer<HealingState> {

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
        selfType = FIELD_MEDIC;

        prepare();
        cur = new HealingState(new ArrayList<MyMove>(), actionPoints, holdingFieldRation, holdingMedikit, start.x, start.y);
        rec();
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

    @Override
    public void rec() {
        cur.distSum = getDistToTeammatesSum();

        cur.minHp = getMinHp();
        if (cur.better(best)) {
            best = new HealingState(cur);
        }

        tryEatFieldRation();

        tryHealTeammates();
        tryHealSelfWithMedikit();
        tryHealSelfWithAbility();

        tryMove();
    }

    private void tryHealSelfWithMedikit() {
        if (!cur.holdingMedikit) {
            return;
        }
        int healValue = game.getMedikitHealSelfBonusHitpoints();
        cur.holdingMedikit = false;
        newTryHeal(healValue, game.getMedikitUseCost(), MyMove.USE_MEDIKIT_SELF, selfType.ordinal(), true);
        cur.holdingMedikit = true;
    }

    private void tryHealSelfWithAbility() {
        newTryHeal(game.getFieldMedicHealSelfBonusHitpoints(), game.getFieldMedicHealCost(), MyMove.HEAL_SELF, selfType.ordinal(), false);
    }

    private void newTryHeal(int healValue, int healCost, MyMove healAction, int typeOrdinal, boolean avoidOverheal) {
        if (cur.actionPoints < healCost) {
            return;
        }
        int oldHp = hp[typeOrdinal];
        if (oldHp >= Utils.INITIAL_TROOPER_HP) {
            return;
        }
        int newHp = Math.min(Utils.INITIAL_TROOPER_HP, oldHp + healValue);
        int diffHp = newHp - oldHp;
        if (avoidOverheal && diffHp < healValue) {
            return;
        }

        addAction(healAction);
        hp[typeOrdinal] = newHp;
        cur.actionPoints -= healCost;
        cur.healedSum += diffHp;

        rec();

        cur.healedSum -= diffHp;
        cur.actionPoints += healCost;
        hp[typeOrdinal] = oldHp;
        popAction();
    }

    private int getDistToTeammatesSum() {
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
            int d = dist[i][cur.x][cur.y];
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

    @Override
    protected boolean freeCell(int toX, int toY) {
        return map[toX][toY] == '.';
    }

    private void tryHealTeammates() {
        tryHealTeammates(MyMove.directedHeals, game.getFieldMedicHealBonusHitpoints(), game.getFieldMedicHealCost(), false);
        if (cur.holdingMedikit) {
            cur.holdingMedikit = false;
            tryHealTeammates(MyMove.directedMedikitUses, game.getMedikitBonusHitpoints(), game.getMedikitUseCost(), true);
            cur.holdingMedikit = true;
        }
    }

    private void tryHealTeammates(MyMove[] heals, int healValue, int healCost, boolean avoidOverheal) {
        for (MyMove heal : heals) {
            int toX = cur.x + heal.getDx();
            int toY = cur.y + heal.getDy();
            if (!inField(toX, toY)) {
                continue;
            }
            char targetChar = map[toX][toY];
            if (!Utils.isLetter(targetChar)) {
                continue;
            }
            int index = getIndex(targetChar);
            newTryHeal(healValue, healCost, heal, index, avoidOverheal);
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
