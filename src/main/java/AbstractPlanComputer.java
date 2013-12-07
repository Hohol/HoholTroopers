import model.BonusType;
import model.Game;

import model.Move;
import model.TrooperType;

import java.util.*;

import static model.ActionType.LOWER_STANCE;
import static model.ActionType.MOVE;
import static model.ActionType.RAISE_STANCE;
import static model.TrooperStance.PRONE;
import static model.TrooperStance.STANDING;

public abstract class AbstractPlanComputer<S extends AbstractState> {
    protected static final long MAX_RECURSIVE_CALLS = 500000;
    protected final int m;
    protected final int n;
    protected final char[][] map;
    protected final Game game;
    protected final Utils utils;
    protected final List<MutableTrooper> teammates; //here, unlike MyStrategy, teammates does not contain self
    protected S cur, best;
    protected TrooperType selfType;
    protected boolean[] visibilities;
    protected char[][] mapWithoutTeammates;
    long recursiveCallsCnt;
    BonusType[][] bonuses;
    MutableTrooper[][] troopers;
    int[][][] visibleCnt;
    static List<Cell3D>[][][] cellsVisibleFrom;
    static List<Cell3D>[][][][] cellsVisibleFromCache;
    MutableTrooper self;    //for immutable fields only
    boolean mapIsStatic;
    List<MyMove> prevActions;
    Map<Long, Set<TrooperType>> killedEnemies;
    int initialTeamSize;
    int mediumMoveIndex;
    private int[][] scoutingValue;

    public AbstractPlanComputer(
            char[][] map,
            Utils utils,
            List<MutableTrooper> teammates,
            boolean[] visibilities,
            BonusType[][] bonuses,
            MutableTrooper[][] troopers,
            MutableTrooper self,
            boolean mapIsStatic,
            List<MyMove> prevActions,
            Map<Long, Set<TrooperType>> killedEnemies,
            int mediumMoveIndex,
            int initialTeamSize
    ) {
        m = map[0].length;
        n = map.length;
        this.map = map;
        this.game = utils.getGame();
        this.utils = utils;
        this.teammates = teammates;
        this.visibilities = visibilities;
        this.bonuses = bonuses;
        this.troopers = troopers;
        this.self = self;
        this.mapIsStatic = mapIsStatic;
        this.prevActions = prevActions;
        this.killedEnemies = killedEnemies;
        this.initialTeamSize = initialTeamSize;
        this.mediumMoveIndex = mediumMoveIndex;
    }

    protected void addAction(MyMove action) {
        cur.actions.add(action);
    }

    protected void popAction() {
        cur.actions.remove(cur.actions.size() - 1);
    }

    protected boolean inField(int toX, int toY) {
        return toX >= 0 && toX < n && toY >= 0 && toY < m;
    }

    protected boolean isFree(int x, int y) {
        return map[x][y] == '.' || map[x][y] == '?' || troopers[x][y] != null && troopers[x][y].getHitpoints() <= 0;
    }

    protected abstract void tryAllActions();

    abstract void updateBest();

    protected void rec() {
        recursiveCallsCnt++;
        BonusType bonus = bonuses[cur.x][cur.y];
        boolean oldHoldingGrenade = cur.holdingGrenade;
        boolean oldHoldingFieldRation = cur.holdingFieldRation;
        boolean oldHoldingMedikit = cur.holdingMedikit;

        if (bonus == BonusType.GRENADE && !cur.holdingGrenade) {
            bonuses[cur.x][cur.y] = null;
            cur.holdingGrenade = true;
        }
        if (bonus == BonusType.FIELD_RATION && !cur.holdingFieldRation) {
            bonuses[cur.x][cur.y] = null;
            cur.holdingFieldRation = true;
        }
        if (bonus == BonusType.MEDIKIT && !cur.holdingMedikit) {
            bonuses[cur.x][cur.y] = null;
            cur.holdingMedikit = true;
        }

        updateBest();
        if (recursiveCallsCnt > MAX_RECURSIVE_CALLS) {
            return;
        }

        tryAllActions();

        bonuses[cur.x][cur.y] = bonus;
        cur.holdingGrenade = oldHoldingGrenade;
        cur.holdingFieldRation = oldHoldingFieldRation;
        cur.holdingMedikit = oldHoldingMedikit;
    }

