package compgeo;

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
    
    public static Vector2 transform(Vector2 position, Matrix3x2 m) {
        return new Vector2(
                (position.x * m.m11()) + (position.y * m.m21()) + m.m31(),
                (position.x * m.m12()) + (position.y * m.m22()) + m.m32()
        );
    }
}