import static model.TrooperStance.*;
import static model.BonusType.*;

import static model.TrooperType.*;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test
public class AttackTest extends AbstractPlanComputerTest {
    @Test
    void testEmpty() {
        setMap(
                "S"
        );
        check(
                SOLDIER,
                0,
                STANDING, false, false, false);
    }
    @Test
    void testShoot() {
        setMap(
                "Ss"
        );
        setTrooper(1, 0, 100, STANDING);

        check(
                SOLDIER,
                4,
                STANDING, false, false, false, MyMove.shoot(1, 0)
        );

        //----------------------
        setMap(
                "S..s",
                "....",
                "c..."
        );
        setTrooper(3, 0, 1, STANDING);
        setTrooper(0, 2, 50, STANDING);

        check(
                SOLDIER,
                4,
                STANDING, false, false, false, MyMove.shoot(3, 0)
        );

        //----------------------
        setMap(
                "S..s",
                "....",
                "c..."
        );
        setTrooper(3, 0, 1, STANDING);
        setTrooper(0, 2, 2, STANDING);

        check(
                SOLDIER,
                4,
                STANDING, false, false, false, MyMove.shoot(0, 2)
        );
    }

    @Test
    void testShootBugRightAnswer() {
        setMap(
                "S..c",
                "....",
                "s..."
        );
        setTrooper(3, 0, 100, STANDING);
        setTrooper(0, 2, 30, STANDING);

        check(
                SOLDIER,
                4,
                STANDING, false, false, false, MyMove.shoot(0, 2)
        );
    }

    @Test
    void testObstacle() {
        setMap(
                "S.3.s"
        );
        setTrooper(4, 0, 100, STANDING);

        check(
                SOLDIER,
                4,
                STANDING,
                false, false, false);

        //----------------------
        setMap(
                "..f..",
                "..3..",
                "s.C3f",
                ".....",
                "..c.."
        );
        setTrooper(2, 0, 1, STANDING);
        setTrooper(4, 2, 1, STANDING);
        setTrooper(0, 2, 16, STANDING);
        setTrooper(2, 4, 15, STANDING);

        check(
                COMMANDER,
                3,
                STANDING,

                false, false, false, MyMove.shoot(2, 4)
        );

        //----------------------

        setMap(
                "S3.s"
        );
        setTrooper(3, 0, 1, STANDING);

        check(
                SOLDIER,
                12,
                STANDING,
                false,
                false,
                false);
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
        setTrooper(2, 0, 1, STANDING);
        setTrooper(4, 2, 1, STANDING);
        setTrooper(0, 2, 16, STANDING);
        setTrooper(2, 4, 16, STANDING);

        check(
                COMMANDER,
                3,
                STANDING,

                false, false, false, MyMove.shoot(2, 0)
        );

        //----------------------
        setMap(
                "f",
                "2",
                "C"
        );
        setTrooper(0, 0, 1, STANDING);

        check(
                COMMANDER,
                3,
                STANDING,

                false, false, false, MyMove.shoot(0, 0)
        );

        //----------------------
        setMap(
                "..f..",
                "..2..",
                "s.C1f",
                ".....",
                "..c.."
        );
        setTrooper(2, 0, 1, STANDING);
        setTrooper(4, 2, 1, KNEELING);
        setTrooper(0, 2, 16, STANDING);
        setTrooper(2, 4, 16, STANDING);

        check(
                COMMANDER,
                6,
                STANDING,

                false, false, false, MyMove.shoot(2, 0), MyMove.shoot(4, 2)
        );
    }

    @Test
    void testTooFar() {
        setMap(
                "F.....s"
        );
        setTrooper(6, 0, 1, PRONE);

        check(
                FIELD_MEDIC,
                2,
                STANDING,
                false,
                false,
                false,
                MyMove.MOVE_EAST
        );
    }

