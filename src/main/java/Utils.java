import model.*;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import static model.TrooperStance.KNEELING;
import static model.TrooperStance.PRONE;
import static model.BonusType.*;

public class Utils {

    final static Game hardcodedGame = new Game(
            50, 100, 50, 25, 1.0, 2, 2, 4, 6, 2, 5.0, 10, 5, 1, 5, 3, 0.0, 0.5, 1.0, 0.0, 1.0, 2.0, 1.0, 8, 5.0, 80, 60, 2, 50, 30, 2, 5
    );
    public static final int INITIAL_TROOPER_HP = 100;
    final static Direction[] dirs = {Direction.WEST, Direction.SOUTH, Direction.EAST, Direction.NORTH};
    final static int UNREACHABLE = 666;
    public static final int NUMBER_OF_TROOPER_TYPES = TrooperType.values().length;
    public static final int NUMBER_OF_DIRECTION_TYPES = Direction.values().length;
    public static final int NUMBER_OF_ACTION_TYPES = ActionType.values().length;
    public static final Utils HARDCODED_UTILS = new Utils(Utils.hardcodedGame, TrooperParameters.HARDCODED_TROOPER_PARAMETERS);
    public static final int NUMBER_OF_STANCES = TrooperStance.values().length;

    private final Game game;
    private final TrooperParameters trooperParameters;

    public Utils(Game game, TrooperParameters trooperParameters) {
        this.game = game;
        this.trooperParameters = trooperParameters;
    }

    public static char[][] toCharAndTranspose(String[] map) {
        char[][] r = new char[map[0].length()][map.length];
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length(); j++) {
                r[j][i] = map[i].charAt(j);
            }
        }
        return r;
    }

    public static TrooperStance stanceAfterLowering(TrooperStance stance) {
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

    static void log(Object o) {
        if (!MyStrategy.local) {
            return;
        }
        System.out.println(o);
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
            if (getCharForTrooperType(type) == Character.toUpperCase(c)) {
                return type;
            }
        }
        throw new RuntimeException();
    }

    public static int manhattanDist(int x, int y, int x1, int y1) {
        return Math.abs(x - x1) + Math.abs(y - y1);
    }

    public static int[][] bfsByMap(char[][] map, int startX, int startY) { //больше бфсов, хороших и одинаковых
        return bfsByMap(map, startX, startY, null);
    }

    private static int[][] bfsByMap(char[][] map, int startX, int startY, int[][] isStartCell) {
        int[][] dist;
        Queue<Integer> qx = new ArrayDeque<>();
        Queue<Integer> qy = new ArrayDeque<>();
        dist = new int[map.length][map[0].length];
        for (int[] aDist : dist) {
            Arrays.fill(aDist, UNREACHABLE);
        }
        if (startX != -1) {
            qx.add(startX);
            qy.add(startY);
            dist[startX][startY] = 0;
        } else {
            for (int i = 0; i < map.length; i++) {
                for (int j = 0; j < map[i].length; j++) {
                    if (isStartCell[i][j] == 0) {
                        continue;
                    }
                    qx.add(i);
                    qy.add(j);
                    dist[i][j] = 0;
                }
            }
        }

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
                if (map[toX][toY] != '.' && map[toX][toY] != '?') {
                    continue;
                }
                qx.add(toX);
                qy.add(toY);
            }
        }
        return dist;
    }

    public static int[][] bfsByMapAndStartingCells(char[][] map, int[][] isStartCell) { //start from each cell where startMap != 0
        return bfsByMap(map, -1, -1, isStartCell);
    }

    public static Move createMove(ActionType action, Direction dir, int x, int y) {
        Move r = new Move();
        r.setAction(action);
        r.setDirection(dir);
        r.setX(x);
        r.setY(y);
        return r;
    }

    public static boolean isEnemyChar(char ch) {
        return ch >= 'a' && ch <= 'z';
    }

    public static boolean isTeammateChar(char ch) {
        return ch >= 'A' && ch <= 'Z';
    }

    public int getShootRange(TrooperType type, int stance) {
        int range = trooperParameters.getShootRange(type);
        if (type == TrooperType.SNIPER) {
            range += Utils.NUMBER_OF_STANCES - stance - 1; //>_<
        }
        return range;
    }

    public int getVisionRange(TrooperType type) {
        return trooperParameters.getVisionRange(type);
    }

    public int getMoveCost(TrooperStance stance) {
        switch (stance) {
            case PRONE:
                return game.getProneMoveCost();
            case KNEELING:
                return game.getKneelingMoveCost();
            case STANDING:
                return game.getStandingMoveCost();
        }
        throw new RuntimeException();
    }

    public int getMoveCost(int stance) {
        switch (stance) {
            case 0:
                return game.getProneMoveCost();
            case 1:
                return game.getKneelingMoveCost();
            case 2:
                return game.getStandingMoveCost();
        }
        throw new RuntimeException();
    }

    public static TrooperStance stanceAfterRaising(TrooperStance stance) {
        switch (stance) {
            case PRONE:
                return KNEELING;
            case KNEELING:
                return TrooperStance.STANDING;
            case STANDING:
                throw new RuntimeException();
        }
        throw new RuntimeException();
    }

    public static BonusType getBonusTypeByChar(char ch) {
        switch (ch) {
            case '^':
                return FIELD_RATION;
            case '+':
                return MEDIKIT;
            case '*':
                return GRENADE;
        }
        return null;
    }

    public static double dist(int x1, int y1, int x2, int y2) {
        return Math.sqrt(sqrDist(x1, y1, x2, y2));
    }
}
