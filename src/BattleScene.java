import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class BattleScene extends Scene {
    static final int MIN_DRAG = 15;

    private final List<HeroChicken> heroes;
    private final BossChicken boss;
    private final Arena arena = new Arena(KaiDeng.ARENA);
    private final Effects fx = new Effects();
    private final Random random = new Random();
    private final BattleView view = new BattleView();

    private TurnPhase phase = TurnPhase.PLAYER_TURN;
    private HeroChicken aiming;
    private int dragX, dragY;
    private int turnsCompleted;

    BattleScene(KaiDeng game, List<HeroChicken> team, BossChicken boss) {
        super(game);
        this.heroes = new ArrayList<>(team);
        this.boss = boss;

        boss.spawnAt(KaiDeng.BOSS_BATTLE_SPAWN.x, KaiDeng.BOSS_BATTLE_SPAWN.y);
        List<Point> spawns = new ArrayList<>();
        int y = KaiDeng.HERO_BATTLE_SPAWN.y;
        for (HeroChicken hero : heroes) {
            hero.prepareForBattle(KaiDeng.HERO_BATTLE_SPAWN.x, y);
            spawns.add(new Point(KaiDeng.HERO_BATTLE_SPAWN.x, y));
            y += KaiDeng.HERO_SPAWN_GAP;
        }
        arena.generate(random, KaiDeng.BOSS_BATTLE_SPAWN, spawns);
    }

    List<HeroChicken> heroes() { return Collections.unmodifiableList(heroes); }
    BossChicken boss() { return boss; }
    Arena arena() { return arena; }
    TurnPhase phase() { return phase; }
    int turnNumber() { return turnsCompleted + 1; }

    // อัปเดตการต่อสู้และตรวจผลแพ้ชนะ
    @Override
    protected void updateScene() {
        if (fx.consumeHitStop()) return;

        boss.updatePhysics(arena);
        boss.handleObstacles(arena, fx);
        boss.handlePortals(arena, fx);
        fx.updateParticles();

        updateHeroes();
        updatePhase();
        arena.triggerMines(boss, fx);
        arena.collectBuffs(heroes, fx);
        fx.updateTexts();
        arena.tick();

        if (checkOutcome()) return;
        fx.decayShake();
    }

    private void updateHeroes() {
        for (int i = 0; i < heroes.size(); i++) {
            HeroChicken a = heroes.get(i);
            if (!a.isAlive()) continue;
            a.step(arena, fx);
            a.collideWith(boss, fx);
            for (int j = i + 1; j < heroes.size(); j++) {
                HeroChicken b = heroes.get(j);
                if (b.isAlive() && a.collidesWith(b)) a.bounceOff(b, fx);
            }
        }
    }

    private boolean anyoneMoving() {
        if (boss.isMoving()) return true;
        for (HeroChicken h : heroes) if (h.isAlive() && h.isInMotion()) return true;
        return false;
    }

    // ควบคุมลำดับเทิร์นผู้เล่นและบอส
    private void updatePhase() {
        switch (phase) {
            case PLAYER_MOVING:
                if (!anyoneMoving()) phase = TurnPhase.BOSS_TURN;
                break;
            case BOSS_TURN:
                boss.charge(heroes, random);
                phase = TurnPhase.BOSS_MOVING;
                break;
            case BOSS_MOVING:
                if (!anyoneMoving()) startNextPlayerTurn();
                break;
            default:
                break;
        }
    }

    private void startNextPlayerTurn() {
        phase = TurnPhase.PLAYER_TURN;
        turnsCompleted++;
        for (HeroChicken h : heroes) if (h.isAlive()) h.startTurn();
        if (turnsCompleted % 2 == 0) arena.spawnBuff(random);
    }

    private boolean checkOutcome() {
        boolean allDown = true;
        for (HeroChicken h : heroes) if (h.isAlive()) allDown = false;
        if (allDown) { goTo(new ResultScene(game(), this, false)); return true; }
        if (!boss.isAlive()) { goTo(new ResultScene(game(), this, true)); return true; }
        return false;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (phase != TurnPhase.PLAYER_TURN || !SwingUtilities.isLeftMouseButton(e)) return;
        for (HeroChicken h : heroes) {
            if (h.isAlive() && h.isNear(e.getX(), e.getY(), h.size())) {
                aiming = h;
                phase = TurnPhase.AIMING;
                dragX = e.getX();
                dragY = e.getY();
                return;
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (phase == TurnPhase.AIMING) {
            dragX = e.getX();
            dragY = e.getY();
        }
    }

    // ปล่อยไก่ตามแรงและทิศทางที่เล็งไว้
    @Override
    public void mouseReleased(MouseEvent e) {
        if (phase != TurnPhase.AIMING || aiming == null) return;
        double dx = aiming.centerX() - e.getX();
        double dy = aiming.centerY() - e.getY();
        double dist = Math.hypot(dx, dy);

        if (SwingUtilities.isRightMouseButton(e) || dist < MIN_DRAG) {
            aiming = null;
            phase = TurnPhase.PLAYER_TURN;
            return;
        }
        aiming.launch(Math.atan2(dy, dx), aiming.launchPower(dist));
        aiming = null;
        phase = TurnPhase.PLAYER_MOVING;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (phase != TurnPhase.PLAYER_TURN) return;
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_Z && heroes.size() > 0) heroes.get(0).activateSkill();
        if (code == KeyEvent.VK_X && heroes.size() > 1) heroes.get(1).activateSkill();
    }

    @Override
    public boolean wantsHandCursor() {
        if (phase != TurnPhase.PLAYER_TURN) return false;
        for (HeroChicken h : heroes) if (h.isAlive() && h.isNear(mouseX(), mouseY(), h.size())) return true;
        return false;
    }

    @Override
    public void render(Graphics2D g) {
        boolean shaking = fx.isShaking();
        int ox = shaking ? fx.shakeOffset() : 0, oy = shaking ? fx.shakeOffset() : 0;
        g.translate(ox, oy);
        draw(g);
        g.translate(-ox, -oy);
    }

    // วาดสนาม ตัวละคร และสถานะการต่อสู้
    void draw(Graphics2D g) {
        g.drawImage(backgrounds().grass(), 0, 0, null);
        g.drawImage(backgrounds().arenaLayer(), arena.left() - UI.Backgrounds.ARENA_PAD, arena.top() - UI.Backgrounds.ARENA_PAD, null);
        arena.draw(g);

        for (HeroChicken h : heroes) if (h.isAlive()) drawSelectionRing(g, h);
        boss.draw(g);
        for (HeroChicken h : heroes) if (h.isAlive()) h.draw(g);
        fx.drawParticles(g);

        view.drawHud(g, boss, heroes, phase);
        if (phase == TurnPhase.AIMING && aiming != null) view.drawAim(g, aiming, dragX, dragY, arena, MIN_DRAG);
        fx.drawTexts(g);
        view.drawHint(g, phase);
    }

    private void drawSelectionRing(Graphics2D g, HeroChicken h) {
        boolean selected = phase == TurnPhase.AIMING && h == aiming;
        boolean hover = phase == TurnPhase.PLAYER_TURN && h.isNear(mouseX(), mouseY(), h.size());
        if (selected || hover) h.drawRing(g, UI.YELLOW, 6);
    }

    enum TurnPhase {
        PLAYER_TURN(true, "Drag a chicken back to aim"),
        AIMING(true, "Let go to launch"),
        PLAYER_MOVING(true, ""),
        BOSS_TURN(false, ""),
        BOSS_MOVING(false, "");

        private final boolean playerSide;
        private final String hint;

        TurnPhase(boolean playerSide, String hint) {
            this.playerSide = playerSide;
            this.hint = hint;
        }

        boolean isPlayerSide() { return playerSide; }
        String hint() { return hint; }
    }

    static final class ResultScene extends Scene {
        private final BattleScene battle;
        private final boolean victory;

        ResultScene(KaiDeng game, BattleScene battle, boolean victory) {
            super(game);
            this.battle = battle;
            this.victory = victory;
        }

        private void restart() { goTo(new MenuScene(game())); }

        @Override public void keyPressed(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_ENTER) restart(); }
        @Override public void mousePressed(MouseEvent e) { restart(); }

        @Override
        public void render(Graphics2D g) {
            battle.draw(g);
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRect(0, 0, KaiDeng.WIDTH, KaiDeng.HEIGHT);

            int pw = 380, ph = 200, px = (KaiDeng.WIDTH - pw) / 2, py = 200, cx = KaiDeng.WIDTH / 2;
            UI.panel(g, px, py, pw, ph);
            UI.center(g, victory ? "You win!" : "You lost", cx, py + 70, UI.BIG,
                    victory ? new Color(40, 110, 30) : UI.RED);

            UI.center(g, "Turns  " + battle.turnNumber(), cx, py + 108, UI.BODY, UI.DARK);
            if (victory) {
                int alive = 0;
                for (HeroChicken h : battle.heroes()) if (h.isAlive()) alive++;
                UI.center(g, "Chickens left  " + alive + "/" + battle.heroes().size(), cx, py + 134, UI.BODY, UI.DARK);
            } else {
                UI.center(g, "Boss HP left  " + Math.max(0, battle.boss().getHp()), cx, py + 134, UI.BODY, UI.DARK);
            }

            if ((ticks() / 30) % 2 == 0) {
                UI.center(g, "Press Enter to play again", cx, py + 170, UI.SMALL, UI.DARK);
            }
        }
    }
}
