import model.TrooperType;

import static model.TrooperType.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

@Test
public class getApToMoveTest {
    @Test
    void test1() {
        assertEquals(0, TacticPlanComputer.getApToMove(0, 0, 0));
    }

    @Test
    void test2() {
        assertEquals(6, TacticPlanComputer.getApToMove(1, 0, 0));
    }

    @Test
    void test3() {
        assertEquals(6, TacticPlanComputer.getApToMove(1, 0, 1));
    }

    @Test
    void test4() {
        assertEquals(4, TacticPlanComputer.getApToMove(0, 0, 2));
    }

    @Test
    void test5() {
        assertEquals(2, TacticPlanComputer.getApToMove(1, 2, 2));
    }

    @Test
    void test6() {
        assertEquals(12, TacticPlanComputer.getApToMove(2, 0, 0));
    }

    @Test
    void test7() {
        assertEquals(8, TacticPlanComputer.getApToMove(2, 0, 2));
    }

    @Test
    void test8() {
        assertEquals(8, TacticPlanComputer.getApToMove(2, 1, 1));
    }
}
