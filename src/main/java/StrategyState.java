import model.TrooperStance;
import model.TrooperType;

public class StrategyState extends AbstractState <StrategyState> {

    public StrategyState(MutableTrooper self) {
        super(self);
    }

    public StrategyState(StrategyState cur) {
        super(cur);
    }

    @Override
    boolean better(StrategyState old, TrooperType selfType) {
        if(old == null) {
            return true;
        }
        return false;
    }
}
