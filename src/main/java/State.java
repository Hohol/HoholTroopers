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
    int fieldRationsUsed;

    int focusFireParameter;
    int minHp;
    int healDist;
    int helpFactor;
    int helpDist;
    int blockFactor;

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
        this.healDist = cur.healDist;
        this.helpFactor = cur.helpFactor;
        this.helpDist = cur.helpDist;
        this.fieldRationsUsed = cur.fieldRationsUsed;
        this.blockFactor = cur.blockFactor;
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

        int msb = medicSpecificCompare(old);
        int nmsb = nonMedicSpecificBetter(old);

        if (selfType == TrooperType.FIELD_MEDIC) {
            if (msb != 0) {
                return msb < 0;
            }
            if (nmsb != 0) {
                return nmsb < 0;
            }

        } else {
            if (nmsb != 0) {
                return nmsb < 0;
            }

            if (msb != 0) {
                return msb < 0;
            }
        }

        if (holdingMedikit != old.holdingMedikit) {
            return holdingMedikit;
        }
        if (holdingFieldRation != old.holdingFieldRation) {
            return holdingFieldRation;
        }

        if (holdingGrenade != old.holdingGrenade) {
            return holdingGrenade;
        }

        if (focusFireParameter != old.focusFireParameter) {
            return focusFireParameter > old.focusFireParameter;
        }

        if (fieldRationsUsed != old.fieldRationsUsed) {
            return fieldRationsUsed < old.fieldRationsUsed;
        }

        if (actionPoints != old.actionPoints) {
            return actionPoints > old.actionPoints;
        }

        return false;
    }

    int medicSpecificCompare(State old) {
        if (minHp != old.minHp) {
            return old.minHp - minHp;
        }
        if (healDist != old.healDist) {
            return healDist - old.healDist;
        }
        return 0;
    }

    private int nonMedicSpecificBetter(State old) {
        if (helpDist != old.helpDist) {
            return helpDist - old.helpDist;
        }

        if (helpFactor != old.helpFactor) {
            return old.helpFactor - helpFactor;
        }
        return 0;
    }
}