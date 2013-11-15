import model.BonusType;
import model.TrooperStance;

import static model.TrooperType.*;

import java.util.ArrayList;
import java.util.Arrays;

class HealingPlanComputer extends PlanComputer {

    HealingPlanComputer(
            int actionPoints,
            char[][] map,
            boolean holdingFieldRation,
            boolean holdingMedikit,
            Utils utils,
            int selfHp,
            int x, int y,
            int[][] hp
    ) {
        super(map, utils, hp, null, null, null, new State(actionPoints, holdingFieldRation, x, y, TrooperStance.STANDING, selfHp, holdingMedikit, false));
        selfType = FIELD_MEDIC;

        bonuses = new BonusType[map.length][map[0].length];
        prepare();
        rec();
    }

    private void prepare() {
        dist = new ArrayList<>();
        for (Cell cell : allyPositions) {
            int[][] curDist = Utils.bfsByMap(map, cell.x, cell.y);
            dist.add(curDist);
            for (int i = 0; i < map.length; i++) {
                for (int j = 0; j < map[i].length; j++) {
                    if (curDist[i][j] > MyStrategy.MAX_DISTANCE_MEDIC_SHOULD_HEAL) {
                        curDist[i][j] = Utils.UNREACHABLE;
                    }
                }
            }
        }
    }

    @Override
    protected void updateBest() {
        cur.distSum = getDistToTeammatesSum();

        cur.minHp = getMinHp();
        if (cur.better(best)) {
            best = new State(cur);
        }
    }

}
