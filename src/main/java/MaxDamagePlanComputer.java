import model.ActionType;
import model.Game;
import model.TrooperStance;
import model.TrooperType;

import java.util.ArrayList;
import java.util.List;

import static model.ActionType.EAT_FIELD_RATION;
import static model.ActionType.LOWER_STANCE;
import static model.ActionType.SHOOT;

public class MaxDamagePlanComputer {
    private final TrooperType selfType;
    private final TrooperStance minStanceAllowed;
    private final Game game;
    private final Utils utils;

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
            Utils utils
    ) {
        this.selfType = selfType;
        this.minStanceAllowed = minStanceAllowed;
        this.utils = utils;
        this.game = utils.getGame();

        bestActionPoints = actionPoints;
        bestTargetHp = targetHp;
        bestHoldingFieldRation = holdingFieldRation;
        bestCurrentStance = currentStance;
        bestActions = new ArrayList<>();

        rec(actionPoints, currentStance, targetHp, holdingFieldRation);
    }

    public List<ActionType> getActions() {
        return bestActions;
    }

    private void rec(int actionPoints, TrooperStance currentStance, int targetHp, boolean holdingFieldRation) {
        if (targetHp < 0) {
            targetHp = 0;
        }
        updateBest(actionPoints, currentStance, targetHp, holdingFieldRation);
        if (targetHp == 0) {
            return;
        }

        tryEatFieldRation(actionPoints, currentStance, targetHp, holdingFieldRation);

        tryLowerStance(actionPoints, currentStance, targetHp, holdingFieldRation);

        tryShoot(actionPoints, currentStance, targetHp, holdingFieldRation);
    }

    private void updateBest(int actionPoints, TrooperStance currentStance, int targetHp, boolean holdingFieldRation) {
        if (better(actionPoints, currentStance, targetHp, holdingFieldRation)) {
            bestActionPoints = actionPoints;
            bestCurrentStance = currentStance;
            bestTargetHp = targetHp;
            bestHoldingFieldRation = holdingFieldRation;
            bestActions = new ArrayList<>(actions);
        }
    }

    private void tryShoot(int actionPoints, TrooperStance currentStance, int targetHp, boolean holdingFieldRation) {
        if (actionPoints >= utils.getShootCost(selfType)) {
            addAction(SHOOT);

            rec(
                    actionPoints - utils.getShootCost(selfType),
                    currentStance,
                    targetHp - utils.getShootDamage(selfType, currentStance),
                    holdingFieldRation
            );

            popAction();
        }
    }

    private void tryLowerStance(int actionPoints, TrooperStance currentStance, int targetHp, boolean holdingFieldRation) {
        if (actionPoints >= game.getStanceChangeCost() && currentStance.ordinal() > minStanceAllowed.ordinal()) {
            addAction(LOWER_STANCE);

            rec(
                    actionPoints - game.getStanceChangeCost(),
                    utils.stanceAfterLowering(currentStance),
                    targetHp,
                    holdingFieldRation
            );

            popAction();
        }
    }

    private void tryEatFieldRation(int actionPoints, TrooperStance currentStance, int targetHp, boolean holdingFieldRation) {
        if (holdingFieldRation && actionPoints >= game.getFieldRationEatCost() && actionPoints < utils.getInitialActionPoints(selfType)) {
            addAction(EAT_FIELD_RATION);
            rec(
                    utils.actionPointsAfterEatingFieldRation(selfType, actionPoints, game),
                    currentStance,
                    targetHp,
                    false
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
        if (bestTargetHp == 0 && targetHp != 0) {
            return false;
        }
        if (targetHp == 0) {
            if (bestTargetHp != 0) {
                return true;
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
        } else {
            if (currentStance != bestCurrentStance) {
                return currentStance.ordinal() < bestCurrentStance.ordinal();
            }
            if (targetHp != bestTargetHp) {
                return targetHp < bestTargetHp;
            }
            if (holdingFieldRation != bestHoldingFieldRation) {
                return holdingFieldRation;
            }
            if (actionPoints != bestActionPoints) {
                return actionPoints > bestActionPoints;
            }
        }
        return false;
    }
}