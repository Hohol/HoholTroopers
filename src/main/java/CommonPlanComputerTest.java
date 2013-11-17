import static model.TrooperType.*;

import static model.TrooperStance.*;

import org.testng.annotations.Test;

@Test
public class CommonPlanComputerTest extends AbstractPlanComputerTest {
    @Test
    void testEmpty() {
        setMap("F");
        check(
                FIELD_MEDIC,
                12,
                STANDING,
                false,
                false,
                false);
    }

    @Test
    void medicShouldHeal() {
        setMap("SF.s");

        setTrooper(0, 0, 1, STANDING);
        check(
                FIELD_MEDIC,
                2,
                STANDING,
                false,
                false,
                true,
                MyMove.USE_MEDIKIT_WEST
        );
    }

    @Test
    void testHelp() {
        setMap(
                ".C.s",
                "S..."
        );

        check(
                SOLDIER,
                2,
                STANDING,
                false,
                false,
                false,
                MyMove.MOVE_NORTH
        );
    }

    @Test
    void testHelp2() {
        setMap(
                "s.",
                "..",
                "C.",
                ".S"
        );

        check(
                SOLDIER,
                2,
                STANDING,
                false,
                false,
                false,
                MyMove.MOVE_WEST
        );
    }

    @Test
    void testHelp3() {
        setMap(
                "...f",
                ".S..",
                "s..C",
                "F..."
        );

        check(
                SOLDIER,
                2,
                STANDING,
                false,
                false,
                false,
                MyMove.MOVE_SOUTH
        );
    }

    @Test
    void testHelp4() {
        setMap(
                "....S.",
                "......",
                "s....C",
                "......"
        );

        check(
                SOLDIER,
                2,
                STANDING,
                false,
                false,
                false,
                MyMove.MOVE_SOUTH
        );
    }

    @Test
    void testDoNotWasteFieldRation() {
        setMap(
                "f...S^"
        );

        check(
                SOLDIER,
                4,
                STANDING,
                false,
                false,
                false,
                MyMove.shoot(0, 0)
        );
    }

    @Test
    void testDoNotWasteFieldRation2() {
        setMap(
                "f......C.",
                "s...^..SF"
        );

        check(
                SOLDIER,
                12,
                STANDING,
                false,
                true,
                false,
                MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.grenade(0, 1)
        );
    }

    @Test
    void testCollectMedikit() {
        setMap(
                "FS",
                ".+"
        );

        setTrooper(1, 0, 1, STANDING);

        check(
                FIELD_MEDIC,
                6,
                STANDING,
                false,
                false,
                false,
                MyMove.MOVE_SOUTH, MyMove.MOVE_EAST, MyMove.USE_MEDIKIT_NORTH
        );
    }

    @Test
    void testCollectMedikit2() {
        setMap(
                "FSC",
                ".++"
        );

        setTrooper(1, 0, 1, STANDING);
        setTrooper(2, 0, 1, STANDING);

        check(
                FIELD_MEDIC,
                10,
                STANDING,
                false,
                false,
                false,
                MyMove.MOVE_SOUTH, MyMove.MOVE_EAST, MyMove.USE_MEDIKIT_NORTH, MyMove.MOVE_EAST, MyMove.USE_MEDIKIT_NORTH
        );
    }

    @Test
    void testHelpDoNotGoTooFar() {
        setMap(
                ".####...........s........####.",
                ".####..####...##..c####..####.",
                ".......####..####..####.......",
                ".......####.f####..####.......",
                ".####..####...##...####..####.",
                ".####....................####.",
                ".####............C.......####.",
                ".####..####..####.S####..####.",
                ".......####..####..####.......",
                ".......####..####..####.......",
                ".......####..####..####.......",
                "..................F..........."
        );

        check(
                FIELD_MEDIC,
                10,
                STANDING,
                false,
                true,
                false,
                MyMove.MOVE_NORTH, MyMove.MOVE_NORTH, MyMove.MOVE_NORTH, MyMove.MOVE_WEST, MyMove.MOVE_NORTH
        );
    }

