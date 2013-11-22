import model.TrooperStance;
import model.TrooperType;
import org.testng.annotations.Test;

import static model.TrooperType.*;
import static org.testng.Assert.assertEquals;

@Test
public class HealingTest extends AbstractPlanComputerTest {
    int[] hp1d = new int[TrooperType.values().length];

    @Test
    void testEmpty() {
        setHp(FIELD_MEDIC, 100);

        setMap("F");
        setHp2();
        check(FIELD_MEDIC, 5, TrooperStance.STANDING, false, false, false);
    }

    @Test
    void testTrivial() {
        setHp(FIELD_MEDIC, 97);
        setMap("F");
        setHp2();
        check(FIELD_MEDIC, 5, TrooperStance.STANDING, false, false, false, MyMove.HEAL_SELF);


        setHp(FIELD_MEDIC, 87);
        setMap("F");
        setHp2();
        check(FIELD_MEDIC, 5, TrooperStance.STANDING, false, false, false, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF);

        setHp(FIELD_MEDIC, 88);
        setMap("F");
        setHp2();
        check(FIELD_MEDIC, 5, TrooperStance.STANDING, false, false, false, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF);

        setHp(FIELD_MEDIC, 89);
        setMap("F");
        setHp2();
        check(FIELD_MEDIC, 5, TrooperStance.STANDING, false, false, false, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF);
    }

    @Test(enabled = true)
    void testHealTeammate() {
        setHp(FIELD_MEDIC, 100);
        setHp(SOLDIER, 95);
        setMap("FS");
        setHp2();
        check(FIELD_MEDIC, 5, TrooperStance.STANDING, false, false, false, MyMove.HEAL_EAST);

        setHp(FIELD_MEDIC, 90);
        setHp(COMMANDER, 90);
        setMap("FC");
        setHp2();
        check(FIELD_MEDIC, 1, TrooperStance.STANDING, false, false, false, MyMove.HEAL_EAST);

        setHp(FIELD_MEDIC, 90);
        setHp(COMMANDER, 90);
        setMap("F",
                "C");
        setHp2();
        check(FIELD_MEDIC, 1, TrooperStance.STANDING, false, false, false, MyMove.HEAL_SOUTH);

        setHp(FIELD_MEDIC, 98);
        setHp(COMMANDER, 99);
        setMap("F",
                "C");
        setHp2();
        check(FIELD_MEDIC, 1, TrooperStance.STANDING, false, false, false, MyMove.HEAL_SELF);

        setHp(FIELD_MEDIC, 100);
        setHp(COMMANDER, 85);
        setHp(SOLDIER, 90);
        setMap(".C",
                "SF");
        setHp2();
        check(FIELD_MEDIC, 6, TrooperStance.STANDING, false, false, false, MyMove.HEAL_NORTH, MyMove.HEAL_NORTH, MyMove.HEAL_NORTH, MyMove.HEAL_WEST, MyMove.HEAL_WEST);
    }

    @Test
    void testFieldRation() {
        setHp(FIELD_MEDIC, 10);
        setMap("F");
        setHp2();
        check(FIELD_MEDIC, 2, TrooperStance.STANDING, true, false, false, MyMove.EAT_FIELD_RATION, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF);

        setHp(FIELD_MEDIC, 100);
        setMap("F");
        setHp2();
        check(FIELD_MEDIC, 2, TrooperStance.STANDING, true, false, false);

        setHp(FIELD_MEDIC, 90);
        setHp(COMMANDER, 90);
        setHp(SOLDIER, 90);

        setMap(".C",
                "SF");
        setHp2();
        check(FIELD_MEDIC, 2, TrooperStance.STANDING, true, false, false, MyMove.EAT_FIELD_RATION, MyMove.HEAL_NORTH, MyMove.HEAL_NORTH, MyMove.HEAL_WEST, MyMove.HEAL_WEST, MyMove.HEAL_SELF);
    }

    @Test
    void testMove() {
        setHp(FIELD_MEDIC, 100);
        setHp(SOLDIER, 95);
        setMap("F.S");
        setHp2();
        check(FIELD_MEDIC, 2, TrooperStance.STANDING, false, false, false, MyMove.MOVE_EAST);

        setHp(FIELD_MEDIC, 100);
        setHp(SOLDIER, 95);
        setMap("F.S");
        setHp2();
        check(FIELD_MEDIC, 3, TrooperStance.STANDING, false, false, false, MyMove.MOVE_EAST, MyMove.HEAL_EAST);

        setHp(FIELD_MEDIC, 100);
        setHp(SOLDIER, 95);
        setHp(COMMANDER, 95);
        setMap(".C.",
                "S.F");
        setHp2();
        check(FIELD_MEDIC, 4, TrooperStance.STANDING, false, false, false, MyMove.MOVE_WEST, MyMove.HEAL_NORTH, MyMove.HEAL_WEST);

        setHp(FIELD_MEDIC, 100);
        setHp(SOLDIER, 95);
        setMap("...",
                "S#F");
        setHp2();
        check(FIELD_MEDIC, 10, TrooperStance.STANDING, false, false, false, MyMove.MOVE_NORTH, MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.HEAL_SOUTH);

        setHp(FIELD_MEDIC, 100);
        setHp(SOLDIER, 95);
        setMap(".#.",
                "S#F");
        setHp2();
        check(FIELD_MEDIC, 10, TrooperStance.STANDING, false, false, false);

        setHp(FIELD_MEDIC, 100);
        setHp(SOLDIER, 95);
        setHp(COMMANDER, 90);
        setMap(".C",
                "F#",
                ".S");
        setHp2();
        check(FIELD_MEDIC, 4, TrooperStance.STANDING, false, false, false, MyMove.MOVE_NORTH, MyMove.HEAL_EAST, MyMove.HEAL_EAST);
    }

