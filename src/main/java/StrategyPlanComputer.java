import model.BonusType;

import java.util.List;

public class StrategyPlanComputer extends AbstractPlanComputer {

    public StrategyPlanComputer(char[][] map, Utils utils, List<MutableTrooper> teammates, MutableTrooper self, boolean[] visibilities, BonusType[][] bonuses, MutableTrooper[][] troopers) {
        super(map, utils, teammates, self, visibilities, bonuses, troopers);
    }

    @Override
    protected void rec() {

    }
}
