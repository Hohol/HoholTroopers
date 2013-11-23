import static model.TrooperStance.*;
import static model.BonusType.*;

import static model.TrooperType.*;

import org.testng.annotations.Test;

@Test
public class AttackTest extends AbstractPlanComputerTest {
    @Test
    void testEmpty() {
        setMap(
                "S"
        );
        check(
                SOLDIER,
                0
        );
    }

    @Test
    void testShoot() {
        setMap(
                "Ss"
        );

        check(
                SOLDIER,
                4,
                MyMove.shoot(1, 0)
        );

        //----------------------
        setMap(
                "S..s",
                "....",
                "c..."
        );
        enemy(SOLDIER).hp(1);
        enemy(COMMANDER).hp(50);

        check(
                SOLDIER,
                4,
                MyMove.shoot(3, 0)
        );

        //----------------------
        setMap(
                "S..s",
                "....",
                "s..."
        );
        enemy(0, 2).hp(2);
        enemy(3, 0).hp(1);

        check(
                SOLDIER,
                4,
                MyMove.shoot(0, 2)
        );
    }

    @Test
    void testObstacle() {
        setMap(
                "S.3.s"
        );

        check(
                SOLDIER,
                4
        );

        //----------------------

        setMap(
                "S3.s"
        );
        enemy(SOLDIER).hp(1);

        check(
                SOLDIER,
                12
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
        enemy(2, 0).hp(1);
        enemy(4, 2).hp(1);
        enemy(0, 2).hp(16);
        enemy(2, 4).hp(16);

        check(
                COMMANDER,
                3,

                MyMove.shoot(2, 0)
        );

        //----------------------
        setMap(
                "f",
                "2",
                "C"
        );
        enemy(FIELD_MEDIC).hp(1);

        check(
                COMMANDER,
                3,

                MyMove.shoot(0, 0)
        );

        //----------------------
        setMap(
                "..f..",
                "..2..",
                "s.C1f",
                ".....",
                "..c.."
        );
        enemy(2, 0).hp(1);
        enemy(4, 2).hp(1);
        enemy(0, 2).hp(16);
        enemy(2, 4).hp(16);

        check(
                COMMANDER,
                6,

                MyMove.shoot(2, 0), MyMove.shoot(4, 2)
        );
    }

    @Test
    void testTooFar() {
        setMap(
                "F.....s"
        );
        enemy(SOLDIER).stance(PRONE);

        check(
                FIELD_MEDIC,
                2,
                MyMove.MOVE_EAST
        );
    }

    @Test
    void testMovement() {
        setMap(
                "F.....s"
        );
        enemy(SOLDIER).stance(PRONE);

        check(
                FIELD_MEDIC,
                4,
                MyMove.MOVE_EAST, MyMove.shoot(6, 0)
        );
        //-----------
        setMap(
                "...",
                "F3c",
                "..."
        );
        enemy(COMMANDER).hp(1);

        check(
                FIELD_MEDIC,
                12,
                MyMove.MOVE_NORTH, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.shoot(2, 1)
        );

        //-----------

        setMap(
                "c.......C.......f"
        );
        enemy(COMMANDER).hp(15).stance(KNEELING);
        enemy(FIELD_MEDIC).hp(15).stance(KNEELING);

        check(
                COMMANDER,
                12,
                MyMove.MOVE_EAST, MyMove.shoot(16, 0), MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.shoot(0, 0)
        );

        //-----------
    }

    @Test
    void testEnemyBlocksPath() {
        setMap(
                "3c",
                "Fs"
        );
        enemy(COMMANDER).hp(1);

        check(
                FIELD_MEDIC,
                6,
                MyMove.shoot(1, 1), MyMove.shoot(1, 1), MyMove.shoot(1, 1)
        );
    }

    @Test
    void testMoveThroughDeadEnemy() {
        setMap(
                "3c.",
                "Fs.",
                "..."
        );
        enemy(COMMANDER).hp(1);
        enemy(SOLDIER).hp(1);

        check(
                FIELD_MEDIC,
                6,
                MyMove.shoot(1, 1), MyMove.MOVE_EAST, MyMove.shoot(1, 0)
        );
    }

    @Test
    void testFieldRation() {
        setMap(
                "c........S........f"
        );
        enemy(COMMANDER).hp(25);
        enemy(FIELD_MEDIC).hp(25);

        ally(SOLDIER).fieldRation();
        check(
                SOLDIER,
                12,
                MyMove.MOVE_EAST, MyMove.shoot(18, 0), MyMove.EAT_FIELD_RATION, MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.shoot(0, 0)
        );
        //------

        setMap(
                "s.c",
                ".3.",
                "F.."
        );

        enemy(COMMANDER).hp(9);
        enemy(SOLDIER).hp(9);

        ally(FIELD_MEDIC).fieldRation();

        check(
                FIELD_MEDIC,
                5,
                MyMove.EAT_FIELD_RATION, MyMove.MOVE_NORTH, MyMove.shoot(0, 0), MyMove.MOVE_NORTH, MyMove.shoot(2, 0)
        );
    }

    @Test
    void testRaiseStance() {
        setMap(
                "S1c"
        );
        enemy(COMMANDER).hp(9).stance(KNEELING);
        ally(SOLDIER).stance(PRONE);

        check(
                SOLDIER,
                12,
                MyMove.RAISE_STANCE, MyMove.shoot(2, 0)
        );
    }

    @Test
    void testLowerStance() {
        setMap(
                "Fs"
        );
        enemy(SOLDIER).hp(120);
        ally(FIELD_MEDIC).fieldRation();

        check(
                FIELD_MEDIC,
                12,
                MyMove.LOWER_STANCE, MyMove.LOWER_STANCE, MyMove.shoot(1, 0), MyMove.EAT_FIELD_RATION, MyMove.shoot(1, 0), MyMove.shoot(1, 0), MyMove.shoot(1, 0), MyMove.shoot(1, 0)
        );
    }

    @Test
    void testStance() {
        setMap(
                "c.S2.f"
        );
        enemy(COMMANDER).hp(35);
        enemy(FIELD_MEDIC).hp(25);

        ally(SOLDIER).stance(KNEELING);

        check(
                SOLDIER,
                6,
                MyMove.LOWER_STANCE, MyMove.shoot(0, 0)
        );

        //----------

        setMap(
                "c.S2.f"
        );
        enemy(COMMANDER).hp(36);
        enemy(FIELD_MEDIC).hp(25);

        ally(SOLDIER).stance(KNEELING);

        check(
                SOLDIER,
                6,
                MyMove.RAISE_STANCE, MyMove.shoot(5, 0)
        );

        //---------

        setMap(
                "c.S2.f"
        );
        enemy(COMMANDER).hp(35);
        enemy(FIELD_MEDIC).hp(25);

        ally(SOLDIER).fieldRation().stance(KNEELING);

        check(
                SOLDIER,
                12,
                MyMove.shoot(0, 0), MyMove.shoot(0, 0), MyMove.EAT_FIELD_RATION, MyMove.RAISE_STANCE, MyMove.shoot(5, 0)
        );
    }

    @Test
    void testDoNotWasteFieldRation() {
        setMap(
                "S"
        );

        ally(SOLDIER).fieldRation();

        check(
                SOLDIER,
                2
        );
    }

    @Test
    void testGrenade() {
        setMap(
                "S.3.f"
        );
        enemy(FIELD_MEDIC).hp(80);
        ally(SOLDIER).grenade();
        check(
                SOLDIER,
                8,
                MyMove.grenade(4, 0)
        );

        //------
        setMap(
                "Sf"
        );
        enemy(FIELD_MEDIC).hp(80);
        ally(SOLDIER).grenade();
        check(
                SOLDIER,
                8,
                MyMove.shoot(1, 0), MyMove.shoot(1, 0)
        );

        //------
        setMap(
                ".Sf"
        );

        ally(SOLDIER).grenade();
        check(
                SOLDIER,
                10,
                MyMove.MOVE_WEST, MyMove.grenade(2, 0)
        );

        //------
        setMap(
                "S.f"
        );
        enemy(FIELD_MEDIC).hp(70);
        ally(SOLDIER).grenade().stance(PRONE);
        check(
                SOLDIER,
                10,
                MyMove.shoot(2, 0), MyMove.shoot(2, 0)
        );
    }

    @Test
    void testDoNotDamageTeammateWithGrenade() {
        //------
        setMap(
                "S.fF"
        );
        enemy(FIELD_MEDIC).hp(80);
        ally(SOLDIER).grenade();
        check(
                SOLDIER,
                8,
                MyMove.shoot(2, 0), MyMove.shoot(2, 0)
        );
        //---------
        setMap(
                "..fF",
                "S..."
        );
        enemy(FIELD_MEDIC).hp(80);
        ally(SOLDIER).grenade();
        check(
                SOLDIER,
                8,
                MyMove.grenade(1, 0)
        );

        //---------
        setMap(
                "S3..F.f"
        );
        enemy(FIELD_MEDIC).hp(80);
        ally(SOLDIER).grenade();
        check(
                SOLDIER,
                8
        );

        //---------
        setMap(
                "S3.F..f"
        );
        enemy(FIELD_MEDIC).hp(80);
        ally(SOLDIER).grenade();
        check(
                SOLDIER,
                8,
                MyMove.grenade(5, 0)
        );
    }

    @Test
    void testGrenadeForMassiveDamage() {
        //---------
        setMap(
                ".#..fc",
                "F#...s"
        );

        ally(FIELD_MEDIC).grenade().fieldRation();

        check(
                FIELD_MEDIC,
                8,
                MyMove.MOVE_NORTH, MyMove.EAT_FIELD_RATION, MyMove.grenade(5, 0)
        );
    }


    @Test
    void testFocusFire() {
        setMap(
                "fs.....SC"
        );
        check(
                SOLDIER,
                4,
                MyMove.shoot(1, 0)
        );

        //--------------
        setMap(
                "cs.....SC"
        );
        check(
                SOLDIER,
                4,
                MyMove.shoot(1, 0)
        );
    }

    @Test
    void testFocusFire2() {
        setMap(
                "......c",
                "S..C..#",
                "c......"
        );
        check(
                COMMANDER,
                5,
                MyMove.MOVE_SOUTH, MyMove.shoot(0, 2)
        );
    }

    @Test
    void testCollectBonus() {
        setMap(
                "S^..s"
        );
        enemy(SOLDIER).hp(80);
        ally(SOLDIER).grenade();
        check(
                SOLDIER,
                7,
                MyMove.MOVE_EAST, MyMove.EAT_FIELD_RATION, MyMove.grenade(4, 0)
        );
        //--------------

        setMap(
                "^..s",
                "S33."
        );
        enemy(SOLDIER).hp(79);
        ally(SOLDIER).grenade();
        check(
                SOLDIER,
                7,
                MyMove.MOVE_NORTH, MyMove.EAT_FIELD_RATION, MyMove.grenade(3, 0)
        );
        //--------------

        setMap(
                "^..s",
                "S33."
        );
        enemy(SOLDIER).hp(79);
        ally(SOLDIER).grenade();
        check(
                SOLDIER,
                7,
                MyMove.MOVE_NORTH, MyMove.EAT_FIELD_RATION, MyMove.grenade(3, 0)
        );
    }

    @Test
    void testStayOnBonus() {
        setMap(
                "F..s"
        );
        enemy(SOLDIER).hp(80);
        addBonus(0, 0, GRENADE);
        check(
                FIELD_MEDIC,
                8,
                MyMove.grenade(3, 0)
        );
    }

    @Test
    void testBerserkMedic() {
        setMap("F.s");
        enemy(SOLDIER).hp(1000);
        addBonus(0, 0, FIELD_RATION);
        ally(FIELD_MEDIC).fieldRation().stance(PRONE);
        check(
                FIELD_MEDIC,
                12,

                MyMove.shoot(2, 0), MyMove.shoot(2, 0), MyMove.shoot(2, 0),
                MyMove.EAT_FIELD_RATION, MyMove.shoot(2, 0),
                MyMove.EAT_FIELD_RATION, MyMove.shoot(2, 0), MyMove.shoot(2, 0), MyMove.shoot(2, 0), MyMove.shoot(2, 0), MyMove.shoot(2, 0)
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

        addBonus(4, 3, FIELD_RATION);
        ally(SOLDIER).grenade().fieldRation();

        check(
                SOLDIER,
                12,

                MyMove.grenade(1, 1), MyMove.EAT_FIELD_RATION, MyMove.EAT_FIELD_RATION, MyMove.MOVE_NORTH, MyMove.grenade(1, 1)
        );
    }
}