package compgeo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.awt.geom.Path2D;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PunktInPolygonTest {

    private static Vector2 v(float x, float y) {
        return new Vector2(x, y);
    }

    private static Vector2[] poly(float... xy) {
        var p = new Vector2[xy.length / 2];
        for (int i = 0; i < p.length; i++)
            p[i] = v(xy[2 * i], xy[2 * i + 1]);
        return p;
    }

    private static Vector2[] reversed(Vector2[] p) {
        var r = new Vector2[p.length];
        for (int i = 0; i < p.length; i++)
            r[i] = p[p.length - 1 - i];
        return r;
    }

    /** Prueft in beiden Umlaufsinnen und mit Rotation des Startpunkts. */
    private static void check(Vector2[] poly, float x, float y, boolean expected, String what) {
        for (int dir = 0; dir < 2; dir++) {
            var base = dir == 0 ? poly : reversed(poly);
            for (int shift = 0; shift < base.length; shift++) {
                var rot = new Vector2[base.length];
                for (int i = 0; i < base.length; i++)
                    rot[i] = base[(i + shift) % base.length];
                boolean r = PunktInPolygon.contains(rot, v(x, y), false);
                assertEquals(expected, r, what + " bei (" + x + "," + y + "): erwartet " + expected
                        + " (dir=" + dir + ", shift=" + shift + ")");
            }
        }
    }

    private static void checkBoundary(Vector2[] poly, float x, float y, String what) {
        for (int dir = 0; dir < 2; dir++) {
            var base = dir == 0 ? poly : reversed(poly);
            assertTrue(PunktInPolygon.contains(base, v(x, y), true), what + " (boundaryInside=true)");
            assertTrue(!PunktInPolygon.contains(base, v(x, y), false), what + " (boundaryInside=false)");
        }
    }

    // ------------------------------------------------------------------ einfache Formen

    @Test
    void rechteck() {
        var r = poly(0, 0, 10, 0, 10, 5, 0, 5);
        check(r, 5, 2, true, "innen");
        check(r, -1, 2, false, "links");
        check(r, 11, 2, false, "rechts");
        check(r, 5, -1, false, "unten");
        check(r, 5, 6, false, "oben");
    }

    @Test
    void rechteck_rand_und_ecken() {
        var r = poly(0, 0, 10, 0, 10, 5, 0, 5);
        checkBoundary(r, 5, 0, "untere Kante");
        checkBoundary(r, 5, 5, "obere Kante");
        checkBoundary(r, 0, 2, "linke Kante");
        checkBoundary(r, 10, 2, "rechte Kante");
        checkBoundary(r, 0, 0, "Ecke unten links");
        checkBoundary(r, 10, 5, "Ecke oben rechts");
    }

    @Test
    void dreieck() {
        var t = poly(0, 0, 10, 0, 5, 8);
        check(t, 5, 3, true, "innen");
        check(t, 1, 5, false, "links oben ausserhalb");
        check(t, 9, 4, false, "rechts ausserhalb");
        check(t, 5, 9, false, "ueber der Spitze");
        check(t, 5, 8.5f, false, "knapp ueber der Spitze");
        checkBoundary(t, 5, 8, "Spitze");
    }

    // ------------------------------------------------------------------ Strahl durch Ecken (Skizze: P', P'')

    /** Kamm: zwei Zacken ragen von links in das Polygon (Spitzen bei (6,7) und (6,3)). */
    private static final Vector2[] KAMM = poly(0, 0, 10, 0, 10, 10, 0, 10, 0, 8, 6, 7, 0, 6, 0, 4, 6, 3, 0, 2);

    @Test
    void kamm_strahlDurchZackenspitze() {
        // Strahl nach rechts trifft die Zackenspitze: Kante "kreuzt", die andere beruehrt nur
        check(KAMM, 3, 7, false, "in der Zacke 1 (Strahl durch Spitze)");
        check(KAMM, 3, 3, false, "in der Zacke 2 (Strahl durch Spitze)");
        check(KAMM, 7, 7, true, "rechts der Spitze");
        check(KAMM, 7, 3, true, "rechts der Spitze 2");
        checkBoundary(KAMM, 6, 7, "Spitze 1");
        checkBoundary(KAMM, 6, 3, "Spitze 2");
    }

    @Test
    void kamm_zwischenDenZacken() {
        check(KAMM, 3, 5, true, "zwischen den Zacken");
        check(KAMM, 3, 9, true, "ueber Zacke 1");
        check(KAMM, 3, 1, true, "unter Zacke 2");
        check(KAMM, -1, 5, false, "links ausserhalb");
    }

    // ------------------------------------------------------------------ Kanten auf dem Strahl

    /** U-Form: Kerbe von oben zwischen x=3 und x=7 bis hinunter auf y=5. */
    private static final Vector2[] U = poly(0, 0, 10, 0, 10, 10, 7, 10, 7, 5, 3, 5, 3, 10, 0, 10);

    @Test
    void uForm_horizontaleKanteAufDemStrahl() {
        check(U, 1, 5, true, "links der Kerbe, Strahl laeuft ueber Kerbenboden");
        check(U, 8, 5, true, "rechts der Kerbe");
        check(U, -1, 5, false, "links ausserhalb");
        checkBoundary(U, 5, 5, "auf dem Kerbenboden");
        check(U, 5, 7, false, "in der Kerbe");
        check(U, 5, 3, true, "unter der Kerbe");
    }

    @Test
    void uForm_strahlAufHoeheDerOberkante() {
        check(U, 5, 10, false, "in der Oeffnung der Kerbe, Strahl laeuft durch (7,10)");
        check(U, 1, 10, false, "auf der Oberkante-Hoehe ausserhalb (Kante liegt auf dem Strahl)");
        checkBoundary(U, 1, 10, "auf der Oberkante links");
        checkBoundary(U, 9, 10, "auf der Oberkante rechts");
    }

    // ------------------------------------------------------------------ Degenerationen

    @Test
    void doppelteEcken_und_wiederholteStartecke() {
        var base = poly(0, 0, 10, 0, 10, 5, 0, 5);
        var dup = poly(0, 0, 0, 0, 10, 0, 10, 0, 10, 5, 0, 5, 0, 5, 0, 0);
        for (float[] pt : new float[][] { { 5, 2 }, { -1, 2 }, { 11, 2 }, { 5, 6 } }) {
            assertTrue(PunktInPolygon.contains(base, v(pt[0], pt[1]), false)
                    == PunktInPolygon.contains(dup, v(pt[0], pt[1]), false), "Duplikate aendern nichts");
        }
        assertTrue(PunktInPolygon.contains(dup, v(0, 0), true), "doppelte Ecke ist Rand");
        assertTrue(!PunktInPolygon.contains(dup, v(0, 0), false), "doppelte Ecke ist Rand (false)");
    }

    @Test
    void kollinearerZwischenpunkt() {
        // (5,0) liegt mitten auf der unteren Kante
        var r = poly(0, 0, 5, 0, 10, 0, 10, 5, 0, 5);
        check(r, 5, 2, true, "innen");
        check(r, 5, -1, false, "unten");
        checkBoundary(r, 5, 0, "Zwischenpunkt");
        checkBoundary(r, 7, 0, "Kante nach Zwischenpunkt");
    }

    @Test
    void zuWenigEcken() {
        assertTrue(!PunktInPolygon.contains(new Vector2[0], v(0, 0), true), "leer");
        assertTrue(!PunktInPolygon.contains(poly(1, 1), v(1, 1), true), "1 Ecke");
        assertTrue(!PunktInPolygon.contains(poly(0, 0, 4, 4), v(2, 2), true), "2 Ecken");
    }

    @Test
    void negativeKoordinaten() {
        var r = poly(-10, -10, -2, -10, -2, -4, -10, -4);
        check(r, -5, -7, true, "innen");
        check(r, -1, -7, false, "rechts ausserhalb");
        check(r, -5, -3, false, "oben ausserhalb");
    }

    // ------------------------------------------------------------------ Referenz-Vergleich auf dem Gitter

    /** Stern-foermiges Polygon um den Ursprung mit ganzzahligen Ecken (immer einfach, meist nicht-konvex). */
    private static int[][] zufallsPolygon(Random rnd, int range) {
        while (true) {
            int n = 3 + rnd.nextInt(10);
            Set<String> dirs = new HashSet<>();
            List<int[]> pts = new ArrayList<>();
            for (int i = 0; i < n * 3 && pts.size() < n; i++) {
                int x = rnd.nextInt(2 * range + 1) - range, y = rnd.nextInt(2 * range + 1) - range;
                if (x == 0 && y == 0)
                    continue;
                int g = gcd(Math.abs(x), Math.abs(y));
                if (!dirs.add(x / g + "," + y / g))
                    continue; // gleiche Richtung wie ein vorhandener Punkt
                pts.add(new int[] { x, y });
            }
            if (pts.size() < 3)
                continue;
            pts.sort(Comparator.comparingDouble(p -> Math.atan2(p[1], p[0])));
            boolean ok = true; // jede Luecke zwischen Nachbarn < 180 Grad -> Ursprung im Inneren, Polygon einfach
            for (int i = 0; i < pts.size() && ok; i++) {
                int[] p = pts.get(i), q = pts.get((i + 1) % pts.size());
                ok = (long) p[0] * q[1] - (long) p[1] * q[0] > 0;
            }
            if (ok)
                return pts.toArray(new int[0][]);
        }
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    private static boolean onBoundaryExact(int[][] poly, int px, int py) {
        for (int i = 0; i < poly.length; i++) {
            int[] a = poly[i], b = poly[(i + 1) % poly.length];
            long cr = (long) (b[0] - a[0]) * (py - a[1]) - (long) (b[1] - a[1]) * (px - a[0]);
            if (cr == 0 && px >= Math.min(a[0], b[0]) && px <= Math.max(a[0], b[0])
                    && py >= Math.min(a[1], b[1]) && py <= Math.max(a[1], b[1]))
                return true;
        }
        return false;
    }

    /** Referenz 2: Winding Number (Sunday), exakt mit long. */
    private static int windingNumber(int[][] poly, int px, int py) {
        int wn = 0;
        for (int i = 0; i < poly.length; i++) {
            int[] a = poly[i], b = poly[(i + 1) % poly.length];
            long cr = (long) (b[0] - a[0]) * (py - a[1]) - (long) (b[1] - a[1]) * (px - a[0]);
            if (a[1] <= py) {
                if (b[1] > py && cr > 0)
                    wn++;
            } else if (b[1] <= py && cr < 0) {
                wn--;
            }
        }
        return wn;
    }

    @ParameterizedTest(name = "Gitter-Vergleich, Skala {0}, Seed {1}")
    @CsvSource({ "1,1", "1,2", "1,3", "1024,4", "0.0009765625,5", "8,6" })
    void zufallspolygone_gegenReferenzen(float scale, long seed) {
        var rnd = new Random(seed);
        final int range = 8;
        int tested = 0, insideCount = 0, outsideCount = 0, boundaryCount = 0;
        for (int round = 0; round < 1500; round++) {
            int[][] base = zufallsPolygon(rnd, range);
            // zufaellig Duplikate einstreuen: darf nichts aendern
            List<Vector2> list = new ArrayList<>();
            for (int[] p : base) {
                list.add(v(p[0] * scale, p[1] * scale));
                if (rnd.nextInt(6) == 0)
                    list.add(v(p[0] * scale, p[1] * scale));
            }
            if (rnd.nextBoolean())
                Collections.reverse(list);
            var poly = list.toArray(new Vector2[0]);

            Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
            path.moveTo(base[0][0], base[0][1]);
            for (int i = 1; i < base.length; i++)
                path.lineTo(base[i][0], base[i][1]);
            path.closePath();

            for (int px = -range - 1; px <= range + 1; px++) {
                for (int py = -range - 1; py <= range + 1; py++) {
                    var q = v(px * scale, py * scale);
                    boolean boundary = onBoundaryExact(base, px, py);
                    String where = "polygon=" + java.util.Arrays.deepToString(base) + " p=(" + px + "," + py
                            + ") scale=" + scale;
                    tested++;
                    if (boundary) {
                        boundaryCount++;
                        assertTrue(PunktInPolygon.contains(poly, q, true), "Rand muesste true sein: " + where);
                        assertTrue(!PunktInPolygon.contains(poly, q, false), "Rand muesste false sein: " + where);
                    } else {
                        boolean expected = path.contains(px, py);
                        boolean wn = windingNumber(base, px, py) != 0;
                        assertTrue(expected == wn, "Referenzen uneinig (Testfehler): " + where);
                        boolean actual = PunktInPolygon.contains(poly, q, false);
                        assertTrue(actual == expected, "erwartet " + expected + " war " + actual + ": " + where);
                        if (expected)
                            insideCount++;
                        else
                            outsideCount++;
                    }
                }
            }
        }
        // Sicherstellen, dass der Test wirklich alle Klassen von Punkten abdeckt
        assertTrue(insideCount > 1000 && outsideCount > 1000 && boundaryCount > 1000,
                "zu wenig Abdeckung: " + insideCount + "/" + outsideCount + "/" + boundaryCount + " von " + tested);
    }
}
