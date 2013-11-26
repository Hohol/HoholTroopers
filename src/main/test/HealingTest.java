import org.testng.annotations.Test;

import static model.TrooperType.*;
import static org.testng.Assert.assertEquals;

@Test
public class HealingTest extends TacticPlanComputerTest {
    @Test
    void testEmpty() {
        setMap("F");
        ally(FIELD_MEDIC).hp(100);
        check(FIELD_MEDIC, 5);
    }

    @Test
    void testTrivial() {
        setMap("F");
        ally(FIELD_MEDIC).hp(97);

        check(FIELD_MEDIC, 5, MyMove.HEAL_SELF);


        setMap("F");
        ally(FIELD_MEDIC).hp(87);

        check(FIELD_MEDIC, 5, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF);

        setMap("F");
        ally(FIELD_MEDIC).hp(88);

        check(FIELD_MEDIC, 5, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF);

        setMap("F");
        ally(FIELD_MEDIC).hp(89);

        check(FIELD_MEDIC, 5, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF);
    }

    @Test
    void testHealTeammate() {
        setMap("FS");
        ally(FIELD_MEDIC).hp(100);
        ally(SOLDIER).hp(95);

        check(FIELD_MEDIC, 5, MyMove.HEAL_EAST);

        setMap("FC");
        ally(FIELD_MEDIC).hp(90);
        ally(COMMANDER).hp(90);

        check(FIELD_MEDIC, 1, MyMove.HEAL_EAST);

        setMap("F",
                "C");
        ally(FIELD_MEDIC).hp(90);
        ally(COMMANDER).hp(90);

        check(FIELD_MEDIC, 1, MyMove.HEAL_SOUTH);

        setMap("F",
                "C");
        ally(FIELD_MEDIC).hp(98);
        ally(COMMANDER).hp(99);

        check(FIELD_MEDIC, 1, MyMove.HEAL_SELF);

        setMap(".C",
                "SF");
        ally(FIELD_MEDIC).hp(100);
        ally(COMMANDER).hp(85);
        ally(SOLDIER).hp(90);

        check(FIELD_MEDIC, 6, MyMove.HEAL_NORTH, MyMove.HEAL_NORTH, MyMove.HEAL_NORTH, MyMove.HEAL_WEST, MyMove.HEAL_WEST);
    }

    @Test
    void testFieldRation() {
        setMap("F");
        ally(FIELD_MEDIC).hp(10);

        ally(FIELD_MEDIC).fieldRation();
        check(FIELD_MEDIC, 2, MyMove.EAT_FIELD_RATION, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF);

        setMap("F");
        ally(FIELD_MEDIC).hp(100);

        ally(FIELD_MEDIC).fieldRation();
        check(FIELD_MEDIC, 2);


        setMap(".C",
                "SF");
        ally(FIELD_MEDIC).hp(90);
        ally(COMMANDER).hp(90);
        ally(SOLDIER).hp(90);


        ally(FIELD_MEDIC).fieldRation();
        check(FIELD_MEDIC, 2, MyMove.EAT_FIELD_RATION, MyMove.HEAL_NORTH, MyMove.HEAL_NORTH, MyMove.HEAL_WEST, MyMove.HEAL_WEST, MyMove.HEAL_SELF);
    }

    @Test
    void testMove() {
        setMap("F.S");
        ally(FIELD_MEDIC).hp(100);
        ally(SOLDIER).hp(95);

        check(FIELD_MEDIC, 2, MyMove.MOVE_EAST);

        setMap("F.S");
        ally(FIELD_MEDIC).hp(100);
        ally(SOLDIER).hp(95);

        check(FIELD_MEDIC, 3, MyMove.MOVE_EAST, MyMove.HEAL_EAST);

        setMap(".C.",
                "S.F");
        ally(FIELD_MEDIC).hp(100);
        ally(SOLDIER).hp(95);
        ally(COMMANDER).hp(95);

        check(FIELD_MEDIC, 4, MyMove.MOVE_WEST, MyMove.HEAL_NORTH, MyMove.HEAL_WEST);

        setMap("...",
                "S#F");
        ally(FIELD_MEDIC).hp(100);
        ally(SOLDIER).hp(95);

        check(FIELD_MEDIC, 10, MyMove.MOVE_NORTH, MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.HEAL_SOUTH);

        setMap(".#.",
                "S#F");
        ally(FIELD_MEDIC).hp(100);
        ally(SOLDIER).hp(95);

        check(FIELD_MEDIC, 10);

        setMap(".C",
                "F#",
                ".S");
        ally(FIELD_MEDIC).hp(100);
        ally(SOLDIER).hp(95);
        ally(COMMANDER).hp(90);

        check(FIELD_MEDIC, 4, MyMove.MOVE_NORTH, MyMove.HEAL_EAST, MyMove.HEAL_EAST);
    }

