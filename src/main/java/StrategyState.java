import model.TrooperStance;

import static model.TrooperStance.*;

import model.TrooperType;

import static model.TrooperType.*;

public class StrategyState extends AbstractState<StrategyState> {

    int distToDestination;
    int maxDistToTeammate; //if self is leader, it is max of minDistToOtherEnemy to teammates. otherwise it is minDistToOtherEnemy to leader
    int distToLeader;
    int leadersDistToDestination;
    int dangerAreaFactor;

    public StrategyState(MutableTrooper self) {
        super(self);
    }

    public StrategyState(StrategyState cur) {
        super(cur);
        distToDestination = cur.distToDestination;
        maxDistToTeammate = cur.maxDistToTeammate;
        distToLeader = cur.distToLeader;
        leadersDistToDestination = cur.leadersDistToDestination;
        this.dangerAreaFactor = cur.dangerAreaFactor;
    }

    @Override
    boolean better(StrategyState old, TrooperType selfType) {
        if (old == null) {
            return true;
        }

        if (leadersDistToDestination != old.leadersDistToDestination) {
            return leadersDistToDestination < old.leadersDistToDestination;
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

        if (distToLeader != old.distToLeader) {
            return distToLeader < old.distToLeader;
        }

        int magic = getMagic();
        int oldMagic = old.getMagic();
        if (magic != oldMagic) {
            return magic < oldMagic;
        }

        if (selfType == SNIPER) {
            if (stance != old.stance) {
                return stance == KNEELING;
            }
        }

        if (newSeenCellsCnt != old.newSeenCellsCnt) {
            return newSeenCellsCnt > old.newSeenCellsCnt;
        }

        if (actionPoints != old.actionPoints) {
            return actionPoints > old.actionPoints;
        }

        return false;
    }

    int getMagic() {
        return distToDestination * 75 + dangerAreaFactor;
    }

    @Override
    public String toString() {
        return "StrategyState{" +
                "distToDestination=" + distToDestination +
                ", actions=" + actions +
                ", maxDistToTeammate=" + maxDistToTeammate +
                ", distToLeader=" + distToLeader +
                ", leadersDistToDestination=" + leadersDistToDestination +
                '}';
    }
}
