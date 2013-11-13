import static model.TrooperStance.*;

import model.TrooperStance;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;

@Test
public class AttackPlanComputerTest {
    char[][] map;
    int[][] hp;
    TrooperStance[][] stances;

    @Test
    void testEmpty() {
        setMap(
                "S"
        );
        check(
                0,
                0, 0
                //empty
                ,
                STANDING, false, false);
    }

    @Test
    void testShoot() {
        setMap(
                "Ss"
        );
        addEnemy(1, 0, 100, STANDING);

        check(
                4,
                0, 0,
                STANDING, false, false, MyMove.shoot(1, 0)
        );
        //-----------------------
        setMap(
                "S..s",
                "....",
                "c..."
        );
        addEnemy(3, 0, 100, STANDING);
        addEnemy(0, 2, 30, STANDING);

        check(
                4,
                0, 0,
                STANDING, false, false, MyMove.shoot(0, 2)
        );

        //----------------------
        setMap(
                "S..s",
                "....",
                "c..."
        );
        addEnemy(3, 0, 1, STANDING);
        addEnemy(0, 2, 50, STANDING);

        check(
                4,
                0, 0,
                STANDING, false, false, MyMove.shoot(3, 0)
        );

        //----------------------
        setMap(
                "S..s",
                "....",
                "c..."
        );
        addEnemy(3, 0, 1, STANDING);
        addEnemy(0, 2, 2, STANDING);

        check(
                4,
                0, 0,
                STANDING, false, false, MyMove.shoot(0, 2)
        );
    }

    @Test
    void testObstacle() {
        setMap(
                "S.3.s"
        );
        addEnemy(4, 0, 100, STANDING);

        check(
                4,
                0, 0,
                STANDING,
                false, false);

        //----------------------
        setMap(
                "..f..",
                "..3..",
                "s.C3f",
                ".....",
                "..c.."
        );
        addEnemy(2, 0, 1, STANDING);
        addEnemy(4, 2, 1, STANDING);
        addEnemy(0, 2, 16, STANDING);
        addEnemy(2, 4, 15, STANDING);

        check(
                3,
                2, 2,
                STANDING,

                false, false, MyMove.shoot(2, 4)
        );

        //----------------------

        setMap(
                "S3.s"
        );
        addEnemy(3, 0, 1, STANDING);

        check(
                12,
                0, 0,
                STANDING,
                false,
                false
        );
    }

    @Test
    void testHeights() {
        //----------------------
        setMap(
                "..f..",
                "..2..",
                "s.C3f",
                ".....",
                "..c.."
        );
        addEnemy(2, 0, 1, STANDING);
        addEnemy(4, 2, 1, STANDING);
        addEnemy(0, 2, 16, STANDING);
        addEnemy(2, 4, 16, STANDING);

        check(
                3,
                2, 2,
                STANDING,

                false, false, MyMove.shoot(2, 0)
        );

        //----------------------
        setMap(
                "f",
                "2",
                "C"
        );
        addEnemy(0, 0, 1, STANDING);

        check(
                3,
                0, 2,
                STANDING,

                false, false, MyMove.shoot(0, 0)
        );

        //----------------------
        setMap(
                "..f..",
                "..2..",
                "s.C1f",
                ".....",
                "..c.."
        );
        addEnemy(2, 0, 1, STANDING);
        addEnemy(4, 2, 1, KNEELING);
        addEnemy(0, 2, 16, STANDING);
        addEnemy(2, 4, 16, STANDING);

        check(
                6,
                2, 2,
                STANDING,

                false, false, MyMove.shoot(2, 0), MyMove.shoot(4, 2)
        );
    }

    @Test
    void testTooFar() {
        setMap(
                "F.....s"
        );
        addEnemy(6, 0, 1, PRONE);

        check(
                2,
                0, 0,
                STANDING,
                false, false);
    }

    @Test
    void testMovement() {
        setMap(
                "F.....s"
        );
        addEnemy(6, 0, 1, PRONE);

        check(
                4,
                0, 0,
                STANDING,
                false, false, MyMove.MOVE_EAST, MyMove.shoot(6, 0)
        );
        //-----------
        setMap(
                "...",
                "F3c",
                "..."
        );
        addEnemy(2, 1, 1, PRONE);

        check(
                12,
                0, 1,
                STANDING,
                false, false, MyMove.MOVE_NORTH, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.shoot(2, 1)
        );

        //-----------

        setMap(
                "c........S........f"
        );
        addEnemy(0, 0, 25, PRONE);
        addEnemy(18, 0, 25, KNEELING);

        check(
                12,
                9, 0,
                STANDING,
                false, false, MyMove.MOVE_EAST, MyMove.shoot(18, 0)
        );

        //-----------

        setMap(
                "c.......C.......f"
        );
        addEnemy(0, 0, 15, PRONE);
        addEnemy(16, 0, 15, KNEELING);

        check(
                12,
                8, 0,
                STANDING,
                false, false, MyMove.MOVE_EAST, MyMove.shoot(16, 0), MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.shoot(0, 0)
        );

        //-----------
    }

