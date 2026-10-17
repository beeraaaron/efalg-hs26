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
public record Vector4(float x, float y, float z, float w) {
    public static final Vector4 ZERO = new Vector4(0, 0, 0, 0);
    public static final Vector4 ONE = new Vector4(1, 1, 1, 1);
    public static final Vector4 UNIT_X = new Vector4(1, 0, 0, 0);
    public static final Vector4 UNIT_Y = new Vector4(0, 1, 0, 0);
    public static final Vector4 UNIT_Z = new Vector4(0, 0, 1, 0);
    public static final Vector4 UNIT_W = new Vector4(0, 0, 0, 1);

    public Vector4(double x, double y, double z, double w) {
        this((float) x, (float) y, (float) z, (float) w);
    }

    public Vector4(Vector3 v, float w) {
        this(v.x(), v.y(), v.z(), w);
    }

    public Vector4 add(Vector4 v) {
        return new Vector4(x + v.x, y + v.y, z + v.z, w + v.w);
    }

    public static float distance(Vector4 a, Vector4 b) {
        return (float) Math.sqrt(distanceSquared(a, b));
    }

    public static float distanceSquared(Vector4 a, Vector4 b) {
        return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y) + (a.z - b.z) * (a.z - b.z) + (a.w - b.w) * (a.w - b.w);
    }

    public static float dot(Vector4 a, Vector4 b) {
        return a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w;
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    public float lengthSquared() {
        return x * x + y * y + z * z + w * w;
    }

    public static Vector4 lerp(Vector4 v0, Vector4 v1, float t) {
        return new Vector4(
                v0.x * (1 - t) + v1.x * t,
                v0.y * (1 - t) + v1.y * t,
                v0.z * (1 - t) + v1.z * t,
                v0.w * (1 - t) + v1.w * t);
    }

    public static Vector4 multiply(Vector4 a, Vector4 b) {
        return a.multiply(b);
    }

    public Vector4 multiply(Vector4 b) {
        return new Vector4(x * b.x, y * b.y, z * b.z, w * b.w);
    }

    public Vector4 multiply(float s) {
        return new Vector4(x * s, y * s, z * s, w * s);
    }

    public Vector4 negate() {
        return multiply(-1);
    }

    public static Vector4 normalize(Vector4 v) {
        var l = v.length();
        if (Math.abs(l) < 1e-6 || l == 1)
            return v;
        return v.multiply(1f / l);
    }

    public Vector4 subtract(Vector4 v) {
        return subtract(this, v);
    }

    public static Vector4 subtract(Vector4 a, Vector4 b) {
        return new Vector4(a.x - b.x, a.y - b.y, a.z - b.z, a.w - b.w);
    }

    public static Vector4 transform(Vector4 v, Matrix4x4 m) {
        return new Vector4(
                (v.x * m.m11()) + (v.y * m.m21()) + (v.z * m.m31()) + (v.w * m.m41()),
                (v.x * m.m12()) + (v.y * m.m22()) + (v.z * m.m32()) + (v.w * m.m42()),
                (v.x * m.m13()) + (v.y * m.m23()) + (v.z * m.m33()) + (v.w * m.m43()),
                (v.x * m.m14()) + (v.y * m.m24()) + (v.z * m.m34()) + (v.w * m.m44())
        );
    }
}