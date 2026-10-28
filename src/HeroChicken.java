import java.awt.Color;
import java.awt.Graphics2D;
import java.util.function.Supplier;

final class HeroChicken extends Chicken {
    private static final double MAX_POWER = 22.0;
    private static final double POWER_SCALE = 0.035;
    private static final int SPEED_BUFF_TURNS = 3;
    private static final double SPEED_BUFF_AMOUNT = 2.0;
    private static final int STUCK_LIMIT = 90;

    enum Type {
        TANKY("Tanky", "Heavy hitter", 200, 10, 3.0, new Color(255, 140, 0), "tanky.png", Skill.TitanForm::new),
        SPEEDY("Speedy", "Skirmisher", 100, 15, 6.0, new Color(255, 230, 50), "speedy.png", Skill.ShadowSplit::new),
        BALANCED("Balanced", "All-rounder", 150, 12, 4.5, new Color(220, 220, 220), "balanced.png", Skill.EggTrail::new);

        private final String title, role, sprite;
        private final int hp, attack;
        private final double speed;
        private final Color color;
        private final Supplier<Skill> skill;

        Type(String title, String role, int hp, int attack, double speed, Color color, String sprite, Supplier<Skill> skill) {
            this.title = title;
            this.role = role;
            this.hp = hp;
            this.attack = attack;
            this.speed = speed;
            this.color = color;
            this.sprite = sprite;
            this.skill = skill;
        }

        HeroChicken create() { return new HeroChicken(this); }
    }

    private final Type type;
    private final String role;
    private final Skill skill;
    private final double baseSpeed;

    private boolean doubleDamage;
    private int speedBuffTurns;
    private int stuckTimer;

    private HeroChicken(Type type) {
        super(type.title, type.hp, type.attack, type.speed, type.color, 30, type.sprite);
        this.type = type;
        this.role = type.role;
        this.skill = type.skill.get();
        this.baseSpeed = type.speed;
    }

    public final Type type() { return type; }
    public final String role() { return role; }
    public final Skill skill() { return skill; }
    public final double baseSpeed() { return baseSpeed; }
    public final boolean hasDoubleDamage() { return doubleDamage; }
    public final boolean hasSpeedBuff() { return speedBuffTurns > 0; }

    public final void grantDoubleDamage() { doubleDamage = true; }

    public final void applySpeedBuff() {
        setSpeed(baseSpeed + SPEED_BUFF_AMOUNT);
        speedBuffTurns = SPEED_BUFF_TURNS;
    }

    public final void scaleAttack(double factor) { setAttack((int) Math.round(getAttack() * factor)); }

    public final void resizeAroundCenter(int newSize) {
        double cx = centerX(), cy = centerY();
        setSize(newSize);
        setPosition(cx - newSize / 2.0, cy - newSize / 2.0);
    }

    public final void activateSkill() { if (isAlive()) skill.activate(this); }

    public final void startTurn() {
        skill.startTurn(this);
        if (speedBuffTurns > 0 && --speedBuffTurns == 0) setSpeed(baseSpeed);
    }

    public final void prepareForBattle(double x, double y) {
        spawnAt(x, y);
        skill.resetForBattle(this);
    }

    // คำนวณแรงพุ่งจากระยะลากเมาส์
    public final double launchPower(double dragDistance) {
        return Math.min(dragDistance * POWER_SCALE * getSpeed(), MAX_POWER);
    }

    public final double maxPower() { return MAX_POWER; }

    public final void launch(double angle, double power) {
        setVelocity(power * Math.cos(angle), power * Math.sin(angle));
    }

    public final boolean isInMotion() { return isMoving() || skill.isBusy(); }

    // อัปเดตฟิสิกส์และสกิลของไก่ผู้เล่น
    public final void step(Arena arena, Effects fx) {
        updatePhysics(arena);
        skill.afterPhysics(this, arena, fx);
        handleObstacles(arena, fx);
        handlePortals(arena, fx);
        skill.interact(arena, fx);
    }

    // ตรวจการชนและใช้สกิลกับบอส
    public final void collideWith(BossChicken boss, Effects fx) {
        collideBody(boss, fx);
        skill.collide(this, boss, fx);
    }

    private void collideBody(BossChicken boss, Effects fx) {
        double dx = centerX() - boss.centerX();
        double dy = centerY() - boss.centerY();
        double dist = Math.hypot(dx, dy);
        double minDist = (size() + boss.size()) / 2.0;
        if (dist == 0) { dx = 1; dy = 0; dist = 1; }

        if (dist <= minDist + 2.0) {
            if (++stuckTimer >= STUCK_LIMIT) {
                double push = 12.0;
                setVelocity(dx / dist * push, dy / dist * push);
                boss.setVelocity(-dx / dist * push, -dy / dist * push);
                stuckTimer = 0;
                return;
            }
        } else {
            stuckTimer = 0;
        }

        if (dist >= minDist) return;

        if (!isOnHitCooldown() && !boss.isOnHitCooldown()) {
            int dmg = getAttack();
            if (doubleDamage) { dmg *= 2; doubleDamage = false; }
            boss.takeDamage(dmg, fx);
            takeDamage(boss.getAttack(), fx);

            fx.shake(15);
            fx.hitStop(5);
            fx.burst(centerX(), centerY(), getColor(), 15);
            fx.burst(boss.centerX(), boss.centerY(), boss.getColor(), 15);

            double tvx = vx(), tvy = vy();
            setVelocity(boss.vx(), boss.vy());
            boss.setVelocity(tvx, tvy);
        }
        double overlap = minDist - dist;
        if (overlap > 0) {
            translate(dx / dist * overlap / 2.0, dy / dist * overlap / 2.0);
            boss.translate(-dx / dist * overlap / 2.0, -dy / dist * overlap / 2.0);
        }
    }

    public final void bounceOff(HeroChicken other, Effects fx) {
        double dx = centerX() - other.centerX();
        double dy = centerY() - other.centerY();
        double dist = Math.hypot(dx, dy);
        if (dist == 0) { dx = 1; dy = 0; dist = 1; }
        double overlap = (size() + other.size()) / 2.0 - dist;
        if (overlap > 0) {
            translate(dx / dist * overlap / 2.0, dy / dist * overlap / 2.0);
            other.translate(-dx / dist * overlap / 2.0, -dy / dist * overlap / 2.0);
        }
        double tvx = vx(), tvy = vy();
        setVelocity(other.vx(), other.vy());
        other.setVelocity(tvx, tvy);
        fx.burst(centerX(), centerY(), Color.WHITE, 5);
    }

    @Override
    protected void drawAura(Graphics2D g) {
        if (doubleDamage) drawRing(g, UI.RED, 5);
    }

    @Override
    public void draw(Graphics2D g) {
        super.draw(g);
        skill.draw(g, this);
    }
}
