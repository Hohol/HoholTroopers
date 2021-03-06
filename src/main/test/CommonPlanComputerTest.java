import static model.TrooperType.*;
import static model.TrooperStance.*;

import org.testng.annotations.Test;

@Test
public class CommonPlanComputerTest extends TacticPlanComputerTest {

    @Test
    void testEmpty() {
        setMap("F");
        check(
                FIELD_MEDIC,
                12
        );
    }

    @Test
    void medicShouldHeal() {
        setMap("SF.s");

        ally(SOLDIER).hp(1);
        ally(FIELD_MEDIC).medikit();
        check(
                FIELD_MEDIC,
                2,
                MyMove.USE_MEDIKIT_WEST
        );
    }

    @Test
    void testHelp() {
        setMap(
                ".C......f",
                ".........",
                ".........",
                ".........",
                "S........"
        );

        check(
                SOLDIER,
                2,
                MyMove.MOVE_NORTH
        );
    }

    @Test
    void testHelp4() {
        setMap(
                "....S.",
                "..#...",
                "s....C",
                "......"
        );

        check(
                SOLDIER,
                2,
                MyMove.MOVE_SOUTH
        );
    }

    @Test
    void testDoNotWasteFieldRation() {
        setMap(
                "f...S^"
        );

        check(
                SOLDIER,
                4,
                MyMove.shoot(0, 0)
        );
    }

    @Test
    void testCollectMedikit() {
        setMap(
                "FS",
                ".+"
        );

        ally(SOLDIER).hp(1);

        check(
                FIELD_MEDIC,
                6,
                MyMove.MOVE_SOUTH, MyMove.MOVE_EAST, MyMove.USE_MEDIKIT_NORTH
        );
    }

    @Test
    void testCollectMedikit2() {
        setMap(
                "FSC",
                ".++"
        );

        ally(SOLDIER).hp(1);
        ally(COMMANDER).hp(1);

        check(
                FIELD_MEDIC,
                10,
                MyMove.MOVE_SOUTH, MyMove.MOVE_EAST, MyMove.USE_MEDIKIT_NORTH, MyMove.MOVE_EAST, MyMove.USE_MEDIKIT_NORTH
        );
    }

    @Test
    void testHelpDoNotGoTooFar() {
        setMap(
                ".####...........s........####.",
                ".####..####...##..c####..####.",
                ".......####..####..####.......",
                ".......####.f####..####.......",
                ".####..####...##...####..####.",
                ".####....................####.",
                ".####............C.......####.",
                ".####..####..####.S####..####.",
                ".......####..####..####.......",
                ".......####..####..####.......",
                ".......####..####..####.......",
                "..................F..........."
        );

        ally(FIELD_MEDIC).grenade();

        check(
                FIELD_MEDIC,
                10,
                MyMove.MOVE_EAST, MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.MOVE_NORTH, MyMove.MOVE_NORTH
        );
    }

    @Test
    void testMedicDistPriority() {
        setMap(
                "...f..C",
                ".3F3...",
                ".S.....",
                "......."
        );

        check(
                FIELD_MEDIC,
                2,
                MyMove.MOVE_SOUTH
        );
    }

    @Test
    void testNonMedicDistPriority() {
        setMap(
                ".........f..S",
                "3C3..........",
                "F............"
        );

        check(
                COMMANDER,
                2,
                "SCF",
                MyMove.MOVE_NORTH
        );
    }

    @Test
    void testMedicDistPriority2() {
        setMap(
                "C.f...",
                "......",
                "F.S...",
                "......"
        );
        theyDontHave(SOLDIER);
        theyDontHave(COMMANDER);

        check(
                FIELD_MEDIC,
                2,
                MyMove.MOVE_NORTH
        );
    }

    @Test
    void testFocusFireBug() {

        setMap(
                "S....s",
                ".#####",
                ".F....",
                "..f..."
        );

        enemy(SOLDIER).hp(90);
        ally(FIELD_MEDIC).grenade();

        check(
                FIELD_MEDIC,
                8,
                MyMove.grenade(5, 0)
        );
    }

