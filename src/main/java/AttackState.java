import model.TrooperStance;

import java.util.ArrayList;
import java.util.List;

class AttackState extends State<AttackState> {
    int killedCnt;
    int damageSum;
    boolean holdingGrenade;

    public AttackState(AttackState s) {
        this(new ArrayList<>(s.actions), s.actionPoints, s.holdingFieldRation, s.holdingGrenade, s.killedCnt, s.damageSum, s.stance, s.x, s.y);
    }

    public AttackState(List<MyMove> actions, int actionPoints, boolean holdingFieldRation, boolean holdingGrenade, int killedCnt, int damageSum, TrooperStance stance, int x, int y) {
        super(actions, actionPoints, holdingFieldRation, x, y, stance);
        this.killedCnt = killedCnt;
        this.damageSum = damageSum;
        this.actionPoints = actionPoints;
        this.holdingGrenade = holdingGrenade;
    }

    @Override
    public boolean better(AttackState old) {
        if (old == null) {
            return true;
        }
        if (killedCnt != old.killedCnt) {
            return killedCnt > old.killedCnt;
        }
        if (damageSum != old.damageSum) {
            return damageSum > old.damageSum;
        }
        if (holdingGrenade != old.holdingGrenade) {
            return holdingGrenade;
        }
        if (holdingFieldRation != old.holdingFieldRation) {
            return holdingFieldRation;
        }
        if (actionPoints != old.actionPoints) {
            return actionPoints > old.actionPoints;
        }
        return false;
    }
}