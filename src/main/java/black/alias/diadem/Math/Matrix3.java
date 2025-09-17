/*******************************************************************************
 * Copyright 2011
 * LibGDX Authors
 * Mario Zechner <badlogicgames@gmail.com>
 * Nathan Sweet <nathan.sweet@gmail.com> 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package black.alias.diadem.Math;

/** A 3x3 column major matrix for 2D transforms and rotations.
 * @author badlogicgames@gmail.com */
public class Matrix3 {
    public static final int M00 = 0;
    public static final int M01 = 3;
    public static final int M02 = 6;
    public static final int M10 = 1;
    public static final int M11 = 4;
    public static final int M12 = 7;
    public static final int M20 = 2;
    public static final int M21 = 5;
    public static final int M22 = 8;

    public float[] val = new float[9];

    public Matrix3 () {
        idt();
    }

    public Matrix3 (Matrix3 matrix) {
        set(matrix);
    }

    public Matrix3 (float[] values) {
        this.set(values);
    }

    /** Sets this matrix to the identity matrix
     * @return This matrix for the purpose of chaining methods together. */
    public Matrix3 idt () {
        val[M00] = 1;
        val[M10] = 0;
        val[M20] = 0;
        val[M01] = 0;
        val[M11] = 1;
        val[M21] = 0;
        val[M02] = 0;
        val[M12] = 0;
        val[M22] = 1;
        return this;
    }

    /** Postmultiplies this matrix by the given matrix, storing the result in this matrix. For example:
     * 
     * <pre>
     * A.mul(B) results in A := AB.
     * </pre>
     * 
     * @param m The other matrix to multiply by.
     * @return This matrix for the purpose of chaining operations together. */
    public Matrix3 mul (Matrix3 m) {
        float[] val = this.val;

        float v00 = val[M00] * m.val[M00] + val[M01] * m.val[M10] + val[M02] * m.val[M20];
        float v01 = val[M00] * m.val[M01] + val[M01] * m.val[M11] + val[M02] * m.val[M21];
        float v02 = val[M00] * m.val[M02] + val[M01] * m.val[M12] + val[M02] * m.val[M22];

        float v10 = val[M10] * m.val[M00] + val[M11] * m.val[M10] + val[M12] * m.val[M20];
        float v11 = val[M10] * m.val[M01] + val[M11] * m.val[M11] + val[M12] * m.val[M21];
        float v12 = val[M10] * m.val[M02] + val[M11] * m.val[M12] + val[M12] * m.val[M22];

        float v20 = val[M20] * m.val[M00] + val[M21] * m.val[M10] + val[M22] * m.val[M20];
        float v21 = val[M20] * m.val[M01] + val[M21] * m.val[M11] + val[M22] * m.val[M21];
        float v22 = val[M20] * m.val[M02] + val[M21] * m.val[M12] + val[M22] * m.val[M22];

        val[M00] = v00;
        val[M10] = v10;
        val[M20] = v20;
        val[M01] = v01;
        val[M11] = v11;
        val[M21] = v21;
        val[M02] = v02;
        val[M12] = v12;
        val[M22] = v22;

        return this;
    }

    /** Sets this matrix to the given matrix.
     * 
     * @param mat The matrix that is to be copied. (The given matrix is not modified)
     * @return This matrix for the purpose of chaining methods together. */
    public Matrix3 set (Matrix3 mat) {
        System.arraycopy(mat.val, 0, val, 0, val.length);
        return this;
    }

    /** Sets the matrix to the given matrix as a float array. The float array must have at least 9 elements; the first 9 will be
     * copied.
     * 
     * @param values The matrix, in float array form, that is to be copied. Remember that this matrix is in column major order.
     *           (The float array is not modified)
     * @return This matrix for the purpose of chaining methods together. */
    public Matrix3 set (float[] values) {
        System.arraycopy(values, 0, val, 0, val.length);
        return this;
    }

    /** Adds a translational component to the matrix in the 3rd column. The other columns are untouched.
     * 
     * @param x The x-component of the translation vector.
     * @param y The y-component of the translation vector.
     * @return This matrix for the purpose of chaining. */
    public Matrix3 trn (float x, float y) {
        val[M02] += x;
        val[M12] += y;
        return this;
    }

    /** Sets this matrix to a translation matrix.
     * 
     * @param x The translation in x
     * @param y The translation in y
     * @return This matrix for chaining. */
    public Matrix3 setToTranslation (float x, float y) {
        idt();
        val[M02] = x;
        val[M12] = y;
        return this;
    }

    /** Sets this matrix to a scaling matrix.
     * 
     * @param scaleX the scale in x
     * @param scaleY the scale in y
     * @return This matrix for chaining. */
    public Matrix3 setToScaling (float scaleX, float scaleY) {
        idt();
        val[M00] = scaleX;
        val[M11] = scaleY;
        return this;
    }