    @Test
    void testDoNotBlockPathForTeammatesSimplest() {
        setMap(
                "S3333",
                "C...s"
        );

        check(
                COMMANDER,
                2,
                MyMove.MOVE_EAST
        );
    }

    @Test
    void doNotBlockPathForTeammates() {
        setMap(
                "S####",
                "C...s"
        );

        check(
                COMMANDER,
                5,
                MyMove.MOVE_EAST, MyMove.shoot(4, 1)
        );
    }

    @Test
    void testDoNotBlockPathForTeammates2() {
        setMap(
                ".s......SC"
        );
        check(
                SOLDIER,
                4,
                MyMove.MOVE_WEST
        );
    }

    @Test
    void testDoNotBlockPathForTeammates3() {
        setMap(
                "..f..C",
                "3F3...",
                "S....."
        );
        theyDontHave(COMMANDER);
        check(
                FIELD_MEDIC,
                2
        );
    }

    /*@Test
    void testDoNotBlockPathIgnoreMedic() { //FUKIN NO. Medic is the best labyrinth fighter
        setMap(
                "FS......s",
                ".C......s"
        );

        check(
                SOLDIER,
                4,
                MyMove.shoot(8, 0)
        );
    }/**/

    @Test
    void testBonusPriority() {
        setMap(
                "C.S.s"
        );

        ally(SOLDIER).fieldRation().grenade();

        check(
                SOLDIER,
                8,
                MyMove.grenade(4, 0)
        );
    }

    @Test
    void testBug() {
        setMap(
                "cs......#C",
                ".....#..S."
        );
        check(
                SOLDIER,
                2,
                MyMove.MOVE_WEST
        );
    }

    @Test
    void testMoveProne() {
        setMap(
                "S........",
                "........f"
        );
        ally(SOLDIER).fieldRation().stance(PRONE);
        theyDontHave(SOLDIER);
        check(
                SOLDIER,
                12,
                MyMove.MOVE_SOUTH, MyMove.EAT_FIELD_RATION, MyMove.shoot(8, 1), MyMove.shoot(8, 1)
        );
    }

    @Test
    void testEatMedikitIfItCanSaveYou() {
        setMap(
                "S#s",
                "..."
        );

        ally(SOLDIER).hp(80).medikit();
        enemy(SOLDIER).grenade();

        check(
                SOLDIER,
                2,
                MyMove.USE_MEDIKIT_SELF
        );
    }

    @Test
    void testEnemyCanUseGrenadeAndShoot() {
        setMap(
                "s....+S"
        );
        enemy(SOLDIER).grenade();
        ally(SOLDIER).grenade();
        check(
                SOLDIER,
                10,
                MyMove.LOWER_STANCE, MyMove.shoot(0, 0), MyMove.shoot(0, 0)
        );

        setMap(
                "s....+S."
        );
        enemy(SOLDIER).grenade();
        ally(SOLDIER).hp(85);
        check(
                SOLDIER,
                8,
                MyMove.MOVE_EAST, MyMove.LOWER_STANCE, MyMove.shoot(0, 0)
        );
    }

    @Test
    void testUseMedikitIfItCanSaveYou() {
        setMap(
                "s+....S"
        );
        ally(SOLDIER).hp(75);
        check(
                SOLDIER,
                12,
                MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.USE_MEDIKIT_SELF
        );
    }

    @Test
    void testMedicShouldHeal() {
        setMap(
                "F....................................S"
        );

        check(
                FIELD_MEDIC,
                2,
                MyMove.MOVE_EAST
        );
    }

    @Test
    void testMedicShouldHeal2() {
        setMap(
                "F..............",
                "##############.",
                "S.............."
        );

        check(
                FIELD_MEDIC,
                2,
                MyMove.MOVE_EAST
        );
    }

