import model.*;

import static model.ActionType.*;
import static model.TrooperStance.*;
import static model.TrooperType.*;

import java.util.*;

public final class MyStrategy implements Strategy {
    final Random rnd = new Random(3222);
    final static Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    final static int NOT_VISITED = 666;
    Trooper self;
    World world;
    Game game;
    Move move;
    CellType[][] cells;
    boolean[][] occupiedByTrooper;
    static int[][] lastSeen;
    static final boolean local = System.getProperty("ONLINE_JUDGE") == null;

    ArrayList<Trooper> teammates;
    Trooper teammateToFollow;
    static int smallStepNumber;

    Map<Cell, int[][]> bfsCache = new HashMap<>();

    @Override
    public void move(Trooper self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;

        init();

        if (tryHeal()) {
            return;
        }

        if (tryThrowGrenade()) {
            return;
        }

        if (tryShoot()) {
            return;
        }

        if (tryDeblock()) {
            return;
        }

        if (tryMoveToBonus()) {
            return;
        }

        if (tryMove()) {
            return;
        }

        move.setAction(END_TURN);
    }

    private boolean tryMoveToBonus() {
        Bonus bonus = chooseBonus();
        if (bonus == null) {
            return false;
        }
        if (self.getStance() != STANDING && haveTime(game.getStanceChangeCost())) {
            move.setAction(RAISE_STANCE);
            return true;
        }
        return moveTo(bonus);
    }

    private Bonus chooseBonus() {
        int[][] dist = bfs(self.getX(), self.getY());
        Bonus r = null;
        for (Bonus bonus : world.getBonuses()) {
            if (isHoldingBonus(bonus.getType())) {
                continue;
            }
            if (tooFarFromTeammates(bonus)) {
                continue;
            }
            if (!isGoodCell(bonus.getX(), bonus.getY())) {
                continue;
            }
            int d = dist[bonus.getX()][bonus.getY()];
            if (!haveTime(getActionsToMove(d, self.getStance()))) {
                continue;
            }
            if (r == null || d < dist[r.getX()][r.getY()]) {
                r = bonus;
            }
        }
        return r;
    }

    private int getActionsToMove(int dist, TrooperStance stance) {
        return actionsToStandUp(stance) + dist * game.getStandingMoveCost();
    }

    private int actionsToStandUp(TrooperStance stance) {
        return (STANDING.ordinal() - stance.ordinal()) * game.getStanceChangeCost();
    }


    private boolean tooFarFromTeammates(Unit unit) {
        return tooFarFromTeammates(unit.getX(), unit.getY());
    }

    private boolean tooFarFromTeammates(int x, int y) {
        int[][] dist = bfs(x, y);
        for (Trooper trooper : teammates) {
            if (dist[trooper.getX()][trooper.getY()] >= 6) {
                return true;
            }
        }
        return false;
    }

    private boolean isHoldingBonus(BonusType type) {
        return isHoldingBonus(self, type);
    }

    private static boolean isHoldingBonus(Trooper trooper, BonusType type) {
        switch (type) {
            case GRENADE:
                return trooper.isHoldingGrenade();
            case MEDIKIT:
                return trooper.isHoldingMedikit();
            case FIELD_RATION:
                return trooper.isHoldingFieldRation();
        }
        throw new RuntimeException();
    }

    private boolean tryHeal() {
        if (self.getType() != FIELD_MEDIC && !self.isHoldingMedikit()) {
            return false;
        }
        if (self.getType() == FIELD_MEDIC && teammates.size() == 1) {
            return false;
        }
        Trooper target = getMostInjuredTeammate();
        if (target == null) {
            return false;
        }
        if (distTo(target) > 7) {
            return false;
        }
        if (manhattanDist(self, target) <= 1) {
            return heal(target);
        } else {
            return self.getStance() == STANDING &&
                    haveTime(getMoveCost(self)) &&
                    moveTo(target);
        }
    }

    private boolean heal(Trooper target) {
        if (self.isHoldingMedikit() &&
                haveTime(game.getMedikitUseCost()) &&
                target.getMaximalHitpoints() - target.getHitpoints() >= medikitHealValue(target)) {
            move.setAction(USE_MEDIKIT);
            setDirection(target);
            return true;
        }
        if (self.getType() == FIELD_MEDIC && haveTime(game.getFieldMedicHealCost())) {
            move.setAction(HEAL);
            setDirection(target);
            return true;
        }
        return false;
    }

    private int medikitHealValue(Trooper target) {
        if (target.getId() == self.getId()) {
            return game.getMedikitHealSelfBonusHitpoints();
        } else {
            return game.getMedikitBonusHitpoints();
        }
    }

