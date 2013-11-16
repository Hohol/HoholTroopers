import static model.TrooperStance.*;
import static model.BonusType.*;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test
public class AttackPlanComputerTest extends AbstractPlanComputerTest {

    @Test
    void testEmpty() {
        setMap(
                "S"
        );
        check(
                0,
                0, 0
                //empty
                ,
                STANDING, false, false);
    }

    @Test
    void testShoot() {
        setMap(
                "Ss"
        );
        addTrooper(1, 0, 100, STANDING);

        check(
                4,
                0, 0,
                STANDING, false, false, MyMove.shoot(1, 0)
        );

        //----------------------
        setMap(
                "S..s",
                "....",
                "c..."
        );
        addTrooper(3, 0, 1, STANDING);
        addTrooper(0, 2, 50, STANDING);

        check(
                4,
                0, 0,
                STANDING, false, false, MyMove.shoot(3, 0)
        );

        //----------------------
        setMap(
                "S..s",
                "....",
                "c..."
        );
        addTrooper(3, 0, 1, STANDING);
        addTrooper(0, 2, 2, STANDING);

        check(
                4,
                0, 0,
                STANDING, false, false, MyMove.shoot(0, 2)
        );
    }

    @Test
    void testShootBugRightAnswer() {
        setMap(
                "S..c",
                "....",
                "s..."
        );
        addTrooper(3, 0, 100, STANDING);
        addTrooper(0, 2, 30, STANDING);

        check(
                4,
                0, 0,
                STANDING, false, false, MyMove.shoot(0, 2)
        );
    }

    @Test
    void testObstacle() {
        setMap(
                "S.3.s"
        );
        addTrooper(4, 0, 100, STANDING);

        check(
                4,
                0, 0,
                STANDING,
                false, false);

        //----------------------
        setMap(
                "..f..",
                "..3..",
                "s.C3f",
                ".....",
                "..c.."
        );
        addTrooper(2, 0, 1, STANDING);
        addTrooper(4, 2, 1, STANDING);
        addTrooper(0, 2, 16, STANDING);
        addTrooper(2, 4, 15, STANDING);

        check(
                3,
                2, 2,
                STANDING,

                false, false, MyMove.shoot(2, 4)
        );

        //----------------------

        setMap(
                "S3.s"
        );
        addTrooper(3, 0, 1, STANDING);

        check(
                12,
                0, 0,
                STANDING,
                false,
                false
        );
    }

    @Test
    void testHeights() {
        //----------------------
        setMap(
                "..f..",
                "..2..",
                "s.C3f",
                ".....",
                "..c.."
        );
        addTrooper(2, 0, 1, STANDING);
        addTrooper(4, 2, 1, STANDING);
        addTrooper(0, 2, 16, STANDING);
        addTrooper(2, 4, 16, STANDING);

        check(
                3,
                2, 2,
                STANDING,

                false, false, MyMove.shoot(2, 0)
        );

        //----------------------
        setMap(
                "f",
                "2",
                "C"
        );
        addTrooper(0, 0, 1, STANDING);

        check(
                3,
                0, 2,
                STANDING,

                false, false, MyMove.shoot(0, 0)
        );

        //----------------------
        setMap(
                "..f..",
                "..2..",
                "s.C1f",
                ".....",
                "..c.."
        );
        addTrooper(2, 0, 1, STANDING);
        addTrooper(4, 2, 1, KNEELING);
        addTrooper(0, 2, 16, STANDING);
        addTrooper(2, 4, 16, STANDING);

        check(
                6,
                2, 2,
                STANDING,

                false, false, MyMove.shoot(2, 0), MyMove.shoot(4, 2)
        );
    }

    @Test
    void testTooFar() {
        setMap(
                "F.....s"
        );
        addTrooper(6, 0, 1, PRONE);

        check(
                2,
                0, 0,
                STANDING,
                false, false
        );
    }

