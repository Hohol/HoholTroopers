import model.*;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import static model.TrooperStance.KNEELING;
import static model.TrooperStance.PRONE;

public class Utils {

    final static Game hardcodedGame = new Game(
            50, 100, 50, 25, 1.0, 2, 2, 4, 6, 2, 5.0, 10, 5, 1, 5, 3, 0.0, 0.5, 1.0, 0.0, 1.0, 2.0, 1.0, 8, 5.0, 80, 60, 2, 50, 30, 2, 5
    );
    public static final int INITIAL_TROOPER_HP = 100;
    final static Direction[] dirs = {Direction.WEST, Direction.SOUTH, Direction.EAST, Direction.NORTH};
    final static int UNREACHABLE = 666;
    public static final int NUMBER_OF_TROOPER_TYPES = TrooperType.values().length;

    private final Game game;
    private final TrooperParameters trooperParameters;

    public Utils(Game game, TrooperParameters trooperParameters) {
        this.game = game;
        this.trooperParameters = trooperParameters;
    }

    public TrooperStance stanceAfterLowering(TrooperStance stance) {
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

    public int actionPointsAfterEatingFieldRation(TrooperType type, int actionPoints, Game game) {
        int r = actionPoints - game.getFieldRationEatCost() + game.getFieldRationBonusActionPoints();
        int initialActionPoints = trooperParameters.getInitialActionPoints(type);
        r = Math.min(r, initialActionPoints);
        return r;
    }

    public static int sqrDist(int x, int y, int x1, int y1) {
        return sqr(x - x1) + sqr(y - y1);
    }

    static int sqr(int x) {
        return x * x;
    }

    public static double sqr(double x) {
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

    public int getInitialActionPoints(TrooperType type) {
        return trooperParameters.getInitialActionPoints(type);
    }

    public static Move createMove(ActionType action, Direction direction) {
        Move r = new Move();
        r.setAction(action);
        r.setDirection(direction);
        return r;
    }

    public static Move createMove(ActionType action, int x, int y) {
        Move r = new Move();
        r.setAction(action);
        r.setX(x);
        r.setY(y);
        return r;
    }

    public static Move createMove(ActionType action) {
        Move r = new Move();
        r.setAction(action);
        return r;
    }

    public static char getCharForTrooperType(TrooperType type) {
        switch (type) {
            case COMMANDER:
                return 'C';
            case FIELD_MEDIC:
                return 'F';
            case SOLDIER:
                return 'S';
            case SNIPER:
                return 'R';
            case SCOUT:
                return 'T';
        }
        throw new RuntimeException();
    }

    public static boolean isLetter(char ch) {
        return ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z';
    }

    public static TrooperType getTrooperTypeByChar(char c) {
        for (TrooperType type : TrooperType.values()) {
            if (getCharForTrooperType(type) == c) {
                return type;
            }
        }
        throw new RuntimeException();
    }

    public static int manhattanDist(int x, int y, int x1, int y1) {
        return Math.abs(x - x1) + Math.abs(y - y1);
    }

    public static int[][] bfsByMap(char[][] map, int startX, int startY) { //больше бфсов, хороших и одинаковых
        int[][] dist;
        Queue<Integer> qx = new ArrayDeque<>();
        Queue<Integer> qy = new ArrayDeque<>();
        dist = new int[map.length][map[0].length];
        for(int[] aDist : dist) {
            Arrays.fill(aDist, UNREACHABLE);
        }
        qx.add(startX);
        qy.add(startY);
        dist[startX][startY] = 0;
        while (!qx.isEmpty()) {
            int x = qx.poll();
            int y = qy.poll();
            for (Direction dir : dirs) {
                int toX = x + dir.getOffsetX();
                int toY = y + dir.getOffsetY();
                if (toX < 0 || toX >= map.length || toY < 0 || toY >= map[0].length) {
                    continue;
                }
                if (dist[toX][toY] != UNREACHABLE) {
                    continue;
                }
                dist[toX][toY] = dist[x][y] + 1;
                if (map[toX][toY] != '.'){
                    continue;
                }
                qx.add(toX);
                qy.add(toY);
            }
        }
        return dist;
    }
}
