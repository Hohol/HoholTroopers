import model.*;

import static model.ActionType.*;
import static model.TrooperStance.*;
import static model.TrooperType.*;

import java.util.*;

public final class MyStrategy implements Strategy {
    final Random rnd = new Random(3222);
    final static Direction[] dirs = {Direction.WEST, Direction.SOUTH, Direction.EAST, Direction.NORTH};
    final static int UNREACHABLE = 666;
    Trooper self;
    World world;
    Game game;
    Move move;
    CellType[][] cells;
    boolean[][] occupiedByTrooper;
    static int[][] lastSeen;
    static final boolean local = System.getProperty("ONLINE_JUDGE") == null;

    List<Trooper> teammates, enemies;
    Trooper teammateToFollow;
    static int smallMoveIndex;
    final static int FIRST_ROUND_INITIAL_TEAMMATE_COUNT = 3;

    static Map<Cell, int[][]> bfsCache = new HashMap<>(), bfsCacheAvoidNarrowPath = new HashMap<>();

    static EnumMap<TrooperType, List<Cell>> positionHistory = new EnumMap<>(TrooperType.class);

    static {
        for (TrooperType type : TrooperType.values()) {
            positionHistory.put(type, new ArrayList<Cell>());
        }
    }

    Utils utils;

    @Override
    public void move(Trooper self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;

        init();

        if (tryMoveByScript()) {
            return;
        }

        if (tryHeal()) {
            return;
        }

        if (tryThrowGrenade()) {
            return;
        }

        if (tryShoot()) {
            return;
        }

        if (tryHelpTeammateInFight()) {
            return;
        }

        if (tryMoveToBonus()) {
            return;
        }

        if (tryMove()) {
            return;
        }

        if (tryDeblock()) {
            return;
        }

        move.setAction(END_TURN);
    }

    static String script = "";
    static int scriptPos;

    private boolean tryMoveByScript() { //for debug only
        if (!local) {
            return false;
        }
        if (scriptPos >= script.length()) {
            return false;
        } else {
            char command = script.charAt(scriptPos);
            scriptPos++;
            if (command == '.') {
                move.setAction(END_TURN);
            } else if (command == 'g') {
                move.setAction(THROW_GRENADE);
                move.setDirection(Direction.CURRENT_POINT);
            } else {
                move.setAction(MOVE);
                move.setDirection(getDirection(command));
            }
        }
        return true;
    }

    private Direction getDirection(char command) {
        for (Direction dir : dirs) {
            if (Character.toLowerCase(dir.toString().charAt(0)) == command) {
                return dir;
            }
        }
        return null;
    }

    private boolean tryHelpTeammateInFight() {
        if (self.getType() == FIELD_MEDIC) {
            return false;
        }
        if (someEnemyInShootingRange()) {
            return false;
        }
        Trooper target = getLeastHpEnemySomeOfTeammatesCanShoot();
        if (target == null) {
            return false;
        }
        if (self.getStance() != STANDING && haveTime(game.getStanceChangeCost())) {
            move.setAction(RAISE_STANCE);
            return true;
        }
        if (!haveTime(getMoveCost(self))) {
            return false;
        }
        return moveToNearestCellFromWhichCanShoot(target);
    }

    private boolean someEnemyInShootingRange() {
        for (Trooper trooper : enemies) {
            if (canShoot(trooper)) {
                return true;
            }
        }
        return false;
    }

    private boolean moveToNearestCellFromWhichCanShoot(Trooper target) {
        int minDist = Integer.MAX_VALUE;
        int x = -1, y = -1;

        int[][] dist = bfs(self.getX(), self.getY(), false);
        for (int i = 0; i < world.getWidth(); i++) {
            for (int j = 0; j < world.getHeight(); j++) {
                if (world.isVisible(
                        self.getShootingRange(), i, j, STANDING,
                        target.getX(), target.getY(), target.getStance()
                ) && dist[i][j] < minDist && isGoodCell(i, j)) {
                    minDist = dist[i][j];
                    x = i;
                    y = j;
                }
            }
        }
        if (minDist >= 7) {
            return false;
        }
        return moveTo(x, y, false);
    }

