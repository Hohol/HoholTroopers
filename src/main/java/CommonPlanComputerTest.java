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
}