    @Test
    void testMovement() {
        setMap(
                "F.....s"
        );
        addTrooper(6, 0, 1, PRONE);

        check(
                4,
                0, 0,
                STANDING,
                false, false, MyMove.MOVE_EAST, MyMove.shoot(6, 0)
        );
        //-----------
        setMap(
                "...",
                "F3c",
                "..."
        );
        addTrooper(2, 1, 1, PRONE);

        check(
                12,
                0, 1,
                STANDING,
                false, false, MyMove.MOVE_NORTH, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.shoot(2, 1)
        );

        //-----------

        setMap(
                "c........S........f"
        );
        addTrooper(0, 0, 25, PRONE);
        addTrooper(18, 0, 25, KNEELING);

        check(
                12,
                9, 0,
                STANDING,
                false, false, MyMove.MOVE_EAST, MyMove.shoot(18, 0)
        );

        //-----------

        setMap(
                "c.......C.......f"
        );
        addTrooper(0, 0, 15, PRONE);
        addTrooper(16, 0, 15, KNEELING);

        check(
                12,
                8, 0,
                STANDING,
                false, false, MyMove.MOVE_EAST, MyMove.shoot(16, 0), MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.shoot(0, 0)
        );

        //-----------
    }

    @Test
    void testEnemyBlocksPath() {
        setMap(
                "3c",
                "Fs"
        );
        addTrooper(1, 0, 1, PRONE);
        addTrooper(1, 1, 100, KNEELING);

        check(
                6,
                0, 1,
                STANDING,
                false,
                false, MyMove.shoot(1, 1), MyMove.shoot(1, 1), MyMove.shoot(1, 1)
        );
    }

    @Test
    void testMoveThroughDeadEnemy() {
        setMap(
                "3c",
                "Fs"
        );
        addTrooper(1, 0, 1, PRONE);
        addTrooper(1, 1, 9, KNEELING);

        check(
                6,
                0, 1,
                STANDING,
                false,
                false, MyMove.shoot(1, 1), MyMove.MOVE_EAST, MyMove.shoot(1, 0)
        );
    }

    @Test
    void testFieldRation() {
        setMap(
                "c........S........f"
        );
        addTrooper(0, 0, 25, PRONE);
        addTrooper(18, 0, 25, KNEELING);

        check(
                12,
                9, 0,
                STANDING,
                true,
                false, MyMove.MOVE_EAST, MyMove.shoot(18, 0), MyMove.EAT_FIELD_RATION, MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.shoot(0, 0)
        );
        //------

        setMap(
                "s.c",
                ".3.",
                "F.."
        );
        addTrooper(0, 0, 9, PRONE);
        addTrooper(2, 0, 9, KNEELING);

        check(
                5,
                0, 2,
                STANDING,
                true,
                false, MyMove.EAT_FIELD_RATION, MyMove.MOVE_NORTH, MyMove.shoot(0, 0), MyMove.MOVE_NORTH, MyMove.shoot(2, 0)
        );
    }

    @Test
    void testRaiseStance() {
        setMap(
                "S1c"
        );
        addTrooper(2, 0, 9, KNEELING);

        check(
                12,
                0, 0,
                PRONE,
                false,
                false, MyMove.RAISE_STANCE, MyMove.shoot(2, 0)
        );

        setMap(
                "S1c"
        );
        addTrooper(2, 0, 9, KNEELING);

        check(
                12,
                0, 0,
                PRONE,
                false,
                false, MyMove.RAISE_STANCE, MyMove.shoot(2, 0)
        );
    }

    @Test
    void testLowerStance() {
        setMap(
                "Fs"
        );
        addTrooper(1, 0, 120, KNEELING);

        check(
                12,
                0, 0,
                STANDING,
                true,
                false, MyMove.LOWER_STANCE, MyMove.LOWER_STANCE, MyMove.shoot(1, 0), MyMove.EAT_FIELD_RATION, MyMove.shoot(1, 0), MyMove.shoot(1, 0), MyMove.shoot(1, 0), MyMove.shoot(1, 0)
        );
    }

