import model.BonusType;
import model.TrooperType;

import static model.TrooperType.*;

import java.util.*;

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
            Map<Long, Set<TrooperType>> killedEnemies,
            int mediumMoveIndex,
            int initialTeamSize,
            boolean mapIsStatic
    ) {
        super(map, utils, teammates, visibilities, bonuses, troopers, self, mapIsStatic, prevActions, killedEnemies, mediumMoveIndex, initialTeamSize);
        this.destination = destination;
        cur = new StrategyState(self);
    }

    @Override
    protected void prepare() {
        super.prepare();
        distWithoutTeammates = getDistWithoutTeammates();
        distToDestination = Utils.bfsByMap(map, destination.x, destination.y);
        chooseLeader();
        prepareDangerArea();
    }

    private void prepareDangerArea() {
        dangerArea = new List[n][m][Utils.NUMBER_OF_STANCES];
    }

    private List<Cell3D> getDangerCells(int targetX, int targetY, int targetStance) {
        if (dangerArea[targetX][targetY][targetStance] == null) {
            List<Cell3D> r = new ArrayList<>();
            for (int shooterX = 0; shooterX < n; shooterX++) {
                for (int shooterY = 0; shooterY < m; shooterY++) {
                    for (int shooterStance = 0; shooterStance < Utils.NUMBER_OF_STANCES; shooterStance++) {
                        if (isWall(shooterX, shooterY)) {
                            continue;
                        }
                        int shootRange = 9;
                        if (!reachable(shooterX, shooterY, targetX, targetY, Math.min(shooterStance, targetStance), shootRange)) {
                            continue;
                        }
                        boo(r, shooterX, shooterY, shooterStance);
                        boo(r, shooterX + 1, shooterY, shooterStance);
                        boo(r, shooterX - 1, shooterY, shooterStance);
                        boo(r, shooterX, shooterY + 1, shooterStance);
                        boo(r, shooterX, shooterY - 1, shooterStance);
                        boo(r, shooterX, shooterY, shooterStance - 1);
                    }
                }
            }
            dangerArea[targetX][targetY][targetStance] = r;
        }
        return dangerArea[targetX][targetY][targetStance];
    }

    private void boo(List<Cell3D> r, int shooterX, int shooterY, int shooterStance) {
        if (!inField(shooterX, shooterY)) {
            return;
        }
        if (shooterStance < 0) {
            return;
        }
        if (isWall(shooterX, shooterY)) {
            return;
        }
        r.add(new Cell3D(shooterX, shooterY, shooterStance));
    }

    private boolean checkCanShoot(int shooterX, int shooterY, int targetX, int targetY, int shooterStance, int targetStance, TrooperType fieldMedic) {
        if (!inField(shooterX, shooterY) || shooterStance < 0 || shooterStance >= Utils.NUMBER_OF_STANCES) {
            return false;
        }
        return canShoot(shooterX, shooterY, targetX, targetY, shooterStance, targetStance, FIELD_MEDIC);
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

    private int getLeadersDistToDestination() {
        if (selfIsLeader()) {
            return 0;
        }
        int x = cur.x, y = cur.y;
        if (cur.x == destination.x && cur.y == destination.y) {
            return Utils.UNREACHABLE * 2;
        }
        if (leadersDistToDestination[x][y] == -1) {
            int r = getDist(map, x, y) + getDist(mapWithoutTeammates, x, y);
            leadersDistToDestination[x][y] = r;
        }
        return leadersDistToDestination[x][y];
    }

    private int getDist(char[][] mapa, int x, int y) {
        char buf = mapa[x][y];
        mapa[x][y] = Utils.getCharForTrooperType(selfType);
        int r = Utils.bfsByMap(mapa, leader.getX(), leader.getY())[destination.x][destination.y];
        mapa[x][y] = buf;
        return r;
    }

    private int getLeaderIndex(int maxDist) { //returns -1 if self is leader
        int best = -1;
        int maxPriority = leaderPriority(self.getType());
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

    private static int leaderPriority(TrooperType type) {
        switch (type) {
            case SOLDIER:
                return 4;
            case COMMANDER:
                return 3;
            case SCOUT:
                return 5;
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
        cur.leadersDistToDestination = getLeadersDistToDestination();
        cur.dangerAreaFactor= getDangerAreaFactor();

        if (cur.better(best, selfType)) {
            Utils.log(cur);
            best = new StrategyState(cur);
        }
    }

    private int getDangerAreaFactor() {
        int r = 0;
        for (Cell3D cell : getDangerCells(cur.x, cur.y, cur.stance.ordinal())) {
            if (visibleCnt[cell.x][cell.y][cell.stance] == 0) {
                r++;
            }
        }
        return r;
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
