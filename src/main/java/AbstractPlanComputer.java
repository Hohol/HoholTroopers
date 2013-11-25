import model.BonusType;
import model.Game;

import model.TrooperType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static model.TrooperStance.PRONE;
import static model.TrooperStance.STANDING;

public abstract class AbstractPlanComputer <S extends AbstractState> {
    protected static final long MAX_RECURSIVE_CALLS = 3000000;
    protected final int m;
    protected final int n;
    protected final char[][] map;
    protected final Game game;
    protected final Utils utils;
    protected final List<MutableTrooper> teammates; //here, unlike MyStrategy, teammates does not contain self
    protected S cur, best;
    protected TrooperType selfType;
    protected boolean[] visibilities;
    long recursiveCallsCnt;
    BonusType[][] bonuses;
    MutableTrooper[][] troopers;

    public AbstractPlanComputer(char[][] map, Utils utils, List<MutableTrooper> teammates, boolean[] visibilities, BonusType[][] bonuses, MutableTrooper[][] troopers) {
        m = map[0].length;
        n = map.length;
        this.map = map;
        this.game = utils.getGame();
        this.utils = utils;
        this.teammates = teammates;
        this.visibilities = visibilities;
        this.bonuses = bonuses;
        this.troopers = troopers;
    }

    protected void addAction(MyMove action) {
        cur.actions.add(action);
    }

    protected void popAction() {
        cur.actions.remove(cur.actions.size() - 1);
    }

    protected boolean inField(int toX, int toY) {
        return toX >= 0 && toX < n && toY >= 0 && toY < m;
    }

    protected boolean isFree(int x, int y) {
        return map[x][y] == '.' || map[x][y] == '?' || troopers[x][y] != null && troopers[x][y].getHitpoints() <= 0;
    }
    protected abstract void tryAllActions();

    abstract void updateBest();

    protected void rec() {
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

        tryAllActions();

        bonuses[cur.x][cur.y] = bonus;
        cur.holdingGrenade = oldHoldingGrenade;
        cur.holdingFieldRation = oldHoldingFieldRation;
        cur.holdingMedikit = oldHoldingMedikit;
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

    public List<MyMove> getPlan() {
        prepare();
        rec();
        return best.actions;
    }

    protected void prepare() {
        selfType = troopers[cur.x][cur.y].getType();
        map[cur.x][cur.y] = '.';
        troopers[cur.x][cur.y] = null;
    }

    @SuppressWarnings("unused")
    boolean stopOn(MyMove... move) { //for debug only
        return Arrays.asList(move).equals(cur.actions);
    }

    protected List<int[][]> getDistToTeammates() {
        List<int[][]> r = new ArrayList<>();
        for (MutableTrooper ally : teammates) {
            int[][] dist = Utils.bfsByMap(map, ally.getX(), ally.getY());
            r.add(dist);
        }
        return r;
    }

    protected List<int[][]> getDistWithoutTeammates() {
        char[][] mapWithoutTeammates = new char[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                mapWithoutTeammates[i][j] = map[i][j];
                if (Utils.isTeammateChar(map[i][j])) {
                    mapWithoutTeammates[i][j] = '.';
                }
            }
        }
        List<int[][]> distWithoutTeammates = new ArrayList<>();
        for (int allyIndex = 0; allyIndex < teammates.size(); allyIndex++) {
            MutableTrooper ally = teammates.get(allyIndex);
            int[][] dist = Utils.bfsByMap(mapWithoutTeammates, ally.getX(), ally.getY());
            distWithoutTeammates.add(dist);
        }
        return distWithoutTeammates;
    }

    protected boolean visible(int viewerX, int viewerY, int objectX, int objectY, int stance) {
        int width = n;
        int height = m;
        int stanceCount = Utils.NUMBER_OF_STANCES;
        return visibilities[viewerX * height * width * height * stanceCount
                + viewerY * width * height * stanceCount
                + objectX * height * stanceCount
                + objectY * stanceCount
                + stance];
    }

    protected boolean reachable(int viewerX, int viewerY, int objectX, int objectY, int stance, int range) {
        if (Utils.sqrDist(viewerX, viewerY, objectX, objectY) > Utils.sqr(range)) {
            return false;
        }
        return visible(viewerX, viewerY, objectX, objectY, stance);
    }
}
