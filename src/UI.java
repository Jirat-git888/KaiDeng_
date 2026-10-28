import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Random;

final class UI {
    private UI() { }

    static final Color DARK        = new Color(86, 40, 20);
    static final Color WOOD        = new Color(178, 104, 50);
    static final Color WOOD_LIGHT  = new Color(218, 152, 78);
    static final Color PARCH       = new Color(252, 226, 166);
    static final Color PARCH_DARK  = new Color(232, 194, 126);
    static final Color CREAM       = new Color(255, 246, 214);

    static final Color GRASS       = new Color(104, 170, 64);
    static final Color GRASS_DARK  = new Color(88, 150, 52);
    static final Color GRASS_LIGHT = new Color(128, 192, 82);
    static final Color DIRT        = new Color(200, 152, 100);
    static final Color DIRT_DARK   = new Color(178, 130, 82);
    static final Color SKY         = new Color(132, 198, 240);

    static final Color RED    = new Color(214, 66, 52);
    static final Color GREEN  = new Color(72, 198, 80);
    static final Color YELLOW = new Color(250, 202, 58);
    static final Color BLUE   = new Color(84, 150, 216);
    static final Color PURPLE = new Color(188, 100, 204);
    static final Color GREY   = new Color(150, 140, 130);

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static Color lerp(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static Color brighter(Color c, double t) { return lerp(c, Color.WHITE, t); }
    static Color darker(Color c, double t)   { return lerp(c, Color.BLACK, t); }

    static void notch(Graphics2D g, int x, int y, int w, int h, int n, Color c) {
        g.setColor(c);
        g.fillRect(x + n, y, w - 2 * n, h);
        g.fillRect(x, y + n, w, h - 2 * n);
    }

    static void panel(Graphics2D g, int x, int y, int w, int h) {
        notch(g, x, y, w, h, 4, DARK);
        notch(g, x + 2, y + 2, w - 4, h - 4, 2, WOOD);
        g.setColor(WOOD_LIGHT);
        g.fillRect(x + 4, y + 2, w - 8, 2);
        notch(g, x + 6, y + 6, w - 12, h - 12, 2, DARK);
        notch(g, x + 8, y + 8, w - 16, h - 16, 0, PARCH);
    }

    static void plate(Graphics2D g, int x, int y, int w, int h) {
        notch(g, x, y, w, h, 2, DARK);
        notch(g, x + 2, y + 2, w - 4, h - 4, 0, PARCH);
        g.setColor(PARCH_DARK);
        g.fillRect(x + 2, y + h - 4, w - 4, 2);
    }

    static void bar(Graphics2D g, int x, int y, int w, int h, double frac, Color fill) {
        frac = Math.max(0, Math.min(1, frac));
        g.setColor(DARK);
        g.fillRect(x, y, w, h);
        g.setColor(new Color(74, 40, 28));
        g.fillRect(x + 2, y + 2, w - 4, h - 4);
        int fw = (int) Math.round((w - 4) * frac);
        if (fw > 0) {
            g.setColor(fill);
            g.fillRect(x + 2, y + 2, fw, h - 4);
            g.setColor(brighter(fill, 0.35));
            g.fillRect(x + 2, y + 2, fw, Math.max(1, (h - 4) / 3));
        }
    }

    static void button(Graphics2D g, Rectangle r, String label, boolean hover) {
        int x = r.x, y = r.y, w = r.width, h = r.height;
        notch(g, x, y, w, h, 4, hover ? new Color(255, 214, 64) : DARK);
        notch(g, x + 2, y + 2, w - 4, h - 4, 2, DARK);
        notch(g, x + 4, y + 4, w - 8, h - 8, 2, hover ? new Color(255, 240, 190) : PARCH);
        g.setColor(hover ? PARCH : PARCH_DARK);
        g.fillRect(x + 6, y + h - 8, w - 12, 4);
        center(g, label, x + w / 2, y + h / 2 + 10, HEAD, DARK);
    }

    static void skillTile(Graphics2D g, int x, int y, int size, Image icon, String key, int state, int cooldown) {
        notch(g, x, y, size, size, 4, DARK);
        int inset = 2;
        if (state == 0) {
            notch(g, x + 2, y + 2, size - 4, size - 4, 2, YELLOW);
            inset = 4;
        }
        notch(g, x + inset, y + inset, size - 2 * inset, size - 2 * inset, 0,
                state == 1 ? new Color(160, 214, 110) : PARCH);

        boolean hasIcon = icon != null && icon.getWidth(null) > 0;
        if (hasIcon) {
            g.drawImage(icon, x + (size - icon.getWidth(null)) / 2, y + (size - icon.getHeight(null)) / 2, null);
        } else {
            center(g, key, x + size / 2, y + size / 2 + 7, BODY, DARK);
        }

        if (state == 2) {
            g.setColor(new Color(60, 30, 20, 150));
            g.fillRect(x + 2, y + 2, size - 4, size - 4);
            centerOutlined(g, String.valueOf(cooldown), x + size / 2, y + size / 2 + 10, HEAD, CREAM, DARK);
        }

        if (hasIcon) {
            int bx = x - 4, by = y - 4;
            g.setColor(DARK);
            g.fillRect(bx, by, 13, 13);
            g.setColor(CREAM);
            g.fillRect(bx + 1, by + 1, 11, 11);
            center(g, key, bx + 6, by + 10, TINY, DARK);
        }
    }

    static void shadowEllipse(Graphics2D g, double cx, double cy, double w, double h, int alpha) {
        g.setColor(new Color(40, 20, 10, alpha));
        g.fillOval((int) (cx - w / 2), (int) (cy - h / 2), (int) w, (int) h);
    }

    static final int TINY = 1, SMALL = 2, BODY = 2, HEAD = 3, BIG = 4, TITLE = 8;

    private static final int ROWS = 9, ABOVE = 7, PAD = 1;

    private static final class Glyph {
        boolean[][] px = new boolean[ROWS][];
        int minC, maxC, adv;
    }

    private static final HashMap<Character, Glyph> GLYPHS = new HashMap<>();
    private static final HashMap<String, BufferedImage> CACHE = new HashMap<>();

    private static void def(char ch, String rows) {
        String[] r = rows.split("/", -1);
        int width = 0;
        for (String s : r) width = Math.max(width, s.length());
        Glyph g = new Glyph();
        g.minC = Integer.MAX_VALUE;
        g.maxC = -1;
        for (int y = 0; y < ROWS; y++) {
            g.px[y] = new boolean[width];
            if (y >= r.length) continue;
            for (int x = 0; x < r[y].length(); x++) {
                if (r[y].charAt(x) == '#') {
                    g.px[y][x] = true;
                    g.minC = Math.min(g.minC, x);
                    g.maxC = Math.max(g.maxC, x);
                }
            }
        }
        if (g.maxC < 0) { g.minC = 0; g.maxC = 1; }
        g.adv = g.maxC - g.minC + 2;
        GLYPHS.put(ch, g);
    }

    static {
        def(' ', "");

        def('A', ".###./#...#/#...#/#####/#...#/#...#/#...#");
        def('B', "####./#...#/#...#/####./#...#/#...#/####.");
        def('C', ".###./#...#/#..../#..../#..../#...#/.###.");
        def('D', "####./#...#/#...#/#...#/#...#/#...#/####.");
        def('E', "#####/#..../#..../####./#..../#..../#####");
        def('F', "#####/#..../#..../####./#..../#..../#....");
        def('G', ".###./#...#/#..../#.###/#...#/#...#/.###.");
        def('H', "#...#/#...#/#...#/#####/#...#/#...#/#...#");
        def('I', "#####/..#../..#../..#../..#../..#../#####");
        def('J', "..###/...#./...#./...#./...#./#..#./.##..");
        def('K', "#...#/#..#./#.#../##.../#.#../#..#./#...#");
        def('L', "#..../#..../#..../#..../#..../#..../#####");
        def('M', "#...#/##.##/#.#.#/#.#.#/#...#/#...#/#...#");
        def('N', "#...#/##..#/#.#.#/#..##/#...#/#...#/#...#");
        def('O', ".###./#...#/#...#/#...#/#...#/#...#/.###.");
        def('P', "####./#...#/#...#/####./#..../#..../#....");
        def('Q', ".###./#...#/#...#/#...#/#.#.#/#..#./.##.#");
        def('R', "####./#...#/#...#/####./#.#../#..#./#...#");
        def('S', ".####/#..../#..../.###./....#/....#/####.");
        def('T', "#####/..#../..#../..#../..#../..#../..#..");
        def('U', "#...#/#...#/#...#/#...#/#...#/#...#/.###.");
        def('V', "#...#/#...#/#...#/#...#/#...#/.#.#./..#..");
        def('W', "#...#/#...#/#...#/#.#.#/#.#.#/##.##/#...#");
        def('X', "#...#/#...#/.#.#./..#../.#.#./#...#/#...#");
        def('Y', "#...#/#...#/.#.#./..#../..#../..#../..#..");
        def('Z', "#####/....#/...#./..#../.#.../#..../#####");

        def('a', "...../...../.###./....#/.####/#...#/.####");
        def('b', "#..../#..../####./#...#/#...#/#...#/####.");
        def('c', "...../...../.###./#...#/#..../#...#/.###.");
        def('d', "....#/....#/.####/#...#/#...#/#...#/.####");
        def('e', "...../...../.###./#...#/#####/#..../.###.");
        def('f', "..##./.#..#/.#.../###../.#.../.#.../.#...");
        def('g', "...../...../.####/#...#/#...#/#...#/.####/....#/.###.");
        def('h', "#..../#..../####./#...#/#...#/#...#/#...#");
        def('i', "..#../...../.##../..#../..#../..#../.###.");
        def('j', "...#./...../..##./...#./...#./...#./...#./#..#./.##..");
        def('k', "#..../#..../#..#./#.#../##.../#.#../#..#.");
        def('l', ".##../..#../..#../..#../..#../..#../.###.");
        def('m', "...../...../##.#./#.#.#/#.#.#/#.#.#/#.#.#");
        def('n', "...../...../####./#...#/#...#/#...#/#...#");
        def('o', "...../...../.###./#...#/#...#/#...#/.###.");
        def('p', "...../...../####./#...#/#...#/#...#/####./#..../#....");
        def('q', "...../...../.####/#...#/#...#/#...#/.####/....#/....#");
        def('r', "...../...../#.##./##..#/#..../#..../#....");
        def('s', "...../...../.####/#..../.###./....#/####.");
        def('t', "...../.#.../###../.#.../.#.../.#..#/..##.");
        def('u', "...../...../#...#/#...#/#...#/#..##/.##.#");
        def('v', "...../...../#...#/#...#/#...#/.#.#./..#..");
        def('w', "...../...../#...#/#.#.#/#.#.#/#.#.#/.#.#.");
        def('x', "...../...../#...#/.#.#./..#../.#.#./#...#");
        def('y', "...../...../#...#/#...#/#...#/.####/....#/#...#/.###.");
        def('z', "...../...../#####/...#./..#../.#.../#####");

        def('0', ".###./#...#/#..##/#.#.#/##..#/#...#/.###.");
        def('1', "..#../.##../..#../..#../..#../..#../.###.");
        def('2', ".###./#...#/....#/...#./..#../.#.../#####");
        def('3', "####./....#/....#/.###./....#/....#/####.");
        def('4', "...#./..##./.#.#./#..#./#####/...#./...#.");
        def('5', "#####/#..../####./....#/....#/#...#/.###.");
        def('6', ".###./#..../#..../####./#...#/#...#/.###.");
        def('7', "#####/....#/...#./..#../.#.../.#.../.#...");
        def('8', ".###./#...#/#...#/.###./#...#/#...#/.###.");
        def('9', ".###./#...#/#...#/.####/....#/....#/.###.");

        def('.', "././././././#");
        def(',', "././././././#/#");
        def('!', "#/#/#/#/#/./#");
        def('?', ".###./#...#/....#/..##./..#../...../..#..");
        def('+', "...../..#../..#../#####/..#../..#../.....");
        def('-', "...../...../...../#####");
        def('/', "....#/....#/...#./..#../.#.../#..../#....");
        def('%', "##..#/##..#/...#./..#../.#.../#..##/#..##");
        def('>', "..../#.../.#../..#./.#../#.../....");
        def(':', "././#/././#/.");
    }

    private static Glyph glyph(char c) {
        Glyph g = GLYPHS.get(c);
        return g != null ? g : GLYPHS.get('?');
    }

    private static int cells(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += glyph(s.charAt(i)).adv;
        return Math.max(0, w - 1);
    }

    static int width(String s, int scale) { return cells(s) * scale; }

    // สร้างภาพข้อความแบบพิกเซล
    private static BufferedImage image(String s, Color c, Color outline) {
        String key = c.getRGB() + "|" + (outline == null ? 0 : outline.getRGB()) + "|" + s;
        BufferedImage img = CACHE.get(key);
        if (img != null) return img;
        if (CACHE.size() > 800) CACHE.clear();

        int w = cells(s) + PAD * 2, h = ROWS + PAD * 2;
        img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] outlineRgb = outline == null ? null : new int[]{outline.getRGB()};

        for (int pass = 0; pass < 2; pass++) {
            if (pass == 0 && outlineRgb == null) continue;
            int x0 = PAD;
            for (int i = 0; i < s.length(); i++) {
                Glyph g = glyph(s.charAt(i));
                for (int y = 0; y < ROWS; y++) {
                    for (int x = g.minC; x <= g.maxC; x++) {
                        if (x >= g.px[y].length || !g.px[y][x]) continue;
                        int ix = x0 + x - g.minC, iy = PAD + y;
                        if (pass == 0) {
                            for (int dx = -1; dx <= 1; dx++) {
                                for (int dy = -1; dy <= 1; dy++) {
                                    int nx = ix + dx, ny = iy + dy;
                                    if (nx >= 0 && ny >= 0 && nx < w && ny < h) img.setRGB(nx, ny, outlineRgb[0]);
                                }
                            }
                        } else {
                            img.setRGB(ix, iy, c.getRGB());
                        }
                    }
                }
                x0 += g.adv;
            }
        }
        CACHE.put(key, img);
        return img;
    }

