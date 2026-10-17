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
public record Matrix3x2(float m11, float m12, float m21, float m22, float m31, float m32) {

	public static final Matrix3x2 IDENTITY = new Matrix3x2(1, 0, 0, 1, 0, 0);

	public Vector2 translation() {
		return new Vector2(m31, m32);
	}

	public static Matrix3x2 add(Matrix3x2 a, Matrix3x2 b) {
		return new Matrix3x2(a.m11 + b.m11, a.m12 + b.m12, a.m21 + b.m21, a.m22 + b.m22, a.m31 + b.m31, a.m32 + b.m32);
	}

	public static Matrix3x2 multiply(Matrix3x2 a, float b) {
		return new Matrix3x2(a.m11 * b, a.m12 * b, a.m21 * b, a.m22 * b, a.m31 * b, a.m32 * b);
	}

	public static Matrix3x2 subtract(Matrix3x2 a, Matrix3x2 b) {
		return new Matrix3x2(a.m11 - b.m11, a.m12 - b.m12, a.m21 - b.m21, a.m22 - b.m22, a.m31 - b.m31, a.m32 - b.m32);
	}

	public static Matrix3x2 negate(Matrix3x2 a) {
		return new Matrix3x2(-a.m11, -a.m12, -a.m21, -a.m22, -a.m31, -a.m32);
	}

	public static Matrix3x2 createRotation(float radians) {
		var s = (float) Math.sin(radians);
		var c = (float) Math.cos(radians);

		return new Matrix3x2(c, s, -s, c, 0, 0);
	}

	public static Matrix3x2 createScale(float xScale, float yScale) {
		return new Matrix3x2(xScale, 0, 0, yScale, 0, 0);
	}

	public static Matrix3x2 createScale(Vector2 scales) {
		return createScale(scales.x(), scales.y());
	}

	public static Matrix3x2 createScale(float scale) {
		return createScale(scale, scale);
	}

	public static Matrix3x2 createTranslation(Vector2 position) {
		return createTranslation(position.x(), position.y());
	}

	public static Matrix3x2 createTranslation(float xPosition, float yPosition) {
		return new Matrix3x2(1, 0, 0, 1, xPosition, yPosition);
	}

	public static Matrix3x2 invert(Matrix3x2 matrix) {
		float det = (matrix.m11 * matrix.m22) - (matrix.m21 * matrix.m12);

		if (Math.abs(det) < 1e-10)
			return null;

		float invDet = 1.0f / det;

		return new Matrix3x2(matrix.m22() * invDet, -matrix.m12() * invDet, -matrix.m21() * invDet,
				matrix.m11() * invDet, (matrix.m21() * matrix.m32() - matrix.m31() * matrix.m22()) * invDet,
				(matrix.m31() * matrix.m12() - matrix.m11() * matrix.m32()) * invDet);
	}

	public static Matrix3x2 multiply(Matrix3x2 a, Matrix3x2 b, Matrix3x2... others) {
		var m = multiply(a, b);
		for (var c : others)
			m = multiply(m, c);
		return m;
	}

	public static Matrix3x2 multiply(Matrix3x2 a, Matrix3x2 b) {
		return new Matrix3x2(a.m11() * b.m11() + a.m12() * b.m21(), a.m11() * b.m12() + a.m12() * b.m22(),

				a.m21() * b.m11() + a.m22() * b.m21(), a.m21() * b.m12() + a.m22() * b.m22(),

				a.m31() * b.m11() + a.m32() * b.m21() + b.m31(), a.m31() * b.m12() + a.m32() * b.m22() + b.m32());
	}

	public Matrix3x2 multiply(Matrix3x2 b) {
		return multiply(this, b);
	}

	public Matrix3x2 multiply(float s) {
		return multiply(this, s);
	}

	public Matrix3x2 subtract(Matrix3x2 b) {
		return subtract(this, b);
	}

	public float getDeterminant() {
		return m11() * m22() - m21() * m12();
	}

	public float[] toArray() {
		return new float[] { m11, m12, m21, m22, m31, m32 };
	}
}