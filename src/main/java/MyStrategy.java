import model.*;

import java.util.*;

public final class MyStrategy implements Strategy {
    final Random rnd = new Random(322);
    final static Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    final static int NOT_VISITED = 666;
    Trooper self;
    World world;
    Game game;
    Move move;
    CellType[][] cells;
    boolean[][] occupiedByTrooper;
    static int[][] lastSeen;

    ArrayList<Trooper> teammates;
    Trooper teammateToFollow;
    int stepNumber;

    @Override
    public void move(Trooper self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;

        init();

        if (tryMedicHeal()) {
            return;
        }

        if (tryHealSelf()) {
            return;
        }

        if (tryThrowGrenade()) {
            return;
        }

        if (tryShoot()) {
            return;
        }

        if (tryDeblock()) { //todo it is obviously bad
            return;
        }

        if (tryMove()) {
            return;
        }

        move.setAction(ActionType.END_TURN);
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
        move.setAction(ActionType.THROW_GRENADE);
        setDirection(trooper);
    }

    private boolean haveTime(int actionCost) {
        return self.getActionPoints() >= actionCost;
    }

    private void init() {
        stepNumber++;
        cells = world.getCells();
        teammates = getTeammates();
        teammateToFollow = getTeammateToFollow();
        occupiedByTrooper = getOccupiedByTrooper();
        if (lastSeen == null) {
            lastSeen = createIntMap(0);
        }
        updateLastSeen();
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
                            TrooperStance.PRONE
                    )) {
                        lastSeen[i][j] = stepNumber;
                    }
                }
            }
        }
        print(lastSeen);
    }

    private boolean tryMedicHeal() {
        if (self.getType() != TrooperType.FIELD_MEDIC) {
            return false;
        }
        if (!haveTime(game.getFieldMedicHealCost())) {
            return false;
        }
        Trooper target = null;
        int maxDiff = 0;
        for (Trooper trooper : teammates) {
            int diff = trooper.getMaximalHitpoints() - trooper.getHitpoints();
            if (diff > maxDiff) {
                maxDiff = diff;
                target = trooper;
            }
        }
        if (target == null) {
            return false;
        }
        if (manhattanDist(self, target) <= 1) {
            move.setAction(ActionType.HEAL);
            move.setX(target.getX());
            move.setY(target.getY());
        } else {
            if (!haveTime(getMoveCost(self))) {
                return false;
            }
            moveTo(target);
        }
        return true;
    }

    private int manhattanDist(Trooper self, Trooper target) {
        return manhattanDist(self.getX(), self.getY(), target.getX(), target.getY());
    }

    private boolean tryHealSelf() {
        if (game.getMedikitHealSelfBonusHitpoints() > self.getActionPoints()) {
            return false;
        }
        if (!self.isHoldingMedikit()) {
            return false;
        }
        move.setAction(ActionType.USE_MEDIKIT);
        return true;
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
        if (!haveTime(getMoveCost(self))) {
            return false;
        }

        if (self.getId() == teammateToFollow.getId()) {
            if(self.getActionPoints() <= 4) {
                standStill();
                return true;
            }
            moveToNearestLongAgoSeenCell();
            //moveRandom();
            //moveTo(world.getWidth()/2, world.getHeight()/2);
        } else {
            moveTo(teammateToFollow);
        }
        return true;
    }

    private void standStill() {
        move.setAction(ActionType.MOVE);
        move.setDirection(Direction.CURRENT_POINT);
    }

    private void moveToNearestLongAgoSeenCell() {
        int minLastSeen = Integer.MAX_VALUE;
        int minDist = 0;
        int x = -1, y = -1;

        int[][] dist = bfs(self.getX(), self.getY(), -1, -1);
        for (int i = 0; i < world.getWidth(); i++) {
            for (int j = 0; j < world.getHeight(); j++) {
                if (lastSeen[i][j] < minLastSeen || lastSeen[i][j] == minLastSeen && dist[i][j] < minDist) {
                    minLastSeen = lastSeen[i][j];
                    minDist = dist[i][j];
                    x = i;
                    y = j;
                }
            }
        }
        moveTo(x, y);
    }

    private void moveTo(Trooper target) {
        moveTo(target.getX(), target.getY());
    }

    private boolean moveTo(int x, int y) {
        if (x == self.getX() && y == self.getY()) {
            throw new RuntimeException();
        }
        if (manhattanDist(self, x, y) == 1) { //todo ?
            return false;
        }
        int[][] dist = bfs(x, y, self.getX(), self.getY());
        if (dist[self.getX()][self.getY()] == NOT_VISITED) {
            return false;
        }
        for (Direction dir : dirs) {
            int toX = self.getX() + dir.getOffsetX();
            int toY = self.getY() + dir.getOffsetY();
            if (!goodCell(toX, toY)) {
                continue;
            }
            if (dist[toX][toY] == dist[self.getX()][self.getY()] - 1) {
                moveTo(dir);
                return true;
            }
        }
        print(dist);
        throw new RuntimeException();
    }

    private void print(int[][] dist) {
        for (int j = 0; j < dist[0].length; j++) {
            for (int i = 0; i < dist.length; i++) {
                System.out.print(dist[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    private void moveTo(Direction dir) {
        move.setAction(ActionType.MOVE);
        move.setDirection(dir);
    }

    private int[][] bfs(int startX, int startY, int targetX, int targetY) {
        Queue<Integer> qx = new ArrayDeque<>();
        Queue<Integer> qy = new ArrayDeque<>();
        int[][] dist = createIntMap(NOT_VISITED);
        qx.add(startX);
        qy.add(startY);
        dist[startX][startY] = 0;
        while (!qx.isEmpty()) {
            int x = qx.poll();
            int y = qy.poll();
            if (x == targetX && y == targetY) {
                return dist;
            }
            for (Direction dir : dirs) {
                int toX = x + dir.getOffsetX();
                int toY = y + dir.getOffsetY();
                if (toX == targetX && toY == targetY) {
                    dist[toX][toY] = dist[x][y] + 1;
                    return dist;
                }
                if (!goodCell(toX, toY)) {
                    continue;
                }
                if (dist[toX][toY] != NOT_VISITED) {
                    continue;
                }
                qx.add(toX);
                qy.add(toY);
                dist[toX][toY] = dist[x][y] + 1;
            }
        }
        return dist;
    }

    private int[][] createIntMap(int fillValue) {
        int[][] r = new int[world.getWidth()][world.getHeight()];
        for (int i = 0; i < r.length; i++) {
            Arrays.fill(r[i], fillValue);
        }
        return r;
    }

    private int getMoveCost(Trooper self) {
        if (self.getStance() == TrooperStance.KNEELING) {
            return game.getKneelingMoveCost();
        }
        if (self.getStance() == TrooperStance.PRONE) {
            return game.getProneMoveCost();
        }
        if (self.getStance() == TrooperStance.STANDING) {
            return game.getStandingMoveCost();
        }
        throw new IllegalArgumentException();
    }


    private boolean isMoveTo(Direction dir, int x, int y) {
        if (!isValidMove(dir)) {
            return false;
        }
        return manhattanDist(x, y, self.getX() + dir.getOffsetX(), self.getY() + dir.getOffsetY()) <
                manhattanDist(x, y, self.getX(), self.getY());
    }

    private int manhattanDist(Trooper trooper, int x, int y) {
        return manhattanDist(trooper.getX(), trooper.getY(), x, y);
    }

    private int manhattanDist(int x, int y, int x1, int y1) {
        return Math.abs(x - x1) + Math.abs(y - y1);
    }

    private boolean isValidMove(Direction dir, Trooper trooper) {
        int toX = trooper.getX() + dir.getOffsetX();
        int toY = trooper.getY() + dir.getOffsetY();

        return goodCell(toX, toY);
    }

    private boolean isValidMove(Direction dir) {
        return isValidMove(dir, self);
    }

    private boolean goodCell(int toX, int toY) {
        return inField(toX, toY) && cells[toX][toY] == CellType.FREE && !occupiedByTrooper[toX][toY];
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
        move.setAction(ActionType.MOVE);
        if (!a.isEmpty()) {
            move.setDirection(a.get(rnd.nextInt(a.size())));
        } else {
            move.setDirection(Direction.CURRENT_POINT);
        }
    }

    int followPriority(Trooper trooper) {
        if (trooper.getType() == TrooperType.FIELD_MEDIC) {
            return 0;
        }
        if (trooper.getType() == TrooperType.COMMANDER) {
            return 1;
        }
        if (trooper.getType() == TrooperType.SOLDIER) {
            return 2;
        }
        return -7; // >_<
    }

    boolean tryShoot() {
        if (!haveTime(self.getShootCost())) {
            return false;
        }

        Trooper target = getTargetEnemy();
        if(target != null) {
            shoot(target);
            return true;
        }
        return false;
    }

    private Trooper getTargetEnemy() {
        Trooper r = null;
        for (Trooper trooper : world.getTroopers()) {
            if (!trooper.isTeammate()) {
                if (canShoot(trooper)) {
                    if(r == null || trooper.getHitpoints() < r.getHitpoints()) {
                        r = trooper;
                    }
                }
            }
        }
        return r;
    }

    private void shoot(Trooper trooper) {
        move.setAction(ActionType.SHOOT);
        move.setX(trooper.getX());
        move.setY(trooper.getY());
    }

    private boolean canShoot(Trooper trooper) {
        return world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(),
                trooper.getX(), trooper.getY(), trooper.getStance());
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
        for (int i = 0; i < world.getWidth(); i++) {
            r[i] = new boolean[world.getHeight()];
        }
        for (Trooper trooper : world.getTroopers()) {
            r[trooper.getX()][trooper.getY()] = true;
        }
        return r;
    }

    boolean canThrowGrenade(Trooper trooper) {
        return canThrowGrenade(trooper.getX(), trooper.getY());
    }

    private boolean canThrowGrenade(int x, int y) {
        return sqrDist(self.getX(), self.getY(), x, y) <= sqr(game.getGrenadeThrowRange());
    }

    private int sqrDist(int x, int y, int x1, int y1) {
        return sqr(x - x1) + sqr(y - y1);
    }

    private int sqr(int x) {
        return x * x;
    }

    private double sqr(double x) {
        return x * x;
    }

    public void setDirection(Trooper direction) {
        setDirection(direction.getX(), direction.getY());
    }

    private void setDirection(int x, int y) {
        move.setX(x);
        move.setY(y);
    }
}

/*
grenade reachability pattern
######
#####
#####
#####
####
#

карта 20 на 30

*/