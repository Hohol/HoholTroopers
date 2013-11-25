import model.*;

import static model.ActionType.*;
import static model.TrooperStance.*;
import static model.TrooperType.*;

import java.util.*;

public final class MyStrategy implements Strategy {
    static final boolean local = System.getProperty("ONLINE_JUDGE") == null;

    public static final int MAX_DISTANCE_SHOULD_TRY_HELP = 6;

    final Random rnd = new Random(322);

    Trooper self;
    World world;
    Game game;
    Move move;
    CellType[][] cells;
    boolean[][] occupiedByTrooper;
    static int[][] lastSeen;

    static boolean[] vision;

    List<Trooper> teammates;
    static Set<MutableTrooper> enemies = new HashSet<>();
    static int smallMoveIndex;
    static int mediumMoveIndex;

    static Map<Cell, int[][]> bfsCache = new HashMap<>(), bfsCacheAvoidNarrowPath = new HashMap<>();

    static Map<TrooperType, List<Cell>> positionHistory = new EnumMap<>(TrooperType.class);

    Trooper medic, sniper, soldier, commander, scout;
    private BonusType[][] bonuses;
    static Map<TrooperType, List<Integer>> hpHistory = new EnumMap<>(TrooperType.class);
    static boolean[][][] wasSeenOnCurrentBigMove;
    static boolean[][][] damagedArea;
    boolean[][][] canSeeRightNow;  //todo it seems this array is not needed at all
    static List<Cell> suspiciousCells = new ArrayList<>();
    static Cell lastSeenEnemyPos;
    static int prevScore;
    static boolean scoreMustChange;
    static List<MutableTrooper> damageWasDealt = new ArrayList<>();
    static int expectedScoreChange;

    static {
        for (TrooperType type : TrooperType.values()) {
            positionHistory.put(type, new ArrayList<Cell>());
        }
    }

    static Utils utils;
    boolean phantoms;
    private static int initialTeamSize = -1;
    static String moveOrder = "";

    @Override
    public void move(Trooper self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;

        phantoms = enemies != null && enemies.size() > stupidEnemyCnt();

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

        if (tryFightOrHeal()) {
            return;
        }

        if (tryDealWithInvisibleShooters()) {
            return;
        }

        if (tryMove()) {
            return;
        }

        tryRandomShoot();
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
                        !wasSeenOnCurrentBigMove[i][j][self.getStance().ordinal()]) {
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
        Utils.log("Random shoot to (" + cell.x + ", " + cell.y + ")");
        return true;
    }

