import static model.ActionType.*;
import static model.Direction.*;

import model.ActionType;
import model.Direction;
import model.Move;

public class MyMove {

    private static int MAX_COORDINATE = 30;
    private static MyMove[][][][] memo = new MyMove[Utils.NUMBER_OF_ACTION_TYPES][Utils.NUMBER_OF_DIRECTION_TYPES + 1][MAX_COORDINATE + 1][MAX_COORDINATE + 1];

    public static MyMove HEAL_SELF = MyMove.of(HEAL, CURRENT_POINT);

    public static MyMove HEAL_NORTH = MyMove.of(HEAL, NORTH);
    public static MyMove HEAL_EAST = MyMove.of(HEAL, EAST);
    public static MyMove HEAL_SOUTH = MyMove.of(HEAL, SOUTH);
    public static MyMove HEAL_WEST = MyMove.of(HEAL, WEST);

    public static MyMove USE_MEDIKIT_NORTH = MyMove.of(USE_MEDIKIT, NORTH);
    public static MyMove USE_MEDIKIT_EAST = MyMove.of(USE_MEDIKIT, EAST);
    public static MyMove USE_MEDIKIT_SOUTH = MyMove.of(USE_MEDIKIT, SOUTH);
    public static MyMove USE_MEDIKIT_WEST = MyMove.of(USE_MEDIKIT, WEST);

    public static MyMove MOVE_NORTH = MyMove.of(MOVE, NORTH);
    public static MyMove MOVE_EAST = MyMove.of(MOVE, EAST);
    public static MyMove MOVE_SOUTH = MyMove.of(MOVE, SOUTH);
    public static MyMove MOVE_WEST = MyMove.of(MOVE, WEST);

    public static MyMove EAT_FIELD_RATION = MyMove.of(ActionType.EAT_FIELD_RATION);

    public static MyMove USE_MEDIKIT_SELF = MyMove.of(USE_MEDIKIT, CURRENT_POINT);

    public static MyMove RAISE_STANCE = MyMove.of(ActionType.RAISE_STANCE);
    public static MyMove LOWER_STANCE = MyMove.of(ActionType.LOWER_STANCE);

    //changing order of elements in this arrays will break many tests
    static final MyMove[] directedHeals = {HEAL_NORTH, HEAL_EAST, HEAL_SOUTH, HEAL_WEST};
    static final MyMove[] directedMedikitUses = {USE_MEDIKIT_NORTH, USE_MEDIKIT_EAST, USE_MEDIKIT_SOUTH, USE_MEDIKIT_WEST};
    static final MyMove[] movements = {MOVE_NORTH, MOVE_EAST, MOVE_SOUTH, MOVE_WEST};


    Move move;

    private MyMove(Move move) {
        this.move = move;
    }

    private static MyMove of(ActionType action, Direction dir, int x, int y) {
        int dirIndex = (dir == null ? Utils.NUMBER_OF_DIRECTION_TYPES : dir.ordinal());
        if (x == -1) {
            x = MAX_COORDINATE;
        }
        if (y == -1) {
            y = MAX_COORDINATE;
        }
        MyMove r = memo[action.ordinal()][dirIndex][x][y];
        if (r == null) {
            r = new MyMove(Utils.createMove(action, dir, x, y));
            memo[action.ordinal()][dirIndex][x][y] = r;
        }
        return r;
    }

    public static MyMove of(ActionType action, int x, int y) {
        return MyMove.of(action, null, x, y);
    }

    private static MyMove of(ActionType action, Direction dir) {
        return MyMove.of(action, dir, -1, -1);
    }

    private static MyMove of(ActionType action) {
        return MyMove.of(action, null, -1, -1);
    }


    int getDx() {
        return move.getDirection().getOffsetX();
    }

    int getDy() {
        return move.getDirection().getOffsetY();
    }

    public Move getMove() {
        return move;
    }

    @Override
    public String toString() {
        if (move.getAction() == THROW_GRENADE) {
            return "grenade(" + move.getX() + ", " + move.getY() + ")";
        }
        if (move.getAction() == SHOOT) {
            return "shoot(" + move.getX() + ", " + move.getY() + ")";
        }
        if (move.getAction() == HEAL || move.getAction() == USE_MEDIKIT) {
            if (move.getDirection() == CURRENT_POINT) {
                return move.getAction() + "_SELF";
            } else {
                return move.getAction() + "_" + move.getDirection();
            }
        }
        if (move.getAction() == MOVE) {
            return "MOVE_" + move.getDirection();
        }
        if (move.getAction() == HEAL) {
            return "HEAL_" + move.getDirection();
        }
        return move.getAction().toString();
    }

    public static MyMove shoot(int x, int y) {
        return MyMove.of(SHOOT, x, y);
    }

    public static MyMove grenade(int x, int y) {
        return MyMove.of(THROW_GRENADE, x, y);
    }
}