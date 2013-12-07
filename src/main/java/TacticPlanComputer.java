import model.BonusType;
import model.TrooperStance;

import static model.TrooperType.*;

import model.TrooperType;

import java.util.*;

import static model.TrooperStance.*;

public class TacticPlanComputer extends AbstractPlanComputer<TacticState> {

    protected static final int MAX_DIST_MEDIC_SHOULD_TRY_HEAL = 7;
    private static final double INVISIBLE_DAMAGE_QUOTIENT = 0.5;
    private final List<MutableTrooper> enemies;
    private List<MutableTrooper> enemiesWithImaginary;
    private int[][] sqrDistSum;
    List<int[][]> distToTeammatesForHealing;
    List<int[][]> enemyBfs;

    private int[][][] helpFactor;
    private int[][][] helpDist;
    private int[][][] minStanceForHelp;
    private int[][][] maxStanceForHelp; //has meaning only for sniper
    private int[] numberOfTeammatesWhoCanShoot;
    private int[][] numberOfTeammatesWhoCanReachEnemy;
    private int[][] numberOfTeammatesMedicCanReach;
    boolean[] enemyIsAlive;
    int[][] enemyIndex;
    private boolean healForbidden;
    private boolean bonusUseForbidden;
    DamageAndAP[][][][] maxDamageEnemyCanDeal;
    boolean[][] canDamageIfBefore = new boolean[Utils.NUMBER_OF_TROOPER_TYPES][Utils.NUMBER_OF_TROOPER_TYPES];
    boolean[][] canDamageIfAfter = new boolean[Utils.NUMBER_OF_TROOPER_TYPES][Utils.NUMBER_OF_TROOPER_TYPES];
    boolean enemyInitiallyKnowsWhereWeAre;
    int[][][][] actionsEnemyMustSpendToHide;
    boolean[][][][] visibleByEnemy;
    int[][][] apEnemyNeedsToSeeMe;
    int initialTeamSize;
    Set<Cell> enemyKnowsPosition;
    int mediumMoveIndex;
    String moveOrder;
    MutableTrooper investigationResult;
    TrooperType damagedTeammateType;
    Cell lastSeenEnemyPos;
    private int damageDealtToTeammate;

    public TacticPlanComputer(
            char[][] map,
            Utils utils,
            BonusType[][] bonuses,
            boolean[] visibilities,
            boolean healForbidden,
            boolean bonusUseForbidden,
            MutableTrooper[][] troopers,
            List<MutableTrooper> teammates,
            List<MutableTrooper> enemies,
            String moveOrder,
            boolean enemyKnowsWhereWeAre,
            MutableTrooper self,
            List<MyMove> prevActions,
            Cell3D startCell,
            Map<Long, Set<TrooperType>> killedEnemies,
            Set<Cell> enemyKnowsPosition,
            int mediumMoveIndex,
            TrooperType damagedTeammateType,
            int damageDealtToTeammate,
            Cell lastSeenEnemyPos,
            boolean mapIsStatic
    ) {
        super(map, utils, teammates, visibilities, bonuses, troopers, self, mapIsStatic, prevActions, killedEnemies);
        this.cur = new TacticState(self);
        this.healForbidden = healForbidden; //todo it is hack. Actually exist situations where even alone medic should heal himself
        this.bonusUseForbidden = bonusUseForbidden;
        this.enemies = enemies;
        this.enemyInitiallyKnowsWhereWeAre = enemyKnowsWhereWeAre;
        this.enemyKnowsPosition = enemyKnowsPosition;
        initialTeamSize = moveOrder.length();
        this.mediumMoveIndex = mediumMoveIndex;
        this.moveOrder = moveOrder;
        this.damagedTeammateType = damagedTeammateType;
        this.lastSeenEnemyPos = lastSeenEnemyPos;
        this.damageDealtToTeammate = damageDealtToTeammate;
        enemyIndex = new int[n][m];
        for (int i = 0; i < enemies.size(); i++) {
            MutableTrooper enemy = enemies.get(i);
            enemyIndex[enemy.getX()][enemy.getY()] = i;
        }

        if (checkEnemyInitiallyKnowsWhereWeAre(startCell)) {
            enemyInitiallyKnowsWhereWeAre = true;
        }

        canDamageIfBefore = getCanDamageIfBefore(moveOrder, self.getType());
        canDamageIfAfter = getCanDamageIfAfter(moveOrder, self.getType());
    }

    private boolean checkEnemyInitiallyKnowsWhereWeAre(Cell3D startCell) {
        boolean know = false;
        for (MutableTrooper enemy : enemies) {
            if (canSee(enemy, startCell.x, startCell.y, startCell.stance)) {
                know = true;
            }
        }
        return know;
    }

    public boolean enemyWillKnowWhereWeAre() {
        return enemyInitiallyKnowsWhereWeAre || best.enemyKnowsWhereWeAre && best.actions.size() <= 1;
    }

    @Override
    protected int getScoutingValue(int x, int y) {
        for (MutableTrooper enemy : enemies) {
            int d = Utils.manhattanDist(enemy.getX(), enemy.getY(), x, y);
            if (d <= 2) {
                return 100;
            }
        }
        return 1;
    }

    public static boolean[][] getCanDamageIfBefore(String moveOrder, TrooperType selfType) {
        boolean[][] r = new boolean[Utils.NUMBER_OF_TROOPER_TYPES][Utils.NUMBER_OF_TROOPER_TYPES];
        char selfChar = Utils.getCharForTrooperType(selfType);
        int selfInd = -1;
        for (int i = 0; i < moveOrder.length(); i++) {
            if (moveOrder.charAt(i) == selfChar) {
                selfInd = i;
                break;
            }
        }
        for (int i = 0; i < moveOrder.length(); i++) {
            r[Utils.getTrooperTypeByChar(moveOrder.charAt(i)).ordinal()][selfType.ordinal()] = true;
        }
        for (int enemyShift = 1; enemyShift < moveOrder.length(); enemyShift++) {
            char enemyChar = moveOrder.charAt((selfInd + enemyShift) % moveOrder.length());
            int enemyInd = Utils.getTrooperTypeByChar(enemyChar).ordinal();
            for (int allyShift = enemyShift; allyShift < moveOrder.length(); allyShift++) {
                char allyChar = moveOrder.charAt((selfInd + allyShift) % moveOrder.length());
                int allyInd = Utils.getTrooperTypeByChar(allyChar).ordinal();
                r[enemyInd][allyInd] = true;
            }
        }
        return r;
    }

    public static boolean[][] getCanDamageIfAfter(String moveOrder, TrooperType selfType) {
        boolean[][] r = new boolean[Utils.NUMBER_OF_TROOPER_TYPES][Utils.NUMBER_OF_TROOPER_TYPES];
        char selfChar = Utils.getCharForTrooperType(selfType);
        int selfInd = moveOrder.indexOf(selfChar);
        for (int i = 0; i < moveOrder.length(); i++) {
            r[Utils.getTrooperTypeByChar(moveOrder.charAt(i)).ordinal()][selfType.ordinal()] = true;
        }
        for (int enemyShift = 0; enemyShift < moveOrder.length(); enemyShift++) {
            char enemyChar = moveOrder.charAt((selfInd + enemyShift) % moveOrder.length());
            int enemyInd = Utils.getTrooperTypeByChar(enemyChar).ordinal();
            for (int allyShift = enemyShift + 1; allyShift < moveOrder.length(); allyShift++) {
                char allyChar = moveOrder.charAt((selfInd + allyShift) % moveOrder.length());
                int allyInd = Utils.getTrooperTypeByChar(allyChar).ordinal();
                r[enemyInd][allyInd] = true;
            }
        }
        return r;
    }

