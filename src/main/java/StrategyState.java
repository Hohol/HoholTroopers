import model.TrooperStance;
import model.TrooperType;

public class StrategyState extends AbstractState <StrategyState> {

    public StrategyState(MutableTrooper self) {
        super(self);
    }

    @Override
    boolean better(StrategyState old, TrooperType selfType) {
        return false;
    }
}
