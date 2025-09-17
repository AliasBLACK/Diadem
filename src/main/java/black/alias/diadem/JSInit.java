package black.alias.diadem;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * JavaScript Application Initializer
 * 
 * Initializes LWJGL OpenGL context and executes client-side JavaScript applications
 * with WebGL2 API support through JSContext.
 */
public class JSInit {
    
    private long window;
    private JSContext jsContext;
    
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    
    public static void main(String[] args) {
        new JSInit().run();
    }
    
    public void run() {
        System.out.println("Starting JavaScript Application...");
        
        init();
        loop();
        
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        
        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
        
        // Close JavaScript context
        if (jsContext != null) {
            jsContext.close();
        }
    }
    
    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();
        
        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable
        
        // Platform-specific OpenGL context configuration
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("mac")) {
            // macOS requires Core Profile and forward compatibility
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
            System.out.println("Configured OpenGL Core Profile for macOS");
        } else {
            // Windows/Linux can use compatibility profile
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            System.out.println("Configured OpenGL 4.3 Core Profile for " + osName);
        }
        
        // Create the window
        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "JavaScript WebGL Application", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            }
        });
        
        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*
            
            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);
            
            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            
            // Center the window
            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically
        
        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        
        // Enable v-sync
        glfwSwapInterval(1);
        
        // Make the window visible
        glfwShowWindow(window);
        
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        GL.createCapabilities();
        
        // Initialize JavaScript context
        initJSContext();
    }
    
    private void initJSContext() {

        System.out.println("Initializing JavaScript context...");
        jsContext = new JSContext();

        // Init JS Context.
        try {
            // Load polyfills.
            jsContext.executeScriptFile("src/main/lib/polyfills.js"); 

            // Load and execute main.js from client folder as ES6 module
            jsContext.executeModuleFile("src/main/src/main.js");

        } catch (Exception e) {
            System.err.println("Failed to initialize JavaScript context: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loop() {

        // Java-controlled render loop calling Three.js render function
        while (!GLFW.glfwWindowShouldClose(window)) {
            
            // Poll events.
            GLFW.glfwPollEvents();
            
            // Run animation callbacks.
            jsContext.executeScript("runCallbacks()");
            
            // Swap buffers.
            GLFW.glfwSwapBuffers(window);
            
            // Wait for 16ms.
            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Stop the Three.js render loop
        if (jsContext != null) {
            try {
                jsContext.executeScript("if (globalThis.threeJsApp) globalThis.threeJsApp.stop();");
            } catch (Exception e) {
                System.err.println("Error stopping Three.js render loop: " + e.getMessage());
            }
        }
    }
}
