package compgeo;

public class PunktInPolygon {

    private PunktInPolygon() {
    }

    public static boolean contains(Vector2[] poly, Vector2 p, boolean boundaryInside) {
        final int n = poly.length;
        if (n < 3)
            return false;

        final float px = p.x(), py = p.y();
        boolean inside = false;
        Vector2 a = poly[n - 1]; // letzte Ecke: schliesst das Polygon
        for (int i = 0; i < n; i++) {
            final Vector2 b = poly[i];
            final float ax = a.x();
            final float ay = a.y();
            final float bx = b.x();
            final float by = b.y();

            // 1) Rand: liegt p auf der Kante ab? (billige Bounding-Box-Pruefung zuerst)
            if (px >= Math.min(ax, bx) && px <= Math.max(ax, bx)
                    && py >= Math.min(ay, by) && py <= Math.max(ay, by)
                    && cross(ax, ay, bx, by, px, py) == 0)
                return boundaryInside;

            // 2) Halboffene Regel: genau ein Endpunkt liegt strikt ueber dem Strahl
            if ((ay > py) != (by > py)) {
                // x des Schnittpunkts > px  <=>  cross / (by - ay) > 0
                double c = cross(ax, ay, bx, by, px, py);
                if (by > ay ? c > 0 : c < 0)
                    inside = !inside;
            }
            a = b;
        }
        return inside;
    }

    public static boolean containsConvex(Vector2[] poly, Vector2 p, boolean boundaryInside) {
        final int n = poly.length;
        if (n < 3)
            return false;

        final float px = p.x();
        final float py = p.y();
        boolean left = false;
        boolean right = false;
        boolean onLine = false;
        Vector2 a = poly[n - 1];
        for (final Vector2 b : poly) {
            final float ax = a.x();
            final float ay = a.y();
            final float bx = b.x();
            final float by = b.y();

            if (ax != bx || ay != by) { // Nullkante hat keine Richtung -> ueberspringen
                double c = cross(ax, ay, bx, by, px, py);
                if (c > 0) {
                    left = true;
                } else if (c < 0) {
                    right = true;
                } else {
                    onLine = true;
                }
                if (left && right)
                    return false; // auf beiden Seiten -> ausserhalb
            }
            a = b;
        }
        return onLine ? boundaryInside : true; // auf der Geraden einer Kante und sonst innen = Rand
    }

    /** (b - a) x (p - a), in double gerechnet, damit float-Produkte nicht zusaetzlich runden. */
    private static double cross(float ax, float ay, float bx, float by, float px, float py) {
        return ((double) bx - ax) * ((double) py - ay) - ((double) by - ay) * ((double) px - ax);
    }
}
