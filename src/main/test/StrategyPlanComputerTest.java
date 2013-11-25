import static model.TrooperType.*;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

@Test
public class StrategyPlanComputerTest extends AbstractPlanComputerTest {
    Cell dest;

    @Test
    void testEmpty() {
        setMap("S");
        check(
                SOLDIER,
                12
        );
    }

    /*@Test
    void testMoveToDestination() {
        setMap(
                "S........"
        );
        destination(8,0);
        check(
                SOLDIER,
                2,
                MyMove.MOVE_EAST
        );
    }/**/

    @Override
    protected List<MyMove> getActual(String moveOrder, MutableTrooper self) {
        return new StrategyPlanComputer(
                map,
                utils,
                teammates,
                self,
                getVisibilities(),
                bonuses,
                troopers
        ).getPlan();
    }

    private void destination(int x, int y) {
        dest = new Cell(x, y);
    }
}
