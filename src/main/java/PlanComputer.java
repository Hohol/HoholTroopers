import model.BonusType;
import model.Game;
import model.TrooperStance;
import model.TrooperType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static model.TrooperStance.PRONE;
import static model.TrooperStance.STANDING;
import static model.TrooperType.COMMANDER;
import static model.TrooperType.FIELD_MEDIC;

public class PlanComputer {
    private static final long MAX_RECURSIVE_CALLS = 3000000;
    long recursiveCallsCnt;

    private final int n, m;
    private final char[][] map;
    private final Game game;
    private final Utils utils;
    private final List<Cell> enemyPositions = new ArrayList<>();
    private final List<Cell> allyPositions = new ArrayList<>();
    private State cur, best;
    private TrooperType selfType;
    private int[][] hp;
    private boolean[] visibilities;
    private int[][] sqrDistSum;
    BonusType[][] bonuses;
    TrooperStance[][] stances;
    List<int[][]> bfsDistFromTeammateForHealing;
    private int[][][] helpFactor;
    private int[][][] helpDist;
    private int[] numberOfTeammatesWhoCanShoot;
    private int[][] numberOfTeammatesWhoCanReachEnemy;
    boolean[] enemyIsAlive;
    int[][] enemyIndex;
    private int enemyCnt;
    private boolean healForbidden;
    private boolean bonusUseForbidden;
    private boolean[][] hasGrenade;

    int[][][][] maxDamageEnemyCanDeal;

    public PlanComputer(char[][] map, Utils utils, int[][] hp, BonusType[][] bonuses, TrooperStance[][] stances, boolean[][] hasGrenade, boolean[] visibilities, boolean healForbidden, boolean bonusUseForbidden, State state) {
        this.map = map;
        n = map.length;
        m = map[0].length;
        this.utils = utils;
        this.game = utils.getGame();
        this.hp = hp;
        this.bonuses = bonuses;
        this.stances = stances;
        this.visibilities = visibilities;
        this.cur = state;
        this.hp = hp;
        this.hasGrenade = hasGrenade;
        this.healForbidden = healForbidden; //todo it is hack. Actually exist situations where even alone medic should heal himself
        this.bonusUseForbidden = bonusUseForbidden;
        prepare();
        //long start = System.currentTimeMillis();
        rec();
        //long end = System.currentTimeMillis();
        //System.out.println("Calculated in " + (end - start) + " milliseconds");
        //System.out.println("Recursive calls cnt = " + recursiveCallsCnt);
    }