    @Test
    void testStance() {
        setMap(
                "c.S2.f"
        );
        addTrooper(0, 0, 35, STANDING);
        addTrooper(5, 0, 25, STANDING);

        check(
                6,
                2, 0,
                KNEELING,
                false,
                false, MyMove.LOWER_STANCE, MyMove.shoot(0, 0)
        );

        //----------

        setMap(
                "c.S2.f"
        );
        addTrooper(0, 0, 36, STANDING);
        addTrooper(5, 0, 25, STANDING);

        check(
                6,
                2, 0,
                KNEELING,
                false,
                false, MyMove.RAISE_STANCE, MyMove.shoot(5, 0)
        );

        //---------

        setMap(
                "c.S2.f"
        );
        addTrooper(0, 0, 35, STANDING);
        addTrooper(5, 0, 25, STANDING);

        check(
                12,
                2, 0,
                KNEELING,
                true,
                false, MyMove.shoot(0, 0), MyMove.shoot(0, 0), MyMove.EAT_FIELD_RATION, MyMove.RAISE_STANCE, MyMove.shoot(5, 0)
        );
    }

    @Test
    void testDoNotWasteFieldRation() {
        setMap(
                "S"
        );

        check(
                2,
                0, 0,
                KNEELING,
                true,
                false);
    }

    @Test
    void testGrenade() {
        setMap(
                "S.3.f"
        );
        addTrooper(4, 0, 80, STANDING);
        check(
                8,
                0, 0,
                KNEELING,
                false,
                true,
                MyMove.grenade(4, 0)
        );

        //------
        setMap(
                "Sf"
        );
        addTrooper(1, 0, 80, STANDING);
        check(
                8,
                0, 0,
                KNEELING,
                false,
                true,
                MyMove.shoot(1, 0), MyMove.shoot(1, 0)
        );

        //------
        setMap(
                ".Sf"
        );
        addTrooper(2, 0, 80, STANDING);
        check(
                10,
                1, 0,
                STANDING,
                false,
                true,
                MyMove.MOVE_WEST, MyMove.grenade(2, 0)
        );

        //------
        setMap(
                "S,f"
        );
        addTrooper(2, 0, 70, STANDING);
        check(
                10,
                0, 0,
                PRONE,
                false,
                true,
                MyMove.shoot(2, 0), MyMove.shoot(2, 0)
        );
    }

    @Test
    void testDoNotDamageTeammateWithGrenade() {
        //------
        setMap(
                "S.fF"
        );
        addTrooper(2, 0, 80, STANDING);
        check(
                8,
                0, 0,
                STANDING,
                false,
                true,
                MyMove.shoot(2, 0), MyMove.shoot(2, 0)
        );
        //---------
        setMap(
                "S.fF",
                "...."
        );
        addTrooper(2, 0, 80, STANDING);
        check(
                8,
                0, 0,
                STANDING,
                false,
                true,
                MyMove.grenade(2, 1)
        );

        //---------
        setMap(
                "S3..F.f"
        );
        addTrooper(6, 0, 80, STANDING);
        check(
                8,
                0, 0,
                STANDING,
                false,
                true
        );

        //---------
        setMap(
                "S3.F..f"
        );
        addTrooper(6, 0, 80, STANDING);
        check(
                8,
                0, 0,
                STANDING,
                false,
                true,
                MyMove.grenade(5, 0)
        );
    }

    @Test
    void testGrenadeForMassiveDamage() {
        //---------
        setMap(
                "....fc",
                "F....s",
                "......"
        );
        addTrooper(5, 0, 100, STANDING);
        addTrooper(4, 0, 100, STANDING);
        addTrooper(5, 1, 100, STANDING);
        check(
                8,
                0, 1,
                STANDING,
                true,
                true,
                MyMove.MOVE_NORTH, MyMove.EAT_FIELD_RATION, MyMove.grenade(5, 0)
        );
    }


