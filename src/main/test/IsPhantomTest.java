import model.TrooperType;
import static model.TrooperType.*;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

@Test
public class IsPhantomTest {

    @Test
    void test1() {
        check("S", 0, SOLDIER, -100, true);
    }
    @Test
    void test2() {
        check("FSC", 2, FIELD_MEDIC, 0, true);
    }

    @Test
    void test3() {
        check("FSC", 2, FIELD_MEDIC, 1, false);
    }

    @Test
    void test4() {
        check("FSC", 32, FIELD_MEDIC, 1, true);
    }

    @Test
    void testNow() {
        check("FSC", 0, FIELD_MEDIC, 0, false);
    }

    @Test
    void test5() {
        check("TCRSF", 121, COMMANDER, 120, true);
    }

    void check(String moveOrder, int mediumMoveIndex, TrooperType enemyType, int lastSeenTime, boolean expected) {
        MutableTrooper enemy = new MTBuilder()
                .type(enemyType)
                .lastSeenTime(lastSeenTime)
                .build();
        assertEquals(TacticPlanComputer.isPhantom(enemy, mediumMoveIndex, moveOrder), expected);
    }
}