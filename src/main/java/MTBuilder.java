import model.TrooperStance;
import model.TrooperType;

public class MTBuilder {

    private long id;
    private int x;
    private int y;

    private long playerId;
    private int teammateIndex;
    private boolean teammate;

    private TrooperType type;
    private TrooperStance stance = TrooperStance.STANDING;

    private int hitpoints = Utils.INITIAL_TROOPER_HP;
    private int maximalHitpoints;

    private int actionPoints;
    private int initialActionPoints;

    private double visionRange;
    private double shootingRange;

    private int shootCost;
    private int standingDamage;
    private int kneelingDamage;
    private int proneDamage;
    private int damage;

    private boolean holdingGrenade;
    private boolean holdingMedikit;
    private boolean holdingFieldRation;
    private int lastSeenTime;
    
    public MutableTrooper build() {
        return new MutableTrooper(id, x, y, playerId, teammateIndex, teammate, type, stance, hitpoints, maximalHitpoints, actionPoints, initialActionPoints, visionRange, shootingRange, shootCost, standingDamage, kneelingDamage, proneDamage, damage, holdingGrenade, holdingMedikit, holdingFieldRation);
    }
    public MTBuilder hp(int hp) {
        hitpoints = hp;
        return this;
    }

    public MTBuilder grenade() {
        holdingGrenade = true;
        return this;
    }

    public MTBuilder x(int x) {
        this.x = x;
        return this;
    }

    public MTBuilder y(int y) {
        this.y = y;
        return this;
    }

    public MTBuilder teammate() {
        this.teammate = true;
        return this;
    }

    public MTBuilder stance(TrooperStance stance) {
        if(stance == null) { //todo remove
            stance = TrooperStance.STANDING;
        }
        this.stance = stance;
        return this;
    }

    public MTBuilder actionPoints(int actionPoints) {
        this.actionPoints = actionPoints;
        return this;
    }

    public MTBuilder fieldRation() {
        holdingFieldRation = true;
        return this;
    }

    public MTBuilder medikit() {
        holdingMedikit = true;
        return this;
    }

    public MTBuilder type(TrooperType type) {
        this.type = type;
        return this;
    }
}
