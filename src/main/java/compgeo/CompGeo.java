package compgeo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class CompGeo {
    private static final String inputFilename = "compgeo.in";
    private static final String outputFilename = "compgeo.out";
    private static final BufferedReader reader;
    private static String inputLine;
    private static int inputPos =0;

    private static final float EPS = 1e-6f; // 10^-6 = 0.000001

    static {
        try {
            reader = new BufferedReader(new FileReader(inputFilename));
            inputLine = reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        int amountOfPolygons = Integer.parseInt(next());
        var parsedPolygons = parsePolygonPoints(amountOfPolygons);

        var vectors = calculateVectors(parsedPolygons, amountOfPolygons);
        // TODO Anstelle von die Vectors nach Y zu sortieren, müsste ich für alle Vectors
        // Die Events generieren und dann die Events nach Y sortieren.
        // Die Events müssen einen Index haben, um zu Wissen zu welchem Vector sie gehören.
        int size = compact(vectors);
        sortByStartYDescending(vectors, size);
        System.out.println(Arrays.deepToString(vectors));
    }

    private static void sortByStartYDescending(Vector2[][] lines, int size) {
        for (int i = size / 2 - 1; i >= 0; i--) {
            siftDown(lines, i, size);
        }
        for (int end = size - 1; end > 0; end--) {
            var tmp = lines[0];
            lines[0] = lines[end];
            lines[end] = tmp;
            siftDown(lines, 0, end);
        }
    }

    private static void siftDown(Vector2[][] lines, int root, int size) {
        var item = lines[root];
        float key = item[1].y();

        while (true) {
            int child = 2 * root + 1;
            if (child >= size) break;
            if (child + 1 < size && lines[child + 1][1].y() < lines[child][1].y()) {
                child++; // pick the smaller child
            }
            if (lines[child][1].y() >= key) break;
            lines[root] = lines[child];
            root = child;
        }
        lines[root] = item;
    }

    private static int compact(Vector2[][] lines) {
        int count = 0;
        for (int i = 0; i < lines.length; i++) {
            var line = lines[i];
            if (line != null && line[1] != null) {
                lines[count++] = line;
            }
        }
        // Clear the leftover tail so the removed rows can be garbage collected
        for (int i = count; i < lines.length; i++) {
            lines[i] = null;
        }
        return count;
    }

    private static Vector2[][] calculateVectors(Vector2[][] parsedPolygons, int amountOfPolygons) {
        int vectorIndex = 0;
        var vectors = new Vector2[amountOfPolygons * 4][3];
        for (int i = 0; i < amountOfPolygons; i++) {
            var polygonPoints = parsedPolygons[i];
            for (int j = 0; j < polygonPoints.length - 1; j++) {
                var a = polygonPoints[j];
                var b = polygonPoints[j + 1];
                var lineVectors = createLineVectors(a, b);
                if (lineVectors != null) {
                    if (vectorIndex == vectors.length) {
                        vectors = Arrays.copyOf(vectors, vectorIndex * 2);
                    }
                    vectors[vectorIndex] = lineVectors;
                    vectorIndex++;
                }
            }
            var lineVectors = createLineVectors(polygonPoints[0], polygonPoints[polygonPoints.length - 1]);
            if (lineVectors != null) {
                if (vectorIndex == vectors.length) {
                    vectors = Arrays.copyOf(vectors, vectorIndex * 2);
                }
                vectors[vectorIndex] = lineVectors;
                vectorIndex++;
            }
        }
        return vectors;
    }

    private static Vector2[] createLineVectors(Vector2 a, Vector2 b) {
        Vector2 x;
        Vector2 y;
        if (greaterThanApproximately(a.y, b.y)) {
            x = a;
            y = b;
        } else {
            x = b;
            y = a;
        }

        var vector = y.subtract(x);
        if (!equalsApproximately(vector.y, 0f)) {
            var lineVectors = new Vector2[3];
            lineVectors[0] = vector;
            lineVectors[1] = x;
            lineVectors[2] = y;
            return lineVectors;
        } else {
            // Horizontale Linie: Wird ignoriert.
            return null;
        }
    }

    private static boolean equalsApproximately(float a, float b) {
        return Math.abs(a - b) < EPS;
    }

    private static boolean greaterThanApproximately(float a, float b) {
        return a - b > EPS;
    }

    private static Vector2[][] parsePolygonPoints(int amountOfPolygons) throws Exception {
        var polygons = new Vector2[amountOfPolygons][];

        for (int i = 0; i < amountOfPolygons; i++) {
            var amountOfPoints = Integer.parseInt(next());
            var points = new Vector2[amountOfPoints];

            for (int j = 0; j < amountOfPoints; j++) {
                var tokens = next().split(",");
                points[j] = new Vector2(Float.parseFloat(tokens[0]), Float.parseFloat(tokens[1]));
            }
            polygons[i] = points;
        }
        return polygons;
    }

    static String next() throws Exception{
        var nextPos = inputLine.indexOf(' ', inputPos + 1);
        var token = inputLine.substring(inputPos, nextPos == -1 ? inputLine.length() : nextPos);
        if(nextPos==-1) {
            inputLine = reader.readLine();
        }
        inputPos = nextPos+1;
        return token;
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
}
