import static model.TrooperType.*;

import static model.TrooperStance.*;

import org.testng.annotations.Test;

@Test
public class EscapeTest extends TacticPlanComputerTest {
    @Test
    void testEscape() {
        setMap(
                "S2s"
        );
        enemy(SOLDIER).hp(1);
        check(
                SOLDIER,
                2,
                MyMove.LOWER_STANCE
        );

        //----------

    }


    @Test
    void testEscape2() {
        setMap(
                "C.......",
                ".......c"
        );
        ally(COMMANDER).hp(50);
        check(
                COMMANDER,
                2
        );
    }

    @Test
    void testEscape3() {
        setMap(
                "S1s"
        );
        ally(SOLDIER).hp(1);
        check(
                SOLDIER,
                12,
                MyMove.LOWER_STANCE, MyMove.shoot(2, 0), MyMove.shoot(2, 0), MyMove.LOWER_STANCE
        );
    }

    @Test
    void testDoNotFearDeadEnemy() {
        setMap(
                ".#.",
                "S.s"
        );
        ally(SOLDIER).hp(1);
        enemy(SOLDIER).hp(1);
        check(
                SOLDIER,
                6,
                MyMove.shoot(2, 1)
        );
    }

    @Test
    void testDoNotBeCoward() {
        setMap(
                ".....Sf"
        );
        check(
                SOLDIER,
                12,
                MyMove.shoot(6, 0), MyMove.shoot(6, 0), MyMove.shoot(6, 0)
        );
    }

    @Test
    void testDoNotBeCowardMedic() {
        setMap(
                ".FS......s"
        );
        ally(SOLDIER).hp(26);
        ally(FIELD_MEDIC).medikit();
        check(
                FIELD_MEDIC,
                2,
                MyMove.USE_MEDIKIT_EAST
        );
    }

    @Test
    void testDifferentTypesOfEnemiesThreatenDifferently() {
        setMap(
                "...f#",
                ".....",
                "s..F.",
                "##..."
        );
        ally(FIELD_MEDIC).medikit();
        check(
                FIELD_MEDIC,
                2,
                MyMove.MOVE_SOUTH
        );
    }

    @Test
    void testPreferObstaclesForEscape() {
        setMap(
                "......f",
                "S2....s"
        );
        check(
                SOLDIER,
                2,
                MyMove.LOWER_STANCE
        );
    }

    @Test
    void testDoNotTryEscapeIfEnemyCannotReachYouEasily() {
        setMap(
                "...F#....",
                ".........s"
        );
        check(
                FIELD_MEDIC,
                2
        );
    }

    @Test
    void testCounterTestToPrevious() {
        setMap(
                ".#.",
                "C.s"
        );
        check(
                COMMANDER,
                3,
                MyMove.MOVE_NORTH
        );
    }

    @Test
    void testEscape4() {
        setMap(
                ".#........",
                "S........s"
        );
        enemy(SOLDIER).hp(1);
        check(
                SOLDIER,
                2,
                MyMove.MOVE_NORTH
        );
    }

    @Test
    void testHelp() {
        setMap(
                ".C......f",
                "S........"
        );

        check(
                SOLDIER,
                2,
                "CSF",
                MyMove.MOVE_NORTH
        );
    }

    @Test
    void testCommanderBonus() {
        setMap(
                "F....",
                ".####",
                "....s"
        );

        check(
                FIELD_MEDIC,
                2,
                MyMove.MOVE_EAST
        );
    }

    @Test
    void testEnemyShouldChangeStance() {
        setMap(
                "F....",
                ".####",
                "....s"
        );

        enemy(SOLDIER).stance(KNEELING);

        check(
                FIELD_MEDIC,
                2,
                MyMove.MOVE_SOUTH
        );
    }

    @Test
    void testDoNotGoInFightIfCanBeKilled() {
        setMap(
                ".C......s",
                "S#......."
        );

        ally(SOLDIER).hp(1);

        check(
                SOLDIER,
                2
        );
    }

    @Test
    void testBugWithEnemyStance() {
        setMap(
                ".......",
                "s.S.1.F"
        );

        enemy(SOLDIER).stance(PRONE);
        theyDontHave(FIELD_MEDIC);

        check(
                SOLDIER,
                2,
                MyMove.MOVE_NORTH
        );
    }

    @Test
    void testGrenade() {
        setMap(
                ".S...#f"
        );

        enemy(FIELD_MEDIC).grenade();

        check(
                SOLDIER,
                2,
                MyMove.MOVE_WEST
        );
    }

    @Test
    void testGrenadeCollateral() {
        setMap(
                ".S....#f"
        );

        enemy(FIELD_MEDIC).grenade();

        check(
                SOLDIER,
                2,
                MyMove.MOVE_WEST
        );
    }

    @Test
    void testEnemyCanUseFieldRation() {
        setMap(
                ".S....1...f"
        );
        ally(SOLDIER).hp(60);
        enemy(FIELD_MEDIC).fieldRation().grenade();
        check(
                SOLDIER,
                2,
                MyMove.MOVE_WEST
        );
    }

    @Test
    void testConsiderMoveOrder() {
        setMap(
                "S.....cs",
                ".C1....."
        );
        check(
                COMMANDER,
                2
        );
    }

    @Test
    void testConsiderMoveOrder2() {
        setMap(
                ".....C",
                ".####.",
                ".#....",
                "S...cs",
                "......",
                "......",
                "......",
                ".....#",
                "....#f"
        );
        enemy(SOLDIER).stance(KNEELING);
        check(
                COMMANDER,
                3,
                "CSF",
                MyMove.MOVE_WEST
        );
    }
}
