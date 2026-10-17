package compgeo;

public class Schnittpunkt {
    private static final float EPS = 1e-6f; // 10^-6 = 0.000001

    public static Vector2 intersection(Vector2 a, Vector2 b, Vector2 c, Vector2 d) {
        var ab = Vector2.subtract(b, a);
        var cd = Vector2.subtract(d, c);
        var ac = Vector2.subtract(c, a);

        float denominant = cross(ab, cd);
        if (Math.abs(denominant) <= EPS * ab.length() * cd.length())
            return null; // parallel oder kollinear

        float lambdaAB = cross(ac, cd) / denominant; // Position auf AB
        float lambdaCD = cross(ac, ab) / denominant; // Position auf CD

        if (lambdaAB < -EPS || lambdaAB > 1 + EPS || lambdaCD < -EPS || lambdaCD > 1 + EPS)
            return null; // Schnittpunkt liegt ausserhalb mindestens einer Strecke

        return a.add(ab.multiply(lambdaAB));
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
     */
    public static Vector2 intersectionKollinear(Vector2 a, Vector2 b, Vector2 c, Vector2 d) {
        var ab = Vector2.subtract(b, a);
        var cd = Vector2.subtract(d, c);
        var ac = Vector2.subtract(c, a);

        float lenAb = ab.length();
        float lenCd = cd.length();

        float denominante = cross(ab, cd);
        if (Math.abs(denominante) <= EPS * lenAb * lenCd)
            return collinearTouchPoint(a, b, c, d, ab, ac, lenAb, lenCd);

        float lambdaAB = cross(ac, cd) / denominante; // Position auf AB
        float lambdaCD = cross(ac, ab) / denominante; // Position auf CD

        if (lambdaAB < -EPS || lambdaAB > 1 + EPS || lambdaCD < -EPS || lambdaCD > 1 + EPS)
            return null; // Schnittpunkt liegt ausserhalb mindestens einer Strecke

        return a.add(ab.multiply(lambdaAB));
        //return c.add(cd.multiply(lambdaCD));
    }

    /**
     * Parallele Strecken: Nur wenn sie kollinear sind und sich in genau einem Endpunkt beruehren,
     * gibt es genau einen Schnittpunkt.
     */
    private static Vector2 collinearTouchPoint(Vector2 a, Vector2 b, Vector2 c, Vector2 d,
                                               Vector2 ab, Vector2 ac, float lenAb, float lenCd) {
        if (lenAb == 0 || lenCd == 0)
            return null; // degenerierte Strecke

        // C (und wegen parallel auch D) muss auf der Geraden AB liegen
        if (Math.abs(cross(ac, ab)) > EPS * lenAb * Math.max(lenAb, ac.length()))
            return null; // echt parallel, aber versetzt

        // Position von C und D auf AB (A = 0, B = 1)
        float lenSq = ab.lengthSquared();
        float tC = Vector2.dot(ac, ab) / lenSq;
        float tD = Vector2.dot(Vector2.subtract(d, a), ab) / lenSq;
        float tMin = Math.min(tC, tD);
        float tMax = Math.max(tC, tD);

        if (Math.abs(tMax) <= EPS)
            return a; // CD endet in A, liegt sonst "vor" AB
        if (Math.abs(tMin - 1) <= EPS)
            return b; // CD beginnt in B, liegt sonst "hinter" AB

        return null; // getrennt oder ueberlappend
    }

    private static float cross(Vector2 u, Vector2 v) {
        return u.x() * v.y() - u.y() * v.x();
    }

    boolean equalsApproximately(float a, float b) {
        return Math.abs(a - b) < EPS;
    }

    boolean greaterThan(float a, float b) {
        return a - b > EPS;
    }

    boolean greaterThanRobust(float a, float b) {
        return a - b > EPS * Math.max(1f, Math.max(Math.abs(a), Math.abs(b)));
    }

    boolean greaterThanOrEqual(float a, float b) {
        return a == b || a - b > -EPS;   // equivalently: b - a < EPS
    }



    static float ccw(Vector2 a, Vector2 b, Vector2 c) { // Orientierung: >0 links, <0 rechts, 0 kollinear
        return cross(Vector2.subtract(b, a), Vector2.subtract(c, a));
    }
}
