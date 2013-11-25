import static model.ActionType.*;

import model.BonusType;
import model.Move;
import model.TrooperType;

import java.util.ArrayList;
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
    List<Cell>[][][] cellsVisibleFrom;
    boolean[][][] visibleInitially;
    MutableTrooper self;    //for immutable fields only

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
        this.self = self;
        cur = new StrategyState(self);
    }

    @Override
    protected void prepare() {
        super.prepare();
        distToDestination = Utils.bfsByMap(map, destination.x, destination.y);
        distWithoutTeammates = getDistWithoutTeammates();
        chooseLeader();
        prepareVisibleInitially();
        prepareCellsVisibleFrom();
    }

    private void prepareVisibleInitially() {
        visibleInitially = new boolean[n][m][Utils.NUMBER_OF_STANCES];
        markVisibleInitially(self);
        for (MutableTrooper ally : teammates) {
            markVisibleInitially(ally);
        }
    }

    private void markVisibleInitially(MutableTrooper ally) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                for (int stance = 0; stance < Utils.NUMBER_OF_STANCES; stance++) {
                    if (reachable(ally.getX(), ally.getY(), i, j, ally.getStance().ordinal(), ally.getVisionRange())) {
                        visibleInitially[i][j][stance] = true;
                    }
                }
            }
        }
    }

    private void prepareCellsVisibleFrom() {
        cellsVisibleFrom = new List[n][m][Utils.NUMBER_OF_STANCES];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                for (int stance = 0; stance < Utils.NUMBER_OF_STANCES; stance++) {
                    cellsVisibleFrom[i][j][stance] = getCellsVisibleFrom(i, j, stance);
                }
            }
        }
    }

    private List<Cell> getCellsVisibleFrom(int x, int y, int stance) {
        List<Cell> r = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (reachable(x, y, i, j, stance, utils.getVisionRange(selfType))) {
                    r.add(new Cell(i, j));
                }
            }
        }
        return r;
    }

    private void chooseLeader() {
        int ind = getLeaderIndex(15);
        if (ind == -1) {
            ind = getLeaderIndex(100);
        }
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
        cur.leadersDistToDestination = leadersDistToDestination[cur.x][cur.y];
        cur.newSeenCellsCnt = getSeenCellsCnt();

        if (cur.better(best, selfType)) {
            Utils.log(cur);
            best = new StrategyState(cur);
        }
    }

    private int getSeenCellsCnt() {
        int r = 0;
        int x = cur.x, y = cur.y, stance = cur.stance.ordinal();
        boolean[][] wasSeen = new boolean[n][m];
        if (cur.actionPoints > 0) {
            r += markSeen(wasSeen, x, y, stance);
        }
        for (int i = cur.actions.size() - 1; i >= 0; i--) {
            Move action = cur.actions.get(i).getMove();
            if (action.getAction() == MOVE) {
                x -= action.getDirection().getOffsetX();
                y -= action.getDirection().getOffsetY();
                r += markSeen(wasSeen, x, y, stance);
            } else if (action.getAction() == RAISE_STANCE) {
                stance--;
                // no need to mark
            } else if (action.getAction() == LOWER_STANCE) {
                stance++;
                r += markSeen(wasSeen, x, y, stance);
            }
        }
        return r;
    }

    private int markSeen(boolean[][] wasSeen, int x, int y, int stance) {
        int r = 0;
        for (Cell cell : cellsVisibleFrom[x][y][stance]) {
            if (!wasSeen[cell.x][cell.y] && !visibleInitially[cell.x][cell.y][stance]) {
                wasSeen[cell.x][cell.y] = true;
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