    @Test
    void testMovement() {
        setMap(
                "F.....s"
        );
        setTrooper(6, 0, 1, PRONE);

        check(
                FIELD_MEDIC,
                4,
                STANDING,
                false, false, false, MyMove.MOVE_EAST, MyMove.shoot(6, 0)
        );
        //-----------
        setMap(
                "...",
                "F3c",
                "..."
        );
        setTrooper(2, 1, 1, PRONE);

        check(
                FIELD_MEDIC,
                12,
                STANDING,
                false, false, false, MyMove.MOVE_NORTH, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.shoot(2, 1)
        );

        //-----------

        setMap(
                "c........S........f"
        );
        setTrooper(0, 0, 25, PRONE);
        setTrooper(18, 0, 25, KNEELING);

        check(
                SOLDIER,
                12,
                STANDING,
                false, false, false,
                MyMove.MOVE_EAST, MyMove.shoot(18, 0), MyMove.MOVE_WEST, MyMove.MOVE_WEST
        );

        //-----------

        setMap(
                "c.......C.......f"
        );
        setTrooper(0, 0, 15, PRONE);
        setTrooper(16, 0, 15, KNEELING);

        check(
                COMMANDER,
                12,
                STANDING,
                false, false, false, MyMove.MOVE_EAST, MyMove.shoot(16, 0), MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.shoot(0, 0)
        );

        //-----------
    }

    @Test
    void testEnemyBlocksPath() {
        setMap(
                "3c",
                "Fs"
        );
        setTrooper(1, 0, 1, PRONE);
        setTrooper(1, 1, 100, KNEELING);

        check(
                FIELD_MEDIC,
                6,
                STANDING,
                false,
                false, false, MyMove.shoot(1, 1), MyMove.shoot(1, 1), MyMove.shoot(1, 1)
        );
    }

    @Test
    void testMoveThroughDeadEnemy() {
        setMap(
                "3c.",
                "Fs.",
                "..."
        );
        setTrooper(1, 0, 1, PRONE);
        setTrooper(1, 1, 9, KNEELING);

        check(
                FIELD_MEDIC,
                6,
                STANDING,
                false,
                false, false, MyMove.shoot(1, 1), MyMove.MOVE_EAST, MyMove.shoot(1, 0)
        );
    }

    @Test
    void testFieldRation() {
        setMap(
                "c........S........f"
        );
        setTrooper(0, 0, 25, PRONE);
        setTrooper(18, 0, 25, KNEELING);

        check(
                SOLDIER,
                12,
                STANDING,
                true,
                false, false, MyMove.MOVE_EAST, MyMove.shoot(18, 0), MyMove.EAT_FIELD_RATION, MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.shoot(0, 0)
        );
        //------

        setMap(
                "s.c",
                ".3.",
                "F.."
        );
        setTrooper(0, 0, 9, PRONE);
        setTrooper(2, 0, 9, KNEELING);

        check(
                FIELD_MEDIC,
                5,
                STANDING,
                true,
                false,
                false,
                MyMove.EAT_FIELD_RATION, MyMove.MOVE_NORTH, MyMove.shoot(0, 0), MyMove.MOVE_NORTH, MyMove.shoot(2, 0)
        );
    }

    @Test
    void testRaiseStance() {
        setMap(
                "S1c"
        );
        setTrooper(2, 0, 9, KNEELING);

        check(
                SOLDIER,
                12,
                PRONE,
                false,
                false, false, MyMove.RAISE_STANCE, MyMove.shoot(2, 0)
        );

        setMap(
                "S1c"
        );
        setTrooper(2, 0, 9, KNEELING);

        check(
                SOLDIER,
                12,
                PRONE,
                false,
                false, false, MyMove.RAISE_STANCE, MyMove.shoot(2, 0)
        );
    }

    @Test
    void testLowerStance() {
        setMap(
                "Fs"
        );
        setTrooper(1, 0, 120, KNEELING);

        check(
                FIELD_MEDIC,
                12,
                STANDING,
                true,
                false, false, MyMove.LOWER_STANCE, MyMove.LOWER_STANCE, MyMove.shoot(1, 0), MyMove.EAT_FIELD_RATION, MyMove.shoot(1, 0), MyMove.shoot(1, 0), MyMove.shoot(1, 0), MyMove.shoot(1, 0)
        );
    }

