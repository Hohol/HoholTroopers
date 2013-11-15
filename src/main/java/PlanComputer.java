import model.BonusType;
import model.Game;
import model.TrooperStance;
import model.TrooperType;

import java.util.ArrayList;
import java.util.List;

import static model.TrooperStance.PRONE;
import static model.TrooperStance.STANDING;
import static model.TrooperType.FIELD_MEDIC;

public abstract class PlanComputer <S extends State> {
    protected final char[][] map;
    protected final Game game;
    protected final Utils utils;
    protected final List<Cell> enemyPositions = new ArrayList<>();
    protected final List<Cell> allyPositions = new ArrayList<>();
    protected S cur, best;
    protected TrooperType selfType;
    protected int[][] hp;
    protected boolean[] visibilities;
    BonusType[][] bonuses;
    TrooperStance[][] stances;

    public PlanComputer(char[][] map, Utils utils, int[][] hp, BonusType[][] bonuses, TrooperStance[][] stances, boolean[] visibilities) {
        this.map = map;
        this.utils = utils;
        this.game = utils.getGame();
        this.hp = hp;
        this.bonuses = bonuses;
        this.stances = stances;
        this.visibilities = visibilities;
    }

    abstract protected void rec();
    abstract protected void updateBest();


    protected void addAction(MyMove action) {
        cur.actions.add(action);
    }

    protected void popAction() {
        cur.actions.remove(cur.actions.size() - 1);
    }

    protected boolean inField(int toX, int toY) {
        return toX >= 0 && toX < map.length && toY >= 0 && toY < map[0].length;
    }

