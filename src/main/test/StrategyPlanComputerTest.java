import static model.TrooperType.*;
import org.testng.annotations.Test;

import java.util.List;

@Test
public class StrategyPlanComputerTest extends AbstractPlanComputerTest {
    Cell destination; //marked as '@'

    @Override
    protected List<MyMove> getActual(String moveOrder, MutableTrooper self) {
        return new StrategyPlanComputer(
                map,
                utils,
                teammates,
                self,
                getVisibilities(),
                bonuses,
                troopers,
                destination
        ).getPlan();
    }

    @Override
    protected void setMap(String ...smap) {
        super.setMap(smap);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if(map[i][j] == '@') {
                    destination = new Cell(i,j);
                    map[i][j] = '.';
                }
            }
        }
    }

    private void setDestination(int x, int y) {
        destination = new Cell(x,y);
    }

    @Test
    void testEmpty() {
        setMap("S");
        setDestination(0, 0);
        check(
                SOLDIER,
                12
        );
    }

    @Test
    void testMoveToDestination() {
        setMap(
                "S.......@"
        );
        check(
                SOLDIER,
                2,
                MyMove.MOVE_EAST
        );

        setMap(
                "S..",
                "#..",
                "@.."
        );
        check(
                SOLDIER,
                12,
                MyMove.MOVE_EAST, MyMove.MOVE_SOUTH, MyMove.MOVE_SOUTH, MyMove.MOVE_WEST
        );
    }

    @Test
    void doNotMoveTooFarFromTeammates() {
        setMap(
                "RS.................@"
        );
        check(
                SOLDIER,
                10,
                MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.MOVE_EAST
        );
    }

    @Test
    void moveToTeammateIfAlreadyGotTooFar() {
        setMap(
                "R........S............@"
        );
        check(
                SOLDIER,
                2,
                MyMove.MOVE_WEST
        );
    }

    @Test
    void followLeader() {
        setMap(
                "F....",
                ".#...",
                ".S..@",
                "....."
        );
        check(
                FIELD_MEDIC,
                4,
                MyMove.MOVE_SOUTH, MyMove.MOVE_SOUTH
        );

        setMap(
                ".....",
                ".#...",
                ".#...",
                ".#...",
                ".#...",
                ".#...",
                "F#S.@",
                ".#..."
        );
        check(
                FIELD_MEDIC,
                2,
                MyMove.MOVE_NORTH
        );
    }

    @Test
    void followSomeoneElseToAvoidCurvedPath() {
        setMap(
                ".....",
                ".#...",
                ".#...",
                ".#...",
                ".#...",
                ".#...",
                "FRS.@",
                ".#..."
        );
        check(
                FIELD_MEDIC,
                12
        );
    }

    @Test
    void testDeblock() {
        setMap(
                "....#",
                "@..FS"
        );
        check(
                FIELD_MEDIC,
                2,
                MyMove.MOVE_NORTH
        );
    }
}