    private Trooper getLeastHpEnemySomeOfTeammatesCanShoot() {
        Trooper r = null;
        for (Trooper trooper : enemies) {
            if (someTeammateCanShoot(trooper)) {
                if (r == null || trooper.getHitpoints() < r.getHitpoints()) {
                    r = trooper;
                }
            }
        }
        return r;
    }

    private boolean someTeammateCanShoot(Trooper target) {
        return numberOfTeammatesWhoCanShoot(target) > 0;
    }

    private int numberOfTeammatesWhoCanShoot(Trooper target) {
        int cnt = 0;
        for (Trooper trooper : teammates) {
            if (canShoot(trooper, target)) {
                cnt++;
            }
        }
        return cnt;
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
        return moveTo(bonus, true);
    }

    private Bonus chooseBonus() {
        int[][] dist = bfs(self.getX(), self.getY(), true);
        Bonus r = null;
        for (Bonus bonus : world.getBonuses()) {
            if (isHoldingBonus(bonus.getType())) {
                continue;
            }
            if (tooFarFromTeammates(bonus)) {
                continue;
            }
            if (!isGoodCell(bonus.getX(), bonus.getY()) || isNarrowPathNearBorder(bonus.getX(), bonus.getY())) {
                continue;
            }
            int d = dist[bonus.getX()][bonus.getY()];
            if (!haveTime(getActionsToMoveIn(d, self.getStance()))) {
                continue;
            }
            if (r == null || d < dist[r.getX()][r.getY()]) {
                r = bonus;
            }
        }
        return r;
    }

    private int getActionsToMoveIn(int dist, TrooperStance stance) {
        return actionsToStandUp(stance) + dist * game.getStandingMoveCost();
    }

    private int getActionsToMoveNear(int dist, TrooperStance stance) {
        if (dist <= 1) {
            return 0;
        }
        return actionsToStandUp(stance) + (dist - 1) * game.getStandingMoveCost();
    }

    private int actionsToStandUp(TrooperStance stance) {
        return (STANDING.ordinal() - stance.ordinal()) * game.getStanceChangeCost();
    }


    private boolean tooFarFromTeammates(Unit unit) {
        return tooFarFromTeammates(unit.getX(), unit.getY());
    }

