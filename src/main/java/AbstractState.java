import model.TrooperStance;
import model.TrooperType;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractState <S extends AbstractState> {

    public int newSeenCellsCnt;
    List<MyMove> actions;
    boolean holdingFieldRation;
    boolean holdingMedikit;
    boolean holdingGrenade;
    int actionPoints;
    int x;
    int y;
    TrooperStance stance;
    int selfHp;
    int fieldRationsUsed;

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
    public AbstractState(AbstractState s) {
        this.holdingFieldRation = s.holdingFieldRation;
        this.holdingMedikit = s.holdingMedikit;
        this.holdingGrenade = s.holdingGrenade;
        this.actionPoints = s.actionPoints;
        this.actions = new ArrayList<MyMove>(s.actions);
        this.x = s.x;
        this.y = s.y;
        this.stance = s.stance;
        this.selfHp = s.selfHp;
        this.fieldRationsUsed = s.fieldRationsUsed;
        newSeenCellsCnt = s.newSeenCellsCnt;
    }
    public AbstractState(MutableTrooper self) {
        this(self.getActionPoints(), self.getHitpoints(), self.getX(), self.getY(), self.getStance(), self.isHoldingFieldRation(), self.isHoldingGrenade(), self.isHoldingMedikit());
    }
    abstract boolean better(S old, TrooperType selfType);
}