    private Trooper getMostInjuredTeammate() {
        Trooper r = null;
        int maxDiff = 0;
        for (Trooper trooper : teammates) {
            int diff = trooper.getMaximalHitpoints() - trooper.getHitpoints();
            if (diff > maxDiff) {
                maxDiff = diff;
                r = trooper;
            }
        }
        return r;
    }

    private boolean tryDeblock() {
        if (!haveTime(getMoveCost(self))) {
            return false;
        }
        if (isBlockedByMe(teammateToFollow)) {
            moveRandom();
            return true;
        }
        return false;
    }

    private boolean isBlockedByMe(Trooper teammateToFollow) {
        return manhattanDist(self, teammateToFollow) == 1 &&
                moveCnt(teammateToFollow) == 0;
    }

    private int moveCnt(Trooper teammateToFollow) {
        int r = 0;
        for (Direction dir : dirs) {
            if (isValidMove(dir, teammateToFollow)) {
                r++;
            }
        }
        return r;
    }

    private boolean tryThrowGrenade() { //todo не надо бросать гранату в тех кто рядом с нами, и в тех, кого и так можно застрелить
        if (!haveTime(game.getGrenadeThrowCost())) {
            return false;
        }
        if (!self.isHoldingGrenade()) {
            return false;
        }
        for (Trooper trooper : world.getTroopers()) {
            if (!trooper.isTeammate()) {
                if (canThrowGrenade(trooper)) {
                    throwGrenade(trooper);
                    return true;
                }
            }
        }
        return false;
    }

    private void throwGrenade(Trooper trooper) {
        move.setAction(THROW_GRENADE);
        setDirection(trooper);
    }

    private boolean haveTime(int actionCost) {
        return self.getActionPoints() >= actionCost;
    }

    private void init() {
        bfsCache.clear();
        smallStepNumber++;
        log("SmallStepNumber = " + smallStepNumber);
        cells = world.getCells();
        teammates = getTeammates();
        teammateToFollow = getTeammateToFollow();
        occupiedByTrooper = getOccupiedByTrooper();
        if (lastSeen == null) {
            lastSeen = createIntMap(0);
        }
        updateLastSeen();
        //printMap();
    }

    private void log(Object o) {
        if (!local) {
            return;
        }
        System.out.println(o);
    }

    @SuppressWarnings("unused")
    private void printMap() {
        char[][] map = new char[world.getWidth()][world.getHeight()];
        for (char[] row : map) {
            Arrays.fill(row, '.');
        }
        for (Trooper trooper : teammates) {
            map[trooper.getX()][trooper.getY()] = trooper.getType().toString().charAt(0);
        }
        for (int j = 0; j < map[0].length; j++) {
            for (char[] aMap : map) {
                System.out.print(aMap[j]);
            }
            System.out.println();
        }
        System.out.println();
    }

    private void updateLastSeen() {
        for (Trooper trooper : teammates) {
            for (int i = 0; i < world.getWidth(); i++) {
                for (int j = 0; j < world.getHeight(); j++) {
                    if (world.isVisible(
                            trooper.getVisionRange(),
                            trooper.getX(),
                            trooper.getY(),
                            trooper.getStance(),
                            i,
                            j,
                            PRONE
                    )) {
                        lastSeen[i][j] = smallStepNumber;
                    }
                }
            }
        }
    }

    private int manhattanDist(Trooper self, Trooper target) {
        return manhattanDist(self.getX(), self.getY(), target.getX(), target.getY());
    }

    private Trooper getTeammateToFollow() {
        Trooper r = null;
        for (Trooper trooper : world.getTroopers()) {
            if (trooper.isTeammate()) {
                if (r == null || followPriority(trooper) > followPriority(r)) {
                    r = trooper;
                }
            }
        }
        return r;
    }

    private boolean tryMove() {
        if (self.getStance() != STANDING && haveTime(game.getStanceChangeCost())) {
            move.setAction(RAISE_STANCE);
            return true;
        }
        if (!haveTime(getMoveCost(self))) {
            return false;
        }

        if (self.getId() == teammateToFollow.getId()) {
            if (self.getActionPoints() <= 4 || overExtended() || anyTeammateNotFullHp() && medicIsAlive()) {
                standStill();
                return true;
            }
            return moveToNearestLongAgoSeenCell();
        } else {
            Trooper toFollow = teammateToFollow;
            if (teammates.size() >= 3 && distTo(teammateToFollow) >= 7) {
                toFollow = getOtherTeammate();
            }
            if (manhattanDist(self, toFollow) == 1) {
                return false;
            }
            return moveTo(toFollow);
        }
    }

    private boolean anyTeammateNotFullHp() {
        for (Trooper trooper : teammates) {
            if (trooper.getHitpoints() < trooper.getMaximalHitpoints()) {
                return true;
            }
        }
        return false;
    }