    static void draw(Graphics2D g, String s, int x, int y, int scale, Color c) {
        drawOutlined(g, s, x, y, scale, c, null);
    }

    static void drawOutlined(Graphics2D g, String s, int x, int y, int scale, Color c, Color outline) {
        BufferedImage img = image(s, c, outline);
        g.drawImage(img, x - PAD * scale, y - (PAD + ABOVE) * scale, img.getWidth() * scale, img.getHeight() * scale, null);
    }

    static void center(Graphics2D g, String s, int cx, int y, int scale, Color c) {
        draw(g, s, cx - width(s, scale) / 2, y, scale, c);
    }

    static void centerOutlined(Graphics2D g, String s, int cx, int y, int scale, Color c, Color outline) {
        drawOutlined(g, s, cx - width(s, scale) / 2, y, scale, c, outline);
    }

    static void right(Graphics2D g, String s, int right, int y, int scale, Color c) {
        draw(g, s, right - width(s, scale), y, scale, c);
    }

    static void rightOutlined(Graphics2D g, String s, int right, int y, int scale, Color c, Color outline) {
        drawOutlined(g, s, right - width(s, scale), y, scale, c, outline);
    }

    private static BufferedImage toArgb(Image src, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return img;
    }

    static Image scale(Image src, int w, int h) {
        int sw = src.getWidth(null), sh = src.getHeight(null);
        if (sw <= 0 || sh <= 0 || w <= 0 || h <= 0) return src;

        BufferedImage cur = toArgb(src, sw, sh);
        int cw = sw, ch = sh;
        while (cw / 2 >= w && ch / 2 >= h) {
            cw /= 2;
            ch /= 2;
            cur = resize(cur, cw, ch, true);
        }
        return resize(cur, w, h, true);
    }

