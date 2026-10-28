import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

final class Arena {
    private static final int MAX_BUFFS = 4;

    private final Rectangle bounds;
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Portal> portals = new ArrayList<>();
    private final List<EggMine> mines = new ArrayList<>();
    private final List<BuffItem> buffs = new ArrayList<>();

    Arena(Rectangle bounds) { this.bounds = new Rectangle(bounds); }

    int left() { return bounds.x; }
    int top() { return bounds.y; }
    int width() { return bounds.width; }
    int height() { return bounds.height; }
    Rectangle bounds() { return new Rectangle(bounds); }

    List<Obstacle> obstacles() { return Collections.unmodifiableList(obstacles); }
    List<Portal> portals() { return Collections.unmodifiableList(portals); }
    List<EggMine> mines() { return Collections.unmodifiableList(mines); }
    List<BuffItem> buffs() { return Collections.unmodifiableList(buffs); }

    void addMine(EggMine mine) { mines.add(mine); }

    // สร้างสิ่งกีดขวางและหลุมวาร์ปในสนาม
    void generate(Random r, Point bossSpawn, List<Point> heroSpawns) {
        obstacles.clear();
        portals.clear();
        generateObstacles(r, bossSpawn, heroSpawns);
        generatePortals(r, bossSpawn, heroSpawns);
    }

    private void generateObstacles(Random r, Point bossSpawn, List<Point> heroSpawns) {
        int count = 1 + r.nextInt(3);
        for (int i = 0; i < count; i++) {
            for (int attempt = 0; attempt < 100; attempt++) {
                int ox = bounds.x + 50 + r.nextInt(bounds.width - 100);
                int oy = bounds.y + 50 + r.nextInt(bounds.height - 100);
                if (isClear(ox, oy, bossSpawn, 60, heroSpawns, 80, 60)) {
                    obstacles.add(new Obstacle(ox, oy, 35));
                    break;
                }
            }
        }
    }

    private void generatePortals(Random r, Point bossSpawn, List<Point> heroSpawns) {
        Portal[] pair = new Portal[2];
        for (int i = 0; i < 2; i++) {
            int px = 0, py = 0;
            for (int attempt = 0; attempt < 100; attempt++) {
                px = bounds.x + 40 + r.nextInt(bounds.width - 80);
                py = bounds.y + 40 + r.nextInt(bounds.height - 80);
                boolean farFromFirst = i == 0 || Math.hypot(px - pair[0].x(), py - pair[0].y()) >= 100;
                if (farFromFirst && isClear(px, py, bossSpawn, 70, heroSpawns, 70, 60)) break;
            }
            pair[i] = new Portal(px, py, 45);
            portals.add(pair[i]);
        }
        Portal.link(pair[0], pair[1], UI.BLUE, UI.PURPLE);
    }

    private boolean isClear(int x, int y, Point boss, int bossGap, List<Point> heroes, int heroGap, int obstacleGap) {
        if (Math.hypot(x - boss.x, y - boss.y) <= bossGap) return false;
        for (Point p : heroes) if (Math.hypot(x - p.x, y - p.y) <= heroGap) return false;
        for (Obstacle o : obstacles) if (Math.hypot(x - o.x(), y - o.y()) < obstacleGap) return false;
        return true;
    }

    void spawnBuff(Random r) {
        if (buffs.size() >= MAX_BUFFS) return;
        buffs.add(new BuffItem(bounds.x + 20 + r.nextInt(bounds.width - 40),
                bounds.y + 20 + r.nextInt(bounds.height - 40), BuffType.random(r)));
    }

    void collectBuffs(List<HeroChicken> heroes, Effects fx) {
        Iterator<BuffItem> it = buffs.iterator();
        while (it.hasNext()) {
            BuffItem b = it.next();
            for (HeroChicken h : heroes) {
                if (h.isAlive() && h.collidesWith(b)) {
                    b.applyTo(h, fx);
                    it.remove();
                    break;
                }
            }
        }
    }

    void triggerMines(BossChicken boss, Effects fx) {
        Iterator<EggMine> it = mines.iterator();
        while (it.hasNext()) {
            EggMine m = it.next();
            if (boss.isAlive() && boss.collidesWith(m)) {
                boss.takeDamage(25, fx);
                boss.setVelocity(0, 0);
                fx.shake(15);
                fx.hitStop(8);
                fx.burst(m.centerX(), m.centerY(), new Color(255, 100, 50), 30);
                it.remove();
            }
        }
    }