    @Test
    void testSniperRangeChange() {
        setMap(
                "R1.........s"
        );
        enemy(SOLDIER).hp(1);
        check(
                SNIPER,
                12,
                MyMove.LOWER_STANCE, MyMove.shoot(11, 0)
        );

        setMap(
                "R...........s"
        );
        enemy(SOLDIER).hp(95);
        ally(SNIPER).fieldRation();
        check(
                SNIPER,
                12,
                MyMove.MOVE_EAST, MyMove.LOWER_STANCE, MyMove.LOWER_STANCE, MyMove.EAT_FIELD_RATION, MyMove.shoot(12, 0)
        );
    }

    @Test
    void dontFuckingSuicide() {
        setMap(
                "####C#",
                "s.....",
                "#####S"
        );
        ally(COMMANDER).hp(1);
        ally(SOLDIER).hp(1);
        enemy(SOLDIER).hp(1);

        check(
                SOLDIER,
                2
        );
    }

    @Test
    void testHelpingMustConsiderStance() {
        setMap(
                "..R..",
                "11111",
                ".....",
                "..s.S"
        );
        ally(SNIPER).stance(PRONE);
        theyDontHave(SNIPER);
        check(
                SNIPER,
                2,
                MyMove.RAISE_STANCE
        );
    }

    @Test
    void sniperMustLowerStanceToIncreaseRange2() {
        setMap(
                "R1.........f"
        );
        check(
                SNIPER,
                2,
                MyMove.LOWER_STANCE
        );
    }

    @Test
    void dontSuicide2() {
        setMap(
                "#######r",
                "S......F"
        );
        enemy(SNIPER).fieldRation();
        check(
                FIELD_MEDIC,
                6,
                MyMove.shoot(7, 0), MyMove.shoot(7, 0), MyMove.MOVE_WEST
        );
    }

    @Test
    void dontBlockPathForMedic() {
        setMap(
                "#F",
                "SC",
                "..",
                "..",
                ".s"
        );
        ally(SOLDIER).hp(1);
        check(
                COMMANDER,
                3,
                MyMove.MOVE_SOUTH
        );
    }

    @Test
    void simpleScoutingInFight() {
        setMap(
                "S.1.s"
        );
        ally(SOLDIER).hp(1).stance(PRONE);
        enemy(SOLDIER).stance(PRONE);
        check(
                SOLDIER,
                8,
                MyMove.RAISE_STANCE, MyMove.LOWER_STANCE
        );
    }

    @Test
    void doNotLetEnemySeeYou() {
        setMap(
                "S#",
                ".f"
        );
        enemyDoesntKnowWhereWeAre();
        check(
                SOLDIER,
                10,
                MyMove.MOVE_SOUTH, MyMove.shoot(1, 1), MyMove.MOVE_NORTH
        );
    }

    @Test
    void enemyCouldSeeUsInStartingPosition() {
        setMap(
                "#####.###",
                "S.......c"
        );
        enemyDoesntKnowWhereWeAre();
        check(
                SOLDIER,
                12,
                MyMove.shoot(8, 1), MyMove.shoot(8, 1), MyMove.shoot(8, 1)
        );

        setMap(
                "#####.###",
                ".S......c"
        );
        setStartCell(0, 1, STANDING);
        enemyDoesntKnowWhereWeAre();
        check(
                SOLDIER,
                12,
                MyMove.shoot(8, 1), MyMove.shoot(8, 1), MyMove.shoot(8, 1)
        );
    }

    @Test
    void testEnemyCouldNotSeeUsInStartingPosition() {
        setMap(
                "S....1f"
        );
        enemyDoesntKnowWhereWeAre();
        setStartCell(0, 0, PRONE);
        check(
                SOLDIER,
                4,
                MyMove.LOWER_STANCE, MyMove.LOWER_STANCE
        );
    }

    @Test
    void assumeEnemiesDontBlockEachOther() {
        setMap(
                ".......1.#",
                "S......1fs",
                ".......#.#"
        );
        check(
                SOLDIER,
                2,
                MyMove.MOVE_SOUTH
        );
    }

