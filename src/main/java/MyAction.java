import static model.ActionType.*;
import static model.Direction.*;

import model.ActionType;
import model.Direction;
import model.Move;

public enum MyAction {
    HEAL_SELF(HEAL, CURRENT_POINT),

    HEAL_NORTH(HEAL, NORTH),
    HEAL_EAST(HEAL, EAST),
    HEAL_SOUTH(HEAL, SOUTH),
    HEAL_WEST(HEAL, WEST),

    USE_MEDIKIT_NORTH(USE_MEDIKIT, NORTH),
    USE_MEDIKIT_EAST(USE_MEDIKIT, EAST),
    USE_MEDIKIT_SOUTH(USE_MEDIKIT, SOUTH),
    USE_MEDIKIT_WEST(USE_MEDIKIT, WEST),

    MOVE_NORTH(MOVE, NORTH),
    MOVE_EAST(MOVE, EAST),
    MOVE_SOUTH(MOVE, SOUTH),
    MOVE_WEST(MOVE, WEST),

    EAT_FIELD_RATION(ActionType.EAT_FIELD_RATION),
    USE_MEDIKIT_SELF(USE_MEDIKIT, CURRENT_POINT);

    static final MyAction[] directedHeals = {HEAL_NORTH, HEAL_EAST, HEAL_SOUTH, HEAL_WEST};
    static final MyAction[] directedMedikitUses = {USE_MEDIKIT_NORTH, USE_MEDIKIT_EAST, USE_MEDIKIT_SOUTH, USE_MEDIKIT_WEST};
    static final MyAction[] movements = {MOVE_NORTH, MOVE_EAST, MOVE_SOUTH, MOVE_WEST};

    Move move;

    MyAction(Move move) {
        this.move = move;
    }

    MyAction(ActionType action) {
        this(Utils.createMove(action));
    }

    MyAction(ActionType action, Direction dir) {
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