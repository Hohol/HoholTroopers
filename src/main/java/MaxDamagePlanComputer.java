import model.ActionType;
import model.Game;
import model.TrooperStance;
import model.TrooperType;

import java.util.ArrayList;
import java.util.List;

import static model.ActionType.EAT_FIELD_RATION;
import static model.ActionType.LOWER_STANCE;
import static model.ActionType.SHOOT;

class MaxDamagePlanComputer {
    private final TrooperType selfType;
    private final TrooperStance minStanceAllowed;
    private final Game game;

    private int bestActionPoints, bestTargetHp;
    private boolean bestHoldingFieldRation;
    private TrooperStance bestCurrentStance;

    List<ActionType> actions = new ArrayList<>(), bestActions;

    public MaxDamagePlanComputer(
            TrooperType selfType,
            int actionPoints,
            TrooperStance currentStance, TrooperStance minStanceAllowed,
            int targetHp,
            boolean holdingFieldRation,
            Game game
    ) {
        this.selfType = selfType;
        this.minStanceAllowed = minStanceAllowed;
        this.game = game;

        bestActionPoints = actionPoints;
        bestTargetHp = targetHp;
        bestHoldingFieldRation = holdingFieldRation;
        bestCurrentStance = currentStance;
        bestActions = new ArrayList<>();

        rec(actionPoints, currentStance, targetHp, holdingFieldRation);
    }

    List<ActionType> getActions() {
        return bestActions;
    }

    private void rec(int actionPoints, TrooperStance currentStance, int targetHp, boolean holdingFieldRation) {
        if (targetHp <= 0) {
            targetHp = 0;
        }
        if (better(actionPoints, currentStance, targetHp, holdingFieldRation)) {
            bestActionPoints = actionPoints;
            bestCurrentStance = currentStance;
            bestTargetHp = targetHp;
            bestHoldingFieldRation = holdingFieldRation;
            bestActions = new ArrayList<>(actions);
        }
        if (targetHp == 0) {
            return;
        }

        if (holdingFieldRation && actionPoints >= game.getFieldRationEatCost()) {
            addAction(EAT_FIELD_RATION);
            rec(
                    Utils.actionPointsAfterEatingFieldRation(selfType, actionPoints, game),
                    currentStance,
                    targetHp,
                    false
            );
            popAction();
        }

        if (actionPoints >= game.getStanceChangeCost() && currentStance.ordinal() > minStanceAllowed.ordinal()) {
            addAction(LOWER_STANCE);

            rec(
                    actionPoints - game.getStanceChangeCost(),
                    Utils.stanceAfterLowering(currentStance),
                    targetHp,
                    holdingFieldRation
            );

            popAction();
        }

        if (actionPoints >= Utils.getShootCost(selfType)) {
            addAction(SHOOT);

            rec(
                    actionPoints - Utils.getShootCost(selfType),
                    currentStance,
                    targetHp - Utils.getShootDamage(selfType, currentStance),
                    holdingFieldRation
            );

            popAction();
        }
    }

    private void popAction() {
        actions.remove(actions.size() - 1);
    }

    private void addAction(ActionType action) {
        actions.add(action);
    }

    private boolean better(int actionPoints, TrooperStance currentStance, int targetHp, boolean holdingFieldRation) {
        if (targetHp != bestTargetHp) {
            return targetHp < bestTargetHp;
        }
        if (holdingFieldRation != bestHoldingFieldRation) {
            return holdingFieldRation;
        }
        if (actionPoints != bestActionPoints) {
            return actionPoints > bestActionPoints;
        }
        if (currentStance != bestCurrentStance) {
            return currentStance.ordinal() > bestCurrentStance.ordinal();
        }
        return false;
    }
}