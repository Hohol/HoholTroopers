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
        setTrooper(1, 0, 100, STANDING);

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
        setTrooper(3, 0, 1, STANDING);
        setTrooper(0, 2, 50, STANDING);

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
        setTrooper(3, 0, 1, STANDING);
        setTrooper(0, 2, 2, STANDING);

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
        setTrooper(4, 0, 100, STANDING);

        check(
                SOLDIER,
                4
        );

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

                MyMove.shoot(2, 4)
        );

        //----------------------

        setMap(
                "S3.s"
        );
        setTrooper(3, 0, 1, STANDING);

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
        setTrooper(2, 0, 1, STANDING);
        setTrooper(4, 2, 1, STANDING);
        setTrooper(0, 2, 16, STANDING);
        setTrooper(2, 4, 16, STANDING);

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
        setTrooper(0, 0, 1, STANDING);

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
        setTrooper(2, 0, 1, STANDING);
        setTrooper(4, 2, 1, KNEELING);
        setTrooper(0, 2, 16, STANDING);
        setTrooper(2, 4, 16, STANDING);

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
        setTrooper(0, 0, 35, STANDING);
        setTrooper(5, 0, 25, STANDING);

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
        setTrooper(0, 0, 36, STANDING);
        setTrooper(5, 0, 25, STANDING);

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
        setTrooper(0, 0, 35, STANDING);
        setTrooper(5, 0, 25, STANDING);

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
        setTrooper(4, 0, 80, STANDING);
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
        setTrooper(1, 0, 80, STANDING);
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
        setTrooper(2, 0, 70, STANDING);
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
        setTrooper(2, 0, 80, STANDING);
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
        setTrooper(2, 0, 80, STANDING);
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
        setTrooper(6, 0, 80, STANDING);
        ally(SOLDIER).grenade();
        check(
                SOLDIER,
                8
        );

        //---------
        setMap(
                "S3.F..f"
        );
        setTrooper(6, 0, 80, STANDING);
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
        setTrooper(0, 0, 100, STANDING);
        setTrooper(1, 0, 120, STANDING);
        check(
                SOLDIER,
                4,
                MyMove.shoot(1, 0)
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
        setTrooper(4, 0, 80, STANDING);
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
        setTrooper(3, 0, 79, STANDING);
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
        setTrooper(3, 0, 79, STANDING);
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
        setTrooper(3, 0, 80, STANDING);
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
        setTrooper(2, 0, 1000, STANDING);
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