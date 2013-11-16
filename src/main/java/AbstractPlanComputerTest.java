import model.BonusType;
import model.TrooperStance;
import model.TrooperType;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static model.TrooperStance.STANDING;
import static org.testng.Assert.assertEquals;

public class AbstractPlanComputerTest {
    protected BonusType[][] bonuses;
    TrooperStance[][] stances;
    int[][] hp;
    char[][] map;

    protected int height(char ch) {
        if (!Character.isDigit(ch)) {
            return 0;
        }
        return ch - '0';
    }

    protected boolean visibleVert(int x, int y1, int y2, int stance) {
        if (y1 > y2) {
            int tmp = y1;
            y1 = y2;
            y2 = tmp;
        }
        for (int y = y1; y <= y2; y++) {
            if (height(map[x][y]) > stance) {
                return false;
            }
        }
        return true;
    }

    protected boolean visibleHor(int y, int x1, int x2, int stance) {
        if (x1 > x2) {
            int tmp = x1;
            x1 = x2;
            x2 = tmp;
        }
        for (int x = x1; x <= x2; x++) {
            if (height(map[x][y]) > stance) {
                return false;
            }
        }
        return true;
    }

    protected boolean visible(int viewerX, int viewerY, int objectX, int objectY, int stance) {
        if (viewerX == objectX) {
            return visibleVert(viewerX, viewerY, objectY, stance);
        }
        if (viewerY == objectY) {
            return visibleHor(viewerY, viewerX, objectX, stance);
        }
        return false;
    }

    protected boolean[] getVisibilities() {
        int width = map.length;
        int height = map[0].length;
        int stanceCount = Utils.NUMBER_OF_STANCES;
        boolean[] r = new boolean[width * height * width * height * stanceCount];
        for (int viewerX = 0; viewerX < width; viewerX++) {
            for (int viewerY = 0; viewerY < height; viewerY++) {
                for (int objectX = 0; objectX < width; objectX++) {
                    for (int objectY = 0; objectY < height; objectY++) {
                        for (int stance = 0; stance < stanceCount; stance++) {
                            r[viewerX * height * width * height * stanceCount
                                    + viewerY * width * height * stanceCount
                                    + objectX * height * stanceCount
                                    + objectY * stanceCount
                                    + stance] = visible(viewerX, viewerY, objectX, objectY, stance);
                        }
                    }
                }
            }
        }
        return r;
    }

    protected void addBonuses() {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                bonuses[i][j] = Utils.getBonusTypeByChar(map[i][j]);
                if (bonuses[i][j] != null) {
                    map[i][j] = '.';
                }
            }
        }
    }

    protected void setMap(String... map) {
        this.map = Utils.toCharAndTranspose(map);
        hp = new int[this.map.length][this.map[0].length];
        stances = new TrooperStance[this.map.length][this.map[0].length];
        bonuses = new BonusType[this.map.length][this.map[0].length];
        addBonuses();
    }

    protected void addTrooper(int x, int y, int newHp, TrooperStance stance) {
        char ch = map[x][y];
        if (ch < 'a' || ch > 'z') {
            throw new RuntimeException("No enemy trooper in cell (" + x + ", " + y + ")");
        }
        hp[x][y] = newHp;
        stances[x][y] = stance;
    }

    protected void addBonus(int x, int y, BonusType bonus) {
        if (!Utils.isLetter(map[x][y])) {
            throw new RuntimeException("No trooper at cell(" + x + ", " + y + ")");
        }
        bonuses[x][y] = bonus;
    }

    protected void check(
            TrooperType selfType,
            int actionPoints,
            TrooperStance stance,
            boolean holdingFieldRation,
            boolean holdingGrenade,
            boolean holdingMedikit,
            MyMove... expectedAr
    ) {
        int x = -1, y = -1;
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (Utils.isLetter(map[i][j]) && hp[i][j] == 0) {
                    hp[i][j] = Utils.INITIAL_TROOPER_HP;
                    stances[i][j] = STANDING;
                    if (Utils.isTeammateChar(map[i][j]) && Utils.getTrooperTypeByChar(map[i][j]) == selfType) {
                        x = i;
                        y = j;
                    }
                }
            }
        }

        if (x == -1) {
            throw new RuntimeException("No allied " + selfType + " on the map");
        }

        List<MyMove> actual = new PlanComputer(
                map,
                Utils.HARDCODED_UTILS,
                hp,
                bonuses,
                stances,
                getVisibilities(),
                new State(actionPoints, holdingFieldRation, x, y, stance, Utils.INITIAL_TROOPER_HP, holdingMedikit, holdingGrenade)
        ).getPlan().actions;

        List<MyMove> expected = Arrays.asList(expectedAr);
        assertEquals(
                actual,
                expected,
                String.format("\n\nActual: %s \nExpected: %s\n\n", actual, expected)
        );
    }
}
