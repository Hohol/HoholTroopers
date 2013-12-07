import model.*;

import static model.ActionType.*;
import static model.TrooperStance.*;
import static model.TrooperType.*;

import java.util.*;

public final class MyStrategy implements Strategy {
    static final boolean local = System.getProperty("ONLINE_JUDGE") == null;
    //static final boolean local = false;

    public static final int MAX_DISTANCE_SHOULD_TRY_HELP = 6;
    private static final String MY_NAME = "Hohol";

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
    static List<MutableTrooper> enemies = new ArrayList<>();
    static int smallMoveIndex;
    static int mediumMoveIndex;

    static Map<Cell, int[][]> bfsCache = new HashMap<>(), bfsCacheAvoidNarrowPath = new HashMap<>();

    static Map<TrooperType, List<Cell>> positionHistory = new EnumMap<>(TrooperType.class);

    Trooper medic, sniper, soldier, commander, scout;
    static Map<TrooperType, List<Integer>> hpHistory = new EnumMap<>(TrooperType.class);
    static boolean[][][] wasSeenOnCurrentBigMove;
    static double[][][] wasSeenMinDist;
    static boolean[][][] damagedArea;
    boolean[][][] canSeeRightNow;  //todo it seems this array is not needed at all
    static List<Cell> suspiciousCells = new ArrayList<>();
    static Cell lastSeenEnemyPos, lastSeenEnemyPos2;
    static int prevScore;
    static boolean scoreMustChange;
    static List<MutableTrooper> enemiesDamagedOnPreviousMove = new ArrayList<>();
    static int expectedScoreChange;
    List<MyMove> prevActions = new ArrayList<>();
    static int previousTeammatesSize;
    static int lastTimeEnemyKnewWhereWeAre = -5;
    Cell3D startCell;
    static Map<Long, Set<TrooperType>> killedEnemies;

    static List<Cell> enemyKnowsHistoryCells = new ArrayList<>();
    static List<Integer> enemyKnowsHistoryTime = new ArrayList<>();
    Map<TrooperType, Integer> orderIndex = new EnumMap<>(TrooperType.class);
    static boolean wasGrenade;
    static BonusType[][] bonuses;
    static boolean[][] visited;
    static Map<TrooperType, Cell> lastSeenEnemyPosByType = new EnumMap<>(TrooperType.class);

    static {
        for (TrooperType type : TrooperType.values()) {
            positionHistory.put(type, new ArrayList<Cell>());
        }
    }

    static Utils utils;
    boolean phantoms;
    private static int initialTeamSize = -1;
    static String moveOrder = "";
    private List<Trooper> damagedTeammates;
    private static boolean firstSubmove = true;

    @Override
    public void move(Trooper self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;

        phantoms = enemies != null && enemies.size() > visibleRightNowEnemyCnt();

        init();
        moveInternal();
        finish();
    }