    @Test
    void testEnemyBlocksPath() {
        setMap(
                "3c",
                "Fs"
        );
        addEnemy(1, 0, 1, PRONE);
        addEnemy(1, 1, 100, KNEELING);

        check(
                6,
                0, 1,
                STANDING,
                false,
                false, MyMove.shoot(1, 1), MyMove.shoot(1, 1), MyMove.shoot(1, 1)
        );
    }

    @Test
    void testMoveThroughDeadEnemy() {
        setMap(
                "3c",
                "Fs"
        );
        addEnemy(1, 0, 1, PRONE);
        addEnemy(1, 1, 9, KNEELING);

        check(
                6,
                0, 1,
                STANDING,
                false,
                false, MyMove.shoot(1, 1), MyMove.MOVE_EAST, MyMove.shoot(1, 0)
        );
    }

    @Test
    void testFieldRation() {
        setMap(
                "c........S........f"
        );
        addEnemy(0, 0, 25, PRONE);
        addEnemy(18, 0, 25, KNEELING);

        check(
                12,
                9, 0,
                STANDING,
                true,
                false, MyMove.MOVE_EAST, MyMove.shoot(18, 0), MyMove.EAT_FIELD_RATION, MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.shoot(0, 0)
        );
        //------

        setMap(
                "s.c",
                ".3.",
                "F.."
        );
        addEnemy(0, 0, 9, PRONE);
        addEnemy(2, 0, 9, KNEELING);

        check(
                5,
                0, 2,
                STANDING,
                true,
                false, MyMove.EAT_FIELD_RATION, MyMove.shoot(0, 0), MyMove.MOVE_NORTH, MyMove.MOVE_NORTH, MyMove.shoot(2, 0)
        );
    }

    @Test
    void testRaiseStance() {
        setMap(
                "S1c"
        );
        addEnemy(2, 0, 9, KNEELING);

        check(
                12,
                0, 0,
                PRONE,
                false,
                false, MyMove.RAISE_STANCE, MyMove.shoot(2, 0)
        );

        setMap(
                "S1c"
        );
        addEnemy(2, 0, 9, KNEELING);

        check(
                12,
                0, 0,
                PRONE,
                false,
                false, MyMove.RAISE_STANCE, MyMove.shoot(2, 0)
        );
    }

    @Test
    void testLowerStance() {
        setMap(
                "Fs"
        );
        addEnemy(1, 0, 120, KNEELING);

        check(
                12,
                0, 0,
                STANDING,
                true,
                false, MyMove.LOWER_STANCE, MyMove.LOWER_STANCE, MyMove.shoot(1, 0), MyMove.EAT_FIELD_RATION, MyMove.shoot(1, 0), MyMove.shoot(1, 0), MyMove.shoot(1, 0), MyMove.shoot(1, 0)
        );
    }

    @Test
    void testStance() {
        setMap(
                "c.S2.f"
        );
        addEnemy(0, 0, 35, STANDING);
        addEnemy(5, 0, 25, STANDING);

        check(
                6,
                2, 0,
                KNEELING,
                false,
                false, MyMove.LOWER_STANCE, MyMove.shoot(0, 0)
        );

        //----------

        setMap(
                "c.S2.f"
        );
        addEnemy(0, 0, 36, STANDING);
        addEnemy(5, 0, 25, STANDING);

        check(
                6,
                2, 0,
                KNEELING,
                false,
                false, MyMove.RAISE_STANCE, MyMove.shoot(5, 0)
        );

        //---------

        setMap(
                "c.S2.f"
        );
        addEnemy(0, 0, 35, STANDING);
        addEnemy(5, 0, 25, STANDING);

        check(
                12,
                2, 0,
                KNEELING,
                true,
                false, MyMove.shoot(0, 0), MyMove.shoot(0, 0), MyMove.EAT_FIELD_RATION, MyMove.RAISE_STANCE, MyMove.shoot(5, 0)
        );
    }

