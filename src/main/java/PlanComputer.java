import model.Game;
import model.TrooperType;

import java.util.List;

import static model.TrooperType.FIELD_MEDIC;

public abstract class PlanComputer <S extends State> {
    protected final char[][] map;
    protected final Game game;
    protected final Utils utils;
    protected S cur, best;
    protected TrooperType selfType;

    public PlanComputer(char[][] map, Utils utils) {
        this.map = map;
        this.utils = utils;
        this.game = utils.getGame();
    }

    abstract protected void rec();

    protected void addAction(MyMove action) {
        cur.actions.add(action);
    }

    protected void popAction() {
        cur.actions.remove(cur.actions.size() - 1);
    }

    protected boolean inField(int toX, int toY) {
        return toX >= 0 && toX < map.length && toY >= 0 && toY < map[0].length;
    }

    protected void tryMove() {
        int moveCost = utils.getMoveCost(cur.stance);
        if (cur.actionPoints < moveCost) {
            return;
        }
        for (MyMove movement : MyMove.movements) {
            int toX = cur.x + movement.getDx();
            int toY = cur.y + movement.getDy();
            if (!inField(toX, toY)) {
                continue;
            }
            if (!freeCell(toX, toY)) {
                continue;
            }
            addAction(movement);

            cur.actionPoints -= moveCost;
            cur.x += movement.getDx();
            cur.y += movement.getDy();
            rec();
            cur.x -= movement.getDx();
            cur.y -= movement.getDy();
            cur.actionPoints += moveCost;
            popAction();
        }
    }

    protected abstract boolean freeCell(int toX, int toY);

    protected void tryEatFieldRation() {
        if (cur.holdingFieldRation && cur.actionPoints >= game.getFieldRationEatCost() && cur.actionPoints < utils.getInitialActionPoints(selfType)) {
            addAction(MyMove.EAT_FIELD_RATION);
            int oldActionPoints = cur.actionPoints;
            cur.actionPoints = utils.actionPointsAfterEatingFieldRation(selfType, cur.actionPoints, game);
            cur.holdingFieldRation = false;
            rec();
            cur.holdingFieldRation = true;
            cur.actionPoints = oldActionPoints;
            popAction();
        }
    }
}
