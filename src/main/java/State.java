import model.TrooperStance;

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

    protected State(
            int actionPoints,
            boolean holdingFieldRation,
            int x,
            int y,
            TrooperStance stance,
            int hp,
            boolean holdingMedikit,
            boolean holdingGrenade
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
        this(cur.actionPoints, cur.holdingFieldRation, cur.x, cur.y, cur.stance, cur.selfHp, cur.holdingMedikit, cur.holdingGrenade);
        this.actions = new ArrayList<>(cur.actions);
        this.killedCnt = cur.killedCnt;
        this.damageSum = cur.damageSum;
        this.healedSum = cur.healedSum;
        this.focusFireParameter = cur.focusFireParameter;
        this.minHp = cur.minHp;
        this.distSum = cur.distSum;
    }

    boolean better(State old) {
        if (old == null) {
            return true;
        }

        if (killedCnt != old.killedCnt) {
            return killedCnt > old.killedCnt;
        }
        if (damageSum != old.damageSum) {
            return damageSum > old.damageSum;
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

        if (holdingGrenade != old.holdingGrenade) {
            return holdingGrenade;
        }

        if (focusFireParameter != old.focusFireParameter) {
            return focusFireParameter > old.focusFireParameter;
        }

        return false;
    }
}