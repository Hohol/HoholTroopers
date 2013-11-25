import model.TrooperStance;
import model.TrooperType;

public class StrategyState extends AbstractState<StrategyState> {

    int distToDestination;
    public int maxDistToTeammate;

    public StrategyState(MutableTrooper self) {
        super(self);
    }

    public StrategyState(StrategyState cur) {
        super(cur);
        distToDestination = cur.distToDestination;
        maxDistToTeammate = cur.maxDistToTeammate;
    }

    @Override
    boolean better(StrategyState old, TrooperType selfType) {
        if (old == null) {
            return true;
        }


        //--
        boolean tooFar = maxDistToTeammate > StrategyPlanComputer.MAX_DIST_TO_TEAMMATE;
        boolean oldTooFar = old.maxDistToTeammate > StrategyPlanComputer.MAX_DIST_TO_TEAMMATE;
        if (tooFar != oldTooFar) {
            return !tooFar;
        }
        if (tooFar) {
            return maxDistToTeammate < old.maxDistToTeammate;
        }
        //--


        if (distToDestination != old.distToDestination) {
            return distToDestination < old.distToDestination;
        }

        if (holdingMedikit != old.holdingMedikit) {
            return holdingMedikit;
        }
        if (holdingFieldRation != old.holdingFieldRation) {
            return holdingFieldRation;
        }

        if (holdingGrenade != old.holdingGrenade) {
            return holdingGrenade;
        }

        if (fieldRationsUsed != old.fieldRationsUsed) {
            return fieldRationsUsed < old.fieldRationsUsed;
        }

        if (actionPoints != old.actionPoints) {
            return actionPoints > old.actionPoints;
        }

        return false;
    }
}