    void tick() {
        for (Portal p : portals) p.tick();
        for (BuffItem b : buffs) b.tick();
    }

    void draw(Graphics2D g) {
        for (Portal p : portals) p.draw(g);
        for (Obstacle o : obstacles) o.draw(g);
        for (EggMine m : mines) m.draw(g);
        for (BuffItem b : buffs) b.draw(g);
    }

    private static final int MAX_PREDICT_STEPS = 300;

    static final class Trajectory {
        private final List<List<double[]>> segments = new ArrayList<>();
        private double endAngle;

        List<List<double[]>> segments() { return Collections.unmodifiableList(segments); }

        double endAngle() { return endAngle; }
        double[] endPoint() {
            List<double[]> last = segments.get(segments.size() - 1);
            return last.get(last.size() - 1);
        }
    }

    // จำลองการเคลื่อนที่เพื่อคาดการณ์วิถี
    Trajectory predict(HeroChicken hero, double vx, double vy) {
        Trajectory result = new Trajectory();
        List<double[]> current = new ArrayList<>();
        result.segments.add(current);

        double x = hero.x(), y = hero.y();
        int size = hero.size();

        Portal lastWarped = hero.lastWarpedPortal();

        int left = left(), top = top(), right = left + width(), bottom = top + height();
        double moveVx = vx, moveVy = vy;
        result.endAngle = Math.atan2(vy, vx);
        current.add(new double[]{x + size / 2.0, y + size / 2.0});

        for (int step = 0; step < MAX_PREDICT_STEPS; step++) {
            moveVx = vx;
            moveVy = vy;
            x += vx;
            y += vy;
            vx *= 0.94;
            vy *= 0.94;
            if (Math.abs(vx) < 0.2) vx = 0;
            if (Math.abs(vy) < 0.2) vy = 0;

            if (x <= left)           { x = left;           vx *= -1; }
            if (x + size >= right)   { x = right - size;   vx *= -1; }
            if (y <= top)            { y = top;            vy *= -1; }
            if (y + size >= bottom)  { y = bottom - size;  vy *= -1; }

            for (Obstacle obs : obstacles) {
                double dx = (x + size / 2.0) - obs.centerX();
                double dy = (y + size / 2.0) - obs.centerY();
                double dist = Math.hypot(dx, dy);
                double minDist = (size + obs.size()) / 2.0;
                if (dist >= minDist) continue;

                if (dist == 0) { dx = 1; dy = 0; dist = 1; }
                double nx = dx / dist, ny = dy / dist;
                double dot = vx * nx + vy * ny;
                if (dot < 0) {
                    vx = vx - 2 * dot * nx;
                    vy = vy - 2 * dot * ny;
                }
                double overlap = minDist - dist;
                x += nx * overlap;
                y += ny * overlap;
            }

            if (lastWarped != null && !overlaps(x, y, size, lastWarped)) lastWarped = null;
            for (Portal p : portals) {
                if (p == lastWarped || p.linked() == null || !overlaps(x, y, size, p)) continue;
                current.add(new double[]{x + size / 2.0, y + size / 2.0});
                Portal exit = p.linked();
                x = exit.x() + (exit.size() - size) / 2.0;
                y = exit.y() + (exit.size() - size) / 2.0;
                lastWarped = exit;
                current = new ArrayList<>();
                result.segments.add(current);
                break;
            }

            current.add(new double[]{x + size / 2.0, y + size / 2.0});
            if (vx == 0 && vy == 0) break;
        }

        if (Math.hypot(moveVx, moveVy) > 0) result.endAngle = Math.atan2(moveVy, moveVx);
        return result;
    }

    private static boolean overlaps(double x, double y, int size, Portal p) {
        return Math.hypot(x + size / 2.0 - p.centerX(), y + size / 2.0 - p.centerY()) < (size + p.size()) / 2.0;
    }

    static final class Obstacle extends Entity {
        Obstacle(double x, double y, int size) { super(x, y, size); }

