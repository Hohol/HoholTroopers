import model.TrooperStance;

import java.util.List;

abstract class State <S extends State> {
    List<MyMove> actions;

    int actionPoints;
    boolean holdingFieldRation;
    int x;
    int y;
    TrooperStance stance;

    protected State(List<MyMove> actions, int actionPoints, boolean holdingFieldRation, int x, int y, TrooperStance stance) {
        this.actions = actions;
        this.actionPoints = actionPoints;
        this.holdingFieldRation = holdingFieldRation;
        this.y = y;
        this.x = x;
        this.stance = stance;
    }

    abstract boolean better(S cur);
}