    private void prepare() {
        enemyIndex = new int[n][m];
        selfType = getType(cur.x, cur.y);
        cur.selfHp = hp[cur.x][cur.y];
        map[cur.x][cur.y] = '.';
        sqrDistSum = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < map[i].length; j++) {
                char ch = map[i][j];
                if (Utils.isEnemyChar(ch)) {
                    enemyPositions.add(new Cell(i, j));
                    enemyCnt++;
                    enemyIndex[i][j] = enemyPositions.size() - 1;
                } else if (Utils.isTeammateChar(ch)) {
                    allyPositions.add(new Cell(i, j));
                    updateSqrDistSum(i, j);
                } else {
                    hp[i][j] = Integer.MAX_VALUE;
                }
            }
        }
        enemyIsAlive = new boolean[enemyCnt];
        Arrays.fill(enemyIsAlive, true);

        bfsDistFromTeammateForHealing = new ArrayList<>();
        for (Cell cell : allyPositions) {
            int[][] curDist = Utils.bfsByMap(map, cell.x, cell.y);
            bfsDistFromTeammateForHealing.add(curDist);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < map[i].length; j++) {
                    if (curDist[i][j] > MyStrategy.MAX_DISTANCE_MEDIC_SHOULD_HEAL) {
                        curDist[i][j] = Utils.UNREACHABLE;
                    }
                }
            }
        }
        prepareHelp();
        numberOfTeammatesWhoCanShoot = new int[enemyPositions.size()];
        for (int i = 0; i < enemyPositions.size(); i++) {
            Cell enemyPos = enemyPositions.get(i);
            int enemyStance = stances[enemyPos.x][enemyPos.y].ordinal();
            for (Cell allyPos : allyPositions) {
                int allyStance = stances[allyPos.x][allyPos.y].ordinal();
                TrooperType allyType = getType(allyPos.x, allyPos.y);
                if (canShoot(allyPos.x, allyPos.y, enemyPos.x, enemyPos.y, Math.min(enemyStance, allyStance), allyType)) {
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
        maxDamageEnemyCanDeal = new int[enemyCnt][n][m][Utils.NUMBER_OF_STANCES];

        for (int enemyIndex = 0; enemyIndex < enemyCnt; enemyIndex++) {
            Cell enemyPos = enemyPositions.get(enemyIndex);
            int[][] dist = Utils.bfsByMap(map, enemyPos.x, enemyPos.y);
            int initialEnemyStance = stances[enemyPos.x][enemyPos.y].ordinal();
            TrooperType enemyType = Utils.getTrooperTypeByChar(map[enemyPos.x][enemyPos.y]);

            for (int shooterX = 0; shooterX < n; shooterX++) {
                for (int shooterY = 0; shooterY < m; shooterY++) {
                    if (dist[shooterX][shooterY] > 6) {
                        continue;
                    }
                    if (!isFree(shooterX, shooterY) && (shooterX != enemyPos.x || shooterY != enemyPos.y)) {
                        continue;
                    }
                    for (int targetX = 0; targetX < n; targetX++) {
                        for (int targetY = 0; targetY < m; targetY++) {
                            boolean grenade = hasGrenade[enemyPos.x][enemyPos.y];
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
                                    if (targetX == 18 && targetY == 14 && enemyIndex == 0 && targetStance == 2 && shooterStance == 2 && shooterX == enemyPos.x + 2 && shooterY == enemyPos.y + 1) {
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
        final int maxStance = Utils.NUMBER_OF_STANCES - 1;
        int maxDamage = 0;
        for (int shootStance = minStance; shootStance <= maxStance; shootStance++) {
            for (int walkStance = Math.max(curStance, shootStance); walkStance <= maxStance; walkStance++) {
                int stanceChangeCnt = walkStance - curStance + walkStance - shootStance;
                int remainingActionPoints = actionPoints
                        - stanceChangeCnt * game.getStanceChangeCost()
                        - dist * utils.getMoveCost(TrooperStance.values()[walkStance]);
                int shootCnt = remainingActionPoints / utils.getShootCost(type);
                int damage = 0;
                if (canShoot) {
                    damage = Math.max(damage, shootCnt * utils.getShootDamage(type, TrooperStance.values()[shootStance]));
                }
                if (canThrowGrenadeDirect && remainingActionPoints >= game.getGrenadeThrowCost()) {
                    damage = Math.max(damage, game.getGrenadeDirectDamage());
                }
                if (canThrowGrenadeCollateral && remainingActionPoints >= game.getGrenadeThrowCost()) {
                    damage = Math.max(damage, game.getGrenadeCollateralDamage());
                }
                maxDamage = Math.max(maxDamage, damage);
            }
        }
        return maxDamage;
    }

    private void prepareHelp() {
        helpFactor = new int[enemyCnt][n][m];
        int[][] distFromMe = Utils.bfsByMap(map, cur.x, cur.y);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (!isFree(i, j)) {
                    continue;
                }
                if (distFromMe[i][j] > MyStrategy.MAX_DISTANCE_SHOULD_TRY_HELP) {
                    continue;
                }
                for (int enemyIndex = 0; enemyIndex < enemyCnt; enemyIndex++) {
                    Cell enemyPos = enemyPositions.get(enemyIndex);
                    if (!canShoot(i, j, enemyPos.x, enemyPos.y, stances[enemyPos.x][enemyPos.y].ordinal(), selfType)) {
                        continue;
                    }
                    int d = 1;
                    for (Cell allyPos : allyPositions) {
                        TrooperType allyType = getType(allyPos.x, allyPos.y);
                        int allyStance = stances[allyPos.x][allyPos.y].ordinal();
                        if (canShoot(allyPos.x, allyPos.y, enemyPos.x, enemyPos.y, allyStance, allyType)) {
                            d++;
                        }
                    }
                    helpFactor[enemyIndex][i][j] += d;
                }
            }
        }

        helpDist = new int[enemyCnt][][];
        for (int i = 0; i < enemyCnt; i++) {
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
        for (int enemyIndex = 0; enemyIndex < enemyCnt; enemyIndex++) {
            if (!enemyIsAlive[enemyIndex]) {
                continue;
            }
            max += maxDamageEnemyCanDeal[enemyIndex][cur.x][cur.y][cur.stance.ordinal()];
        }

        if (max >= cur.selfHp) {
            return true;
        }

        for (Cell allyPos : allyPositions) {
            int t = 0;
            int stance = stances[allyPos.x][allyPos.y].ordinal();
            for (int enemyIndex = 0; enemyIndex < enemyCnt; enemyIndex++) {
                if (!enemyIsAlive[enemyIndex]) {
                    continue;
                }
                t += maxDamageEnemyCanDeal[enemyIndex][allyPos.x][allyPos.y][stance];
            }
            if (t >= hp[allyPos.x][allyPos.y]) {
                return true;
            }
        }

        return false;
    }

    private int getMaxDamageEnemyCanDeal() { //todo maximize minimal hp
        int max = 0;
        for (int enemyIndex = 0; enemyIndex < enemyCnt; enemyIndex++) {
            if (!enemyIsAlive[enemyIndex]) {
                continue;
            }
            max += maxDamageEnemyCanDeal[enemyIndex][cur.x][cur.y][cur.stance.ordinal()];
        }

        for (Cell allyPos : allyPositions) {
            int t = 0;
            int stance = stances[allyPos.x][allyPos.y].ordinal();
            for (int enemyIndex = 0; enemyIndex < enemyCnt; enemyIndex++) {
                if (!enemyIsAlive[enemyIndex]) {
                    continue;
                }
                t += maxDamageEnemyCanDeal[enemyIndex][allyPos.x][allyPos.y][stance];
            }
            max = Math.max(max, t);
        }

        return max;
    }

    private int getHelpDist() {
        int r = Integer.MAX_VALUE;
        for (int enemyIndex = 0; enemyIndex < enemyCnt; enemyIndex++) {
            if (!enemyIsAlive[enemyIndex]) {
                continue;
            }
            r = Math.min(r, helpDist[enemyIndex][cur.x][cur.y]);
        }
        return r;
    }

    private int getHelpFactor() {
        int r = 0;
        for (int enemyIndex = 0; enemyIndex < enemyCnt; enemyIndex++) {
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

            for (Cell ally : allyPositions) {
                if (getType(ally.x, ally.y) == FIELD_MEDIC) {
                    continue;
                }
                int[][] dist = Utils.bfsByMap(map, ally.x, ally.y);
                if (canReachSomeEnemy(ally.x, ally.y, dist)) {
                    numberOfTeammatesWhoCanReachEnemy[cur.x][cur.y]++;
                }
            }

            map[cur.x][cur.y] = buf;
        }
        return numberOfTeammatesWhoCanReachEnemy[cur.x][cur.y];
    }

    private boolean canReachSomeEnemy(int startX, int startY, int[][] dist) {
        int allyStance = stances[startX][startY].ordinal();
        TrooperType allyType = getType(startX, startY);

        for (int newX = 0; newX < n; newX++) {
            for (int newY = 0; newY < map[newX].length; newY++) {
                if (!isFree(newX, newY) && !(newX == startX && newY == startY)) {
                    continue;
                }
                if (dist[newX][newY] >= 7) {
                    continue;
                }
                for (Cell enemyPos : enemyPositions) {
                    int enemyStance = stances[enemyPos.x][enemyPos.y].ordinal();
                    if (canShoot(newX, newY, enemyPos.x, enemyPos.y, Math.min(enemyStance, allyStance), allyType)) {
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

    private void newTryHeal(int healValue, int healCost, MyMove healAction, boolean avoidOverheal, Cell c) {
        if (cur.actionPoints < healCost) {
            return;
        }

        int oldHp = c == null ? cur.selfHp : hp[c.x][c.y];//hp1d[typeOrdinal];
        if (oldHp >= Utils.INITIAL_TROOPER_HP) {
            return;
        }
        int newHp = Math.min(Utils.INITIAL_TROOPER_HP, oldHp + healValue);
        int diffHp = newHp - oldHp;
        if (avoidOverheal && diffHp < healValue) {
            return;
        }

        addAction(healAction);

        if (c == null) {
            cur.selfHp = newHp;
        } else {
            hp[c.x][c.y] = newHp;
        }
        cur.actionPoints -= healCost;
        cur.healedSum += diffHp;

        rec();

        cur.healedSum -= diffHp;
        cur.actionPoints += healCost;
        if (c == null) {
            cur.selfHp = oldHp;
        } else {
            hp[c.x][c.y] = oldHp;
        }
        popAction();
    }

    private void tryHealSelfWithMedikit() {
        if (!cur.holdingMedikit) {
            return;
        }
        int healValue = game.getMedikitHealSelfBonusHitpoints();
        cur.holdingMedikit = false;
        newTryHeal(healValue, game.getMedikitUseCost(), MyMove.USE_MEDIKIT_SELF, true, null);
        cur.holdingMedikit = true;
    }

    private void tryHealTeammates(MyMove[] heals, int healValue, int healCost, boolean avoidOverheal) {
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
            newTryHeal(healValue, healCost, heal, avoidOverheal, new Cell(toX, toY));
        }
    }

    private void tryHealWithMedikit() {
        if (cur.holdingMedikit) {
            cur.holdingMedikit = false;
            tryHealTeammates(MyMove.directedMedikitUses, game.getMedikitBonusHitpoints(), game.getMedikitUseCost(), true);
            cur.holdingMedikit = true;
        }
        tryHealSelfWithMedikit();
    }

    private void tryHealSelfWithAbility() {
        newTryHeal(game.getFieldMedicHealSelfBonusHitpoints(), game.getFieldMedicHealCost(), MyMove.HEAL_SELF, false, null);
    }

    private void tryHealAsMedic() {
        if (selfType != FIELD_MEDIC) {
            return;
        }
        tryHealTeammates(MyMove.directedHeals, game.getFieldMedicHealBonusHitpoints(), game.getFieldMedicHealCost(), false);
        tryHealSelfWithAbility();
    }

    private boolean isFree(int x, int y) {
        return map[x][y] == '.' || map[x][y] == '?' || hp[x][y] <= 0;
    }

    void dealDamage(int ex, int ey, int damage) {
        if (!inField(ex, ey)) {
            return;
        }
        if (!Utils.isEnemyChar(map[ex][ey])) {
            return;
        }

        if (hp[ex][ey] > 0) {
            if (damage >= hp[ex][ey]) {
                cur.killCnt++;
                enemyIsAlive[enemyIndex[ex][ey]] = false;
            }
            cur.damageSum += Math.min(damage, hp[ex][ey]);
        }
        hp[ex][ey] -= damage;
    }

    private void undealDamage(int ex, int ey, int damage) {
        if (!inField(ex, ey)) {
            return;
        }
        if (!Utils.isEnemyChar(map[ex][ey])) {
            return;
        }

        if (hp[ex][ey] + damage > 0) {
            if (hp[ex][ey] <= 0) {
                cur.killCnt--;
                enemyIsAlive[enemyIndex[ex][ey]] = true;
            }
            cur.damageSum -= Math.min(damage, hp[ex][ey] + damage);
        }
        hp[ex][ey] += damage;
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
        for (Cell pos : enemyPositions) {
            int ex = pos.x, ey = pos.y;
            if (hp[ex][ey] > 0 && canShoot(cur.x, cur.y, ex, ey, Math.min(cur.stance.ordinal(), stances[ex][ey].ordinal()))) {
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
        for (Cell pos : enemyPositions) {
            tryThrowGrenade(pos.x, pos.y);
            tryThrowGrenade(pos.x + 1, pos.y);
            tryThrowGrenade(pos.x - 1, pos.y);
            tryThrowGrenade(pos.x, pos.y + 1);
            tryThrowGrenade(pos.x, pos.y - 1);
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
        for (int i = 0; i < enemyPositions.size(); i++) {
            Cell enemy = enemyPositions.get(i);
            if (hp[enemy.x][enemy.y] <= 0) {
                continue;
            }
            r += (10000 - sqrDistSum[enemy.x][enemy.y] + 10000 * numberOfTeammatesWhoCanShoot[i]) * (Utils.INITIAL_TROOPER_HP * 2 - hp[enemy.x][enemy.y]); // =)
        }
        return r;
    }

    private int getMinHp() {
        int mi = cur.selfHp;
        for (Cell cell : allyPositions) {
            mi = Math.min(mi, hp[cell.x][cell.y]);
        }
        return mi;
    }

    private int getDistToTeammatesSum() {
        int r = 0;
        int minHp = Integer.MAX_VALUE;
        for (Cell cell : allyPositions) {
            minHp = Math.min(minHp, hp[cell.x][cell.y]);
        }
        for (int i = 0; i < allyPositions.size(); i++) {
            Cell cell = allyPositions.get(i);
            int d = bfsDistFromTeammateForHealing.get(i)[cur.x][cur.y];
            if (hp[cell.x][cell.y] == minHp) {
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
