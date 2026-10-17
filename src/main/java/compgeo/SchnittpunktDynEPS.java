package compgeo;

public class SchnittpunktDynEPS {
    /**
     * Relativer Rundungsfehler einer float-Operation (2^-24), mit Reserve verdoppelt.
     * Gilt fuer jedes Produkt und jede Subtraktion in den Kreuz-/Skalarprodukten.
     */
    private static final float RND = 0x1p-23f;

    /**
     * Unsicherheit der Eingabekoordinaten in ulp. Die Punkte sind meist selbst Ergebnisse von
     * float-Rechnungen: Ein auf float gerundeter Wert weicht um hoechstens 0.5 ulp von seinem "gemeinten"
     * reellen Wert ab.
     */
    private static final float INPUT_ULPS = 0.5f;

    /**
     * Differenzvektor samt Fehlerschranke pro Komponente (nx, ny).
     * Die Schranke ist die Summe der Unsicherheiten beider Punkte (je INPUT_ulps * ulp der Koordinate)
     * plus der Rundung der Subtraktion selbst. Sie haengt damit von der Groesse der Koordinaten ab,
     * und zwar pro Komponente: Eine Koordinate nahe 0 traegt (fast) keine Unsicherheit bei.
     */
    private record Diff(float x, float y, float nx, float ny) {
        static Diff of(Vector2 to, Vector2 from) {
            float x = to.x() - from.x();
            float y = to.y() - from.y();
            return new Diff(x, y,
                    INPUT_ULPS * (Math.ulp(to.x()) + Math.ulp(from.x())) + 0.5f * Math.ulp(x),
                    INPUT_ULPS * (Math.ulp(to.y()) + Math.ulp(from.y())) + 0.5f * Math.ulp(y));
        }

        boolean isZero() {
            return x == 0 && y == 0;
        }

        float lengthSquared() {
            return x * x + y * y;
        }
    }

    private static float cross(Diff u, Diff v) {
        return u.x * v.y - u.y * v.x;
    }

    /** Fehlerschranke von cross(u, v): Fortpflanzung der Eingabeunsicherheit + Rundung der Produkte. */
    private static float crossErr(Diff u, Diff v) {
        return Math.abs(u.nx * v.y) + Math.abs(u.ny * v.x)
                + Math.abs(u.x * v.ny) + Math.abs(u.y * v.nx)
                + RND * (Math.abs(u.x * v.y) + Math.abs(u.y * v.x));
    }

    private static float dot(Diff u, Diff v) {
        return u.x * v.x + u.y * v.y;
    }

    /** Fehlerschranke von dot(u, v). */
    private static float dotErr(Diff u, Diff v) {
        return Math.abs(u.nx * v.x) + Math.abs(u.x * v.nx)
                + Math.abs(u.ny * v.y) + Math.abs(u.y * v.ny)
                + RND * (Math.abs(u.x * v.x) + Math.abs(u.y * v.y));
    }

    /**
     * Schnittpunkt der Strecken AB und CD.
     *
     * @return der Schnittpunkt, oder null falls
     *         - sich die Strecken nicht schneiden,
     *         - die Strecken parallel (nicht kollinear) sind,
     *         - die Strecken kollinear sind und sich ueberlappen (unendlich viele Punkte)
     *           oder getrennt sind,
     *         - eine der Strecken die Laenge 0 hat.
     *         Beruehren sich kollineare Strecken in genau einem Punkt, wird dieser Punkt zurueckgegeben.
     *         Alle Entscheidungen beruecksichtigen die Genauigkeit von float bei den gegebenen Koordinaten
     *         (siehe {@link #INPUT_ULPS}, {@link #RND}): Abweichungen im Bereich des Rundungsrauschens
     *         zaehlen als Beruehrung bzw. als parallel/kollinear.
     */
    public static Vector2 intersection(Vector2 a, Vector2 b, Vector2 c, Vector2 d) {
        var ab = Diff.of(b, a);
        var cd = Diff.of(d, c);
        if (ab.isZero() || cd.isZero())
            return null; // degenerierte Strecke

        var ac = Diff.of(c, a);

        float denom = cross(ab, cd);
        float denomErr = crossErr(ab, cd);
        float absDenom = Math.abs(denom);

        // Vorzeichen von denom nicht mehr vom Rauschen unterscheidbar -> parallel
        if (absDenom <= denomErr)
            return collinearTouchPoint(a, b, c, d);

        float t = cross(ac, cd) / denom; // Position auf AB
        float u = cross(ac, ab) / denom; // Position auf CD

        // Toleranz von t und u: Fehler des Zaehlers plus Fehler des Nenners, beides geteilt durch |denom|
        float tTol = (crossErr(ac, cd) + Math.abs(t) * denomErr) / absDenom;
        float uTol = (crossErr(ac, ab) + Math.abs(u) * denomErr) / absDenom;

        if (t < -tTol || t > 1 + tTol || u < -uTol || u > 1 + uTol)
            return null; // Schnittpunkt liegt ausserhalb mindestens einer Strecke

        // t auf [0, 1] klemmen: der Punkt liegt dann auf AB, Endpunkte werden exakt zurueckgegeben
        return Vector2.lerp(a, b, Math.min(1f, Math.max(0f, t)));
    }

    /**
     * Parallele Strecken: Nur wenn sie kollinear sind und sich in genau einem Endpunkt beruehren,
     * gibt es genau einen Schnittpunkt. Als Referenz dient die laengere Strecke (genauere Richtung).
     */
    private static Vector2 collinearTouchPoint(Vector2 a, Vector2 b, Vector2 c, Vector2 d) {
        var ab = Diff.of(b, a);
        var cd = Diff.of(d, c);
        if (cd.lengthSquared() > ab.lengthSquared())
            return collinearTouchPoint(c, d, a, b); // Rollen tauschen, danach ist AB die laengere

        var ac = Diff.of(c, a);
        var ad = Diff.of(d, a);

        // C und D muessen (im Rahmen der Fehlerschranke) auf der Geraden AB liegen
        if (Math.abs(cross(ac, ab)) > crossErr(ac, ab) || Math.abs(cross(ad, ab)) > crossErr(ad, ab))
            return null; // parallel, aber versetzt

        // Position von C und D auf AB (A = 0, B = 1) samt Toleranz
        float lenSq = ab.lengthSquared();
        float lenSqErr = dotErr(ab, ab);
        float tC = dot(ac, ab) / lenSq;
        float tD = dot(ad, ab) / lenSq;
        float tolC = (dotErr(ac, ab) + Math.abs(tC) * lenSqErr) / lenSq;
        float tolD = (dotErr(ad, ab) + Math.abs(tD) * lenSqErr) / lenSq;

        boolean cIsMax = tC >= tD;
        float tMax = cIsMax ? tC : tD;
        float tolMax = cIsMax ? tolC : tolD;
        float tMin = cIsMax ? tD : tC;
        float tolMin = cIsMax ? tolD : tolC;

        if (Math.abs(tMax) <= tolMax)
            return a; // CD endet in A, liegt sonst "vor" AB
        if (Math.abs(tMin - 1) <= tolMin)
            return b; // CD beginnt in B, liegt sonst "hinter" AB

        return null; // getrennt oder ueberlappend
    }
}