    @Test
    void testDoNotWasteFieldRation() {
        setMap(
                "S"
        );

        check(
                2,
                0, 0,
                KNEELING,
                true,
                false);
    }

    @Test
    void testGrenade() {
        setMap(
                "S.3.f"
        );
        addEnemy(4, 0, 80, STANDING);
        check(
                8,
                0, 0,
                KNEELING,
                false,
                true,
                MyMove.grenade(4, 0)
        );

        //------
        setMap(
                "Sf"
        );
        addEnemy(1, 0, 80, STANDING);
        check(
                8,
                0, 0,
                KNEELING,
                false,
                true,
                MyMove.shoot(1, 0), MyMove.shoot(1, 0)
        );

        //------
        setMap(
                ".Sf"
        );
        addEnemy(2, 0, 80, STANDING);
        check(
                10,
                1, 0,
                STANDING,
                false,
                true,
                MyMove.MOVE_WEST, MyMove.grenade(2, 0)
        );

        //------
        setMap(
                "S,f"
        );
        addEnemy(2, 0, 70, STANDING);
        check(
                10,
                0, 0,
                PRONE,
                false,
                true,
                MyMove.shoot(2,0), MyMove.shoot(2, 0)
        );
    }

    @Test
    void testDoNotDamageTeammateWithGrenade() {
        //------
        setMap(
                "S.fF"
        );
        addEnemy(2, 0, 80, STANDING);
        check(
                8,
                0, 0,
                STANDING,
                false,
                true,
                MyMove.shoot(2,0), MyMove.shoot(2, 0)
        );
        //---------
        setMap(
                "S.fF",
                "...."
        );
        addEnemy(2, 0, 80, STANDING);
        check(
                8,
                0, 0,
                STANDING,
                false,
                true,
                MyMove.grenade(2,1)
        );
    }

    @Test
    void testGrenadeForMassiveDamage() {
        //---------
        setMap(
                "....fc",
                "F....s",
                "......"
        );
        addEnemy(5, 0, 100, STANDING);
        addEnemy(4, 0, 100, STANDING);
        addEnemy(5, 1, 100, STANDING);
        check(
                8,
                0, 1,
                STANDING,
                true,
                true,
                MyMove.MOVE_NORTH, MyMove.EAT_FIELD_RATION, MyMove.grenade(5, 0)
        );
    }

    private void addEnemy(int x, int y, int newHp, TrooperStance stance) {
        char ch = map[x][y];
        if (ch < 'a' || ch > 'z') {
            throw new RuntimeException("No enemy trooper in cell (" + x + ", " + y + ")");
        }
        hp[x][y] = newHp;
        stances[x][y] = stance;
    }

    private void setMap(String... map) {
        this.map = Utils.toCharAndTranspose(map);
        hp = new int[this.map.length][this.map[0].length];
        stances = new TrooperStance[this.map.length][this.map[0].length];
    }

    private boolean[] getVisibilities() {
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

    private boolean visible(int viewerX, int viewerY, int objectX, int objectY, int stance) {
        if (viewerX == objectX) {
            return visibleVert(viewerX, viewerY, objectY, stance);
        }
        if (viewerY == objectY) {
            return visibleHor(viewerY, viewerX, objectX, stance);
        }
        return false;
    }

    private boolean visibleHor(int y, int x1, int x2, int stance) {
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

    private boolean visibleVert(int x, int y1, int y2, int stance) {
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

    private int height(char ch) {
        if (!Character.isDigit(ch)) {
            return 0;
        }
        return ch - '0';
    }

    private void check(
            int actionPoints,
            int x,
            int y,
            TrooperStance stance,
            boolean holdingFieldRation,
            boolean holdingGrenade, MyMove... expectedAr
    ) {
        if (!Utils.isCapitalLetter(map[x][y])) {
            throw new RuntimeException("No allied trooper in cell (" + x + ", " + y + ")");
        }
        List<MyMove> actual = new AttackPlanComputer(
                actionPoints,
                x,
                y,
                map,
                hp,
                holdingFieldRation,
                holdingGrenade,
                stance,
                getVisibilities(),
                stances,
                Utils.HARDCODED_UTILS
        ).getActions();
        List<MyMove> expected = Arrays.asList(expectedAr);
        assertEquals(
                actual,
                expected,
                String.format("\n\nActual: %s \nExpected: %s\n\n", actual, expected)
        );
    }
}