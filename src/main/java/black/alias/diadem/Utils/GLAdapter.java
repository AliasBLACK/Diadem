package black.alias.diadem.Utils;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class GLAdapter {
    public static void glClearColor(double red, double green, double blue, double alpha) {
        GL11.glClearColor((float)red, (float)green, (float)blue, (float)alpha);
    }
    
    public static void glBlendColor(double red, double green, double blue, double alpha) {
        GL14.glBlendColor((float)red, (float)green, (float)blue, (float)alpha);
    }
    
    // LibGDX-style texImage2D method with ByteBuffer parameter to avoid overload ambiguity
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, ByteBuffer pixels) {
        GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }
    
    // LibGDX-style texImage3D method with ByteBuffer parameter to avoid overload ambiguity
    public static void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, ByteBuffer pixels) {
        GL12.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, pixels);
    }
    
    // Uniform functions with proper type casting
    public static void glUniform1f(int location, double value) {
        GL20.glUniform1f(location, (float)value);
    }
    
    public static void glUniform2f(int location, double x, double y) {
        GL20.glUniform2f(location, (float)x, (float)y);
    }
    
    public static void glUniform3f(int location, double x, double y, double z) {
        GL20.glUniform3f(location, (float)x, (float)y, (float)z);
    }
    
    public static void glUniform4f(int location, double x, double y, double z, double w) {
        GL20.glUniform4f(location, (float)x, (float)y, (float)z, (float)w);
    }
    
    // Vector uniform functions with proper type casting
    public static void glUniform1fv(int location, FloatBuffer value) {
        GL20.glUniform1fv(location, value);
    }
    
    public static void glUniform2fv(int location, FloatBuffer value) {
        GL20.glUniform2fv(location, value);
    }
    
    public static void glUniform3fv(int location, FloatBuffer value) {
        GL20.glUniform3fv(location, value);
    }
    
    public static void glUniform4fv(int location, FloatBuffer value) {
        GL20.glUniform4fv(location, value);
    }
    
    public static void glUniform1iv(int location, IntBuffer value) {
        GL20.glUniform1iv(location, value);
    }
    
    public static void glUniform2iv(int location, IntBuffer value) {
        GL20.glUniform2iv(location, value);
    }
    
    public static void glUniform3iv(int location, IntBuffer value) {
        GL20.glUniform3iv(location, value);
    }
    
    public static void glUniform4iv(int location, IntBuffer value) {
        GL20.glUniform4iv(location, value);
    }
    
    // Matrix uniform functions with proper type casting
    public static void glUniformMatrix2fv(int location, boolean transpose, FloatBuffer value) {
        GL20.glUniformMatrix2fv(location, transpose, value);
    }
    
    public static void glUniformMatrix3fv(int location, boolean transpose, FloatBuffer value) {
        GL20.glUniformMatrix3fv(location, transpose, value);
    }
    
    public static void glUniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
        GL20.glUniformMatrix4fv(location, transpose, value);
    }
    
    // Helper method to convert JavaScript array to FloatBuffer
    public static FloatBuffer createFloatBuffer(double[] values) {
        FloatBuffer buffer = BufferUtils.newFloatBuffer(values.length);
        for (int i = 0; i < values.length; i++) {
            buffer.put(i, (float) values[i]);
        }
        buffer.rewind();
        return buffer;
    }
    
    // Helper method to convert JavaScript array to IntBuffer
    public static IntBuffer createIntBuffer(double[] values) {
        IntBuffer buffer = BufferUtils.newIntBuffer(values.length);
        for (int i = 0; i < values.length; i++) {
            buffer.put(i, (int) values[i]);
        }
        buffer.rewind();
        return buffer;
    }
}
