/*******************************************************************************
 * Copyright 2024
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
 * WebGL2 Triangle Application that creates a proper OpenGL context
 * and renders a triangle using the WebGL2 JavaScript bridge.
 */
public class SimpleTriangleTest {
    
    private long window;
    private WebGL2Context webgl2Context;
    
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    
    public static void main(String[] args) {
        System.out.println("Starting WebGL2 Triangle Application...");
        new SimpleTriangleTest().run();
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
        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "WebGL2 Triangle Demo", NULL, NULL);
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
        
        // Initialize OpenGL capabilities
        GL.createCapabilities();
        
        // Initialize WebGL2 context after OpenGL is ready
        initWebGL2Context();
    }
    
    private void initWebGL2Context() {
        System.out.println("Initializing WebGL2 context...");
        
        try {
            webgl2Context = new WebGL2Context();
            
            // Initialize WebGL2 triangle app and start JavaScript render loop
            String appScript = """
                console.log('Initializing WebGL2 Triangle App...');
                
                class TriangleApp {
                    constructor() {
                        this.program = null;
                        this.vao = null;
                        this.running = false;
                        this.frameCount = 0;
                        
                        this.init();
                    }
                    
                    init() {
                        // Set up viewport and clear color
                        gl.viewport(0, 0, 800, 600);
                        gl.clearColor(0.2, 0.3, 0.3, 1.0);
                        gl.enable(gl.DEPTH_TEST);
                        
                        // Create shaders and program
                        this.createShaderProgram();
                        
                        // Set up triangle geometry
                        this.setupGeometry();
                        
                        console.log('Triangle app initialized successfully');
                    }
                    
                    createShaderProgram() {
                        // Vertex shader source
                        const vertexShaderSource = `#version 330 core
                        layout (location = 0) in vec3 aPos;
                        layout (location = 1) in vec3 aColor;
                        
                        out vec3 vertexColor;
                        
                        void main() {
                            gl_Position = vec4(aPos, 1.0);
                            vertexColor = aColor;
                        }`;
                        
                        // Fragment shader source
                        const fragmentShaderSource = `#version 330 core
                        in vec3 vertexColor;
                        out vec4 FragColor;
                        
                        void main() {
                            FragColor = vec4(vertexColor, 1.0);
                        }`;
                        
                        // Create and compile shaders
                        const vertexShader = this.createShader(gl.VERTEX_SHADER, vertexShaderSource);
                        const fragmentShader = this.createShader(gl.FRAGMENT_SHADER, fragmentShaderSource);
                        
                        if (!vertexShader || !fragmentShader) {
                            throw new Error('Failed to create shaders');
                        }
                        
                        // Create and link program
                        this.program = gl.createProgram();
                        gl.attachShader(this.program, vertexShader);
                        gl.attachShader(this.program, fragmentShader);
                        gl.linkProgram(this.program);
                        
                        // Check program linking
                        if (!gl.getProgramParameter(this.program, gl.LINK_STATUS)) {
                            const log = gl.getProgramInfoLog(this.program);
                            throw new Error('Shader program linking failed: ' + log);
                        }
                        
                        // Clean up shaders
                        gl.deleteShader(vertexShader);
                        gl.deleteShader(fragmentShader);
                    }
                    
                    createShader(type, source) {
                        const shader = gl.createShader(type);
                        gl.shaderSource(shader, source);
                        gl.compileShader(shader);
                        
                        if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
                            const log = gl.getShaderInfoLog(shader);
                            console.error(`Shader compilation failed (${type === gl.VERTEX_SHADER ? 'vertex' : 'fragment'}):`, log);
                            gl.deleteShader(shader);
                            return null;
                        }
                        
                        return shader;
                    }
                    
                    setupGeometry() {
                        // Triangle vertices (position + color)
                        const vertices = new Float32Array([
                            // positions         // colors
                             0.0,  0.5, 0.0,     1.0, 0.0, 0.0,  // top vertex (red)
                            -0.5, -0.5, 0.0,     0.0, 1.0, 0.0,  // bottom left (green)
                             0.5, -0.5, 0.0,     0.0, 0.0, 1.0   // bottom right (blue)
                        ]);
                        
                        // Create and bind VAO
                        this.vao = gl.createVertexArray();
                        gl.bindVertexArray(this.vao);
                        
                        // Create and bind VBO
                        const vbo = gl.createBuffer();
                        gl.bindBuffer(gl.ARRAY_BUFFER, vbo);
                        gl.bufferData(gl.ARRAY_BUFFER, vertices, gl.STATIC_DRAW);
                        
                        // Position attribute
                        gl.vertexAttribPointer(0, 3, gl.FLOAT, false, 6 * 4, 0);
                        gl.enableVertexAttribArray(0);
                        
                        // Color attribute
                        gl.vertexAttribPointer(1, 3, gl.FLOAT, false, 6 * 4, 3 * 4);
                        gl.enableVertexAttribArray(1);
                    }
                    
                    render() {
                        // Clear the screen
                        gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
                        
                        // Use shader program
                        gl.useProgram(this.program);
                        
                        // Bind VAO and draw
                        gl.bindVertexArray(this.vao);
                        gl.drawArrays(gl.TRIANGLES, 0, 3);
                        
                        this.frameCount++;
                        if (this.frameCount % 60 === 0) {
                            console.log('Rendered frame', this.frameCount);
                        }
                    }
                    
                    startRenderLoop() {
                        this.running = true;
                        console.log('JavaScript render loop initialized - will be called from Java');
                    }
                    
                    stop() {
                        this.running = false;
                        console.log('Stopping render loop...');
                    }
                    
                    cleanup() {
                        this.stop();
                        if (this.vao) gl.deleteVertexArray(this.vao);
                        if (this.program) gl.deleteProgram(this.program);
                        console.log('Triangle app cleaned up');
                    }
                }
                
                // Create global app instance
                globalThis.triangleApp = new TriangleApp();
                
                // Start the render loop
                globalThis.triangleApp.startRenderLoop();
                
                console.log('WebGL2 Triangle App started');
            """;
            
            webgl2Context.executeScript(appScript);
            System.out.println("WebGL2 context initialized and JavaScript render loop started");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize WebGL2 context: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loop() {
        System.out.println("Starting main loop - JavaScript render loop is now handling rendering");
        
        // Java-controlled render loop calling JavaScript render function
        while (!glfwWindowShouldClose(window)) {
            // Poll for window events
            glfwPollEvents();
            
            // Call JavaScript render function directly from Java
            try {
                if (webgl2Context != null) {
                    webgl2Context.executeScript("if (globalThis.triangleApp && globalThis.triangleApp.running) { globalThis.triangleApp.render(); }");
                }
                glfwSwapBuffers(window);
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Stop the JavaScript render loop
        if (webgl2Context != null) {
            try {
                webgl2Context.executeScript("if (globalThis.triangleApp) globalThis.triangleApp.stop();");
            } catch (Exception e) {
                System.err.println("Error stopping JavaScript render loop: " + e.getMessage());
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
