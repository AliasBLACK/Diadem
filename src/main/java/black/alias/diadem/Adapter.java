package black.alias.diadem;

import org.lwjgl.opengl.GL11;

public class Adapter {
    public void glClearColor(double red, double green, double blue, double alpha) {
        GL11.glClearColor((float)red, (float)green, (float)blue, (float)alpha);
    }
}