    @Test
    void testStance() {
        setMap(
                "c.S2.f"
        );
        setTrooper(0, 0, 35, STANDING);
        setTrooper(5, 0, 25, STANDING);

        check(
                SOLDIER,
                6,
                KNEELING,
                false,
                false, false, MyMove.LOWER_STANCE, MyMove.shoot(0, 0)
        );

        //----------

        setMap(
                "c.S2.f"
        );
        setTrooper(0, 0, 36, STANDING);
        setTrooper(5, 0, 25, STANDING);

        check(
                SOLDIER,
                6,
                KNEELING,
                false,
                false, false, MyMove.RAISE_STANCE, MyMove.shoot(5, 0)
        );

        //---------

        setMap(
                "c.S2.f"
        );
        setTrooper(0, 0, 35, STANDING);
        setTrooper(5, 0, 25, STANDING);

        check(
                SOLDIER,
                12,
                KNEELING,
                true,
                false, false, MyMove.shoot(0, 0), MyMove.shoot(0, 0), MyMove.EAT_FIELD_RATION, MyMove.RAISE_STANCE, MyMove.shoot(5, 0)
        );
    }

    @Test
    void testDoNotWasteFieldRation() {
        setMap(
                "S"
        );

        check(
                SOLDIER,
                2,
                KNEELING,
                true,
                false, false);
    }

    @Test
    void testGrenade() {
        setMap(
                "S.3.f"
        );
        setTrooper(4, 0, 80, STANDING);
        check(
                SOLDIER,
                8,
                KNEELING,
                false,
                true,
                false, MyMove.grenade(4, 0)
        );

        //------
        setMap(
                "Sf"
        );
        setTrooper(1, 0, 80, STANDING);
        check(
                SOLDIER,
                8,
                KNEELING,
                false,
                true,
                false, MyMove.shoot(1, 0), MyMove.shoot(1, 0)
        );

        //------
        setMap(
                ".Sf"
        );
        setTrooper(2, 0, 80, STANDING);
        check(
                SOLDIER,
                10,
                STANDING,
                false,
                true,
                false, MyMove.MOVE_WEST, MyMove.grenade(2, 0)
        );

        //------
        setMap(
                "S,f"
        );
        setTrooper(2, 0, 70, STANDING);
        check(
                SOLDIER,
                10,
                PRONE,
                false,
                true,
                false, MyMove.shoot(2, 0), MyMove.shoot(2, 0)
        );
    }