    protected void tryRaiseStance() {
        if (cur.actionPoints < game.getStanceChangeCost()) {
            return;
        }
        if (cur.stance == STANDING) {
            return;
        }
        addAction(MyMove.RAISE_STANCE);
        cur.actionPoints -= game.getStanceChangeCost();
        cur.stance = Utils.stanceAfterRaising(cur.stance);
        see(1);
        rec();
        see(-1);
        cur.stance = Utils.stanceAfterLowering(cur.stance);
        cur.actionPoints += game.getStanceChangeCost();
        popAction();
    }

    protected void tryLowerStance() {
        if (cur.actionPoints < game.getStanceChangeCost()) {
            return;
        }
        if (cur.stance == PRONE) {
            return;
        }
        addAction(MyMove.LOWER_STANCE);
        cur.actionPoints -= game.getStanceChangeCost();
        cur.stance = Utils.stanceAfterLowering(cur.stance);
        rec();
        cur.stance = Utils.stanceAfterRaising(cur.stance);
        cur.actionPoints += game.getStanceChangeCost();
        popAction();
    }

    protected void tryMove() {
        int moveCost = utils.getMoveCost(cur.stance);
        if (cur.actionPoints < moveCost) {
            return;
        }
        for (MyMove movement : MyMove.movements) {
            int toX = cur.x + movement.getDx();
            int toY = cur.y + movement.getDy();
            if (!inField(toX, toY)) {
                continue;
            }
            if (!isFree(toX, toY)) {
                continue;
            }
            addAction(movement);

            cur.actionPoints -= moveCost;
            cur.x += movement.getDx();
            cur.y += movement.getDy();
            see(1);
            rec();
            see(-1);
            cur.x -= movement.getDx();
            cur.y -= movement.getDy();
            cur.actionPoints += moveCost;
            popAction();
        }
    }

    private void see(int d) {
        if (cur.actionPoints == 0) {
            return;
        }
        for (Cell3D cell : cellsVisibleFrom[cur.x][cur.y][cur.stance.ordinal()]) {
            if (visibleCnt[cell.x][cell.y][cell.stance] == 0) {
                cur.newSeenCellsCnt += scoutingValue[cell.x][cell.y];
            }
            visibleCnt[cell.x][cell.y][cell.stance] += d;
            if (visibleCnt[cell.x][cell.y][cell.stance] == 0) {
                cur.newSeenCellsCnt -= scoutingValue[cell.x][cell.y];
            }
        }
    }

    public List<MyMove> getPlan() {
        prepare();
        rec();
        return best.actions;
    }

    protected void prepare() {
        selfType = troopers[cur.x][cur.y].getType();
        map[cur.x][cur.y] = '.';
        troopers[cur.x][cur.y] = null;
        prepareVisibleInitially();
        prepareCellsVisibleFrom();
        prepareScoutingValue();
        mapWithoutTeammates = getMapWithoutTeammates();
    }

