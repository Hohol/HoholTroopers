import model.TrooperStance;
import model.TrooperType;

import java.util.List;


public abstract class TacticPlanComputerTest extends AbstractPlanComputerTest {

    private boolean enemyKnowsWhereWeAre;
    private Cell3D startCell;

    @Override
    protected void setMap(String... smap) {
        super.setMap(smap);
        enemyKnowsWhereWeAre = true;
        startCell = null;
    }

    protected void enemyDoesntKnowWhereWeAre() {
        enemyKnowsWhereWeAre = false;
    }

    protected void setStartCell(int x, int y, TrooperStance stance) {
        startCell = new Cell3D(x, y, stance.ordinal());
    }

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
        if (startCell == null) {
            startCell = new Cell3D(self.getX(), self.getY(), self.getStance().ordinal());
        }
        final boolean mapIsStatic = false;
        return new TacticPlanComputer(
                map,
                utils,
                bonuses,
                vision,
                false,
                false,
                troopers,
                teammates,
                enemies,
                moveOrder,
                enemyKnowsWhereWeAre,
                self,
                prevActions,
                startCell,
                mapIsStatic
        ).getPlan();
    }
}