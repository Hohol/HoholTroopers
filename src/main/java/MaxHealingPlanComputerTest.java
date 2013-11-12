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
                false, false);
    }

    @Test
    void testTrivial() {
        setHp(FIELD_MEDIC, 97);
        check(
                5,
                new String[]{
                        "F"
                },

                false, false, MyAction.HEAL_SELF
        );


        setHp(FIELD_MEDIC, 87);
        check(
                5,
                new String[]{
                        "F"
                },

                false, false, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF
        );

        setHp(FIELD_MEDIC, 88);
        check(
                5,
                new String[]{
                        "F"
                },

                false, false, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF
        );

        setHp(FIELD_MEDIC, 89);
        check(
                5,
                new String[]{
                        "F"
                },

                false, false, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF
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

                false, false, MyAction.HEAL_EAST
        );

        setHp(FIELD_MEDIC, 90);
        setHp(COMMANDER, 90);
        check(
                1,
                new String[] {
                        "FC"
                },

                false, false, MyAction.HEAL_EAST
        );

        setHp(FIELD_MEDIC, 90);
        setHp(COMMANDER, 90);
        check(
                1,
                new String[] {
                        "F",
                        "C"
                },

                false, false, MyAction.HEAL_SOUTH
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

                false, MyAction.HEAL_SELF
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

                false, false, MyAction.HEAL_NORTH, MyAction.HEAL_NORTH, MyAction.HEAL_NORTH, MyAction.HEAL_WEST, MyAction.HEAL_WEST
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

                false, MyAction.EAT_FIELD_RATION, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF
        );

        setHp(FIELD_MEDIC, 100);
        check(
                2,
                new String[] {
                        "F"
                },
                true

                //empty
                ,
                false);

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

                false,

                MyAction.EAT_FIELD_RATION, MyAction.HEAL_SELF, MyAction.HEAL_NORTH, MyAction.HEAL_NORTH, MyAction.HEAL_WEST, MyAction.HEAL_WEST
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
                false,
                false,
                MyAction.MOVE_EAST
        );

        setHp(FIELD_MEDIC, 100);
        setHp(SOLDIER, 95);
        check(
                3,
                new String[] {
                        "F.S"
                },
                false,

                false, MyAction.MOVE_EAST, MyAction.HEAL_EAST
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

                false, MyAction.MOVE_WEST, MyAction.HEAL_NORTH, MyAction.HEAL_WEST
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

                false, MyAction.MOVE_NORTH, MyAction.MOVE_WEST, MyAction.MOVE_WEST, MyAction.HEAL_SOUTH
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
                ,
                false);

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

                false, MyAction.MOVE_NORTH, MyAction.HEAL_EAST, MyAction.HEAL_EAST
        );
    }

    @Test
    void testBugWithSelfHealing() {
        setHp(FIELD_MEDIC, 1);
        check(
                6,
                new String[] {
                        "F."
                },
                false,

                false, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF
                //not fucking [MOVE_EAST, HEAL_WEST, HEAL_WEST, HEAL_WEST, HEAL_WEST] !
        );
    }

    @Test
    void testMaximizeMinimalHp() {
        setHp(FIELD_MEDIC, 100);
        setHp(COMMANDER, 1);
        setHp(SOLDIER, 1);
        check(
                2,
                new String[] {
                        "FC",
                        "S."
                },
                false,

                false, MyAction.HEAL_EAST, MyAction.HEAL_SOUTH
        );

        setHp(FIELD_MEDIC, 19);
        setHp(SOLDIER, 20);
        check(
                5,
                new String[] {
                        "F.S",
                },
                false,

                false, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF
        );

        setHp(FIELD_MEDIC, 20);
        setHp(SOLDIER, 20);
        check(
                5,
                new String[] {
                        "F.S",
                },
                false,
                false,

                MyAction.MOVE_EAST, MyAction.HEAL_EAST, MyAction.HEAL_EAST, MyAction.HEAL_EAST
        );

        setHp(FIELD_MEDIC, 20);
        setHp(SOLDIER, 19);
        check(
                5,
                new String[] {
                        "F.S",
                },
                false,

                false, MyAction.MOVE_EAST, MyAction.HEAL_EAST, MyAction.HEAL_EAST, MyAction.HEAL_EAST
        );
    }

    @Test
    void testMedikit() {
        setHp(FIELD_MEDIC, 1);
        check(
                2,
                new String[] {
                        "F"
                },
                false,
                true,
                MyAction.USE_MEDIKIT_SELF
        );

        setHp(FIELD_MEDIC, 1);
        setHp(SOLDIER, 50);
        check(
                2,
                new String[] {
                        "FS"
                },
                false,
                true,

                MyAction.USE_MEDIKIT_EAST
        );

        setHp(FIELD_MEDIC, 1);
        check(
                4,
                new String[] {
                        "F"
                },
                false,
                true,

                MyAction.USE_MEDIKIT_SELF, MyAction.HEAL_SELF, MyAction.HEAL_SELF
        );

        setHp(FIELD_MEDIC, 50);
        setHp(COMMANDER, 50);
        check(
                7,
                new String[] {
                        "F..C"
                },
                false,
                true,

                MyAction.HEAL_SELF, MyAction.MOVE_EAST, MyAction.MOVE_EAST, MyAction.USE_MEDIKIT_EAST
        );
    }

    @Test
    void avoidOverHealWithMedikit() {
        setHp(FIELD_MEDIC, 80);
        check(
                2,
                new String[] {
                        "F"
                },
                false,
                true,
                MyAction.HEAL_SELF, MyAction.HEAL_SELF
        );
    }

    @Test
    void testMinimizeSumOfDistance() {
        setHp(SOLDIER, 1);
        setHp(FIELD_MEDIC, 100);
        setHp(COMMANDER, 100);
        check(
                3,
                new String[] {
                        ".S",
                        "F.",
                        ".C"
                },
                false,
                false,
                MyAction.MOVE_EAST, MyAction.HEAL_NORTH
        );

        setHp(SOLDIER, 1);
        setHp(FIELD_MEDIC, 100);
        setHp(COMMANDER, 100);
        check(
                3,
                new String[] {
                        ".#C",
                        ".S.",
                        "F.."
                },
                false,
                false,
                MyAction.MOVE_EAST, MyAction.HEAL_NORTH
        );

        setHp(SOLDIER, 100);
        setHp(FIELD_MEDIC, 100);
        setHp(COMMANDER, 100);
        check(
                12,
                new String[] {
                        "..C",
                        ".S.",
                        "F.."
                },
                false,
                false,
                MyAction.MOVE_NORTH, MyAction.MOVE_NORTH, MyAction.MOVE_EAST
        );
    }

    @Test
    void testMinimizeSumOfDistancePrioritizeInjured() {
        setHp(SOLDIER, 1);
        setHp(FIELD_MEDIC, 100);
        setHp(COMMANDER, 100);
        check(
                2,
                new String[] {
                        "S.F.C"
                },
                false,
                false,
                MyAction.MOVE_WEST
        );

        setHp(SOLDIER, 100);
        setHp(FIELD_MEDIC, 100);
        setHp(COMMANDER, 1);
        check(
                2,
                new String[] {
                        "S.F.C"
                },
                false,
                false,
                MyAction.MOVE_EAST
        );

        setHp(SNIPER, 1);
        setHp(COMMANDER, 2);
        setHp(SOLDIER, 4);
        setHp(FIELD_MEDIC, 100);

        check(
                2,
                new String[] {
                        "..S..",
                        ".....",
                        "R.F.C",
                        ".....",
                        "....."
                },
                false,
                false,
                MyAction.MOVE_WEST
        );

        setHp(SNIPER, 1);
        setHp(COMMANDER, 2);
        setHp(SCOUT, 3);
        setHp(SOLDIER, 4);
        setHp(FIELD_MEDIC, 100);

        check(
                2,
                new String[] {
                        "..S..",
                        ".....",
                        "R.F.C",
                        ".....",
                        "..T.."
                },
                false,
                false,
                MyAction.MOVE_WEST
        );
    }

    @Test
    void testDoNotTryToReachUnreachable() {
        setHp(COMMANDER, 1);
        setHp(FIELD_MEDIC, 100);

        check(
                12,
                new String[] {
                        "........#.......",
                        "...F....#....C..",
                        "........#.......",
                },
                false,
                false
        );
    }

    @Test
    void testDoNotGoTooFar() {
        setHp(COMMANDER, 1);
        setHp(SOLDIER, 100);
        setHp(FIELD_MEDIC, 100);

        check(
                12,
                new String[] {
                        "F...............",
                        "S##############.",
                        "C..............."
                },
                false,
                false
        );
    }

    @Test (enabled = false) //manual
    void testTooSlow() {
        //setHp(SNIPER, 1);
        setHp(COMMANDER, 2);
        //setHp(SCOUT, 3);
        setHp(SOLDIER, 4);
        setHp(FIELD_MEDIC, 1);

        check(
                12,
                new String[] {
                        "..........",
                        "..S.......",
                        "..F.......",
                        "..........",
                        ".........."
                },
                true,
                true
        );
    }

    private void setHp(TrooperType trooper, int val) {
        hp[trooper.ordinal()] = val;
    }

    private void check(int actionPoints, String[] map, boolean holdingFieldRation, boolean holdingMedikit, MyAction... expectedAr) {
        char[][] cmap = createMap(map);
        List<MyAction> actual = new MaxHealingPlanComputer(
                actionPoints,
                cmap,
                hp,
                holdingFieldRation,
                holdingMedikit,
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
        char[][] r = new char[map[0].length()][map.length];
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length(); j++) {
                r[j][i] = map[i].charAt(j);
            }
        }
        return r;
    }
}
