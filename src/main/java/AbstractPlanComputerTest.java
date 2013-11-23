import model.BonusType;
import model.TrooperStance;
import model.TrooperType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class AbstractPlanComputerTest {
    protected BonusType[][] bonuses;
    char[][] map;
    int n, m;

    protected void setMap(String... smap) {
        map = Utils.toCharAndTranspose(smap);
        n = this.map.length;
        m = this.map[0].length;
        bonuses = new BonusType[this.map.length][this.map[0].length];
        n = this.map.length;
        m = this.map[0].length;
        initBuilders();
        addBonuses();
    }

    private void initBuilders() {
        builders = new MTBuilder[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (Utils.isLetter(map[i][j])) {
                    builders[i][j] = new MTBuilder()
                            .x(i)
                            .y(j);
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

    protected MTBuilder enemy(TrooperType type) {
        int x = -1, y = -1;
        int cnt = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (Utils.isEnemyChar(map[i][j]) && Character.toUpperCase(map[i][j]) == Utils.getCharForTrooperType(type)) {
                    x = i;
                    y = j;
                    cnt++;
                }
            }
        }
        if (x == -1) {
            throw new RuntimeException("No enemy " + type + " on the map");
        }
        if (cnt > 1) {
            throw new RuntimeException("Multiple enemy " + type + " on the map");
        }

        return builders[x][y];
    }

    protected MTBuilder enemy(int x, int y) {
        if (!Utils.isEnemyChar(map[x][y])) {
            throw new RuntimeException("No enemy in cell (" + x + ", " + y + ")");
        }
        return builders[x][y];
    }

    protected void check(
            TrooperType selfType,
            int actionPoints,
            MyMove... expectedAr
    ) {
        MTBuilder selfBuilder = ally(selfType)
                .actionPoints(actionPoints);

        MutableTrooper self = selfBuilder.build();

        prepareTroopers(selfType);

        List<MyMove> actual = new PlanComputer(
                map,
                Utils.HARDCODED_UTILS,
                bonuses,
                getVisibilities(),
                false,
                false, troopers, teammates, enemies,

                new State(self)
        ).getPlan().actions;

        List<MyMove> expected = Arrays.asList(expectedAr);
        assertEquals(
                actual,
                expected,
                String.format("\n\nExpected: %s \nActual: %s\n\n", expected, actual)
        );
    }

    protected MutableTrooper[][] troopers;
    protected List<MutableTrooper> enemies, teammates;
    MTBuilder[][] builders;

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
}
