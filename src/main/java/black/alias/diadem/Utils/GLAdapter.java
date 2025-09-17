package black.alias.diadem.Utils;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import java.nio.ByteBuffer;

public class GLAdapter {
    public static void glClearColor(double red, double green, double blue, double alpha) {
        GL11.glClearColor((float)red, (float)green, (float)blue, (float)alpha);
    }
    
    // LibGDX-style texImage2D method with ByteBuffer parameter to avoid overload ambiguity
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, ByteBuffer pixels) {
        GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }
    
    // LibGDX-style texImage3D method with ByteBuffer parameter to avoid overload ambiguity
    public static void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, ByteBuffer pixels) {
        GL12.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, pixels);
    }
}
