import model.BonusType;
import model.TrooperStance;
import model.TrooperType;

import static model.TrooperStance.*;

import java.util.ArrayList;
import java.util.List;

public class AttackPlanComputer extends PlanComputer<AttackState> {
    private int[][] sqrDistSum;

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
            Utils utils,
            int selfHp
    ) {
        super(map, utils, hp, bonuses, stances, visibilities);

        cur = new AttackState(new ArrayList<MyMove>(), actionPoints, holdingFieldRation, holdingGrenade, 0, 0, stance, x, y, 0, selfHp, 0, false);

        prepare();

        rec();
    }

    private void prepare() {
        selfType = Utils.getTrooperTypeByChar(map[cur.x][cur.y]);
        map[cur.x][cur.y] = '.';
        sqrDistSum = new int[map.length][map[0].length];
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                char ch = map[i][j];
                if (Utils.isSmallLetter(ch)) {
                    enemyPositions.add(new Cell(i, j));
                } else {
                    hp[i][j] = Integer.MAX_VALUE;
                }
                if (Utils.isCapitalLetter(ch)) {
                    updateSqrDistSum(i, j);
                }
            }
        }
    }

    private void updateSqrDistSum(int x, int y) {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                sqrDistSum[i][j] += Utils.sqrDist(x, y, i, j);
            }
        }
    }

    @Override
    protected void updateBest() {
        cur.focusFireParameter = getFocusFireParameter();
        if (cur.better(best)) {
            best = new AttackState(cur);
        }
    }

    private int getFocusFireParameter() {
        int r = 0;
        for (Cell enemy : enemyPositions) {
            if (hp[enemy.x][enemy.y] <= 0) {
                continue;
            }
            r += (10000 - sqrDistSum[enemy.x][enemy.y]) * (Utils.INITIAL_TROOPER_HP * 2 - hp[enemy.x][enemy.y]);
        }
        return r;
    }
}
