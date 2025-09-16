/*******************************************************************************
 * Copyright 2022 See AUTHORS file.
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

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static black.alias.diadem.GLENUMS.*;
import static black.alias.diadem.BufferUtils.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.*;

/** Simple OpenGL ES application that renders a colored triangle using the Diadem library
 * @author Diadem Team */
public class SimpleTriangleApp {
    
    private long window;
    private GLES32 gl;
    
    // Shader program and buffer objects
    private int shaderProgram;
    private int vao;
    private int vbo;
    
    // Vertex data for a simple triangle
    private final float[] vertices = {
        // positions     // colors
         0.0f,  0.5f,    1.0f, 0.0f, 0.0f, // top vertex - red
        -0.5f, -0.5f,    0.0f, 1.0f, 0.0f, // bottom left - green  
         0.5f, -0.5f,    0.0f, 0.0f, 1.0f  // bottom right - blue
    };
    
    // Vertex shader source
    private final String vertexShaderSource = 
        "#version 330 core\n" +
        "layout (location = 0) in vec2 aPos;\n" +
        "layout (location = 1) in vec3 aColor;\n" +
        "out vec3 vertexColor;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(aPos, 0.0, 1.0);\n" +
        "    vertexColor = aColor;\n" +
        "}\0";
    
    // Fragment shader source
    private final String fragmentShaderSource = 
        "#version 330 core\n" +
        "in vec3 vertexColor;\n" +
        "out vec4 FragColor;\n" +
        "void main() {\n" +
        "    FragColor = vec4(vertexColor, 1.0);\n" +
        "}\0";
    
    public void run() {
        init();
        loop();
        cleanup();
    }
    
    private void init() {
        // Setup error callback
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
        
        // Create window
        window = glfwCreateWindow(800, 600, "Diadem OpenGL ES Triangle", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        // Setup key callback
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
        });
        
        // Center window
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            
            glfwGetWindowSize(window, pWidth, pHeight);
            
            long monitor = glfwGetPrimaryMonitor();
            org.lwjgl.glfw.GLFWVidMode vidmode = glfwGetVideoMode(monitor);
            
            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        }
        
        // Make OpenGL context current
        glfwMakeContextCurrent(window);
        
        // Enable v-sync
        glfwSwapInterval(1);
        
        // Make window visible
        glfwShowWindow(window);
        
        // Initialize OpenGL capabilities
        GL.createCapabilities();
        
        // Initialize our OpenGL ES wrapper
        gl = new GLES32();
        
        // Setup OpenGL
        setupOpenGL();
    }
    
    private void setupOpenGL() {
        // Create and compile vertex shader
        int vertexShader = gl.glCreateShader(GL_VERTEX_SHADER);
        gl.glShaderSource(vertexShader, vertexShaderSource);
        gl.glCompileShader(vertexShader);
        checkShaderCompilation(vertexShader, "VERTEX");
        
        // Create and compile fragment shader
        int fragmentShader = gl.glCreateShader(GL_FRAGMENT_SHADER);
        gl.glShaderSource(fragmentShader, fragmentShaderSource);
        gl.glCompileShader(fragmentShader);
        checkShaderCompilation(fragmentShader, "FRAGMENT");
        
        // Create shader program
        shaderProgram = gl.glCreateProgram();
        gl.glAttachShader(shaderProgram, vertexShader);
        gl.glAttachShader(shaderProgram, fragmentShader);
        gl.glLinkProgram(shaderProgram);
        checkProgramLinking(shaderProgram);
        
        // Delete shaders as they're linked into our program now and no longer necessary
        gl.glDeleteShader(vertexShader);
        gl.glDeleteShader(fragmentShader);
        
        // Create vertex array object and vertex buffer object
        IntBuffer vaoBuffer = newIntBuffer(1);
        gl.glGenVertexArrays(1, vaoBuffer);
        vao = vaoBuffer.get(0);
        
        IntBuffer vboBuffer = newIntBuffer(1);
        gl.glGenBuffers(1, vboBuffer);
        vbo = vboBuffer.get(0);
        
        // Bind VAO
        gl.glBindVertexArray(vao);
        
        // Bind and fill VBO
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer vertexBuffer = newFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();
        gl.glBufferData(GL_ARRAY_BUFFER, vertices.length * 4, vertexBuffer, GL_STATIC_DRAW);
        
        // Configure vertex attributes
        // Position attribute (location = 0)
        gl.glVertexAttribPointer(0, 2, GL_FLOAT, false, 5 * 4, 0);
        gl.glEnableVertexAttribArray(0);
        
        // Color attribute (location = 1)
        gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 5 * 4, 2 * 4);
        gl.glEnableVertexAttribArray(1);
        
        // Unbind VBO and VAO
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);
    }
    
    private void checkShaderCompilation(int shader, String type) {
        IntBuffer success = newIntBuffer(1);
        gl.glGetShaderiv(shader, GL_COMPILE_STATUS, success);
        if (success.get(0) == 0) {
            String infoLog = gl.glGetShaderInfoLog(shader);
            throw new RuntimeException("ERROR::SHADER::" + type + "::COMPILATION_FAILED\n" + infoLog);
        }
    }
    
    private void checkProgramLinking(int program) {
        IntBuffer success = newIntBuffer(1);
        gl.glGetProgramiv(program, GL_LINK_STATUS, success);
        if (success.get(0) == 0) {
            String infoLog = gl.glGetProgramInfoLog(program);
            throw new RuntimeException("ERROR::SHADER::PROGRAM::LINKING_FAILED\n" + infoLog);
        }
    }
    
    private void loop() {
        // Set clear color to dark blue
        gl.glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        
        // Run the rendering loop until the user has attempted to close the window
        while (!glfwWindowShouldClose(window)) {
            // Poll for window events
            glfwPollEvents();
            
            // Clear the framebuffer
            gl.glClear(GL_COLOR_BUFFER_BIT);
            
            // Use our shader program
            gl.glUseProgram(shaderProgram);
            
            // Bind VAO and draw triangle
            gl.glBindVertexArray(vao);
            gl.glDrawArrays(GL_TRIANGLES, 0, 3);
            
            // Swap front and back buffers
            glfwSwapBuffers(window);
        }
    }
    
    private void cleanup() {
        // Delete OpenGL objects
        if (vao != 0) {
            IntBuffer vaoBuffer = newIntBuffer(1);
            vaoBuffer.put(vao).flip();
            gl.glDeleteVertexArrays(1, vaoBuffer);
        }
        
        if (vbo != 0) {
            IntBuffer vboBuffer = newIntBuffer(1);
            vboBuffer.put(vbo).flip();
            gl.glDeleteBuffers(1, vboBuffer);
        }
        
        if (shaderProgram != 0) {
            gl.glDeleteProgram(shaderProgram);
        }
        
        // Free the window callbacks and destroy the window
        org.lwjgl.glfw.Callbacks.glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        
        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
    
    public static void main(String[] args) {
        new SimpleTriangleApp().run();
    }
}