    @Test
    void maximizeNumberOfStepsEnemyMustMakeToHideFromYou() {
        setMap(
                "...######",
                "R2.....1f"
        );
        check(
                SNIPER,
                8,
                MyMove.MOVE_NORTH, MyMove.MOVE_EAST, MyMove.MOVE_EAST, MyMove.MOVE_SOUTH
        );
    }

    @Test
    void preferScoutCellsNearEnemy() {
        setMap(
                ".s###",
                "S####",
                ".1..."
        );
        check(
                SOLDIER,
                4,
                MyMove.MOVE_NORTH, MyMove.MOVE_SOUTH
        );
    }

    @Test
    void testSniperBug() {
        setMap(
                "R2......1f"
        );
        ally(SNIPER).stance(KNEELING);
        check(
                SNIPER,
                10,
                MyMove.RAISE_STANCE
        );
    }

    @Test
    void weCanKnowTheirCommanderIsDead() {
        setMap(
                ".#r",
                ".F."
        );
        theyDontHave(COMMANDER);
        check(
                FIELD_MEDIC,
                6,
                MyMove.MOVE_EAST, MyMove.shoot(2, 0), MyMove.MOVE_WEST
        );
    }

    @Test
    void weCanKnowTheirCommanderIsDeadButCheckPlayerId() {
        setMap(
                ".#r",
                ".F."
        );
        theyDontHave(1, COMMANDER);
        theyDontHave(FIELD_MEDIC);
        check(
                FIELD_MEDIC,
                6,
                MyMove.MOVE_WEST, MyMove.MOVE_NORTH
        );
    }

    @Test
    void testBug2() {
        setMap(
                "R1.........r"
        );
        check(
                SNIPER,
                2,
                MyMove.LOWER_STANCE
        );
    }

    @Test
    void thereMayBeSniperEvenIfWeDontSeeHim() {
        setMap(
                "R.1......1f"
        );
        ally(SNIPER).stance(PRONE);
        check(
                SNIPER,
                3,
                "RFSC"
        );
    }

    @Test
    void enemyCanKnowPositionOfOurTroopersEvenIfDoesNotSeeThem() {
        setMap(
                "S.1......1r"
        );
        enemyKnowsPositionOf(SOLDIER);
        check(
                SOLDIER,
                2,
                MyMove.MOVE_EAST
        );
    }

    @Test
    void testImaginarySniper() {
        setMap(
                "S##",
                ".f."
        );
        ally(SOLDIER).stance(PRONE).hp(95);
        enemy(FIELD_MEDIC).hp(1);
        check(
                SOLDIER,
                10,
                "SFCR",
                MyMove.RAISE_STANCE, MyMove.RAISE_STANCE, MyMove.MOVE_SOUTH, MyMove.MOVE_NORTH
        );
    }

    @Test
    void testImaginaryNotOnlySniper() {
        setMap(
                "S1...#f"
        );
        theyDontHave(SNIPER);
        check(
                SOLDIER,
                4,
                "RFCST",
                MyMove.LOWER_STANCE, MyMove.LOWER_STANCE
        );
    }

    @Test
    void imaginaryEnemiesMustBeNearByBfsDist() {
        setMap(
                "S.....f....",
                ".##########",
                "######.####"
        );
        enemy(FIELD_MEDIC).hp(1);
        ally(SOLDIER).hp(95);
        enemyKnowsPositionOf(SOLDIER);

        theyDontHave(COMMANDER);
        theyDontHave(SOLDIER);
        theyDontHave(SCOUT);

        check(
                SOLDIER,
                4,
                "SFRTC",
                MyMove.MOVE_SOUTH
        );
    }

    @Test
    void dunnoHowToNameIt() {
        setMap(
                "s.........",
                "s........s",
                "####.S####"
        );
        ally(SOLDIER).hp(1);
        check(
                SOLDIER,
                2
        );
    }

