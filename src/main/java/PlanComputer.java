import model.BonusType;
import model.Game;
import model.TrooperStance;
import model.TrooperType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static model.TrooperStance.PRONE;
import static model.TrooperStance.STANDING;
import static model.TrooperType.FIELD_MEDIC;

public class PlanComputer {

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
    private int[][] helpFactor;
    private int[][] helpDist;

    public PlanComputer(char[][] map, Utils utils, int[][] hp, BonusType[][] bonuses, TrooperStance[][] stances, boolean[] visibilities, State state) {
        this.map = map;
        this.utils = utils;
        this.game = utils.getGame();
        this.hp = hp;
        this.bonuses = bonuses;
        this.stances = stances;
        this.visibilities = visibilities;
        this.cur = state;
        this.hp = hp;
        prepare();
        rec();
    }

    private void prepare() {
        selfType = Utils.getTrooperTypeByChar(map[cur.x][cur.y]);
        map[cur.x][cur.y] = '.';
        sqrDistSum = new int[map.length][map[0].length];
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                char ch = map[i][j];
                if (Utils.isEnemyChar(ch)) {
                    enemyPositions.add(new Cell(i, j));
                } else if (Utils.isTeammateChar(ch)) {
                    allyPositions.add(new Cell(i, j));
                    updateSqrDistSum(i, j);
                } else {
                    hp[i][j] = Integer.MAX_VALUE;
                }
            }
        }
        bfsDistFromTeammateForHealing = new ArrayList<>();
        for (Cell cell : allyPositions) {
            int[][] curDist = Utils.bfsByMap(map, cell.x, cell.y);
            bfsDistFromTeammateForHealing.add(curDist);
            for (int i = 0; i < map.length; i++) {
                for (int j = 0; j < map[i].length; j++) {
                    if (curDist[i][j] > MyStrategy.MAX_DISTANCE_MEDIC_SHOULD_HEAL) {
                        curDist[i][j] = Utils.UNREACHABLE;
                    }
                }
            }
        }
        prepareHelp();
    }

    private void prepareHelp() {
        helpFactor = new int[map.length][map[0].length];
        for (Cell cell : allyPositions) {
            TrooperType type = Utils.getTrooperTypeByChar(map[cell.x][cell.y]);
            for (Cell e : enemyPositions) {
                int stance = stances[cell.x][cell.y].ordinal();
                if (canShoot(cell.x, cell.y, e.x, e.y, stance, type)) {
                    for (int i = 0; i < map.length; i++) {
                        for (int j = 0; j < map[i].length; j++) {
                            if (!freeCell(i, j)) {
                                continue;
                            }
                            if (canShoot(i, j, e.x, e.y, stances[e.x][e.y].ordinal(), selfType)) {
                                helpFactor[i][j]++;
                            }
                        }
                    }
                }
            }
        }
        helpDist = Utils.bfsForHelp(map, helpFactor);
    }

    void updateBest() {

        //todo то, что вычисляется за O(1), можно и не хранить

        cur.distSum = getDistToTeammatesSum();
        cur.minHp = getMinHp();
        cur.focusFireParameter = getFocusFireParameter();
        cur.helpFactor = helpFactor[cur.x][cur.y];
        cur.helpDist = helpDist[cur.x][cur.y];
        if (cur.better(best, selfType)) {
            best = new State(cur);
        }
    }


    private void addAction(MyMove action) {
        cur.actions.add(action);
    }

    private void popAction() {
        cur.actions.remove(cur.actions.size() - 1);
    }

    private boolean inField(int toX, int toY) {
        return toX >= 0 && toX < map.length && toY >= 0 && toY < map[0].length;
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
            if (!freeCell(toX, toY)) {
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

    private boolean freeCell(int x, int y) {
        return map[x][y] == '.' || hp[x][y] <= 0;
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
                cur.killedCnt++;
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
                cur.killedCnt--;
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
        int width = map.length;
        int height = map[0].length;
        int stanceCount = Utils.NUMBER_OF_STANCES;
        return visibilities[viewerX * height * width * height * stanceCount
                + viewerY * width * height * stanceCount
                + objectX * height * stanceCount
                + objectY * stanceCount
                + stance];
    }

    private boolean canShoot(int viewerX, int viewerY, int objectX, int objectY, int stance, TrooperType type) {
        if (Utils.sqrDist(viewerX, viewerY, objectX, objectY) > Utils.sqr(utils.getShootRange(type))) {
            return false;
        }
        return visible(viewerX, viewerY, objectX, objectY, stance);
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
        tryEatFieldRation();
        tryHealAsMedic();
        tryHealWithMedikit();
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
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                sqrDistSum[i][j] += Utils.sqrDist(x, y, i, j);
            }
        }
    }

    private int getFocusFireParameter() {
        int r = 0;
        for (Cell enemy : enemyPositions) {
            if (hp[enemy.x][enemy.y] <= 0) {
                continue;
            }
            r += (10000 - sqrDistSum[enemy.x][enemy.y]) * (Utils.INITIAL_TROOPER_HP * 2 - hp[enemy.x][enemy.y]);
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
    boolean stopOn(MyMove ...move) { //for debug only
        return Arrays.asList(move).equals(cur.actions);
    }
}