    @Test
    void testBugWithSelfHealing() {
        setHp(FIELD_MEDIC, 1);
        setMap("F.");
        setHp2();
        check(FIELD_MEDIC, 6, TrooperStance.STANDING, false, false, false, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF);
    }

    @Test
    void testMaximizeMinimalHp() {
        setHp(FIELD_MEDIC, 100);
        setHp(COMMANDER, 1);
        setHp(SOLDIER, 1);
        setMap("FC",
                "S.");
        setHp2();
        check(FIELD_MEDIC, 2, TrooperStance.STANDING, false, false, false, MyMove.HEAL_EAST, MyMove.HEAL_SOUTH);

        setHp(FIELD_MEDIC, 19);
        setHp(SOLDIER, 20);
        setMap("F.S");
        setHp2();
        check(FIELD_MEDIC, 5, TrooperStance.STANDING, false, false, false, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF);

        setHp(FIELD_MEDIC, 20);
        setHp(SOLDIER, 20);
        setMap("F.S");
        setHp2();
        check(FIELD_MEDIC, 5, TrooperStance.STANDING, false, false, false, MyMove.MOVE_EAST, MyMove.HEAL_EAST, MyMove.HEAL_EAST, MyMove.HEAL_EAST);

        setHp(FIELD_MEDIC, 20);
        setHp(SOLDIER, 19);
        setMap("F.S");
        setHp2();
        check(FIELD_MEDIC, 5, TrooperStance.STANDING, false, false, false, MyMove.MOVE_EAST, MyMove.HEAL_EAST, MyMove.HEAL_EAST, MyMove.HEAL_EAST);
    }

    @Test
    void testMedikit() {
        setHp(FIELD_MEDIC, 1);
        setMap("F");
        setHp2();
        check(FIELD_MEDIC, 2, TrooperStance.STANDING, false, false, true, MyMove.USE_MEDIKIT_SELF);

        setHp(FIELD_MEDIC, 1);
        setHp(SOLDIER, 50);
        setMap("FS");
        setHp2();
        check(FIELD_MEDIC, 2, TrooperStance.STANDING, false, false, true, MyMove.USE_MEDIKIT_EAST);

        setHp(FIELD_MEDIC, 1);
        setMap("F");
        setHp2();
        check(FIELD_MEDIC, 4, TrooperStance.STANDING, false, false, true, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.USE_MEDIKIT_SELF);

        setHp(FIELD_MEDIC, 50);
        setHp(COMMANDER, 50);
        setMap("F..C");
        setHp2();
        check(FIELD_MEDIC, 7, TrooperStance.STANDING, false, false, true, MyMove.HEAL_SELF, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.USE_MEDIKIT_EAST);
    }

    @Test
    void testTest() {
        setHp(FIELD_MEDIC, 50);
        setHp(COMMANDER, 50);
        setMap("FC");
        setHp2();
        check(FIELD_MEDIC, 4, TrooperStance.STANDING, false, false, true, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.USE_MEDIKIT_EAST);
    }

    @Test
    void testMinimizeSumOfDistance() {
        setHp(SOLDIER, 1);
        setHp(FIELD_MEDIC, 100);
        setHp(COMMANDER, 100);
        setMap(".S",
                "F.",
                ".C");
        setHp2();
        check(FIELD_MEDIC, 3, TrooperStance.STANDING, false, false, false, MyMove.MOVE_EAST, MyMove.HEAL_NORTH);

        setHp(SOLDIER, 1);
        setHp(FIELD_MEDIC, 100);
        setHp(COMMANDER, 100);
        setMap(".#C",
                ".S.",
                "F..");
        setHp2();
        check(FIELD_MEDIC, 3, TrooperStance.STANDING, false, false, false, MyMove.MOVE_EAST, MyMove.HEAL_NORTH);

        setHp(SOLDIER, 100);
        setHp(FIELD_MEDIC, 100);
        setHp(COMMANDER, 100);
        setMap("..C",
                ".S.",
                "F..");
        setHp2();
        check(FIELD_MEDIC, 12, TrooperStance.STANDING, false, false, false, MyMove.MOVE_NORTH, MyMove.MOVE_NORTH, MyMove.MOVE_EAST);
    }

