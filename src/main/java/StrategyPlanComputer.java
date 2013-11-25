import model.BonusType;

import java.util.List;

public class StrategyPlanComputer extends AbstractPlanComputer<StrategyState> {

    Cell destination;
    int[][] distToDestination;

    public StrategyPlanComputer(
            char[][] map,
            Utils utils,
            List<MutableTrooper> teammates,
            MutableTrooper self,
            boolean[] visibilities,
            BonusType[][] bonuses,
            MutableTrooper[][] troopers,
            Cell destination
    ) {
        super(map, utils, teammates, visibilities, bonuses, troopers);
        this.destination = destination;
        cur = new StrategyState(self);
    }

    @Override
    protected void prepare() {
        super.prepare();
        distToDestination = Utils.bfsByMap(map, destination.x, destination.y);
    }

    @Override
    protected void tryAllActions() {
        tryMove();
        tryRaiseStance();
        tryLowerStance();
    }

    @Override
    void updateBest() {
        cur.distToDestination = distToDestination[cur.x][cur.y];
        if (cur.better(best, selfType)) {
            best = new StrategyState(cur);
        }
    }
}
