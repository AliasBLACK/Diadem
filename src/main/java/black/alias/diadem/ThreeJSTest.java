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
 * Three.js integration test using WebGL2 bridge
 */
public class ThreeJSTest {
    
    private long window;
    private JSContext webgl2Context;
    
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    
    public static void main(String[] args) {
        System.out.println("Starting Three.js WebGL2 Test...");
        new ThreeJSTest().run();
    }
    
    public void run() {
        init();
        loop();
        cleanup();
    }
    
    private void init() {
        // Setup an error callback
        GLFWErrorCallback.createPrint(System.err).set();
        
        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        
        // Create the window
        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "Three.js WebGL2 Demo", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        // Setup key callback
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
        });
        
        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            
            // Get the window size
            glfwGetWindowSize(window, pWidth, pHeight);
            
            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            
            // Center the window
            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        }
        
        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        
        // Enable v-sync
        glfwSwapInterval(1);
        
        // Make the window visible
        glfwShowWindow(window);
        
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        GL.createCapabilities();
        
        // Initialize WebGL2 context
        initWebGL2Context();
    }
    
    private void initWebGL2Context() {
        try {
            System.out.println("Initializing WebGL2 context...");
            webgl2Context = new JSContext();
            
            // Add polyfills for missing browser APIs
            String polyfills = """
                // Fake browser APIs for Three.js
                globalThis.window = globalThis;
                globalThis.self = globalThis;
                globalThis.navigator = {
                    userAgent: 'GraalVM',
                    platform: 'Java'
                };
                
                // Fake document object
                globalThis.document = {
                    createElement: function(tagName) {
                        if (tagName === 'canvas') {
                            return new HTMLCanvasElement();
                        }
                        return {
                            tagName: tagName.toUpperCase(),
                            style: {},
                            setAttribute: function() {},
                            getAttribute: function() { return null; },
                            appendChild: function() {},
                            removeChild: function() {}
                        };
                    },
                    createElementNS: function(namespace, tagName) {
                        return this.createElement(tagName);
                    },
                    body: {
                        appendChild: function() {},
                        removeChild: function() {}
                    }
                };
                
                // Fake canvas and WebGL context
                globalThis.HTMLCanvasElement = class HTMLCanvasElement {
                    constructor() {
                        this.width = 800;
                        this.height = 600;
                        this.style = {};
                        this.eventListeners = {};
                    }
                    getContext(type) {
                        if (type === 'webgl2' || type === 'webgl') {
                            return gl; // Return our WebGL2 context
                        }
                        return null;
                    }
                    setAttribute() {}
                    getAttribute() { return null; }
                    addEventListener(event, callback, options) {
                        if (!this.eventListeners[event]) {
                            this.eventListeners[event] = [];
                        }
                        this.eventListeners[event].push(callback);
                    }
                    removeEventListener(event, callback) {
                        if (this.eventListeners[event]) {
                            const index = this.eventListeners[event].indexOf(callback);
                            if (index > -1) {
                                this.eventListeners[event].splice(index, 1);
                            }
                        }
                    }
                    dispatchEvent(event) {
                        if (this.eventListeners[event.type]) {
                            this.eventListeners[event.type].forEach(callback => callback(event));
                        }
                    }
                };
                
                globalThis.AbortController = class AbortController {
                    constructor() {
                        this.signal = { aborted: false };
                    }
                    abort() {
                        this.signal.aborted = true;
                    }
                };
                
                globalThis.FileReader = class FileReader {
                    readAsDataURL() {}
                    readAsArrayBuffer() {}
                };
                
                console.log('Browser API polyfills loaded');
            """;
            
            webgl2Context.executeScript(polyfills);
            
            // Load Three.js CJS file
            try {
                String threeCjsPath = "src/main/lib/three.cjs";
                org.graalvm.polyglot.Source threeCjsSource = org.graalvm.polyglot.Source.newBuilder("js", 
                    new java.io.File(threeCjsPath))
                    .mimeType("application/javascript")
                    .name("three.cjs")
                    .build();
                
                // Set up CommonJS-like environment
                webgl2Context.executeScript("""
                    globalThis.module = { exports: {} };
                    globalThis.exports = globalThis.module.exports;
                """);
                
                // Load the CJS file
                webgl2Context.getJavaScriptContext().eval(threeCjsSource);
                
                // Extract THREE from module.exports
                webgl2Context.executeScript("""
                    globalThis.THREE = globalThis.module.exports;
                    console.log('Loaded Three.js CJS, available classes:', Object.keys(THREE).filter(k => typeof THREE[k] === 'function').slice(0, 10));
                    
                    // Check if WebGLRenderer is available
                    if (typeof THREE === 'object' && THREE !== null) {
                        if (typeof THREE.WebGLRenderer === 'function') {
                            console.log('WebGLRenderer is available!');
                        } else {
                            console.log('WebGLRenderer is NOT available - will need to create a fake one');
                        }
                    } else {
                        console.error('THREE object is not available');
                    }
                """);
                
                System.out.println("Three.js loaded using CJS format");
                
            } catch (Exception e) {
                System.err.println("Failed to load Three.js CJS: " + e.getMessage());
                throw new RuntimeException("Three.js loading failed", e);
            }
            
            // Create Three.js scene using minimal example
            String sceneScript = """
                console.log('Setting up Three.js scene...');
                
                // Three.js minimal example implementation
                var scene = new THREE.Scene();
                var camera = new THREE.PerspectiveCamera(
                    75, // Field of View
                    800 / 600, // aspect ratio (window.innerWidth / window.innerHeight)
                    0.1, // near clipping plane
                    1000 // far clipping plane
                );
                var renderer = new THREE.WebGLRenderer({
                    alpha: true, // transparent background
                    antialias: true, // smooth edges
                    context: gl // Use our WebGL2 context
                });
                renderer.setSize(800, 600); // window.innerWidth, window.innerHeight
                
                var geometry = new THREE.BoxGeometry(1, 1, 1);
                var material = new THREE.MeshNormalMaterial();
                var cube = new THREE.Mesh(geometry, material);
                scene.add(cube);
                
                camera.position.z = 5; // move camera back so we can see the cube
                
                console.log('Three.js minimal example setup complete');
                
                // Implement the render function from minimal example
                var frameCount = 0;
                var render = function() {
                    // Animate cube rotation
                    cube.rotation.x += 0.05;
                    cube.rotation.y += 0.05;
                    
                    // Use Three.js WebGLRenderer to render the scene
                    renderer.render(scene, camera);
                    
                    frameCount++;
                    if (frameCount % 60 === 0) {
                        console.log('Three.js rendered frame', frameCount);
                    }
                };
                
                // Store render function globally so Java can call it
                globalThis.threeJsRender = render;
                
                """;
            
            webgl2Context.executeScript(sceneScript);
            System.out.println("Three.js scene initialized");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize Three.js context: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loop() {
        System.out.println("Starting main loop - Three.js rendering");
        
        // Java-controlled render loop calling Three.js render function
        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents();
            
            // Call Three.js render function
            webgl2Context.executeScript("if (globalThis.threeJsRender) { globalThis.threeJsRender(); }");
            
            GLFW.glfwSwapBuffers(window);
            
            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Stop the Three.js render loop
        if (webgl2Context != null) {
            try {
                webgl2Context.executeScript("if (globalThis.threeJsApp) globalThis.threeJsApp.stop();");
            } catch (Exception e) {
                System.err.println("Error stopping Three.js render loop: " + e.getMessage());
            }
        }
    }
    
    private void cleanup() {
        // Close WebGL2 context
        if (webgl2Context != null) {
            webgl2Context.close();
        }
        
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        
        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
        
        System.out.println("Application cleanup completed");
    }
}
