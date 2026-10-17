import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class HeubergerFixed {
    static final BufferedReader reader;
    private static String line;
    private static int pos = 0;

    private static final double EPS = 1e-9;
    // so weit weg vom Ursprung liegt mindestens die hintere Kante eines Schattens
    private static final double MIN_FAR_DISTANCE = 1e6;

    private static int targetsHitCounter = 0;

    static {
        try {
            reader = new BufferedReader(new FileReader("heuberger.in"));
            line = reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String next() throws Exception {
        var nextPos = line.indexOf(' ', pos + 1);
        var token = line.substring(pos, nextPos == -1 ? line.length() : nextPos);
        if (nextPos == -1) {
            line = reader.readLine();
        }
        pos = nextPos + 1;
        return token;
    }

    public static void main(String[] args) throws Exception {
        try (reader; var out = new PrintWriter("heuberger.out")) {
            var amountOfTargets = Integer.parseInt(next());
            final var targets = new Vector2[amountOfTargets][2];

            int j = 0;
            while (j < amountOfTargets) {
                var a = new Vector2(Double.parseDouble(next()), Double.parseDouble(next()));
                var b = new Vector2(Double.parseDouble(next()), Double.parseDouble(next()));
                // Ausrichtung: Urs (Ursprung) liegt immer links von a -> b, d.h. cross(a, b) > 0
                if (cross(a, b) < 0) {
                    var tmp = a;
                    a = b;
                    b = tmp;
                }
                targets[j][0] = a;
                targets[j][1] = b;
                j++;
            }

            // Pro Ziel ein Schatten-Viereck (a, b, b hinten, a hinten).
            // Die hintere Kante wird um einen Faktor 2^k nach hinten geschoben (exakt, weil Zweierpotenz),
            // und zwar so weit, dass sie sicher hinter allen Zielen liegt.
            var unreachableAreas = new ArrayList<ArrayList<Vector2>>();
            final var farScale = new double[amountOfTargets];
            for (int i = 0; i < amountOfTargets; i++) {
                var a = targets[i][0];
                var b = targets[i][1];
                double distanceOfLine = cross(a, b) / Vector2.distance(a, b); // Abstand der Geraden ab zum Ursprung
                double k = 1;
                while (k * distanceOfLine < MIN_FAR_DISTANCE) {
                    k *= 2;
                }
                farScale[i] = k;

                var area = new ArrayList<Vector2>(4);
                area.add(a);
                area.add(b);
                area.add(b.multiply(k));
                area.add(a.multiply(k));
                unreachableAreas.add(area);
            }

            for (int i = 0; i < amountOfTargets; i++) {
                if (!isCompletelyHidden(i, targets, unreachableAreas, farScale)) {
                    targetsHitCounter++;
                }
            }
            out.println(targetsHitCounter);
        }
    }

    /**
     * Ein Ziel ist unerreichbar, wenn jeder seiner Punkte im Inneren (ohne Rand!) der vereinigten
     * Schatten der anderen Ziele liegt. Randpunkte zaehlen nicht, denn dort streift der Pfeil nur.
     */
    private static boolean isCompletelyHidden(int self, Vector2[][] targets,
                                              List<ArrayList<Vector2>> areas, double[] farScale) {
        var a = targets[self][0];
        var b = targets[self][1];

        // Schnelltest: beide Endpunkte muessen strikt in irgendeinem fremden Schatten liegen
        if (!insideAnyOtherArea(self, a, areas) || !insideAnyOtherArea(self, b, areas)) {
            return false;
        }

        // Endpunkte reichen nicht (Schatten koennen eine Luecke dazwischen lassen).
        // Darum: pro fremdem Schatten das Stueck des Ziels bestimmen, das strikt darin liegt,
        // P(t) = a + t * (b - a), und diese offenen Intervalle zusammenfuehren (mergen).
        var d = b.subtract(a);
        var intervals = new ArrayList<double[]>();
        for (int j = 0; j < targets.length; j++) {
            if (j == self) continue;
            var p = targets[j][0];
            var q = targets[j][1];
            var u = q.subtract(p);
            double c = cross(p, q);
            double cu = cross(u, a.subtract(p));
            double du = cross(u, d);

            var iv = new double[]{Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
            if (restrict(iv, cross(p, a), cross(p, d))                       // links vom Strahl durch p
                    && restrict(iv, cross(a, q), cross(d, q))                // rechts vom Strahl durch q
                    && restrict(iv, -cu, -du)                                // hinter der Kante pq
                    && restrict(iv, cu + (farScale[j] - 1) * c, du)) {       // vor der hinteren Kante
                if (iv[1] > -EPS && iv[0] < 1 + EPS) {
                    intervals.add(iv);
                }
            }
        }
        intervals.sort((x, y) -> Double.compare(x[0], y[0]));

        // [0, 1] muss luckenlos ueberdeckt sein; offene Intervalle, die sich nur beruehren, lassen
        // einen Streifschuss-Punkt frei und zaehlen deshalb nicht als Ueberdeckung.
        double reach = 0;
        for (var iv : intervals) {
            if (iv[0] >= reach - EPS) break;
            reach = Math.max(reach, iv[1]);
        }
        return reach > 1 + EPS;
    }

    /** Schraenkt iv auf {t : alpha + beta * t > 0} ein. Gibt false zurueck, wenn nichts uebrig bleibt. */
    private static boolean restrict(double[] iv, double alpha, double beta) {
        if (beta == 0) {
            return alpha > 0;
        }
        double t = -alpha / beta;
        if (beta > 0) {
            iv[0] = Math.max(iv[0], t);
        } else {
            iv[1] = Math.min(iv[1], t);
        }
        return iv[0] < iv[1];
    }

    private static boolean insideAnyOtherArea(int self, Vector2 p, List<ArrayList<Vector2>> areas) {
        for (int j = 0; j < areas.size(); j++) {
            if (j != self && contains(areas.get(j), p, false)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(List<Vector2> poly, Vector2 p, boolean boundaryInside) {
        final int n = poly.size();
        if (n < 3)
            return false;

        final double px = p.x(), py = p.y();
        boolean inside = false;
        Vector2 a = poly.get(n - 1); // letzte Ecke: schliesst das Polygon
        for (int i = 0; i < n; i++) {
            final Vector2 b = poly.get(i);
            final double ax = a.x();
            final double ay = a.y();
            final double bx = b.x();
            final double by = b.y();

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

    /** (b - a) x (p - a) */
    private static double cross(double ax, double ay, double bx, double by, double px, double py) {
        return (bx - ax) * (py - ay) - (by - ay) * (px - ax);
    }

    private static double cross(Vector2 u, Vector2 v) {
        return u.x() * v.y() - u.y() * v.x();
    }

    /*
     * Copyright (c) 2013 - 2022 Simon Felix
     * All rights reserved.
     *
     * Redistribution and use in source and binary forms, with or without
     * modification, are permitted provided that the following conditions are met:
     *
     *  Redistributions of source code must retain the above copyright notice,
     *   this list of conditions and the following disclaimer.
     *  Redistributions in binary form must reproduce the above copyright notice,
     *   this list of conditions and the following disclaimer in the documentation
     *   and/or other materials provided with the distribution.
     *  Neither the name of FHNW nor the names of its contributors may
     *   be used to endorse or promote products derived from this software without
     *   specific prior written permission.
     *
     * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
     * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
     * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
     * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER BE LIABLE FOR ANY
     * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
     * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
     * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
     * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
     * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
     * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
     */
    public record Vector2(double x, double y) {
        public static final Vector2 ZERO = new Vector2(0, 0);
        public static final Vector2 ONE = new Vector2(1, 1);
        public static final Vector2 UNIT_X = new Vector2(1, 0);
        public static final Vector2 UNIT_Y = new Vector2(0, 1);

        public static Vector2 add(Vector2 a, Vector2 b) {
            return new Vector2(a.x + b.x, a.y + b.y);
        }

        public Vector2 add(Vector2 v) {
            return add(this, v);
        }

        public static double distance(Vector2 a, Vector2 b) {
            return Math.sqrt(distanceSquared(a, b));
        }

        public static double distanceSquared(Vector2 a, Vector2 b) {
            return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
        }

        public static double dot(Vector2 a, Vector2 b) {
            return a.x * b.x + a.y * b.y;
        }

        public double length() {
            return Math.sqrt(lengthSquared());
        }

        public double lengthSquared() {
            return x * x + y * y;
        }

        public static Vector2 lerp(Vector2 a, Vector2 b, double t) {
            return new Vector2(
                    a.x * (1 - t) + b.x * t,
                    a.y * (1 - t) + b.y * t
            );
        }

        public static Vector2 multiply(Vector2 a, Vector2 b) {
            return a.multiply(b);
        }

        public Vector2 multiply(Vector2 b) {
            return new Vector2(x * b.x, y * b.y);
        }

        public Vector2 multiply(double s) {
            return new Vector2(x * s, y * s);
        }

        public Vector2 negate() {
            return multiply(-1);
        }

        public static Vector2 normalize(Vector2 v) {
            var l = v.length();
            if (Math.abs(l) < 1e-6 || l == 1)
                return v;
            return v.multiply(1 / l);
        }

        public static Vector2 reflect(Vector2 vector, Vector2 normal) {
            return vector.subtract(normal.multiply(2 * dot(vector, normal)));
        }

        public Vector2 subtract(Vector2 v) {
            return subtract(this, v);
        }

        public static Vector2 subtract(Vector2 a, Vector2 b) {
            return new Vector2(a.x - b.x, a.y - b.y);
        }
    }
}