    @Test
    void testBugWithSelfHealing() {
        setMap("F.");
        ally(FIELD_MEDIC).hp(1);

        check(FIELD_MEDIC, 6, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF);
    }

    @Test
    void testMaximizeMinimalHp() {
        setMap("FC",
                "S.");
        ally(FIELD_MEDIC).hp(100);
        ally(COMMANDER).hp(1);
        ally(SOLDIER).hp(1);

        check(FIELD_MEDIC, 2, MyMove.HEAL_EAST, MyMove.HEAL_SOUTH);

        setMap("F.S");
        ally(FIELD_MEDIC).hp(19);
        ally(SOLDIER).hp(20);

        check(FIELD_MEDIC, 5, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF);

        setMap("F.S");
        ally(FIELD_MEDIC).hp(20);
        ally(SOLDIER).hp(20);

        check(FIELD_MEDIC, 5, MyMove.MOVE_EAST, MyMove.HEAL_EAST, MyMove.HEAL_EAST, MyMove.HEAL_EAST);

        setMap("F.S");
        ally(FIELD_MEDIC).hp(20);
        ally(SOLDIER).hp(19);

        check(FIELD_MEDIC, 5, MyMove.MOVE_EAST, MyMove.HEAL_EAST, MyMove.HEAL_EAST, MyMove.HEAL_EAST);
    }

    @Test
    void testMedikit() {
        setMap("F");
        ally(FIELD_MEDIC).hp(1);


        ally(FIELD_MEDIC).medikit();
        check(FIELD_MEDIC, 2, MyMove.USE_MEDIKIT_SELF);

        setMap("FS");
        ally(FIELD_MEDIC).hp(1);
        ally(SOLDIER).hp(50);
        ally(FIELD_MEDIC).medikit();
        check(FIELD_MEDIC, 2, MyMove.USE_MEDIKIT_EAST);

        setMap("F");
        ally(FIELD_MEDIC).hp(1);
        ally(FIELD_MEDIC).medikit();
        check(FIELD_MEDIC, 4, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.USE_MEDIKIT_SELF);

        setMap("F..C");
        ally(FIELD_MEDIC).hp(50);
        ally(COMMANDER).hp(50);
        ally(FIELD_MEDIC).medikit();
        check(FIELD_MEDIC, 7, MyMove.HEAL_SELF, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.USE_MEDIKIT_EAST);
    }

    @Test
    void testTest() {
        setMap("FC");
        ally(FIELD_MEDIC).hp(50);
        ally(COMMANDER).hp(50);
        ally(FIELD_MEDIC).medikit();
        check(FIELD_MEDIC, 4, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.USE_MEDIKIT_EAST);
    }

    @Test
    void testMinimizeSumOfDistance() {
        setMap(".S",
                "F.",
                ".C");
        ally(SOLDIER).hp(1);
        ally(FIELD_MEDIC).hp(100);
        ally(COMMANDER).hp(100);

        check(FIELD_MEDIC, 3, MyMove.MOVE_EAST, MyMove.HEAL_NORTH);

        setMap(".#C",
                ".S.",
                "F..");
        ally(SOLDIER).hp(1);
        ally(FIELD_MEDIC).hp(100);
        ally(COMMANDER).hp(100);
        check(FIELD_MEDIC, 3, MyMove.MOVE_EAST, MyMove.HEAL_NORTH);

        setMap("..C",
                ".S.",
                "F..");
        ally(SOLDIER).hp(100);
        ally(FIELD_MEDIC).hp(100);
        ally(COMMANDER).hp(100);
        check(FIELD_MEDIC, 12, MyMove.MOVE_NORTH, MyMove.MOVE_NORTH, MyMove.MOVE_EAST);
    }

