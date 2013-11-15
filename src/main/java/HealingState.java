import model.TrooperStance;

import java.util.ArrayList;
import java.util.List;

class HealingState extends State <HealingState> {
    int minHp;
    int distSum;

    protected HealingState(List<MyMove> actions, int actionPoints, boolean holdingFieldRation, boolean holdingMedikit, int x, int y, int selfHp, int killedCnt, int damageSum, int focusFireParameter, boolean holdingGrenade, int healedSum) {
        super(actions, actionPoints, holdingFieldRation, x, y, TrooperStance.STANDING, selfHp, holdingMedikit, killedCnt, damageSum, focusFireParameter, holdingGrenade, healedSum);
    }

    public HealingState(HealingState cur) {
        this(new ArrayList<>(cur.actions), cur.actionPoints, cur.holdingFieldRation, cur.holdingMedikit, cur.x, cur.y, cur.selfHp, cur.killedCnt, cur.damageSum, cur.focusFireParameter, cur.holdingGrenade, cur.healedSum);
        healedSum = cur.healedSum;
        minHp = cur.minHp;
        distSum = cur.distSum;
    }

    @Override
    public boolean better(HealingState old) {
        if (old == null) {
            return true;
        }
        if (healedSum != old.healedSum) {
            return healedSum > old.healedSum;
        }

        if (minHp != old.minHp) {
            return minHp > old.minHp;
        }
        if (holdingMedikit != old.holdingMedikit) {
            return holdingMedikit;
        }
        if (holdingFieldRation != old.holdingFieldRation) {
            return holdingFieldRation;
        }
        if (distSum != old.distSum) {
            return distSum < old.distSum;
        }
        if (actionPoints != old.actionPoints) {
            return actionPoints > old.actionPoints;
        }
        return false;
    }
}