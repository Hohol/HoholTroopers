import model.Game;
import model.Trooper;
import model.TrooperStance;
import model.TrooperType;

import static model.TrooperStance.KNEELING;
import static model.TrooperStance.PRONE;
import static model.TrooperType.SCOUT;

class Utils {

    final static Game hardcodedGame = new Game(
            50, 100, 50, 25, 1.0, 2, 2, 4, 6, 2, 5.0, 10, 5, 1, 5, 3, 0.0, 0.5, 1.0, 0.0, 1.0, 2.0, 1.0, 8, 5.0, 80, 60, 2, 50, 30, 2, 5
    );

    private final Game game;
    private final TrooperParameters trooperParameters;

    Utils(Game game, TrooperParameters trooperParameters) {
        this.game = game;
        this.trooperParameters = trooperParameters;
    }

    TrooperStance stanceAfterLowering(TrooperStance stance) {
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

    int actionPointsAfterEatingFieldRation(TrooperType type, int actionPoints, Game game) {
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

    public Game getGame() {
        return game;
    }


    public int getShootDamage(TrooperType type, TrooperStance stance) {
        return trooperParameters.getShootDamage(type, stance);
    }

    public int getShootCost(TrooperType type) {
        return trooperParameters.getShootCost(type);
    }
}
