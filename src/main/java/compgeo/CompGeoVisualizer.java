package compgeo;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class CompGeoVisualizer extends JPanel {
    List<List<double[]>> polygons = new ArrayList<>();   // from compgeo.in
    List<List<double[]>> triangles = new ArrayList<>();  // from compgeo.out
    double scale = 1, ox, oy;   // ox, oy = screen position of the origin (0,0)
    boolean fitted = false;
    int lastX, lastY;
    DecimalFormat fmt = new DecimalFormat("0.######");

    static final Color[] POLY_COLORS = {Color.BLUE, Color.RED, new Color(0, 140, 0)};
    static final Color[] TRI_COLORS = {new Color(255, 140, 0), new Color(190, 0, 190), new Color(139, 69, 19)};

    CompGeoVisualizer() throws Exception {
        // compgeo.in: polygon count, then per line: point count followed by "x,y" points
        Scanner in = new Scanner(new File("compgeo.in"));
        int n = in.nextInt();
        for (int i = 0; i < n; i++) {
            int k = in.nextInt();
            polygons.add(readPoints(in, k));
        }
        in.close();

        // compgeo.out: triangle count, then per line: 3 points "x,y" (floating point numbers)
        File outFile = new File("compgeo.out");
        if (outFile.exists()) {
            Scanner out = new Scanner(outFile);
            int t = out.nextInt();
            for (int i = 0; i < t; i++) triangles.add(readPoints(out, 3));
            out.close();
        } else {
            System.err.println("compgeo.out not found - showing only compgeo.in");
        }

        // mouse wheel: zoom around the cursor
        addMouseWheelListener(e ->
                zoom(Math.pow(1.4, -e.getPreciseWheelRotation()), e.getX(), e.getY()));

        // keyboard fallback: + / - zoom around the window center, 0 resets
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (c == '+' || c == '=') zoom(1.4, getWidth() / 2.0, getHeight() / 2.0);
                else if (c == '-') zoom(1 / 1.4, getWidth() / 2.0, getHeight() / 2.0);
                else if (c == '0') { fit(); repaint(); }
            }
        });
        // drag: pan, double-click: reset view
        MouseAdapter mouse = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                lastX = e.getX();
                lastY = e.getY();
                if (e.getClickCount() == 2) { fit(); repaint(); }
            }
            public void mouseDragged(MouseEvent e) {
                ox += e.getX() - lastX;
                oy += e.getY() - lastY;
                lastX = e.getX();
                lastY = e.getY();
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    // reads k points of the form "x,y"; Double.parseDouble also handles 2.00e+2, 300.000, 0.0000 ...
    static List<double[]> readPoints(Scanner in, int k) {
        List<double[]> pts = new ArrayList<>();
        for (int j = 0; j < k; j++) {
            String[] p = in.next().split(",");
            pts.add(new double[]{Double.parseDouble(p[0]), Double.parseDouble(p[1])});
        }
        return pts;
    }

    // zoom by a factor around the screen position (mx, my)
    void zoom(double factor, double mx, double my) {
        double wx = (mx - ox) / scale, wy = (oy - my) / scale;
        scale *= factor;
        ox = mx - wx * scale;
        oy = my + wy * scale;
        repaint();
    }

    // choose zoom/offset so that all points are visible
    void fit() {
        double minX = 0, maxX = 0, minY = 0, maxY = 0;
        List<List<List<double[]>>> all = List.of(polygons, triangles);
        for (List<List<double[]>> shapes : all)
            for (List<double[]> shape : shapes)
                for (double[] p : shape) {
                    minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]);
                    minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]);
                }
        double size = Math.max(Math.max(maxX - minX, maxY - minY), 1);
        scale = Math.min(getWidth(), getHeight()) * 0.8 / size;
        ox = getWidth() / 2.0 - (minX + maxX) / 2 * scale;
        oy = getHeight() / 2.0 + (minY + maxY) / 2 * scale;
    }

    double sx(double x) { return ox + x * scale; }
    double sy(double y) { return oy - y * scale; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!fitted) { fit(); fitted = true; }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        // grid step: 1, 2 or 5 times a power of ten, at least ~60 px apart
        double raw = 60 / scale;
        double mag = Math.pow(10, Math.floor(Math.log10(raw)));
        double m = raw / mag;
        double step = (m <= 1 ? 1 : m <= 2 ? 2 : m <= 5 ? 5 : 10) * mag;

        double xMin = -ox / scale, xMax = (w - ox) / scale;
        double yMin = (oy - h) / scale, yMax = oy / scale;
        int labelY = (int) Math.max(12, Math.min(h - 4, sy(0) + 14));
        int labelX = (int) Math.max(4, Math.min(w - 40, sx(0) + 4));

        for (long k = (long) Math.ceil(xMin / step); k * step <= xMax; k++) {
            double v = k * step;
            g2.setColor(k == 0 ? Color.BLACK : Color.LIGHT_GRAY);
            g2.drawLine((int) sx(v), 0, (int) sx(v), h);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(fmt.format(v), (int) sx(v) + 3, labelY);
        }
        for (long k = (long) Math.ceil(yMin / step); k * step <= yMax; k++) {
            double v = k * step;
            g2.setColor(k == 0 ? Color.BLACK : Color.LIGHT_GRAY);
            g2.drawLine(0, (int) sy(v), w, (int) sy(v));
            if (k != 0) {
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(fmt.format(v), labelX, (int) sy(v) - 3);
            }
        }

        // triangles first (translucent fill, thick edges), so the polygons stay visible on top
        for (int i = 0; i < triangles.size(); i++) {
            List<double[]> tri = triangles.get(i);
            Color c = TRI_COLORS[i % TRI_COLORS.length];
            Path2D.Double path = new Path2D.Double();
            for (int j = 0; j < tri.size(); j++) {
                if (j == 0) path.moveTo(sx(tri.get(j)[0]), sy(tri.get(j)[1]));
                else path.lineTo(sx(tri.get(j)[0]), sy(tri.get(j)[1]));
            }
            path.closePath();
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70));
            g2.fill(path);
            drawVectors(g2, tri, c, 3f, 6);
        }

        // polygons: vectors from each point to the next, plus the points
        for (int i = 0; i < polygons.size(); i++)
            drawVectors(g2, polygons.get(i), POLY_COLORS[i % POLY_COLORS.length], 1.5f, 8);

        g2.setColor(Color.GRAY);
        g2.drawString("Wheel or +/-: zoom   Drag: pan   Double-click or 0: reset", 8, h - 8);
    }

    // draws the closed chain of vectors p0->p1->...->p0 with arrowheads, plus a dot on every point
    void drawVectors(Graphics2D g2, List<double[]> pts, Color c, float width, int dot) {
        g2.setColor(c);
        g2.setStroke(new BasicStroke(width));
        for (int j = 0; j < pts.size(); j++) {
            double[] a = pts.get(j), b = pts.get((j + 1) % pts.size());
            arrow(g2, sx(a[0]), sy(a[1]), sx(b[0]), sy(b[1]));
        }
        for (double[] p : pts)
            g2.fillOval((int) sx(p[0]) - dot / 2, (int) sy(p[1]) - dot / 2, dot, dot);
        g2.setStroke(new BasicStroke(1f));
    }

    void arrow(Graphics2D g2, double x1, double y1, double x2, double y2) {
        g2.draw(new Line2D.Double(x1, y1, x2, y2));
        double angle = Math.atan2(y2 - y1, x2 - x1);
        for (double d : new double[]{0.4, -0.4})
            g2.draw(new Line2D.Double(x2, y2,
                    x2 - 12 * Math.cos(angle + d), y2 - 12 * Math.sin(angle + d)));
    }

    public static void main(String[] args) throws Exception {
        JFrame f = new JFrame("compgeo");
        CompGeoVisualizer panel = new CompGeoVisualizer();
        f.add(panel);
        f.setSize(800, 800);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
        panel.requestFocusInWindow();
    }
}