    @Test
    void testDoNotDamageTeammateWithGrenade() {
        //------
        setMap(
                "S.fF"
        );
        setTrooper(2, 0, 80, STANDING);
        check(
                SOLDIER,
                8,
                STANDING,
                false,
                true,
                false, MyMove.shoot(2, 0), MyMove.shoot(2, 0)
        );
        //---------
        setMap(
                "S.fF",
                "...."
        );
        setTrooper(2, 0, 80, STANDING);
        check(
                SOLDIER,
                8,
                STANDING,
                false,
                true,
                false, MyMove.grenade(2, 1)
        );

        //---------
        setMap(
                "S3..F.f"
        );
        setTrooper(6, 0, 80, STANDING);
        check(
                SOLDIER,
                8,
                STANDING,
                false,
                true,
                false);

        //---------
        setMap(
                "S3.F..f"
        );
        setTrooper(6, 0, 80, STANDING);
        check(
                SOLDIER,
                8,
                STANDING,
                false,
                true,
                false, MyMove.grenade(5, 0)
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
        setTrooper(5, 0, 100, STANDING);
        setTrooper(4, 0, 100, STANDING);
        setTrooper(5, 1, 100, STANDING);
        check(
                FIELD_MEDIC,
                8,
                STANDING,
                true,
                true,
                false, MyMove.MOVE_NORTH, MyMove.EAT_FIELD_RATION, MyMove.grenade(5, 0)
        );
    }


    @Test
    void testFocusFire() {
        setMap(
                "....c",
                "S.C..",
                "s...."
        );
        setTrooper(4, 0, 100, STANDING);
        setTrooper(0, 2, 120, STANDING);
        check(
                COMMANDER,
                5,
                STANDING,
                false,
                false,
                false, MyMove.MOVE_SOUTH, MyMove.shoot(0, 2)
        );


        setMap(
                "....c",
                "S.C..",
                "s...."
        );
        setTrooper(4, 0, 100, STANDING);
        setTrooper(0, 2, 120, STANDING);
        check(
                COMMANDER,
                5,
                STANDING,
                false,
                false,
                false, MyMove.MOVE_SOUTH, MyMove.shoot(0, 2)
        );

        //---------------

        setMap(
                "cs.....S."
        );
        setTrooper(0, 0, 100, STANDING);
        setTrooper(1, 0, 120, STANDING);
        check(
                SOLDIER,
                4,
                STANDING,
                false,
                false,
                false, MyMove.shoot(0, 0)
        );

        //--------------
        setMap(
                "cs.....SC"
        );
        setTrooper(0, 0, 100, STANDING);
        setTrooper(1, 0, 120, STANDING);
        check(
                SOLDIER,
                4,
                STANDING,
                false,
                false,
                false, MyMove.shoot(1, 0)
        );

        //--------------
        setMap(
                "cs......SC",
                ".........."
        );
        setTrooper(0, 0, 100, STANDING);
        setTrooper(1, 0, 120, STANDING);
        check(
                SOLDIER,
                4,
                STANDING,
                false,
                false,
                false, MyMove.shoot(1, 0)
        );
        //--------------
    }

    @Test
    void testCollectBonus() {
        setMap(
                "S^..s"
        );
        setTrooper(4, 0, 80, STANDING);
        check(
                SOLDIER,
                7,
                STANDING,
                false,
                true,
                false, MyMove.MOVE_EAST, MyMove.EAT_FIELD_RATION, MyMove.grenade(4, 0)
        );
        //--------------

        setMap(
                "^..s",
                "S33."
        );
        setTrooper(3, 0, 79, STANDING);
        check(
                SOLDIER,
                7,
                STANDING,
                false,
                true,
                false, MyMove.MOVE_NORTH, MyMove.EAT_FIELD_RATION, MyMove.grenade(3, 0)
        );
        //--------------

        setMap(
                "^..s",
                "S33."
        );
        setTrooper(3, 0, 79, STANDING);
        check(
                SOLDIER,
                7,
                STANDING,
                false,
                true,
                false, MyMove.MOVE_NORTH, MyMove.EAT_FIELD_RATION, MyMove.grenade(3, 0)
        );
    }

    @Test
    void testStayOnBonus() {
        setMap(
                "F..s"
        );
        setTrooper(3, 0, 80, STANDING);
        addBonus(0, 0, GRENADE);
        check(
                FIELD_MEDIC,
                8,
                STANDING,
                false,
                false,
                false, MyMove.grenade(3, 0)
        );
    }

    @Test
    void testBerserkMedic() {
        setMap("F.s");
        setTrooper(2, 0, 1000, STANDING);
        addBonus(0, 0, FIELD_RATION);
        check(
                FIELD_MEDIC,
                12,
                PRONE,
                true, // FIELD_RATION
                false,

                false, MyMove.shoot(2, 0), MyMove.shoot(2, 0), MyMove.shoot(2, 0),
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
        setTrooper(1, 1, 120, STANDING);
        setTrooper(2, 1, 100, STANDING);
        setTrooper(1, 2, 100, STANDING);

        addBonus(4, 3, FIELD_RATION);

        check(
                SOLDIER,
                12,
                STANDING,
                true,
                true,

                false, MyMove.grenade(1, 1), MyMove.EAT_FIELD_RATION, MyMove.EAT_FIELD_RATION, MyMove.MOVE_NORTH, MyMove.grenade(1, 1)
        );
    }
    /**/
}