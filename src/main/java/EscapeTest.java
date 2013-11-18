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
}
