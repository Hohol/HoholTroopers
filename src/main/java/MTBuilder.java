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
}