    @Test
    void testMinimizeSumOfDistancePrioritizeInjured() {
        setMap("S.F.C");
        ally(SOLDIER).hp(1);
        ally(FIELD_MEDIC).hp(100);
        ally(COMMANDER).hp(100);
        check(FIELD_MEDIC, 2, MyMove.MOVE_WEST);

        setMap("S.F.C");
        ally(SOLDIER).hp(100);
        ally(FIELD_MEDIC).hp(100);
        ally(COMMANDER).hp(1);
        check(FIELD_MEDIC, 2, MyMove.MOVE_EAST);

        setMap("..S..",
                ".....",
                "R.F.C",
                ".....",
                ".....");
        ally(SNIPER).hp(1);
        ally(COMMANDER).hp(2);
        ally(SOLDIER).hp(4);
        ally(FIELD_MEDIC).hp(100);
        check(FIELD_MEDIC, 2, MyMove.MOVE_WEST);

        setMap("..S..",
                ".....",
                "R.F.C",
                ".....",
                "..T..");
        ally(SNIPER).hp(1);
        ally(COMMANDER).hp(2);
        ally(SCOUT).hp(3);
        ally(SOLDIER).hp(4);
        ally(FIELD_MEDIC).hp(100);
        check(FIELD_MEDIC, 2, MyMove.MOVE_WEST);
    }

    @Test
    void testDoNotGoTooFar() {
        setMap(
                "F..........R",
                "S#########..",
                "C..........."
        );
        ally(COMMANDER).hp(1);
        ally(SOLDIER).hp(100);
        ally(FIELD_MEDIC).hp(100);
        check(FIELD_MEDIC, 12);
    }

    @Test
    void testSomeAnotherBug() {
        setMap("SF....C");
        ally(COMMANDER).hp(90);
        ally(SOLDIER).hp(95);
        ally(FIELD_MEDIC).hp(100);
        ally(FIELD_MEDIC).fieldRation();
        check(FIELD_MEDIC, 8, MyMove.HEAL_WEST, MyMove.EAT_FIELD_RATION, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.HEAL_EAST, MyMove.HEAL_EAST);
    }

    @Test
    void testOneMoreBug() {
        setMap("F....C");
        ally(COMMANDER).hp(90);
        ally(FIELD_MEDIC).hp(97);
        ally(FIELD_MEDIC).fieldRation();
        check(FIELD_MEDIC, 8, MyMove.HEAL_SELF, MyMove.EAT_FIELD_RATION, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.HEAL_EAST, MyMove.HEAL_EAST);
    }

    @Test
    void testOneMoreBug2() {
        setMap("F");
        ally(FIELD_MEDIC).hp(1);
        ally(FIELD_MEDIC).fieldRation();
        check(FIELD_MEDIC, 8, MyMove.HEAL_SELF, MyMove.EAT_FIELD_RATION, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF, MyMove.HEAL_SELF);
    }

    @Test
    void testOneMoreBug3() {
        setMap("F");
        ally(FIELD_MEDIC).hp(99);
        ally(FIELD_MEDIC).fieldRation();
        check(FIELD_MEDIC, 1, MyMove.HEAL_SELF);
    }

    @Test
    void testProbablyBug() {
        setMap(".C",
                "SF");
        ally(FIELD_MEDIC).hp(100);
        ally(COMMANDER).hp(25);
        ally(SOLDIER).hp(60);
        ally(FIELD_MEDIC).medikit();
        check(FIELD_MEDIC, 12, MyMove.HEAL_NORTH, MyMove.HEAL_NORTH, MyMove.HEAL_NORTH, MyMove.HEAL_NORTH, MyMove.HEAL_WEST, MyMove.HEAL_WEST, MyMove.HEAL_WEST, MyMove.HEAL_WEST, MyMove.HEAL_WEST, MyMove.HEAL_WEST, MyMove.USE_MEDIKIT_NORTH);
    }

}
