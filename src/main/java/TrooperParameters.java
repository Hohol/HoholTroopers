import model.Trooper;
import model.TrooperStance;
import model.TrooperType;

import java.util.List;

public interface TrooperParameters {
    int getShootDamage(TrooperType type, TrooperStance stance);
    int getShootCost(TrooperType type);

    class HardcodedTrooperParameters implements TrooperParameters {
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
    }

    class TrooperParametersImpl implements TrooperParameters {

        int[][] damage = new int[TrooperType.values().length][TrooperStance.values().length];
        int[] shootCost = new int[TrooperType.values().length];

        public TrooperParametersImpl(List<Trooper> troopers) {
            for(Trooper trooper : troopers) {
                for(TrooperStance stance : TrooperStance.values()) {
                    damage[trooper.getType().ordinal()][stance.ordinal()] = trooper.getDamage(stance);
                }
                shootCost[trooper.getType().ordinal()] = trooper.getShootCost();
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
    }
}
