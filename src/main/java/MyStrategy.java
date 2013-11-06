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
    boolean[][] occupiedByTrooper;

    ArrayList<Trooper> teammates;
    Trooper teammateToFollow;

    @Override
    public void move(Trooper self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;

        init();

        if (tryMedicHeal()) {
            return;
        }

        if (tryHealSelf()) {
            return;
        }

        if(tryThrowGrenade()) {
            return;
        }

        if (tryShoot()) {
            return;
        }

        if (tryMove()) {
            return;
        }

        move.setAction(ActionType.END_TURN);
    }

    private boolean tryThrowGrenade() { //todo не надо бросать гранату в тех кто рядом с нами, и в тех, кого и так можно застрелить
        if(!haveTime(game.getGrenadeThrowCost())) {
            return false;
        }
        if(!self.isHoldingGrenade()) {
            return false;
        }
        for (Trooper trooper : world.getTroopers()) {
            if (!trooper.isTeammate()) {
                if (canThrowGrenade(trooper)) {
                    throwGrenade(trooper);
                    return true;
                }
            }
        }
        return false;
    }

    private void throwGrenade(Trooper trooper) {
        move.setAction(ActionType.THROW_GRENADE);
        setDirection(trooper);
    }

    private boolean haveTime(int actionCost) {
        return self.getActionPoints() >= actionCost;
    }

    private void init() {
        cells = world.getCells();
        teammates = getTeammates();
        teammateToFollow = getTeammateToFollow();
        occupiedByTrooper = getOccupiedByTrooper();
    }

    private boolean tryMedicHeal() {
        if(self.getType() != TrooperType.FIELD_MEDIC) {
            return false;
        }
        if(!haveTime(game.getFieldMedicHealCost())) {
            return false;
        }
        Trooper target = null;
        int maxDiff = 0;
        for (Trooper trooper : teammates) {
            int diff = trooper.getMaximalHitpoints() - trooper.getHitpoints();
            if(diff > maxDiff) {
                maxDiff = diff;
                target = trooper;
            }
        }
        if(target == null) {
            return false;
        }
        if(manhattanDist(self, target) <= 1) {
            move.setAction(ActionType.HEAL);
            move.setX(target.getX());
            move.setY(target.getY());
        } else {
            moveTo(target);
        }
        return true;
    }

    private int manhattanDist(Trooper self, Trooper target) {
        return manhattanDist(self.getX(), self.getY(), target.getX(), target.getY());
    }

    private boolean tryHealSelf() {
        if(game.getMedikitHealSelfBonusHitpoints() > self.getActionPoints()) {
            return false;
        }
        if(!self.isHoldingMedikit()) {
            return false;
        }
        move.setAction(ActionType.USE_MEDIKIT);
        return true;
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
        if (!haveTime(getMoveCost(self))) {
            return false;
        }

        if (self.getId() == teammateToFollow.getId()) {
            moveRandom();
            //moveTo(world.getWidth()/2, world.getHeight()/2);
        } else {
            moveTo(teammateToFollow);
        }
        return true;
    }

    private void moveTo(Trooper target) {
         moveTo(target.getX(), target.getY());
    }

    private void moveTo(int x, int y) {
        for (Direction dir : dirs) {
            if (isMoveTo(dir, x, y)) {
                move.setAction(ActionType.MOVE);
                move.setDirection(dir);
                return;
            }
        }
    }

    private int getMoveCost(Trooper self) {
        if(self.getStance() == TrooperStance.KNEELING) {
            return game.getKneelingMoveCost();
        }
        if(self.getStance() == TrooperStance.PRONE) {
            return game.getProneMoveCost();
        }
        if(self.getStance() == TrooperStance.STANDING) {
            return game.getStandingMoveCost();
        }
        throw new IllegalArgumentException();
    }



    private boolean isMoveTo(Direction dir, int x, int y) {
        if (!isValidMove(dir)) {
            return false;
        }
        return manhattanDist(x, y, self.getX() + dir.getOffsetX(), self.getY() + dir.getOffsetY()) <
               manhattanDist(x, y, self.getX(), self.getY());
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

        return inField(toX, toY) && cells[toX][toY] == CellType.FREE && !occupiedByTrooper[toX][toY];
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
            move.setAction(ActionType.MOVE);
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
        return -7; // >_<
    }

    boolean tryShoot() {
        if (!haveTime(self.getShootCost())) {
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

    public ArrayList<Trooper> getTeammates() {
        ArrayList<Trooper> r = new ArrayList<>();
        for (Trooper trooper : world.getTroopers()) {
            if(trooper.isTeammate()) {
                r.add(trooper);
            }
        }
        return r;
    }

    public boolean[][] getOccupiedByTrooper() {
        boolean[][] r = new boolean[world.getWidth()][world.getHeight()];
        for (int i = 0; i < world.getWidth(); i++) {
            r[i] = new boolean[world.getHeight()];
        }
        for(Trooper trooper : world.getTroopers()) {
            r[trooper.getX()][trooper.getY()] = true;
        }
        return r;
    }

    boolean canThrowGrenade(Trooper trooper) {
        return canThrowGrenade(trooper.getX(), trooper.getY());
    }

    private boolean canThrowGrenade(int x, int y) {
        return sqrDist(self.getX(), self.getY(), x, y) <= sqr(game.getGrenadeThrowRange());
    }

    private int sqrDist(int x, int y, int x1, int y1) {
        return sqr(x-x1) + sqr(y-y1);
    }

    private int sqr(int x) {
        return x*x;
    }

    private double sqr(double x) {
        return x*x;
    }

    public void setDirection(Trooper direction) {
        setDirection(direction.getX(), direction.getY());
    }

    private void setDirection(int x, int y) {
        move.setX(x);
        move.setY(y);
    }
}

/*
grenade reachability pattern
######
#####
#####
#####
####
#

карта 20 на 30

*/