    @Test
    void testMinimizeSumOfDistancePrioritizeInjured() {
        setHp(SOLDIER, 1);
        setHp(FIELD_MEDIC, 100);
        setHp(COMMANDER, 100);
        setMap("S.F.C");
        setHp2();
        check(FIELD_MEDIC, 2, TrooperStance.STANDING, false, false, false, MyMove.MOVE_WEST);

        setHp(SOLDIER, 100);
        setHp(FIELD_MEDIC, 100);
        setHp(COMMANDER, 1);
        setMap("S.F.C");
        setHp2();
        check(FIELD_MEDIC, 2, TrooperStance.STANDING, false, false, false, MyMove.MOVE_EAST);

        setHp(SNIPER, 1);
        setHp(COMMANDER, 2);
        setHp(SOLDIER, 4);
        setHp(FIELD_MEDIC, 100);

        setMap("..S..",
                ".....",
                "R.F.C",
                ".....",
                ".....");
        setHp2();
        check(FIELD_MEDIC, 2, TrooperStance.STANDING, false, false, false, MyMove.MOVE_WEST);

        setHp(SNIPER, 1);
        setHp(COMMANDER, 2);
        setHp(SCOUT, 3);
        setHp(SOLDIER, 4);
        setHp(FIELD_MEDIC, 100);

        setMap("..S..",
                ".....",
                "R.F.C",
                ".....",
                "..T..");
        setHp2();
        check(FIELD_MEDIC, 2, TrooperStance.STANDING, false, false, false, MyMove.MOVE_WEST);
    }

    @Test
    void testDoNotTryToReachUnreachable() {
        setHp(COMMANDER, 1);
        setHp(FIELD_MEDIC, 100);

        setMap("........#.......",
                "...F....#....C..",
                "........#.......");
        setHp2();
        check(FIELD_MEDIC, 12, TrooperStance.STANDING, false, false, false);
    }

    @Test
    void testDoNotGoTooFar() {
        setHp(COMMANDER, 1);
        setHp(SOLDIER, 100);
        setHp(FIELD_MEDIC, 100);

        setMap("F...............",
                "S##############.",
                "C...............");
        setHp2();
        check(FIELD_MEDIC, 12, TrooperStance.STANDING, false, false, false);
    }

    @Test(enabled = false)
        //manual
    void testTooSlow() {
        //setHp(SNIPER, 1);
        setHp(COMMANDER, 2);
        //setHp(SCOUT, 3);
        setHp(SOLDIER, 4);
        setHp(FIELD_MEDIC, 1);

        setMap("..........",
                "..S.......",
                "..F.......",
                "..........",
                "..........");
        setHp2();
        check(FIELD_MEDIC, 12, TrooperStance.STANDING, true, false, true);
    }

    @Test
    void testSomeAnotherBug() {
        setHp(COMMANDER, 90);
        setHp(SOLDIER, 95);
        setHp(FIELD_MEDIC, 100);

        setMap("SF....C");
        setHp2();
        check(FIELD_MEDIC, 8, TrooperStance.STANDING, true, false, false, MyMove.HEAL_WEST, MyMove.EAT_FIELD_RATION, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.HEAL_EAST, MyMove.HEAL_EAST);
    }

    @Test
    void testOneMoreBug() {
        setHp(COMMANDER, 90);
        setHp(FIELD_MEDIC, 97);

        setMap("F....C");
        setHp2();
        check(FIELD_MEDIC, 8, TrooperStance.STANDING, true, false, false, MyMove.HEAL_SELF, MyMove.EAT_FIELD_RATION, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.HEAL_EAST, MyMove.HEAL_EAST);
    }

    @Test
    void testOneMoreBug2() {
        setHp(FIELD_MEDIC, 1);

        setMap("F");
        setHp2();
        check(FIELD_MEDIC, 8, TrooperStance.STANDING, true, false, false, MyMove.HEAL_SELF, MyMove.EAT_FIELD_RATION, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF);
    }

    @Test
    void testOneMoreBug3() {
        setHp(FIELD_MEDIC, 99);

        setMap("F");
        setHp2();
        check(FIELD_MEDIC, 1, TrooperStance.STANDING, true, false, false, MyMove.HEAL_SELF);
    }

    @Test
    void testProbablyBug() {
        setHp(FIELD_MEDIC, 100);
        setHp(COMMANDER, 25);
        setHp(SOLDIER, 60);

        setMap(".C",
                "SF");
        setHp2();
        check(FIELD_MEDIC, 12, TrooperStance.STANDING, false, false, true, MyMove.HEAL_NORTH, MyMove.HEAL_NORTH, MyMove.HEAL_NORTH, MyMove.HEAL_NORTH, MyMove.HEAL_WEST, MyMove.HEAL_WEST, MyMove.HEAL_WEST, MyMove.HEAL_WEST, MyMove.HEAL_WEST, MyMove.HEAL_WEST, MyMove.USE_MEDIKIT_NORTH);
    }

    private void setHp(TrooperType trooper, int val) {
        hp1d[trooper.ordinal()] = val;
    }

    private void setHp2() {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (Utils.isTeammateChar(map[i][j])) {
                    int index = Utils.getTrooperTypeByChar(map[i][j]).ordinal();
                    setTrooper(i, j, hp1d[index], TrooperStance.STANDING);
                }
            }
        }
    }
}
