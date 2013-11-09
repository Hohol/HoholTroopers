import model.Game;
import model.Trooper;
import model.TrooperStance;
import model.TrooperType;

import static model.TrooperStance.KNEELING;
import static model.TrooperStance.PRONE;
import static model.TrooperType.SCOUT;

public class Utils {
    static TrooperStance stanceAfterLowering(TrooperStance stance) {
        switch (stance) {
            case PRONE:
                return null;
            case KNEELING:
                return PRONE;
            case STANDING:
                return KNEELING;
        }
        throw new RuntimeException();
    }

    static int getShootDamage(TrooperType type, TrooperStance stance) {
        return standingDamage(type) + bonusDamage(type) * (3 - stance.ordinal() - 1);
    }

    static int bonusDamage(TrooperType type) {
        switch (type) {
            case COMMANDER:
                return 5;
            case FIELD_MEDIC:
                return 3;
            case SOLDIER:
                return 5;
            case SNIPER:
                break;
            case SCOUT:
                break;
        }
        throw new RuntimeException();
    }

    static int standingDamage(TrooperType type) {
        switch (type) {
            case COMMANDER:
                return 15;
            case FIELD_MEDIC:
                return 9;
            case SOLDIER:
                return 30;
            case SNIPER:
                break;
            case SCOUT:
                break;
        }
        throw new RuntimeException();
    }

    static int getShootCost(TrooperType type) {
        switch (type) {
            case COMMANDER:
                return 3;
            case FIELD_MEDIC:
                return 2;
            case SOLDIER:
                return 4;
            case SNIPER:
                break;
            case SCOUT:
                break;
        }
        throw new RuntimeException();
    }

    static int actionPointsAfterEatingFieldRation(TrooperType type, int actionPoints, Game game) {
        int r = actionPoints - game.getFieldRationEatCost() + game.getFieldRationBonusActionPoints();
        int initialActionPoints = (type == SCOUT ? 12 : 10);
        r = Math.min(r, initialActionPoints);
        return r;
    }

    static int sqrDist(int x, int y, int x1, int y1) {
        return sqr(x - x1) + sqr(y - y1);
    }

    static int sqr(int x) {
        return x * x;
    }

    static double sqr(double x) {
        return x * x;
    }
}