    @Test
    void testHelpDoNotGoTooFar2() {
        setMap(
                ".####...........s........####.",
                ".####..####...##...####..####.",
                ".......####..####..####.......",
                ".......####.f####..####.......",
                ".####..####...##...####..####.",
                ".####....................####.",
                ".####............S.......####.",
                ".####..####..####.F####..####.",
                ".......####..####..####.......",
                ".......####..####..####.......",
                ".......####..####..####.......",
                "..................C..........."
        );

        check(
                COMMANDER,
                10,
                STANDING,
                false,
                false,
                false,
                MyMove.MOVE_NORTH, MyMove.MOVE_NORTH, MyMove.MOVE_NORTH, MyMove.MOVE_WEST, MyMove.MOVE_NORTH
        );
    }

    @Test
    void testHelpBug() {
        setMap(
                ".s",
                ".S",
                "C3",
                ".F"
        );

        check(
                COMMANDER,
                4,
                STANDING,
                false,
                true,
                false,
                MyMove.MOVE_NORTH, MyMove.MOVE_NORTH
        );
    }

    @Test
    void testMedicDistPriority() {
        setMap(
                "...f..C",
                ".3F3...",
                ".S.....",
                "......."
        );

        check(
                FIELD_MEDIC,
                2,
                STANDING,
                false,
                false,
                false,
                MyMove.MOVE_SOUTH
        );
    }

    @Test
    void testNonMedicDistPriority() {
        setMap(
                "..f..S",
                "3C3...",
                "F....."
        );

        check(
                COMMANDER,
                2,
                STANDING,
                false,
                false,
                false,
                MyMove.MOVE_NORTH
        );
    }

    @Test
    void testNonMedicDistPriority2() {
        setMap(
                "Cf....",
                "......",
                "F.S...",
                "......"
        );

        check(
                FIELD_MEDIC,
                2,
                STANDING,
                false,
                false,
                false,
                MyMove.MOVE_EAST
        );
    }

    @Test
    void testFocusFireBug() {

        setMap(
                "S....s",
                ".#####",
                ".F....",
                "..f..."
        );

        setTrooper(5, 0, 90, STANDING);

        check(
                FIELD_MEDIC,
                8,
                STANDING,
                false,
                true,
                false,
                MyMove.grenade(5, 0)
        );
    }

    @Test
    void testDoNotBlockPathForTeammatesSimplest() {
        setMap(
                "S3333",
                "C...s"
        );

        check(
                COMMANDER,
                2,
                STANDING,
                false,
                false,
                false,
                MyMove.MOVE_EAST
        );
    }

    @Test
    void testDoNotBlockPathForTeammates() {
        setMap(
                "S3333",
                "C...s"
        );

        check(
                COMMANDER,
                5,
                STANDING,
                false,
                false,
                false,
                MyMove.MOVE_EAST, MyMove.shoot(4, 1)
        );

        setMap(
                "cs......SC"
        );
        setTrooper(0, 0, 100, STANDING);
        setTrooper(1, 0, 120, STANDING);
        check(
                SOLDIER,
                4,
                STANDING,
                false,
                false,
                false,
                MyMove.MOVE_WEST
        );

        //---------------


    }

    @Test
    void testDoNotBlockPathForTeammates2() {
        setMap(
                "..f..C",
                "3F3...",
                "S....."
        );

        check(
                FIELD_MEDIC,
                2,
                STANDING,
                false,
                false,
                false
        );
    }

    @Test
    void testDoNotBlockPathIgnoreMedic() {
        setMap(
                "FS......s",
                ".C......."
        );

        check(
                SOLDIER,
                4,
                STANDING,
                false,
                false,
                false,
                MyMove.shoot(8, 0)
        );
    }

    @Test
    void testBonusPriority() {
        setMap(
                "C.S.s"
        );

        check(
                SOLDIER,
                8,
                STANDING,
                true,
                true,
                false,
                MyMove.grenade(4,0)
        );
    }

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
                MyMove.LOWER_STANCE, MyMove.shoot(2,0), MyMove.shoot(2,0), MyMove.LOWER_STANCE
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
                MyMove.MOVE_SOUTH, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.shoot(2,0), MyMove.MOVE_WEST
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
}
