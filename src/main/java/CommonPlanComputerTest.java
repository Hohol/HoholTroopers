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
                MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.grenade(0,1)
        );
    }
}
