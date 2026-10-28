import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Random;

final class BossChicken extends Chicken {
    private static final double NORMAL_POWER = 17.0;
    private static final double ENRAGED_POWER = 25.0;

    private boolean enraged;

    BossChicken(double x, double y) {
        super("Void Boss", 400, 15, 5.0, new Color(180, 0, 180), 40, "boss.png");
        setPosition(x, y);
    }

    public boolean isEnraged() { return enraged; }

    // เลือกเป้าหมายและพุ่งโจมตีผู้เล่น
    public void charge(List<HeroChicken> heroes, Random random) {
        HeroChicken target = null;
        double nearest = Double.MAX_VALUE;
        for (HeroChicken h : heroes) {
            if (!h.isAlive()) continue;
            double d = Math.hypot(h.x() - x(), h.y() - y());
            if (d < nearest) { nearest = d; target = h; }
        }
        double angle = target != null
                ? Math.atan2(target.y() - y(), target.x() - x()) + (random.nextDouble() - 0.5) * 0.3
                : random.nextDouble() * 2 * Math.PI;
        double power = enraged ? ENRAGED_POWER : NORMAL_POWER;
        setVelocity(power * Math.cos(angle), power * Math.sin(angle));
    }

    @Override
    public void takeDamage(int amount, Effects fx) {
        super.takeDamage(amount, fx);
        if (!enraged && isAlive() && getHp() <= getMaxHp() * 0.4) enrage(fx);
    }

    private void enrage(Effects fx) {
        enraged = true;
        translate(-7.5, -7.5);
        setSize(size() + 15);
        setAttack(getAttack() + 5);
        setColor(new Color(220, 20, 20));
        fx.floatText((int) x(), (int) y() - 20, "ENRAGED!", UI.RED);
        fx.burst(centerX(), centerY(), UI.RED, 40);
    }

    @Override protected Color barColor(double frac) { return UI.RED; }

    @Override
    protected void drawAura(Graphics2D g) {
        if (enraged && isAlive()) drawRing(g, UI.RED, 5);
    }
}
