import model.BonusType;
import model.Game;
import model.TrooperStance;

import static model.TrooperType.*;

import model.TrooperType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static model.TrooperStance.*;

public class PlanComputer {
    private static final long MAX_RECURSIVE_CALLS = 3000000;
    long recursiveCallsCnt;

    private final int n, m;
    private final char[][] map;
    private final Game game;
    private final Utils utils;
    private final List<MutableTrooper> enemies;
    private final List<MutableTrooper> teammates; //here, unlike MyStrategy, teammates does not contain self
    private State cur, best;
    private TrooperType selfType;
    private boolean[] visibilities;
    private int[][] sqrDistSum;
    BonusType[][] bonuses;
    List<int[][]> bfsDistFromTeammateForHealing;
    private int[][][] helpFactor;
    private int[][][] helpDist;
    private int[] numberOfTeammatesWhoCanShoot;
    private int[][] numberOfTeammatesWhoCanReachEnemy;
    boolean[] enemyIsAlive;
    int[][] enemyIndex;
    private boolean healForbidden;
    private boolean bonusUseForbidden;

    int[][][][] maxDamageEnemyCanDeal;

    MutableTrooper[][] troopers;

    public PlanComputer(char[][] map, Utils utils, BonusType[][] bonuses, boolean[] visibilities, boolean healForbidden, boolean bonusUseForbidden, MutableTrooper[][] troopers, List<MutableTrooper> teammates, List<MutableTrooper> enemies, State state
    ) {
        this.map = map;
        n = map.length;
        m = map[0].length;
        this.utils = utils;
        this.game = utils.getGame();
        this.bonuses = bonuses;
        this.visibilities = visibilities;
        this.cur = state;
        this.healForbidden = healForbidden; //todo it is hack. Actually exist situations where even alone medic should heal himself
        this.bonusUseForbidden = bonusUseForbidden;
        this.troopers = troopers;
        this.teammates = teammates;
        this.enemies = enemies;
        enemyIndex = new int[n][m];
        for (int i = 0; i < enemies.size(); i++) {
            MutableTrooper enemy = enemies.get(i);
            enemyIndex[enemy.getX()][enemy.getY()] = i;
        }
        prepare();
        rec();
    }

