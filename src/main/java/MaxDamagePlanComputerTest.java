import model.*;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static model.TrooperType.*;
import static model.TrooperStance.*;
import static model.ActionType.*;
import static org.testng.Assert.*;

@Test
public class MaxDamagePlanComputerTest {
    @Test
    void testEmpty() {
        check(
                SOLDIER,
                1,
                STANDING,
                STANDING,
                100,
                false
        );
        check(
                FIELD_MEDIC,
                1,
                STANDING,
                STANDING,
                100,
                true
        );
    }

    @Test
    void test() {
        check(
                SOLDIER,
                4,
                STANDING, STANDING,
                30,
                false,

                SHOOT
        );
        check(
                COMMANDER,
                8,
                PRONE, PRONE,
                100,
                false,

                SHOOT, SHOOT
        );
    }

    @Test
    void testFieldRationEating() {
        check(
                FIELD_MEDIC,
                2,
                PRONE, PRONE,
                100,
                true,

                EAT_FIELD_RATION, SHOOT, SHOOT
        );
        check(
                SOLDIER,
                10,
                STANDING, STANDING,
                100,
                false,

                SHOOT, SHOOT
        );
        check(
                SOLDIER,
                10,
                STANDING, STANDING,
                100,
                true,

                SHOOT, EAT_FIELD_RATION, SHOOT, SHOOT
        );
        check(
                SOLDIER,
                12,
                STANDING, STANDING,
                100,
                true,

                SHOOT, SHOOT, SHOOT
        );
        check(
                SOLDIER,
                10,
                PRONE, PRONE,
                70,
                true,

                SHOOT, SHOOT
        );
        check(
                SOLDIER,
                10,
                PRONE, PRONE,
                71,
                true,

                SHOOT, EAT_FIELD_RATION, SHOOT, SHOOT
        );
        check(
                SOLDIER,
                10,
                STANDING, STANDING,
                60,
                true,

                SHOOT, EAT_FIELD_RATION, SHOOT, SHOOT
        );
        check(
                SOLDIER,
                10,
                STANDING, STANDING,
                61,
                true,

                SHOOT, EAT_FIELD_RATION, SHOOT, SHOOT
        );
    }

    @Test
    void testLowering() {
        check(
                COMMANDER,
                5,
                KNEELING,
                PRONE,
                21,
                false,

                LOWER_STANCE, SHOOT
        );

        check(
                SOLDIER,
                12,
                STANDING,
                KNEELING,
                100,
                true,

                LOWER_STANCE, SHOOT, EAT_FIELD_RATION, SHOOT, SHOOT
        );
        check(
                SOLDIER,
                12,
                STANDING,
                KNEELING,
                120,
                true,

                LOWER_STANCE, SHOOT, EAT_FIELD_RATION, SHOOT, SHOOT
        );
        check(
                SOLDIER,
                12,
                STANDING,
                PRONE,
                90,
                true,

                LOWER_STANCE, SHOOT, EAT_FIELD_RATION, SHOOT, SHOOT
        );
        check(
                SOLDIER,
                12,
                STANDING,
                PRONE,
                120,
                true,

                LOWER_STANCE, LOWER_STANCE, SHOOT, SHOOT
        );
    }

    @Test
    void testBerserkMedic() {
        check(
                FIELD_MEDIC,
                12,
                STANDING,
                PRONE,
                120,
                true,

                LOWER_STANCE, LOWER_STANCE, SHOOT, EAT_FIELD_RATION, SHOOT, SHOOT, SHOOT, SHOOT
        );
    }

    @Test(enabled = false)
    void findLongestPlan() { //it has length 8
        List<ActionType> r = null;
        for (TrooperType selfType : TrooperType.values()) {
            if (selfType == SNIPER || selfType == SCOUT) {
                continue;
            }
            for (int actionPoints = 1; actionPoints <= 12; actionPoints++) {
                for (TrooperStance curStance : TrooperStance.values()) {
                    for (TrooperStance minStance : TrooperStance.values()) {
                        for (int hp = 1; hp <= 120; hp++) {
                            List<ActionType> test = new MaxDamagePlanComputer(
                                    selfType,
                                    actionPoints,
                                    curStance,
                                    minStance,
                                    hp,
                                    true,
                                    Utils.HARDCODED_UTILS
                            ).getActions();
                            if (r == null || test.size() > r.size()) {
                                r = test;
                            }
                        }
                    }
                }
            }
        }
        System.out.println(r);
    }

    private void check(
            TrooperType selfType,
            int actionPoints,
            TrooperStance currentStance,
            TrooperStance minStanceAllowed,
            int targetHp,
            boolean holdingFieldRation,
            ActionType... expectedAr
    ) {
        List<ActionType> actual = new MaxDamagePlanComputer(
                selfType,
                actionPoints,
                currentStance, minStanceAllowed,
                targetHp,
                holdingFieldRation,
                Utils.HARDCODED_UTILS
        ).getActions();
        List<ActionType> expected = Arrays.asList(expectedAr);
        assertEquals(
                actual,
                expected,
                String.format("\n\nActual: %s \nExpected: %s\n\n", actual, expected)
        );
    }
}
