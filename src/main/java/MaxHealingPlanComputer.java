import model.ActionType;
import model.Game;
import model.TrooperType;

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

    List<MyAction> bestActions = new ArrayList<>();
    int bestActionPoints, bestHealedSum;
    boolean bestHoldingFieldRation;
    boolean bestHoldingMedikit;
    int bestMinHp;

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

        bestActionPoints = actionPoints;
        bestHoldingFieldRation = holdingFieldRation;

        Cell start = findStartAndClearHpFromTrash();
        rec(actionPoints, start.x, start.y, 0, holdingFieldRation, holdingMedikit);
    }

    private Cell findStartAndClearHpFromTrash() {
        Cell r = null;
        boolean used[] = new boolean[TrooperType.values().length];
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (map[i][j] == Utils.MEDIC_CHAR) {
                    r = new Cell(i, j);
                }
                if (Utils.isLetter(map[i][j])) {
                    used[Utils.getTrooperTypeByChar(map[i][j]).ordinal()] = true;
                }
            }
        }
        for (int i = 0; i < hp.length; i++) {
            if (!used[i]) {
                hp[i] = Integer.MAX_VALUE;
            }
        }
        return r;
    }

    public List<MyAction> getActions() {
        return bestActions;
    }

    private void rec(int actionPoints, int x, int y, int healedSum, boolean holdingFieldRation, boolean holdingMedikit) {
        updateBest(actionPoints, healedSum, holdingFieldRation, holdingMedikit);

        tryEatFieldRation(actionPoints, x, y, healedSum, holdingFieldRation, holdingMedikit);

        tryHealTeammates(actionPoints, x, y, healedSum, holdingFieldRation, holdingMedikit);

        tryHealSelf(actionPoints, x, y, healedSum, holdingFieldRation, holdingMedikit);

        tryMove(actionPoints, x, y, healedSum, holdingFieldRation, holdingMedikit);
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
        if(holdingMedikit && actionPoints >= game.getMedikitUseCost()) {
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
            if (!Utils.isLetter(targetChar) || targetChar == Utils.getCharForTrooperType(FIELD_MEDIC)) {
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
        if(avoidOverheal && hpDiff < healValue) {
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

    private void tryHealSelf(int actionPoints, int x, int y, int healedSum, boolean holdingFieldRation, boolean holdingMedikit) {
        if(actionPoints >= game.getFieldMedicHealCost()) {
            tryHeal(actionPoints - game.getFieldMedicHealCost(), x, y, healedSum, MyAction.HEAL_SELF, FIELD_MEDIC.ordinal(), game.getFieldMedicHealSelfBonusHitpoints(), holdingFieldRation, holdingMedikit);
        }
        if(holdingMedikit && actionPoints >= game.getMedikitUseCost()) {
            tryHeal(actionPoints - game.getMedikitUseCost(), x, y, healedSum, MyAction.USE_MEDIKIT_SELF, FIELD_MEDIC.ordinal(), game.getMedikitHealSelfBonusHitpoints(), holdingFieldRation, false);
        }
    }

    private boolean inField(int toX, int toY) {
        return toX >= 0 && toX < map.length && toY >= 0 && toY < map[0].length;
    }

    private void updateBest(int actionPoints, int healedSum, boolean holdingFieldRation, boolean holdingMedikit) {
        int minHp = getMinHp();
        if (better(actionPoints, healedSum, holdingFieldRation, minHp, holdingMedikit)) {
            bestActionPoints = actionPoints;
            bestHealedSum = healedSum;
            bestHoldingFieldRation = holdingFieldRation;
            bestMinHp = minHp;
            bestHoldingMedikit = holdingMedikit;

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

    private boolean better(int actionPoints, int healedSum, boolean holdingFieldRation, int minHp, boolean holdingMedikit) {
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
