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
public record Matrix4x4(float m11, float m12, float m13, float m14,
                        float m21, float m22, float m23, float m24,
                        float m31, float m32, float m33, float m34,
                        float m41, float m42, float m43, float m44) {

    public static final Matrix4x4 IDENTITY = new Matrix4x4(
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1);

    public Vector3 translation() {
        return new Vector3(m41, m42, m43);
    }

    public static Matrix4x4 add(Matrix4x4 a, Matrix4x4 b) {
        return new Matrix4x4(
                a.m11 + b.m11, a.m12 + b.m12, a.m13 + b.m13, a.m14 + b.m14,
                a.m21 + b.m21, a.m22 + b.m22, a.m23 + b.m23, a.m24 + b.m24,
                a.m31 + b.m31, a.m32 + b.m32, a.m33 + b.m33, a.m34 + b.m34,
                a.m41 + b.m41, a.m42 + b.m42, a.m43 + b.m43, a.m44 + b.m44);
    }

    public static Matrix4x4 multiply(Matrix4x4 a, float b) {
        return new Matrix4x4(
                a.m11 * b, a.m12 * b, a.m13 * b, a.m14 * b,
                a.m21 * b, a.m22 * b, a.m23 * b, a.m24 * b,
                a.m31 * b, a.m32 * b, a.m33 * b, a.m34 * b,
                a.m41 * b, a.m42 * b, a.m43 * b, a.m44 * b);
    }

    public static Matrix4x4 subtract(Matrix4x4 a, Matrix4x4 b) {
        return new Matrix4x4(
                a.m11 - b.m11, a.m12 - b.m12, a.m13 - b.m13, a.m14 - b.m14,
                a.m21 - b.m21, a.m22 - b.m22, a.m23 - b.m23, a.m24 - b.m24,
                a.m31 - b.m31, a.m32 - b.m32, a.m33 - b.m33, a.m34 - b.m34,
                a.m41 - b.m41, a.m42 - b.m42, a.m43 - b.m43, a.m44 - b.m44);
    }

    public static Matrix4x4 negate(Matrix4x4 a) {
        return new Matrix4x4(
                -a.m11, -a.m12, -a.m13, -a.m14,
                -a.m21, -a.m22, -a.m23, -a.m24,
                -a.m31, -a.m32, -a.m33, -a.m34,
                -a.m41, -a.m42, -a.m43, -a.m44);
    }

    public static Matrix4x4 createFromAxisAngle(Vector3 axis, float angle) {
        var x = axis.x();
        var y = axis.y();
        var z = axis.z();
        var sa = (float) Math.sin(angle);
        var ca = (float) Math.cos(angle);
        var xx = x * x;
        var yy = y * y;
        var zz = z * z;
        var xy = x * y;
        var xz = x * z;
        var yz = y * z;

        return new Matrix4x4(
                xx + ca * (1 - xx), xy - ca * xy + sa * z, xz - ca * xz - sa * y, 0,
                xy - ca * xy - sa * z, yy + ca * (1 - yy), yz - ca * yz + sa * x, 0,
                xz - ca * xz + sa * y, yz - ca * yz - sa * x, zz + ca * (1 - zz), 0,
                0, 0, 0, 1);
    }

    public static Matrix4x4 createLookAt(Vector3 cameraPosition, Vector3 cameraTarget, Vector3 cameraUpVector) {
        var zaxis = Vector3.normalize(cameraPosition.subtract(cameraTarget));
        var xaxis = Vector3.normalize(Vector3.cross(cameraUpVector, zaxis));
        var yaxis = Vector3.cross(zaxis, xaxis);

        return new Matrix4x4(
                xaxis.x(), yaxis.x(), zaxis.x(), 0,
                xaxis.y(), yaxis.y(), zaxis.y(), 0,
                xaxis.z(), yaxis.z(), zaxis.z(), 0,
                -Vector3.dot(xaxis, cameraPosition), -Vector3.dot(yaxis, cameraPosition), -Vector3.dot(zaxis, cameraPosition), 1);
    }

    public static Matrix4x4 createPerspectiveFieldOfView(float fieldOfView, float aspectRatio, float nearPlaneDistance, float farPlaneDistance) {
        if (fieldOfView <= 0 || fieldOfView >= Math.PI || nearPlaneDistance <= 0 || farPlaneDistance <= 0 || nearPlaneDistance >= farPlaneDistance)
            throw new IllegalArgumentException();

        var yScale = 1f / (float) Math.tan(fieldOfView * 0.5);
        var xScale = yScale / aspectRatio;
        var negFarRange = farPlaneDistance / (nearPlaneDistance - farPlaneDistance);

        return new Matrix4x4(
                xScale, 0, 0, 0,
                0, yScale, 0, 0,
                0, 0, negFarRange, -1,
                0, 0, nearPlaneDistance * negFarRange, 0);
    }

    public static Matrix4x4 createRotationX(float radians) {
        var s = (float) Math.sin(radians);
        var c = (float) Math.cos(radians);

        return new Matrix4x4(
                1, 0, 0, 0,
                0, c, s, 0,
                0, -s, c, 0,
                0, 0, 0, 1);
    }

    public static Matrix4x4 createRotationY(float radians) {
        var s = (float) Math.sin(radians);
        var c = (float) Math.cos(radians);

        return new Matrix4x4(
                c, 0, -s, 0,
                0, 1, 0, 0,
                s, 0, c, 0,
                0, 0, 0, 1);
    }

    public static Matrix4x4 createRotationZ(float radians) {
        var s = (float) Math.sin(radians);
        var c = (float) Math.cos(radians);

        return new Matrix4x4(
                c, s, 0, 0,
                -s, c, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
    }

    public static Matrix4x4 createScale(float xScale, float yScale, float zScale) {
        return new Matrix4x4(
                xScale, 0, 0, 0,
                0, yScale, 0, 0,
                0, 0, zScale, 0,
                0, 0, 0, 1);
    }

    public static Matrix4x4 createScale(Vector3 scales) {
        return createScale(scales.x(), scales.y(), scales.z());
    }

    public static Matrix4x4 createScale(float scale) {
        return createScale(scale, scale, scale);
    }

    public static Matrix4x4 createTranslation(Vector3 position) {
        return createTranslation(position.x(), position.y(), position.z());
    }

    public static Matrix4x4 createTranslation(float xPosition, float yPosition, float zPosition) {
        return new Matrix4x4(
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                xPosition, yPosition, zPosition, 1);
    }

    public static Matrix4x4 invert(Matrix4x4 matrix) {
        // This implementation is based on the DirectX Math Library XMMatrixInverse method
        // https://github.com/microsoft/DirectXMath/blob/master/Inc/DirectXMathMatrix.inl

        //                                       -1
        // If you have matrix M, inverse Matrix M   can compute
        //
        //     -1       1
        //    M   = --------- A
        //            det(M)
        //
        // A is adjugate (adjoint) of M, where,
        //
        //      T
        // A = C
        //
        // C is Cofactor matrix of M, where,
        //           i + j
        // C   = (-1)      * det(M  )
        //  ij                    ij
        //
        //     [ a b c d ]
        // M = [ e f g h ]
        //     [ i j k l ]
        //     [ m n o p ]
        //
        // First Row
        //           2 | f g h |
        // C   = (-1)  | j k l | = + ( f ( kp - lo ) - g ( jp - ln ) + h ( jo - kn ) )
        //  11         | n o p |
        //
        //           3 | e g h |
        // C   = (-1)  | i k l | = - ( e ( kp - lo ) - g ( ip - lm ) + h ( io - km ) )
        //  12         | m o p |
        //
        //           4 | e f h |
        // C   = (-1)  | i j l | = + ( e ( jp - ln ) - f ( ip - lm ) + h ( in - jm ) )
        //  13         | m n p |
        //
        //           5 | e f g |
        // C   = (-1)  | i j k | = - ( e ( jo - kn ) - f ( io - km ) + g ( in - jm ) )
        //  14         | m n o |
        //
        // Second Row
        //           3 | b c d |
        // C   = (-1)  | j k l | = - ( b ( kp - lo ) - c ( jp - ln ) + d ( jo - kn ) )
        //  21         | n o p |
        //
        //           4 | a c d |
        // C   = (-1)  | i k l | = + ( a ( kp - lo ) - c ( ip - lm ) + d ( io - km ) )
        //  22         | m o p |
        //
        //           5 | a b d |
        // C   = (-1)  | i j l | = - ( a ( jp - ln ) - b ( ip - lm ) + d ( in - jm ) )
        //  23         | m n p |
        //
        //           6 | a b c |
        // C   = (-1)  | i j k | = + ( a ( jo - kn ) - b ( io - km ) + c ( in - jm ) )
        //  24         | m n o |
        //
        // Third Row
        //           4 | b c d |
        // C   = (-1)  | f g h | = + ( b ( gp - ho ) - c ( fp - hn ) + d ( fo - gn ) )
        //  31         | n o p |
        //
        //           5 | a c d |
        // C   = (-1)  | e g h | = - ( a ( gp - ho ) - c ( ep - hm ) + d ( eo - gm ) )
        //  32         | m o p |
        //
        //           6 | a b d |
        // C   = (-1)  | e f h | = + ( a ( fp - hn ) - b ( ep - hm ) + d ( en - fm ) )
        //  33         | m n p |
        //
        //           7 | a b c |
        // C   = (-1)  | e f g | = - ( a ( fo - gn ) - b ( eo - gm ) + c ( en - fm ) )
        //  34         | m n o |
        //
        // Fourth Row
        //           5 | b c d |
        // C   = (-1)  | f g h | = - ( b ( gl - hk ) - c ( fl - hj ) + d ( fk - gj ) )
        //  41         | j k l |
        //
        //           6 | a c d |
        // C   = (-1)  | e g h | = + ( a ( gl - hk ) - c ( el - hi ) + d ( ek - gi ) )
        //  42         | i k l |
        //
        //           7 | a b d |
        // C   = (-1)  | e f h | = - ( a ( fl - hj ) - b ( el - hi ) + d ( ej - fi ) )
        //  43         | i j l |
        //
        //           8 | a b c |
        // C   = (-1)  | e f g | = + ( a ( fk - gj ) - b ( ek - gi ) + c ( ej - fi ) )
        //  44         | i j k |
        //
        // Cost of operation
        // 53 adds, 104 muls, and 1 div.
        float a = matrix.m11, b = matrix.m12, c = matrix.m13, d = matrix.m14;
        float e = matrix.m21, f = matrix.m22, g = matrix.m23, h = matrix.m24;
        float i = matrix.m31, j = matrix.m32, k = matrix.m33, l = matrix.m34;
        float m = matrix.m41, n = matrix.m42, o = matrix.m43, p = matrix.m44;

        float kp_lo = k * p - l * o;
        float jp_ln = j * p - l * n;
        float jo_kn = j * o - k * n;
        float ip_lm = i * p - l * m;
        float io_km = i * o - k * m;
        float in_jm = i * n - j * m;

        float a11 = +(f * kp_lo - g * jp_ln + h * jo_kn);
        float a12 = -(e * kp_lo - g * ip_lm + h * io_km);
        float a13 = +(e * jp_ln - f * ip_lm + h * in_jm);
        float a14 = -(e * jo_kn - f * io_km + g * in_jm);

        float det = a * a11 + b * a12 + c * a13 + d * a14;

        if (Math.abs(det) < 1e-10)
            return null;

        float invDet = 1.0f / det;


        float gp_ho = g * p - h * o;
        float fp_hn = f * p - h * n;
        float fo_gn = f * o - g * n;
        float ep_hm = e * p - h * m;
        float eo_gm = e * o - g * m;
        float en_fm = e * n - f * m;

        float gl_hk = g * l - h * k;
        float fl_hj = f * l - h * j;
        float fk_gj = f * k - g * j;
        float el_hi = e * l - h * i;
        float ek_gi = e * k - g * i;
        float ej_fi = e * j - f * i;

        return new Matrix4x4(
                a11 * invDet,
                -(b * kp_lo - c * jp_ln + d * jo_kn) * invDet,
                +(b * gp_ho - c * fp_hn + d * fo_gn) * invDet,
                -(b * gl_hk - c * fl_hj + d * fk_gj) * invDet,
                a12 * invDet,
                +(a * kp_lo - c * ip_lm + d * io_km) * invDet,
                -(a * gp_ho - c * ep_hm + d * eo_gm) * invDet,
                +(a * gl_hk - c * el_hi + d * ek_gi) * invDet,
                a13 * invDet,
                -(a * jp_ln - b * ip_lm + d * in_jm) * invDet,
                +(a * fp_hn - b * ep_hm + d * en_fm) * invDet,
                -(a * fl_hj - b * el_hi + d * ej_fi) * invDet,
                a14 * invDet,
                +(a * jo_kn - b * io_km + c * in_jm) * invDet,
                -(a * fo_gn - b * eo_gm + c * en_fm) * invDet,
                +(a * fk_gj - b * ek_gi + c * ej_fi) * invDet);
    }

    public static Matrix4x4 multiply(Matrix4x4 a, Matrix4x4 b, Matrix4x4... others) {
        var m = multiply(a, b);
        for (var c : others)
            m = multiply(m, c);
        return m;
    }

    public static Matrix4x4 multiply(Matrix4x4 a, Matrix4x4 b) {
        return new Matrix4x4(
                a.m11 * b.m11 + a.m12 * b.m21 + a.m13 * b.m31 + a.m14 * b.m41,
                a.m11 * b.m12 + a.m12 * b.m22 + a.m13 * b.m32 + a.m14 * b.m42,
                a.m11 * b.m13 + a.m12 * b.m23 + a.m13 * b.m33 + a.m14 * b.m43,
                a.m11 * b.m14 + a.m12 * b.m24 + a.m13 * b.m34 + a.m14 * b.m44,

                a.m21 * b.m11 + a.m22 * b.m21 + a.m23 * b.m31 + a.m24 * b.m41,
                a.m21 * b.m12 + a.m22 * b.m22 + a.m23 * b.m32 + a.m24 * b.m42,
                a.m21 * b.m13 + a.m22 * b.m23 + a.m23 * b.m33 + a.m24 * b.m43,
                a.m21 * b.m14 + a.m22 * b.m24 + a.m23 * b.m34 + a.m24 * b.m44,

                a.m31 * b.m11 + a.m32 * b.m21 + a.m33 * b.m31 + a.m34 * b.m41,
                a.m31 * b.m12 + a.m32 * b.m22 + a.m33 * b.m32 + a.m34 * b.m42,
                a.m31 * b.m13 + a.m32 * b.m23 + a.m33 * b.m33 + a.m34 * b.m43,
                a.m31 * b.m14 + a.m32 * b.m24 + a.m33 * b.m34 + a.m34 * b.m44,

                a.m41 * b.m11 + a.m42 * b.m21 + a.m43 * b.m31 + a.m44 * b.m41,
                a.m41 * b.m12 + a.m42 * b.m22 + a.m43 * b.m32 + a.m44 * b.m42,
                a.m41 * b.m13 + a.m42 * b.m23 + a.m43 * b.m33 + a.m44 * b.m43,
                a.m41 * b.m14 + a.m42 * b.m24 + a.m43 * b.m34 + a.m44 * b.m44);
    }

    public Matrix4x4 multiply(Matrix4x4 b) {
        return multiply(this, b);
    }

    public Matrix4x4 multiply(float s) {
        return multiply(this, s);
    }

    public Matrix4x4 subtract(Matrix4x4 b) {
        return subtract(this, b);
    }

    public static Matrix4x4 transpose(Matrix4x4 m) {
        return new Matrix4x4(
                m.m11, m.m21, m.m31, m.m41,
                m.m12, m.m22, m.m32, m.m42,
                m.m13, m.m23, m.m33, m.m43,
                m.m41, m.m42, m.m43, m.m44);
    }

    public float getDeterminant() {
        float a = m11, b = m12, c = m13, d = m14;
        float e = m21, f = m22, g = m23, h = m24;
        float i = m31, j = m32, k = m33, l = m34;
        float m = m41, n = m42, o = m43, p = m44;

        float kp_lo = k * p - l * o;
        float jp_ln = j * p - l * n;
        float jo_kn = j * o - k * n;
        float ip_lm = i * p - l * m;
        float io_km = i * o - k * m;
        float in_jm = i * n - j * m;

        return a * (f * kp_lo - g * jp_ln + h * jo_kn) -
                b * (e * kp_lo - g * ip_lm + h * io_km) +
                c * (e * jp_ln - f * ip_lm + h * in_jm) -
                d * (e * jo_kn - f * io_km + g * in_jm);
    }

    public float[] toArray() {
        return new float[]{
                m11, m12, m13, m14,
                m21, m22, m23, m24,
                m31, m32, m33, m34,
                m41, m42, m43, m44
        };
    }
}