import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Heuberger {
    static final BufferedReader reader;
    private static String line;
    private static int pos =0;

    private static final float EPS = 1e-6f; // 10^-6 = 0.000001
    private static final float MAX_DISTANCE = 100000f;

    private static int targetsHitCounter = 0;

    static{
        try{
            reader=new BufferedReader(new FileReader("heuberger.in"));
            line =reader.readLine();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String next() throws Exception{
        var nextPos = line.indexOf(' ', pos + 1);
        var token = line.substring(pos, nextPos == -1 ? line.length() : nextPos);
        if(nextPos==-1) {
            line = reader.readLine();
        }
        pos = nextPos+1;
        return token;
    }

    public static void main(String[] args) throws Exception
    {
        try(reader; var out = new PrintWriter("heuberger.out"))
        {
            //your code here
            //var a = Integer.parseInt(next());
            //out.println("solution");
            var amountOfTargets = Integer.parseInt(next());
            final var targets = new Vector2[amountOfTargets][2];

            int j = 0;
            while (j < amountOfTargets) {
                var x1 = Float.parseFloat(next());
                var y1 = Float.parseFloat(next());
                var x2 = Float.parseFloat(next());
                var y2 = Float.parseFloat(next());
                var a = new Vector2(x1, y1);
                var b = new Vector2(x2, y2);
                targets[j][0] = a;
                targets[j][1] = b;
                j++;
            }

            var unreachableAreas = new ArrayList<ArrayList<Vector2>>();
            for (int targetIndex = 0; targetIndex < amountOfTargets; targetIndex++) {
                var target = targets[targetIndex];
                var area = new Vector2[4];
                // TODO maybe dont multiply but add in the right direction
                var zeroInf = target[0].multiply(MAX_DISTANCE);
                var oneInf = target[1].multiply(MAX_DISTANCE);
                area[0] = target[0];
                area[1] = target[1];
                area[2] = oneInf;
                area[3] = zeroInf;

                var newUnreachableArea = new ArrayList<Vector2>();
                for (int x = unreachableAreas.size() - 1; x > 0; x--) {
                    var unreachableArea = unreachableAreas.get(x);

                    boolean intersects = false;
                    if (contains(unreachableArea, target[0], true) || contains(unreachableArea, target[1], true)) {
                        intersects = true;
                    }

                    newUnreachableArea = new ArrayList<>();
                    if (intersects) {
                        for (int m = 0; m < unreachableArea.size() / 2; m++) {
                            newUnreachableArea.add(unreachableArea.get(m));
                        }
                        newUnreachableArea.add(area[0]);
                        newUnreachableArea.add(area[1]);
                        newUnreachableArea.add(unreachableArea.get(unreachableArea.size() - 1));
                        newUnreachableArea.add(unreachableArea.getLast());
                        newUnreachableArea.add(area[2]);
                        newUnreachableArea.add(area[3]);
                        unreachableAreas.set(x, newUnreachableArea);
                    }
                }
                if (newUnreachableArea.isEmpty()) {
                    newUnreachableArea.add(area[0]);
                    newUnreachableArea.add(area[1]);
                    newUnreachableArea.add(area[2]);
                    newUnreachableArea.add(area[3]);
                    unreachableAreas.add(newUnreachableArea);
                }
            }


            for (int i = 0; i < amountOfTargets; i++) {
                var target = targets[i];

                j = 0;
                boolean reachable = true;
                while (reachable && j < unreachableAreas.size()) {
                    var area = unreachableAreas.get(j);
                    if (contains(area, target[0], false) && contains(area, target[1], false)) {
                        reachable = false;
                    }
                    j++;
                }
                if (reachable) {
                    targetsHitCounter++;
                }
            }
            out.println(targetsHitCounter);


            //for (int i = 0; i < targets.length; i++) {
            //    var target = targets[i];
            //    var targetsInBetween = findTargetsInBetween();
//
            //    int j = 0;
            //    int x = 0;
            //    var targetHittable = true;
            //    var blockingTargets = new Vector2[targets.length][2];
            //    while (j < targetsInBetween.length) {
            //        var targetInBetween = targetsInBetween[j];
            //        var ursVector1 = target[0];
            //        var ursVector2 = target[1];
            //        if (intersection(Vector2.ZERO, ursVector1, targetInBetween[0], targetInBetween[1]) != null ||
            //                 intersection(Vector2.ZERO, ursVector2, targetInBetween[0], targetInBetween[1]) != null) {
            //            targetHittable = false;
            //            blockingTargets[x] = targetInBetween;
            //            x++;
            //        }
            //    }
            //    if (targetHittable) {
            //        targetsHitCounter++;
            //    } else {
//
            //    }
            //}
        }
    }

    public static boolean contains(List<Vector2> poly, Vector2 p, boolean boundaryInside) {
        final int n = poly.size();
        if (n < 3)
            return false;

        final float px = p.x(), py = p.y();
        boolean inside = false;
        Vector2 a = poly.get(n - 1); // letzte Ecke: schliesst das Polygon
        for (int i = 0; i < n; i++) {
            final Vector2 b = poly.get(i);
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

    /** (b - a) x (p - a), in double gerechnet, damit float-Produkte nicht zusaetzlich runden. */
    private static double cross(float ax, float ay, float bx, float by, float px, float py) {
        return ((double) bx - ax) * ((double) py - ay) - ((double) by - ay) * ((double) px - ax);
    }

    private static boolean equalsApproximately(float a, float b) {
        return Math.abs(a - b) < EPS;
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
    public record Vector2(float x, float y) {
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

        public static float distance(Vector2 a, Vector2 b) {
            return (float) Math.sqrt(distanceSquared(a, b));
        }

        public static float distanceSquared(Vector2 a, Vector2 b) {
            return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
        }

        public static float dot(Vector2 a, Vector2 b) {
            return a.x * b.x + a.y * b.y;
        }

        public float length() {
            return (float) Math.sqrt(lengthSquared());
        }

        public float lengthSquared() {
            return x * x + y * y;
        }

        public static Vector2 lerp(Vector2 a, Vector2 b, float t) {
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

        public Vector2 multiply(float s) {
            return new Vector2(x * s, y * s);
        }

        public Vector2 negate() {
            return multiply(-1);
        }

        public static Vector2 normalize(Vector2 v) {
            var l = v.length();
            if (Math.abs(l) < 1e-6 || l == 1)
                return v;
            return v.multiply(1f / l);
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

    //public static Vector2 intersection(Vector2 a, Vector2 b, Vector2 c, Vector2 d) {
    //    var ab = Vector2.subtract(b, a);
    //    var cd = Vector2.subtract(d, c);
    //    var ac = Vector2.subtract(c, a);
//
    //    float denominant = cross(ab, cd);
    //    if (Math.abs(denominant) <= EPS * ab.length() * cd.length())
    //        return null; // parallel oder kollinear
//
    //    float lambdaAB = cross(ac, cd) / denominant; // Position auf AB
    //    float lambdaCD = cross(ac, ab) / denominant; // Position auf CD
//
    //    if (lambdaAB < -EPS || lambdaAB > 1 + EPS || lambdaCD < -EPS || lambdaCD > 1 + EPS)
    //        return null; // Schnittpunkt liegt ausserhalb mindestens einer Strecke
//
    //    return a.add(ab.multiply(lambdaAB));
    //}

    //private static float cross(Vector2 u, Vector2 v) {
    //    return u.x() * v.y() - u.y() * v.x();
    //}
}
