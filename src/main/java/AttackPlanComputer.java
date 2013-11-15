import model.BonusType;
import model.TrooperStance;

public class AttackPlanComputer extends PlanComputer {

    public AttackPlanComputer(
            int actionPoints,
            int x,
            int y,
            char[][] map,
            int[][] hp,
            boolean holdingFieldRation,
            boolean holdingGrenade,
            TrooperStance stance,
            boolean[] visibilities,
            TrooperStance[][] stances,
            BonusType[][] bonuses,
            Utils utils
    ) {
        super(map, utils, hp, bonuses, stances, visibilities,
                new State(actionPoints, holdingFieldRation, x, y, stance, Utils.INITIAL_TROOPER_HP, false, holdingGrenade)


                );
        rec();
    }
}
