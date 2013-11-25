import model.BonusType;
import model.Trooper;
import model.TrooperType;

import java.util.List;

public class StrategyPlanComputer extends AbstractPlanComputer<StrategyState> {

    static final int MAX_DIST_TO_TEAMMATE = 5; // 5 was not chosen arbitrary. It is max_sniper_range - soldier_vision_range

    Cell destination;
    int[][] distToDestination;
    //List<int[][]> distToTeammates;
    List<int[][]> distWithoutTeammates;
    MutableTrooper leader;
    private int[][] distToLeader;
    int[][] leadersDistToDestination; //[self.x][self.y]

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
        distWithoutTeammates = getDistWithoutTeammates();
        chooseLeader();
    }

    private void chooseLeader() {
        int ind = getLeaderIndex();
        leadersDistToDestination = new int[n][m];
        if (ind != -1) {
            leader = teammates.get(ind);
            distToLeader = getDistWithoutTeammates().get(ind);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    if (!isFree(i, j)) {
                        continue;
                    }
                    char buf = map[i][j];
                    map[i][j] = Utils.getCharForTrooperType(selfType);
                    leadersDistToDestination[i][j] = Utils.bfsByMap(map, leader.getX(), leader.getY())[destination.x][destination.y];
                    map[i][j] = buf;
                }
            }
        }
    }

    private int getLeaderIndex() { //returns -1 if self is leader
        int best = -1;
        int maxPriority = leaderPriority(selfType);
        for (int i = 0; i < teammates.size(); i++) {
            MutableTrooper ally = teammates.get(i);
            int prior = leaderPriority(ally.getType());
            if (prior > maxPriority) {
                best = i;
                maxPriority = prior;
            }
        }
        return best;
    }

    boolean selfIsLeader() {
        return leader == null;
    }

    public static int leaderPriority(TrooperType type) {
        switch (type) {
            case SOLDIER:
                return 5;
            case COMMANDER:
                return 4;
            case SCOUT:
                return 3;
            case FIELD_MEDIC:
                return 2;
            case SNIPER:
                return 1;
        }
        throw new RuntimeException();
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
        cur.distToLeader = getDistToLeader();
        cur.leadersDistToDestination = leadersDistToDestination[cur.x][cur.y];

        if (cur.better(best, selfType)) {
            best = new StrategyState(cur);
        }
    }

    private int getDistToLeader() {
        return selfIsLeader() ? 0 : distToLeader[cur.x][cur.y];
    }

    private int getMaxDistToTeammate() {
        int ma = 0;
        for (int[][] dist : distWithoutTeammates) {
            ma = Math.max(ma, dist[cur.x][cur.y]);
        }
        return ma;
    }
}
