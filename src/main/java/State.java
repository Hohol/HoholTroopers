import model.TrooperStance;

import java.util.List;

abstract class State <S extends State> {

    List<MyMove> actions;
    int actionPoints;
    boolean holdingFieldRation;
    int x;
    int y;
    TrooperStance stance;
    int selfHp;
    int healedSum;
    boolean holdingMedikit;
    int killedCnt;
    int damageSum;
    int focusFireParameter;
    boolean holdingGrenade;

    protected State(
            List<MyMove> actions,
            int actionPoints,
            boolean holdingFieldRation,
            int x,
            int y,
            TrooperStance stance,
            int hp,
            boolean holdingMedikit,
            int killedCnt,
            int damageSum,
            int focusFireParameter,
            boolean holdingGrenade,
            int healedSum
    ) {
        this.actions = actions;
        this.actionPoints = actionPoints;
        this.holdingFieldRation = holdingFieldRation;
        this.y = y;
        this.x = x;
        this.stance = stance;
        this.selfHp = hp;
        this.holdingMedikit = holdingMedikit;
        this.killedCnt = killedCnt;
        this.damageSum = damageSum;
        this.focusFireParameter = focusFireParameter;
        this.holdingGrenade = holdingGrenade;
        this.healedSum = healedSum;
    }

    abstract boolean better(S cur);
}