    public String toString () {
        return "[" + val[0] + "|" + val[3] + "|" + val[6] + "]\n[" + val[1] + "|" + val[4] + "|" + val[7] + "]\n[" + val[2] + "|"
            + val[5] + "|" + val[8] + "]";
    }

    /** @return The determinant of this matrix */
    public float det () {
        return val[M00] * val[M11] * val[M22] + val[M01] * val[M12] * val[M20] + val[M02] * val[M10] * val[M21]
            - val[M00] * val[M12] * val[M21] - val[M01] * val[M10] * val[M22] - val[M02] * val[M11] * val[M20];
    }

    /** Inverts this matrix given that the determinant is != 0.
     * @return This matrix for the purpose of chaining methods together.
     * @throws GdxRuntimeException if the matrix is singular (not invertible) */
    public Matrix3 inv () {
        float det = det();
        if (det == 0) {
            System.err.println("Error: Can't invert a singular matrix");
            return this;
        }

        float inv_det = 1.0f / det;

        float[] tmp = new float[9];

        tmp[M00] = val[M11] * val[M22] - val[M21] * val[M12];
        tmp[M10] = val[M20] * val[M12] - val[M10] * val[M22];
        tmp[M20] = val[M10] * val[M21] - val[M20] * val[M11];
        tmp[M01] = val[M21] * val[M02] - val[M01] * val[M22];
        tmp[M11] = val[M00] * val[M22] - val[M20] * val[M02];
        tmp[M21] = val[M20] * val[M01] - val[M00] * val[M21];
        tmp[M02] = val[M01] * val[M12] - val[M11] * val[M02];
        tmp[M12] = val[M10] * val[M02] - val[M00] * val[M12];
        tmp[M22] = val[M00] * val[M11] - val[M10] * val[M01];

        val[M00] = inv_det * tmp[M00];
        val[M10] = inv_det * tmp[M10];
        val[M20] = inv_det * tmp[M20];
        val[M01] = inv_det * tmp[M01];
        val[M11] = inv_det * tmp[M11];
        val[M21] = inv_det * tmp[M21];
        val[M02] = inv_det * tmp[M02];
        val[M12] = inv_det * tmp[M12];
        val[M22] = inv_det * tmp[M22];

        return this;
    }

    /** Copies the values from the provided matrix to this matrix.
     * @param mat The matrix to copy.
     * @return This matrix for the purposes of chaining. */
    public Matrix3 set (Matrix4 mat) {
        val[M00] = mat.val[Matrix4.M00];
        val[M10] = mat.val[Matrix4.M10];
        val[M20] = mat.val[Matrix4.M20];
        val[M01] = mat.val[Matrix4.M01];
        val[M11] = mat.val[Matrix4.M11];
        val[M21] = mat.val[Matrix4.M21];
        val[M02] = mat.val[Matrix4.M02];
        val[M12] = mat.val[Matrix4.M12];
        val[M22] = mat.val[Matrix4.M22];
        return this;
    }

    /** Sets this 3x3 matrix to the top left 3x3 corner of the provided 4x4 matrix.
     * @param mat The matrix whose top left corner will be copied. This matrix will not be modified.
     * @return This matrix for the purpose of chaining operations. */
    public Matrix3 setAsAffine (Matrix4 mat) {
        val[M00] = mat.val[Matrix4.M00];
        val[M10] = mat.val[Matrix4.M10];
        val[M20] = 0;
        val[M01] = mat.val[Matrix4.M01];
        val[M11] = mat.val[Matrix4.M11];
        val[M21] = 0;
        val[M02] = mat.val[Matrix4.M03];
        val[M12] = mat.val[Matrix4.M13];
        val[M22] = 1;
        return this;
    }

    /** Sets this matrix to a rotation matrix that will rotate any vector in counter-clockwise direction around the z-axis.
     * @param degrees the angle in degrees.
     * @return This matrix for the purpose of chaining operations. */
    public Matrix3 setToRotation (float degrees) {
        return setToRotationRad((float)Math.toRadians(degrees));
    }

    /** Sets this matrix to a rotation matrix that will rotate any vector in counter-clockwise direction around the z-axis.
     * @param radians the angle in radians.
     * @return This matrix for the purpose of chaining operations. */
    public Matrix3 setToRotationRad (float radians) {
        float cos = (float)Math.cos(radians);
        float sin = (float)Math.sin(radians);

        this.val[M00] = cos;
        this.val[M10] = sin;
        this.val[M20] = 0;

        this.val[M01] = -sin;
        this.val[M11] = cos;
        this.val[M21] = 0;

        this.val[M02] = 0;
        this.val[M12] = 0;
        this.val[M22] = 1;

        return this;
    }

    public Matrix3 setToTranslationAndScaling (float translationX, float translationY, float scalingX, float scalingY) {
        idt();
        val[M02] = translationX;
        val[M12] = translationY;
        val[M00] = scalingX;
        val[M11] = scalingY;
        return this;
    }
}