    private boolean medicIsAlive() {
        for (Trooper trooper : teammates) {
            if (trooper.getType() == FIELD_MEDIC) {
                return true;
            }
        }
        return false;
    }

    private Trooper getOtherTeammate() {
        for (Trooper trooper : teammates) {
            if (trooper.getId() != self.getId() && trooper.getId() != teammateToFollow.getId()) {
                return trooper;
            }
        }
        throw new RuntimeException();
    }

    private int distTo(Trooper trooper) {
        return distTo(trooper.getX(), trooper.getY());
    }

    private int distTo(int x, int y) {
        int[][] dist = bfs(self.getX(), self.getY());
        return dist[x][y];
    }


    private boolean overExtended() {
        for (Trooper trooper : teammates) {
            if (manhattanDist(self, trooper) >= 5) {
                return true;
            }
        }
        return false;
    }

    private void standStill() {
        move.setAction(MOVE);
        move.setDirection(Direction.CURRENT_POINT);
    }

    private boolean moveToNearestLongAgoSeenCell() {
        int minLastSeen = Integer.MAX_VALUE;
        int minDist = 0;
        int x = -1, y = -1;

        int[][] dist = bfs(self.getX(), self.getY());
        for (int i = 0; i < world.getWidth(); i++) {
            for (int j = 0; j < world.getHeight(); j++) {
                if (cells[i][j] == CellType.FREE && !isNarrowPathNearTheBorder(i, j) &&
                        (lastSeen[i][j] < minLastSeen || lastSeen[i][j] == minLastSeen && dist[i][j] < minDist)) {
                    minLastSeen = lastSeen[i][j];
                    minDist = dist[i][j];
                    x = i;
                    y = j;
                }
            }
        }
        return moveTo(x, y);
    }

    private boolean moveTo(Unit target) {
        return moveTo(target.getX(), target.getY());
    }

    private boolean moveTo(int x, int y) {
        if (x == self.getX() && y == self.getY()) {
            throw new RuntimeException();
        }
        int[][] dist = bfs(x, y);
        if (dist[self.getX()][self.getY()] == NOT_VISITED) {
            return false;
        }

        for (Direction dir : dirs) {
            int toX = self.getX() + dir.getOffsetX();
            int toY = self.getY() + dir.getOffsetY();
            if (!isGoodCell(toX, toY)) {
                continue;
            }
            if (dist[toX][toY] == dist[self.getX()][self.getY()] - 1) {
                moveTo(dir);
                return true;
            }
        }
        throw new RuntimeException();
    }

