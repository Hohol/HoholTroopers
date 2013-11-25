import model.TrooperStance;
import model.TrooperType;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractState <S extends AbstractState> {

    boolean holdingFieldRation;
    boolean holdingMedikit;
    boolean holdingGrenade;
    int actionPoints;
    List<MyMove> actions;
    int x;
    int y;
    TrooperStance stance;
    int selfHp;

    public AbstractState(int actionPoints, int hp, int x, int y, TrooperStance stance, boolean holdingFieldRation, boolean holdingGrenade, boolean holdingMedikit) {
        this.holdingFieldRation = holdingFieldRation;
        this.holdingMedikit = holdingMedikit;
        this.holdingGrenade = holdingGrenade;
        this.actionPoints = actionPoints;
        this.actions = new ArrayList<>();
        this.x = x;
        this.y = y;
        this.stance = stance;
        this.selfHp = hp;
    }
    public AbstractState(MutableTrooper self) {
        this(self.getActionPoints(), self.getHitpoints(), self.getX(), self.getY(), self.getStance(), self.isHoldingFieldRation(), self.isHoldingGrenade(), self.isHoldingMedikit());
    }
    abstract boolean better(S old, TrooperType selfType);
}