    private void prepareScoutingValue() {
        scoutingValue = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                scoutingValue[i][j] = getScoutingValue(i, j);
            }
        }
    }

    protected int getScoutingValue(int x, int y) {
        return 1;
    }

    @SuppressWarnings("unused")
    boolean stopOn(MyMove... move) { //for debug only
        return Arrays.asList(move).equals(cur.actions);
    }

    protected List<int[][]> getDistToTeammates() {
        List<int[][]> r = new ArrayList<>();
        for (MutableTrooper ally : teammates) {
            int[][] dist = Utils.bfsByMap(map, ally.getX(), ally.getY());
            r.add(dist);
        }
        return r;
    }

    protected List<int[][]> getDistWithoutTeammates() {
        List<int[][]> distWithoutTeammates = new ArrayList<>();
        for (int allyIndex = 0; allyIndex < teammates.size(); allyIndex++) {
            MutableTrooper ally = teammates.get(allyIndex);
            int[][] dist = Utils.bfsByMap(mapWithoutTeammates, ally.getX(), ally.getY());
            distWithoutTeammates.add(dist);
        }
        return distWithoutTeammates;
    }

    private char[][] getMapWithoutTeammates() {
        char[][] mapWithoutTeammates = new char[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                mapWithoutTeammates[i][j] = map[i][j];
                if (Utils.isTeammateChar(map[i][j])) {
                    mapWithoutTeammates[i][j] = '.';
                }
            }
        }
        return mapWithoutTeammates;
    }

    protected boolean visible(int viewerX, int viewerY, int objectX, int objectY, int stance) {
        int width = n;
        int height = m;
        int stanceCount = Utils.NUMBER_OF_STANCES;
        return visibilities[viewerX * height * width * height * stanceCount
                + viewerY * width * height * stanceCount
                + objectX * height * stanceCount
                + objectY * stanceCount
                + stance];
    }

    protected boolean reachable(int viewerX, int viewerY, int objectX, int objectY, int stance, int range) {
        if (Utils.sqrDist(viewerX, viewerY, objectX, objectY) > Utils.sqr(range)) {
            return false;
        }
        return visible(viewerX, viewerY, objectX, objectY, stance);
    }

    protected void markVisibleInitially(MutableTrooper ally) {
        int x = ally.getX();
        int y = ally.getY();
        int stance = ally.getStance().ordinal();
        int visionRange = ally.getVisionRange();
        markVisibleInitially(x, y, stance, visionRange);
    }

    private void markVisibleInitially(int x, int y, int viewerStance, int visionRange) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                for (int targetStance = 0; targetStance < Utils.NUMBER_OF_STANCES; targetStance++) {
                    if (reachable(x, y, i, j, Math.min(viewerStance, targetStance), visionRange)) {
                        visibleCnt[i][j][targetStance] = 1;
                    }
                }
            }
        }
    }

    protected List<Cell3D> getCellsVisibleFrom(int x, int y, int viewerStance) {
        int range = utils.getVisionRange(selfType);
        List<Cell3D> r = new ArrayList<>();
        int minI = Math.max(0, x - range);
        int maxI = Math.min(n - 1, x + range);
        int minJ = Math.max(0, y - range);
        int maxJ = Math.min(m - 1, y + range);
        for (int i = minI; i <= maxI; i++) {
            for (int j = minJ; j <= maxJ; j++) {
                if (isWall(i, j)) {
                    continue;
                }
                for (int targetStance = 0; targetStance < Utils.NUMBER_OF_STANCES; targetStance++) {
                    if (reachable(x, y, i, j, Math.min(viewerStance, targetStance), range)) {
                        r.add(new Cell3D(i, j, targetStance));
                    }
                }
            }
        }
        return r;
    }

    protected boolean isWall(int i, int j) {
        if (map[i][j] == '#' || Character.isDigit(map[i][j])) {
            return true;
        }
        return false;
    }

    protected void prepareCellsVisibleFrom() {
        if (cellsVisibleFromCache == null) {
            cellsVisibleFromCache = new List[initialTeamSize][][][];
        }
        if (!mapIsStatic) {
            cellsVisibleFrom = getCellsVisibleFrom();
        } else {
            if (cellsVisibleFromCache[mediumMoveIndex % initialTeamSize] == null) {
                cellsVisibleFromCache[mediumMoveIndex % initialTeamSize] = getCellsVisibleFrom();
            }
            cellsVisibleFrom = cellsVisibleFromCache[mediumMoveIndex % initialTeamSize];
        }
    }

    private List<Cell3D>[][][] getCellsVisibleFrom() {
        List<Cell3D>[][][] r = new List[n][m][Utils.NUMBER_OF_STANCES];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                for (int stance = 0; stance < Utils.NUMBER_OF_STANCES; stance++) {
                    r[i][j][stance] = getCellsVisibleFrom(i, j, stance);
                }
            }
        }
        return r;
    }

    protected void prepareVisibleInitially() {
        visibleCnt = new int[n][m][Utils.NUMBER_OF_STANCES];
        int x = cur.x, y = cur.y, stance = cur.stance.ordinal();
        for (int i = prevActions.size() - 1; i >= 0; i--) {
            Move move = prevActions.get(i).getMove();
            if (move.getAction() == MOVE) {
                x -= move.getDirection().getOffsetX();
                y -= move.getDirection().getOffsetY();
            } else if (move.getAction() == RAISE_STANCE) {
                stance--;
            } else if (move.getAction() == LOWER_STANCE) {
                stance++;
            }
            markVisibleInitially(x, y, stance, self.getVisionRange());
        }
        markVisibleInitially(self);
        for (MutableTrooper ally : teammates) {
            markVisibleInitially(ally);
        }
    }

    protected boolean canShoot(int shooterX, int shooterY, int targetX, int targetY, int shooterStance, int targetStance, TrooperType shooterType) {
        int shootRange = utils.getShootRange(shooterType, shooterStance);
        return reachable(shooterX, shooterY, targetX, targetY, Math.min(shooterStance, targetStance), shootRange);
    }
}
