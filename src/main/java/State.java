import model.TrooperStance;
import model.TrooperType;

import java.util.ArrayList;
import java.util.List;

class State {

    List<MyMove> actions;
    int actionPoints;
    boolean holdingFieldRation;
    boolean holdingMedikit;
    boolean holdingGrenade;
    int x;
    int y;
    TrooperStance stance;
    int selfHp;

    //----

    int healedSum;
    int killedCnt;
    int damageSum;

    int focusFireParameter;
    int minHp;
    int distSum;
    int helpFactor;
    int helpDist;

    protected State(
            int actionPoints,
            int hp, int x, int y, TrooperStance stance, boolean holdingFieldRation,
            boolean holdingGrenade, boolean holdingMedikit
    ) {
        this.actions = new ArrayList<>();
        this.actionPoints = actionPoints;
        this.holdingFieldRation = holdingFieldRation;
        this.y = y;
        this.x = x;
        this.stance = stance;
        this.selfHp = hp;
        this.holdingMedikit = holdingMedikit;
        this.holdingGrenade = holdingGrenade;
    }

    protected State(State cur) {
        this(cur.actionPoints, cur.selfHp, cur.x, cur.y, cur.stance, cur.holdingFieldRation, cur.holdingGrenade, cur.holdingMedikit);
        this.actions = new ArrayList<>(cur.actions);
        this.killedCnt = cur.killedCnt;
        this.damageSum = cur.damageSum;
        this.healedSum = cur.healedSum;
        this.focusFireParameter = cur.focusFireParameter;
        this.minHp = cur.minHp;
        this.distSum = cur.distSum;
        this.helpFactor = cur.helpFactor;
        this.helpDist = cur.helpDist;
    }

    boolean better(State old, TrooperType selfType) {
        if (old == null) {
            return true;
        }

        if (killedCnt != old.killedCnt) {
            return killedCnt > old.killedCnt;
        }

        int hpDiff = damageSum + healedSum;
        int oldHpDiff = old.damageSum + old.healedSum;

        if (hpDiff != oldHpDiff) {
            return hpDiff > oldHpDiff;
        }

        if (helpDist != old.helpDist) {
            return helpDist < old.helpDist;
        }

        if (helpFactor != old.helpFactor) {
            return helpFactor > old.helpFactor;
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
        if (selfType == TrooperType.FIELD_MEDIC) {
            if (distSum != old.distSum) {
                return distSum < old.distSum;
            }
        }
        if (actionPoints != old.actionPoints) {
            return actionPoints > old.actionPoints;
        }

        if (holdingGrenade != old.holdingGrenade) {
            return holdingGrenade;
        }

        if (focusFireParameter != old.focusFireParameter) {
            return focusFireParameter > old.focusFireParameter;
        }

        return false;
    }
}