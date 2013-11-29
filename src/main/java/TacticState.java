import model.TrooperType;

class TacticState extends AbstractState<TacticState> {
    int healedSum;
    int killCnt;
    int damageSum;

    int focusFireParameter;
    int minHp;
    int healDist;
    int helpFactor;
    int helpDist;

    TacticPlanComputer.DamageAndAP maxDamageEnemyCanDeal;
    int numberOfTeammatesWhoCanReachEnemy;
    boolean someOfTeammatesCanBeKilled;
    int numberOfTeammatesMedicCanReach;
    public boolean enemyKnowsWhereWeAre;
    int actionsEnemyMustSpendToHide;

    protected TacticState(TacticState cur) {
        super(cur);
        this.killCnt = cur.killCnt;
        this.damageSum = cur.damageSum;
        this.healedSum = cur.healedSum;
        this.focusFireParameter = cur.focusFireParameter;
        this.minHp = cur.minHp;
        this.healDist = cur.healDist;
        this.helpFactor = cur.helpFactor;
        this.helpDist = cur.helpDist;

        this.numberOfTeammatesWhoCanReachEnemy = cur.numberOfTeammatesWhoCanReachEnemy;

        this.maxDamageEnemyCanDeal = cur.maxDamageEnemyCanDeal;
        this.someOfTeammatesCanBeKilled = cur.someOfTeammatesCanBeKilled;
        this.numberOfTeammatesMedicCanReach = cur.numberOfTeammatesMedicCanReach;
        this.enemyKnowsWhereWeAre = cur.enemyKnowsWhereWeAre;
        this.actionsEnemyMustSpendToHide = cur.actionsEnemyMustSpendToHide;
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

        if (enemyKnowsWhereWeAre != old.enemyKnowsWhereWeAre) {
            return !enemyKnowsWhereWeAre;
        }/**/

        int hpDiff = damageSum + healedSum - maxDamageEnemyCanDeal.damage;
        int oldHpDiff = old.damageSum + old.healedSum - old.maxDamageEnemyCanDeal.damage;

        if (Math.abs(hpDiff - oldHpDiff) < 50) {
            if (numberOfTeammatesWhoCanReachEnemy != old.numberOfTeammatesWhoCanReachEnemy) {
                return numberOfTeammatesWhoCanReachEnemy > old.numberOfTeammatesWhoCanReachEnemy;
            }
            if (numberOfTeammatesMedicCanReach != old.numberOfTeammatesMedicCanReach) {
                return numberOfTeammatesMedicCanReach > old.numberOfTeammatesMedicCanReach;
            }
        }

        if (Math.abs(hpDiff - oldHpDiff) < 10) {
            if (maxDamageEnemyCanDeal.ap != old.maxDamageEnemyCanDeal.ap) {
                return maxDamageEnemyCanDeal.ap > old.maxDamageEnemyCanDeal.ap;
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

        int msb = medicSpecificBetter(old);
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

        if (newSeenCellsCnt != old.newSeenCellsCnt) {
            return newSeenCellsCnt > old.newSeenCellsCnt;
        }

        if (actionPoints != old.actionPoints) {
            return actionPoints > old.actionPoints;
        }

        return false;
    }

    int medicSpecificBetter(TacticState old) {
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
        if (actionsEnemyMustSpendToHide != old.actionsEnemyMustSpendToHide) {
            return old.actionsEnemyMustSpendToHide - actionsEnemyMustSpendToHide;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "TacticState{" +
                "healedSum=" + healedSum +
                ", killCnt=" + killCnt +
                ", damageSum=" + damageSum +
                ", focusFireParameter=" + focusFireParameter +
                ", minHp=" + minHp +
                ", healDist=" + healDist +
                ", helpFactor=" + helpFactor +
                ", helpDist=" + helpDist +
                ", maxDamageEnemyCanDeal=" + maxDamageEnemyCanDeal +
                ", numberOfTeammatesWhoCanReachEnemy=" + numberOfTeammatesWhoCanReachEnemy +
                ", someOfTeammatesCanBeKilled=" + someOfTeammatesCanBeKilled +
                ", numberOfTeammatesMedicCanReach=" + numberOfTeammatesMedicCanReach +
                ", enemyKnowsWhereWeAre=" + enemyKnowsWhereWeAre +
                ", actionsEnemyMustSpendToHide=" + actionsEnemyMustSpendToHide +
                '}';
    }
}