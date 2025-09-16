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
            .allowExperimentalOptions(true)
            .option("js.esm-eval-returns-exports", "true")
            .option("js.ecmascript-version", "2022")
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
        String webgl2Script = """
            // Create a global 'gl' object that mimics the WebGL2RenderingContext
            const gl = {
                // Buffer bits
                DEPTH_BUFFER_BIT: 0x00000100,
                STENCIL_BUFFER_BIT: 0x00000400,
                COLOR_BUFFER_BIT: 0x00004000,
                
                // Boolean values
                FALSE: 0,
                TRUE: 1,
                
                // Primitive types
                POINTS: 0x0000,
                LINES: 0x0001,
                LINE_LOOP: 0x0002,
                LINE_STRIP: 0x0003,
                TRIANGLES: 0x0004,
                TRIANGLE_STRIP: 0x0005,
                TRIANGLE_FAN: 0x0006,
                
                // Blending
                ZERO: 0,
                ONE: 1,
                SRC_COLOR: 0x0300,
                ONE_MINUS_SRC_COLOR: 0x0301,
                SRC_ALPHA: 0x0302,
                ONE_MINUS_SRC_ALPHA: 0x0303,
                DST_ALPHA: 0x0304,
                ONE_MINUS_DST_ALPHA: 0x0305,
                DST_COLOR: 0x0306,
                ONE_MINUS_DST_COLOR: 0x0307,
                SRC_ALPHA_SATURATE: 0x0308,
                FUNC_ADD: 0x8006,
                BLEND_EQUATION: 0x8009,
                BLEND_EQUATION_RGB: 0x8009,
                BLEND_EQUATION_ALPHA: 0x883D,
                FUNC_SUBTRACT: 0x800A,
                FUNC_REVERSE_SUBTRACT: 0x800B,
                BLEND_DST_RGB: 0x80C8,
                BLEND_SRC_RGB: 0x80C9,
                BLEND_DST_ALPHA: 0x80CA,
                BLEND_SRC_ALPHA: 0x80CB,
                CONSTANT_COLOR: 0x8001,
                ONE_MINUS_CONSTANT_COLOR: 0x8002,
                CONSTANT_ALPHA: 0x8003,
                ONE_MINUS_CONSTANT_ALPHA: 0x8004,
                BLEND_COLOR: 0x8005,
                
                // Buffers
                ARRAY_BUFFER: 0x8892,
                ELEMENT_ARRAY_BUFFER: 0x8893,
                ARRAY_BUFFER_BINDING: 0x8894,
                ELEMENT_ARRAY_BUFFER_BINDING: 0x8895,
                STREAM_DRAW: 0x88E0,
                STATIC_DRAW: 0x88E4,
                DYNAMIC_DRAW: 0x88E8,
                BUFFER_SIZE: 0x8764,
                BUFFER_USAGE: 0x8765,
                CURRENT_VERTEX_ATTRIB: 0x8626,
                
                // Culling
                FRONT: 0x0404,
                BACK: 0x0405,
                FRONT_AND_BACK: 0x0408,
                CULL_FACE: 0x0B44,
                
                // Enable/Disable
                TEXTURE_2D: 0x0DE1,
                BLEND: 0x0BE2,
                DITHER: 0x0BD0,
                STENCIL_TEST: 0x0B90,
                DEPTH_TEST: 0x0B71,
                SCISSOR_TEST: 0x0C11,
                POLYGON_OFFSET_FILL: 0x8037,
                SAMPLE_ALPHA_TO_COVERAGE: 0x809E,
                SAMPLE_COVERAGE: 0x80A0,
                
                // Errors
                NO_ERROR: 0,
                INVALID_ENUM: 0x0500,
                INVALID_VALUE: 0x0501,
                INVALID_OPERATION: 0x0502,
                OUT_OF_MEMORY: 0x0505,
                
                // Front face
                CW: 0x0900,
                CCW: 0x0901,
                
                // Data types
                BYTE: 0x1400,
                UNSIGNED_BYTE: 0x1401,
                SHORT: 0x1402,
                UNSIGNED_SHORT: 0x1403,
                INT: 0x1404,
                UNSIGNED_INT: 0x1405,
                FLOAT: 0x1406,
                FIXED: 0x140C,
                
                // Pixel formats
                DEPTH_COMPONENT: 0x1902,
                ALPHA: 0x1906,
                RGB: 0x1907,
                RGBA: 0x1908,
                LUMINANCE: 0x1909,
                LUMINANCE_ALPHA: 0x190A,
                UNSIGNED_SHORT_4_4_4_4: 0x8033,
                UNSIGNED_SHORT_5_5_5_1: 0x8034,
                UNSIGNED_SHORT_5_6_5: 0x8363,
                
                // Shaders
                FRAGMENT_SHADER: 0x8B30,
                VERTEX_SHADER: 0x8B31,
                MAX_VERTEX_ATTRIBS: 0x8869,
                MAX_VERTEX_UNIFORM_VECTORS: 0x8DFB,
                MAX_VARYING_VECTORS: 0x8DFC,
                MAX_COMBINED_TEXTURE_IMAGE_UNITS: 0x8B4D,
                MAX_VERTEX_TEXTURE_IMAGE_UNITS: 0x8B4C,
                MAX_TEXTURE_IMAGE_UNITS: 0x8872,
                MAX_FRAGMENT_UNIFORM_VECTORS: 0x8DFD,
                SHADER_TYPE: 0x8B4F,
                DELETE_STATUS: 0x8B80,
                LINK_STATUS: 0x8B82,
                VALIDATE_STATUS: 0x8B83,
                ATTACHED_SHADERS: 0x8B85,
                ACTIVE_UNIFORMS: 0x8B86,
                ACTIVE_UNIFORM_MAX_LENGTH: 0x8B87,
                ACTIVE_ATTRIBUTES: 0x8B89,
                ACTIVE_ATTRIBUTE_MAX_LENGTH: 0x8B8A,
                SHADING_LANGUAGE_VERSION: 0x8B8C,
                CURRENT_PROGRAM: 0x8B8D,
                COMPILE_STATUS: 0x8B81,
                INFO_LOG_LENGTH: 0x8B84,
                SHADER_SOURCE_LENGTH: 0x8B88,
                SHADER_COMPILER: 0x8DFA,
                
                // Depth/Stencil functions
                NEVER: 0x0200,
                LESS: 0x0201,
                EQUAL: 0x0202,
                LEQUAL: 0x0203,
                GREATER: 0x0204,
                NOTEQUAL: 0x0205,
                GEQUAL: 0x0206,
                ALWAYS: 0x0207,
                KEEP: 0x1E00,
                REPLACE: 0x1E01,
                INCR: 0x1E02,
                DECR: 0x1E03,
                INVERT: 0x150A,
                INCR_WRAP: 0x8507,
                DECR_WRAP: 0x8508,
                
                // String names
                VENDOR: 0x1F00,
                RENDERER: 0x1F01,
                VERSION: 0x1F02,
                EXTENSIONS: 0x1F03,
                
                // Texture filtering
                NEAREST: 0x2600,
                LINEAR: 0x2601,
                NEAREST_MIPMAP_NEAREST: 0x2700,
                LINEAR_MIPMAP_NEAREST: 0x2701,
                NEAREST_MIPMAP_LINEAR: 0x2702,
                LINEAR_MIPMAP_LINEAR: 0x2703,
                TEXTURE_MAG_FILTER: 0x2800,
                TEXTURE_MIN_FILTER: 0x2801,
                TEXTURE_WRAP_S: 0x2802,
                TEXTURE_WRAP_T: 0x2803,
                
                // Textures
                TEXTURE: 0x1702,
                TEXTURE_CUBE_MAP: 0x8513,
                TEXTURE_BINDING_CUBE_MAP: 0x8514,
                TEXTURE_CUBE_MAP_POSITIVE_X: 0x8515,
                TEXTURE_CUBE_MAP_NEGATIVE_X: 0x8516,
                TEXTURE_CUBE_MAP_POSITIVE_Y: 0x8517,
                TEXTURE_CUBE_MAP_NEGATIVE_Y: 0x8518,
                TEXTURE_CUBE_MAP_POSITIVE_Z: 0x8519,
                TEXTURE_CUBE_MAP_NEGATIVE_Z: 0x851A,
                MAX_CUBE_MAP_TEXTURE_SIZE: 0x851C,
                
                // Texture units
                TEXTURE0: 0x84C0,
                TEXTURE1: 0x84C1,
                TEXTURE2: 0x84C2,
                TEXTURE3: 0x84C3,
                TEXTURE4: 0x84C4,
                TEXTURE5: 0x84C5,
                TEXTURE6: 0x84C6,
                TEXTURE7: 0x84C7,
                TEXTURE8: 0x84C8,
                TEXTURE9: 0x84C9,
                TEXTURE10: 0x84CA,
                TEXTURE11: 0x84CB,
                TEXTURE12: 0x84CC,
                TEXTURE13: 0x84CD,
                TEXTURE14: 0x84CE,
                TEXTURE15: 0x84CF,
                TEXTURE16: 0x84D0,
                TEXTURE17: 0x84D1,
                TEXTURE18: 0x84D2,
                TEXTURE19: 0x84D3,
                TEXTURE20: 0x84D4,
                TEXTURE21: 0x84D5,
                TEXTURE22: 0x84D6,
                TEXTURE23: 0x84D7,
                TEXTURE24: 0x84D8,
                TEXTURE25: 0x84D9,
                TEXTURE26: 0x84DA,
                TEXTURE27: 0x84DB,
                TEXTURE28: 0x84DC,
                TEXTURE29: 0x84DD,
                TEXTURE30: 0x84DE,
                TEXTURE31: 0x84DF,
                ACTIVE_TEXTURE: 0x84E0,
                
                // Texture wrapping
                REPEAT: 0x2901,
                CLAMP_TO_EDGE: 0x812F,
                MIRRORED_REPEAT: 0x8370,
                
                // Uniform types
                FLOAT_VEC2: 0x8B50,
                FLOAT_VEC3: 0x8B51,
                FLOAT_VEC4: 0x8B52,
                INT_VEC2: 0x8B53,
                INT_VEC3: 0x8B54,
                INT_VEC4: 0x8B55,
                BOOL: 0x8B56,
                BOOL_VEC2: 0x8B57,
                BOOL_VEC3: 0x8B58,
                BOOL_VEC4: 0x8B59,
                FLOAT_MAT2: 0x8B5A,
                FLOAT_MAT3: 0x8B5B,
                FLOAT_MAT4: 0x8B5C,
                SAMPLER_2D: 0x8B5E,
                SAMPLER_CUBE: 0x8B60,
                
                // Vertex attributes
                VERTEX_ATTRIB_ARRAY_ENABLED: 0x8622,
                VERTEX_ATTRIB_ARRAY_SIZE: 0x8623,
                VERTEX_ATTRIB_ARRAY_STRIDE: 0x8624,
                VERTEX_ATTRIB_ARRAY_TYPE: 0x8625,
                VERTEX_ATTRIB_ARRAY_NORMALIZED: 0x886A,
                VERTEX_ATTRIB_ARRAY_POINTER: 0x8645,
                VERTEX_ATTRIB_ARRAY_BUFFER_BINDING: 0x889F,
                
                // Implementation limits
                MAX_TEXTURE_SIZE: 0x0D33,
                MAX_VIEWPORT_DIMS: 0x0D3A,
                SUBPIXEL_BITS: 0x0D50,
                RED_BITS: 0x0D52,
                GREEN_BITS: 0x0D53,
                BLUE_BITS: 0x0D54,
                ALPHA_BITS: 0x0D55,
                DEPTH_BITS: 0x0D56,
                STENCIL_BITS: 0x0D57,
                
                // Framebuffers
                FRAMEBUFFER: 0x8D40,
                RENDERBUFFER: 0x8D41,
                RGBA4: 0x8056,
                RGB5_A1: 0x8057,
                RGB565: 0x8D62,
                DEPTH_COMPONENT16: 0x81A5,
                STENCIL_INDEX8: 0x8D48,
                COLOR_ATTACHMENT0: 0x8CE0,
                DEPTH_ATTACHMENT: 0x8D00,
                STENCIL_ATTACHMENT: 0x8D20,
                NONE: 0,
                FRAMEBUFFER_COMPLETE: 0x8CD5,
                
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
                clearDepth: (depth) => webgl2Bridge.clearDepth(depth),
                clearStencil: (stencil) => webgl2Bridge.clearStencil(stencil),
                compileShader: (shader) => webgl2Bridge.compileShader(shader),
                depthFunc: (func) => webgl2Bridge.depthFunc(func),
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
                texImage2D: (target, level, internalformat, width, height, border, format, type, pixels) => {
                    return webgl2Bridge.texImage2D(target, level, internalformat, width, height, border, format, type, pixels);
                },
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
                
                // Context attributes method required by Three.js
                getContextAttributes: () => ({
                    alpha: true,
                    antialias: true,
                    depth: true,
                    failIfMajorPerformanceCaveat: false,
                    powerPreference: 'default',
                    premultipliedAlpha: true,
                    preserveDrawingBuffer: false,
                    stencil: false,
                    desynchronized: false
                }),
                
                // Extension support method required by Three.js
                getExtension: (name) => {
                    // Return fake extension objects for common WebGL extensions
                    switch(name) {
                        case 'WEBGL_debug_renderer_info':
                            return {
                                UNMASKED_VENDOR_WEBGL: 0x9245,
                                UNMASKED_RENDERER_WEBGL: 0x9246
                            };
                        case 'EXT_texture_filter_anisotropic':
                        case 'WEBKIT_EXT_texture_filter_anisotropic':
                        case 'MOZ_EXT_texture_filter_anisotropic':
                            return {
                                TEXTURE_MAX_ANISOTROPY_EXT: 0x84FE,
                                MAX_TEXTURE_MAX_ANISOTROPY_EXT: 0x84FF
                            };
                        case 'WEBGL_compressed_texture_s3tc':
                            return {
                                COMPRESSED_RGB_S3TC_DXT1_EXT: 0x83F0,
                                COMPRESSED_RGBA_S3TC_DXT1_EXT: 0x83F1,
                                COMPRESSED_RGBA_S3TC_DXT3_EXT: 0x83F2,
                                COMPRESSED_RGBA_S3TC_DXT5_EXT: 0x83F3
                            };
                        case 'OES_vertex_array_object':
                            return {
                                createVertexArrayOES: () => webgl2Bridge.createVertexArray(),
                                deleteVertexArrayOES: (vao) => webgl2Bridge.deleteVertexArray(vao),
                                bindVertexArrayOES: (vao) => webgl2Bridge.bindVertexArray(vao)
                            };
                        case 'ANGLE_instanced_arrays':
                            return {
                                drawArraysInstancedANGLE: (mode, first, count, primcount) => 
                                    webgl2Bridge.drawArraysInstanced(mode, first, count, primcount),
                                drawElementsInstancedANGLE: (mode, count, type, offset, primcount) => 
                                    webgl2Bridge.drawElementsInstanced(mode, count, type, offset, primcount)
                            };
                        default:
                            return null; // Extension not supported
                    }
                },
                
                // Shader precision format method required by Three.js
                getShaderPrecisionFormat: (shaderType, precisionType) => {
                    // Return fake precision format info
                    return {
                        rangeMin: 127,
                        rangeMax: 127,
                        precision: 23
                    };
                },
                
                // Parameter query method required by Three.js
                getParameter: (pname) => {
                    if (pname === undefined || pname === null) {
                        return 0;
                    }
                    // Return appropriate values for common WebGL parameters
                    switch(pname) {
                        case 0x1F00: // GL_VENDOR
                            return 'LWJGL';
                        case 0x1F01: // GL_RENDERER
                            return 'LWJGL OpenGL Renderer';
                        case 0x1F02: // GL_VERSION
                            return 'WebGL 2.0 (OpenGL ES 3.0 Chromium)';
                        case 0x8B8C: // GL_SHADING_LANGUAGE_VERSION
                            return 'WebGL GLSL ES 3.00 (OpenGL ES GLSL ES 3.0 Chromium)';
                        case 0x0D33: // GL_MAX_TEXTURE_SIZE
                            return 16384;
                        case 0x851C: // GL_MAX_RENDERBUFFER_SIZE
                            return 16384;
                        case 0x80E9: // GL_MAX_TEXTURE_IMAGE_UNITS
                            return 32;
                        case 0x8872: // GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS
                            return 32;
                        case 0x8B4D: // GL_MAX_VERTEX_UNIFORM_VECTORS
                            return 1024;
                        case 0x8DFD: // GL_MAX_VARYING_VECTORS
                            return 30;
                        case 0x8B4C: // GL_MAX_FRAGMENT_UNIFORM_VECTORS
                            return 1024;
                        case 0x8869: // GL_MAX_VERTEX_ATTRIBS
                            return 16;
                        case 0x8DFB: // GL_MAX_VIEWPORT_DIMS
                            return new Int32Array([16384, 16384]);
                        case 0x846D: // GL_ALIASED_POINT_SIZE_RANGE
                            return new Float32Array([1, 1024]);
                        case 0x846E: // GL_ALIASED_LINE_WIDTH_RANGE
                            return new Float32Array([1, 1]);
                        case 0x8073: // GL_DEPTH_BITS
                            return 24;
                        case 0x8D48: // GL_STENCIL_BITS
                            return 8;
                        case 0x8D50: // GL_RED_BITS
                        case 0x8D51: // GL_GREEN_BITS
                        case 0x8D52: // GL_BLUE_BITS
                            return 8;
                        case 0x8D53: // GL_ALPHA_BITS
                            return 8;
                        case 0x8B8D: // GL_CURRENT_PROGRAM
                            return 0;
                        case 0x8CA6: // GL_ARRAY_BUFFER_BINDING
                        case 0x8CA7: // GL_ELEMENT_ARRAY_BUFFER_BINDING
                            return 0;
                        case 0x8069: // GL_TEXTURE_BINDING_2D
                            return 0;
                        case 0x84E0: // GL_ACTIVE_TEXTURE
                            return 0x84C0; // GL_TEXTURE0
                        default:
                            result = 0;
                            break;
                    }
                    return result;
                },
                
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
                    webgl2Bridge.uniformBlockBinding(program, blockIndex, binding),
                
                // HIGH PRIORITY: Buffer operations
                bufferSubData: (target, offset, data) => webgl2Bridge.bufferSubData(target, offset, data),
                getBufferSubData: (target, offset, returnedData) => webgl2Bridge.getBufferSubData(target, offset, returnedData),
                copyBufferSubData: (readTarget, writeTarget, readOffset, writeOffset, size) => 
                    webgl2Bridge.copyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size),
                
                // HIGH PRIORITY: Drawing functions
                drawRangeElements: (mode, start, end, count, type, offset) => 
                    webgl2Bridge.drawRangeElements(mode, start, end, count, type, offset),
                drawBuffers: (buffers) => webgl2Bridge.drawBuffers(buffers),
                clearBufferfv: (buffer, drawbuffer, value) => webgl2Bridge.clearBufferfv(buffer, drawbuffer, value),
                clearBufferiv: (buffer, drawbuffer, value) => webgl2Bridge.clearBufferiv(buffer, drawbuffer, value),
                clearBufferuiv: (buffer, drawbuffer, value) => webgl2Bridge.clearBufferuiv(buffer, drawbuffer, value),
                clearBufferfi: (buffer, drawbuffer, depth, stencil) => webgl2Bridge.clearBufferfi(buffer, drawbuffer, depth, stencil),
                
                // HIGH PRIORITY: Vertex attributes
                vertexAttribDivisor: (index, divisor) => webgl2Bridge.vertexAttribDivisor(index, divisor),
                vertexAttribIPointer: (index, size, type, stride, offset) => 
                    webgl2Bridge.vertexAttribIPointer(index, size, type, stride, offset),
                vertexAttribI4i: (index, x, y, z, w) => webgl2Bridge.vertexAttribI4i(index, x, y, z, w),
                vertexAttribI4ui: (index, x, y, z, w) => webgl2Bridge.vertexAttribI4ui(index, x, y, z, w),
                vertexAttribI4iv: (index, v) => webgl2Bridge.vertexAttribI4iv(index, v),
                vertexAttribI4uiv: (index, v) => webgl2Bridge.vertexAttribI4uiv(index, v),
                
                // HIGH PRIORITY: Validation functions
                isVertexArray: (vertexArray) => webgl2Bridge.isVertexArray(vertexArray),
                getIndexedParameter: (target, index) => webgl2Bridge.getIndexedParameter(target, index),
                
                // MISSING BASIC WebGL functions that Three.js requires
                frontFace: (mode) => webgl2Bridge.frontFace(mode),
                cullFace: (mode) => webgl2Bridge.cullFace(mode),
                blendFunc: (sfactor, dfactor) => webgl2Bridge.blendFunc(sfactor, dfactor),
                blendFuncSeparate: (srcRGB, dstRGB, srcAlpha, dstAlpha) => 
                    webgl2Bridge.blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha),
                blendEquation: (mode) => webgl2Bridge.blendEquation(mode),
                blendEquationSeparate: (modeRGB, modeAlpha) => webgl2Bridge.blendEquationSeparate(modeRGB, modeAlpha),
                blendColor: (red, green, blue, alpha) => webgl2Bridge.blendColor(red, green, blue, alpha),
                depthMask: (flag) => webgl2Bridge.depthMask(flag),
                colorMask: (red, green, blue, alpha) => webgl2Bridge.colorMask(red, green, blue, alpha),
                stencilFunc: (func, ref, mask) => webgl2Bridge.stencilFunc(func, ref, mask),
                stencilFuncSeparate: (face, func, ref, mask) => webgl2Bridge.stencilFuncSeparate(face, func, ref, mask),
                stencilOp: (fail, zfail, zpass) => webgl2Bridge.stencilOp(fail, zfail, zpass),
                stencilOpSeparate: (face, fail, zfail, zpass) => webgl2Bridge.stencilOpSeparate(face, fail, zfail, zpass),
                stencilMask: (mask) => webgl2Bridge.stencilMask(mask),
                stencilMaskSeparate: (face, mask) => webgl2Bridge.stencilMaskSeparate(face, mask),
                depthRange: (zNear, zFar) => webgl2Bridge.depthRange(zNear, zFar),
                polygonOffset: (factor, units) => webgl2Bridge.polygonOffset(factor, units),
                sampleCoverage: (value, invert) => webgl2Bridge.sampleCoverage(value, invert),
                scissor: (x, y, width, height) => webgl2Bridge.scissor(x, y, width, height),
                lineWidth: (width) => webgl2Bridge.lineWidth(width),
                pixelStorei: (pname, param) => webgl2Bridge.pixelStorei(pname, param),
                readPixels: (x, y, width, height, format, type, pixels) => 
                    webgl2Bridge.readPixels(x, y, width, height, format, type, pixels),
                
                // MEDIUM PRIORITY: Query objects
                createQuery: () => webgl2Bridge.createQuery(),
                deleteQuery: (query) => webgl2Bridge.deleteQuery(query),
                isQuery: (query) => webgl2Bridge.isQuery(query),
                beginQuery: (target, query) => webgl2Bridge.beginQuery(target, query),
                endQuery: (target) => webgl2Bridge.endQuery(target),
                getQuery: (target, pname) => webgl2Bridge.getQuery(target, pname),
                getQueryParameter: (query, pname) => webgl2Bridge.getQueryParameter(query, pname),
                
                // MEDIUM PRIORITY: Sampler objects
                createSampler: () => webgl2Bridge.createSampler(),
                deleteSampler: (sampler) => webgl2Bridge.deleteSampler(sampler),
                bindSampler: (unit, sampler) => webgl2Bridge.bindSampler(unit, sampler),
                isSampler: (sampler) => webgl2Bridge.isSampler(sampler),
                samplerParameteri: (sampler, pname, param) => webgl2Bridge.samplerParameteri(sampler, pname, param),
                samplerParameterf: (sampler, pname, param) => webgl2Bridge.samplerParameterf(sampler, pname, param),
                getSamplerParameter: (sampler, pname) => webgl2Bridge.getSamplerParameter(sampler, pname),
                
                // MEDIUM PRIORITY: Texture storage and operations
                texStorage2D: (target, levels, internalformat, width, height) => 
                    webgl2Bridge.texStorage2D(target, levels, internalformat, width, height),
                texStorage3D: (target, levels, internalformat, width, height, depth) => 
                    webgl2Bridge.texStorage3D(target, levels, internalformat, width, height, depth),
                copyTexSubImage3D: (target, level, xoffset, yoffset, zoffset, x, y, width, height) => 
                    webgl2Bridge.copyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height),
                compressedTexImage3D: (target, level, internalformat, width, height, depth, border, imageSize, data) => 
                    webgl2Bridge.compressedTexImage3D(target, level, internalformat, width, height, depth, border, imageSize, data),
                compressedTexSubImage3D: (target, level, xoffset, yoffset, zoffset, width, height, depth, format, imageSize, data) => 
                    webgl2Bridge.compressedTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, imageSize, data),
                
                // MEDIUM PRIORITY: Programs and shaders
                getFragDataLocation: (program, name) => webgl2Bridge.getFragDataLocation(program, name),
                
                // MEDIUM PRIORITY: Uniform buffer objects
                getUniformIndices: (program, uniformNames) => webgl2Bridge.getUniformIndices(program, uniformNames),
                getActiveUniforms: (program, uniformIndices, pname) => webgl2Bridge.getActiveUniforms(program, uniformIndices, pname),
                getActiveUniformBlockParameter: (program, uniformBlockIndex, pname) => 
                    webgl2Bridge.getActiveUniformBlockParameter(program, uniformBlockIndex, pname),
                getActiveUniformBlockName: (program, uniformBlockIndex) => 
                    webgl2Bridge.getActiveUniformBlockName(program, uniformBlockIndex),
                
                // LOW PRIORITY: Transform feedback
                createTransformFeedback: () => webgl2Bridge.createTransformFeedback(),
                deleteTransformFeedback: (transformFeedback) => webgl2Bridge.deleteTransformFeedback(transformFeedback),
                isTransformFeedback: (transformFeedback) => webgl2Bridge.isTransformFeedback(transformFeedback),
                bindTransformFeedback: (target, transformFeedback) => webgl2Bridge.bindTransformFeedback(target, transformFeedback),
                transformFeedbackVaryings: (program, varyings, bufferMode) => 
                    webgl2Bridge.transformFeedbackVaryings(program, varyings, bufferMode),
                getTransformFeedbackVarying: (program, index) => webgl2Bridge.getTransformFeedbackVarying(program, index),
                pauseTransformFeedback: () => webgl2Bridge.pauseTransformFeedback(),
                resumeTransformFeedback: () => webgl2Bridge.resumeTransformFeedback(),
                
                // LOW PRIORITY: Sync objects
                fenceSync: (condition, flags) => webgl2Bridge.fenceSync(condition, flags),
                isSync: (sync) => webgl2Bridge.isSync(sync),
                deleteSync: (sync) => webgl2Bridge.deleteSync(sync),
                clientWaitSync: (sync, flags, timeout) => webgl2Bridge.clientWaitSync(sync, flags, timeout),
                waitSync: (sync, flags, timeout) => webgl2Bridge.waitSync(sync, flags, timeout),
                getSyncParameter: (sync, pname) => webgl2Bridge.getSyncParameter(sync, pname),
                
                // LOW PRIORITY: Framebuffer operations
                blitFramebuffer: (srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter) => 
                    webgl2Bridge.blitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter),
                framebufferTextureLayer: (target, attachment, texture, level, layer) => 
                    webgl2Bridge.framebufferTextureLayer(target, attachment, texture, level, layer),
                invalidateFramebuffer: (target, attachments) => webgl2Bridge.invalidateFramebuffer(target, attachments),
                invalidateSubFramebuffer: (target, attachments, x, y, width, height) => 
                    webgl2Bridge.invalidateSubFramebuffer(target, attachments, x, y, width, height),
                readBuffer: (mode) => webgl2Bridge.readBuffer(mode),
                
                // LOW PRIORITY: Renderbuffer operations
                getInternalformatParameter: (target, internalformat, pname) => 
                    webgl2Bridge.getInternalformatParameter(target, internalformat, pname),
                renderbufferStorageMultisample: (target, samples, internalformat, width, height) => 
                    webgl2Bridge.renderbufferStorageMultisample(target, samples, internalformat, width, height),
                
                // LOW PRIORITY: Additional uniform functions
                uniform1ui: (location, v0) => webgl2Bridge.uniform1ui(location, v0),
                uniform2ui: (location, v0, v1) => webgl2Bridge.uniform2ui(location, v0, v1),
                uniform3ui: (location, v0, v1, v2) => webgl2Bridge.uniform3ui(location, v0, v1, v2),
                uniform4ui: (location, v0, v1, v2, v3) => webgl2Bridge.uniform4ui(location, v0, v1, v2, v3),
                uniform1uiv: (location, value) => webgl2Bridge.uniform1uiv(location, value),
                uniform2uiv: (location, value) => webgl2Bridge.uniform2uiv(location, value),
                uniform3uiv: (location, value) => webgl2Bridge.uniform3uiv(location, value),
                uniform4uiv: (location, value) => webgl2Bridge.uniform4uiv(location, value),
                
                // LOW PRIORITY: Additional matrix uniform functions
                uniformMatrix2x3fv: (location, transpose, data) => webgl2Bridge.uniformMatrix2x3fv(location, transpose, data),
                uniformMatrix3x2fv: (location, transpose, data) => webgl2Bridge.uniformMatrix3x2fv(location, transpose, data),
                uniformMatrix2x4fv: (location, transpose, data) => webgl2Bridge.uniformMatrix2x4fv(location, transpose, data),
                uniformMatrix4x2fv: (location, transpose, data) => webgl2Bridge.uniformMatrix4x2fv(location, transpose, data),
                uniformMatrix3x4fv: (location, transpose, data) => webgl2Bridge.uniformMatrix3x4fv(location, transpose, data),
                uniformMatrix4x3fv: (location, transpose, data) => webgl2Bridge.uniformMatrix4x3fv(location, transpose, data),

                // FRAMEBUFFER AND RENDERBUFFER FUNCTIONS
                createFramebuffer: () => webgl2Bridge.createFramebuffer(),
                deleteFramebuffer: (framebuffer) => webgl2Bridge.deleteFramebuffer(framebuffer),
                bindFramebuffer: (target, framebuffer) => webgl2Bridge.bindFramebuffer(target, framebuffer),
                isFramebuffer: (framebuffer) => webgl2Bridge.isFramebuffer(framebuffer),
                framebufferTexture2D: (target, attachment, textarget, texture, level) => webgl2Bridge.framebufferTexture2D(target, attachment, textarget, texture, level),
                framebufferRenderbuffer: (target, attachment, renderbuffertarget, renderbuffer) => webgl2Bridge.framebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer),
                checkFramebufferStatus: (target) => webgl2Bridge.checkFramebufferStatus(target),
                getFramebufferAttachmentParameter: (target, attachment, pname) => webgl2Bridge.getFramebufferAttachmentParameter(target, attachment, pname),
                createRenderbuffer: () => webgl2Bridge.createRenderbuffer(),
                deleteRenderbuffer: (renderbuffer) => webgl2Bridge.deleteRenderbuffer(renderbuffer),
                bindRenderbuffer: (target, renderbuffer) => webgl2Bridge.bindRenderbuffer(target, renderbuffer),
                isRenderbuffer: (renderbuffer) => webgl2Bridge.isRenderbuffer(renderbuffer),
                renderbufferStorage: (target, internalformat, width, height) => webgl2Bridge.renderbufferStorage(target, internalformat, width, height),
                getRenderbufferParameter: (target, pname) => webgl2Bridge.getRenderbufferParameter(target, pname),

                // SHADER UNIFORM AND ATTRIBUTE FUNCTIONS
                getActiveUniform: (program, index) => webgl2Bridge.getActiveUniform(program, index),
                getActiveAttrib: (program, index) => webgl2Bridge.getActiveAttrib(program, index),
                getUniformLocation: (program, name) => webgl2Bridge.getUniformLocation(program, name),
                getAttribLocation: (program, name) => webgl2Bridge.getAttribLocation(program, name),
                getProgramParameter: (program, pname) => webgl2Bridge.getProgramParameter(program, pname),
                getShaderParameter: (shader, pname) => webgl2Bridge.getShaderParameter(shader, pname),
                getProgramInfoLog: (program) => webgl2Bridge.getProgramInfoLog(program),
                getShaderInfoLog: (shader) => webgl2Bridge.getShaderInfoLog(shader),

                // BASIC UNIFORM MATRIX FUNCTIONS
                uniformMatrix2fv: (location, transpose, data) => webgl2Bridge.uniformMatrix2fv(location, transpose, data),
                uniformMatrix3fv: (location, transpose, data) => webgl2Bridge.uniformMatrix3fv(location, transpose, data),
                uniformMatrix4fv: (location, transpose, data) => webgl2Bridge.uniformMatrix4fv(location, transpose, data)
            };
            
            // Make gl globally available
            globalThis.gl = gl;
            
            // Also expose WebGL constants globally for Three.js
            globalThis.GL_VERSION = 0x1F02;
            globalThis.GL_VENDOR = 0x1F00;
            globalThis.GL_RENDERER = 0x1F01;
            globalThis.GL_SHADING_LANGUAGE_VERSION = 0x8B8C;
            
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
        """;
        
        jsContext.eval("js", webgl2Script);
    }
    
    /**
     * Execute JavaScript code with WebGL2 context available
     */
    public Value executeScript(String script) {
        return jsContext.eval("js", script);
    }
    
    /**
     * Execute ES6 module code with WebGL2 context available
     */
    public Value executeModule(String moduleCode) {
        try {
            return jsContext.eval(org.graalvm.polyglot.Source.newBuilder("js", moduleCode, "module.mjs")
                .mimeType("application/javascript+module")
                .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute ES6 module", e);
        }
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
