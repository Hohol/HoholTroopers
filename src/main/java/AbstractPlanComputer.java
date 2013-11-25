import model.BonusType;
import model.Game;
import model.TrooperStance;

import static model.TrooperType.*;

import model.TrooperType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static model.TrooperStance.*;

public abstract class AbstractPlanComputer {
    protected static final long MAX_RECURSIVE_CALLS = 3000000;
    protected final int m;
    protected final int n;
    protected final char[][] map;
    protected final Game game;
    protected final Utils utils;
    protected final List<MutableTrooper> teammates; //here, unlike MyStrategy, teammates does not contain self
    protected State best;
    protected State cur;
    protected TrooperType selfType;
    protected boolean[] visibilities;
    long recursiveCallsCnt;
    BonusType[][] bonuses;
    MutableTrooper[][] troopers;

    public AbstractPlanComputer(char[][] map, Utils utils, List<MutableTrooper> teammates, MutableTrooper self, boolean[] visibilities, BonusType[][] bonuses, MutableTrooper[][] troopers) {
        m = map[0].length;
        n = map.length;
        this.map = map;
        this.game = utils.getGame();
        this.utils = utils;
        this.teammates = teammates;
        this.cur = new State(self);
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
    abstract protected void rec();
}
