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
                STANDING,
                false,
                false,
                false,
                MyMove.LOWER_STANCE
        );

        //---------------------

        setMap(
                "S1s"
        );
        setTrooper(0, 0, 1, STANDING);
        check(
                SOLDIER,
                12,
                STANDING,
                false,
                false,
                false,
                MyMove.LOWER_STANCE, MyMove.shoot(2, 0), MyMove.shoot(2, 0), MyMove.LOWER_STANCE
        );
        //----------
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
    }


    @Test
    void testEscape2() {
        setMap(
                "C.......",
                ".......c"
        );
        setTrooper(0, 0, 50, STANDING);
        check(
                COMMANDER,
                2,
                STANDING,
                false,
                false,
                false
        );
    }

    @Test
    void testDoNotFearDeadEnemy() {
        setMap(
                "...",
                "S.s"
        );
        setTrooper(0, 1, 1, STANDING);
        setTrooper(2, 1, 1, STANDING);
        check(
                SOLDIER,
                6,
                STANDING,
                false,
                false,
                false,
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
                STANDING,
                false,
                false,
                false,
                MyMove.shoot(6, 0), MyMove.shoot(6, 0), MyMove.shoot(6, 0)
        );
    }

    @Test
    void testDoNotBeCowardMedic() {
        setMap(
                ".FS......s"
        );
        setTrooper(2, 0, 1, STANDING);
        check(
                FIELD_MEDIC,
                2,
                STANDING,
                false,
                false,
                true,
                MyMove.USE_MEDIKIT_EAST
        );
    }

    @Test
    void testSoldierShootingRangeIsGreaterThanVisionRange() {
        setMap(
                ".S.......s"
        );
        check(
                SOLDIER,
                4,
                STANDING,
                false,
                false,
                true,
                MyMove.shoot(9,0)
        );
    }

    @Test
    void testOtherTroopersCanGiveVision() {
        setMap(
                ".S......fs"
        );
        check(
                SOLDIER,
                4,
                STANDING,
                false,
                false,
                true,
                MyMove.MOVE_WEST
        );

        //----------------------------

        setMap(
                ".S......f.s"
        );
        check(
                SOLDIER,
                4,
                STANDING,
                false,
                false,
                true,
                MyMove.shoot(8, 0)
        );
    }

    @Test
    void nonVisibleEnemyCanGiveVision() {
        setMap(
                ".S......?s"
        );
        check(
                SOLDIER,
                4,
                STANDING,
                false,
                false,
                true,
                MyMove.MOVE_WEST
        );

        //-------------------

        setMap(
                ".S.......s",
                "..........",
                "..........",
                "..........",
                "..........",
                "..........",
                "..........",
                "..........",
                ".?........"
        );
        check(
                SOLDIER,
                4,
                STANDING,
                false,
                false,
                true,
                MyMove.shoot(9,0)
        );
    }

    @Test
    void testDifferentTypesOfEnemiesThreatenDifferently() {
        setMap(
                ".f.",
                "sF.",
                "..."
        );
        check(
                FIELD_MEDIC,
                2,
                STANDING,
                false,
                false,
                true,
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
                STANDING,
                false,
                false,
                false,
                MyMove.LOWER_STANCE
        );
    }
}
