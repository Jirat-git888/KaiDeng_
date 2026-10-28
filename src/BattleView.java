import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.List;

final class BattleView {
    // วาดสถานะทีมและบอส
    void drawHud(Graphics2D g, BossChicken boss, List<HeroChicken> heroes, BattleScene.TurnPhase phase) {
        drawBossBar(g, boss);
        drawTurnTab(g, phase);
        drawHeroCards(g, heroes);
    }

    void drawHint(Graphics2D g, BattleScene.TurnPhase phase) {
        if (!phase.hint().isEmpty()) {
            UI.centerOutlined(g, phase.hint(), KaiDeng.WIDTH / 2, 506, UI.BODY, UI.CREAM, UI.DARK);
        }
    }

    private void drawBossBar(Graphics2D g, BossChicken boss) {
        int x = 220, y = 10, w = 360, h = 50;
        UI.plate(g, x, y, w, h);
        boolean angry = boss.isEnraged();
        UI.draw(g, angry ? "Void Boss  is angry!" : "Void Boss", x + 12, y + 22, UI.BODY, angry ? UI.RED : UI.DARK);
        UI.right(g, Math.max(0, boss.getHp()) + "/" + boss.getMaxHp(), x + w - 12, y + 22, UI.SMALL, UI.DARK);
        UI.bar(g, x + 12, y + 28, w - 24, 14, boss.hpFraction(), UI.RED);
    }

    private void drawTurnTab(Graphics2D g, BattleScene.TurnPhase phase) {
        boolean mine = phase.isPlayerSide();
        UI.plate(g, 330, 66, 140, 32);
        UI.center(g, mine ? "Your turn" : "Boss turn", KaiDeng.WIDTH / 2, 89, UI.BODY,
                mine ? new Color(40, 110, 30) : UI.RED);
    }

    private void drawHeroCards(Graphics2D g, List<HeroChicken> heroes) {
        for (int i = 0; i < heroes.size(); i++) {
            HeroChicken hero = heroes.get(i);
            int x = 10, y = 10 + i * 58, w = 176, h = 50;
            UI.plate(g, x, y, w, h);

            g.setColor(UI.DARK);
            g.fillRect(x + 6, y + 8, 34, 34);
            g.setColor(UI.PARCH_DARK);
            g.fillRect(x + 8, y + 10, 30, 30);
            Image face = hero.sprite(28);
            if (face != null) g.drawImage(face, x + 9, y + 11, null);
            else { g.setColor(hero.getColor()); g.fillOval(x + 12, y + 14, 22, 22); }

            double frac = hero.hpFraction();
            UI.bar(g, x + 46, y + 10, 72, 12, frac, frac < 0.3 ? UI.RED : UI.GREEN);
            UI.draw(g, Math.max(0, hero.getHp()) + "/" + hero.getMaxHp(), x + 46, y + 40, UI.SMALL, UI.DARK);

            if (!hero.isAlive()) {
                g.setColor(new Color(0, 0, 0, 100));
                g.fillRect(x, y, w, h);
                continue;
            }
            Skill skill = hero.skill();
            int state = skill.isActive() ? 1 : skill.isReady() ? 0 : 2;
            UI.skillTile(g, x + w - 46, y + 6, 38, skill.icon(), i == 0 ? "Z" : "X", state, skill.cooldown());
        }
    }

    // วาดเส้นคาดการณ์วิถีขณะเล็ง
    void drawAim(Graphics2D g, HeroChicken hero, int dragX, int dragY, Arena arena, int minDrag) {
        double cx = hero.centerX(), cy = hero.centerY();

        g.setColor(UI.DARK);
        g.setStroke(new BasicStroke(3f));
        g.drawLine((int) cx, (int) cy, dragX, dragY);
        g.setStroke(new BasicStroke(1f));
        g.fillRect(dragX - 4, dragY - 4, 8, 8);

        double dx = cx - dragX, dy = cy - dragY;
        double dragDist = Math.hypot(dx, dy);
        if (dragDist < minDrag) return;

        double power = hero.launchPower(dragDist);
        Color dot = UI.lerp(UI.CREAM, UI.RED, power / hero.maxPower());
        double angle = Math.atan2(dy, dx);
        Arena.Trajectory res = arena.predict(hero, power * Math.cos(angle), power * Math.sin(angle));

        double travelled = 0, nextDot = 16, spacing = 15;
        for (List<double[]> seg : res.segments()) {
            for (int i = 1; i < seg.size(); i++) {
                double[] a = seg.get(i - 1), b = seg.get(i);
                double len = Math.hypot(b[0] - a[0], b[1] - a[1]);
                if (len == 0) continue;
                while (nextDot <= travelled + len) {
                    double t = (nextDot - travelled) / len;
                    int px = (int) (a[0] + (b[0] - a[0]) * t), py = (int) (a[1] + (b[1] - a[1]) * t);
                    g.setColor(UI.DARK);
                    g.fillRect(px - 4, py - 4, 8, 8);
                    g.setColor(dot);
                    g.fillRect(px - 2, py - 2, 4, 4);
                    nextDot += spacing;
                }
                travelled += len;
            }
        }

        double[] last = res.endPoint();
        double a = res.endAngle();
        int as = 16;
        int[] xp = {(int) last[0], (int) (last[0] - as * Math.cos(a - Math.PI / 6)), (int) (last[0] - as * Math.cos(a + Math.PI / 6))};
        int[] yp = {(int) last[1], (int) (last[1] - as * Math.sin(a - Math.PI / 6)), (int) (last[1] - as * Math.sin(a + Math.PI / 6))};
        g.setColor(dot);
        g.fillPolygon(xp, yp, 3);
        g.setColor(UI.DARK);
        g.drawPolygon(xp, yp, 3);
    }
}
