import model.TrooperType;
import org.testng.annotations.Test;
import static org.testng.Assert.*;
import static model.TrooperType.*;

@Test
public class CanDamageTest {
    boolean[][] canDamageIfBefore, canDamageIfAfter;
    String moveOrder;
    TrooperType selfType;

    @Test
    void testBeforeTrivial() {
        init("S", SOLDIER);
        addBefore(SOLDIER, SOLDIER);
        checkBefore();
    }

    @Test
    void testBefore() {
        init("CSF", COMMANDER);
        addBefore(SOLDIER, COMMANDER);
        addBefore(COMMANDER, COMMANDER);
        addBefore(FIELD_MEDIC, COMMANDER);

        addBefore(SOLDIER, SOLDIER);

        addBefore(SOLDIER, FIELD_MEDIC);
        addBefore(FIELD_MEDIC, FIELD_MEDIC);
        checkBefore();

        init("CSF", FIELD_MEDIC);
        addBefore(COMMANDER, FIELD_MEDIC);
        addBefore(SOLDIER, FIELD_MEDIC);
        addBefore(FIELD_MEDIC, FIELD_MEDIC);

        addBefore(COMMANDER, COMMANDER);
        addBefore(COMMANDER, SOLDIER);

        addBefore(SOLDIER, SOLDIER);
        checkBefore();
    }

    @Test
    void testAfterTrivial() {
        init("S", SOLDIER);
        addAfter(SOLDIER, SOLDIER);
        checkAfter();
    }

    @Test
    void testAfter() {
        init("CSF", COMMANDER);
        addAfter(SOLDIER, COMMANDER);
        addAfter(FIELD_MEDIC, COMMANDER);
        addAfter(COMMANDER, COMMANDER);

        addAfter(COMMANDER, SOLDIER);

        addAfter(COMMANDER, FIELD_MEDIC);
        addAfter(SOLDIER, FIELD_MEDIC);

        checkAfter();

        init("SF", FIELD_MEDIC);
        addAfter(SOLDIER, FIELD_MEDIC);
        addAfter(FIELD_MEDIC, FIELD_MEDIC);

        addAfter(FIELD_MEDIC, SOLDIER);

        checkAfter();
    }

    void checkBefore() {
        boolean[][] actualIfBefore = TacticPlanComputer.getCanDamageIfBefore(moveOrder, selfType);
        for (int i = 0; i < actualIfBefore.length; i++) {
            for (int j = 0; j < actualIfBefore[i].length; j++) {
                assertEquals(actualIfBefore[i][j], canDamageIfBefore[i][j],
                        String.format("Fail on before (%s, %s). Expected = %s", TrooperType.values()[i], TrooperType.values()[j], canDamageIfBefore[i][j])
                );
            }
        }
    }

    private void checkAfter() {
        boolean[][] actualIfAfter = TacticPlanComputer.getCanDamageIfAfter(moveOrder, selfType);
        for (int i = 0; i < actualIfAfter.length; i++) {
            for (int j = 0; j < actualIfAfter[i].length; j++) {
                assertEquals(actualIfAfter[i][j], canDamageIfAfter[i][j],
                        String.format("Fail on after (%s, %s). Expected = %s", TrooperType.values()[i], TrooperType.values()[j], canDamageIfAfter[i][j])
                );
            }
        }
    }

    private void addBefore(TrooperType shooter, TrooperType target) {
        canDamageIfBefore[shooter.ordinal()][target.ordinal()] = true;
    }

    private void addAfter(TrooperType shooter, TrooperType target) {
        canDamageIfAfter[shooter.ordinal()][target.ordinal()] = true;
    }

    private void init(String order, TrooperType selfType) {
        this.moveOrder = order;
        this.selfType = selfType;
        canDamageIfBefore = new boolean[Utils.NUMBER_OF_TROOPER_TYPES][Utils.NUMBER_OF_TROOPER_TYPES];
        canDamageIfAfter = new boolean[Utils.NUMBER_OF_TROOPER_TYPES][Utils.NUMBER_OF_TROOPER_TYPES];
    }
}
