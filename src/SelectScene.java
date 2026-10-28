import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

final class SelectScene extends Scene {
    private final List<HeroCard> cards = new ArrayList<>();
    private final List<HeroChicken.Type> picked = new ArrayList<>();

    SelectScene(KaiDeng game) {
        super(game);
        HeroChicken.Type[] types = HeroChicken.Type.values();
        for (int i = 0; i < types.length; i++) cards.add(new HeroCard(types[i], i));
    }

    private boolean isFull() { return picked.size() >= KaiDeng.TEAM_SIZE; }

    // เลือกไก่สองตัวแล้วเริ่มต่อสู้ทันที
    @Override
    public void mousePressed(MouseEvent e) {
        for (HeroCard card : cards) {
            if (card.contains(e.getX(), e.getY()) && !picked.contains(card.type()) && !isFull()) {
                picked.add(card.type());
            }
        }
        if (isFull()) {
            List<HeroChicken> team = new ArrayList<>();
            for (HeroChicken.Type type : picked) team.add(type.create());
            BossChicken boss = new BossChicken(KaiDeng.BOSS_BATTLE_SPAWN.x, KaiDeng.BOSS_BATTLE_SPAWN.y);
            goTo(new BattleScene(game(), team, boss));
        }
    }

    @Override
    public boolean wantsHandCursor() {
        if (isFull()) return false;
        for (HeroCard card : cards) {
            if (card.contains(mouseX(), mouseY()) && !picked.contains(card.type())) return true;
        }
        return false;
    }

    @Override
    public void render(Graphics2D g) {
        g.drawImage(backgrounds().grass(), 0, 0, null);

        UI.panel(g, 210, 20, 380, 64);
        UI.center(g, "Pick " + KaiDeng.TEAM_SIZE + " chickens  " + picked.size() + "/" + KaiDeng.TEAM_SIZE,
                KaiDeng.WIDTH / 2, 61, UI.HEAD, UI.DARK);

        for (HeroCard card : cards) {
            boolean selected = picked.contains(card.type());
            boolean hover = card.contains(mouseX(), mouseY()) && !selected && !isFull();
            card.draw(g, selected, hover, isFull() && !selected);
        }
    }

    private static final class HeroCard {
        private final HeroChicken hero;
        private final Rectangle bounds;

        HeroCard(HeroChicken.Type type, int index) {
            this.hero = type.create();
            this.bounds = new Rectangle(24 + index * 256, 110, 240, 378);
        }

        HeroChicken.Type type() { return hero.type(); }
        boolean contains(int x, int y) { return bounds.contains(x, y); }

        void draw(Graphics2D g, boolean selected, boolean hover, boolean dimmed) {
            int x = bounds.x, y = bounds.y, w = bounds.width, h = bounds.height;
            int cx = x + w / 2;

            if (selected || hover) {
                UI.notch(g, x - 8, y - 8, w + 16, h + 16, 4, UI.DARK);
                UI.notch(g, x - 6, y - 6, w + 12, h + 12, 4, selected ? UI.BLUE : UI.YELLOW);
            }
            UI.panel(g, x, y, w, h);

            int sx = cx - 58, sy = y + 18, ss = 116;
            UI.notch(g, sx, sy, ss, ss, 4, UI.DARK);
            UI.notch(g, sx + 2, sy + 2, ss - 4, ss - 4, 2, new Color(198, 150, 90));
            UI.notch(g, sx + 4, sy + 4, ss - 8, ss - 8, 0, new Color(224, 184, 118));
            UI.shadowEllipse(g, cx, sy + ss - 14, 64, 10, 70);
            Image sprite = hero.sprite(96);
            if (sprite != null) {
                g.drawImage(sprite, cx - 48, sy + ss - 96 - 8, null);
            } else {
                g.setColor(UI.DARK);
                g.fillOval(cx - 43, sy + 15, 86, 86);
                g.setColor(hero.getColor());
                g.fillOval(cx - 40, sy + 18, 80, 80);
            }

            UI.center(g, hero.getName(), cx, y + 162, UI.HEAD, UI.DARK);

            int lx = x + 24, rx = x + w - 24;
            String[] labels = {"HP", "Attack", "Speed"};
            String[] values = {String.valueOf(hero.getMaxHp()), String.valueOf(hero.getAttack()), String.valueOf(hero.baseSpeed())};
            for (int k = 0; k < 3; k++) {
                int ty = y + 198 + k * 24;
                UI.draw(g, labels[k], lx, ty, UI.BODY, UI.DARK);
                UI.right(g, values[k], rx, ty, UI.BODY, UI.DARK);
            }

            Skill skill = hero.skill();
            g.setColor(UI.PARCH_DARK);
            g.fillRect(lx, y + 258, w - 48, 2);
            int nameX = lx;
            if (skill.icon() != null) {
                int iy = y + 266;
                UI.notch(g, lx, iy, 36, 36, 2, UI.DARK);
                UI.notch(g, lx + 2, iy + 2, 32, 32, 0, new Color(224, 184, 118));
                g.drawImage(skill.icon(), lx + 2, iy + 2, null);
                nameX = lx + 46;
            }
            UI.draw(g, skill.name(), nameX, y + 289, UI.BODY, UI.RED);
            UI.draw(g, "Cooldown " + skill.maxCooldown(), nameX, y + 301, UI.TINY, new Color(150, 100, 60));
            String[] lines = skill.description().split("\n");
            for (int k = 0; k < lines.length; k++) UI.draw(g, lines[k], lx, y + 326 + k * 18, UI.SMALL, UI.DARK);

            if (selected) {
                UI.plate(g, cx - 44, y - 30, 88, 30);
                UI.center(g, "Picked", cx, y - 8, UI.SMALL, new Color(40, 110, 30));
            } else if (dimmed) {
                g.setColor(new Color(0, 0, 0, 90));
                g.fillRect(x, y, w, h);
            }
        }
    }
}
