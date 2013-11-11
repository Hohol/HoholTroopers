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

    List<MyAction> bestActions = new ArrayList<>();
    int bestActionPoints, bestHealedSum;
    boolean bestHoldingFieldRation;

    MaxHealingPlanComputer( //todo remember about transpose
                            int actionPoints,
                            char[][] map,
                            int[] hp, //TrooperType.ordinal() to hp
                            boolean holdingFieldRation,
                            Utils utils
    ) {
        this.map = map;
        this.hp = hp.clone();
        this.utils = utils;
        this.game = utils.getGame();
        moveCost = game.getStandingMoveCost(); //assume that medic is standing

        bestActionPoints = actionPoints;
        bestHoldingFieldRation = holdingFieldRation;

        Cell start = findStart();
        rec(actionPoints, start.x, start.y, 0, holdingFieldRation);
    }

    private Cell findStart() {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (map[i][j] == Utils.MEDIC_CHAR) {
                    return new Cell(j, i); // j, i >_<
                }
            }
        }
        throw new RuntimeException();
    }

    public List<MyAction> getActions() {
        return bestActions;
    }

    private void rec(int actionPoints, int x, int y, int healedSum, boolean holdingFieldRation) {
        updateBest(actionPoints, healedSum, holdingFieldRation);

        tryEatFieldRation(actionPoints, x, y, healedSum, holdingFieldRation);

        tryHealTeammates(actionPoints, x, y, healedSum, holdingFieldRation);

        tryHealSelf(actionPoints, x, y, healedSum, holdingFieldRation);

        tryMove(actionPoints, x, y, healedSum, holdingFieldRation);
    }

    private void tryMove(int actionPoints, int x, int y, int healedSum, boolean holdingFieldRation) {
        if (actionPoints >= moveCost) {
            for (MyAction movement : MyAction.movements) {
                int toX = x + movement.getDx();
                int toY = y + movement.getDy();
                if (!inField(toX, toY)) {
                    continue;
                }
                if(map[toY][toX] != '.') {
                    continue;
                }
                addAction(movement);
                rec(
                        actionPoints - moveCost,
                        toX,
                        toY,
                        healedSum,
                        holdingFieldRation
                );
                popAction();
            }
        }
    }

    private void tryEatFieldRation(int actionPoints, int x, int y, int healedSum, boolean holdingFieldRation) {
        if (holdingFieldRation && actionPoints >= game.getFieldRationEatCost() && actionPoints < utils.getInitialActionPoints(FIELD_MEDIC)) {
            addAction(MyAction.EAT_FIELD_RATION);
            rec(
                    utils.actionPointsAfterEatingFieldRation(FIELD_MEDIC, actionPoints, game),
                    x,
                    y,
                    healedSum,
                    false
            );
            popAction();
        }
    }

    private void tryHealTeammates(int actionPoints, int x, int y, int healedSum, boolean holdingFieldRation) {
        if (actionPoints >= game.getFieldMedicHealCost()) {
            for (MyAction heal : MyAction.directedHeals) {
                int toX = x + heal.getDx();
                int toY = y + heal.getDy();
                if (!inField(toX, toY)) {
                    continue;
                }
                char target = map[toY][toX];
                if (!Character.isAlphabetic(target)) {
                    continue;
                }
                int index = getIndex(target);
                tryHeal(actionPoints, x, y, healedSum, heal, index, game.getFieldMedicHealBonusHitpoints(), holdingFieldRation);
            }
        }
    }

    private void tryHeal(int actionPoints, int x, int y, int healedSum, MyAction healAction, int targetOrdinal, int healValue, boolean holdingFieldRation) {
        if (actionPoints < game.getFieldMedicHealCost()) {
            return;
        }
        int oldHp = hp[targetOrdinal];
        if (oldHp >= Utils.INITIAL_TROOPER_HP) {
            return;
        }
        int newHp = Math.min(oldHp + healValue, Utils.INITIAL_TROOPER_HP);
        hp[targetOrdinal] = newHp;

        addAction(healAction);
        rec(
                actionPoints - game.getFieldMedicHealCost(),
                x,
                y,
                healedSum + newHp - oldHp,
                holdingFieldRation
        );

        popAction();

        hp[targetOrdinal] = oldHp;
    }

    private void tryHealSelf(int actionPoints, int x, int y, int healedSum, boolean holdingFieldRation) {
        tryHeal(actionPoints, x, y, healedSum, MyAction.HEAL_SELF, FIELD_MEDIC.ordinal(), game.getFieldMedicHealSelfBonusHitpoints(), holdingFieldRation);
    }

    private boolean inField(int toX, int toY) {
        return toX >= 0 && toX < map[0].length && toY >= 0 && toY < map.length;
    }

    private void updateBest(int actionPoints, int healedSum, boolean holdingFieldRation) {
        if (better(actionPoints, healedSum, holdingFieldRation)) {
            bestActionPoints = actionPoints;
            bestHealedSum = healedSum;
            bestHoldingFieldRation = holdingFieldRation;

            bestActions = new ArrayList<>(actions);
        }
    }

    private boolean better(int actionPoints, int healedSum, boolean holdingFieldRation) {
        if (healedSum != bestHealedSum) {
            return healedSum > bestHealedSum;
        }
        if(holdingFieldRation != bestHoldingFieldRation) {
            return holdingFieldRation;
        }
        if(actionPoints != bestActionPoints) {
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
        switch(targetChar) {
            case 'F':
                return FIELD_MEDIC.ordinal();
            case 'S':
                return SOLDIER.ordinal();
            case 'C':
                return COMMANDER.ordinal();
        }
        return -1;
    }
}