    private void prepare() {
        selfType = getType(cur.x, cur.y);
        map[cur.x][cur.y] = '.';
        sqrDistSum = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < map[i].length; j++) {
                char ch = map[i][j];
                if (Utils.isTeammateChar(ch)) {
                    updateSqrDistSum(i, j);
                }
            }
        }
        enemyIsAlive = new boolean[enemies.size()];
        Arrays.fill(enemyIsAlive, true);

        char[][] mapWithoutTeammates = new char[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                mapWithoutTeammates[i][j] = map[i][j];
                if (Utils.isTeammateChar(map[i][j])) {
                    mapWithoutTeammates[i][j] = '.';
                }
            }
        }

        bfsDistFromTeammateForHealing = new ArrayList<>();
        for (MutableTrooper ally : teammates) {
            int[][] curDist = Utils.bfsByMap(map, ally.getX(), ally.getY());
            int[][] curDistWithoutTeammates = Utils.bfsByMap(mapWithoutTeammates, ally.getX(), ally.getY());
            bfsDistFromTeammateForHealing.add(curDist);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < map[i].length; j++) {
                    if (curDist[i][j] - curDistWithoutTeammates[i][j] > 7) {
                        curDist[i][j] = Utils.UNREACHABLE;
                    }
                }
            }
        }
        prepareHelp();
        numberOfTeammatesWhoCanShoot = new int[enemies.size()];
        for (int i = 0; i < enemies.size(); i++) {
            MutableTrooper enemy = enemies.get(i);
            int enemyStance = enemy.getStance().ordinal();
            for (MutableTrooper ally : teammates) {
                int allyStance = ally.getStance().ordinal();
                TrooperType allyType = getType(ally.getX(), ally.getY());
                if (canShoot(ally.getX(), ally.getY(), enemy.getX(), enemy.getY(), Math.min(enemyStance, allyStance), allyType)) {
                    numberOfTeammatesWhoCanShoot[i]++;
                }
            }
        }
        numberOfTeammatesWhoCanReachEnemy = new int[n][m];
        for (int i = 0; i < n; i++) {
            Arrays.fill(numberOfTeammatesWhoCanReachEnemy[i], -1);
        }
        prepareMaxDamageEnemyCanDeal();
    }



    private void prepareMaxDamageEnemyCanDeal() {
        maxDamageEnemyCanDeal = new int[enemies.size()][n][m][Utils.NUMBER_OF_STANCES];

        for (int enemyIndex = 0; enemyIndex < enemies.size(); enemyIndex++) {
            MutableTrooper enemy = enemies.get(enemyIndex);
            int[][] dist = Utils.bfsByMap(map, enemy.getX(), enemy.getY());
            int initialEnemyStance = enemy.getStance().ordinal();
            TrooperType enemyType = Utils.getTrooperTypeByChar(map[enemy.getX()][enemy.getY()]);

            for (int shooterX = 0; shooterX < n; shooterX++) {
                for (int shooterY = 0; shooterY < m; shooterY++) {
                    if (dist[shooterX][shooterY] > 6) {
                        continue;
                    }
                    if (!isFree(shooterX, shooterY) && (shooterX != enemy.getX() || shooterY != enemy.getY())) {
                        continue;
                    }
                    for (int targetX = 0; targetX < n; targetX++) {
                        for (int targetY = 0; targetY < m; targetY++) {
                            boolean grenade = troopers[enemy.getX()][enemy.getY()].isHoldingGrenade();
                            boolean canThrowGrenadeDirectly = grenade && canThrowGrenade(shooterX, shooterY, targetX, targetY);
                            boolean canThrowGrenadeCollateral = grenade &&
                                    (
                                            canThrowGrenade(shooterX, shooterY, targetX + 1, targetY) ||
                                                    canThrowGrenade(shooterX, shooterY, targetX - 1, targetY) ||
                                                    canThrowGrenade(shooterX, shooterY, targetX, targetY + 1) ||
                                                    canThrowGrenade(shooterX, shooterY, targetX, targetY - 1)
                                    );
                            for (int targetStance = 0; targetStance < Utils.NUMBER_OF_STANCES; targetStance++) {
                                int minShooterStance = -1;
                                for (int shooterStance = 0; shooterStance < Utils.NUMBER_OF_STANCES; shooterStance++) {
                                    boolean canShoot = canShoot(shooterX, shooterY, targetX, targetY, Math.min(targetStance, shooterStance), enemyType);
                                    if (targetX == 18 && targetY == 14 && enemyIndex == 0 && targetStance == 2 && shooterStance == 2 && shooterX == enemy.getX() + 2 && shooterY == enemy.getY() + 1) {
                                        int x = 0;
                                        x++;
                                    }
                                    if (canShoot && minShooterStance == -1) {
                                        minShooterStance = shooterStance;
                                    }
                                    int actionPoints = utils.getInitialActionPointsWithCommanderBonus(enemyType);
                                    int maxDamage = getMaxDamage(enemyType, actionPoints, dist[shooterX][shooterY], initialEnemyStance, minShooterStance, canShoot, canThrowGrenadeDirectly, canThrowGrenadeCollateral);
                                    maxDamageEnemyCanDeal[enemyIndex][targetX][targetY][targetStance] =
                                            Math.max(maxDamageEnemyCanDeal[enemyIndex][targetX][targetY][targetStance], maxDamage);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean canThrowGrenade(int shooterX, int shooterY, int targetX, int targetY) {
        if (Utils.sqrDist(shooterX, shooterY, targetX, targetY) > Utils.sqr(game.getGrenadeThrowRange())) {
            return false;
        }
        return true;
    }

    private int getMaxDamage(TrooperType type, int actionPoints, int dist, int curStance, int minStance, boolean canShoot, boolean canThrowGrenadeDirect, boolean canThrowGrenadeCollateral) {
        if (!canShoot && !canThrowGrenadeDirect && !canThrowGrenadeCollateral) {
            return 0;
        }
        boolean canThrowGrenade = canThrowGrenadeDirect || canThrowGrenadeCollateral;
        int grenadeDamage = canThrowGrenadeDirect ? game.getGrenadeDirectDamage() :
                canThrowGrenadeCollateral ? game.getGrenadeCollateralDamage() : 0;
        final int maxStance = Utils.NUMBER_OF_STANCES - 1;
        int maxDamage = 0;
        for (int shootStance = minStance; shootStance <= maxStance; shootStance++) {
            for (int walkStance = Math.max(curStance, shootStance); walkStance <= maxStance; walkStance++) {
                int stanceChangeCnt = walkStance - curStance + walkStance - shootStance;
                int remainingActionPoints = actionPoints
                        - stanceChangeCnt * game.getStanceChangeCost()
                        - dist * utils.getMoveCost(TrooperStance.values()[walkStance]);
                int oneShotDamage = canShoot ? utils.getShootDamage(type, TrooperStance.values()[shootStance]) : 0;
                int damage = getMaxDamage(remainingActionPoints, canShoot, oneShotDamage, utils.getShootCost(type), canThrowGrenade, grenadeDamage);
                maxDamage = Math.max(maxDamage, damage);
            }
        }
        return maxDamage;
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
                    if (!canShoot(i, j, enemy.getX(), enemy.getY(), enemy.getStance().ordinal(), selfType)) {
                        continue;
                    }
                    int d = 1;
                    for (MutableTrooper ally : teammates) {
                        TrooperType allyType = getType(ally.getX(), ally.getY());
                        int allyStance = ally.getStance().ordinal();
                        if (canShoot(ally.getX(), ally.getY(), enemy.getX(), enemy.getY(), allyStance, allyType)) {
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

    void updateBest() {

        //todo do not store parameters calculable in O(1)

        cur.healDist = getDistToTeammatesSum();
        cur.minHp = getMinHp();
        cur.focusFireParameter = getFocusFireParameter();
        cur.helpFactor = getHelpFactor();
        cur.helpDist = getHelpDist();
        cur.numberOfTeammatesWhoCanReachEnemy = getNumberOfTeammatesWhoCanReachEnemy();
        cur.maxDamageEnemyCanDeal = getMaxDamageEnemyCanDeal();
        cur.someOfTeammatesCanBeKilled = someOfTeammatesCanBeKilled();
        //getMaxDamageEnemyCanDeal();
        if (cur.better(best, selfType)) {
            //cur.better(best, selfType);
            best = new State(cur);
        }
    }

    private boolean someOfTeammatesCanBeKilled() {
        int max = 0;
        for (int enemyIndex = 0; enemyIndex < enemies.size(); enemyIndex++) {
            if (!enemyIsAlive[enemyIndex]) {
                continue;
            }
            max += maxDamageEnemyCanDeal[enemyIndex][cur.x][cur.y][cur.stance.ordinal()];
        }

        if (max >= cur.selfHp) {
            return true;
        }

        for (MutableTrooper ally : teammates) {
            int t = 0;
            int stance = ally.getStance().ordinal();
            for (int enemyIndex = 0; enemyIndex < enemies.size(); enemyIndex++) {
                if (!enemyIsAlive[enemyIndex]) {
                    continue;
                }
                t += maxDamageEnemyCanDeal[enemyIndex][ally.getX()][ally.getY()][stance];
            }
            if (t >= troopers[ally.getX()][ally.getY()].getHitpoints()) {
                return true;
            }
        }

        return false;
    }

    private int getMaxDamageEnemyCanDeal() { //todo maximize minimal hp
        int max = 0;
        for (int enemyIndex = 0; enemyIndex < enemies.size(); enemyIndex++) {
            if (!enemyIsAlive[enemyIndex]) {
                continue;
            }
            max += maxDamageEnemyCanDeal[enemyIndex][cur.x][cur.y][cur.stance.ordinal()];
        }

        for (MutableTrooper ally : teammates) {
            int t = 0;
            int stance = ally.getStance().ordinal();
            for (int enemyIndex = 0; enemyIndex < enemies.size(); enemyIndex++) {
                if (!enemyIsAlive[enemyIndex]) {
                    continue;
                }
                t += maxDamageEnemyCanDeal[enemyIndex][ally.getX()][ally.getY()][stance];
            }
            max = Math.max(max, t);
        }

        return max;
    }

    private int getHelpDist() {
        int r = Integer.MAX_VALUE;
        for (int enemyIndex = 0; enemyIndex < enemies.size(); enemyIndex++) {
            if (!enemyIsAlive[enemyIndex]) {
                continue;
            }
            r = Math.min(r, helpDist[enemyIndex][cur.x][cur.y]);
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

    private int getNumberOfTeammatesWhoCanReachEnemy() {
        if (numberOfTeammatesWhoCanReachEnemy[cur.x][cur.y] == -1) {
            char buf = map[cur.x][cur.y];
            map[cur.x][cur.y] = Utils.getCharForTrooperType(selfType);
            numberOfTeammatesWhoCanReachEnemy[cur.x][cur.y] = 0;

            for (MutableTrooper ally : teammates) {
                if (getType(ally.getX(), ally.getY()) == FIELD_MEDIC) {
                    continue;
                }
                int[][] dist = Utils.bfsByMap(map, ally.getX(), ally.getY());
                if (canReachSomeEnemy(ally.getX(), ally.getY(), dist)) {
                    numberOfTeammatesWhoCanReachEnemy[cur.x][cur.y]++;
                }
            }

            map[cur.x][cur.y] = buf;
        }
        return numberOfTeammatesWhoCanReachEnemy[cur.x][cur.y];
    }

    private boolean canReachSomeEnemy(int startX, int startY, int[][] dist) {
        int allyStance = troopers[startX][startY].getStance().ordinal();
        TrooperType allyType = getType(startX, startY);

        for (int newX = 0; newX < n; newX++) {
            for (int newY = 0; newY < map[newX].length; newY++) {
                if (!isFree(newX, newY) && !(newX == startX && newY == startY)) {
                    continue;
                }
                if (dist[newX][newY] >= 7) {
                    continue;
                }
                for (MutableTrooper enemy : enemies) {
                    int enemyStance = enemy.getStance().ordinal();
                    if (canShoot(newX, newY, enemy.getX(), enemy.getY(), Math.min(enemyStance, allyStance), allyType)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private TrooperType getType(int x, int y) {
        return Utils.getTrooperTypeByChar(map[x][y]);
    }


    private void addAction(MyMove action) {
        cur.actions.add(action);
    }

    private void popAction() {
        cur.actions.remove(cur.actions.size() - 1);
    }

    private boolean inField(int toX, int toY) {
        return toX >= 0 && toX < n && toY >= 0 && toY < m;
    }

    private void tryMove() {
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
            rec();
            cur.x -= movement.getDx();
            cur.y -= movement.getDy();
            cur.actionPoints += moveCost;
            popAction();
        }
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

    public State getPlan() {
        return best;
    }

    private void newTryHeal(int healValue, int healCost, MyMove healAction, Cell c) {
        if (cur.actionPoints < healCost) {
            return;
        }

        int oldHp = c == null ? cur.selfHp : troopers[c.x][c.y].getHitpoints();
        if (oldHp >= Utils.INITIAL_TROOPER_HP) {
            return;
        }
        int newHp = Math.min(Utils.INITIAL_TROOPER_HP, oldHp + healValue);
        int diffHp = newHp - oldHp;

        addAction(healAction);

        if (c == null) {
            cur.selfHp = newHp;
        } else {
            troopers[c.x][c.y].setHitpoints(newHp);
        }
        cur.actionPoints -= healCost;
        cur.healedSum += diffHp;

        rec();

        cur.healedSum -= diffHp;
        cur.actionPoints += healCost;
        if (c == null) {
            cur.selfHp = oldHp;
        } else {
            troopers[c.x][c.y].setHitpoints(oldHp);
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
            char targetChar = map[toX][toY];
            if (!Utils.isTeammateChar(targetChar)) {
                continue;
            }
            newTryHeal(healValue, healCost, heal, new Cell(toX, toY));
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

    private boolean isFree(int x, int y) {
        return map[x][y] == '.' || map[x][y] == '?' || troopers[x][y] != null && troopers[x][y].getHitpoints() <= 0;
    }

    void dealDamage(int ex, int ey, int damage) {
        if (!inField(ex, ey)) {
            return;
        }
        if (!Utils.isEnemyChar(map[ex][ey])) {
            return;
        }

        if (troopers[ex][ey].getHitpoints() > 0) {
            if (damage >= troopers[ex][ey].getHitpoints()) {
                cur.killCnt++;
                enemyIsAlive[enemyIndex[ex][ey]] = false;
            }
            cur.damageSum += Math.min(damage, troopers[ex][ey].getHitpoints());
        }
        troopers[ex][ey].decHp(damage);
    }

    private void undealDamage(int ex, int ey, int damage) {
        if (!inField(ex, ey)) {
            return;
        }
        if (!Utils.isEnemyChar(map[ex][ey])) {
            return;
        }

        if (troopers[ex][ey].getHitpoints() + damage > 0) {
            if (troopers[ex][ey].getHitpoints() <= 0) {
                cur.killCnt--;
                enemyIsAlive[enemyIndex[ex][ey]] = true;
            }
            cur.damageSum -= Math.min(damage, troopers[ex][ey].getHitpoints() + damage);
        }
        troopers[ex][ey].decHp(-damage);
    }

    private void unshoot(int ex, int ey) {
        int damage = utils.getShootDamage(selfType, cur.stance);
        cur.actionPoints += utils.getShootCost(selfType);

        undealDamage(ex, ey, damage);

        popAction();
    }

    private void shoot(int ex, int ey) {
        addAction(MyMove.shoot(ex, ey));

        int damage = utils.getShootDamage(selfType, cur.stance);
        cur.actionPoints -= utils.getShootCost(selfType);
        dealDamage(ex, ey, damage);
    }

    private boolean visible(int viewerX, int viewerY, int objectX, int objectY, int stance) {
        int width = n;
        int height = m;
        int stanceCount = Utils.NUMBER_OF_STANCES;
        return visibilities[viewerX * height * width * height * stanceCount
                + viewerY * width * height * stanceCount
                + objectX * height * stanceCount
                + objectY * stanceCount
                + stance];
    }

    private boolean reachable(int viewerX, int viewerY, int objectX, int objectY, int stance, int range) {
        if (Utils.sqrDist(viewerX, viewerY, objectX, objectY) > Utils.sqr(range)) {
            return false;
        }
        return visible(viewerX, viewerY, objectX, objectY, stance);
    }

    private boolean canSee(int viewerX, int viewerY, int objectX, int objectY, int stance, TrooperType type) {
        int range = utils.getVisionRange(type);
        return reachable(viewerX, viewerY, objectX, objectY, stance, range);
    }

    private boolean canShoot(int viewerX, int viewerY, int objectX, int objectY, int stance, TrooperType type) {
        int shootRange = utils.getShootRange(type);
        return reachable(viewerX, viewerY, objectX, objectY, stance, shootRange);
    }

    private boolean canShoot(int viewerX, int viewerY, int objectX, int objectY, int stance) {
        return canShoot(viewerX, viewerY, objectX, objectY, stance, selfType);
    }

    private void tryShoot() {
        int shootCost = utils.getShootCost(selfType);
        if (cur.actionPoints < shootCost) {
            return;
        }
        for (MutableTrooper enemy : enemies) {
            int ex = enemy.getX(), ey = enemy.getY();
            if (troopers[ex][ey].getHitpoints() > 0 && canShoot(cur.x, cur.y, ex, ey, Math.min(cur.stance.ordinal(), enemy.getStance().ordinal()))) {
                shoot(ex, ey);
                rec();
                unshoot(ex, ey);
            }
        }
    }

    private void tryLowerStance() {
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

    private void tryRaiseStance() {
        if (cur.actionPoints < game.getStanceChangeCost()) {
            return;
        }
        if (cur.stance == STANDING) {
            return;
        }
        addAction(MyMove.RAISE_STANCE);
        cur.actionPoints -= game.getStanceChangeCost();
        cur.stance = Utils.stanceAfterRaising(cur.stance);
        rec();
        cur.stance = Utils.stanceAfterLowering(cur.stance);
        cur.actionPoints += game.getStanceChangeCost();
        popAction();
    }

    private void unthrowGrenade(int ex, int ey) {
        popAction();
        cur.actionPoints += game.getGrenadeThrowCost();
        cur.holdingGrenade = true;
        undealDamage(ex, ey, game.getGrenadeDirectDamage());
        undealDamage(ex + 1, ey, game.getGrenadeCollateralDamage());
        undealDamage(ex - 1, ey, game.getGrenadeCollateralDamage());
        undealDamage(ex, ey + 1, game.getGrenadeCollateralDamage());
        undealDamage(ex, ey - 1, game.getGrenadeCollateralDamage());
    }

    private void throwGrenade(int ex, int ey) {
        addAction(MyMove.grenade(ex, ey));
        cur.actionPoints -= game.getGrenadeThrowCost();
        cur.holdingGrenade = false;
        dealDamage(ex, ey, game.getGrenadeDirectDamage());
        dealDamage(ex + 1, ey, game.getGrenadeCollateralDamage());
        dealDamage(ex - 1, ey, game.getGrenadeCollateralDamage());
        dealDamage(ex, ey + 1, game.getGrenadeCollateralDamage());
        dealDamage(ex, ey - 1, game.getGrenadeCollateralDamage());
    }

    private boolean forbidden(int x, int y) {
        if (!inField(x, y)) {
            return false;
        }
        if (x == cur.x && y == cur.y) {
            return true;
        }
        if (Utils.isTeammateChar(map[x][y])) {
            return true;
        }
        return false;
    }

    private boolean canThrowGrenade(int ex, int ey) {
        if (!inField(ex, ey)) {
            return false;
        }
        if (Utils.sqrDist(cur.x, cur.y, ex, ey) > Utils.sqr(game.getGrenadeThrowRange())) {
            return false;
        }
        if (forbidden(ex, ey) ||
                forbidden(ex + 1, ey) ||
                forbidden(ex - 1, ey) ||
                forbidden(ex, ey + 1) ||
                forbidden(ex, ey - 1)) {
            return false;
        }
        return true;
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

    private void rec() {
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

        bonuses[cur.x][cur.y] = bonus;
        cur.holdingGrenade = oldHoldingGrenade;
        cur.holdingFieldRation = oldHoldingFieldRation;
        cur.holdingMedikit = oldHoldingMedikit;
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
            if (troopers[enemy.getX()][enemy.getY()].getHitpoints() <= 0) {
                continue;
            }
            r += (10000 - sqrDistSum[enemy.getX()][enemy.getY()] + 10000 * numberOfTeammatesWhoCanShoot[enemyIndex]) * (Utils.INITIAL_TROOPER_HP * 2 - troopers[enemy.getX()][enemy.getY()].getHitpoints()); // =)
        }
        return r;
    }

    private int getMinHp() {
        int mi = cur.selfHp;
        for (MutableTrooper ally : teammates) {
            mi = Math.min(mi, troopers[ally.getX()][ally.getY()].getHitpoints());
        }
        return mi;
    }

    private int getDistToTeammatesSum() {
        int r = 0;
        int minHp = Integer.MAX_VALUE;
        for (MutableTrooper ally : teammates) {
            minHp = Math.min(minHp, troopers[ally.getX()][ally.getY()].getHitpoints());
        }
        for (int i = 0; i < teammates.size(); i++) {
            MutableTrooper ally = teammates.get(i);
            int d = bfsDistFromTeammateForHealing.get(i)[cur.x][cur.y];
            if (troopers[ally.getX()][ally.getY()].getHitpoints() == minHp) {
                d *= 100;
            }
            r += d;
        }
        return r;
    }

    @SuppressWarnings("unused")
    boolean stopOn(MyMove... move) { //for debug only
        return Arrays.asList(move).equals(cur.actions);
    }
}