    void preferAttackNonPhantomEnemies() {
        setMap(
                "c..",
                "#..",
                "S.s"
        );
        enemy(COMMANDER).lastSeenTime(-10);
        ally(SOLDIER).grenade();
        check(
                SOLDIER,
                8,
                MyMove.grenade(2, 2)
        );
    }

    @Test
    void preferAttackWellScoutedEnemy() {
        setMap(
                "R.........sf"
        );
        ally(SNIPER).stance(PRONE);
        setMediumMoveIndex(5);
        enemy(SOLDIER).lastSeenTime(5);
        enemy(FIELD_MEDIC).lastSeenTime(0);
        check(
                SNIPER,
                9,
                MyMove.shoot(10, 0)
        );
    }

    @Test
    void preferAttackWellScoutedEnemy2() {
        setMap(
                "R.........sf"
        );
        ally(SNIPER).stance(PRONE);
        setMediumMoveIndex(5);
        enemy(SOLDIER).lastSeenTime(0);
        enemy(FIELD_MEDIC).lastSeenTime(5);
        check(
                SNIPER,
                9,
                MyMove.shoot(11, 0)
        );
    }

    @Test
    void dontFuckingSuicide2() {
        setMap(
                ".S......r"
        );
        ally(SOLDIER).hp(1);
        enemy(SNIPER).stance(PRONE);
        check(
                SOLDIER,
                2,
                MyMove.MOVE_WEST
        );
    }

    @Test
    void dontFuckingSuicide3() {
        setMap(
                "Ts",
                ".#",
                ".."
        );
        ally(SCOUT).hp(1);
        enemyDoesntKnowWhereWeAre();
        enemy(SOLDIER).grenade();
        check(
                SCOUT,
                6,
                MyMove.MOVE_SOUTH, MyMove.MOVE_SOUTH, MyMove.MOVE_EAST
        );
    }

    @Test
    void comeCloserToThrowGrenade() {
        setMap(
                "S.....#s"
        );
        ally(SOLDIER).grenade();
        check(
                SOLDIER,
                2,
                MyMove.MOVE_EAST
        );
    }

    @Test
    void testInvestigation() {
        setMap(
                "S..",
                "##."
        );
        teammateWasDamaged(SOLDIER, defaultDamageValue);
        ally(SOLDIER).grenade();
        check(
                SOLDIER,
                8,
                MyMove.grenade(2,1)
        );
    }

    @Test
    void testInvestigation2() {
        setMap(
                "F1................"
        );
        teammateWasDamaged(FIELD_MEDIC, defaultDamageValue);
        check(
                FIELD_MEDIC,
                12,
                MyMove.LOWER_STANCE, MyMove.LOWER_STANCE
        );
    }

    @Test
    void enemyCanEasilyScoutYou() {
        setMap(
                "ST......1rt"
        );
        ally(SCOUT).hp(80);
        enemy(SNIPER).stance(PRONE);
        enemy(SCOUT).stance(PRONE);
        check(
                SCOUT,
                12,
                MyMove.LOWER_STANCE, MyMove.LOWER_STANCE
        );
    }

    @Test
    void sniperIsUsuallyUselessInLabyrinth() {
        setMap(
                ".F..",
                "#R#.",
                "###s"
        );
        check(
                FIELD_MEDIC,
                2
        );
    }

    @Test
    void placeImaginaryEnemyNearLastSeenPosition() {
        setMap(
                "#..........",
                ".#########f",
                "R........1."
        );
        setLastSeenEnemyPosition(SNIPER, 10, 2);
        setMoveOrder("RSTF");
        theyDontHave(SCOUT);
        theyDontHave(SOLDIER);
        check(
                SNIPER,
                2,
                MyMove.MOVE_NORTH
        );
    }

    @Test
    void testBuildingBug() {
        setMap(
                "..........",
                ".#1##1#2#.",
                ".#.....s1.",
                "........#.",
                ".##1##2#1.",
                ".........S"
        );
        enemy(SOLDIER).stance(PRONE);
        check(
                SOLDIER,
                12,
                MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.MOVE_WEST, MyMove.MOVE_WEST
        );
    }
}