    @SuppressWarnings("unused")
    private void print(int[][] dist) {
        for (int j = 0; j < dist[0].length; j++) {
            for (int[] aDist : dist) {
                System.out.print(aDist[j] + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    private void moveTo(Direction dir) {
        move.setAction(MOVE);
        move.setDirection(dir);
    }

    private int[][] bfs(int startX, int startY) {
        Cell startCell = new Cell(startX, startY);
        int[][] dist = bfsCache.get(startCell);
        if (dist != null) {
            return dist;
        }
        Queue<Integer> qx = new ArrayDeque<>();
        Queue<Integer> qy = new ArrayDeque<>();
        dist = createIntMap(NOT_VISITED);
        qx.add(startX);
        qy.add(startY);
        dist[startX][startY] = 0;
        while (!qx.isEmpty()) {
            int x = qx.poll();
            int y = qy.poll();
            for (Direction dir : dirs) {
                int toX = x + dir.getOffsetX();
                int toY = y + dir.getOffsetY();
                if (!inField(toX, toY)) {
                    continue;
                }
                if (dist[toX][toY] != NOT_VISITED) {
                    continue;
                }
                dist[toX][toY] = dist[x][y] + 1;
                if (!isGoodCell(toX, toY)) {
                    continue;
                }
                qx.add(toX);
                qy.add(toY);
            }
        }
        bfsCache.put(startCell, dist);
        return dist;
    }

    private int[][] createIntMap(int fillValue) {
        int[][] r = new int[world.getWidth()][world.getHeight()];
        for (int[] aR : r) {
            Arrays.fill(aR, fillValue);
        }
        return r;
    }

    private int getMoveCost(Trooper self) {
        if (self.getStance() == KNEELING) {
            return game.getKneelingMoveCost();
        }
        if (self.getStance() == PRONE) {
            return game.getProneMoveCost();
        }
        if (self.getStance() == STANDING) {
            return game.getStandingMoveCost();
        }
        throw new IllegalArgumentException();
    }

    private int manhattanDist(int x, int y, int x1, int y1) {
        return Math.abs(x - x1) + Math.abs(y - y1);
    }

    private boolean isValidMove(Direction dir, Trooper trooper) {
        int toX = trooper.getX() + dir.getOffsetX();
        int toY = trooper.getY() + dir.getOffsetY();

        return isGoodCell(toX, toY);
    }

    private boolean isValidMove(Direction dir) {
        return isValidMove(dir, self);
    }

    private boolean isGoodCell(int toX, int toY) {
        return inField(toX, toY) && cells[toX][toY] == CellType.FREE &&
                !occupiedByTrooper[toX][toY] && !isNarrowPathNearTheBorder(toX, toY);
    }

    private boolean isNarrowPathNearTheBorder(int toX, int toY) {
        return toX == 0 && cells[toX + 1][toY] != CellType.FREE ||
                toX == world.getWidth() - 1 && cells[toX - 1][toY] != CellType.FREE ||
                toY == 0 && cells[toX][toY + 1] != CellType.FREE ||
                toY == world.getHeight() - 1 && cells[toX][toY - 1] != CellType.FREE;
    }

    private boolean inField(int toX, int toY) {
        return toX >= 0 && toY >= 0 && toX < world.getWidth() && toY < world.getHeight();
    }

    private void moveRandom() {
        ArrayList<Direction> a = new ArrayList<>();
        for (Direction dir : dirs) {
            if (isValidMove(dir)) {
                a.add(dir);
            }
        }
        move.setAction(MOVE);
        if (!a.isEmpty()) {
            move.setDirection(a.get(rnd.nextInt(a.size())));
        } else {
            move.setDirection(Direction.CURRENT_POINT);
        }
    }

    int followPriority(Trooper trooper) {
        if (trooper.getType() == FIELD_MEDIC) {
            return 0;
        }
        if (trooper.getType() == COMMANDER) {
            return 1;
        }
        if (trooper.getType() == SOLDIER) {
            return 2;
        }
        return -7; // >_<
    }

    boolean tryShoot() {
        Trooper target = getTargetEnemy();
        if (target == null) {
            return false;
        }

        TrooperStance minStance = getMinStanceCanStillShoot(target);

        List<ActionType> plan = new MaxDamagePlanComputer(
                self.getType(),
                self.getActionPoints(),
                self.getStance(), minStance,
                target.getHitpoints(),
                self.isHoldingFieldRation(),
                game
        ).getActions();

        if (plan.isEmpty()) {
            return false;
        }

        ActionType action = plan.get(0);
        if (action == SHOOT) {
            shoot(target);
        } else {
            move.setAction(action);
        }
        return true;
    }

    private TrooperStance getMinStanceCanStillShoot(Trooper target) {
        for (TrooperStance stance : TrooperStance.values()) {
            if (canShoot(target, stance)) {
                return stance;
            }
        }
        throw new RuntimeException();
    }

    private Trooper getTargetEnemy() {
        Trooper r = null;
        for (Trooper trooper : world.getTroopers()) {
            if (!trooper.isTeammate()) {
                if (canShoot(trooper)) {
                    if (r == null || trooper.getHitpoints() < r.getHitpoints()) {
                        r = trooper;
                    }
                }
            }
        }
        return r;
    }

    private void shoot(Trooper trooper) {
        move.setAction(SHOOT);
        setDirection(trooper.getX(), trooper.getY());
    }

    private boolean canShoot(Trooper target) {
        return canShoot(target, self.getStance());
    }

    private boolean canShoot(Trooper target, TrooperStance selfStance) {
        return world.isVisible(self.getShootingRange(), self.getX(), self.getY(), selfStance,
                target.getX(), target.getY(), target.getStance());
    }

    public ArrayList<Trooper> getTeammates() {
        ArrayList<Trooper> r = new ArrayList<>();
        for (Trooper trooper : world.getTroopers()) {
            if (trooper.isTeammate()) {
                r.add(trooper);
            }
        }
        return r;
    }

    public boolean[][] getOccupiedByTrooper() {
        boolean[][] r = new boolean[world.getWidth()][world.getHeight()];
        for (Trooper trooper : world.getTroopers()) {
            r[trooper.getX()][trooper.getY()] = true;
        }
        return r;
    }

    boolean canThrowGrenade(Trooper trooper) {
        return canThrowGrenade(trooper.getX(), trooper.getY());
    }

    private boolean canThrowGrenade(int x, int y) {
        return Utils.sqrDist(self.getX(), self.getY(), x, y) <= Utils.sqr(game.getGrenadeThrowRange());
    }

    public void setDirection(Trooper trooper) {
        setDirection(trooper.getX(), trooper.getY());
    }

    private void setDirection(int x, int y) {
        move.setX(x);
        move.setY(y);
    }
}
