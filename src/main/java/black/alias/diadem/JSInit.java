package black.alias.diadem;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class JSInit {
    
    private long window;
    private JSContext jsContext;
    private black.alias.diadem.Loaders.ScriptManager scriptManager;
    private black.alias.diadem.Loaders.AssetManager assetManager;
    
    private Settings settings;
    
    public static void main(String[] args) {
        new JSInit().run();
    }
    
    public void run() {
        init();
        loop();
        
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
        
        if (jsContext != null) {
            jsContext.close();
        }
    }
    
    private void init() {
        // Load settings first
        settings = Settings.load();
        
        GLFWErrorCallback.createPrint(System.err).set();
        
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("mac")) {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        } else {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        }
        
        // Create window using settings
        long monitor = settings.isFullscreen() ? glfwGetPrimaryMonitor() : NULL;
        window = glfwCreateWindow(settings.getResolutionWidth(), settings.getResolutionHeight(), 
                                settings.getWindowTitle(), monitor, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
        });
        
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            
            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            
            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        }
        
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();
        
        initJSContext();
    }
    
    private void initJSContext() {
        jsContext = new JSContext();
        scriptManager = new black.alias.diadem.Loaders.ScriptManager(jsContext);
        assetManager = new black.alias.diadem.Loaders.AssetManager(jsContext);

        try {
            jsContext.executeScriptFile("/polyfills.js"); 
            jsContext.executeModule("import * as THREE from 'three'; globalThis.THREE = THREE;");
            jsContext.setupGLTFLoader();
            jsContext.setupTextureLoader();
            assetManager.setupAssetLoader();
            jsContext.executeScriptFile("/extensions.js");
            scriptManager.loadMainScript();
        } catch (Exception e) {
            System.err.println("Failed to initialize JavaScript context: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loop() {
        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents();
            jsContext.executeScript("runCallbacks()");
            GLFW.glfwSwapBuffers(window);
            
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        if (jsContext != null) {
            try {
                jsContext.executeScript("if (globalThis.threeJsApp) globalThis.threeJsApp.stop();");
            } catch (Exception e) {
                System.err.println("Error stopping Three.js render loop: " + e.getMessage());
            }
        }
    }
}