    void moveInternal() {
        if (tryMoveByScript()) {
            return;
        }

        //detectInvisibleShooters();
        //printSuspiciousCells();

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

    int visibleRightNowEnemyCnt() {
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
        for (Trooper ally : teammates) {
            List<Integer> history = hpHistory.get(ally.getType());
            if (history.size() < 2) {
                continue;
            }
            if (ally.getHitpoints() < history.get(history.size() - 2)) { //todo newHp != odlHp + previousMoveHealValue
                damagedTeammates.add(ally);
                enemyKnowsHistoryCells.add(new Cell(ally.getX(), ally.getY()));
                enemyKnowsHistoryTime.add(mediumMoveIndex);
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
        List<MyMove> actions = getTacticPlan(); //possibly changes lastTimeEnemyKnewWhereWeAre value
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
        return stopOn(moveIndex, type) && self.getActionPoints() <= actionsPoints;
    }

    private boolean shouldFightOrHeal() {
        if (seeSomeEnemy()) {
            return true;
        }
        if (!damagedTeammates.isEmpty()) {
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


    static boolean enemyKnowsWhereWeAre;

    private List<MyMove> getTacticPlan() {
        boolean healForbidden = (self.getType() == FIELD_MEDIC && teammates.size() == 1);
        boolean bonusUseForbidden = !seeSomeEnemy() && medicIsAlive();

        enemyKnowsWhereWeAre = checkEnemyKnowsWhereWeAre();

        TrooperType damagedTeammate = damagedTeammates.isEmpty() ? null : damagedTeammates.get(0).getType();
        TacticPlanComputer computer = new TacticPlanComputer(
                createCharMap(),
                utils,
                bonuses,
                vision,
                healForbidden,
                bonusUseForbidden,
                getTroopers2d(),
                teammatesWithoutSelf(),
                enemies,
                moveOrder,
                initialTeamSize,
                enemyKnowsWhereWeAre,
                new MutableTrooper(self, -1), //todo remove lastSeenTime from MutableTrooper
                prevActions,
                startCell,
                killedEnemies,
                getEnemyKnowsPositions(),
                mediumMoveIndex,
                damagedTeammate,
                getLastDamageTaken(damagedTeammate),
                lastSeenEnemyPos2,
                lastSeenEnemyPosByType,
                true
        );
        List<MyMove> r = computer.getPlan();
        if (computer.enemyWillKnowWhereWeAre()) {
            lastTimeEnemyKnewWhereWeAre = world.getMoveIndex();
            enemyKnowsWhereWeAre = true;
        }

        return r;
    }

    private int getLastDamageTaken(TrooperType damagedTeammate) {
        if (damagedTeammate == null) {
            return 0;
        }
        List<Integer> history = hpHistory.get(damagedTeammate);
        if (history.size() < 2) {
            return 0;
        }
        return history.get(history.size() - 2) - history.get(history.size() - 1);
    }

    private Set<Cell> getEnemyKnowsPositions() {
        Set<Cell> r = new HashSet<>();
        for (int i = enemyKnowsHistoryCells.size() - 1; i >= 0; i--) {
            Cell cell = enemyKnowsHistoryCells.get(i);
            int time = enemyKnowsHistoryTime.get(i);
            if (mediumMoveIndex - time >= initialTeamSize) {
                break;
            }
            r.add(cell);
        }
        return r;
    }

    private boolean checkEnemyKnowsWhereWeAre() {
        if (damagedTeammates.size() != 0 || teammates.size() != previousTeammatesSize) {
            lastTimeEnemyKnewWhereWeAre = world.getMoveIndex();
        }
        return world.getMoveIndex() - lastTimeEnemyKnewWhereWeAre <= 1;
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
                bonuses,
                getTroopers2d(),
                destination,
                prevActions,
                killedEnemies,
                mediumMoveIndex,
                initialTeamSize,
                true
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

    private boolean someoneNeeds(BonusType bonus) {
        if (!isHolding(self, bonus)) {
            return true;
        }
        for (Trooper ally : teammates) {
            if (!isHolding(ally, bonus)) {
                return true;
            }
        }
        return false;
    }

    private boolean isHolding(Trooper ally, BonusType bonus) {
        switch (bonus) {
            case GRENADE:
                return ally.isHoldingGrenade();
            case MEDIKIT:
                return ally.isHoldingMedikit();
            case FIELD_RATION:
                return ally.isHoldingFieldRation();
        }
        throw new RuntimeException();
    }

    private void init() {
        if (visited == null) {
            visited = new boolean[world.getWidth()][world.getHeight()];
        }
        visited[self.getX()][self.getY()] = true;
        if (bonuses == null) {
            bonuses = new BonusType[world.getWidth()][world.getHeight()];
        }
        if (killedEnemies == null) {
            killedEnemies = new HashMap<>();
            for (Player player : world.getPlayers()) {
                if (player.getName().equals(MY_NAME)) {
                    continue;
                }
                killedEnemies.put(player.getId(), new HashSet<TrooperType>());
            }
        }
        if (firstSubmove) {
            startCell = new Cell3D(self.getX(), self.getY(), self.getStance().ordinal());
            firstSubmove = false;
        }

        bfsCache.clear();
        bfsCacheAvoidNarrowPath.clear();
        smallMoveIndex++;
        Utils.log("BigStepNumber = " + world.getMoveIndex());
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
            orderIndex.put(self.getType(), mediumMoveIndex);
        } else {
            mediumMoveIndex = world.getMoveIndex() * initialTeamSize + orderIndex.get(self.getType());
        }
        occupiedByTrooper = getOccupiedByTrooper();

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
        damagedTeammates = getDamagedTeammates();
        updateBonuses();
        printHp();
        printMap();
    }

    private void updateBonuses() {
        boolean[][] seeBonusRightNow = new boolean[world.getWidth()][world.getHeight()];
        for (Bonus bonus : world.getBonuses()) {
            bonuses[bonus.getX()][bonus.getY()] = bonus.getType();
            seeBonusRightNow[bonus.getX()][bonus.getY()] = true;
        }
        for (int i = 0; i < world.getWidth(); i++) {
            for (int j = 0; j < world.getHeight(); j++) {
                if (canSeeRightNow[i][j][0] && !seeBonusRightNow[i][j]) {
                    bonuses[i][j] = null;
                }
            }
        }
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
        for (MutableTrooper enemy : enemies) {
            if (!TacticPlanComputer.isPhantom(enemy, mediumMoveIndex, moveOrder)) {
                lastSeenEnemyPos = new Cell(enemy.getX(), enemy.getY());
                lastSeenEnemyPos2 = lastSeenEnemyPos;
                lastSeenEnemyPosByType.put(enemy.getType(), lastSeenEnemyPos);
            }
        }
        prevScore = getMyScore();
        prevActions.add(MyMove.of(move));
        if (isLastSubMove()) {
            wasSeenOnCurrentBigMove = null;
            prevActions.clear();
            previousTeammatesSize = teammates.size();
            firstSubmove = true;
            mediumMoveIndex++;
        }
        dealDamage();
    }

    private void dealDamage() {
        updateDamagedArea();
        Iterator<MutableTrooper> it = enemies.iterator();
        scoreMustChange = false;
        enemiesDamagedOnPreviousMove.clear();
        expectedScoreChange = 0;
        wasGrenade = (move.getAction() == THROW_GRENADE);
        while (it.hasNext()) {
            MutableTrooper enemy = it.next();
            int oldHp = enemy.getHitpoints();
            int d = Utils.manhattanDist(enemy.getX(), enemy.getY(), move.getX(), move.getY());
            if (move.getAction() == THROW_GRENADE) {
                if (d == 0) {
                    enemy.decHp(game.getGrenadeDirectDamage());
                } else if (d == 1) {
                    enemy.decHp(game.getGrenadeCollateralDamage());
                }
            } else if (move.getAction() == SHOOT && d == 0) {
                enemy.decHp(Math.min(utils.getShootDamage(self.getType(), self.getStance()), enemy.getHitpoints()));
            }
            expectedScoreChange += (oldHp - enemy.getHitpoints()) * game.getTrooperDamageScoreFactor();
            if (enemy.getHitpoints() <= 0) {
                expectedScoreChange += game.getTrooperEliminationScore();
                markKilled(enemy);
                it.remove();
            } else {
                if (enemy.getHitpoints() != oldHp) {
                    scoreMustChange = true;
                    enemiesDamagedOnPreviousMove.add(enemy);
                }
                if (isLastSubMove()) {
                    enemy.setHp(Math.min(enemy.getHitpoints() + 50, Utils.INITIAL_TROOPER_HP));
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
        wasSeenMinDist = new double[world.getWidth()][world.getHeight()][Utils.NUMBER_OF_STANCES];

        for (int i = 0; i < world.getWidth(); i++) {
            for (int j = 0; j < world.getHeight(); j++) {
                for (TrooperStance targetStance : TrooperStance.values()) {
                    wasSeenMinDist[i][j][targetStance.ordinal()] = 1000;
                    for (Trooper ally : teammates) {
                        if (world.isVisible(ally.getVisionRange(), ally.getX(), ally.getY(), ally.getStance(), i, j, targetStance)) {
                            canSeeRightNow[i][j][targetStance.ordinal()] = true;
                            wasSeenOnCurrentBigMove[i][j][targetStance.ordinal()] = true;
                            wasSeenMinDist[i][j][targetStance.ordinal()] = Math.min(
                                    wasSeenMinDist[i][j][targetStance.ordinal()],
                                    Utils.dist(i, j, ally.getX(), ally.getY())
                            );
                            if (ally.getType() == SCOUT) {
                                wasSeenMinDist[i][j][targetStance.ordinal()] = 0; //hack!
                            }
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
            System.out.print(trooper.getType() + ": " + trooper.getHitpoints() + " hp");
            if (trooper.getId() == self.getId()) {
                System.out.print(" [self]");
            }
            System.out.println();
        }
        for (MutableTrooper trooper : enemies) {
            System.out.println("Enemy " + trooper.getType() + ": " + trooper.getHitpoints() + " hp");
        }
    }

    private void updateEnemies() {

        for (MutableTrooper mt : enemiesDamagedOnPreviousMove) {
            if (prevScore == getMyScore()) {
                if (!movePhantomEnemy(mt, true)) {
                    enemies.remove(mt);
                }
            } else {
                if (getMyScore() - prevScore != expectedScoreChange && !wasGrenade) {
                    enemies.remove(mt);
                    markKilled(mt);
                } else {
                    mt.updateLastSeenTime(mediumMoveIndex);
                }
            }
        }
        Set<MutableTrooper> seeRightNow = new HashSet<>();
        for (Trooper trooper : world.getTroopers()) {
            seeRightNow.add(new MutableTrooper(trooper, -1));
            unkill(trooper);   //we could previously mistakenly suppose him to be dead
        }
        Iterator<MutableTrooper> it = enemies.iterator();
        while (it.hasNext()) {
            MutableTrooper mt = it.next();
            if (expired(mt)) {
                it.remove();
                continue;
            }
            if (disappeared(seeRightNow, mt)) {
                if (!movePhantomEnemy(mt, false)) {
                    it.remove();
                }
            }
        }
        for (Trooper trooper : world.getTroopers()) {
            if (!trooper.isTeammate()) {
                MutableTrooper mt = new MutableTrooper(trooper, mediumMoveIndex);
                if (enemies.contains(mt)) {
                    enemies.remove(mt);
                }
                enemies.add(mt);
            }
        }
    }

    private void unkill(Trooper trooper) {
        Set<TrooperType> set = killedEnemies.get(trooper.getPlayerId());
        if (set == null) {
            return;
        }
        set.remove(trooper.getType());
    }

    private void markKilled(MutableTrooper mt) {
        Set<TrooperType> set = killedEnemies.get(mt.getPlayerId());
        if (set == null) {
            set = EnumSet.noneOf(TrooperType.class);
            killedEnemies.put(mt.getPlayerId(), set);
        }
        set.add(mt.getType());
    }

    private boolean disappeared(Set<MutableTrooper> seeRightNow, MutableTrooper mt) {
        if (seeRightNow.contains(mt)) {
            return false;
        }
        if (!canSeeRightNow[mt.getX()][mt.getY()][mt.getStance().ordinal()]) {
            return false;
        }
        double distWeCouldSeeHim = self.getVisionRange();
        if (mt.getType() == SNIPER) {
            if (mt.getStance() == PRONE) {
                distWeCouldSeeHim -= game.getSniperProneStealthBonus();
            } else if (mt.getStance() == KNEELING) {
                distWeCouldSeeHim -= game.getSniperKneelingStealthBonus();
            }
        }
        return wasSeenMinDist[mt.getX()][mt.getY()][mt.getStance().ordinal()] <= distWeCouldSeeHim + 1e-6;
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
        int expirationTime = finals() ? 5 : 2;
        return mediumMoveIndex - mt.getLastSeenTime() > expirationTime * initialTeamSize;
    }

    private boolean finals() {
        return initialTeamSize == 5;
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
            destination = chooseBonus();
            if (destination == null) {
                destination = getNearestLongAgoSeenCell();
            }
        }
        moveByPlan(getStrategyPlan(destination));
        return true;
    }

    private Cell chooseBonus() {
        int minDist = Integer.MAX_VALUE;
        Cell r = null;
        Trooper someone = teammates.get(0);
        for (int i = 0; i < world.getWidth(); i++) {
            for (int j = 0; j < world.getHeight(); j++) {
                if (visited[i][j]) {
                    continue;
                }
                if (bonuses[i][j] != null && someoneNeeds(bonuses[i][j])) {
                    int dist = Utils.manhattanDist(someone.getX(), someone.getY(), i, j);
                    if (dist < minDist) {
                        minDist = dist;
                        r = new Cell(i, j);
                    }
                }
            }
        }
        return r;
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