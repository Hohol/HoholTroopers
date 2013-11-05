import model.*;

import java.util.ArrayList;
import java.util.Random;

public final class MyStrategy implements Strategy {
    final Random rnd = new Random();
    final static Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    Trooper self;
    World world;
    Game game;
    Move move;
    CellType[][] cells;

    Trooper teammateToFollow;

    @Override
    public void move(Trooper self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;
        cells = world.getCells();

        teammateToFollow = getTeammateToFollow();

        if (tryShoot()) {
            return;
        }

        if (tryMove()) {
            return;
        }
    }

    private Trooper getTeammateToFollow() {
        Trooper r = null;
        for (Trooper trooper : world.getTroopers()) {
            if (trooper.isTeammate()) {
                if (r == null || followPriority(trooper) > followPriority(r)) {
                    r = trooper;
                }
            }
        }
        return r;
    }

    private boolean tryMove() {
        if (self.getActionPoints() < game.getStandingMoveCost()) {
            return false;
        }

        move.setAction(ActionType.MOVE);

        if (self.getId() == teammateToFollow.getId()) {
            moveRandom();
        } else {
            follow();
        }
        return true;
    }

    private void follow() {
        for (Direction dir : dirs) {
            if (goodMoveToFollow(dir)) {
                move.setDirection(dir);
                return;
            }
        }
    }

    private boolean goodMoveToFollow(Direction dir) {
        if (!isValidMove(dir)) {
            return false;
        }
        return manhattanDist(teammateToFollow, self.getX() + dir.getOffsetX(), self.getY() + dir.getOffsetY()) <
               manhattanDist(teammateToFollow, self.getX(), self.getY());
    }

    private int manhattanDist(Trooper trooper, int x, int y) {
        return manhattanDist(trooper.getX(), trooper.getY(), x, y);
    }

    private int manhattanDist(int x, int y, int x1, int y1) {
        return Math.abs(x-x1) + Math.abs(y-y1);
    }

    private boolean isValidMove(Direction dir) {
        int toX = self.getX() + dir.getOffsetX();
        int toY = self.getY() + dir.getOffsetY();

        return inField(toX, toY) && cells[toX][toY] == CellType.FREE;
    }

    private boolean inField(int toX, int toY) {
        return toX >= 0 && toY >= 0 && toX < world.getWidth() && toY < world.getHeight();
    }

    private void moveRandom() {
        ArrayList<Direction> a = new ArrayList<>();
        for(Direction dir : dirs) {
            if(isValidMove(dir)) {
                a.add(dir);
            }
        }
        if(!a.isEmpty()) {
            move.setDirection(a.get(rnd.nextInt(a.size())));
        }
    }

    int followPriority(Trooper trooper) {
        if (trooper.getType() == TrooperType.FIELD_MEDIC) {
            return 0;
        }
        if (trooper.getType() == TrooperType.COMMANDER) {
            return 1;
        }
        if (trooper.getType() == TrooperType.SOLDIER) {
            return 2;
        }
        throw new IllegalArgumentException();
    }

    boolean tryShoot() {
        if (self.getActionPoints() < self.getShotCost()) {
            return false;
        }
        for (Trooper trooper : world.getTroopers()) {
            if (!trooper.isTeammate()) {
                if (canShoot(trooper)) {
                    shoot(trooper);
                    return true;
                }
            }
        }
        return false;
    }

    private void shoot(Trooper trooper) {
        move.setAction(ActionType.SHOOT);
        move.setX(trooper.getX());
        move.setY(trooper.getY());
    }

    private boolean canShoot(Trooper trooper) {
        return world.isVisible(self.getShootingRange(), self.getX(), self.getY(), self.getStance(),
                trooper.getX(), trooper.getY(), trooper.getStance());
    }
}
