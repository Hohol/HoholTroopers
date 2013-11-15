import model.BonusType;

import static model.TrooperType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class MaxHealingPlanComputer extends PlanComputer<HealingState> {

    List<int[][]> dist;

    MaxHealingPlanComputer(
            int actionPoints,
            char[][] map,
            int[] hp1d,
            boolean holdingFieldRation,
            boolean holdingMedikit,
            Utils utils,
            int selfHp
    ) {
        super(map, utils, null, null, null, null);
        selfType = FIELD_MEDIC;

        bonuses = new BonusType[map.length][map[0].length];
        Cell start = getStart();
        prepare(hp1d);
        cur = new HealingState(new ArrayList<MyMove>(), actionPoints, holdingFieldRation, holdingMedikit, start.x, start.y, selfHp, 0, 0, 0, false, 0);
        rec();
    }

    private Cell getStart() {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (map[i][j] == Utils.getCharForTrooperType(FIELD_MEDIC)) {
                    return new Cell(i, j);
                }
            }
        }
        return null;
    }

    private void prepare(int[] hp1d) {
        hp = new int[map.length][map[0].length];
        for (int[] aHp : hp) {
            Arrays.fill(aHp, Integer.MAX_VALUE);
        }
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (Utils.isLetter(map[i][j])) {
                    if (map[i][j] == Utils.getCharForTrooperType(FIELD_MEDIC)) {
                        map[i][j] = '.'; //erase medic
                    } else {
                        int index = Utils.getTrooperTypeByChar(map[i][j]).ordinal();
                        allyPositions.add(new Cell(i, j));
                        hp[i][j] = hp1d[index];
                    }
                }
            }
        }
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
            best = new HealingState(cur);
        }
    }

    private int getDistToTeammatesSum() {
        int r = 0;
        int minHp = Integer.MAX_VALUE;
        for (Cell cell : allyPositions) {
            minHp = Math.min(minHp, hp[cell.x][cell.y]);
        }
        for (int i = 0; i < allyPositions.size(); i++) {
            Cell cell = allyPositions.get(i);
            int d = dist.get(i)[cur.x][cur.y];
            if (hp[cell.x][cell.y] == minHp) {
                d *= 100;
            }
            r += d;
        }
        return r;
    }

    private int getMinHp() {
        int mi = cur.selfHp;
        for (Cell cell : allyPositions) {
            mi = Math.min(mi, hp[cell.x][cell.y]);
        }
        return mi;
    }
}
