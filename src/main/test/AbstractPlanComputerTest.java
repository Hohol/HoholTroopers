import model.BonusType;
import model.TrooperStance;
import model.TrooperType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;

public abstract class AbstractPlanComputerTest {

    protected static final Utils utils = Utils.HARDCODED_UTILS;
    protected BonusType[][] bonuses;
    protected MutableTrooper[][] troopers;
    protected List<MutableTrooper> teammates;
    protected List<MutableTrooper> enemies;
    protected List<MyMove> prevActions = new ArrayList<>();
    char[][] map;
    int m;
    int n;
    MTBuilder[][] builders;
    protected boolean[] vision;

    protected void initBuilders() {
        builders = new MTBuilder[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                char ch = map[i][j];
                if (Utils.isLetter(ch)) {
                    builders[i][j] = new MTBuilder()
                            .x(i)
                            .y(j)
                            .type(Utils.getTrooperTypeByChar(ch));
                    if (Utils.isTeammateChar(map[i][j])) {
                        builders[i][j].teammate();
                    }
                }
            }
        }
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

    protected void addBonus(int x, int y, BonusType bonus) {
        if (!Utils.isLetter(map[x][y])) {
            throw new RuntimeException("No trooper in cell(" + x + ", " + y + ")");
        }
        bonuses[x][y] = bonus;
    }

    protected void setMap(String... smap) {
        map = Utils.toCharAndTranspose(smap);
        n = this.map.length;
        m = this.map[0].length;
        findVision();
        bonuses = new BonusType[this.map.length][this.map[0].length];
        initBuilders();
        prevActions = new ArrayList<>();
        addBonuses();
    }

    protected MTBuilder ally(TrooperType type) {
        int x = -1, y = -1;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (Utils.isTeammateChar(map[i][j]) && map[i][j] == Utils.getCharForTrooperType(type)) {
                    x = i;
                    y = j;
                }
            }
        }
        if (x == -1) {
            throw new RuntimeException("No allied " + type + " on the map");
        }
        return builders[x][y];
    }

    protected abstract List<MyMove> getActual(String moveOrder, MutableTrooper self);

    protected int height(char ch) {
        if (ch == '#') {
            ch = '3';
        }
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

    protected void setVisible(int viewerX, int viewerY, int objectX, int objectY, TrooperStance stance) {
        int width = map.length;
        int height = map[0].length;
        int stanceCount = Utils.NUMBER_OF_STANCES;
        for (int stanceIndex = stance.ordinal(); stanceIndex < stanceCount; stanceIndex++) {
            vision[viewerX * height * width * height * stanceCount
                    + viewerY * width * height * stanceCount
                    + objectX * height * stanceCount
                    + objectY * stanceCount
                    + stanceIndex] = true;
        }
    }

    private void findVision() {
        int width = map.length;
        int height = map[0].length;
        int stanceCount = Utils.NUMBER_OF_STANCES;
        vision = new boolean[width * height * width * height * stanceCount];
        for (int viewerX = 0; viewerX < width; viewerX++) {
            for (int viewerY = 0; viewerY < height; viewerY++) {
                for (int objectX = 0; objectX < width; objectX++) {
                    for (int objectY = 0; objectY < height; objectY++) {
                        for (int stance = 0; stance < stanceCount; stance++) {
                            vision[viewerX * height * width * height * stanceCount
                                    + viewerY * width * height * stanceCount
                                    + objectX * height * stanceCount
                                    + objectY * stanceCount
                                    + stance] = visible(viewerX, viewerY, objectX, objectY, stance);
                        }
                    }
                }
            }
        }
    }

    protected void prepareTroopers(TrooperType selfType) {
        troopers = new MutableTrooper[n][m];
        enemies = new ArrayList<>();
        teammates = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (Utils.isLetter(map[i][j])) {
                    troopers[i][j] = builders[i][j].build();
                }
            }
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (Utils.isTeammateChar(map[i][j]) && Utils.getTrooperTypeByChar(map[i][j]) == selfType) {
                    continue;
                }
                if (troopers[i][j] != null && troopers[i][j].isTeammate()) {
                    teammates.add(troopers[i][j]);
                }
                if (troopers[i][j] != null && !troopers[i][j].isTeammate()) {
                    enemies.add(troopers[i][j]);
                }
            }
        }
    }

    protected void check(TrooperType selfType, int actionPoints, String moveOrder, MyMove ...expectedAr) {
        MTBuilder selfBuilder = ally(selfType)
                .actionPoints(actionPoints);

        MutableTrooper self = selfBuilder.build();

        prepareTroopers(selfType);

        List<MyMove> actual = getActual(moveOrder, self);

        List<MyMove> expected = Arrays.asList(expectedAr);
        assertEquals(
                actual,
                expected,
                String.format("\n\nExpected: %s \nActual: %s\n\n", expected, actual)
        );
    }

    protected String getDefaultMoveOrder() {
        String r = "";
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                char ch = map[i][j];
                if(Utils.isLetter(ch)) {
                    ch = Character.toUpperCase(ch);
                    if(!r.contains(Character.toString(ch))) {
                        r += ch;
                    }
                }
            }
        }
        return r;
    }

    protected void check(
            TrooperType selfType,
            int actionPoints,
            MyMove... expectedAr
    ) {
        check(selfType, actionPoints, getDefaultMoveOrder(), expectedAr);
    }

    protected void setPrevActions(MyMove... actions) {
        prevActions = Arrays.asList(actions);
    }
}