    // ปรับขนาดสไปรต์ให้พอดีกับกรอบ
    static Image scaleFit(Image src, int box) {
        int sw = src.getWidth(null), sh = src.getHeight(null);
        if (sw <= 0 || sh <= 0 || box <= 0) return src;

        BufferedImage full = toArgb(src, sw, sh);
        Rectangle b = opaqueBounds(full);
        BufferedImage cropped = full.getSubimage(b.x, b.y, b.width, b.height);

        double f = Math.min((double) box / b.width, (double) box / b.height);
        BufferedImage scaled;
        if (f >= 1) {
            int k = (int) Math.floor(f);
            scaled = resize(cropped, b.width * k, b.height * k, false);
        } else {
            int tw = Math.max(1, (int) Math.round(b.width * f));
            int th = Math.max(1, (int) Math.round(b.height * f));
            scaled = (BufferedImage) scale(cropped, tw, th);
        }

        BufferedImage out = new BufferedImage(box, box, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(scaled, (box - scaled.getWidth()) / 2, box - scaled.getHeight(), null);
        g.dispose();
        return out;
    }

    private static Rectangle opaqueBounds(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((img.getRGB(x, y) >>> 24) > 20) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) return new Rectangle(0, 0, w, h);
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static BufferedImage resize(BufferedImage src, int w, int h, boolean smooth) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, smooth
                ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
                : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    static final class Backgrounds {
        static final int FENCE = 10;
        static final int ARENA_PAD = FENCE + 8;

        private final int width, height;
        private final BufferedImage grass, menu, arenaLayer;

        Backgrounds(int width, int height, Rectangle arena) {
            this.width = width;
            this.height = height;
            this.grass = makeGrass();
            this.menu = makeMenu();
            this.arenaLayer = makeArenaLayer(arena);
        }

        BufferedImage grass() { return grass; }
        BufferedImage menu() { return menu; }
        BufferedImage arenaLayer() { return arenaLayer; }

        private BufferedImage rgb() { return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); }

        private void paintGrass(Graphics2D g, int y0, int y1, long seed) {
            g.setColor(GRASS);
            g.fillRect(0, y0, width, y1 - y0);
            Random r = new Random(seed);
            for (int i = 0; i < 260; i++) {
                int x = r.nextInt(width / 4) * 4, y = y0 + r.nextInt(Math.max(1, (y1 - y0) / 4)) * 4;
                g.setColor(r.nextBoolean() ? GRASS_DARK : GRASS_LIGHT);
                g.fillRect(x, y, 4, 4);
                if (r.nextInt(3) == 0) g.fillRect(x + 4, y + 4, 4, 4);
            }
            Color[] petals = {Color.WHITE, new Color(255, 170, 190), YELLOW};
            for (int i = 0; i < 16; i++) {
                int x = r.nextInt(width / 4) * 4, y = y0 + r.nextInt(Math.max(1, (y1 - y0) / 4)) * 4;
                g.setColor(petals[r.nextInt(petals.length)]);
                g.fillRect(x, y, 6, 6);
                g.setColor(YELLOW);
                g.fillRect(x + 2, y + 2, 2, 2);
            }
        }

        private BufferedImage makeGrass() {
            BufferedImage img = rgb();
            Graphics2D g = img.createGraphics();
            paintGrass(g, 0, height, 3);
            g.dispose();
            return img;
        }

        private BufferedImage makeMenu() {
            BufferedImage img = rgb();
            Graphics2D g = img.createGraphics();
            g.setColor(SKY);
            g.fillRect(0, 0, width, height);
            g.setColor(GRASS_DARK);
            g.fillOval(-160, 330, 640, 300);
            g.fillOval(380, 350, 640, 300);
            g.setColor(GRASS_LIGHT);
            g.fillOval(180, 370, 520, 260);
            paintGrass(g, 410, height, 9);
            g.setColor(DARK);
            g.fillRect(0, 408, width, 4);
            g.dispose();
            return img;
        }

        // สร้างภาพพื้นหลังของสนามต่อสู้
        private BufferedImage makeArenaLayer(Rectangle arena) {
            int pad = ARENA_PAD, aw = arena.width, ah = arena.height;
            BufferedImage img = new BufferedImage(aw + pad * 2, ah + pad * 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();

            notch(g, pad - FENCE, pad - FENCE, aw + FENCE * 2, ah + FENCE * 2, 4, DARK);
            notch(g, pad - FENCE + 2, pad - FENCE + 2, aw + FENCE * 2 - 4, ah + FENCE * 2 - 4, 2, WOOD);
            g.setColor(WOOD_LIGHT);
            g.fillRect(pad - FENCE + 4, pad - FENCE + 2, aw + FENCE * 2 - 8, 2);

            g.setColor(DARK);
            g.fillRect(pad - 2, pad - 2, aw + 4, ah + 4);
            g.setColor(DIRT);
            g.fillRect(pad, pad, aw, ah);
            g.setColor(DIRT_DARK);
            g.fillRect(pad, pad, aw, 4);

            Random r = new Random(21);
            for (int i = 0; i < 90; i++) {
                int x = pad + 8 + r.nextInt((aw - 16) / 2) * 2, y = pad + 8 + r.nextInt((ah - 16) / 2) * 2;
                g.setColor(r.nextBoolean() ? DIRT_DARK : new Color(216, 172, 122));
                g.fillRect(x, y, r.nextBoolean() ? 4 : 2, 2);
            }

            int[][] posts = {{0, 0}, {aw, 0}, {0, ah}, {aw, ah}, {aw / 2, 0}, {aw / 2, ah}};
            for (int[] p : posts) {
                int px = pad + p[0] - 8, py = pad + p[1] - 8;
                g.setColor(DARK);
                g.fillRect(px - 2, py - 2, 20, 20);
                g.setColor(WOOD_LIGHT);
                g.fillRect(px, py, 16, 16);
                g.setColor(WOOD);
                g.fillRect(px, py + 10, 16, 6);
            }
            g.dispose();
            return img;
        }
    }
}
