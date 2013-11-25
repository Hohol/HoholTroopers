import model.TrooperStance;
import model.TrooperType;

import java.util.ArrayList;

class TacticState extends AbstractState <TacticState> {

    //----

    int healedSum;
    int killCnt;
    int damageSum;
    int fieldRationsUsed;

    int focusFireParameter;
    int minHp;
    int healDist;
    int helpFactor;
    int helpDist;

    int maxDamageEnemyCanDeal;
    int numberOfTeammatesWhoCanReachEnemy;
    boolean someOfTeammatesCanBeKilled;

    protected TacticState(TacticState cur) {
        super(cur.actionPoints, cur.selfHp, cur.x, cur.y, cur.stance, cur.holdingFieldRation, cur.holdingGrenade, cur.holdingMedikit);
        this.actions = new ArrayList<>(cur.actions);
        this.killCnt = cur.killCnt;
        this.damageSum = cur.damageSum;
        this.healedSum = cur.healedSum;
        this.focusFireParameter = cur.focusFireParameter;
        this.minHp = cur.minHp;
        this.healDist = cur.healDist;
        this.helpFactor = cur.helpFactor;
        this.helpDist = cur.helpDist;
        this.fieldRationsUsed = cur.fieldRationsUsed;

        this.numberOfTeammatesWhoCanReachEnemy = cur.numberOfTeammatesWhoCanReachEnemy;

        this.maxDamageEnemyCanDeal = cur.maxDamageEnemyCanDeal;
        this.someOfTeammatesCanBeKilled = cur.someOfTeammatesCanBeKilled;
    }

    public TacticState(MutableTrooper self) {
        super(self);
    }

    @Override
    boolean better(TacticState old, TrooperType selfType) {
        if (old == null) {
            return true;
        }

        int killDiff = killCnt;
        int oldKillDiff = old.killCnt;

        if (someOfTeammatesCanBeKilled) {
            killDiff--;
        }
        if (old.someOfTeammatesCanBeKilled) {
            oldKillDiff--;
        }

        if (killDiff != oldKillDiff) {
            return killDiff > oldKillDiff;
        }

        int hpDiff = damageSum + healedSum - maxDamageEnemyCanDeal;
        int oldHpDiff = old.damageSum + old.healedSum - old.maxDamageEnemyCanDeal;

        if (Math.abs(hpDiff - oldHpDiff) < 50) {
            if (numberOfTeammatesWhoCanReachEnemy != old.numberOfTeammatesWhoCanReachEnemy) {
                return numberOfTeammatesWhoCanReachEnemy > old.numberOfTeammatesWhoCanReachEnemy;
            }
        }

        if (hpDiff != oldHpDiff) {
            return hpDiff > oldHpDiff;
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

    int medicSpecificCompare(TacticState old) {
        if (minHp != old.minHp) {
            return old.minHp - minHp;
        }
        if (healDist != old.healDist) {
            return healDist - old.healDist;
        }
        return 0;
    }

    private int nonMedicSpecificBetter(TacticState old) {
        if (helpDist != old.helpDist) {
            return helpDist - old.helpDist;
        }

        if (helpFactor != old.helpFactor) {
            return old.helpFactor - helpFactor;
        }
        return 0;
    }
}