import static model.TrooperType.*;
import static model.TrooperStance.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.List;

@Test
public class StrategyPlanComputerTest extends AbstractPlanComputerTest {
    Cell destination;
    static final char DESTINATION_CHAR = '@';

    @Override
    protected List<MyMove> getActual(String moveOrder, MutableTrooper self) {
        if (destination == null) {
            throw new RuntimeException("Destination not specified");
        }
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
    protected void setMap(String... smap) {
        super.setMap(smap);
        destination = null;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (map[i][j] == DESTINATION_CHAR) {
                    destination = new Cell(i, j);
                    map[i][j] = '.';
                }
            }
        }
    }

    private void setDestination(int x, int y) {
        if (destination != null) {
            throw new RuntimeException("Destination already specified");
        }
        destination = new Cell(x, y);
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

    @Test
    void simpleScouting() {
        setMap(
                ".#....",
                ".FS..@"
        );
        check(
                FIELD_MEDIC,
                4,
                MyMove.MOVE_WEST, MyMove.MOVE_EAST
        );
    }

    @Test
    void raiseStanceToScout() {
        setMap(
                "S1...@"
        );

        ally(SOLDIER).stance(PRONE);

        check(
                SOLDIER,
                3,
                MyMove.RAISE_STANCE
        );
    }

    @Test
    void teammatesCanSeeAlso() {
        setMap(
                "C.....",
                "#.####",
                "#FS..@",
                "#.####",
                "..####"
        );
        check(
                FIELD_MEDIC,
                8,
                MyMove.MOVE_SOUTH, MyMove.MOVE_SOUTH, MyMove.MOVE_NORTH, MyMove.MOVE_NORTH
        );
    }

    @Test
    void youDontSeeAnythingWhenActionPointsAreOver() {
        setMap(
                "....#....",
                "...S#...@"
        );
        check(
                SOLDIER,
                2
        );
    }

    @Test
    void testBug() {
        setMap(
                "..C...@",
                ".####..",
                ".####..",
                ".####R.",
                ".####F.",
                "S......"
        );
        check(
                COMMANDER,
                2,
                MyMove.MOVE_WEST
        );
    }

    @Test
    void sniperShouldEndTurnKneeling() {
        setMap("R@");
        check(
                SNIPER,
                12,
                MyMove.MOVE_EAST, MyMove.LOWER_STANCE
        );
    }

    @Test
    void collectBonus() {
        setMap(
                "S@.F+"
        );
        check(
                FIELD_MEDIC,
                6,
                MyMove.MOVE_EAST, MyMove.MOVE_WEST, MyMove.MOVE_WEST
        );
    }
}