import model.*;

/**
 * Класс, определяющий бойца. Содержит также все свойства юнита.
 */
@SuppressWarnings("unused")
public final class MutableTrooper {
    private final long id;
    private int x;
    private int y;

    private final long playerId;
    private final int teammateIndex;
    private final boolean teammate;

    private final TrooperType type;
    private TrooperStance stance;

    private int hitpoints;
    private final int maximalHitpoints;

    private final int actionPoints;
    private final int initialActionPoints;

    private final int visionRange;
    private final double shootingRange;

    private final int shootCost;
    private final int standingDamage;
    private final int kneelingDamage;
    private final int proneDamage;
    private final int damage;

    private final boolean holdingGrenade;
    private final boolean holdingMedikit;
    private final boolean holdingFieldRation;
    private int lastSeenTime; //todo it does not belong here

    public MutableTrooper(Trooper t, int lastSeenTime) {
        this.id = t.getId();
        this.x = t.getX();
        this.y = t.getY();
        this.playerId = t.getPlayerId();
        this.teammateIndex = t.getTeammateIndex();
        this.teammate = t.isTeammate();
        this.type = t.getType();
        this.stance = t.getStance();
        this.hitpoints = t.getHitpoints();
        this.maximalHitpoints = t.getMaximalHitpoints();
        this.actionPoints = t.getActionPoints();
        this.initialActionPoints = t.getInitialActionPoints();
        this.visionRange = (int)(t.getVisionRange() + 0.5);
        this.shootingRange = t.getShootingRange();
        this.shootCost = t.getShootCost();
        this.standingDamage = t.getStandingDamage();
        this.kneelingDamage = t.getKneelingDamage();
        this.proneDamage = t.getProneDamage();
        this.damage = t.getDamage();
        this.holdingGrenade = t.isHoldingGrenade();
        this.holdingMedikit = t.isHoldingMedikit();
        this.holdingFieldRation = t.isHoldingFieldRation();
        this.lastSeenTime = lastSeenTime;
    }

    public MutableTrooper(long id, int x, int y, long playerId, int teammateIndex, boolean teammate, TrooperType type, TrooperStance stance, int hitpoints, int maximalHitpoints, int actionPoints, int initialActionPoints, int visionRange, double shootingRange, int shootCost, int standingDamage, int kneelingDamage, int proneDamage, int damage, boolean holdingGrenade, boolean holdingMedikit, boolean holdingFieldRation) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.playerId = playerId;
        this.teammateIndex = teammateIndex;
        this.teammate = teammate;
        this.type = type;
        this.stance = stance;
        this.hitpoints = hitpoints;
        this.maximalHitpoints = maximalHitpoints;
        this.actionPoints = actionPoints;
        this.initialActionPoints = initialActionPoints;
        this.visionRange = visionRange;
        this.shootingRange = shootingRange;
        this.shootCost = shootCost;
        this.standingDamage = standingDamage;
        this.kneelingDamage = kneelingDamage;
        this.proneDamage = proneDamage;
        this.damage = damage;
        this.holdingGrenade = holdingGrenade;
        this.holdingMedikit = holdingMedikit;
        this.holdingFieldRation = holdingFieldRation;
    }

    /**
     * @return Возвращает идентификатор игрока, в команду которого входит боец.
     */
    public long getPlayerId() {
        return playerId;
    }

    /**
     * @return Возвращает 0-индексированный номер бойца в команде.
     */
    public int getTeammateIndex() {
        return teammateIndex;
    }

    /**
     * @return Возвращает {@code true}, если и только если данный боец является дружественным.
     */
    public boolean isTeammate() {
        return teammate;
    }

    /**
     * @return Возвращает тип бойца.
     */
    public TrooperType getType() {
        return type;
    }

    /**
     * @return Возвращает стойку бойца.
     */
    public TrooperStance getStance() {
        return stance;
    }

    /**
     * @return Возвращает текущее количество очков здоровья у бойца.
     */
    public int getHitpoints() {
        return hitpoints;
    }

    /**
     * @return Возвращает максимальное количество очков здоровья у бойца.
     */
    public int getMaximalHitpoints() {
        return maximalHitpoints;
    }

    /**
     * @return Возвращает текущее количество очков действия у бойца.
     */
    public int getActionPoints() {
        return actionPoints;
    }

    /**
     * @return Возвращает количество очков действия, которое даётся бойцу в начале хода.
     */
    public int getInitialActionPoints() {
        return initialActionPoints;
    }

    /**
     * @return Возвращает дальность обзора бойца.
     */
    public int getVisionRange() {
        return visionRange;
    }

    /**
     * @return Возвращает дальность стрельбы бойца.
     */
    public double getShootingRange() {
        return shootingRange;
    }

    /**
     * @return Возвращает количество очков действия, необходимое бойцу для совершения выстрела.
     */
    public int getShootCost() {
        return shootCost;
    }

    /**
     * @return Возвращает урон одного выстрела бойца, находящегося в положении стоя.
     */
    public int getStandingDamage() {
        return standingDamage;
    }

    /**
     * @return Возвращает урон одного выстрела бойца, находящегося в положении сидя.
     */
    public int getKneelingDamage() {
        return kneelingDamage;
    }

    /**
     * @return Возвращает урон одного выстрела бойца, находящегося в положении лёжа.
     */
    public int getProneDamage() {
        return proneDamage;
    }

    /**
     * @param stance Стойка бойца для подсчёта урона.
     * @return Возвращает урон одного выстрела бойца в указанной стойке.
     */
    public int getDamage(TrooperStance stance) {
        switch (stance) {
            case PRONE:
                return proneDamage;
            case KNEELING:
                return kneelingDamage;
            case STANDING:
                return standingDamage;
            default:
                throw new IllegalArgumentException("Unsupported stance: " + stance + '.');
        }
    }

    /**
     * @return Возвращает урон одного выстрела бойца в данной стойке.
     */
    public int getDamage() {
        return damage;
    }

    /**
     * @return Возвращает {@code true}, если и только если данный боец несёт с собой гранату.
     */
    public boolean isHoldingGrenade() {
        return holdingGrenade;
    }

    /**
     * @return Возвращает {@code true}, если и только если данный боец несёт с собой аптечку.
     */
    public boolean isHoldingMedikit() {
        return holdingMedikit;
    }

    /**
     * @return Возвращает {@code true}, если и только если данный боец несёт с собой сухой паёк.
     */
    public boolean isHoldingFieldRation() {
        return holdingFieldRation;
    }

    public void decHp(int d) {
        hitpoints -= d;
    }

    @Override
    public boolean equals(Object o) {
        MutableTrooper t = (MutableTrooper) o;
        return getId() == t.getId();
    }

    @Override
    public int hashCode() {
        return Long.valueOf(getId()).hashCode();
    }

    public int getLastSeenTime() {
        return lastSeenTime;
    }

    public void updateLastSeenTime(int moveIndex) {
        lastSeenTime = moveIndex;
    }

    public void setHp(int hp) {
        hitpoints = hp;
    }

    public void setStance(TrooperStance stance) {
        this.stance = stance;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public final long getId() {
        return id;
    }

    /**
     * @return Возвращает X-координату центра объекта. Ось абсцисс направлена слева направо.
     */
    public int getX() {
        return x;
    }

    /**
     * @return Возвращает Y-координату центра объекта. Ось ординат направлена свеху вниз.
     */
    public int getY() {
        return y;
    }

    public void setHitpoints(int hp) {
        hitpoints = hp;
    }
}