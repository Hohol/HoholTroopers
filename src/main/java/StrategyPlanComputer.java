import model.BonusType;

import java.util.List;

public class StrategyPlanComputer extends AbstractPlanComputer <StrategyState> {

    public StrategyPlanComputer(char[][] map, Utils utils, List<MutableTrooper> teammates, MutableTrooper self, boolean[] visibilities, BonusType[][] bonuses, MutableTrooper[][] troopers) {
        super(map, utils, teammates, visibilities, bonuses, troopers);
        cur = new StrategyState(self);
    }

    @Override
    protected void tryAllActions() {
        tryMove();
        tryRaiseStance();
        tryLowerStance();
    }

    @Override
    void updateBest() {
        if(cur.better(best, selfType)) {
            best = new StrategyState(cur);
        }
    }
}
