import model.BonusType;

import java.util.List;

public class StrategyPlanComputer extends AbstractPlanComputer<StrategyState> {

    static final int MAX_DIST_TO_TEAMMATE = 5; // 5 was not chosen arbitrary. It is max_sniper_range - soldier_vision_range

    Cell destination;
    int[][] distToDestination;
    List<int[][]> distToTeammates;

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
        distToTeammates = getDistToTeammates();
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
        cur.maxDistToTeammate = getMaxDistToTeammate();

        if (cur.better(best, selfType)) {
            best = new StrategyState(cur);
        }
    }

    private int getMaxDistToTeammate() {
        int ma = 0;
        for (int[][] dist : distToTeammates) {
            ma = Math.max(ma, dist[cur.x][cur.y]);
        }
        return ma;
    }
}
