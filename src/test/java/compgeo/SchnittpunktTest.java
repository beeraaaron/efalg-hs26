package compgeo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests fuer {@link Schnittpunkt#intersection(Vector2, Vector2, Vector2, Vector2)}.
 *
 * Konventionen / dokumentiertes Verhalten:
 *  - Kein Schnittpunkt -> null.
 *  - Parallele Strecken -> null.
 *  - Kollineare Strecken: genau ein gemeinsamer Punkt (Beruehrung) -> dieser Punkt;
 *    Ueberlappung (unendlich viele Punkte) oder getrennt -> null.
 *  - Degenerierte Strecken (Laenge 0, A == B oder C == D) -> immer null.
 *  - Beruehrung an einem Endpunkt zaehlt als Schnitt.
 *
 * Fast alle "Kreuzung / Endpunkt / ausserhalb"-Tests pruefen automatisch alle 8 Varianten
 * (A/B vertauscht, C/D vertauscht, Strecken vertauscht): das Ergebnis darf nicht von
 * der Reihenfolge der Argumente abhaengen.
 */
class SchnittpunktTest {

    // ------------------------------------------------------------------ Hilfsmittel

    static Stream<Float> decimalScales() {
        return Stream.of(1e-6f, 1e-3f, 0.1f, 1f, 3.7f, 1e3f, 1e6f, 1e9f);
    }

    /** Zweierpotenzen: Skalierung ist in float exakt, also keine zusaetzlichen Rundungsfehler. */
    static Stream<Float> powerOfTwoScales() {
        return Stream.of(0x1p-20f, 0x1p-10f, 1f, 0x1p10f, 0x1p20f, 0x1p30f);
    }

    private static Vector2 v(float x, float y) {
        return new Vector2(x, y);
    }

    private static Vector2 v(float x, float y, float scale) {
        return new Vector2(x * scale, y * scale);
    }

    private static Vector2 call(int variant, Vector2 a, Vector2 b, Vector2 c, Vector2 d) {
        var p = (variant & 1) == 0 ? a : b;
        var q = (variant & 1) == 0 ? b : a;
        var r = (variant & 2) == 0 ? c : d;
        var w = (variant & 2) == 0 ? d : c;
        return (variant & 4) == 0
                ? Schnittpunkt.intersectionKollinear(p, q, r, w)
                : Schnittpunkt.intersectionKollinear(r, w, p, q);
    }

    private static String msg(int variant, Vector2 a, Vector2 b, Vector2 c, Vector2 d) {
        return String.format("Variante %d: a=%s b=%s c=%s d=%s", variant, a, b, c, d);
    }

    private static void assertPoint(Vector2 expected, Vector2 actual, float tol, String message) {
        assertNotNull(actual, message + " -> erwartet wurde ein Schnittpunkt, war aber null");
        assertEquals(expected.x(), actual.x(), tol, message + " (x)");
        assertEquals(expected.y(), actual.y(), tol, message + " (y)");
    }

    /** Erwartet den Schnittpunkt in allen 8 Argument-Varianten. */
    private static void assertIntersectsAt(Vector2 expected, float tol,
                                           Vector2 a, Vector2 b, Vector2 c, Vector2 d) {
        for (int variant = 0; variant < 8; variant++) {
            assertPoint(expected, call(variant, a, b, c, d), tol, msg(variant, a, b, c, d));
        }
    }

    /** Erwartet "kein Schnittpunkt" in allen 8 Argument-Varianten. */
    private static void assertNoIntersection(Vector2 a, Vector2 b, Vector2 c, Vector2 d) {
        for (int variant = 0; variant < 8; variant++) {
            var result = call(variant, a, b, c, d);
            assertNull(result, msg(variant, a, b, c, d) + " -> erwartet null, war aber " + result);
        }
    }

    @Test
    void simpleSchnittpunktTest() {
        var a = new Vector2(-2f, -2f);
        var b = new Vector2(12f, 2f);
        var c = new Vector2(-1f, 7f);
        var d = new Vector2(11f, -1f);
        assertIntersectsAt(new Vector2(8.1f, 0.9f), 0.1f, a, b, c, d);
    }

    // ------------------------------------------------------------------ 1. Kreuzung

    @ParameterizedTest(name = "rechtwinklige Kreuzung, Skala {0}")
    @MethodSource("decimalScales")
    void kreuzung_rechtwinklig(float s) {
        // Asymmetrisch (t = 0.25, u = 0.75), damit Verwechslungen von t/u auffallen.
        assertIntersectsAt(v(1, 0, s), 1e-4f * s,
                v(0, 0, s), v(4, 0, s), v(1, -3, s), v(1, 1, s));
    }

    @ParameterizedTest(name = "schiefwinklige Kreuzung, Skala {0}")
    @MethodSource("decimalScales")
    void kreuzung_schiefwinklig(float s) {
        assertIntersectsAt(v(2, 1, s), 1e-4f * s,
                v(0, 0, s), v(4, 2, s), v(0, 2, s), v(4, 0, s));
    }

    @ParameterizedTest(name = "Kreuzung mit negativen Koordinaten, Skala {0}")
    @MethodSource("decimalScales")
    void kreuzung_negativeKoordinaten(float s) {
        assertIntersectsAt(v(-1, 1, s), 1e-4f * s,
                v(-3, -1, s), v(1, 3, s), v(-4, 4, s), v(2, -2, s));
    }

    @Test
    void kreuzung_imUrsprung() {
        assertIntersectsAt(v(0, 0), 1e-6f,
                v(-2, -2), v(2, 2), v(-2, 2), v(2, -2));
    }

    @Test
    void kreuzung_dezimalwerteOhneExakteDarstellung() {
        // 0.1, 0.2, 0.7 ... sind in float nicht exakt darstellbar.
        var p = v(0.1f, 0.2f);
        var d1 = v(0.7f, 0.3f);
        var d2 = v(-0.2f, 0.9f);
        assertIntersectsAt(p, 1e-5f,
                p.subtract(d1), p.add(d1), p.subtract(d2), p.add(d2));
    }

    @Test
    void kreuzung_sehrNaheAnEinemEndpunkt_aberInnerhalb() {
        // t = 0.99
        assertIntersectsAt(v(9.9f, 0), 1e-4f,
                v(0, 0), v(10, 0), v(9.9f, -1), v(9.9f, 1));
    }

    @Test
    void kreuzung_spitzerWinkel() {
        // Winkel ca. 0.11 Grad, aber deutlich ueber der Parallel-Schwelle.
        assertIntersectsAt(v(5, 0), 1e-3f,
                v(0, 0), v(10, 0), v(0, -0.01f), v(10, 0.01f));
    }

    @Test
    void kreuzung_sehrKurzeStreckeKreuztSehrLange() {
        assertIntersectsAt(v(3, 0), 1e-2f,
                v(-1000, 0), v(1000, 0), v(3, -0.001f), v(3, 0.001f));
    }

    @Test
    void kreuzung_langeDuenneStrecke() {
        assertIntersectsAt(v(5e5f, 0), 0.5f,
                v(0, 0), v(1e6f, 0), v(5e5f, -1e-3f), v(5e5f, 1e-3f));
    }

    // ------------------------------------------------------------------ 2. Beruehrung am Endpunkt

    @Test
    void endpunkt_endpunktTrifftEndpunkt_V_Form() {
        assertIntersectsAt(v(1, 1), 1e-6f,
                v(0, 0), v(1, 1), v(1, 1), v(2, 0));
    }

    @Test
    void endpunkt_endpunktTrifftEndpunkt_stumpferWinkel() {
        assertIntersectsAt(v(3, 0), 1e-6f,
                v(0, 0), v(3, 0), v(3, 0), v(5, 2));
    }

    @Test
    void endpunkt_gemeinsameEcke_rechterWinkel() {
        assertIntersectsAt(v(0, 0), 1e-6f,
                v(0, 0), v(5, 0), v(0, 0), v(0, 5));
    }

    @Test
    void endpunkt_T_Form_waagrecht() {
        // Endpunkt von CD liegt in der Mitte von AB.
        assertIntersectsAt(v(2, 0), 1e-6f,
                v(0, 0), v(4, 0), v(2, 0), v(2, 3));
    }

    @Test
    void endpunkt_T_Form_diagonal() {
        assertIntersectsAt(v(2, 2), 1e-6f,
                v(0, 0), v(4, 4), v(2, 2), v(5, 0));
    }

    @ParameterizedTest(name = "Endpunkt-Beruehrung, Zweierpotenz-Skala {0}")
    @MethodSource("powerOfTwoScales")
    void endpunkt_beruehrung_exakteSkalen(float s) {
        assertIntersectsAt(v(1, 1, s), 1e-6f * s,
                v(0, 0, s), v(1, 1, s), v(1, 1, s), v(2, 0, s));
        assertIntersectsAt(v(2, 0, s), 1e-6f * s,
                v(0, 0, s), v(4, 0, s), v(2, 0, s), v(2, 3, s));
    }

    @ParameterizedTest(name = "Endpunkt-Beruehrung (B == C), Skala {0}")
    @MethodSource("decimalScales")
    void endpunkt_beruehrung_beliebigeSkalen(float s) {
        // Hier ist c bitgleich b, also ist t exakt 1 und u exakt 0.
        var a = v(0, 0, s);
        var b = v(1, 1, s);
        var d = v(2, 0, s);
        assertPoint(b, Schnittpunkt.intersectionKollinear(a, b, b, d), 1e-5f * s, "B == C");
    }

    @Test
    void endpunkt_beruehrung_krummeDezimalwerte() {
        var a = v(0.1f, 0.7f);
        var b = v(0.9f, 0.3f);
        var d = v(0.2f, 0.1f);
        assertPoint(b, Schnittpunkt.intersectionKollinear(a, b, b, d), 1e-6f, "B == C");

        // Gemeinsamer Startpunkt (A == C)
        var d2 = v(0.5f, 0.9f);
        assertPoint(a, Schnittpunkt.intersectionKollinear(a, b, a, d2), 1e-6f, "A == C");
    }

    @Test
    void endpunkt_rundungsfehlerBeiEndpunktDarfNichtZuNullFuehren() {
        // B liegt (bis auf Rundung) genau auf der Mitte von CD, t ~ 1 +- Rundungsfehler.
        var a = v(0.1f, 0.1f);
        var b = v(0.7f, 0.3f);
        var w = v(0.3f, -0.7f);
        var c = b.subtract(w);
        var d = b.add(w);
        assertPoint(b, Schnittpunkt.intersectionKollinear(a, b, c, d), 1e-5f, "Endpunkt mit Rundung");
    }

    @ParameterizedTest(name = "Endpunkt knapp getroffen / knapp verfehlt, Skala {0}")
    @MethodSource("decimalScales")
    void endpunkt_knappGetroffenUndKnappVerfehlt(float s) {
        var a = v(0, 0, s);
        var b = v(1, 0, s);

        // 1e-4 vor dem Endpunkt: Treffer
        var cInside = v(0.9999f, -1, s);
        var dInside = v(0.9999f, 1, s);
        assertIntersectsAt(v(0.9999f, 0, s), 5e-6f * s, a, b, cInside, dInside);

        // 1e-4 hinter dem Endpunkt: kein Treffer
        assertNoIntersection(a, b, v(1.0001f, -1, s), v(1.0001f, 1, s));

        // 1e-4 vor dem Startpunkt
        assertNoIntersection(a, b, v(-0.0001f, -1, s), v(-0.0001f, 1, s));
    }

    // ------------------------------------------------------------------ 3. Ausserhalb

    @ParameterizedTest(name = "Geraden schneiden sich, Strecken nicht, Skala {0}")
    @MethodSource("decimalScales")
    void ausserhalb_verlaengerungSchneidet(float s) {
        // Schnittpunkt hinter B (t > 1)
        assertNoIntersection(v(0, 0, s), v(1, 0, s), v(2, -1, s), v(2, 1, s));
        // Schnittpunkt vor A (t < 0)
        assertNoIntersection(v(0, 0, s), v(1, 0, s), v(-1, -1, s), v(-1, 1, s));
        // Schnittpunkt hinter D (u > 1)
        assertNoIntersection(v(0, 0, s), v(2, 0, s), v(1, -3, s), v(1, -1, s));
        // Schnittpunkt vor C (u < 0)
        assertNoIntersection(v(0, 0, s), v(2, 0, s), v(1, 1, s), v(1, 3, s));
        // Schnittpunkt ausserhalb beider Strecken
        assertNoIntersection(v(0, 0, s), v(1, 1, s), v(3, 0, s), v(2, 1, s));
    }

    @ParameterizedTest(name = "weit auseinander liegende Strecken, Skala {0}")
    @MethodSource("decimalScales")
    void ausserhalb_weitEntfernt(float s) {
        assertNoIntersection(v(0, 0, s), v(1, 1, s), v(100, 100, s), v(101, 99, s));
        assertNoIntersection(v(0, 0, s), v(1, 0, s), v(50, -1, s), v(50, 1, s));
    }

    @Test
    void ausserhalb_LForm_mitLuecke() {
        assertNoIntersection(v(0, 0), v(1, 0), v(2, -1), v(2, 1));
    }

    // ------------------------------------------------------------------ 4. Parallel

    @ParameterizedTest(name = "parallele Strecken, Skala {0}")
    @MethodSource("decimalScales")
    void parallel_verschiedeneRichtungen(float s) {
        // waagrecht
        assertNoIntersection(v(0, 0, s), v(2, 0, s), v(0, 1, s), v(3, 1, s));
        // senkrecht
        assertNoIntersection(v(0, 0, s), v(0, 2, s), v(1, 0, s), v(1, 3, s));
        // diagonal
        assertNoIntersection(v(0, 0, s), v(1, 1, s), v(0, 1, s), v(1, 2, s));
        // steil
        assertNoIntersection(v(0, 0, s), v(1, 2, s), v(1, 0, s), v(2, 2, s));
    }

    @Test
    void parallel_nahBeieinander() {
        assertNoIntersection(v(0, 0), v(1000, 0), v(0, 1e-3f), v(1000, 1e-3f));
    }

    @Test
    void parallel_knappUnterDerSchwelle_giltAlsParallel() {
        // sin(Winkel) ~ 1e-7 < 1e-6: per Definition "parallel", auch wenn sie sich in A == C beruehren.
        assertNoIntersection(v(0, 0), v(1000, 0), v(0, 0), v(1000, 1e-4f));
    }

    @Test
    void parallel_knappUeberDerSchwelle_wirdErkannt() {
        // sin(Winkel) ~ 1e-4 > 1e-6, gemeinsamer Startpunkt
        assertIntersectsAt(v(0, 0), 1e-6f,
                v(0, 0), v(1000, 0), v(0, 0), v(1000, 0.1f));
    }

    // ------------------------------------------------------------------ 5. Kollinear

    @ParameterizedTest(name = "kollinear (Richtung 1,2), Skala {0}")
    @MethodSource("decimalScales")
    void kollinear_diagonal(float s) {
        // Punkte k*(1,2)*s liegen auch nach Rundung exakt auf einer Geraden.
        var p0 = v(0, 0, s);
        var p1 = v(1, 2, s);
        var p2 = v(2, 4, s);
        var p3 = v(3, 6, s);

        assertNoIntersection(p0, p2, p1, p3); // ueberlappend
        assertNoIntersection(p0, p1, p2, p3); // getrennt
        assertIntersectsAt(p1, 1e-5f * s, p0, p1, p1, p2); // beruehren sich genau in p1
        assertNoIntersection(p0, p3, p1, p2); // eine liegt in der anderen
        assertNoIntersection(p0, p2, p0, p2); // identisch
    }

    @ParameterizedTest(name = "kollinear (waagrecht), Skala {0}")
    @MethodSource("decimalScales")
    void kollinear_waagrecht(float s) {
        assertNoIntersection(v(0, 5, s), v(2, 5, s), v(1, 5, s), v(3, 5, s));
        assertNoIntersection(v(0, 5, s), v(1, 5, s), v(2, 5, s), v(3, 5, s));
        assertIntersectsAt(v(1, 5, s), 1e-5f * s, v(0, 5, s), v(1, 5, s), v(1, 5, s), v(2, 5, s));
    }

    @Test
    void kollinear_senkrecht() {
        assertNoIntersection(v(1, 0), v(1, 2), v(1, 1), v(1, 3));
        assertNoIntersection(v(1, 0), v(1, 1), v(1, 2), v(1, 3));
        assertIntersectsAt(v(1, 1), 1e-6f, v(1, 0), v(1, 1), v(1, 1), v(1, 2));
    }

    @Test
    void kollinear_grosseOffsets() {
        assertNoIntersection(v(1e6f, 1e6f), v(1e6f + 2, 1e6f + 2),
                v(1e6f + 1, 1e6f + 1), v(1e6f + 3, 1e6f + 3));
        // Beruehrung in genau einem Punkt
        assertIntersectsAt(v(1e6f + 2, 1e6f + 2), 1e-3f,
                v(1e6f, 1e6f), v(1e6f + 2, 1e6f + 2),
                v(1e6f + 2, 1e6f + 2), v(1e6f + 4, 1e6f + 4));
    }

    @Test
    void kollinear_beruehrungInGenauEinemPunkt() {
        // Ende an Anfang (B == C), schiefe Richtung
        assertIntersectsAt(v(2, 1), 1e-6f, v(0, 0), v(2, 1), v(2, 1), v(6, 3));
        // Kopf an Kopf (B == D), verschiedene Laengen
        assertIntersectsAt(v(4, 0), 1e-6f, v(0, 0), v(4, 0), v(10, 0), v(4, 0));
        // Fuss an Fuss (A == C)
        assertIntersectsAt(v(0, 0), 1e-6f, v(0, 0), v(-3, 0), v(0, 0), v(7, 0));
        // negative Richtung, Diagonale
        assertIntersectsAt(v(-1, -1), 1e-6f, v(-3, -3), v(-1, -1), v(-1, -1), v(0, 0));
        // sehr kurze Strecke beruehrt lange Strecke am Ende
        assertIntersectsAt(v(1000, 0), 1e-3f, v(0, 0), v(1000, 0), v(1000, 0), v(1000.5f, 0));
    }

    @ParameterizedTest(name = "kollinear: knapp getrennt / knapp ueberlappend, Skala {0}")
    @MethodSource("decimalScales")
    void kollinear_knappGetrenntOderUeberlappend(float s) {
        // Luecke von 1e-4 (relativ zur Laenge von AB) -> null
        assertNoIntersection(v(0, 0, s), v(1, 0, s), v(1.0001f, 0, s), v(2, 0, s));
        // Ueberlappung von 1e-4 -> unendlich viele Punkte -> null
        assertNoIntersection(v(0, 0, s), v(1, 0, s), v(0.9999f, 0, s), v(2, 0, s));
    }

    @Test
    void kollinear_parallelVersetzt_beruehrtNicht() {
        assertNoIntersection(v(0, 0), v(1, 0), v(1, 0.01f), v(2, 0.01f));
    }

    // ------------------------------------------------------------------ 6. Degenerierte Strecken

    @Test
    void degeneriert_punktAufAndererStrecke() {
        // Fuehrt bei fehlender Pruefung auf denom == 0 zu 0/0 = NaN.
        assertNoIntersection(v(1, 0), v(1, 0), v(1, -1), v(1, 1));
        assertNoIntersection(v(0, 0), v(2, 0), v(1, 0), v(1, 0));
    }

    @Test
    void degeneriert_punktNebenAndererStrecke() {
        assertNoIntersection(v(5, 5), v(5, 5), v(0, 0), v(1, 0));
        assertNoIntersection(v(0, 0), v(1, 0), v(5, 5), v(5, 5));
    }

    @Test
    void degeneriert_beideStreckenSindPunkte() {
        assertNoIntersection(v(1, 1), v(1, 1), v(1, 1), v(1, 1));
        assertNoIntersection(v(1, 1), v(1, 1), v(2, 2), v(2, 2));
    }

    @Test
    void degeneriert_nullvektoren() {
        assertNoIntersection(v(0, 0), v(0, 0), v(0, 0), v(0, 0));
    }

    // ------------------------------------------------------------------ 7. Grosse Offsets / Rundung

    @ParameterizedTest(name = "grosser Offset {0}")
    @CsvSource({"0", "1", "1000", "100000", "1000000", "10000000", "-1000000"})
    void offset_kreuzungWeitWegVomUrsprung(float o) {
        // Alle Werte sind ganzzahlig und < 2^24, also exakt in float darstellbar.
        // Die Differenzen (ab, cd, ac) sind klein und exakt -> der Offset darf nichts verfaelschen.
        assertIntersectsAt(v(o + 1, o), 1e-3f,
                v(o - 2, o), v(o + 2, o), v(o + 1, o - 3), v(o + 1, o + 1));
    }

    // ------------------------------------------------------------------ 8. Gitter-Test gegen exakte Referenz

    /**
     * Alle 65'536 Kombinationen von vier Punkten auf einem 4x4-Gitter, gegen eine exakte
     * Referenz mit ganzen Zahlen (long) verglichen. Deckt Kreuzung, Beruehrung, T-Stuecke,
     * parallel, kollinear und degenerierte Faelle automatisch ab.
     * (offset, step) sind so gewaehlt, dass alle Koordinaten exakt in float darstellbar sind.
     */
    @ParameterizedTest(name = "Gitter: offset={0}, step={1}")
    @CsvSource({"0,1", "4096,0.25", "-100000,8", "1000000,1", "0,0.0009765625"})
    void gitter_vergleichMitExakterReferenz(float offset, float step) {
        final int n = 4;
        final int pts = n * n;
        final float tol = 2 * Math.ulp(offset) + 1e-5f * step;

        for (int code = 0; code < pts * pts * pts * pts; code++) {
            int ia = code % pts;
            int ib = (code / pts) % pts;
            int ic = (code / (pts * pts)) % pts;
            int id = code / (pts * pts * pts);

            long ax = ia % n, ay = ia / n;
            long bx = ib % n, by = ib / n;
            long cx = ic % n, cy = ic / n;
            long dx = id % n, dy = id / n;

            Vector2 expected = referenz(ax, ay, bx, by, cx, cy, dx, dy, offset, step);

            var a = v(offset + step * ax, offset + step * ay);
            var b = v(offset + step * bx, offset + step * by);
            var c = v(offset + step * cx, offset + step * cy);
            var d = v(offset + step * dx, offset + step * dy);

            String where = String.format("a=(%d,%d) b=(%d,%d) c=(%d,%d) d=(%d,%d) offset=%s step=%s",
                    ax, ay, bx, by, cx, cy, dx, dy, offset, step);

            var actual = Schnittpunkt.intersectionKollinear(a, b, c, d);

            if (expected == null) {
                assertNull(actual, "erwartet null, war aber " + actual + " bei " + where);
            } else {
                assertNotNull(actual, "erwartet " + expected + ", war aber null bei " + where);
                assertEquals(expected.x(), actual.x(), tol, "x bei " + where);
                assertEquals(expected.y(), actual.y(), tol, "y bei " + where);
            }
        }
    }

    /** Exakte Referenz mit ganzzahliger Arithmetik; null bei parallel/kollinear/degeneriert. */
    private static Vector2 referenz(long ax, long ay, long bx, long by,
                                    long cx, long cy, long dx, long dy,
                                    float offset, float step) {
        long abx = bx - ax, aby = by - ay;
        long cdx = dx - cx, cdy = dy - cy;
        long acx = cx - ax, acy = cy - ay;

        boolean degeneriert = (abx == 0 && aby == 0) || (cdx == 0 && cdy == 0);
        long den = abx * cdy - aby * cdx;
        if (degeneriert)
            return null;
        if (den == 0) {
            // parallel: nur bei Kollinearitaet und Beruehrung in genau einem Endpunkt gibt es einen Punkt
            if (acx * aby - acy * abx != 0)
                return null;
            long l = abx * abx + aby * aby;
            long tc = acx * abx + acy * aby;
            long td = (acx + cdx) * abx + (acy + cdy) * aby;
            long lo = Math.min(tc, td);
            long hi = Math.max(tc, td);
            if (hi == 0)
                return new Vector2((float) (offset + step * ax), (float) (offset + step * ay));
            if (lo == l)
                return new Vector2((float) (offset + step * bx), (float) (offset + step * by));
            return null;
        }

        long tn = acx * cdy - acy * cdx;
        long un = acx * aby - acy * abx;
        if (den < 0) {
            den = -den;
            tn = -tn;
            un = -un;
        }
        if (tn < 0 || tn > den || un < 0 || un > den)
            return null;

        double t = (double) tn / den;
        return new Vector2(
                (float) (offset + step * (ax + abx * t)),
                (float) (offset + step * (ay + aby * t)));
    }

    // ------------------------------------------------------------------ 9. Zufallstests (fester Seed)

    @Test
    @DisplayName("Zufall: P liegt in beiden Strecken -> P wird gefunden")
    void zufall_schnittpunktInBeidenStrecken() {
        var rnd = new Random(42);
        for (int i = 0; i < 2000; i++) {
            var p = v((rnd.nextFloat() * 2 - 1) * 1000, (rnd.nextFloat() * 2 - 1) * 1000);
            var dirs = zufallsRichtungen(rnd);
            var d1 = dirs[0];
            var d2 = dirs[1];

            var a = p.subtract(d1.multiply(1 + rnd.nextFloat() * 49));
            var b = p.add(d1.multiply(1 + rnd.nextFloat() * 49));
            var c = p.subtract(d2.multiply(1 + rnd.nextFloat() * 49));
            var d = p.add(d2.multiply(1 + rnd.nextFloat() * 49));

            assertIntersectsAt(p, 1e-2f, a, b, c, d);
        }
    }

    @Test
    @DisplayName("Zufall: P liegt ausserhalb mindestens einer Strecke -> null")
    void zufall_schnittpunktAusserhalb() {
        var rnd = new Random(1337);
        for (int i = 0; i < 2000; i++) {
            var p = v((rnd.nextFloat() * 2 - 1) * 1000, (rnd.nextFloat() * 2 - 1) * 1000);
            var dirs = zufallsRichtungen(rnd);
            var d1 = dirs[0];
            var d2 = dirs[1];

            // AB beginnt erst im Abstand a1 von P (gleiche Seite) -> P ausserhalb von AB
            float side = rnd.nextBoolean() ? 1 : -1;
            float a1 = 1 + rnd.nextFloat() * 49;
            float len = 1 + rnd.nextFloat() * 49;
            var a = p.add(d1.multiply(side * a1));
            var b = p.add(d1.multiply(side * (a1 + len)));

            // CD enthaelt P
            var c = p.subtract(d2.multiply(1 + rnd.nextFloat() * 49));
            var d = p.add(d2.multiply(1 + rnd.nextFloat() * 49));

            assertNoIntersection(a, b, c, d);
        }
    }

    /** Zwei Einheitsrichtungen mit einem Winkel zwischen 30 und 150 Grad (|sin| >= 0.5). */
    private static Vector2[] zufallsRichtungen(Random rnd) {
        double th1 = rnd.nextDouble() * 2 * Math.PI;
        double delta = Math.PI / 6 + rnd.nextDouble() * (2 * Math.PI / 3);
        double th2 = th1 + (rnd.nextBoolean() ? delta : -delta);
        return new Vector2[] {
                v((float) Math.cos(th1), (float) Math.sin(th1)),
                v((float) Math.cos(th2), (float) Math.sin(th2))
        };
    }
}