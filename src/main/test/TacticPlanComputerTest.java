import model.TrooperType;

import java.util.List;


public abstract class TacticPlanComputerTest extends AbstractPlanComputerTest {

    protected MTBuilder enemy(TrooperType type) {
        int x = -1, y = -1;
        int cnt = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (Utils.isEnemyChar(map[i][j]) && Character.toUpperCase(map[i][j]) == Utils.getCharForTrooperType(type)) {
                    x = i;
                    y = j;
                    cnt++;
                }
            }
        }
        if (x == -1) {
            throw new RuntimeException("No enemy " + type + " on the map");
        }
        if (cnt > 1) {
            throw new RuntimeException("Multiple enemy " + type + " on the map");
        }

        return builders[x][y];
    }

    protected MTBuilder enemy(int x, int y) {
        if (!Utils.isEnemyChar(map[x][y])) {
            throw new RuntimeException("No enemy in cell (" + x + ", " + y + ")");
        }
        return builders[x][y];
    }

    @Override
    protected List<MyMove> getActual(String moveOrder, MutableTrooper self) {
        return new TacticPlanComputer(
                    map,
                    utils,
                    bonuses,
                    getVisibilities(),
                    false,
                    false, troopers, teammates, enemies,
                    moveOrder,
                    self
            ).getPlan();
    }

}