    private boolean tooFarFromTeammates(int x, int y) {
        for (Trooper trooper : teammates) {
            if (manhattanDist(x, y, trooper.getX(), trooper.getY()) >= 6) {
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
        if (self.getType() != FIELD_MEDIC) {
            return oldTryHeal();
        }

        if (teammates.size() == 1) {
            return false;
        }

        char[][] map = createSimpleMap();
        print("Simple map for healing", map);

        boolean seeSomeEnemy = enemies.size() > 0;

        boolean holdingAndShouldUseFieldRation = self.isHoldingFieldRation() && seeSomeEnemy;
        boolean holdingAndShouldUseMedikit = self.isHoldingMedikit() && seeSomeEnemy;

        List<MyAction> actions = new MaxHealingPlanComputer(
                self.getActionPoints(),
                map,
                getHpByOrdinal(),
                holdingAndShouldUseFieldRation,
                holdingAndShouldUseMedikit,
                utils
        ).getActions();

        if (actions.isEmpty()) {
            return false;
        }
        Move bestMove = actions.get(0).getMove();
        move.setAction(bestMove.getAction());
        move.setDirection(bestMove.getDirection());
        move.setX(bestMove.getX());
        move.setY(bestMove.getY());
        return true;
    }

    private boolean oldTryHeal() {    //todo rework
        if (!self.isHoldingMedikit()) {
            return false;
        }
        if (teammates.size() == 1) {
            return false;
        }
        Trooper target = getTeammateToHeal();
        if (target == null) {
            return false;
        }
        if (manhattanDist(self, target) <= 1) {
            return heal(target);
        } else {
            return self.getStance() == STANDING &&
                    haveTime(getMoveCost(self)) &&
                    moveTo(target, false);
        }
    }

    private void print(String msg, char[][] map) {
        if (!local) {
            return;
        }
        System.out.println(msg);
        for (int j = 0; j < map[0].length; j++) {
            for (char[] aMap : map) {
                System.out.print(aMap[j]);
            }
            System.out.println();
        }
        System.out.println();
    }

    private int[] getHpByOrdinal() {
        int[] hp = new int[TrooperType.values().length];
        for (Trooper trooper : teammates) {
            hp[trooper.getType().ordinal()] = trooper.getHitpoints();
        }
        return hp;
    }

    private boolean tooCurvedPathTo(Unit target, boolean avoidNarrowPathNearBorder) {
        return tooCurvedPathTo(target.getX(), target.getY(), avoidNarrowPathNearBorder);
    }

    private boolean medikitOverheals(Trooper target) {
        return target.getMaximalHitpoints() - target.getHitpoints() < medikitHealValue(target);
    }

    private boolean heal(Trooper target) {
        if (self.isHoldingMedikit() &&
                haveTime(game.getMedikitUseCost()) &&
                !medikitOverheals(target)
                ) {
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

    private Trooper getTeammateToHeal() { //todo separate methods for medic and not
        Trooper r = null;
        int minDist = Integer.MAX_VALUE;
        for (Trooper target : teammates) {
            if (self.getType() != FIELD_MEDIC && medikitOverheals(target)) {
                continue;
            }
            if (target.getHitpoints() >= target.getMaximalHitpoints()) {
                continue;
            }
            if (tooCurvedPathTo(target, false)) {
                continue;
            }
            int dist = distTo(target, false);

            if (self.getType() != FIELD_MEDIC) {
                int actionsToMoveNearAndHeal = getActionsToMoveNear(dist, self.getStance()) + game.getMedikitUseCost();
                if (!haveTime(actionsToMoveNearAndHeal)) {
                    continue;
                }
            }
            if (dist == 0) {
                dist = 1; // doesn't matter if heal self or teammate near
            }
            if (r == null || dist < minDist || dist == minDist && target.getHitpoints() < r.getHitpoints()) {
                minDist = dist;
                r = target;
            }
        }
        return r;
    }

    private boolean tryDeblock() {
        if (!haveTime(getMoveCost(self))) {
            return false;
        }
        if (isBlockedByMe(teammateToFollow)) {
            int canMakeMoveCnt = self.getActionPoints() / getMoveCost(self);
            if (canMakeMoveCnt % 2 == 0) {
                move.setAction(MOVE);
                move.setDirection(Direction.CURRENT_POINT);
            } else {
                moveRandom();
            }
            return true;
        }
        return false;
    }

    private boolean isBlockedByMe(Trooper teammateToFollow) {
        return distTo(teammateToFollow.getX(), teammateToFollow.getY(), true) != UNREACHABLE &&
                isBlocked(teammateToFollow);
    }

    private boolean isBlocked(Trooper trooper) {
        int[][] dist = bfs(trooper.getX(), trooper.getY(), true);
        int cnt = 0;
        for (int[] aDist : dist) {
            for (int j = 0; j < aDist.length; j++) {
                if (dist[j][j] != UNREACHABLE) {
                    cnt++;
                }
            }
        }
        return cnt < 10;
    }

    private boolean tryThrowGrenade() {
        if (!haveTime(game.getGrenadeThrowCost())) {
            return false;
        }
        if (!self.isHoldingGrenade()) {
            return false;
        }
        for (Trooper trooper : enemies) {
            if (canThrowGrenade(trooper)) {
                throwGrenade(trooper);
                return true;
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
        bfsCacheAvoidNarrowPath.clear();
        smallMoveIndex++;
        log("SmallStepNumber = " + smallMoveIndex);
        cells = world.getCells();
        teammates = getTeammates();
        enemies = getEnemies();
        teammateToFollow = getTeammateToFollow();
        occupiedByTrooper = getOccupiedByTrooper();
        if (lastSeen == null) {
            lastSeen = createIntMap(0);
        }
        if (utils == null) {
            utils = new Utils(game, new TrooperParameters.TrooperParametersImpl(teammates));
        }
        updateLastSeen();
        updatePositionHistory();
        verifyDamage();
        printMap();
    }

    private void updatePositionHistory() {
        positionHistory.get(self.getType()).add(new Cell(self.getX(), self.getY()));
    }

    void verifyDamage() {
        if (utils.getShootDamage(self.getType(), self.getStance()) != self.getDamage()) {
            throw new RuntimeException();
        }
    }

    private List<Trooper> getEnemies() {
        List<Trooper> enemies = new ArrayList<>();
        for (Trooper trooper : world.getTroopers()) {
            if (!trooper.isTeammate()) {
                enemies.add(trooper);
            }
        }
        Collections.sort(enemies, new Comparator<Trooper>() {
            @Override
            public int compare(Trooper o1, Trooper o2) {
                return Long.compare(o1.getId(), o2.getId());
            }
        });
        return enemies;
    }

    private void log(Object o) {
        if (!local) {
            return;
        }
        System.out.println(o);
    }

    char[][] createSimpleMap() {
        char[][] map = new char[world.getWidth()][world.getHeight()];
        for (int i = 0; i < world.getWidth(); i++) {
            for (int j = 0; j < world.getHeight(); j++) {
                if (cells[i][j] != CellType.FREE) {
                    map[i][j] = '#';
                } else {
                    map[i][j] = '.';
                }
            }
        }
        for (Trooper trooper : teammates) {
            map[trooper.getX()][trooper.getY()] = Utils.getCharForTrooperType(trooper.getType());
        }
        return map;
    }

    @SuppressWarnings("unused")
    private void printMap() {
        if (!local) {
            return;
        }
        char[][] map = new char[world.getWidth()][world.getHeight()];
        for (char[] row : map) {
            Arrays.fill(row, '?');
        }
        for (int i = 0; i < world.getWidth(); i++) {
            for (int j = 0; j < world.getHeight(); j++) {
                if (cells[i][j] != CellType.FREE) {
                    map[i][j] = '#';
                }
            }
        }
        for (Trooper trooper : teammates) {
            for (int i = 0; i < world.getWidth(); i++) {
                for (int j = 0; j < world.getHeight(); j++) {
                    if (world.isVisible(trooper.getVisionRange(), trooper.getX(), trooper.getY(), trooper.getStance(),
                            i, j, STANDING)) {
                        map[i][j] = '.';
                    }
                }
            }
        }
        for (Trooper trooper : teammates) {
            map[trooper.getX()][trooper.getY()] = Utils.getCharForTrooperType(trooper.getType());
        }
        for (Trooper trooper : enemies) {
            map[trooper.getX()][trooper.getY()] = Character.toLowerCase(Utils.getCharForTrooperType(trooper.getType()));
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
                        lastSeen[i][j] = smallMoveIndex;
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
        for (Trooper trooper : teammates) {
            if (r == null || followPriority(trooper) > followPriority(r)) {
                r = trooper;
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
                return false;
            }
            return moveToNearestLongAgoSeenCell();
        } else {
            Trooper toFollow = teammateToFollow;

            boolean fullTeam = (teammates.size() >= FIRST_ROUND_INITIAL_TEAMMATE_COUNT);

            if (fullTeam && tooCurvedPathTo(teammateToFollow, false)) {
                toFollow = getOtherTeammate();
            }

            if (self.getType() == FIELD_MEDIC && fullTeam) {
                if (moveBehind(toFollow)) {
                    return true;
                }
            } else {
                if (moveAtSide(toFollow)) {
                    return true;
                }
            }
            if (manhattanDist(self, toFollow) == 1) {
                return false;
            }
            return moveTo(toFollow, false);
        }
    }

    private boolean tooCurvedPathTo(int x, int y, boolean avoidNarrowPathNearBorder) {
        return distTo(x, y, avoidNarrowPathNearBorder) - manhattanDist(x, y) >= 7;
    }

    private int manhattanDist(int x, int y) {
        return manhattanDist(self.getX(), self.getY(), x, y);
    }

    private boolean moveAtSide(Trooper trooper) {
        Cell sidePos = getNearestSidePosition(trooper);
        return moveNearToTeammate(sidePos);
    }

    private Cell getNearestSidePosition(Trooper trooper) {
        Cell behindPos = getBehindPosition(trooper);
        if (behindPos == null) {
            return null;
        }
        int behD = -1;
        for (int d = 0; d < 4; d++) {
            Direction dir = dirs[d];
            if (trooper.getX() + dir.getOffsetX() == behindPos.x &&
                    trooper.getY() + dir.getOffsetY() == behindPos.y) {
                behD = d;
                break;
            }
        }
        Cell r = null;
        int minDist = Integer.MAX_VALUE;
        for (int shift = 1; shift <= 3; shift += 2) {
            Direction dir = dirs[(behD + shift) % 4];
            Cell to = new Cell(trooper.getX() + dir.getOffsetX(), trooper.getY() + dir.getOffsetY());
            if (!inField(to.x, to.y)) {
                continue;
            }
            if (cells[to.x][to.y] != CellType.FREE) {
                continue;
            }
            int dist = distTo(to.x, to.y, false);
            if (dist < minDist) {
                minDist = dist;
                r = to;
            }
        }
        return r;
    }


    private boolean moveBehind(Trooper trooper) {
        Cell behindPos = getBehindPosition(trooper);
        return moveNearToTeammate(behindPos);
    }

    private boolean moveNearToTeammate(Cell pos) {
        if (pos == null) {
            return false;
        }
        if (self.getX() == pos.x && self.getY() == pos.y) {
            return false;
        }

        if (tooCurvedPathTo(pos.x, pos.y, false)) {
            return false;
        }

        if (!isGoodCell(pos.x, pos.y) && manhattanDist(self.getX(), self.getY(), pos.x, pos.y) == 1) {
            move.setAction(END_TURN);
            return true;
        }

        return moveTo(pos.x, pos.y, false);
    }

    private Cell getBehindPosition(Trooper trooper) {
        List<Cell> history = positionHistory.get(trooper.getType());
        if (history.isEmpty()) {
            return null;
        }
        Cell lastPosition = new Cell(trooper.getX(), trooper.getY());
        for (int i = history.size() - 1; i >= 0; i--) {
            Cell pos = history.get(i);
            if (!pos.equals(lastPosition)) {
                return pos;
            }
        }
        return null;
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

    private int distTo(Unit target, boolean avoidNarrowPathNearBorder) {
        return distTo(target.getX(), target.getY(), avoidNarrowPathNearBorder);
    }

    private int distTo(int x, int y, boolean avoidNarrowPathNearBorder) {
        int[][] dist = bfs(self.getX(), self.getY(), avoidNarrowPathNearBorder);
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

    private boolean moveToNearestLongAgoSeenCell() {
        int minLastSeen = Integer.MAX_VALUE;
        int minDist = Integer.MAX_VALUE;
        int x = -1, y = -1;

        int[][] dist = bfs(self.getX(), self.getY(), true);
        for (int i = 0; i < world.getWidth(); i++) {
            for (int j = 0; j < world.getHeight(); j++) {
                if (cells[i][j] == CellType.FREE && !isNarrowPathNearBorder(i, j) &&
                        (lastSeen[i][j] < minLastSeen || lastSeen[i][j] == minLastSeen && dist[i][j] < minDist)) {
                    minLastSeen = lastSeen[i][j];
                    minDist = dist[i][j];
                    x = i;
                    y = j;
                }
            }
        }
        return moveTo(x, y, true);
    }

    private boolean moveTo(Unit target, boolean avoidNarrowPathNearBorder) {
        return moveTo(target.getX(), target.getY(), avoidNarrowPathNearBorder);
    }

    private boolean moveTo(int x, int y, boolean avoidNarrowPathNearBorder) {
        if (x == self.getX() && y == self.getY()) {
            throw new RuntimeException();
        }
        int[][] dist = bfs(x, y, avoidNarrowPathNearBorder);
        if (dist[self.getX()][self.getY()] == UNREACHABLE) {
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

    private int[][] bfs(int startX, int startY, boolean avoidNarrowPathNearBorder) {
        Cell startCell = new Cell(startX, startY);
        Map<Cell, int[][]> cache = avoidNarrowPathNearBorder ? bfsCacheAvoidNarrowPath : bfsCache;
        int[][] dist = cache.get(startCell);
        if (dist != null) {
            return dist;
        }
        Queue<Integer> qx = new ArrayDeque<>();
        Queue<Integer> qy = new ArrayDeque<>();
        dist = createIntMap(UNREACHABLE);
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
                if (dist[toX][toY] != UNREACHABLE) {
                    continue;
                }
                dist[toX][toY] = dist[x][y] + 1;
                if (!isGoodCell(toX, toY) || avoidNarrowPathNearBorder && isNarrowPathNearBorder(toX, toY)) {
                    continue;
                }
                qx.add(toX);
                qy.add(toY);
            }
        }
        cache.put(startCell, dist);
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

        return isGoodCell(toX, toY) && !isNarrowPathNearBorder(toX, toY);
    }

    private boolean isValidMove(Direction dir) {
        return isValidMove(dir, self);
    }

    private boolean isGoodCell(int toX, int toY) {
        return inField(toX, toY) && cells[toX][toY] == CellType.FREE &&
                !occupiedByTrooper[toX][toY];
    }

    private boolean isNarrowPathNearBorder(int toX, int toY) {
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
        Trooper target = getEnemyToShoot();
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
                utils
        ).getActions();

        if (plan.isEmpty()) {
            move.setAction(END_TURN);
            return true;
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

    private Trooper getEnemyToShoot() {
        Trooper r = null;
        int maxCanShootCnt = 0;
        for (Trooper target : enemies) {
            if (canShoot(target)) {
                int canShootCnt = numberOfTeammatesWhoCanShoot(target);
                if (r == null || canShootCnt > maxCanShootCnt || canShootCnt == maxCanShootCnt && target.getHitpoints() < r.getHitpoints()) {
                    r = target;
                    maxCanShootCnt = canShootCnt;
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
        return canShoot(self, target);
    }

    private boolean canShoot(Trooper shooter, Trooper target) {
        return world.isVisible(shooter.getShootingRange(), shooter.getX(), shooter.getY(), shooter.getStance(),
                target.getX(), target.getY(), target.getStance());
    }

    private boolean canShoot(Trooper target, TrooperStance shooterStance) {
        return world.isVisible(self.getShootingRange(), self.getX(), self.getY(), shooterStance,
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

    @SuppressWarnings("unused")
    private Trooper getTrooper(String playerName, TrooperType type) { //for debug only
        long playerId = -1;
        for (Player player : world.getPlayers()) {
            if (player.getName().equalsIgnoreCase(playerName)) {
                playerId = player.getId();
                break;
            }
        }
        if (playerId == -1) {
            return null;
        }
        for (Trooper trooper : world.getTroopers()) {
            if (trooper.getPlayerId() == playerId && trooper.getType() == type) {
                return trooper;
            }
        }
        return null;
    }
}
