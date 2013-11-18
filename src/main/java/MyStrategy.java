import model.*;

import static model.ActionType.*;
import static model.TrooperStance.*;
import static model.TrooperType.*;

import java.util.*;

public final class MyStrategy implements Strategy {
    public final static int HP_TO_TRY_ESCAPE = 50;
    public static final int MAX_DISTANCE_MEDIC_SHOULD_HEAL = 6;
    public static final int MAX_DISTANCE_SHOULD_TRY_HELP = 6;

    final Random rnd = new Random(3222);

    Trooper self;
    World world;
    Game game;
    Move move;
    CellType[][] cells;
    boolean[][] occupiedByTrooper;
    static int[][] lastSeen;
    static final boolean local = System.getProperty("ONLINE_JUDGE") == null;
    static boolean[] vision;

    List<Trooper> teammates;
    Set<MutableTrooper> enemies;
    Trooper teammateToFollow;
    static int smallMoveIndex;
    final static int FIRST_ROUND_INITIAL_TEAMMATE_COUNT = 3;

    static Map<Cell, int[][]> bfsCache = new HashMap<>(), bfsCacheAvoidNarrowPath = new HashMap<>();

    static Map<TrooperType, List<Cell>> positionHistory = new EnumMap<>(TrooperType.class);

    Trooper medic, sniper, soldier, commander, scout;
    private BonusType[][] bonuses;
    static Map<TrooperType, List<Integer>> hpHistory = new EnumMap<>(TrooperType.class);
    static boolean[][] wasSeenOnCurrentBigMove;
    static List<Cell> suspiciousCells = new ArrayList<>();
    static Cell lastSeenEnemyPos;
    static int prevScore, curScore;
    static boolean wasRandomShoot;

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
        moveInternal();
        finish();
    }

    void moveInternal() {
        if (tryMoveByScript()) {
            return;
        }

        detectInvisibleShooters();
        printSuspiciousCells();

        if (tryEverything()) {
            return;
        }

        if (tryDealWithInvisibleShooters()) {
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
        if (tryRandomShoot()) {
            return;
        }
    }

    private boolean tryRandomShoot() {
        if (!haveTime(self.getShootCost())) {
            return false;
        }
        if (self.getVisionRange() >= self.getShootingRange()) {
            return false;
        }
        List<Cell> cells = new ArrayList<>();
        for (int i = 0; i < world.getWidth(); i++) {
            for (int j = 0; j < world.getHeight(); j++) {
                if (!isFreeCell(i, j)) {
                    continue;
                }
                if (world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(), i, j, STANDING) &&
                        !wasSeenOnCurrentBigMove[i][j]) {
                    cells.add(new Cell(i, j));
                }
            }
        }
        printCells("Random shoot cells", cells);
        if (cells.isEmpty()) {
            return false;
        }
        Cell cell = cells.get(rnd.nextInt(cells.size()));
        move.setAction(SHOOT);
        setDirection(cell.x, cell.y);
        wasRandomShoot = true;
        log("Random shoot to (" + cell.x + ", " + cell.y + ")");
        return true;
    }

    private void printCells(String message, List<Cell> cells) {
        if (!local) {
            return;
        }
        if (cells.isEmpty()) {
            log("No cells");
            return;
        }
        char[][] map = getMapForPrinting();
        for (Cell cell : cells) {
            map[cell.x][cell.y] = '!';
        }
        print(message, map);
    }

    private void printSuspiciousCells() {
        printCells("Suspicious cells", suspiciousCells);

    }

    private char[][] getMapForPrinting() {
        char[][] map = new char[world.getWidth()][world.getHeight()];
        for (char[] row : map) {
            Arrays.fill(row, '?');
        }
        for (int i = 0; i < wasSeenOnCurrentBigMove.length; i++) {
            for (int j = 0; j < wasSeenOnCurrentBigMove[i].length; j++) {
                if (wasSeenOnCurrentBigMove[i][j]) {
                    map[i][j] = '.';
                }
            }
        }
        for (Trooper trooper : teammates) {
            map[trooper.getX()][trooper.getY()] = Utils.getCharForTrooperType(trooper.getType());
        }
        for (MutableTrooper trooper : enemies) {
            map[trooper.getX()][trooper.getY()] = Character.toLowerCase(Utils.getCharForTrooperType(trooper.getType()));
        }
        for (int i = 0; i < world.getWidth(); i++) {
            for (int j = 0; j < world.getHeight(); j++) {
                if (cells[i][j] != CellType.FREE) {
                    map[i][j] = '#';
                }
            }
        }
        return map;
    }

    private boolean tryDealWithInvisibleShooters() {
        if (suspiciousCells.isEmpty()) {
            return false;
        }
        if (self.getStance() != STANDING) {
            if (!haveTime(game.getStanceChangeCost())) {
                return false;
            }
            move.setAction(RAISE_STANCE);
            return true;
        }
        Cell cell = null;
        for (Cell c : suspiciousCells) {
            if (!tooCurvedPathTo(c.x, c.y, false)) {
                cell = c;
                break;
            }
        }
        if (cell == null) {
            return false;
        }
        if (!haveTime(game.getStandingMoveCost())) {
            return false;
        }
        return moveTo(cell.x, cell.y, true);
    }

    private void detectInvisibleShooters() {
        if (seeSomeEnemy()) {
            suspiciousCells.clear();
            return;
        }

        removeVisibleCellsFromSuspicious();

        if (!suspiciousCells.isEmpty()) {
            return;
        }

        for (Trooper trooper : world.getTroopers()) { // world.getTroopers() to include me
            if (!trooper.isTeammate()) {
                continue;
            }
            List<Integer> history = hpHistory.get(trooper.getType());
            if (history.size() < 2) {
                continue;
            }
            if (trooper.getHitpoints() < history.get(history.size() - 2)) { //todo newHp != odlHp + previousMoveHealValue
                findSuspiciousCells(trooper);
                if (suspiciousCells.isEmpty()) { //todo dafuk? kiting?
                    continue;
                }
                return;
            }
        }
    }

    private void removeVisibleCellsFromSuspicious() {
        for (int i = suspiciousCells.size() - 1; i >= 0; i--) {
            Cell c = suspiciousCells.get(i);
            if (wasSeenOnCurrentBigMove[c.x][c.y]) {
                suspiciousCells.remove(i);
            }
        }
    }

    private void findSuspiciousCells(Trooper trooper) {
        suspiciousCells.clear();
        for (int i = 0; i < world.getWidth(); i++) {
            for (int j = 0; j < world.getHeight(); j++) {
                if (!isFreeCell(i, j)) {
                    continue;
                }
                if (world.isVisible(utils.getShootRange(SOLDIER), i, j, STANDING, trooper.getX(), trooper.getY(), trooper.getStance()) && //todo sniper has greater range
                        !wasSeenOnCurrentBigMove[i][j]) {
                    suspiciousCells.add(new Cell(i, j));
                }
            }
        }
    }

    boolean seeSomeEnemy() {
        return enemies.size() > 0;
    }

    private boolean tryEverything() {

        if (!shouldTrySomething()) {
            return false;
        }

        List<MyMove> actions = getPlan().actions;
        moveByPlan(actions);
        return true;
    }

    private boolean shouldTrySomething() {
        if (seeSomeEnemy()) {
            return true;
        }
        if (self.getType() == FIELD_MEDIC && teammates.size() == 1) {
            return false;
        }
        return self.getType() == FIELD_MEDIC && !allTeammatesFullHp();
    }

    private void moveByPlan(List<MyMove> actions) {
        if (actions.isEmpty()) {
            move.setAction(END_TURN);
            return;
        }
        Move bestMove = actions.get(0).getMove();
        move.setAction(bestMove.getAction());
        move.setDirection(bestMove.getDirection());
        move.setX(bestMove.getX());
        move.setY(bestMove.getY());
    }

    @SuppressWarnings("unused")
    boolean interesting(List<MyMove> actions) { // for debug only
        int x = self.getX();
        int y = self.getY();
        for (MyMove move : actions) {
            if (move.getMove().getAction() == MOVE) {
                x += move.getMove().getDirection().getOffsetX();
                y += move.getMove().getDirection().getOffsetY();
                if (bonuses[x][y] != null && bonuses[x][y] != BonusType.MEDIKIT) {
                    return true;
                }
            }
        }
        return false;
    }

    private TrooperStance[][] getStances() {
        TrooperStance[][] stances = new TrooperStance[world.getWidth()][world.getHeight()];
        for (MutableTrooper trooper : enemies) {
            stances[trooper.getX()][trooper.getY()] = trooper.getStance();
        }
        for (Trooper trooper : teammates) {
            stances[trooper.getX()][trooper.getY()] = trooper.getStance();
        }
        return stances;
    }

    //m s c
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
        for (Direction dir : Utils.dirs) {
            if (Character.toLowerCase(dir.toString().charAt(0)) == command) {
                return dir;
            }
        }
        return null;
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
            if (bonusTooFarFromTeammates(bonus)) {
                continue;
            }
            if (!isFreeCell(bonus.getX(), bonus.getY()) || isNarrowPathNearBorder(bonus.getX(), bonus.getY())) {
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


    private boolean bonusTooFarFromTeammates(Unit unit) {
        return bonusTooFarFromTeammates(unit.getX(), unit.getY());
    }

    private boolean bonusTooFarFromTeammates(int x, int y) {
        for (Trooper trooper : teammates) {
            if (Utils.manhattanDist(x, y, trooper.getX(), trooper.getY()) >= 7) {
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

    private State getPlan() {
        boolean holdingAndShouldUseFieldRation = self.isHoldingFieldRation() && seeSomeEnemy();
        boolean holdingAndShouldUseMedikit = self.isHoldingMedikit() && seeSomeEnemy();
        boolean holdingAndShouldUseGrenade = self.isHoldingGrenade() && seeSomeEnemy();

        return new PlanComputer(
                createCharMap(),
                utils,
                getHpArray(),
                bonuses,
                getStances(),
                vision,
                new State(
                        self.getActionPoints(),
                        self.getHitpoints(),
                        self.getX(),
                        self.getY(),
                        self.getStance(),
                        holdingAndShouldUseFieldRation,
                        holdingAndShouldUseGrenade,
                        holdingAndShouldUseMedikit
                )
        ).getPlan();
    }

    private int[][] getHpArray() {
        int[][] hp = new int[world.getWidth()][world.getHeight()];
        for (Trooper trooper : teammates) {
            hp[trooper.getX()][trooper.getY()] = trooper.getHitpoints();
        }
        for (MutableTrooper trooper : enemies) {
            hp[trooper.getX()][trooper.getY()] = trooper.getHitpoints();
        }
        return hp;
    }

    private BonusType[][] getBonuses() {
        BonusType[][] bonuses = new BonusType[world.getWidth()][world.getHeight()];
        for (Bonus bonus : world.getBonuses()) {
            bonuses[bonus.getX()][bonus.getY()] = bonus.getType();
        }
        return bonuses;
    }

    private boolean oldTryHeal() {    //todo rework
        if (!self.isHoldingMedikit()) {
            return false;
        }
        if (teammates.size() == 1) {
            return false;
        }
        if (enemies.size() == 0) {
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
        return distTo(teammateToFollow.getX(), teammateToFollow.getY(), true) != Utils.UNREACHABLE &&
                isBlocked(teammateToFollow);
    }

    private boolean isBlocked(Trooper trooper) {
        int[][] dist = bfs(trooper.getX(), trooper.getY(), true);
        int cnt = 0;
        for (int i = 0; i < dist.length; i++) {
            for (int j = 0; j < dist[i].length; j++) {
                if (dist[i][j] != Utils.UNREACHABLE && isFreeCell(i, j)) {
                    cnt++;
                }
            }
        }
        return cnt < 50;
    }

    private boolean haveTime(int actionCost) {
        return self.getActionPoints() >= actionCost;
    }

    private void init() {
        curScore = getMyScore();
        if (wasRandomShoot && curScore != prevScore) {
            log("Random shoot just hit someone!");
        }
        wasRandomShoot = false;
        bfsCache.clear();
        bfsCacheAvoidNarrowPath.clear();
        smallMoveIndex++;
        log("SmallStepNumber = " + smallMoveIndex);
        cells = world.getCells();
        teammates = getTeammates();
        updateEnemies();
        teammateToFollow = getTeammateToFollow();
        occupiedByTrooper = getOccupiedByTrooper();
        bonuses = getBonuses();

        if (lastSeen == null) {
            lastSeen = createIntMap(0);
        }
        if (utils == null) {
            utils = new Utils(game, new TrooperParameters.TrooperParametersImpl(teammates));
        }
        if (vision == null) {
            vision = world.getCellVisibilities();
        }
        updateWeCanSee();
        if (lastSeenEnemyPos != null && wasSeenOnCurrentBigMove[lastSeenEnemyPos.x][lastSeenEnemyPos.y] && Utils.manhattanDist(self.getX(), self.getY(), lastSeenEnemyPos.x, lastSeenEnemyPos.y) <= 2) {
            lastSeenEnemyPos = null;
        }
        updateHpHistory();
        updateLastSeen();
        updatePositionHistory();
        verifyDamage();
        printHp();
        printMap();
    }

    private int getMyScore() {
        String name = local ? "MyStrategy" : "Hohol";
        //String name = "Hohol"; //for repeater
        for (Player player : world.getPlayers()) {
            if (player.getName().equals(name)) {
                return player.getScore();
            }
        }
        throw new RuntimeException();
    }

    private void finish() {
        if (!enemies.isEmpty()) {
            MutableTrooper t = enemies.iterator().next();
            lastSeenEnemyPos = new Cell(t.getX(), t.getY());
        }
        prevScore = curScore;
        if (lastSubMove()) {
            enemies.clear();
            wasSeenOnCurrentBigMove = null;
        }
        dealDamageToEnemies();
    }

    private void dealDamageToEnemies() {
        Iterator<MutableTrooper> it = enemies.iterator();
        while (it.hasNext()) {
            MutableTrooper trooper = it.next();
            if (move.getAction() == THROW_GRENADE) {
                int d = Utils.manhattanDist(trooper.getX(), trooper.getY(), move.getX(), move.getY());
                if (d == 0) {
                    trooper.decHp(game.getGrenadeDirectDamage());
                } else if (d == 1) {
                    trooper.decHp(game.getGrenadeCollateralDamage());
                }
            } else if (move.getAction() == SHOOT) {
                trooper.decHp(utils.getShootDamage(self.getType(), self.getStance()));
            }
            if (trooper.getHitpoints() <= 0) {
                it.remove();
            }
        }
    }

    private boolean lastSubMove() {
        int actionCost = 0;
        switch (move.getAction()) {
            case END_TURN:
                return true;
            case MOVE:
                actionCost = utils.getMoveCost(self.getStance());
                break;
            case SHOOT:
                actionCost = self.getShootCost();
                break;
            case RAISE_STANCE:
            case LOWER_STANCE:
                actionCost = game.getStanceChangeCost();
                break;
            case THROW_GRENADE:
                actionCost = game.getGrenadeThrowCost();
                break;
            case USE_MEDIKIT:
                actionCost = game.getMedikitUseCost();
                break;
            case EAT_FIELD_RATION:
                return false;
            case HEAL:
                actionCost = game.getFieldMedicHealCost();
                break;
            case REQUEST_ENEMY_DISPOSITION:
                actionCost = game.getCommanderRequestEnemyDispositionCost();
                break;
        }
        return actionCost == self.getActionPoints();
    }

    private void updateWeCanSee() {
        if (wasSeenOnCurrentBigMove == null) {
            wasSeenOnCurrentBigMove = new boolean[world.getWidth()][world.getHeight()];
        }
        for (Trooper trooper : teammates) {
            for (int i = 0; i < world.getWidth(); i++) {
                for (int j = 0; j < world.getHeight(); j++) {
                    if (world.isVisible(trooper.getVisionRange(), trooper.getX(), trooper.getY(), trooper.getStance(), i, j, PRONE)) {
                        wasSeenOnCurrentBigMove[i][j] = true;
                    }
                }
            }
        }
    }

    private void updateHpHistory() {
        if (hpHistory.isEmpty()) {
            for (TrooperType trooperType : TrooperType.values()) {
                hpHistory.put(trooperType, new ArrayList<Integer>());
            }
        }
        for (Trooper trooper : teammates) {
            hpHistory.get(trooper.getType()).add(trooper.getHitpoints());
        }
    }

    private void printHp() {
        if (!local) {
            return;
        }
        for (Trooper trooper : teammates) {
            System.out.println(trooper.getType() + ": " + trooper.getHitpoints() + " hp");
        }
        for (MutableTrooper trooper : enemies) {
            System.out.println("Enemy " + trooper.getType() + ": " + trooper.getHitpoints() + " hp");
        }
    }

    private void updatePositionHistory() {
        positionHistory.get(self.getType()).add(new Cell(self.getX(), self.getY()));
    }

    void verifyDamage() {
        if (utils.getShootDamage(self.getType(), self.getStance()) != self.getDamage()) {
            throw new RuntimeException();
        }
    }

    private void updateEnemies() {
        if (enemies == null) {
            enemies = new HashSet<>();
        }
        for (Trooper trooper : world.getTroopers()) {
            if (!trooper.isTeammate()) {
                MutableTrooper mt = new MutableTrooper(trooper);
                if (enemies.contains(mt)) {
                    enemies.remove(mt);
                }
                enemies.add(new MutableTrooper(trooper));
            }
        }
    }

    private static void log(Object o) {
        if (!local) {
            return;
        }
        System.out.println(o);
    }

    char[][] createCharMap() {
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
        for (MutableTrooper trooper : enemies) {
            map[trooper.getX()][trooper.getY()] = Character.toLowerCase(Utils.getCharForTrooperType(trooper.getType()));
        }
        return map;
    }

    @SuppressWarnings("unused")
    private void printMap() {
        if (!local) {
            return;
        }
        char[][] map = getMapForPrinting();
        print("Map ", map);
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
        return Utils.manhattanDist(self.getX(), self.getY(), target.getX(), target.getY());
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
            if (self.getActionPoints() <= 4 && world.getMoveIndex() >= 1 || someoneNotFullHpAndMedicCanHealHim()) {
                return false;
            }
            if (lastSeenEnemyPos != null) {
                return cautiouslyMoveTo(lastSeenEnemyPos.x, lastSeenEnemyPos.y);
            }
            Cell r = getNearestLongAgoSeenCell();
            return cautiouslyMoveTo(r.x, r.y);
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

    private boolean someoneNotFullHpAndMedicCanHealHim() {
        if (medic == null) {
            return false;
        }
        int[][] dist = bfs(medic.getX(), medic.getY(), false);
        for (Trooper trooper : teammates) {
            if (trooper.getHitpoints() < trooper.getMaximalHitpoints() &&
                    dist[trooper.getX()][trooper.getY()] <= MAX_DISTANCE_MEDIC_SHOULD_HEAL) {
                return true;
            }
        }
        return false;
    }

    private boolean tooCurvedPathTo(int x, int y, boolean avoidNarrowPathNearBorder) {
        return distTo(x, y, avoidNarrowPathNearBorder) - manhattanDist(x, y) >= 7;
    }

    private int manhattanDist(int x, int y) {
        return Utils.manhattanDist(self.getX(), self.getY(), x, y);
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
            Direction dir = Utils.dirs[d];
            if (trooper.getX() + dir.getOffsetX() == behindPos.x &&
                    trooper.getY() + dir.getOffsetY() == behindPos.y) {
                behD = d;
                break;
            }
        }
        Cell r = null;
        int minDist = Integer.MAX_VALUE;
        for (int shift = 1; shift <= 3; shift += 2) {
            Direction dir = Utils.dirs[(behD + shift) % 4];
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

        if (!isFreeCell(pos.x, pos.y) && Utils.manhattanDist(self.getX(), self.getY(), pos.x, pos.y) == 1) {
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

    private boolean allTeammatesFullHp() {
        for (Trooper trooper : teammates) {
            if (trooper.getHitpoints() < trooper.getMaximalHitpoints()) {
                return false;
            }
        }
        return true;
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

    private Cell getNearestLongAgoSeenCell() {
        int minLastSeen = Integer.MAX_VALUE;
        int minDist = Integer.MAX_VALUE;
        int x = -1, y = -1;

        //int[][] dist = bfs(self.getX(), self.getY(), true);
        for (int i = 0; i < world.getWidth(); i++) {
            for (int j = 0; j < world.getHeight(); j++) {
                int newDist = Utils.manhattanDist(i, j, world.getWidth() / 2, world.getHeight() / 2);
                if (cells[i][j] == CellType.FREE && !isNarrowPathNearBorder(i, j) &&
                        (lastSeen[i][j] < minLastSeen || lastSeen[i][j] == minLastSeen &&
                                 newDist < minDist
                        )) {
                    minLastSeen = lastSeen[i][j];
                    minDist = newDist;
                    x = i;
                    y = j;
                }
            }
        }
        return new Cell(x, y);
    }

    private boolean cautiouslyMoveTo(int x, int y) {
        List<Direction> availableDirs = getFirstStepForMovingTo(x, y, true);
        Direction bestDir = null;
        int minDist = Integer.MAX_VALUE;
        for (Direction dir : availableDirs) {
            int toX = self.getX() + dir.getOffsetX();
            int toY = self.getY() + dir.getOffsetY();
            int maxDist = 0;
            for (Trooper trooper : teammates) {
                int dist = Utils.manhattanDist(toX, toY, trooper.getX(), trooper.getY());
                maxDist = Math.max(maxDist, dist);
            }
            if (maxDist < minDist) {
                minDist = maxDist;
                bestDir = dir;
            }
        }
        if (minDist >= 5) {
            return false;
        }
        if (bestDir == null) {
            return false;
        }
        moveTo(bestDir);
        return true;
    }

    private boolean moveTo(Unit target, boolean avoidNarrowPathNearBorder) {
        return moveTo(target.getX(), target.getY(), avoidNarrowPathNearBorder);
    }

    private boolean moveTo(int x, int y, boolean avoidNarrowPathNearBorder) {
        List<Direction> availableDirs = getFirstStepForMovingTo(x, y, avoidNarrowPathNearBorder);
        if (availableDirs.isEmpty()) {
            return false;
        }
        moveTo(availableDirs.get(0));
        return true;
    }

    private List<Direction> getFirstStepForMovingTo(int x, int y, boolean avoidNarrowPathNearBorder) {
        if (x == self.getX() && y == self.getY()) {
            throw new RuntimeException();
        }
        int[][] dist = bfs(x, y, avoidNarrowPathNearBorder);
        List<Direction> availableDirs = new ArrayList<>();
        if (dist[self.getX()][self.getY()] != Utils.UNREACHABLE) {
            for (Direction dir : Utils.dirs) {
                int toX = self.getX() + dir.getOffsetX();
                int toY = self.getY() + dir.getOffsetY();
                if (!isFreeCell(toX, toY)) {
                    continue;
                }
                if (dist[toX][toY] == dist[self.getX()][self.getY()] - 1) {
                    availableDirs.add(dir);
                }
            }
        }
        return availableDirs;
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
        if (dist == null) {
            dist = bfsInternal(startX, startY, avoidNarrowPathNearBorder);
            cache.put(startCell, dist);
        }
        return dist;
    }

    private int[][] bfsInternal(int startX, int startY, boolean avoidNarrowPathNearBorder) {
        int[][] dist;
        Queue<Integer> qx = new ArrayDeque<>();
        Queue<Integer> qy = new ArrayDeque<>();
        dist = createIntMap(Utils.UNREACHABLE);
        qx.add(startX);
        qy.add(startY);
        dist[startX][startY] = 0;
        while (!qx.isEmpty()) {
            int x = qx.poll();
            int y = qy.poll();
            for (Direction dir : Utils.dirs) {
                int toX = x + dir.getOffsetX();
                int toY = y + dir.getOffsetY();
                if (!inField(toX, toY)) {
                    continue;
                }
                if (dist[toX][toY] != Utils.UNREACHABLE) {
                    continue;
                }
                dist[toX][toY] = dist[x][y] + 1;
                if (!isFreeCell(toX, toY) || avoidNarrowPathNearBorder && isNarrowPathNearBorder(toX, toY)) {
                    continue;
                }
                qx.add(toX);
                qy.add(toY);
            }
        }
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
        return utils.getMoveCost(self.getStance());
    }

    private boolean isValidMove(Direction dir, Trooper trooper) {
        int toX = trooper.getX() + dir.getOffsetX();
        int toY = trooper.getY() + dir.getOffsetY();

        return isFreeCell(toX, toY) && !isNarrowPathNearBorder(toX, toY);
    }

    private boolean isValidMove(Direction dir) {
        return isValidMove(dir, self);
    }

    private boolean isFreeCell(int toX, int toY) {
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
        for (Direction dir : Utils.dirs) {
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

        commander = null;
        medic = null;
        soldier = null;
        sniper = null;
        scout = null;

        ArrayList<Trooper> r = new ArrayList<>();
        for (Trooper trooper : world.getTroopers()) {
            if (trooper.isTeammate()) {
                r.add(trooper);
                switch (trooper.getType()) {
                    case COMMANDER:
                        commander = trooper;
                        break;
                    case FIELD_MEDIC:
                        medic = trooper;
                        break;
                    case SOLDIER:
                        soldier = trooper;
                        break;
                    case SNIPER:
                        sniper = trooper;
                        break;
                    case SCOUT:
                        scout = trooper;
                        break;
                }
            }
        }
        Collections.sort(r, new Comparator<Trooper>() {
            @Override
            public int compare(Trooper o1, Trooper o2) {
                return Long.compare(o1.getId(), o2.getId());
            }
        });
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
