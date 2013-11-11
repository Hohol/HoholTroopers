import model.TrooperType;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static model.TrooperType.*;
import static org.testng.Assert.assertEquals;

@Test
public class MaxHealingPlanComputerTest {
    Utils utils = new Utils(Utils.hardcodedGame, TrooperParameters.HARDCODED_TROOPER_PARAMETERS);
    int[] hp = new int[TrooperType.values().length];

    @Test
    void testEmpty() {
        setHp(FIELD_MEDIC, 100);

        check(
                5,
                new String[]{
                        "F"
                }

                //empty
                ,
                false);
    }

    @Test
    void testTrivial() {
        setHp(FIELD_MEDIC, 97);
        check(
                5,
                new String[]{
                        "F"
                },

                false, MyAction.HEAL_SELF
        );


        setHp(FIELD_MEDIC, 87);
        check(
                5,
                new String[]{
                        "F"
                },

                false, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF
        );

        setHp(FIELD_MEDIC, 88);
        check(
                5,
                new String[]{
                        "F"
                },

                false, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF
        );

        setHp(FIELD_MEDIC, 89);
        check(
                5,
                new String[]{
                        "F"
                },

                false, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF
        );
    }

    @Test (enabled = true)
    void testHealTeammate() {
        setHp(FIELD_MEDIC, 100);
        setHp(SOLDIER, 95);
        check(
                5,
                new String[] {
                        "FS"
                },

                false, MyAction.HEAL_EAST
        );

        setHp(FIELD_MEDIC, 90);
        setHp(COMMANDER, 90);
        check(
                1,
                new String[] {
                        "FC"
                },

                false, MyAction.HEAL_EAST
        );

        setHp(FIELD_MEDIC, 90);
        setHp(COMMANDER, 90);
        check(
                1,
                new String[] {
                        "F",
                        "C"
                },

                false, MyAction.HEAL_SOUTH
        );

        setHp(FIELD_MEDIC, 98);
        setHp(COMMANDER, 99);
        check(
                1,
                new String[] {
                        "F",
                        "C"
                },
                false,

                MyAction.HEAL_SELF
        );

        setHp(FIELD_MEDIC, 100);
        setHp(COMMANDER, 85);
        setHp(SOLDIER, 90);
        check(
                6,
                new String[] {
                        ".C",
                        "SF"
                },

                false, MyAction.HEAL_NORTH, MyAction.HEAL_NORTH, MyAction.HEAL_NORTH, MyAction.HEAL_WEST, MyAction.HEAL_WEST
        );
    }

    @Test
    void testFieldRation() {
        setHp(FIELD_MEDIC, 10);
        check(
                2,
                new String[] {
                        "F"
                },
                true,

                MyAction.EAT_FIELD_RATION, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF
        );

        setHp(FIELD_MEDIC, 100);
        check(
                2,
                new String[] {
                        "F"
                },
                true

                //empty
        );

        setHp(FIELD_MEDIC, 90);
        setHp(COMMANDER, 90);
        setHp(SOLDIER, 90);

        check(
                2,
                new String[] {
                        ".C",
                        "SF"
                },
                true,

                MyAction.EAT_FIELD_RATION, MyAction.HEAL_NORTH, MyAction.HEAL_NORTH, MyAction.HEAL_WEST, MyAction.HEAL_WEST, MyAction.HEAL_SELF
        );
    }

    @Test
    void testMove() {
        setHp(FIELD_MEDIC, 100);
        setHp(SOLDIER, 95);
        check(
                2,
                new String[] {
                        "F.S"
                },
                false

                //empty
        );

        setHp(FIELD_MEDIC, 100);
        setHp(SOLDIER, 95);
        check(
                3,
                new String[] {
                        "F.S"
                },
                false,

                MyAction.MOVE_EAST, MyAction.HEAL_EAST
        );

        setHp(FIELD_MEDIC, 100);
        setHp(SOLDIER, 95);
        setHp(COMMANDER, 95);
        check(
                4,
                new String[] {
                        ".C.",
                        "S.F"
                },
                false,

                MyAction.MOVE_WEST, MyAction.HEAL_NORTH, MyAction.HEAL_WEST
        );

        setHp(FIELD_MEDIC, 100);
        setHp(SOLDIER, 95);
        check(
                10,
                new String[] {
                        "...",
                        "S#F"
                },
                false,

                MyAction.MOVE_NORTH, MyAction.MOVE_WEST, MyAction.MOVE_WEST, MyAction.HEAL_SOUTH
        );

        setHp(FIELD_MEDIC, 100);
        setHp(SOLDIER, 95);
        check(
                10,
                new String[] {
                        ".#.",
                        "S#F"
                },
                false

                //empty
        );

        setHp(FIELD_MEDIC, 100);
        setHp(SOLDIER, 95);
        setHp(COMMANDER, 90);
        check(
                4,
                new String[] {
                        ".C",
                        "F#",
                        ".S"
                },
                false,

                MyAction.MOVE_NORTH, MyAction.HEAL_EAST, MyAction.HEAL_EAST
        );
    }

    private void setHp(TrooperType trooper, int val) {
        hp[trooper.ordinal()] = val;
    }

    private void check(int actionPoints, String[] map, boolean holdingFieldRation, MyAction... expectedAr) {
        char[][] cmap = createMap(map);
        List<MyAction> actual = new MaxHealingPlanComputer(
                actionPoints,
                cmap,
                hp,
                holdingFieldRation,
                utils
        ).getActions();
        List<MyAction> expected = Arrays.asList(expectedAr);
        assertEquals(
                actual,
                expected,
                String.format("\n\nActual: %s \nExpected: %s\n\n", actual, expected)
        );
    }

    private char[][] createMap(String[] map) {
        char[][] r = new char[map.length][];
        for (int i = 0; i < map.length; i++) {
            r[i] = map[i].toCharArray();
        }
        return r;
    }
}
