import model.TrooperStance;

import static model.TrooperStance.*;

import java.util.ArrayList;
import java.util.List;

public class AttackPlanComputer extends PlanComputer<AttackState> {
    private final List<Cell> enemyPositions = new ArrayList<>();
    private boolean[] visibilities;
    TrooperStance[][] stances;
    private int[][] hp;

    public AttackPlanComputer(
            int actionPoints,
            int x,
            int y,
            char[][] map,
            int[][] hp,
            boolean holdingFieldRation,
            boolean holdingGrenade,
            TrooperStance stance,
            boolean[] visibilities,
            TrooperStance[][] stances,
            Utils utils
    ) {
        super(map, utils);
        this.hp = hp;
        this.visibilities = visibilities;
        this.stances = stances;

        cur = new AttackState(new ArrayList<MyMove>(), actionPoints, holdingFieldRation, holdingGrenade, 0, 0, stance, x, y);

        prepare();

        rec();
    }

    private void prepare() {
        selfType = Utils.getTrooperTypeByChar(map[cur.x][cur.y]);
        map[cur.x][cur.y] = '.';
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                char ch = map[i][j];
                if (Utils.isSmallLetter(ch)) {
                    enemyPositions.add(new Cell(i, j));
                } else {
                    hp[i][j] = Integer.MAX_VALUE;
                }
            }
        }
    }

    public List<MyMove> getActions() {
        return best.actions;
    }

    protected void rec() {
        updateBest();
        tryThrowGrenade();
        tryEatFieldRation();
        tryShoot();
        tryMove();
        tryRaiseStance();
        tryLowerStance();
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

    private void tryThrowGrenade(int x, int y) {
        if (canThrowGrenade(x, y)) {
            throwGrenade(x, y);
            rec();
            unthrowGrenade(x, y);
        }
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

    private boolean forbidden(int x, int y) {
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

    @Override
    protected boolean freeCell(int x, int y) {
        return map[x][y] == '.' || hp[x][y] <= 0;
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

    private boolean canShoot(int viewerX, int viewerY, int objectX, int objectY, int stance) {
        if (Utils.sqrDist(viewerX, viewerY, objectX, objectY) > Utils.sqr(utils.getShootRange(selfType))) {
            return false;
        }
        int width = map.length;
        int height = map[0].length;
        int stanceCount = Utils.NUMBER_OF_STANCES;
        return visibilities[viewerX * height * width * height * stanceCount
                + viewerY * width * height * stanceCount
                + objectX * height * stanceCount
                + objectY * stanceCount
                + stance];
    }

    private void shoot(int ex, int ey) {
        addAction(MyMove.shoot(ex, ey));

        int damage = utils.getShootDamage(selfType, cur.stance);
        cur.actionPoints -= utils.getShootCost(selfType);
        dealDamage(ex, ey, damage);
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

    private void undealDamage(int ex, int ey, int damage) {
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

    private void unshoot(int ex, int ey) {
        int damage = utils.getShootDamage(selfType, cur.stance);
        cur.actionPoints += utils.getShootCost(selfType);

        undealDamage(ex, ey, damage);

        popAction();
    }


    private void updateBest() {
        if (cur.better(best)) {
            best = new AttackState(cur);
        }
    }
}
