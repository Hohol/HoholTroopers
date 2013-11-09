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
    final static Game game = new Game(
            50, 100, 50, 25, 1.0, 2, 2, 4, 6, 2, 5.0, 1, 5, 3, 0.0, 0.5, 1.0, 0.0, 1.0, 2.0, 1.0, 8, 5.0, 80, 60, 2, 50, 30, 2, 5
    );

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
                STANDING, STANDING,
                60,
                true,

                SHOOT, SHOOT
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
                100,
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

                LOWER_STANCE, SHOOT, EAT_FIELD_RATION, SHOOT, SHOOT
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

    @Test (enabled = false)
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
                            List<ActionType> test = new MyStrategy.MaxDamagePlanComputer(
                                    selfType,
                                    actionPoints,
                                    curStance,
                                    minStance,
                                    hp,
                                    true,
                                    game
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
        List<ActionType> actual = new MyStrategy.MaxDamagePlanComputer(
                selfType,
                actionPoints,
                currentStance, minStanceAllowed,
                targetHp,
                holdingFieldRation,
                game
        ).getActions();
        List<ActionType> expected = Arrays.asList(expectedAr);
        System.out.println("Actual: " + actual);
        System.out.println("Expected: " + expected);
        System.out.println();
        assertEquals(
                actual,
                expected
        );
    }
}
