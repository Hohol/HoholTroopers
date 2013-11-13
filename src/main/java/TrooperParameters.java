import model.Trooper;
import model.TrooperStance;
import model.TrooperType;

import java.util.List;

public interface TrooperParameters {
    int getShootDamage(TrooperType type, TrooperStance stance);
    int getShootCost(TrooperType type);

    int getInitialActionPoints(TrooperType type);

    TrooperParameters HARDCODED_TROOPER_PARAMETERS = new TrooperParameters() {
        @Override
        public int getShootCost(TrooperType type) {
            switch (type) {
                case COMMANDER:
                    return 3;
                case FIELD_MEDIC:
                    return 2;
                case SOLDIER:
                    return 4;
                case SNIPER:
                    return 9;
                case SCOUT:
                    return 4;
            }
            throw new RuntimeException();
        }

        @Override
        public int getInitialActionPoints(TrooperType type) {
            if(type == TrooperType.SCOUT) {
                return 12;
            } else {
                return 10;
            }
        }

        @Override
        public int getShootRange(TrooperType type) {
            switch (type) {
                case COMMANDER:
                    return 7;
                case FIELD_MEDIC:
                    return 5;
                case SOLDIER:
                    return 8;
                case SNIPER:
                    return 10;
                case SCOUT:
                    return 6;
            }
            throw new RuntimeException();
        }

        @Override
        public int getShootDamage(TrooperType type, TrooperStance stance) {
            return standingDamage(type) + bonusDamage(type) * (3 - stance.ordinal() - 1);
        }

        private int bonusDamage(TrooperType type) {
            switch (type) {
                case COMMANDER:
                    return 5;
                case FIELD_MEDIC:
                    return 3;
                case SOLDIER:
                    return 5;
                case SNIPER:
                    return 15;
                case SCOUT:
                    return 5;
            }
            throw new RuntimeException();
        }

        private int standingDamage(TrooperType type) {
            switch (type) {
                case COMMANDER:
                    return 15;
                case FIELD_MEDIC:
                    return 9;
                case SOLDIER:
                    return 25;
                case SNIPER:
                    return 65;
                case SCOUT:
                    return 20;
            }
            throw new RuntimeException();
        }
    };

    int getShootRange(TrooperType type);

    class TrooperParametersImpl implements TrooperParameters {

        private int[][] damage = new int[TrooperType.values().length][TrooperStance.values().length];
        private int[] shootCost = new int[TrooperType.values().length];
        private int[] initialActionPoints = new int[TrooperType.values().length];
        private int[] range = new int[Utils.NUMBER_OF_TROOPER_TYPES];

        public TrooperParametersImpl(List<Trooper> troopers) {
            for(Trooper trooper : troopers) {
                int typeOrdinal = trooper.getType().ordinal();
                for(TrooperStance stance : TrooperStance.values()) {
                    damage[typeOrdinal][stance.ordinal()] = trooper.getDamage(stance);
                }
                shootCost[typeOrdinal] = trooper.getShootCost();
                initialActionPoints[typeOrdinal] = trooper.getInitialActionPoints();
                range[typeOrdinal] = (int)(trooper.getShootingRange() + 0.5);
            }
        }

        @Override
        public int getShootDamage(TrooperType type, TrooperStance stance) {
            return damage[type.ordinal()][stance.ordinal()];
        }

        @Override
        public int getShootCost(TrooperType type) {
            return shootCost[type.ordinal()];
        }

        @Override
        public int getInitialActionPoints(TrooperType type) {
            return initialActionPoints[type.ordinal()];
        }

        @Override
        public int getShootRange(TrooperType type) {
            return range[type.ordinal()];
        }
    }
}