        @Override
        public void draw(Graphics2D g) {
            int px = (int) x(), py = (int) y(), s = size();
            UI.shadowEllipse(g, centerX() + 2, y() + s * 0.92, s, s * 0.4, 90);
            g.setColor(UI.DARK);
            g.fillOval(px - 2, py - 2, s + 4, s + 4);
            g.setColor(new Color(150, 142, 134));
            g.fillOval(px, py, s, s);
            g.setColor(new Color(186, 180, 172));
            g.fillRect(px + s / 5, py + s / 6, s / 4, s / 6);
        }
    }

    static final class Portal extends Entity {
        private Portal linked;
        private Color tint = UI.PURPLE;

        Portal(double x, double y, int size) { super(x, y, size); }

        static void link(Portal a, Portal b, Color tintA, Color tintB) {
            a.linked = b;
            b.linked = a;
            a.tint = tintA;
            b.tint = tintB;
        }

        Portal linked() { return linked; }
        Color tint() { return tint; }

        @Override
        public void draw(Graphics2D g) {
            int px = (int) x(), py = (int) y(), d = size();
            g.setColor(UI.DARK);
            g.fillOval(px - 2, py - 2, d + 4, d + 4);
            g.setColor(tint);
            g.fillOval(px, py, d, d);
            g.setColor(UI.darker(tint, 0.5));
            g.fillOval(px + 7, py + 7, d - 14, d - 14);

            double a = age() * 0.07;
            int dx = (int) (centerX() + Math.cos(a) * (d / 2.0 - 4)) - 2;
            int dy = (int) (centerY() + Math.sin(a) * (d / 2.0 - 4)) - 2;
            g.setColor(UI.CREAM);
            g.fillRect(dx, dy, 4, 4);
        }
    }

    static final class EggMine extends Entity {
        EggMine(double x, double y) { super(x, y, 20); }

        @Override
        public void draw(Graphics2D g) {
            int px = (int) x(), py = (int) y(), s = size();
            UI.shadowEllipse(g, centerX() + 1, y() + s, s * 0.8, 6, 90);
            g.setColor(UI.DARK);
            g.fillOval(px, py - 2, s, s + 2);
            g.setColor(UI.CREAM);
            g.fillOval(px + 2, py, s - 4, s - 2);
            g.setColor(UI.RED);
            g.fillRect(px + s / 2 - 3, py + s / 2 - 3, 6, 6);
        }
    }

    enum BuffType {
        HEAL("+", UI.GREEN) {
            @Override void apply(HeroChicken hero, Effects fx) {
                hero.heal(50);
                fx.floatText((int) hero.x(), (int) hero.y(), "+50", UI.GREEN);
            }
        },
        DAMAGE_X2("x2", UI.RED) {
            @Override void apply(HeroChicken hero, Effects fx) {
                hero.grantDoubleDamage();
                fx.floatText((int) hero.x(), (int) hero.y(), "x2 DMG", UI.YELLOW);
            }
        },
        SPEED_UP(">>", UI.BLUE) {
            @Override void apply(HeroChicken hero, Effects fx) {
                hero.applySpeedBuff();
                fx.floatText((int) hero.x(), (int) hero.y(), "SPEED UP", UI.BLUE);
            }
        };

        private final String symbol;
        private final Color color;

        BuffType(String symbol, Color color) {
            this.symbol = symbol;
            this.color = color;
        }

        String symbol() { return symbol; }
        Color color() { return color; }

        abstract void apply(HeroChicken hero, Effects fx);

        static BuffType random(Random random) {
            BuffType[] all = values();
            return all[random.nextInt(all.length)];
        }
    }

    static final class BuffItem extends Entity {
        private final BuffType type;

        BuffItem(double x, double y, BuffType type) {
            super(x, y, 22);
            this.type = type;
        }

        BuffType type() { return type; }

        void applyTo(HeroChicken hero, Effects fx) { type.apply(hero, fx); }

        @Override
        public void draw(Graphics2D g) {
            int bob = (age() / 20 + (int) x()) % 2 == 0 ? 0 : -2;
            int px = (int) x(), py = (int) y() + bob, s = size();
            UI.shadowEllipse(g, centerX(), y() + s + 2, s * 0.8, 6, 90);
            g.setColor(UI.DARK);
            g.fillOval(px - 2, py - 2, s + 4, s + 4);
            g.setColor(type.color());
            g.fillOval(px, py, s, s);
            UI.center(g, type.symbol(), px + s / 2, py + s / 2 + 3, UI.TINY, UI.CREAM);
        }
    }
}
