import static model.ActionType.*;

import model.BonusType;
import model.Move;
import model.TrooperType;

import static model.TrooperType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StrategyPlanComputer extends AbstractPlanComputer<StrategyState> {

    static final int MAX_DIST_TO_TEAMMATE = 5; // 5 was not chosen arbitrary. It is max_sniper_range - soldier_vision_range

    Cell destination;
    int[][] distToDestination;
    List<int[][]> distWithoutTeammates;
    MutableTrooper leader;
    private int[][] distToLeader;
    int[][] leadersDistToDestination;
    List<Cell3D>[][][] dangerArea;

    public StrategyPlanComputer(
            char[][] map,
            Utils utils,
            List<MutableTrooper> teammates,
            MutableTrooper self,
            boolean[] visibilities,
            BonusType[][] bonuses,
            MutableTrooper[][] troopers,
            Cell destination,
            List<MyMove> prevActions,
            boolean mapIsStatic
    ) {
        super(map, utils, teammates, visibilities, bonuses, troopers, self, mapIsStatic, prevActions);
        this.destination = destination;
        cur = new StrategyState(self);
    }

    @Override
    protected void prepare() {
        super.prepare();
        distToDestination = Utils.bfsByMap(map, destination.x, destination.y);
        distWithoutTeammates = getDistWithoutTeammates();
        chooseLeader();
        prepareDangerArea();
    }

    private void prepareDangerArea() {
        dangerArea = new List[n][m][Utils.NUMBER_OF_STANCES];
        for (int shooterX = 0; shooterX < n; shooterX++) {
            for (int shooterY = 0; shooterY < m; shooterY++) {
                if (isWall(shooterX, shooterY)) {
                    continue;
                }
                for (int stance = 0; stance < Utils.NUMBER_OF_STANCES; stance++) {
                    if (visibleCnt[shooterX][shooterY] != 0) {
                        continue;
                    }
                    for (int targetX = 0; targetX < n; targetX++) {
                        for (int targetY = 0; targetY < m; targetY++) {
                            if (canShoot(shooterX, shooterY, targetX, targetY, stance, stance, FIELD_MEDIC)) {
                                if (dangerArea[targetX][targetY][stance] == null) {
                                    dangerArea[targetX][targetY][stance] = new ArrayList<>();
                                }
                                dangerArea[targetX][targetY][stance].add(new Cell3D(shooterX, shooterY, stance));
                            }
                        }
                    }
                }
            }
        }
    }

    private void chooseLeader() {
        int ind = getLeaderIndex(15);
        if (ind == -1) {
            ind = getLeaderIndex(100);
        }
        leadersDistToDestination = new int[n][m];
        for (int[] a : leadersDistToDestination) {
            Arrays.fill(a, -1);
        }
        if (ind != -1) {
            leader = teammates.get(ind);
            distToLeader = getDistWithoutTeammates().get(ind);
        }
    }

    private int getLeadersDistToDestination(int i, int j) {
        if (selfIsLeader()) {
            return 0;
        }
        if (leadersDistToDestination[i][j] == -1) {
            char buf = map[i][j];
            map[i][j] = Utils.getCharForTrooperType(selfType);
            int r = Utils.bfsByMap(map, leader.getX(), leader.getY())[destination.x][destination.y];
            map[i][j] = buf;
            leadersDistToDestination[i][j] = r;
        }
        return leadersDistToDestination[i][j];
    }

    private int getLeaderIndex(int maxDist) { //returns -1 if self is leader
        int best = -1;
        int maxPriority = leaderPriority(selfType);
        for (int i = 0; i < teammates.size(); i++) {
            MutableTrooper ally = teammates.get(i);
            if (distWithoutTeammates.get(i)[cur.x][cur.y] > maxDist) {
                continue;
            }
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
        cur.leadersDistToDestination = getLeadersDistToDestination(cur.x, cur.y);
        cur.stayingInDangerArea = checkDangerArea();
        ;

        if (cur.better(best, selfType)) {
            best = new StrategyState(cur);
        }
    }

    private boolean checkDangerArea() {
        if (dangerArea[cur.x][cur.y][cur.stance.ordinal()] == null) {
            return false;
        }
        for (Cell3D cell : dangerArea[cur.x][cur.y][cur.stance.ordinal()]) {
            if (visibleCnt[cell.x][cell.y] == 0) {
                return true;
            }
        }
        return false;
    }

    private int getDistToLeader() {
        return selfIsLeader() ? 0 : distToLeader[cur.x][cur.y];
    }

    private int getMaxDistToTeammate() {
        if (!selfIsLeader()) {
            return distToLeader[cur.x][cur.y];
        }
        int ma = 0;
        for (int[][] dist : distWithoutTeammates) {
            ma = Math.max(ma, dist[cur.x][cur.y]);
        }
        return ma;
    }
}
