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
                ".C......f",
                ".........",
                ".........",
                ".........",
                "S........"
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
    void testHelp4() {
        setMap(
                "....S.",
                "..#...",
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
                ".........f..S",
                "3C3..........",
                "F............"
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
    void testMedicDistPriority2() {
        setMap(
                "C.f...",
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
                MyMove.MOVE_NORTH
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
                ".s......SC"
        );
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
                ".C......s"
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
                MyMove.grenade(4, 0)
        );
    }

    @Test
    void testBug() {
        setMap(
                "cs......#C",
                ".....#..S."
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
    void testBug2() {
        setMap(
                "..f",
                "...",
                "S1s"
        );
        setTrooper(2, 2, 1, STANDING);
        check(
                SOLDIER,
                6,
                STANDING,
                false,
                false,
                false,
                MyMove.shoot(2, 2)
        );
    }

    @Test
    void testMoveProne() {
        setMap(
                "S........",
                "........f"
        );
        setTrooper(0, 0, 120, PRONE);
        check(
                SOLDIER,
                12,
                PRONE,
                true,
                false,
                false,
                MyMove.MOVE_SOUTH, MyMove.EAT_FIELD_RATION, MyMove.shoot(8,1), MyMove.shoot(8,1)
        );
    }

    @Test
    void testEatMedikitIfItCanSaveYou() {
        setMap(
                "S#s",
                "..."
        );
        setTrooper(0, 0, 80, STANDING);
        giveGrenade(2, 0);
        check(
                SOLDIER,
                2,
                STANDING,
                false,
                false,
                true,
                MyMove.USE_MEDIKIT_SELF
        );
    }
}
