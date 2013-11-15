import model.TrooperStance;

import java.util.ArrayList;
import java.util.List;

class AttackState extends State<AttackState> {

    public AttackState(AttackState s) {
        this(new ArrayList<>(s.actions), s.actionPoints, s.holdingFieldRation, s.holdingGrenade, s.killedCnt, s.damageSum, s.stance, s.x, s.y, s.focusFireParameter, s.selfHp, s.healedSum, s.holdingMedikit);
    }

    public AttackState(
            List<MyMove> actions,
            int actionPoints,
            boolean holdingFieldRation,
            boolean holdingGrenade,
            int killedCnt,
            int damageSum,
            TrooperStance stance,
            int x,
            int y,
            int focusFireParameter,
            int selfHp,
            int healedSum,
            boolean holdingMedikit
    ) {
        super(actions, actionPoints, holdingFieldRation, x, y, stance, selfHp, holdingMedikit, killedCnt, damageSum, focusFireParameter, holdingGrenade, healedSum);
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

        if (focusFireParameter != old.focusFireParameter) {
            return focusFireParameter > old.focusFireParameter;
        }

        if (actionPoints != old.actionPoints) {
            return actionPoints > old.actionPoints;
        }
        return false;
    }
}