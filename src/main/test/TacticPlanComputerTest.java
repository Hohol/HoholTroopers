import model.TrooperStance;
import model.TrooperType;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public abstract class TacticPlanComputerTest extends AbstractPlanComputerTest {

    protected Cell lastSeenEnemyPosition;
    protected int defaultDamageValue = 25;
    private boolean enemyKnowsWhereWeAre;
    private Cell3D startCell;
    private Set<Cell> enemyKnowsPosition;
    private TrooperType damagedTeammate;
    protected MutableTrooper investigationResult;
    private int damageDealtToTeammate;

    @Override
    protected void setMap(String... smap) {
        super.setMap(smap);
        enemyKnowsWhereWeAre = true;
        startCell = null;
        enemyKnowsPosition = new HashSet<>();
        mediumMoveIndex = 0;
        damagedTeammate = null;
        moveOrder = null;
        lastSeenEnemyPosition = null;
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

    protected void setMediumMoveIndex(int index) {
        mediumMoveIndex = index;
    }

    @Override
    protected List<MyMove> getActual(String moveOrder, MutableTrooper self) {
        if (startCell == null) {
            startCell = new Cell3D(self.getX(), self.getY(), self.getStance().ordinal());
        }
        final boolean mapIsStatic = false;
        TacticPlanComputer computer = new TacticPlanComputer(
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
                moveOrder.length(),
                enemyKnowsWhereWeAre,
                self,
                prevActions,
                startCell,
                killedEnemies,
                enemyKnowsPosition,
                mediumMoveIndex,
                damagedTeammate,
                damageDealtToTeammate,
                lastSeenEnemyPosition,
                mapIsStatic
        );
        List<MyMove> result = computer.getPlan();
        investigationResult = computer.getInvestigationResult();
        return result;
    }

    protected void enemyKnowsPositionOf(TrooperType type) {
        int x = -1, y = -1;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if(map[i][j] == Utils.getCharForTrooperType(type)) {
                    x = i;
                    y = j;
                }
            }
        }
        if(x == -1) {
            throw new RuntimeException();
        }
        enemyKnowsPosition.add(new Cell(x,y));
    }

    protected void teammateWasDamaged(TrooperType type, int damageValue) {
        damagedTeammate = type;
        damageDealtToTeammate = damageValue;
    }

    protected void setMoveOrder(String moveOrder) {
        this.moveOrder = moveOrder;
    }

    protected void setLastSeenEnemyPosition(int x, int y) {
        lastSeenEnemyPosition = new Cell(x, y);
    }
}