    @Test
    void testFocusFire() {
        setMap(
                "....c",
                "S.C..",
                "s...."
        );
        addTrooper(4, 0, 100, STANDING);
        addTrooper(0, 2, 120, STANDING);
        check(
                5,
                2, 1,
                STANDING,
                false,
                false,
                MyMove.MOVE_SOUTH, MyMove.shoot(0, 2)
        );


        setMap(
                "....c",
                "S.C..",
                "s...."
        );
        addTrooper(4, 0, 100, STANDING);
        addTrooper(0, 2, 120, STANDING);
        check(
                5,
                2, 1,
                STANDING,
                false,
                false,
                MyMove.MOVE_SOUTH, MyMove.shoot(0, 2)
        );

        //---------------

        setMap(
                "cs.....S."
        );
        addTrooper(0, 0, 100, STANDING);
        addTrooper(1, 0, 120, STANDING);
        check(
                4,
                7, 0,
                STANDING,
                false,
                false,
                MyMove.shoot(0, 0)
        );

        //--------------
        setMap(
                "cs.....SC"
        );
        addTrooper(0, 0, 100, STANDING);
        addTrooper(1, 0, 120, STANDING);
        check(
                4,
                7, 0,
                STANDING,
                false,
                false,
                MyMove.shoot(1, 0)
        );

        //--------------
        setMap(
                "cs......SC"
        );
        addTrooper(0, 0, 100, STANDING);
        addTrooper(1, 0, 120, STANDING);
        check(
                4,
                8, 0,
                STANDING,
                false,
                false,
                MyMove.shoot(1, 0)
        );
    }

    @Test
    void testCollectBonus() {
        setMap(
                "S^..s"
        );
        addTrooper(4, 0, 80, STANDING);
        check(
                7,
                0, 0,
                STANDING,
                false,
                true,
                MyMove.MOVE_EAST, MyMove.EAT_FIELD_RATION, MyMove.grenade(4, 0)
        );
        //--------------

        setMap(
                "^..s",
                "S33."
        );
        addTrooper(3, 0, 79, STANDING);
        check(
                7,
                0, 1,
                STANDING,
                false,
                true,
                MyMove.MOVE_NORTH, MyMove.EAT_FIELD_RATION, MyMove.grenade(3, 0)
        );
        //--------------

        setMap(
                "^..s",
                "S33."
        );
        addTrooper(3, 0, 79, STANDING);
        check(
                7,
                0, 1,
                STANDING,
                false,
                true,
                MyMove.MOVE_NORTH, MyMove.EAT_FIELD_RATION, MyMove.grenade(3, 0)
        );
    }

    @Test
    void testStayOnBonus() {
        setMap(
                "F..s"
        );
        addTrooper(3, 0, 80, STANDING);
        addBonus(0, 0, GRENADE);
        check(
                8,
                0, 0,
                STANDING,
                false,
                false,
                MyMove.grenade(3, 0)
        );
    }

    @Test
    void testBerserkMedic() {
        setMap("F.s");
        addTrooper(2, 0, 1000, STANDING);
        addBonus(0, 0, FIELD_RATION);
        check(
                12,
                0, 0,
                PRONE,
                true, // FIELD_RATION
                false,

                MyMove.shoot(2, 0), MyMove.shoot(2, 0), MyMove.shoot(2, 0),
                MyMove.EAT_FIELD_RATION, MyMove.shoot(2, 0),
                MyMove.EAT_FIELD_RATION, MyMove.shoot(2, 0), MyMove.shoot(2, 0), MyMove.shoot(2, 0), MyMove.shoot(2, 0), MyMove.shoot(2, 0)
                // OMG STAHP!1
        );
    }

    @Test
    void test2Grenades2FieldRations() {
        setMap(
                "........",
                ".sc.....",
                ".f..*...",
                "....S..."
        );
        addTrooper(1, 1, 120, STANDING);
        addTrooper(2, 1, 100, STANDING);
        addTrooper(1, 2, 100, STANDING);

        addBonus(4, 3, FIELD_RATION);

        check(
                12,
                4, 3,
                STANDING,
                true,
                true,

                MyMove.grenade(1, 1), MyMove.EAT_FIELD_RATION, MyMove.EAT_FIELD_RATION, MyMove.MOVE_NORTH, MyMove.grenade(1, 1)
        );
    }
}