    @Override
    protected void prepare() {
        super.prepare();
        investigate();
        sqrDistSum = new int[n][m];
        for (MutableTrooper ally : teammates) {
            updateSqrDistSum(ally.getX(), ally.getY());
        }

        enemyIsAlive = new boolean[enemies.size()];
        Arrays.fill(enemyIsAlive, true);

        prepareDistForHealing();

        prepareHelp();
        numberOfTeammatesWhoCanShoot = new int[enemies.size()];
        for (int i = 0; i < enemies.size(); i++) {
            MutableTrooper enemy = enemies.get(i);
            int enemyStance = enemy.getStance().ordinal();
            for (MutableTrooper ally : teammates) {
                int allyStance = ally.getStance().ordinal();
                if (canShoot(ally.getX(), ally.getY(), enemy.getX(), enemy.getY(), allyStance, enemyStance, ally.getType())) {
                    numberOfTeammatesWhoCanShoot[i]++;
                }
            }
        }
        numberOfTeammatesWhoCanReachEnemy = new int[n][m];
        numberOfTeammatesMedicCanReach = new int[n][m];
        for (int i = 0; i < n; i++) {
            Arrays.fill(numberOfTeammatesWhoCanReachEnemy[i], -1);
            Arrays.fill(numberOfTeammatesMedicCanReach[i], -1);
        }
        actionsEnemyMustSpendToHide = new int[enemies.size()][n][m][Utils.NUMBER_OF_STANCES];
        for (int enemyIndex = 0; enemyIndex < enemies.size(); enemyIndex++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    Arrays.fill(actionsEnemyMustSpendToHide[enemyIndex][i][j], -1);
                }
            }
        }
        prepareImaginaryEnemies();
        maxDamageEnemyCanDeal = new DamageAndAP[enemiesWithImaginary.size()][n][m][Utils.NUMBER_OF_STANCES];
        prepareVisibleByEnemy();
        prepareStepsEnemyMustMakeToSeeMe();
        printMap();
    }

    private void investigate() {
        if (damagedTeammateType == null) {
            return;
        }
        TrooperType suspectedType1 = typeByMoveIndex(mediumMoveIndex);
        int index = mediumMoveIndex - 1;
        if (index < 0) {
            index += moveOrder.length();
        }
        TrooperType suspectedType2 = typeByMoveIndex(index);
        if (isVisibleAndNotPhantom(suspectedType1) || isVisibleAndNotPhantom(suspectedType2)) {
            return;
        }

        List<TrooperType> suspectedTypes = new ArrayList<>();
        suspectedTypes.add(suspectedType1);
        if (suspectedType2 != suspectedType1) {
            suspectedTypes.add(suspectedType2);
        }
        investigationResult = getSuspected(suspectedTypes);
        if (investigationResult == null) {
            return;
        }
        for (int enemyIndex = 0; enemyIndex < enemies.size(); enemyIndex++) {
            MutableTrooper enemy = enemies.get(enemyIndex);
            if (enemy.getType() == investigationResult.getType()) {
                erase(enemy);
                add(investigationResult);
                enemies.set(enemyIndex, investigationResult);
                return;
            }
        }
        add(investigationResult);
        enemies.add(investigationResult);
    }

    private void add(MutableTrooper enemy) {
        map[enemy.getX()][enemy.getY()] = Character.toLowerCase(Utils.getCharForTrooperType(enemy.getType()));
        troopers[enemy.getX()][enemy.getY()] = enemy;
    }

    private void erase(MutableTrooper enemy) {
        map[enemy.getX()][enemy.getY()] = '.';
        troopers[enemy.getX()][enemy.getY()] = null;
    }

    private MutableTrooper getSuspected(List<TrooperType> suspectedTypes) {
        MutableTrooper damagedTeammate = null;
        if (damagedTeammateType == selfType) {
            damagedTeammate = self;
        } else {
            for (MutableTrooper teammate : teammates) {
                if (teammate.getType() == damagedTeammateType) {
                    damagedTeammate = teammate;
                }
            }
        }
        if (damagedTeammate == null) {
            throw new RuntimeException();
        }
        long playerId = anyEnemyId();
        char[][] mapWithoutEnemies = getMapWithoutEnemies();
        int[][][][] bfs = new int[n][m][][];
        Cell3D bestPos = null;
        int minDist = Integer.MAX_VALUE;
        HashSet<Cell> set = new HashSet<>();
        TrooperType bestType = null;
        for (TrooperType suspectedType : suspectedTypes) {
            if (!isAlive(playerId, suspectedType)) {
                continue;
            }
            if (!couldDealThatDamage(suspectedType, damageDealtToTeammate)) {
                continue;
            }
            for (int toX = 0; toX < n; toX++) {
                for (int toY = 0; toY < m; toY++) {
                    if (isWall(toX, toY)) {
                        continue;
                    }
                    for (int toStance = Utils.NUMBER_OF_STANCES - 1; toStance >= 0; toStance--) {
                        boolean shoot = canShoot(toX, toY, damagedTeammate.getX(), damagedTeammate.getY(), toStance, damagedTeammate.getStance().ordinal(), suspectedType);
                        boolean grenade = canDamageWithGrenade(toX, toY, damagedTeammate.getX(), damagedTeammate.getY()) && damageCouldBeDoneWithGrenade(damageDealtToTeammate);
                        if (!shoot && !grenade) {
                            continue;
                        }
                        for (int fromX = 0; fromX < n; fromX++) {
                            for (int fromY = 0; fromY < m; fromY++) {
                                if (isWall(fromX, fromY)) {
                                    continue;
                                }
                                if (Utils.isLetter(map[fromX][fromY])) {
                                    continue;
                                }
                                for (int fromStance = Utils.NUMBER_OF_STANCES - 1; fromStance >= 0; fromStance--) {
                                    if (visibleCnt[fromX][fromY][fromStance] != 0) {
                                        continue;
                                    }
                                    int[][] dist = bfs[fromX][fromY] == null ? bfs[fromX][fromY] = Utils.bfsByMap(mapWithoutEnemies, fromX, fromY) : bfs[fromX][fromY];
                                    int hasActions = getMaxInitialAP(suspectedType, playerId);
                                    int actionsToMove = getActionsToMove(dist[toX][toY], fromStance, toStance) * 2;
                                    int needActionsToShoot = actionsToMove + utils.getShootCost(suspectedType);
                                    if (!shoot) {
                                        needActionsToShoot = Integer.MAX_VALUE;
                                    }
                                    int needActionsToThrowGrenade = actionsToMove + game.getGrenadeThrowCost();
                                    if (!grenade) {
                                        needActionsToThrowGrenade = Integer.MAX_VALUE;
                                    }
                                    if (needActionsToShoot > hasActions && needActionsToThrowGrenade > hasActions) {
                                        continue;
                                    }
                                    int d = minManhattanDistToOtherEnemy(playerId, fromX, fromY, suspectedType);
                                    if (d < minDist) {
                                        minDist = d;
                                        bestPos = new Cell3D(fromX, fromY, fromStance);
                                        bestType = suspectedType;
                                    }
                                    set.add(new Cell(fromX, fromY));
                                }
                            }
                        }
                    }
                }
            }
        }

        if (bestPos == null) {
            return null;
        }

        return new MTBuilder()
                .x(bestPos.x)
                .y(bestPos.y)
                .stance(TrooperStance.values()[bestPos.stance])
                .type(bestType)
                .playerId(playerId)
                .lastSeenTime(set.size() == 1 ? mediumMoveIndex : mediumMoveIndex - moveOrder.length())
                .build();
    }

    private boolean couldDealThatDamage(TrooperType suspectedType, int damage) {
        if (damage == 60 || damage == 80) { //anyone could throw grenade
            return true;
        }
        if (damage % 5 != 0) {
            return suspectedType == FIELD_MEDIC;
        }
        return true;
    }

    private boolean damageCouldBeDoneWithGrenade(int damageDealtToTeammate) {
        return damageDealtToTeammate == 60 || damageDealtToTeammate == 80;
    }

    private boolean canDamageWithGrenade(int toX, int toY, int x, int y) {
        return canThrowGrenade(toX, toY, x, y) ||
                canThrowGrenade(toX, toY, x + 1, y) ||
                canThrowGrenade(toX, toY, x - 1, y) ||
                canThrowGrenade(toX, toY, x, y + 1) ||
                canThrowGrenade(toX, toY, x, y - 1);
    }

    private TrooperType typeByMoveIndex(int enemyMoveIndex) {
        return Utils.getTrooperTypeByChar(moveOrder.charAt(enemyMoveIndex % moveOrder.length()));
    }

    private int minManhattanDistToOtherEnemy(long playerId, int fromX, int fromY, TrooperType type) {
        int dist = 10000;
        if (lastSeenEnemyPos != null && enemies.isEmpty()) {
            dist = Utils.manhattanDist(fromX, fromY, lastSeenEnemyPos.x, lastSeenEnemyPos.y);
        }
        for (MutableTrooper enemy : enemies) {
            if (enemy.getPlayerId() != playerId) {
                continue;
            }
            if (enemy.getType() == type) {
                continue;
            }
            dist = Math.min(dist, Utils.manhattanDist(fromX, fromY, enemy.getX(), enemy.getY()));
        }
        return dist;
    }

    private boolean isVisibleAndNotPhantom(TrooperType suspectedType) {
        for (MutableTrooper enemy : enemies) {
            if (enemy.getType() == suspectedType && !isPhantom(enemy, mediumMoveIndex, moveOrder)) {
                return true;
            }
        }
        return false;
    }

    private long anyEnemyId() {
        return killedEnemies.keySet().iterator().next();
    }

    private void prepareStepsEnemyMustMakeToSeeMe() {
        apEnemyNeedsToSeeMe = new int[n][m][Utils.NUMBER_OF_STANCES];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                for (int stance = 0; stance < Utils.NUMBER_OF_STANCES; stance++) {
                    apEnemyNeedsToSeeMe[i][j][stance] = -1;
                }
            }
        }
    }

    private void printMap() {
        if (!MyStrategy.local) {
            return;
        }
        char[][] mapToPrint = new char[n][m];
        for (int i = 0; i < n; i++) {
            System.arraycopy(map[i], 0, mapToPrint[i], 0, m);
        }
        mapToPrint[cur.x][cur.y] = Utils.getCharForTrooperType(selfType);
        for (MutableTrooper enemy : enemiesWithImaginary) {
            mapToPrint[enemy.getX()][enemy.getY()] = Character.toLowerCase(Utils.getCharForTrooperType(enemy.getType()));
        }
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                System.out.print(mapToPrint[i][j]);
            }
            System.out.println();
        }
        System.out.println();
    }

    private List<MutableTrooper> createImaginaryEnemies() {
        if (initialTeamSize <= 3) { // in first round, enemies could already be killed, so dont add anybody
            return new ArrayList<>();
        }
        if (initialTeamSize == 4) { // in second round, enemies still could be killed by someone, but sniper is just too dangerous
            return createImaginary(SNIPER);
        }/**/
        if (initialTeamSize == 5) { // in finals no one could kill them but us =(
            List<MutableTrooper> r = new ArrayList<>();
            for (int i = 0; i < moveOrder.length(); i++) {
                TrooperType type = Utils.getTrooperTypeByChar(moveOrder.charAt(i));
                r.addAll(createImaginary(type));
            }
            return r;
        }
        throw new RuntimeException();
    }

    private List<MutableTrooper> createImaginary(TrooperType type) {
        Set<Long> playerIds = new HashSet<>();
        for (MutableTrooper enemy : enemies) {
            playerIds.add(enemy.getPlayerId());
        }
        List<MutableTrooper> r = new ArrayList<>();
        for (Long playerId : playerIds) {
            if (!positionIsKnown(playerId, type) && isAlive(playerId, type)) {
                MutableTrooper trooper = createImaginary(playerId, type);
                if (trooper != null) {
                    map[trooper.getX()][trooper.getY()] = Character.toLowerCase(Utils.getCharForTrooperType(trooper.getType()));
                    r.add(trooper);
                }
            }
        }
        return r;
    }

    private boolean positionIsKnown(long playerId, TrooperType type) {
        for (MutableTrooper enemy : enemies) {
            if (enemy.getPlayerId() == playerId && enemy.getType() == type) {
                return true;
            }
        }
        return false;
    }

    private MutableTrooper createImaginary(long playerId, TrooperType type) {
        Cell bestPos = getSupposedImaginaryEnemyPosition(playerId);
        if (bestPos == null) {
            return null;
        }

        // id? teammateIndex? lastSeenTime?
        MutableTrooper mt = new MTBuilder()
                .x(bestPos.x)
                .y(bestPos.y)
                .playerId(playerId)
                .type(type)
                .stance(PRONE)
                .build();
        return mt;
    }

    private Cell getSupposedImaginaryEnemyPosition(Long playerId) {
        Cell bestPos = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (!isFree(i, j)) {
                    continue;
                }/**/
                for (int stance = 0; stance < Utils.NUMBER_OF_STANCES; stance++) {
                    if (visibleCnt[i][j][stance] != 0) {
                        continue;
                    }
                    int dist = minDistToOtherNearestEnemy(playerId, i, j);
                    if (dist < minDist) {
                        minDist = dist;
                        bestPos = new Cell(i, j);
                    }
                }
            }
        }
        return bestPos;
    }

    private int minDistToOtherNearestEnemy(Long playerId, int i, int j) {
        int dist = Integer.MAX_VALUE;
        for (int enemyIndex = 0; enemyIndex < enemies.size(); enemyIndex++) {
            MutableTrooper enemy = enemies.get(enemyIndex);
            if (enemy.getPlayerId() != playerId) {
                continue;
            }
            dist = Math.min(dist, enemyBfs.get(enemyIndex)[i][j] * 100 + Utils.manhattanDist(enemy.getX(), enemy.getY(), i, j));
        }
        return dist;
    }

    private boolean isAlive(Long playerId, TrooperType type) {
        Set<TrooperType> killed = killedEnemies.get(playerId);
        if (killed == null) {
            return true;
        }
        return !killed.contains(type);
    }

    private void prepareVisibleByEnemy() {
        visibleByEnemy = new boolean[enemiesWithImaginary.size()][n][m][Utils.NUMBER_OF_STANCES];
        for (int enemyIndex = 0; enemyIndex < enemiesWithImaginary.size(); enemyIndex++) {
            MutableTrooper enemy = enemiesWithImaginary.get(enemyIndex);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    for (int stance = 0; stance < Utils.NUMBER_OF_STANCES; stance++) {
                        if (canSee(enemy, i, j, stance)) {
                            visibleByEnemy[enemyIndex][i][j][stance] = true;
                        }
                    }
                }
            }
        }
    }

    private void prepareDistForHealing() {
        List<int[][]> distWithoutTeammates = getDistWithoutTeammates();
        distToTeammatesForHealing = getDistToTeammates();
        for (int allyIndex = 0; allyIndex < teammates.size(); allyIndex++) {
            int[][] dist = distToTeammatesForHealing.get(allyIndex);
            int[][] distWithout = distWithoutTeammates.get(allyIndex);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    if (dist[i][j] - distWithout[i][j] > MAX_DIST_MEDIC_SHOULD_TRY_HEAL) {
                        dist[i][j] = Utils.UNREACHABLE;
                    }
                }
            }
        }
    }

    private void prepareImaginaryEnemies() {
        enemyBfs = new ArrayList<>();
        char[][] mapWithoutEnemies = getMapWithoutEnemies();

        for (MutableTrooper enemy : enemies) {
            enemyBfs.add(Utils.bfsByMap(mapWithoutEnemies, enemy.getX(), enemy.getY()));
        }

        enemiesWithImaginary = new ArrayList<>(enemies);
        enemiesWithImaginary.addAll(createImaginaryEnemies());

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                mapWithoutEnemies[i][j] = map[i][j];
                if (Utils.isEnemyChar(map[i][j])) {
                    mapWithoutEnemies[i][j] = '.';
                }
            }
        }

        for (int i = enemies.size(); i < enemiesWithImaginary.size(); i++) {
            MutableTrooper enemy = enemiesWithImaginary.get(i);
            enemyBfs.add(Utils.bfsByMap(mapWithoutEnemies, enemy.getX(), enemy.getY()));
        }
    }

    private char[][] getMapWithoutEnemies() {
        char[][] mapWithoutEnemies = new char[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                mapWithoutEnemies[i][j] = map[i][j];
                if (Utils.isEnemyChar(map[i][j])) {
                    mapWithoutEnemies[i][j] = '.';
                }
            }
        }
        return mapWithoutEnemies;
    }

    private DamageAndAP getDamageAndAP(int enemyIndex, int targetX, int targetY, int targetStance) {
        if (maxDamageEnemyCanDeal[enemyIndex][targetX][targetY][targetStance] == null) {
            DamageAndAP r = null;
            MutableTrooper enemy = enemiesWithImaginary.get(enemyIndex);
            int[][] dist = enemyBfs.get(enemyIndex);
            int initialEnemyStance = enemy.getStance().ordinal();
            for (int shooterX = 0; shooterX < n; shooterX++) {
                for (int shooterY = 0; shooterY < m; shooterY++) {
                    if (dist[shooterX][shooterY] > 6) {
                        continue;
                    }
                    if (isWall(shooterX, shooterY)) {
                        continue;
                    }

                    boolean grenade = enemy.isHoldingGrenade();
                    boolean canThrowGrenadeDirectly = grenade && canThrowGrenade(shooterX, shooterY, targetX, targetY);
                    boolean canThrowGrenadeCollateral = grenade &&
                            (
                                    canThrowGrenade(shooterX, shooterY, targetX + 1, targetY) ||
                                            canThrowGrenade(shooterX, shooterY, targetX - 1, targetY) ||
                                            canThrowGrenade(shooterX, shooterY, targetX, targetY + 1) ||
                                            canThrowGrenade(shooterX, shooterY, targetX, targetY - 1)
                            );

                    int minShooterStance = -1;
                    for (int shooterStance = 0; shooterStance < Utils.NUMBER_OF_STANCES; shooterStance++) {
                        boolean canShoot = canShoot(shooterX, shooterY, targetX, targetY, shooterStance, targetStance, enemy.getType());
                        if (canShoot && minShooterStance == -1) {
                            minShooterStance = shooterStance;
                        }
                        int actionPoints = getMaxInitialAP(enemy.getType(), enemy.getPlayerId());
                        if (enemy.isHoldingFieldRation()) {
                            actionPoints += game.getFieldRationBonusActionPoints() - game.getFieldRationEatCost();
                        }
                        DamageAndAP p = getMaxDamage(enemy.getType(), actionPoints, dist[shooterX][shooterY], initialEnemyStance, minShooterStance, canShoot, canThrowGrenadeDirectly, canThrowGrenadeCollateral);
                        if (!getEnemyCanSeeOrKnowsPosition(targetX, targetY, targetStance) && getApEnemyNeedsToSee(targetX, targetY, targetStance) > 2) {
                            p.damage = (int) (p.damage * INVISIBLE_DAMAGE_QUOTIENT + 0.5);
                        }
                        if (r == null) {
                            r = p;
                        }
                        r = DamageAndAP.max(r, p);
                    }
                }
            }
            maxDamageEnemyCanDeal[enemyIndex][targetX][targetY][targetStance] = r;
        }
        return maxDamageEnemyCanDeal[enemyIndex][targetX][targetY][targetStance];
    }

    private int getMaxInitialAP(TrooperType enemyType, long playerId) {
        int r = utils.getInitialActionPoints(enemyType);
        if (enemyType != COMMANDER && enemyType != SCOUT && commanderIsAlive(playerId)) {
            r += game.getCommanderAuraBonusActionPoints();
        }
        return r;
    }

    private boolean commanderIsAlive(long playerId) { //todo check if just killed
        if (!killedEnemies.containsKey(playerId)) {
            return true;
        }
        return !killedEnemies.get(playerId).contains(COMMANDER);
    }

    private boolean canThrowGrenade(int shooterX, int shooterY, int targetX, int targetY) {
        return Utils.sqrDist(shooterX, shooterY, targetX, targetY) <= Utils.sqr(game.getGrenadeThrowRange());
    }

    public MutableTrooper getInvestigationResult() {
        return investigationResult;
    }

    static class DamageAndAP {
        int damage; //max damage enemy can deal
        int ap;     //action points he should waste on moving before dealing that damage

        DamageAndAP(int damage, int ap) {
            this.damage = damage;
            this.ap = ap;
        }

        static DamageAndAP ZERO = new DamageAndAP(0, 0);

        static DamageAndAP max(DamageAndAP a, DamageAndAP b) {
            if (a.damage > b.damage) {
                return a;
            }
            if (a.damage < b.damage) {
                return b;
            }
            if (a.ap < b.ap) {
                return a;
            }
            return b;
        }

        @Override
        public String toString() {
            return "DamageAndAP{" +
                    "damage=" + damage +
                    ", ap=" + ap +
                    '}';
        }

    }

    private DamageAndAP getMaxDamage(TrooperType type, int actionPoints, int dist, int curStance, int minStance, boolean canShoot, boolean canThrowGrenadeDirect, boolean canThrowGrenadeCollateral) {
        if (!canShoot && !canThrowGrenadeDirect && !canThrowGrenadeCollateral) {
            return DamageAndAP.ZERO;
        }
        boolean canThrowGrenade = canThrowGrenadeDirect || canThrowGrenadeCollateral;
        int grenadeDamage = canThrowGrenadeDirect ? game.getGrenadeDirectDamage() :
                canThrowGrenadeCollateral ? game.getGrenadeCollateralDamage() : 0;
        final int maxStance = Utils.NUMBER_OF_STANCES - 1;
        int maxDamage = 0;
        int bestAp = 0;
        int shootCost = utils.getShootCost(type);
        for (int shootStance = minStance; shootStance <= maxStance; shootStance++) {
            int oneShotDamage = canShoot ? utils.getShootDamage(type, TrooperStance.values()[shootStance]) : 0;
            int remainingActionPoints = actionPoints - getApToMove(dist, curStance, shootStance);
            int damage = getMaxDamage(remainingActionPoints, canShoot, oneShotDamage, shootCost, canThrowGrenade, grenadeDamage);
            if (damage > maxDamage) {
                maxDamage = damage;
                bestAp = actionPoints - remainingActionPoints;
            }

            maxDamage = Math.max(maxDamage, damage);
        }
        return new DamageAndAP(maxDamage, bestAp);
    }

    private int getMaxDamage(int remainingActionPoints, boolean canShoot, int shootDamage, int shootCost, boolean canThrowGrenade, int grenadeDamage) {
        int r = 0;
        if (canThrowGrenade && remainingActionPoints >= game.getGrenadeThrowCost()) {
            int damage = grenadeDamage;
            if (canShoot) {
                damage += (remainingActionPoints - game.getGrenadeThrowCost()) / shootCost * shootDamage;
            }
            r = damage;
        }
        if (canShoot) {
            r = Math.max(r, remainingActionPoints / shootCost * shootDamage);
        }
        return r;
    }

    private void prepareHelp() {
        helpFactor = new int[enemies.size()][n][m];
        minStanceForHelp = new int[enemies.size()][n][m];
        maxStanceForHelp = new int[enemies.size()][n][m];
        int[][] distFromMe = Utils.bfsByMap(map, cur.x, cur.y);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (!isFree(i, j)) {
                    continue;
                }
                if (distFromMe[i][j] > MyStrategy.MAX_DISTANCE_SHOULD_TRY_HELP) {
                    continue;
                }
                for (int enemyIndex = 0; enemyIndex < enemies.size(); enemyIndex++) {
                    MutableTrooper enemy = enemies.get(enemyIndex);
                    int minStance = -1;
                    int maxStance = -1;
                    for (int stance = 0; stance < Utils.NUMBER_OF_STANCES; stance++) {
                        if (canShoot(i, j, enemy.getX(), enemy.getY(), stance, enemy.getStance().ordinal(), selfType)
                                || cur.holdingGrenade && canThrowGrenade(i, j, enemy.getX(), enemy.getY())) {
                            if (minStance == -1) {
                                minStance = stance;
                            }
                            maxStance = stance;
                        }
                    }
                    if (minStance == -1) {
                        continue;
                    }
                    minStanceForHelp[enemyIndex][i][j] = minStance;
                    maxStanceForHelp[enemyIndex][i][j] = maxStance;

                    int d = 1;
                    for (MutableTrooper ally : teammates) {
                        TrooperType allyType = ally.getType();
                        int allyStance = ally.getStance().ordinal();
                        if (canShoot(ally.getX(), ally.getY(), enemy.getX(), enemy.getY(), allyStance, enemy.getStance().ordinal(), allyType)) {
                            d++;
                        }
                    }
                    helpFactor[enemyIndex][i][j] += d;
                }
            }
        }

        helpDist = new int[enemies.size()][][];
        for (int i = 0; i < enemies.size(); i++) {
            helpDist[i] = Utils.bfsByMapAndStartingCells(map, helpFactor[i]);
        }
    }

    @Override
    void updateBest() {
        cur.healDist = getDistToTeammatesSum();
        cur.minHp = getMinHp();
        cur.focusFireParameter = getFocusFireParameter();
        cur.helpFactor = getHelpFactor();
        cur.helpDist = getHelpDist();
        cur.numberOfTeammatesWhoCanReachEnemy = getNumberOfTeammatesWhoCanReachEnemy();
        cur.numberOfTeammatesMedicCanReach = getNumberOfTeammatesMedicCanReach();
        cur.enemyKnowsWhereWeAre = checkEnemyKnowsWhereWeAre();
        cur.actionsEnemyMustSpendToHide = actionsEnemyMustSpendToHide();
        cur.apEnemyNeedsToSeeMe = getApEnemyNeedsToSeeMe();
        if (stopOn(MyMove.LOWER_STANCE)) {
            int x = 0;
            x++;
        }
        updateMaxDamageEnemyCanDeal();

        if (cur.better(best, selfType)) {
            Utils.log(cur);
            best = new TacticState(cur);
        }
    }

    private int getApEnemyNeedsToSeeMe() {
        return getApEnemyNeedsToSee(cur.x, cur.y, cur.stance.ordinal());
    }

    private int getApEnemyNeedsToSee(int x, int y, int stance) { //todo consider move order
        if (apEnemyNeedsToSeeMe[x][y][stance] == -1) {
            int mi = Integer.MAX_VALUE;
            for (int enemyIndex = 0; enemyIndex < enemiesWithImaginary.size(); enemyIndex++) {
                MutableTrooper enemy = enemiesWithImaginary.get(enemyIndex);
                int[][] dist = enemyBfs.get(enemyIndex);
                for (int viewX = 0; viewX < n; viewX++) {
                    for (int viewY = 0; viewY < m; viewY++) {
                        if (dist[viewX][viewY] > 6) {
                            continue;
                        }
                        if (isWall(viewX, viewY)) {
                            continue;
                        }
                        for (int viewStance = 0; viewStance < Utils.NUMBER_OF_STANCES; viewStance++) {
                            if (!canSee(viewX, viewY, viewStance, x, y, stance, enemy)) {
                                continue;
                            }
                            int hasActionPoints = getMaxInitialAP(enemy.getType(), enemy.getPlayerId());
                            int needActionPoints = getApToMove(dist[viewX][viewY], enemy.getStance().ordinal(), viewStance);
                            if (needActionPoints > hasActionPoints) {
                                continue;
                            }
                            mi = Math.min(mi, needActionPoints);
                        }
                    }
                }
            }
            apEnemyNeedsToSeeMe[x][y][stance] = mi;
        }
        return apEnemyNeedsToSeeMe[x][y][stance];
    }

    private boolean canSee(int viewX, int viewY, int viewStance, int targetX, int targetY, int targetStance, MutableTrooper viewer) { // if change this, change another canSee
        return reachable(viewX, viewY, targetX, targetY, Math.min(viewStance, targetStance), viewer.getVisionRange());
    }

    static int getApToMove(int dist, int fromStance, int toStance) {
        int minStance = Math.min(fromStance, toStance);
        int maxStance = Math.max(fromStance, toStance);
        if (dist <= 1) {
            return dist * moveCost(maxStance) + (maxStance - minStance) * 2;
        }
        return dist * 2 + (2 - maxStance + 2 - minStance) * 2;
    }

    static int moveCost(int stance) {
        if (stance == 2) {
            return 2;
        } else if (stance == 1) {
            return 4;
        } else {
            return 6;
        }
    }

    private int actionsEnemyMustSpendToHide() {
        int r = 0;
        for (int enemyIndex = 0; enemyIndex < enemies.size(); enemyIndex++) {
            MutableTrooper enemy = enemies.get(enemyIndex);
            if (!enemy.isAlive()) {
                continue;
            }
            int mi = getNumberOfActionsEnemyMustSpendToHide(enemyIndex);
            r += mi;
        }
        return r;
    }

    private int getNumberOfActionsEnemyMustSpendToHide(int enemyIndex) {
        if (actionsEnemyMustSpendToHide[enemyIndex][cur.x][cur.y][cur.stance.ordinal()] == -1) {
            MutableTrooper enemy = enemies.get(enemyIndex);
            int[][] dist = enemyBfs.get(enemyIndex);
            int mi = Integer.MAX_VALUE;
            for (int hideX = 0; hideX < n; hideX++) {
                for (int hideY = 0; hideY < m; hideY++) {
                    if (dist[hideX][hideY] == Utils.UNREACHABLE) {
                        continue;
                    }
                    if (isWall(hideX, hideY)) {
                        continue;
                    }
                    for (int hideStance = 0; hideStance < Utils.NUMBER_OF_STANCES; hideStance++) {
                        if (!canShoot(cur.x, cur.y, hideX, hideY, cur.stance.ordinal(), hideStance, selfType)) {
                            int d = getActionsToMove(dist[hideX][hideY], enemy.getStance().ordinal(), hideStance);
                            mi = Math.min(mi, d);
                        }
                    }
                }
            }
            actionsEnemyMustSpendToHide[enemyIndex][cur.x][cur.y][cur.stance.ordinal()] = mi;
        }
        return actionsEnemyMustSpendToHide[enemyIndex][cur.x][cur.y][cur.stance.ordinal()];
    }

    private int getActionsToMove(int dist, int fromStance, int toStance) {
        int r = Integer.MAX_VALUE;
        for (int moveStance = Math.max(fromStance, toStance); moveStance < Utils.NUMBER_OF_STANCES; moveStance++) {
            int t = dist * utils.getMoveCost(moveStance) + (moveStance - fromStance + moveStance - toStance) * game.getStanceChangeCost();
            r = Math.min(r, t);
        }
        return r;
    }

    private boolean checkEnemyKnowsWhereWeAre() {
        if (enemyInitiallyKnowsWhereWeAre) {
            return true;
        }
        if (finalsAndHavingBigAdvantage()) {
            return true;
        }
        for (MutableTrooper enemy : enemiesWithImaginary) {
            if (!enemy.isAlive()) {
                continue;
            }
            if (canSee(enemy, cur.x, cur.y, cur.stance.ordinal())) {
                return true;
            }
            for (MutableTrooper ally : teammates) {
                if (canSee(enemy, ally.getX(), ally.getY(), ally.getStance().ordinal())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canSee(MutableTrooper trooper, int targetX, int targetY, int targetStance) {
        return reachable(trooper.getX(), trooper.getY(), targetX, targetY, Math.min(trooper.getStance().ordinal(), targetStance), (int) trooper.getVisionRange());
    }

    private void updateMaxDamageEnemyCanDeal() {  //todo it is not actually correct. max damage value and canKill may correspond to different move orders

        cur.maxDamageEnemyCanDeal = DamageAndAP.ZERO;
        cur.someOfTeammatesCanBeKilled = false;
        cur.enemyCanKillMe = false;

        if (finalsAndHavingBigAdvantage()) {
            return;
        }

        DamageAndAP damage = DamageAndAP.max(
                getMaxDamageEnemyCanDeal(cur.x, cur.y, selfType, cur.stance, canDamageIfBefore),
                getMaxDamageEnemyCanDeal(cur.x, cur.y, selfType, cur.stance, canDamageIfAfter)
        );
        if (damage.damage >= cur.selfHp) {
            cur.someOfTeammatesCanBeKilled = true;
            cur.enemyCanKillMe = true;
        }
        damage.damage = Math.min(damage.damage, cur.selfHp);
        cur.maxDamageEnemyCanDeal = DamageAndAP.max(cur.maxDamageEnemyCanDeal, damage);

        for (MutableTrooper ally : teammates) {
            damage = DamageAndAP.max(
                    getMaxDamageEnemyCanDeal(ally.getX(), ally.getY(), ally.getType(), ally.getStance(), canDamageIfBefore),
                    getMaxDamageEnemyCanDeal(ally.getX(), ally.getY(), ally.getType(), ally.getStance(), canDamageIfAfter)
            );
            if (damage.damage >= ally.getHitpoints()) {
                cur.someOfTeammatesCanBeKilled = true;
            }
            damage.damage = Math.min(damage.damage, ally.getHitpoints());
            cur.maxDamageEnemyCanDeal = DamageAndAP.max(cur.maxDamageEnemyCanDeal, damage);
        }
    }

    private boolean finalsAndHavingBigAdvantage() {
        if (finals() && teammates.size() + 1 - aliveEnemyCnt() >= 3) {
            return true;
        }
        return false;
    }

    private boolean finals() {
        return initialTeamSize == 5;
    }

    private int aliveEnemyCnt() {
        return enemiesWithImaginary.size() - cur.killCnt;
    }

    private DamageAndAP getMaxDamageEnemyCanDeal(int x, int y, TrooperType targetType, TrooperStance stance, boolean[][] canDamage) {
        int damage = 0;
        int ap = 0;
        for (int enemyIndex = 0; enemyIndex < enemiesWithImaginary.size(); enemyIndex++) {
            MutableTrooper enemy = enemiesWithImaginary.get(enemyIndex);
            if (!enemy.isAlive()) {
                continue;
            }
            if (!canDamage[enemy.getType().ordinal()][targetType.ordinal()]) {
                continue;
            }
            DamageAndAP p = getDamageAndAP(enemyIndex, x, y, stance.ordinal());
            damage += p.damage;
            ap += p.ap;
            if (p.damage == 0) {
                ap += 100;
            }
        }
        if (damage == 0) {
            return DamageAndAP.ZERO;
        }
        return new DamageAndAP(damage, ap);
    }

    private boolean getEnemyCanSeeOrKnowsPosition(int x, int y, int stance) {
        if (enemyKnowsPosition.contains(new Cell(x, y))) {
            return true;
        }
        for (int enemyIndex = 0; enemyIndex < enemiesWithImaginary.size(); enemyIndex++) {
            if (enemiesWithImaginary.get(enemyIndex).isAlive() && visibleByEnemy[enemyIndex][x][y][stance]) {
                return true;
            }
        }
        return false;
    }

    private int getHelpDist() {
        int r = Integer.MAX_VALUE;
        for (int enemyIndex = 0; enemyIndex < enemies.size(); enemyIndex++) {
            if (!enemyIsAlive[enemyIndex]) {
                continue;
            }
            int dist = helpDist[enemyIndex][cur.x][cur.y];

            // and here comes magic
            if (dist == 0) {
                dist = -3;
                if (cur.stance.ordinal() < minStanceForHelp[enemyIndex][cur.x][cur.y]) {
                    dist += minStanceForHelp[enemyIndex][cur.x][cur.y] - cur.stance.ordinal();
                }
                if (cur.stance.ordinal() > maxStanceForHelp[enemyIndex][cur.x][cur.y]) {
                    dist += cur.stance.ordinal() - maxStanceForHelp[enemyIndex][cur.x][cur.y];
                }
            }

            r = Math.min(r, dist);
        }
        return r;
    }

    private int getHelpFactor() {
        int r = 0;
        for (int enemyIndex = 0; enemyIndex < enemies.size(); enemyIndex++) {
            if (!enemyIsAlive[enemyIndex]) {
                continue;
            }
            r += helpFactor[enemyIndex][cur.x][cur.y];
        }
        return r;
    }

    private int getNumberOfTeammatesMedicCanReach() {
        MutableTrooper medic = null;
        for (MutableTrooper ally : teammates) {
            if (ally.getType() == FIELD_MEDIC) {
                medic = ally;
                break;
            }
        }
        if (medic == null) { //even if self is medic
            return -1;
        }
        if (numberOfTeammatesMedicCanReach[cur.x][cur.y] == -1) {
            char buf = map[cur.x][cur.y];
            map[cur.x][cur.y] = Utils.getCharForTrooperType(selfType);
            numberOfTeammatesMedicCanReach[cur.x][cur.y] = 0;

            for (MutableTrooper ally : teammates) {
                if (ally.getType() == FIELD_MEDIC) {
                    continue;
                }
                int[][] dist = Utils.bfsByMap(map, medic.getX(), medic.getY());
                if (dist[ally.getX()][ally.getY()] <= MAX_DIST_MEDIC_SHOULD_TRY_HEAL) {
                    numberOfTeammatesMedicCanReach[cur.x][cur.y]++;
                }
            }
            map[cur.x][cur.y] = buf;
        }
        return numberOfTeammatesMedicCanReach[cur.x][cur.y];
    }

    private int getNumberOfTeammatesWhoCanReachEnemy() {
        if (numberOfTeammatesWhoCanReachEnemy[cur.x][cur.y] == -1) {
            char buf = map[cur.x][cur.y];
            map[cur.x][cur.y] = Utils.getCharForTrooperType(selfType);
            numberOfTeammatesWhoCanReachEnemy[cur.x][cur.y] = 0;

            for (MutableTrooper ally : teammates) {
                if (ally.getType() == FIELD_MEDIC) {
                    continue;
                }
                int[][] dist = Utils.bfsByMap(map, ally.getX(), ally.getY());
                if (canReachSomeEnemy(ally, dist)) {
                    numberOfTeammatesWhoCanReachEnemy[cur.x][cur.y]++;
                }
            }
            map[cur.x][cur.y] = buf;
        }
        return numberOfTeammatesWhoCanReachEnemy[cur.x][cur.y];
    }

    private boolean canReachSomeEnemy(MutableTrooper ally, int[][] dist) {
        TrooperType allyType = ally.getType();

        for (int newX = 0; newX < n; newX++) {
            for (int newY = 0; newY < map[newX].length; newY++) {
                if (!isFree(newX, newY) && !(newX == ally.getX() && newY == ally.getY())) {
                    continue;
                }
                if (dist[newX][newY] >= 7) {
                    continue;
                }
                for (MutableTrooper enemy : enemies) {
                    int enemyStance = enemy.getStance().ordinal();
                    if (canShoot(newX, newY, enemy.getX(), enemy.getY(), STANDING.ordinal(), enemyStance, allyType)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void tryEatFieldRation() {
        if (cur.holdingFieldRation && cur.actionPoints >= game.getFieldRationEatCost() && cur.actionPoints < utils.getInitialActionPoints(selfType)) {
            addAction(MyMove.EAT_FIELD_RATION);
            int oldActionPoints = cur.actionPoints;
            cur.actionPoints = utils.actionPointsAfterEatingFieldRation(selfType, cur.actionPoints, game);
            cur.holdingFieldRation = false;
            cur.fieldRationsUsed++;
            rec();
            cur.fieldRationsUsed--;
            cur.holdingFieldRation = true;
            cur.actionPoints = oldActionPoints;
            popAction();
        }
    }

    private void newTryHeal(int healValue, int healCost, MyMove healAction, MutableTrooper ally) { // if ally == null, heal self
        if (cur.actionPoints < healCost) {
            return;
        }

        int oldHp = ally == null ? cur.selfHp : ally.getHitpoints();
        if (oldHp >= Utils.INITIAL_TROOPER_HP) {
            return;
        }
        int newHp = Math.min(Utils.INITIAL_TROOPER_HP, oldHp + healValue);
        int diffHp = newHp - oldHp;

        addAction(healAction);

        if (ally == null) {
            cur.selfHp = newHp;
        } else {
            ally.setHitpoints(newHp);
        }
        cur.actionPoints -= healCost;
        cur.healedSum += diffHp;

        rec();

        cur.healedSum -= diffHp;
        cur.actionPoints += healCost;
        if (ally == null) {
            cur.selfHp = oldHp;
        } else {
            ally.setHitpoints(oldHp);
        }
        popAction();
    }

    private void tryHealSelfWithMedikit() {
        if (!cur.holdingMedikit) {
            return;
        }
        int healValue = game.getMedikitHealSelfBonusHitpoints();
        cur.holdingMedikit = false;
        newTryHeal(healValue, game.getMedikitUseCost(), MyMove.USE_MEDIKIT_SELF, null);
        cur.holdingMedikit = true;
    }

    private void tryHealTeammates(MyMove[] heals, int healValue, int healCost) {
        for (MyMove heal : heals) {
            int toX = cur.x + heal.getDx();
            int toY = cur.y + heal.getDy();
            if (!inField(toX, toY)) {
                continue;
            }
            MutableTrooper trooper = troopers[toX][toY];
            if (trooper == null || !trooper.isTeammate()) {
                continue;
            }
            newTryHeal(healValue, healCost, heal, trooper);
        }
    }

    private void tryHealWithMedikit() {
        if (cur.holdingMedikit) {
            cur.holdingMedikit = false;
            tryHealTeammates(MyMove.directedMedikitUses, game.getMedikitBonusHitpoints(), game.getMedikitUseCost());
            cur.holdingMedikit = true;
        }
        tryHealSelfWithMedikit();
    }

    private void tryHealSelfWithAbility() {
        newTryHeal(game.getFieldMedicHealSelfBonusHitpoints(), game.getFieldMedicHealCost(), MyMove.HEAL_SELF, null);
    }

    private void tryHealAsMedic() {
        if (selfType != FIELD_MEDIC) {
            return;
        }
        tryHealTeammates(MyMove.directedHeals, game.getFieldMedicHealBonusHitpoints(), game.getFieldMedicHealCost());
        tryHealSelfWithAbility();
    }

    void dealDamage(int ex, int ey, int damage, boolean grenade) {
        if (!inField(ex, ey)) {
            return;
        }
        if (!enemyInCell(ex, ey)) {
            return;
        }

        MutableTrooper enemy = troopers[ex][ey];

        damage = getDamageConsiderPhantom(enemy, damage, grenade);

        if (enemy.getHitpoints() > 0) {
            if (damage >= enemy.getHitpoints()) {
                cur.killCnt++;
                enemyIsAlive[enemyIndex[ex][ey]] = false;
            }
            cur.damageSum += Math.min(damage, enemy.getHitpoints());
        }
        enemy.decHp(damage);
    }

    private boolean enemyInCell(int ex, int ey) {
        return (troopers[ex][ey] != null && !troopers[ex][ey].isTeammate());
    }

    private int getDamageConsiderPhantom(MutableTrooper enemy, int damage, boolean grenade) {
        if (isPhantom(enemy, mediumMoveIndex, moveOrder)) {
            damage /= 2;
        }
        return damage;
    }

    static boolean isPhantom(MutableTrooper enemy, int mediumMoveIndex, String moveOrder) {
        if (enemy.getLastSeenTime() == mediumMoveIndex) {
            return false;
        }
        int enemyIndex;
        for (int i = enemy.getLastSeenTime(); ; i++) {
            if (moveOrder.charAt((i + moveOrder.length()) % moveOrder.length()) == Utils.getCharForTrooperType(enemy.getType())) {
                enemyIndex = i;
                break;
            }
        }
        return enemyIndex >= enemy.getLastSeenTime() && enemyIndex <= mediumMoveIndex;
    }

    private void undealDamage(int ex, int ey, int damage, boolean grenade) {
        if (!inField(ex, ey)) {
            return;
        }
        if (!enemyInCell(ex, ey)) {
            return;
        }

        MutableTrooper enemy = troopers[ex][ey];

        damage = getDamageConsiderPhantom(enemy, damage, grenade);

        if (enemy.getHitpoints() + damage > 0) {
            if (enemy.getHitpoints() <= 0) {
                cur.killCnt--;
                enemyIsAlive[enemyIndex[ex][ey]] = true;
            }
            cur.damageSum -= Math.min(damage, enemy.getHitpoints() + damage);
        }
        enemy.decHp(-damage);
    }

    private void unshoot(int ex, int ey) {
        int damage = utils.getShootDamage(selfType, cur.stance);
        cur.actionPoints += utils.getShootCost(selfType);

        undealDamage(ex, ey, damage, false);

        popAction();
    }

    private void shoot(int ex, int ey) {
        addAction(MyMove.shoot(ex, ey));

        int damage = utils.getShootDamage(selfType, cur.stance);
        cur.actionPoints -= utils.getShootCost(selfType);
        dealDamage(ex, ey, damage, false);
    }

    private void tryShoot() {
        int shootCost = utils.getShootCost(selfType);
        if (cur.actionPoints < shootCost) {
            return;
        }
        for (MutableTrooper enemy : enemies) {
            if (enemy.getHitpoints() > 0 && canShoot(cur.x, cur.y, enemy.getX(), enemy.getY(), cur.stance.ordinal(), enemy.getStance().ordinal(), selfType)) {
                shoot(enemy.getX(), enemy.getY());
                rec();
                unshoot(enemy.getX(), enemy.getY());
            }
        }
    }

    private void unthrowGrenade(int ex, int ey) {
        popAction();
        cur.actionPoints += game.getGrenadeThrowCost();
        cur.holdingGrenade = true;
        undealDamage(ex, ey, game.getGrenadeDirectDamage(), true);
        undealDamage(ex + 1, ey, game.getGrenadeCollateralDamage(), true);
        undealDamage(ex - 1, ey, game.getGrenadeCollateralDamage(), true);
        undealDamage(ex, ey + 1, game.getGrenadeCollateralDamage(), true);
        undealDamage(ex, ey - 1, game.getGrenadeCollateralDamage(), true);
    }

    private void throwGrenade(int ex, int ey) {
        addAction(MyMove.grenade(ex, ey));
        cur.actionPoints -= game.getGrenadeThrowCost();
        cur.holdingGrenade = false;
        dealDamage(ex, ey, game.getGrenadeDirectDamage(), true);
        dealDamage(ex + 1, ey, game.getGrenadeCollateralDamage(), true);
        dealDamage(ex - 1, ey, game.getGrenadeCollateralDamage(), true);
        dealDamage(ex, ey + 1, game.getGrenadeCollateralDamage(), true);
        dealDamage(ex, ey - 1, game.getGrenadeCollateralDamage(), true);
    }

    private boolean forbidden(int x, int y) {
        if (!inField(x, y)) {
            return false;
        }
        if (x == cur.x && y == cur.y) {
            return true;
        }
        return troopers[x][y] != null && troopers[x][y].isTeammate();
    }

    private boolean canThrowGrenade(int ex, int ey) {
        if (!inField(ex, ey)) {
            return false;
        }
        if (Utils.sqrDist(cur.x, cur.y, ex, ey) > Utils.sqr(game.getGrenadeThrowRange())) {
            return false;
        }
        return !(forbidden(ex, ey) ||
                forbidden(ex + 1, ey) ||
                forbidden(ex - 1, ey) ||
                forbidden(ex, ey + 1) ||
                forbidden(ex, ey - 1));
    }

    private void tryThrowGrenade(int x, int y) {
        if (canThrowGrenade(x, y)) {
            throwGrenade(x, y);
            rec();
            unthrowGrenade(x, y);
        }
    }

    private void tryThrowGrenade() {
        if (!cur.holdingGrenade || cur.actionPoints < game.getGrenadeThrowCost()) {
            return;
        }
        for (MutableTrooper enemy : enemies) {
            tryThrowGrenade(enemy.getX(), enemy.getY());
            tryThrowGrenade(enemy.getX() + 1, enemy.getY());
            tryThrowGrenade(enemy.getX() - 1, enemy.getY());
            tryThrowGrenade(enemy.getX(), enemy.getY() + 1);
            tryThrowGrenade(enemy.getX(), enemy.getY() - 1);
        }
    }

    @Override
    protected void tryAllActions() {
        if (!bonusUseForbidden) {
            tryEatFieldRation();
        }
        if (!healForbidden) {
            tryHealAsMedic();
            if (!bonusUseForbidden) {
                tryHealWithMedikit();
            }
        }
        tryMove();
        tryThrowGrenade();
        tryShoot();
        tryRaiseStance();
        tryLowerStance();
    }

    private void updateSqrDistSum(int x, int y) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < map[i].length; j++) {
                sqrDistSum[i][j] += Utils.sqrDist(x, y, i, j);
            }
        }
    }

    private int getFocusFireParameter() {
        int r = 0;
        for (int enemyIndex = 0; enemyIndex < enemies.size(); enemyIndex++) {
            MutableTrooper enemy = enemies.get(enemyIndex);
            if (enemy.getHitpoints() <= 0) {
                continue;
            }
            r += (10000 - sqrDistSum[enemy.getX()][enemy.getY()] + 10000 * numberOfTeammatesWhoCanShoot[enemyIndex]) * (Utils.INITIAL_TROOPER_HP * 2 - enemy.getHitpoints()); // =)
        }
        return r;
    }

    private int getMinHp() {
        int mi = cur.selfHp;
        for (MutableTrooper ally : teammates) {
            mi = Math.min(mi, ally.getHitpoints());
        }
        return mi;
    }

    private int getDistToTeammatesSum() {
        int r = 0;
        int minHp = Integer.MAX_VALUE;
        for (MutableTrooper ally : teammates) {
            minHp = Math.min(minHp, ally.getHitpoints());
        }
        for (int i = 0; i < teammates.size(); i++) {
            MutableTrooper ally = teammates.get(i);
            int d = distToTeammatesForHealing.get(i)[cur.x][cur.y];
            if (ally.getHitpoints() == minHp) {
                d *= 100;
            }
            r += d;
        }
        return r;
    }

}
