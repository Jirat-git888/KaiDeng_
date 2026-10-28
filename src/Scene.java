import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

abstract class Scene {
    private final KaiDeng game;
    private int ticks;
    private int mouseX = -1, mouseY = -1;

    protected Scene(KaiDeng game) { this.game = game; }

    // อัปเดตสถานะของฉากปัจจุบัน
    public final void update() {
        ticks++;
        updateScene();
    }

    protected void updateScene() { }
    public abstract void render(Graphics2D g);

    public void pointerMoved(int x, int y) { mouseX = x; mouseY = y; }
    public void mousePressed(MouseEvent e) { }
    public void mouseDragged(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }
    public void keyPressed(KeyEvent e) { }
    public void keyReleased(KeyEvent e) { }

    public boolean wantsHandCursor() { return false; }

    protected final int ticks() { return ticks; }
    protected final int mouseX() { return mouseX; }
    protected final int mouseY() { return mouseY; }
    protected final KaiDeng game() { return game; }
    protected final UI.Backgrounds backgrounds() { return game.backgrounds(); }

    protected final void goTo(Scene next) { game.show(next); }
}