    protected void tryMove() {
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

    protected void tryEatFieldRation() {
        if (cur.holdingFieldRation && cur.actionPoints >= game.getFieldRationEatCost() && cur.actionPoints < utils.getInitialActionPoints(selfType)) {
            addAction(MyMove.EAT_FIELD_RATION);
            int oldActionPoints = cur.actionPoints;
            cur.actionPoints = utils.actionPointsAfterEatingFieldRation(selfType, cur.actionPoints, game);
            cur.holdingFieldRation = false;
            rec();
            cur.holdingFieldRation = true;
            cur.actionPoints = oldActionPoints;
            popAction();
        }
    }

    public S getPlan() {
        return best;
    }

    protected void newTryHeal(int healValue, int healCost, MyMove healAction, boolean avoidOverheal, Cell c) {
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

    protected void tryHealSelfWithMedikit() {
        if (!cur.holdingMedikit) {
            return;
        }
        int healValue = game.getMedikitHealSelfBonusHitpoints();
        cur.holdingMedikit = false;
        newTryHeal(healValue, game.getMedikitUseCost(), MyMove.USE_MEDIKIT_SELF, true, null);
        cur.holdingMedikit = true;
    }

    protected void tryHealTeammates(MyMove[] heals, int healValue, int healCost, boolean avoidOverheal) {
        for (MyMove heal : heals) {
            int toX = cur.x + heal.getDx();
            int toY = cur.y + heal.getDy();
            if (!inField(toX, toY)) {
                continue;
            }
            char targetChar = map[toX][toY];
            if (!Utils.isCapitalLetter(targetChar)) {
                continue;
            }
            newTryHeal(healValue, healCost, heal, avoidOverheal, new Cell(toX, toY));
        }
    }

    protected void tryHealWithMedikit() {
        if (cur.holdingMedikit) {
            cur.holdingMedikit = false;
            tryHealTeammates(MyMove.directedMedikitUses, game.getMedikitBonusHitpoints(), game.getMedikitUseCost(), true);
            cur.holdingMedikit = true;
        }
        tryHealSelfWithMedikit();
    }

    protected void tryHealSelfWithAbility() {
        newTryHeal(game.getFieldMedicHealSelfBonusHitpoints(), game.getFieldMedicHealCost(), MyMove.HEAL_SELF, false, null);
    }

    protected void tryHealAsMedic() {
        if (selfType != FIELD_MEDIC) {
            return;
        }
        tryHealTeammates(MyMove.directedHeals, game.getFieldMedicHealBonusHitpoints(), game.getFieldMedicHealCost(), false);
        tryHealSelfWithAbility();
    }

    protected boolean freeCell(int x, int y) {
        return map[x][y] == '.' || hp[x][y] <= 0;
    }

    void dealDamage(int ex, int ey, int damage) {
        if (!inField(ex, ey)) {
            return;
        }
        if (!Utils.isSmallLetter(map[ex][ey])) {
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

    protected void undealDamage(int ex, int ey, int damage) {
        if (!inField(ex, ey)) {
            return;
        }
        if (!Utils.isSmallLetter(map[ex][ey])) {
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

    protected void unshoot(int ex, int ey) {
        int damage = utils.getShootDamage(selfType, cur.stance);
        cur.actionPoints += utils.getShootCost(selfType);

        undealDamage(ex, ey, damage);

        popAction();
    }

    protected void shoot(int ex, int ey) {
        addAction(MyMove.shoot(ex, ey));

        int damage = utils.getShootDamage(selfType, cur.stance);
        cur.actionPoints -= utils.getShootCost(selfType);
        dealDamage(ex, ey, damage);
    }

    protected boolean visible(int viewerX, int viewerY, int objectX, int objectY, int stance) {
        int width = map.length;
        int height = map[0].length;
        int stanceCount = Utils.NUMBER_OF_STANCES;
        return visibilities[viewerX * height * width * height * stanceCount
                + viewerY * width * height * stanceCount
                + objectX * height * stanceCount
                + objectY * stanceCount
                + stance];
    }

    protected boolean canShoot(int viewerX, int viewerY, int objectX, int objectY, int stance, TrooperType type) {
        if (Utils.sqrDist(viewerX, viewerY, objectX, objectY) > Utils.sqr(utils.getShootRange(type))) {
            return false;
        }
        return visible(viewerX, viewerY, objectX, objectY, stance);
    }

    protected boolean canShoot(int viewerX, int viewerY, int objectX, int objectY, int stance) {
        return canShoot(viewerX, viewerY, objectX, objectY, stance, selfType);
    }

    protected void tryShoot() {
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

    protected void tryLowerStance() {
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

    protected void tryRaiseStance() {
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

    protected void unthrowGrenade(int ex, int ey) {
        popAction();
        cur.actionPoints += game.getGrenadeThrowCost();
        cur.holdingGrenade = true;
        undealDamage(ex, ey, game.getGrenadeDirectDamage());
        undealDamage(ex + 1, ey, game.getGrenadeCollateralDamage());
        undealDamage(ex - 1, ey, game.getGrenadeCollateralDamage());
        undealDamage(ex, ey + 1, game.getGrenadeCollateralDamage());
        undealDamage(ex, ey - 1, game.getGrenadeCollateralDamage());
    }

    protected void throwGrenade(int ex, int ey) {
        addAction(MyMove.grenade(ex, ey));
        cur.actionPoints -= game.getGrenadeThrowCost();
        cur.holdingGrenade = false;
        dealDamage(ex, ey, game.getGrenadeDirectDamage());
        dealDamage(ex + 1, ey, game.getGrenadeCollateralDamage());
        dealDamage(ex - 1, ey, game.getGrenadeCollateralDamage());
        dealDamage(ex, ey + 1, game.getGrenadeCollateralDamage());
        dealDamage(ex, ey - 1, game.getGrenadeCollateralDamage());
    }

    protected boolean forbidden(int x, int y) {
        if (!inField(x, y)) {
            return false;
        }
        if (x == cur.x && y == cur.y) {
            return true;
        }
        if (Utils.isCapitalLetter(map[x][y])) {
            return true;
        }
        return false;
    }

    protected boolean canThrowGrenade(int ex, int ey) {
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

    protected void tryThrowGrenade(int x, int y) {
        if (canThrowGrenade(x, y)) {
            throwGrenade(x, y);
            rec();
            unthrowGrenade(x, y);
        }
    }

    protected void tryThrowGrenade() {
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
}
