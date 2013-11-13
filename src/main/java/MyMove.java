import static model.ActionType.*;
import static model.Direction.*;

import model.ActionType;
import model.Direction;
import model.Move;

public class MyMove {
    public static MyMove HEAL_SELF = new MyMove(HEAL, CURRENT_POINT);

    public static MyMove HEAL_NORTH = new MyMove(HEAL, NORTH);
    public static MyMove HEAL_EAST = new MyMove(HEAL, EAST);
    public static MyMove HEAL_SOUTH = new MyMove(HEAL, SOUTH);
    public static MyMove HEAL_WEST = new MyMove(HEAL, WEST);

    public static MyMove USE_MEDIKIT_NORTH = new MyMove(USE_MEDIKIT, NORTH);
    public static MyMove USE_MEDIKIT_EAST = new MyMove(USE_MEDIKIT, EAST);
    public static MyMove USE_MEDIKIT_SOUTH = new MyMove(USE_MEDIKIT, SOUTH);
    public static MyMove USE_MEDIKIT_WEST = new MyMove(USE_MEDIKIT, WEST);

    public static MyMove MOVE_NORTH = new MyMove(MOVE, NORTH);
    public static MyMove MOVE_EAST = new MyMove(MOVE, EAST);
    public static MyMove MOVE_SOUTH = new MyMove(MOVE, SOUTH);
    public static MyMove MOVE_WEST = new MyMove(MOVE, WEST);

    public static MyMove EAT_FIELD_RATION = new MyMove(ActionType.EAT_FIELD_RATION);
    public static MyMove USE_MEDIKIT_SELF = new MyMove(USE_MEDIKIT, CURRENT_POINT);

    static final MyMove[] directedHeals = {HEAL_NORTH, HEAL_EAST, HEAL_SOUTH, HEAL_WEST};
    static final MyMove[] directedMedikitUses = {USE_MEDIKIT_NORTH, USE_MEDIKIT_EAST, USE_MEDIKIT_SOUTH, USE_MEDIKIT_WEST};
    static final MyMove[] movements = {MOVE_NORTH, MOVE_EAST, MOVE_SOUTH, MOVE_WEST};

    Move move;

    MyMove(Move move) {
        this.move = move;
    }

    MyMove(ActionType action) {
        this(Utils.createMove(action));
    }

    MyMove(ActionType action, Direction dir) {
        this(Utils.createMove(action, dir));
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
}