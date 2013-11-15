import model.TrooperType;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static model.TrooperType.*;
import static org.testng.Assert.assertEquals;

@Test
public class MaxHealingPlanComputerTest {
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
        checkWithExpectedHealedSum(
                5,
                new String[]{
                        "F"
                },
                false, false,
                3,
                MyMove.HEAL_SELF
        );


        setHp(FIELD_MEDIC, 87);
        check(
                5,
                new String[]{
                        "F"
                },

                false, false, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF
        );

        setHp(FIELD_MEDIC, 88);
        check(
                5,
                new String[]{
                        "F"
                },

                false, false, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF
        );

        setHp(FIELD_MEDIC, 89);
        check(
                5,
                new String[]{
                        "F"
                },

                false, false, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF
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

                false, false, MyMove.HEAL_EAST
        );

        setHp(FIELD_MEDIC, 90);
        setHp(COMMANDER, 90);
        check(
                1,
                new String[] {
                        "FC"
                },

                false, false, MyMove.HEAL_EAST
        );

        setHp(FIELD_MEDIC, 90);
        setHp(COMMANDER, 90);
        check(
                1,
                new String[] {
                        "F",
                        "C"
                },

                false, false, MyMove.HEAL_SOUTH
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

                false, MyMove.HEAL_SELF
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

                false, false, MyMove.HEAL_NORTH, MyMove.HEAL_NORTH, MyMove.HEAL_NORTH, MyMove.HEAL_WEST, MyMove.HEAL_WEST
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

                false, MyMove.EAT_FIELD_RATION, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF
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

                MyMove.EAT_FIELD_RATION, MyMove.HEAL_NORTH, MyMove.HEAL_NORTH, MyMove.HEAL_WEST, MyMove.HEAL_WEST, MyMove.HEAL_SELF
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
                MyMove.MOVE_EAST
        );

        setHp(FIELD_MEDIC, 100);
        setHp(SOLDIER, 95);
        check(
                3,
                new String[] {
                        "F.S"
                },
                false,

                false, MyMove.MOVE_EAST, MyMove.HEAL_EAST
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

                false, MyMove.MOVE_WEST, MyMove.HEAL_NORTH, MyMove.HEAL_WEST
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

                false, MyMove.MOVE_NORTH, MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.HEAL_SOUTH
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

                false, MyMove.MOVE_NORTH, MyMove.HEAL_EAST, MyMove.HEAL_EAST
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

                false, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF
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

                false, MyMove.HEAL_EAST, MyMove.HEAL_SOUTH
        );

        setHp(FIELD_MEDIC, 19);
        setHp(SOLDIER, 20);
        check(
                5,
                new String[] {
                        "F.S",
                },
                false,

                false, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF
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

                MyMove.MOVE_EAST, MyMove.HEAL_EAST, MyMove.HEAL_EAST, MyMove.HEAL_EAST
        );

        setHp(FIELD_MEDIC, 20);
        setHp(SOLDIER, 19);
        check(
                5,
                new String[] {
                        "F.S",
                },
                false,

                false, MyMove.MOVE_EAST, MyMove.HEAL_EAST, MyMove.HEAL_EAST, MyMove.HEAL_EAST
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
                MyMove.USE_MEDIKIT_SELF
        );

        setHp(FIELD_MEDIC, 1);
        setHp(SOLDIER, 50);
        checkWithExpectedHealedSum(
                2,
                new String[] {
                        "FS"
                },
                false,
                true,
                50,
                MyMove.USE_MEDIKIT_EAST
        );

        setHp(FIELD_MEDIC, 1);
        check(
                4,
                new String[] {
                        "F"
                },
                false,
                true,

                MyMove.USE_MEDIKIT_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF
        );