    private void printCells(String message, List<Cell> cells) {
        if (!local) {
            return;
        }
        Utils.log(message);
        if (cells.isEmpty()) {
            Utils.log("No cells");
            return;
        }
        char[][] map = getMapForPrinting();
        for (Cell cell : cells) {
            map[cell.x][cell.y] = '!';
        }
        print(map);
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
                if (wasSeenOnCurrentBigMove[i][j][PRONE.ordinal()]) {
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

    int stupidEnemyCnt() {
        int r = 0;
        for (Trooper trooper : world.getTroopers()) {
            if (!trooper.isTeammate()) {
                r++;
            }
        }
        return r;
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
        List<Trooper> damagedTeammates = getDamagedTeammates();
        for (Trooper ally : damagedTeammates) {
            findSuspiciousCells(ally);
            if (suspiciousCells.isEmpty()) { //todo dafuk? kiting?
                continue;
            }
            return;
        }
    }

    private List<Trooper> getDamagedTeammates() {
        List<Trooper> damagedTeammates = new ArrayList<>();
        for (Trooper trooper : teammates) {
            List<Integer> history = hpHistory.get(trooper.getType());
            if (history.size() < 2) {
                continue;
            }
            if (trooper.getHitpoints() < history.get(history.size() - 2)) { //todo newHp != odlHp + previousMoveHealValue
                damagedTeammates.add(trooper);
            }
        }
        return damagedTeammates;
    }

    private void removeVisibleCellsFromSuspicious() {
        for (int i = suspiciousCells.size() - 1; i >= 0; i--) {
            Cell c = suspiciousCells.get(i);
            if (wasSeenOnCurrentBigMove[c.x][c.y][PRONE.ordinal()]) {
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
                if (world.isVisible(utils.getShootRange(SOLDIER, STANDING.ordinal()), i, j, STANDING, trooper.getX(), trooper.getY(), trooper.getStance()) && //todo sniper has greater range
                        !wasSeenOnCurrentBigMove[i][j][PRONE.ordinal()]) {
                    suspiciousCells.add(new Cell(i, j));
                }
            }
        }
    }

    boolean seeSomeEnemy() {
        return enemies.size() > 0;
    }

    private boolean tryFightOrHeal() {
        if (!shouldFightOrHeal()) {
            return false;
        }
        List<MyMove> actions = getTacticPlan();
        moveByPlan(actions);
        return true;
    }

    private void moveByPlan(List<MyMove> actions) {
        Utils.log(self.getType() + " having " + self.getActionPoints() + " action points is going to " + actions);
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
    boolean stopOn(int moveIndex, TrooperType type) { //for debug only
        return world.getMoveIndex() >= moveIndex && self.getType() == type;
    }

    @SuppressWarnings("unused")
    boolean stopOn(int moveIndex, TrooperType type, int actionsPoints) {
        return stopOn(moveIndex, type) && self.getActionPoints() == actionsPoints;
    }

    private boolean shouldFightOrHeal() {
        if (seeSomeEnemy()) {
            return true;
        }
        if (self.getType() == FIELD_MEDIC && teammates.size() == 1) {
            return false;
        }
        return !allTeammatesFullHp() && someoneCanHeal();
    }

    private boolean someoneCanHeal() {
        if (medicIsAlive()) {
            return true;
        }
        for (Trooper ally : teammates) {
            if (ally.isHoldingMedikit()) {
                return true;
            }
        }
        return false;
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

    private List<MyMove> getTacticPlan() {
        boolean healForbidden = (self.getType() == FIELD_MEDIC && teammates.size() == 1);
        boolean bonusUseForbidden = !seeSomeEnemy() && medicIsAlive();

        return new TacticPlanComputer(
                createCharMap(),
                utils,
                bonuses,
                vision,
                healForbidden,
                bonusUseForbidden,
                getTroopers2d(),
                teammatesWithoutSelf(),
                new ArrayList<>(enemies),
                moveOrder,
                new MutableTrooper(self, -1) //todo remove lastSeenTime from MutableTrooper
        ).getPlan();
    }

    private boolean medicIsAlive() {
        return medic != null;
    }

    private List<MyMove> getStrategyPlan(Cell destination) {
        return new StrategyPlanComputer(
                createCharMap(),
                utils,
                teammatesWithoutSelf(),
                new MutableTrooper(self, -1),
                vision,
                getBonuses(),
                getTroopers2d(),
                destination
        ).getPlan();
    }

    private List<MutableTrooper> teammatesWithoutSelf() { //todo store in teammates MutableTroopers without self
        ArrayList<MutableTrooper> r = new ArrayList<>();
        for (Trooper ally : teammates) {
            if (ally.getId() != self.getId()) {
                r.add(new MutableTrooper(ally, -1));
            }
        }
        return r;
    }

    private MutableTrooper[][] getTroopers2d() {
        MutableTrooper[][] r = new MutableTrooper[world.getWidth()][world.getHeight()];
        for (Trooper ally : teammates) {
            r[ally.getX()][ally.getY()] = new MutableTrooper(ally, -1);
        }
        for (MutableTrooper enemy : enemies) {
            r[enemy.getX()][enemy.getY()] = enemy;
        }
        return r;
    }

    private BonusType[][] getBonuses() {
        BonusType[][] bonuses = new BonusType[world.getWidth()][world.getHeight()];
        for (Bonus bonus : world.getBonuses()) {
            bonuses[bonus.getX()][bonus.getY()] = bonus.getType();
        }
        return bonuses;
    }

    private void print(char[][] map) {
        if (!local) {
            return;
        }
        for (int j = 0; j < map[0].length; j++) {
            for (char[] aMap : map) {
                System.out.print(aMap[j]);
            }
            System.out.println();
        }
        System.out.println();
    }

    private boolean haveTime(int actionCost) {
        return self.getActionPoints() >= actionCost;
    }

    private void init() {
        bfsCache.clear();
        bfsCacheAvoidNarrowPath.clear();
        smallMoveIndex++;
        Utils.log("SmallStepNumber = " + smallMoveIndex);
        cells = world.getCells();
        teammates = getTeammates();
        if (initialTeamSize == -1) {
            initialTeamSize = teammates.size();
        }
        if (moveOrder.length() != initialTeamSize) {
            char ch = Utils.getCharForTrooperType(self.getType());
            if (moveOrder.indexOf(ch) == -1) {
                moveOrder += ch;
            }
        }
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
        updateVisibility();
        updateEnemies();
        if (lastSeenEnemyPos != null && wasSeenOnCurrentBigMove[lastSeenEnemyPos.x][lastSeenEnemyPos.y][PRONE.ordinal()] && Utils.manhattanDist(self.getX(), self.getY(), lastSeenEnemyPos.x, lastSeenEnemyPos.y) <= 2) {
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
        String name = "Hohol";
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
        prevScore = getMyScore();
        if (isLastSubMove()) {
            mediumMoveIndex++;
            wasSeenOnCurrentBigMove = null;
        }
        dealDamage();
    }

    private void dealDamage() {
        updateDamagedArea();
        Iterator<MutableTrooper> it = enemies.iterator();
        scoreMustChange = false;
        damageWasDealt.clear();
        expectedScoreChange = 0;
        while (it.hasNext()) {
            MutableTrooper trooper = it.next();
            int oldHp = trooper.getHitpoints();
            int d = Utils.manhattanDist(trooper.getX(), trooper.getY(), move.getX(), move.getY());
            if (move.getAction() == THROW_GRENADE) {
                if (d == 0) {
                    trooper.decHp(game.getGrenadeDirectDamage());
                } else if (d == 1) {
                    trooper.decHp(game.getGrenadeCollateralDamage());
                }
            } else if (move.getAction() == SHOOT && d == 0) {
                trooper.decHp(Math.min(utils.getShootDamage(self.getType(), self.getStance()), trooper.getHitpoints()));
            }
            expectedScoreChange += (oldHp - trooper.getHitpoints()) * game.getTrooperDamageScoreFactor();
            if (trooper.getHitpoints() <= 0) {
                expectedScoreChange += game.getTrooperEliminationScore();
                it.remove();
            } else {
                if (trooper.getHitpoints() != oldHp) {
                    scoreMustChange = true;
                    damageWasDealt.add(trooper);
                }
                if (isLastSubMove()) {
                    trooper.setHp(Math.min(trooper.getHitpoints() + 50, Utils.INITIAL_TROOPER_HP));
                }
            }
        }
    }

    void updateDamagedArea() {
        damagedArea = new boolean[world.getWidth()][world.getHeight()][Utils.NUMBER_OF_STANCES];
        for (int stance = 0; stance < 3; stance++) {
            if (move.getAction() == THROW_GRENADE) {
                if (inField(move.getX(), move.getY())) {
                    damagedArea[move.getX()][move.getY()][stance] = true;
                }
                for (Direction dir : Utils.dirs) {
                    int toX = move.getX() + dir.getOffsetX();
                    int toY = move.getY() + dir.getOffsetY();
                    if (inField(toX, toY)) {
                        damagedArea[toX][toY][stance] = true;
                    }
                }
            } else if (move.getAction() == SHOOT) {
                if (world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(), move.getX(), move.getY(), TrooperStance.values()[stance])) {
                    damagedArea[move.getX()][move.getY()][stance] = true;
                }
            }
        }

    }

    private boolean isLastSubMove() {
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

    private void updateVisibility() {
        if (wasSeenOnCurrentBigMove == null) {
            wasSeenOnCurrentBigMove = new boolean[world.getWidth()][world.getHeight()][Utils.NUMBER_OF_STANCES];
        }
        canSeeRightNow = new boolean[world.getWidth()][world.getHeight()][Utils.NUMBER_OF_STANCES];
        for (Trooper trooper : teammates) {
            for (int i = 0; i < world.getWidth(); i++) {
                for (int j = 0; j < world.getHeight(); j++) {
                    for (TrooperStance targetStance : TrooperStance.values()) {
                        if (world.isVisible(trooper.getVisionRange(), trooper.getX(), trooper.getY(), trooper.getStance(), i, j, targetStance)) {
                            canSeeRightNow[i][j][targetStance.ordinal()] = true;
                            wasSeenOnCurrentBigMove[i][j][targetStance.ordinal()] = true;
                        }
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

        for (MutableTrooper mt : damageWasDealt) {
            if (prevScore == getMyScore()) {
                if (!movePhantomEnemy(mt, true)) {
                    enemies.remove(mt);
                }
            } else {
                if (getMyScore() - prevScore > expectedScoreChange) {
                    enemies.remove(mt);
                } else {
                    mt.updateLastSeenTime(world.getMoveIndex());
                }
            }
        }
        Set<Long> seeRightNowIds = new HashSet<>();
        for (Trooper trooper : world.getTroopers()) {
            seeRightNowIds.add(trooper.getId());
        }
        Iterator<MutableTrooper> it = enemies.iterator();
        while (it.hasNext()) {
            MutableTrooper mt = it.next();
            if (expired(mt)) {
                it.remove();
                continue;
            }
            if (!seeRightNowIds.contains(mt.getId()) && canSeeRightNow[mt.getX()][mt.getY()][mt.getStance().ordinal()]) {
                if (!movePhantomEnemy(mt, false)) {
                    it.remove();
                }
            }
        }
        for (Trooper trooper : world.getTroopers()) {
            if (!trooper.isTeammate()) {
                MutableTrooper mt = new MutableTrooper(trooper, world.getMoveIndex());
                if (enemies.contains(mt)) {
                    enemies.remove(mt);
                }
                enemies.add(mt);
            }
        }
    }

    private boolean movePhantomEnemy(MutableTrooper mt, boolean reasonIsDamage) {
        int minD = Integer.MAX_VALUE;
        TrooperStance bestStance = null;
        int bestX = -1, bestY = -1;
        for (TrooperStance newStance : TrooperStance.values()) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int newX = mt.getX() + dx;
                    int newY = mt.getY() + dy;
                    if (!inField(newX, newY) || cells[newX][newY] != CellType.FREE) {
                        continue;
                    }
                    if (canSeeRightNow[newX][newY][newStance.ordinal()]) {
                        continue;
                    }
                    if (reasonIsDamage && damagedArea[newX][newY][newStance.ordinal()]) {
                        continue;
                    }
                    int d = Utils.manhattanDist(newX, newY, mt.getX(), mt.getY()) + Math.abs(newStance.ordinal() - mt.getStance().ordinal());
                    if (d < minD) {
                        minD = d;
                        bestStance = newStance;
                        bestX = newX;
                        bestY = newY;
                    }
                }
            }
        }
        if (bestStance != null) {
            mt.setStance(bestStance);
            mt.setX(bestX);
            mt.setY(bestY);
            return true;
        }
        return false;
    }

    private boolean expired(MutableTrooper mt) {
        return world.getMoveIndex() - mt.getLastSeenTime() > 2;
    }

    char[][] createCharMap() {
        char[][] map = new char[world.getWidth()][world.getHeight()];
        for (int i = 0; i < world.getWidth(); i++) {
            for (int j = 0; j < world.getHeight(); j++) {
                if (cells[i][j] != CellType.FREE) {
                    map[i][j] = '#';
                } else {
                    if (wasSeenOnCurrentBigMove[i][j][PRONE.ordinal()]) {
                        map[i][j] = '.';
                    } else {
                        map[i][j] = '?';
                    }
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
        System.out.println();
        print(map);
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

    private boolean tryMove() {
        Cell destination;
        if (lastSeenEnemyPos != null) {
            destination = lastSeenEnemyPos;
        } else {
            destination = getNearestLongAgoSeenCell();
        }
        moveByPlan(getStrategyPlan(destination));
        return true;
    }

    private boolean tooCurvedPathTo(int x, int y, boolean avoidNarrowPathNearBorder) {
        return distTo(x, y, avoidNarrowPathNearBorder) - manhattanDist(x, y) >= 7;
    }

    private int manhattanDist(int x, int y) {
        return Utils.manhattanDist(self.getX(), self.getY(), x, y);
    }

    private boolean allTeammatesFullHp() {
        for (Trooper trooper : teammates) {
            if (trooper.getHitpoints() < trooper.getMaximalHitpoints()) {
                return false;
            }
        }
        return true;
    }

    private int distTo(int x, int y, boolean avoidNarrowPathNearBorder) {
        int[][] dist = bfs(self.getX(), self.getY(), avoidNarrowPathNearBorder);
        return dist[x][y];
    }

    private Cell getNearestLongAgoSeenCell() {
        int minLastSeen = Integer.MAX_VALUE;
        int minDist = Integer.MAX_VALUE;
        int x = -1, y = -1;

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

    private boolean moveTo(int x, int y, boolean avoidNarrowPathNearBorder) {
        List<Direction> availableDirs = getFirstStepForMovingTo(x, y, avoidNarrowPathNearBorder);
        if (availableDirs.isEmpty()) {
            return false;
        }
        moveTo(availableDirs.get(0));
        return true;
    }

    private List<Direction> getFirstStepForMovingTo(int fromX, int fromY, int destX, int destY, boolean avoidNarrowPathNearBorder) {
        int[][] dist = bfs(destX, destY, avoidNarrowPathNearBorder);
        List<Direction> availableDirs = new ArrayList<>();
        if (dist[fromX][fromY] != Utils.UNREACHABLE) {
            for (Direction dir : Utils.dirs) {
                int toX = fromX + dir.getOffsetX();
                int toY = fromY + dir.getOffsetY();
                if (!isFreeCell(toX, toY)) {
                    continue;
                }
                if (dist[toX][toY] == dist[fromX][fromY] - 1) {
                    availableDirs.add(dir);
                }
            }
        }
        return availableDirs;
    }

    private List<Direction> getFirstStepForMovingTo(int toX, int toY, boolean avoidNarrowPathNearBorder) {
        return getFirstStepForMovingTo(self.getX(), self.getY(), toX, toY, avoidNarrowPathNearBorder);
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
        return r;
    }

    public boolean[][] getOccupiedByTrooper() {
        boolean[][] r = new boolean[world.getWidth()][world.getHeight()];
        for (Trooper trooper : world.getTroopers()) {
            r[trooper.getX()][trooper.getY()] = true;
        }
        return r;
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