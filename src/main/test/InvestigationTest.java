import model.TrooperStance;

import static model.TrooperType.*;

import model.TrooperType;

import static model.TrooperStance.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

@Test
public class InvestigationTest extends TacticPlanComputerTest {

    private void checkInvestigation(TrooperType type, int x, int y, TrooperStance stance, boolean phantom) {
        MutableTrooper expected = new MTBuilder().type(type).x(x).y(y).stance(stance).build();
        checkInvestigation(expected, phantom);
    }

    private void checkInvestigationNothing() {
        checkInvestigation(null, true);
    }

    private void checkInvestigation(MutableTrooper expected, boolean phantom) {
        TrooperType selfType = null;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (Utils.isTeammateChar(map[i][j])) {
                    selfType = Utils.getTrooperTypeByChar(map[i][j]);
                }
            }
        }
        check(
                selfType, 0
        );
        String resultString = "null";
        if(investigationResult != null) {
            resultString = String.format("%s %s %s %s", investigationResult.getType(), investigationResult.getX(), investigationResult.getY(), investigationResult.getStance());
        }
        if (expected == null) {
            assertTrue(investigationResult == null, resultString);
            return;
        }
        assertTrue(investigationResult != null, "result is null");
        assertTrue(equal(investigationResult, expected), resultString);
        assertEquals(TacticPlanComputer.isPhantom(investigationResult, mediumMoveIndex, moveOrder), phantom, "wrong phantomness");
    }

    boolean equal(MutableTrooper a, MutableTrooper b) {
        return a.getType() == b.getType() && a.getX() == b.getX() && a.getY() == b.getY() && a.getStance() == b.getStance();
    }

    @Test
    void testSimple() {
        setMap(
                "S.",
                "#."
        );
        teammateWasDamaged(SOLDIER);
        checkInvestigation(
                SOLDIER,
                1, 1, STANDING, false
        );
    }

    @Test
    void testObstacle() {
        setMap(
                "F.1."
        );
        teammateWasDamaged(FIELD_MEDIC);
        checkInvestigation(
                FIELD_MEDIC,
                3, 0, PRONE, false
        );
    }

    @Test
    void wasNoDamage() {
        setMap(
                "S"
        );
        checkInvestigationNothing();
    }

    @Test
    void cantFind() {
        setMap("S");
        teammateWasDamaged(SOLDIER);
        checkInvestigationNothing();
    }

    @Test
    void notEnoughAP() {
        setMap(
                "########.",
                "FT......."
        );
        theyDontHave(COMMANDER);
        theyDontHave(SCOUT);
        teammateWasDamaged(SCOUT);
        checkInvestigationNothing();
    }

    @Test
    void notEnoughAP2() {
        setMap(
                "#######.",
                "FT......"
        );
        theyDontHave(COMMANDER);
        theyDontHave(SCOUT);
        teammateWasDamaged(FIELD_MEDIC);
        checkInvestigationNothing();
    }

    @Test
    void EnoughAP() {
        setMap(
                "#######.",
                "FT......"
        );
        theyDontHave(COMMANDER);
        teammateWasDamaged(SCOUT);
        checkInvestigation(FIELD_MEDIC, 7, 0, STANDING, false);
    }

    @Test
    void suspectedWasKilled() {
        setMap(
                "#######.",
                ".T......"
        );
        theyDontHave(COMMANDER);
        theyDontHave(SCOUT);
        teammateWasDamaged(SCOUT);
        checkInvestigationNothing();
    }

    @Test
    void suspectedIsVisible() {
        setMap(
                "S....s",
                "......"
        );
        teammateWasDamaged(SOLDIER);
        checkInvestigationNothing();
    }

    @Test
    void suspectedIsPhantom() {
        setMap(
                "S.......",
                "#######.",
                "s......."
        );
        enemy(SOLDIER).lastSeenTime(0);
        setMediumMoveIndex(100);
        teammateWasDamaged(SOLDIER);
        checkInvestigation(SOLDIER, 7, 1, STANDING, true);
    }

    @Test
    void suspectedMayNotBeInOccupiedCell() {
        setMap(
                "R.......f."
        );
        setMoveOrder("SRF");
        setMediumMoveIndex(1);
        teammateWasDamaged(SNIPER);
        checkInvestigation(SNIPER, 9, 0, STANDING, false);
    }

    @Test
    void suspectedMustBeNearOtherEnemies() {
        setMap(
                "S....",
                ".##f.",
                "..###",
                "..###"
        );
        setMoveOrder("RSF");
        setMediumMoveIndex(1);
        teammateWasDamaged(SOLDIER);
        checkInvestigation(SOLDIER, 4, 1, STANDING, true);
    }

    @Test
    void anotherMoveOrder() {
        setMap(
                "S.",
                "#."
        );
        teammateWasDamaged(SOLDIER);
        setMoveOrder("SF");
        theyDontHave(SOLDIER);
        checkInvestigation(FIELD_MEDIC, 1, 1, STANDING, false);
    }

    @Test
    void returnNullIfSeeOneOfSuspected() {
        setMap(
                "S...f",
                "####."
        );
        teammateWasDamaged(SOLDIER);
        checkInvestigationNothing();
    }
}