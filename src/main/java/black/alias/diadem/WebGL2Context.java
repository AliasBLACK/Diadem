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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * WebGL2 Context that bridges JavaScript WebGL2 API calls to Java GLES implementation.
 * Provides a complete WebGL2-compatible interface for JavaScript applications.
 */
public class WebGL2Context implements AutoCloseable {
    private final Context jsContext;
    private final GLES32 gles;
    
    public WebGL2Context() {
        this.gles = new GLES32();
        this.jsContext = Context.newBuilder("js")
            .allowAllAccess(true)
            .build();
        
        // Load and initialize the WebGL2 bridge
        try {
            String bridgeScript = loadBridgeScript();
            jsContext.eval("js", bridgeScript);
            
            // Create bridge instance - no parameters needed since it uses Java.type directly
            jsContext.eval("js", "const webgl2Bridge = new WebGL2Bridge();");
            
            // Expose WebGL2 functions globally
            exposeWebGL2Functions();
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to load WebGL2 bridge script", e);
        }
    }
    
    private String loadBridgeScript() throws IOException {
        // Load the bridge script from resources
        return new String(Files.readAllBytes(Paths.get("src/main/resources/webgl2-bridge.js")));
    }
    
    private void exposeWebGL2Functions() {
        // Create a global 'gl' object that mimics WebGL2RenderingContext
        jsContext.eval("js", """
            const gl = {
                // Constants
                DEPTH_BUFFER_BIT: 0x00000100,
                STENCIL_BUFFER_BIT: 0x00000400,
                COLOR_BUFFER_BIT: 0x00004000,
                TRIANGLES: 0x0004,
                TRIANGLE_STRIP: 0x0005,
                TRIANGLE_FAN: 0x0006,
                UNSIGNED_BYTE: 0x1401,
                UNSIGNED_SHORT: 0x1403,
                FLOAT: 0x1406,
                RGBA: 0x1908,
                TEXTURE_2D: 0x0DE1,
                ARRAY_BUFFER: 0x8892,
                ELEMENT_ARRAY_BUFFER: 0x8893,
                STATIC_DRAW: 0x88E4,
                DYNAMIC_DRAW: 0x88E8,
                VERTEX_SHADER: 0x8B31,
                FRAGMENT_SHADER: 0x8B30,
                COMPILE_STATUS: 0x8B81,
                LINK_STATUS: 0x8B82,
                
                // WebGL2 specific constants
                TEXTURE_3D: 0x806F,
                TEXTURE_2D_ARRAY: 0x8C1A,
                UNIFORM_BUFFER: 0x8A11,
                TRANSFORM_FEEDBACK_BUFFER: 0x8C8E,
                
                // Core functions
                activeTexture: (texture) => webgl2Bridge.activeTexture(texture),
                attachShader: (program, shader) => webgl2Bridge.attachShader(program, shader),
                bindBuffer: (target, buffer) => webgl2Bridge.bindBuffer(target, buffer),
                bindTexture: (target, texture) => webgl2Bridge.bindTexture(target, texture),
                bufferData: (target, data, usage) => webgl2Bridge.bufferData(target, data, usage),
                clear: (mask) => webgl2Bridge.clear(mask),
                clearColor: (r, g, b, a) => webgl2Bridge.clearColor(r, g, b, a),
                compileShader: (shader) => webgl2Bridge.compileShader(shader),
                createBuffer: () => webgl2Bridge.createBuffer(),
                createProgram: () => webgl2Bridge.createProgram(),
                createShader: (type) => webgl2Bridge.createShader(type),
                createTexture: () => webgl2Bridge.createTexture(),
                deleteBuffer: (buffer) => webgl2Bridge.deleteBuffer(buffer),
                deleteProgram: (program) => webgl2Bridge.deleteProgram(program),
                deleteShader: (shader) => webgl2Bridge.deleteShader(shader),
                deleteTexture: (texture) => webgl2Bridge.deleteTexture(texture),
                drawArrays: (mode, first, count) => webgl2Bridge.drawArrays(mode, first, count),
                drawElements: (mode, count, type, offset) => webgl2Bridge.drawElements(mode, count, type, offset),
                enable: (cap) => webgl2Bridge.enable(cap),
                disable: (cap) => webgl2Bridge.disable(cap),
                enableVertexAttribArray: (index) => webgl2Bridge.enableVertexAttribArray(index),
                disableVertexAttribArray: (index) => webgl2Bridge.disableVertexAttribArray(index),
                getAttribLocation: (program, name) => webgl2Bridge.getAttribLocation(program, name),
                getUniformLocation: (program, name) => webgl2Bridge.getUniformLocation(program, name),
                getShaderParameter: (shader, pname) => webgl2Bridge.getShaderParameter(shader, pname),
                getProgramParameter: (program, pname) => webgl2Bridge.getProgramParameter(program, pname),
                getShaderInfoLog: (shader) => webgl2Bridge.getShaderInfoLog(shader),
                getProgramInfoLog: (program) => webgl2Bridge.getProgramInfoLog(program),
                linkProgram: (program) => webgl2Bridge.linkProgram(program),
                shaderSource: (shader, source) => webgl2Bridge.shaderSource(shader, source),
                texImage2D: (...args) => webgl2Bridge.texImage2D(...args),
                texParameteri: (target, pname, param) => webgl2Bridge.texParameteri(target, pname, param),
                uniform1f: (location, x) => webgl2Bridge.uniform1f(location, x),
                uniform1i: (location, x) => webgl2Bridge.uniform1i(location, x),
                uniform2f: (location, x, y) => webgl2Bridge.uniform2f(location, x, y),
                uniform3f: (location, x, y, z) => webgl2Bridge.uniform3f(location, x, y, z),
                uniform4f: (location, x, y, z, w) => webgl2Bridge.uniform4f(location, x, y, z, w),
                uniformMatrix4fv: (location, transpose, value) => webgl2Bridge.uniformMatrix4fv(location, transpose, value),
                useProgram: (program) => webgl2Bridge.useProgram(program),
                vertexAttribPointer: (index, size, type, normalized, stride, offset) => 
                    webgl2Bridge.vertexAttribPointer(index, size, type, normalized, stride, offset),
                viewport: (x, y, width, height) => webgl2Bridge.viewport(x, y, width, height),
                
                // WebGL2 specific functions
                bindVertexArray: (vao) => webgl2Bridge.bindVertexArray(vao),
                createVertexArray: () => webgl2Bridge.createVertexArray(),
                deleteVertexArray: (vao) => webgl2Bridge.deleteVertexArray(vao),
                texImage3D: (...args) => webgl2Bridge.texImage3D(...args),
                texSubImage3D: (...args) => webgl2Bridge.texSubImage3D(...args),
                drawArraysInstanced: (mode, first, count, instanceCount) => 
                    webgl2Bridge.drawArraysInstanced(mode, first, count, instanceCount),
                drawElementsInstanced: (mode, count, type, offset, instanceCount) => 
                    webgl2Bridge.drawElementsInstanced(mode, count, type, offset, instanceCount),
                beginTransformFeedback: (primitiveMode) => webgl2Bridge.beginTransformFeedback(primitiveMode),
                endTransformFeedback: () => webgl2Bridge.endTransformFeedback(),
                bindBufferBase: (target, index, buffer) => webgl2Bridge.bindBufferBase(target, index, buffer),
                bindBufferRange: (target, index, buffer, offset, size) => 
                    webgl2Bridge.bindBufferRange(target, index, buffer, offset, size),
                getUniformBlockIndex: (program, name) => webgl2Bridge.getUniformBlockIndex(program, name),
                uniformBlockBinding: (program, blockIndex, binding) => 
                    webgl2Bridge.uniformBlockBinding(program, blockIndex, binding)
            };
            
            // Make gl globally available
            globalThis.gl = gl;
            
            // Provide setTimeout implementation for render loops
            globalThis.setTimeout = function(callback, delay) {
                const Thread = Java.type('java.lang.Thread');
                const thread = new Thread(function() {
                    try {
                        Thread.sleep(delay || 0);
                        callback();
                    } catch (e) {
                        console.error('setTimeout error:', e);
                    }
                });
                thread.start();
            };
        """);
    }
    
    /**
     * Execute JavaScript code with WebGL2 context available
     */
    public Value executeScript(String script) {
        return jsContext.eval("js", script);
    }
    
    /**
     * Execute JavaScript code from a file
     */
    public Value executeScriptFile(String filename) throws IOException {
        String script = new String(Files.readAllBytes(Paths.get(filename)));
        return executeScript(script);
    }
    
    /**
     * Get the underlying GLES context for direct access
     */
    public GLES32 getGLESContext() {
        return gles;
    }
    
    /**
     * Get the JavaScript context for advanced operations
     */
    public Context getJavaScriptContext() {
        return jsContext;
    }
    
    /**
     * Close the context and free resources
     */
    public void close() {
        jsContext.close();
    }
}
