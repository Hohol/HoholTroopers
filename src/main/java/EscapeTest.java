import static model.TrooperType.*;

import static model.TrooperStance.*;

import org.testng.annotations.Test;

@Test
public class EscapeTest extends AbstractPlanComputerTest {
    @Test
    void testEscape() {
        setMap(
                "S2s"
        );
        setTrooper(0, 0, 1, STANDING);
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
                "...",
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
        ally(SOLDIER).hp(1);
        ally(FIELD_MEDIC).medikit();
        check(
                FIELD_MEDIC,
                2,
                MyMove.USE_MEDIKIT_EAST
        );
    }

    @Test
    void testSoldierShootingRangeIsGreaterThanVisionRange() {
        setMap(
                ".S.......s"
        );
        ally(SOLDIER).medikit();
        check(
                SOLDIER,
                4,
                MyMove.shoot(9, 0)
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
    void testDoNotEscapeIfYouWasteMoveButTheyStillCanEasilyReachYou() {
        setMap(
                ".....",
                "S...c"
        );
        check(
                SOLDIER,
                4,
                MyMove.shoot(4, 1)
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
        setTrooper(0, 1, 1, STANDING);
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
    void testGoToFightIfTeammateCanBeKilled() {
        setMap(
                ".C......s",
                "S#......."
        );

        setTrooper(0, 1, 1, STANDING);
        setTrooper(1, 0, 1, STANDING);

        check(
                SOLDIER,
                2,
                MyMove.MOVE_NORTH
        );
    }

    @Test
    void testBugWithEnemyStance() {
        setMap(
                ".......",
                "s.S.1.F"
        );

        enemy(SOLDIER).stance(PRONE);

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

    /*
    @Test
    void testInteresting() {
        setMap(
                "S#s",
                "..."
        );
        setTrooper(0, 0, 1, STANDING);
        check(
                SOLDIER,
                12,
                STANDING,
                false,
                false,
                false,
                MyMove.MOVE_SOUTH, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.shoot(2, 0), MyMove.MOVE_WEST
        );
    }/**/

}