        setHp(FIELD_MEDIC, 50);
        setHp(COMMANDER, 50);
        checkWithExpectedHealedSum(
                7,
                new String[] {
                        "F..C"
                },
                false,
                true,
                53,
                MyMove.HEAL_SELF, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.USE_MEDIKIT_EAST
        );
    }

    @Test
    void testTest() {
        setHp(FIELD_MEDIC, 50);
        setHp(COMMANDER, 50);
        checkWithExpectedHealedSum(
                4,
                new String[] {
                        "FC"
                },
                false,
                true,
                56,
                MyMove.USE_MEDIKIT_EAST, MyMove.HEAL_SELF, MyMove.HEAL_SELF
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
                MyMove.HEAL_SELF, MyMove.HEAL_SELF
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
                MyMove.MOVE_EAST, MyMove.HEAL_NORTH
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
                MyMove.MOVE_EAST, MyMove.HEAL_NORTH
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
                MyMove.MOVE_NORTH, MyMove.MOVE_NORTH, MyMove.MOVE_EAST
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
                MyMove.MOVE_WEST
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
                MyMove.MOVE_EAST
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
                MyMove.MOVE_WEST
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
                MyMove.MOVE_WEST
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

    @Test
    void testSomeAnotherBug() {
        setHp(COMMANDER, 90);
        setHp(SOLDIER, 95);
        setHp(FIELD_MEDIC, 100);

        check(
                8,
                new String[] {
                        "SF....C",
                },
                true,
                false,
                MyMove.HEAL_WEST, MyMove.EAT_FIELD_RATION, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.HEAL_EAST, MyMove.HEAL_EAST
        );
    }

    @Test
    void testOneMoreBug() {
        setHp(COMMANDER, 90);
        setHp(FIELD_MEDIC, 97);

        check(
                8,
                new String[] {
                        "F....C",
                },
                true,
                false,
                MyMove.HEAL_SELF, MyMove.EAT_FIELD_RATION, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.HEAL_EAST, MyMove.HEAL_EAST
        );
    }

    @Test
    void testOneMoreBug2() {
        setHp(FIELD_MEDIC, 1);

        checkWithExpectedHealedSum(
                8,
                new String[] {
                        "F",
                },
                true,
                false,
                33,
                MyMove.HEAL_SELF, MyMove.EAT_FIELD_RATION, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF
        );
    }

    @Test
    void testOneMoreBug3() {
        setHp(FIELD_MEDIC, 99);

        checkWithExpectedHealedSum(
                1,
                new String[] {
                        "F",
                },
                true,
                false,
                1,
                MyMove.HEAL_SELF
        );
    }

    @Test
    void testProbablyBug() {
        setHp(FIELD_MEDIC, 100);
        setHp(COMMANDER, 25);
        setHp(SOLDIER, 60);

        checkWithExpectedHealedSum(
                12,
                new String[] {
                        ".C",
                        "SF"
                },
                false,
                true,
                100,

                MyMove.HEAL_NORTH, MyMove.HEAL_NORTH, MyMove.HEAL_NORTH, MyMove.HEAL_NORTH,
                MyMove.HEAL_WEST, MyMove.HEAL_WEST, MyMove.HEAL_WEST, MyMove.HEAL_WEST, MyMove.HEAL_WEST, MyMove.HEAL_WEST,
                MyMove.USE_MEDIKIT_NORTH
        );
    }

    private void setHp(TrooperType trooper, int val) {
        hp[trooper.ordinal()] = val;
    }

    private void check(int actionPoints, String[] map, boolean holdingFieldRation, boolean holdingMedikit, MyMove... expectedAr) {
        checkWithExpectedHealedSum(actionPoints, map, holdingFieldRation, holdingMedikit, -1, expectedAr);
    }

    private void checkWithExpectedHealedSum(int actionPoints, String[] map, boolean holdingFieldRation, boolean holdingMedikit, int expectedHealSum, MyMove... expectedAr) {
        char[][] cmap = Utils.toCharAndTranspose(map);
        HealingState plan = new MaxHealingPlanComputer(
                actionPoints,
                cmap,
                hp,
                holdingFieldRation,
                holdingMedikit,
                Utils.HARDCODED_UTILS
        ).getPlan();

        List<MyMove> actual = plan.actions;
        List<MyMove> expected = Arrays.asList(expectedAr);
        assertEquals(
                actual,
                expected,
                String.format("\n\nActual: %s \nExpected: %s\nActual heal: %s\n Expected heal: %s\n\n", actual, expected, plan.healedSum, expectedHealSum)
        );
        if(expectedHealSum != -1) {
            assertEquals(plan.healedSum, expectedHealSum);
        }
    }
}
