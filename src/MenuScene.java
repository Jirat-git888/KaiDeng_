import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

final class MenuScene extends Scene {
    private final Button start;
    private final Button exit;
    private final List<Button> buttons;
    private final Chicken mascotHero = HeroChicken.Type.SPEEDY.create();
    private final Chicken mascotBoss = new BossChicken(0, 0);

    MenuScene(KaiDeng game) {
        super(game);
        start = new Button(new Rectangle(280, 290, 240, 52), "Play", () -> goTo(new SelectScene(game)));
        exit = new Button(new Rectangle(280, 354, 240, 52), "Exit", game::exit);
        buttons = List.of(start, exit);
    }

    // รับการกด Play หรือออกจากเกม
    @Override
    public void mousePressed(MouseEvent e) {
        for (Button b : buttons) {
            if (b.contains(e.getX(), e.getY())) {
                b.click();
                return;
            }
        }
    }

    @Override
    public boolean wantsHandCursor() {
        for (Button b : buttons) if (b.contains(mouseX(), mouseY())) return true;
        return false;
    }

    @Override
    public void render(Graphics2D g) {
        g.drawImage(backgrounds().menu(), 0, 0, null);
        drawCloud(g, 60, 70, 0.25, 1.0);
        drawCloud(g, 420, 130, 0.15, 0.7);

        drawMascot(g, mascotHero, 150, 478, 84);
        drawMascot(g, mascotBoss, 650, 478, 92);

        UI.centerOutlined(g, "KaiDeng", KaiDeng.WIDTH / 2 + 4, 190, UI.TITLE, UI.DARK, UI.DARK);
        UI.centerOutlined(g, "KaiDeng", KaiDeng.WIDTH / 2, 184, UI.TITLE, UI.YELLOW, UI.DARK);

        for (Button b : buttons) b.draw(g, b.contains(mouseX(), mouseY()));
    }

    private void drawCloud(Graphics2D g, int baseX, int y, double speed, double scale) {
        int span = KaiDeng.WIDTH + 260;
        int x = (int) ((baseX + ticks() * speed) % span) - 130;
        int s = (int) (24 * scale);
        g.setColor(java.awt.Color.WHITE);
        g.fillOval(x, y, s * 3, s * 2);
        g.fillOval(x + s, y - s / 2, s * 2, s * 2);
        g.fillOval(x + s * 2, y + s / 4, s * 3, s * 2 - s / 3);
        g.setColor(new java.awt.Color(214, 232, 246));
        g.fillRect(x + s / 2, y + s * 2 - s / 4, s * 4, s / 4);
    }

    private void drawMascot(Graphics2D g, Chicken c, int cx, int cy, int size) {
        UI.shadowEllipse(g, cx, cy + size * 0.5, size * 0.8, 14, 90);
        java.awt.Image img = c.sprite(size);
        if (img != null) {
            g.drawImage(img, cx - size / 2, cy - size / 2, null);
        } else {
            g.setColor(UI.DARK);
            g.fillOval(cx - size / 2 - 3, cy - size / 2 - 3, size + 6, size + 6);
            g.setColor(c.getColor());
            g.fillOval(cx - size / 2, cy - size / 2, size, size);
        }
    }

    private static final class Button {
        private final Rectangle bounds;
        private final String label;
        private final Runnable action;

        Button(Rectangle bounds, String label, Runnable action) {
            this.bounds = new Rectangle(bounds);
            this.label = label;
            this.action = action;
        }

        boolean contains(int x, int y) { return bounds.contains(x, y); }
        void click() { action.run(); }
        void draw(Graphics2D g, boolean hover) { UI.button(g, bounds, label, hover); }
    }
}
