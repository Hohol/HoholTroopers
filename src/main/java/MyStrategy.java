import model.*;

import java.util.Random;

public final class MyStrategy implements Strategy {
    private final Random rnd = new Random();
    Trooper self;
    World world;
    Game game;
    Move move;




    @Override
    public void move(Trooper self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;

        if (tryShoot()) {
            return;
        }

        if(tryMove()) {
            return;
        }
    }

    private boolean tryMove() {
        if (self.getActionPoints() < game.getStandingMoveCost()) {
            return false;
        }

        move.setAction(ActionType.MOVE);

        if (rnd.nextBoolean()) {
            move.setDirection(rnd.nextBoolean() ? Direction.NORTH : Direction.SOUTH);
        } else {
            move.setDirection(rnd.nextBoolean() ? Direction.WEST : Direction.EAST);
        }
        return true;
    }

    boolean tryShoot() {
        if(self.getActionPoints() < self.getShotCost()) {
            return false;
        }
        for (Trooper trooper : world.getTroopers()) {
            if(!trooper.isTeammate()) {
                if(canShoot(trooper)) {
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
