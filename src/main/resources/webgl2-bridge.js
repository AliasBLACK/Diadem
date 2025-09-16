/**
 * WebGL2 JavaScript Bridge for GLES Functions
 * Maps WebGL2 API calls to Java GLES implementation
 */

class WebGL2Bridge {
    constructor() {
        // Direct access to LWJGL OpenGL methods via Java.type
        this.GL11 = Java.type('org.lwjgl.opengl.GL11');
        this.GL15 = Java.type('org.lwjgl.opengl.GL15');
        this.GL20 = Java.type('org.lwjgl.opengl.GL20');
        this.GL30 = Java.type('org.lwjgl.opengl.GL30');
        
        // Create an instance of the Adapter class
        const AdapterClass = Java.type('black.alias.diadem.Adapter');
        this.adapter = new AdapterClass();
        
        // Import BufferUtils for buffer conversion
        this.BufferUtils = Java.type('black.alias.diadem.BufferUtils');
    }

    setupConstants() {
        // WebGL2 Constants mapped to OpenGL ES constants
        this.DEPTH_BUFFER_BIT = 0x00000100;
        this.STENCIL_BUFFER_BIT = 0x00000400;
        this.COLOR_BUFFER_BIT = 0x00004000;
        this.TRIANGLES = 0x0004;
        this.TRIANGLE_STRIP = 0x0005;
        this.TRIANGLE_FAN = 0x0006;
        this.UNSIGNED_BYTE = 0x1401;
        this.UNSIGNED_SHORT = 0x1403;
        this.FLOAT = 0x1406;
        this.RGBA = 0x1908;
        this.TEXTURE_2D = 0x0DE1;
        this.TEXTURE_CUBE_MAP = 0x8513;
        this.ARRAY_BUFFER = 0x8892;
        this.ELEMENT_ARRAY_BUFFER = 0x8893;
        this.STATIC_DRAW = 0x88E4;
        this.DYNAMIC_DRAW = 0x88E8;
        this.VERTEX_SHADER = 0x8B31;
        this.FRAGMENT_SHADER = 0x8B30;
    }

    // Core WebGL2 Functions
    activeTexture(texture) {
        this.GL11.glActiveTexture(texture ? texture : 0);
    }

    attachShader(program, shader) {
        this.GL20.glAttachShader(program ? program : 0, shader ? shader : 0);
    }

    bindBuffer(target, buffer) {
        this.GL15.glBindBuffer(target ? target : 0, buffer ? buffer : 0);
    }

    bindTexture(target, texture) {
        this.GL11.glBindTexture(target ? target : 0, texture ? texture : 0);
    }

    bufferData(target, data, usage) {
        if (data instanceof ArrayBuffer || data instanceof Float32Array || data instanceof Uint16Array) {
            // Convert JavaScript typed arrays to Java buffers
            const BufferUtils = Java.type('black.alias.diadem.BufferUtils');
            if (data instanceof Float32Array) {
                console.log('bufferData: Float32Array with', data.length, 'elements');
                console.log('First few values:', data[0], data[1], data[2], data[3], data[4], data[5]);
                const buffer = BufferUtils.newFloatBuffer(data.length);
                for (let i = 0; i < data.length; i++) {
                    buffer.put(i, data[i]);
                }
                console.log('Buffer created and filled, calling glBufferData...');
                this.GL15.glBufferData(target ? target : 0, buffer, usage ? usage : 0);
                console.log('glBufferData completed');
            } else if (data instanceof Uint16Array) {
                console.log('bufferData: Uint16Array with', data.length, 'elements');
                console.log('First few values:', data[0], data[1], data[2], data[3]);
                const buffer = BufferUtils.newShortBuffer(data.length);
                for (let i = 0; i < data.length; i++) {
                    buffer.put(i, data[i]);
                }
                console.log('Index buffer created and filled, calling glBufferData...');
                this.GL15.glBufferData(target ? target : 0, buffer, usage ? usage : 0);
                console.log('Index glBufferData completed');
            } else {
                // ArrayBuffer
                const buffer = BufferUtils.newByteBuffer(data.byteLength);
                const view = new Uint8Array(data);
                for (let i = 0; i < view.length; i++) {
                    buffer.put(i, view[i]);
                }
                this.GL15.glBufferData(target ? target : 0, buffer, usage ? usage : 0);
            }
        } else if (typeof data === 'number') {
            this.GL15.glBufferData(target ? target : 0, data, usage ? usage : 0);
        }
    }

    clear(mask) {
        this.GL11.glClear(mask ? mask : 0);
    }

    clearColor(red, green, blue, alpha) {
        this.adapter.glClearColor(red, green, blue, alpha);
    }

    clearDepth(depth) {
        this.GL11.glClearDepth(depth !== undefined ? depth : 1.0);
    }

    clearStencil(stencil) {
        this.GL11.glClearStencil(stencil !== undefined ? stencil : 0);
    }

    depthFunc(func) {
        this.GL11.glDepthFunc(func ? func : 0);
    }

    compileShader(shader) {
        this.GL20.glCompileShader(shader ? shader : 0);
    }

    createBuffer() {
        return this.GL15.glGenBuffers();
    }

    createProgram() {
        return this.GL20.glCreateProgram();
    }

    createShader(type) {
        return this.GL20.glCreateShader(type ? type : 0);
    }

    createTexture() {
        return this.GL11.glGenTextures();
    }

    deleteBuffer(buffer) {
        this.GL15.glDeleteBuffers(buffer ? buffer : 0);
    }

    deleteProgram(program) {
        this.GL20.glDeleteProgram(program ? program : 0);
    }

    deleteShader(shader) {
        this.GL20.glDeleteShader(shader ? shader : 0);
    }

    deleteTexture(texture) {
        this.GL11.glDeleteTextures(texture ? texture : 0);
    }

    drawArrays(mode, first, count) {
        this.GL11.glDrawArrays(mode ? mode : 0, first ? first : 0, count ? count : 0);
    }

    drawElements(mode, count, type, offset) {
        this.GL11.glDrawElements(mode ? mode : 0, count ? count : 0, type ? type : 0, offset ? offset : 0);
    }

    enable(cap) {
        this.GL11.glEnable(cap ? cap : 0);
    }

    disable(cap) {
        this.GL11.glDisable(cap ? cap : 0);
    }

    enableVertexAttribArray(index) {
        this.GL20.glEnableVertexAttribArray(index ? index : 0);
    }

    disableVertexAttribArray(index) {
        this.GL20.glDisableVertexAttribArray(index ? index : 0);
    }

    getAttribLocation(program, name) {
        return this.GL20.glGetAttribLocation(program ? program : 0, name ? name : "");
    }

    getUniformLocation(program, name) {
        return this.GL20.glGetUniformLocation(program ? program : 0, name ? name : "");
    }

    getShaderParameter(shader, pname) {
        // LWJGL glGetShaderiv requires a buffer to store the result
        const BufferUtils = Java.type('black.alias.diadem.BufferUtils');
        const buffer = BufferUtils.newIntBuffer(1);
        this.GL20.glGetShaderiv(shader ? shader : 0, pname ? pname : 0, buffer);
        return buffer.get(0);
    }

    getProgramParameter(program, pname) {
        // LWJGL glGetProgramiv requires a buffer to store the result
        const BufferUtils = Java.type('black.alias.diadem.BufferUtils');
        const buffer = BufferUtils.newIntBuffer(1);
        this.GL20.glGetProgramiv(program ? program : 0, pname ? pname : 0, buffer);
        return buffer.get(0);
    }

    getShaderInfoLog(shader) {
        return this.GL20.glGetShaderInfoLog(shader ? shader : 0);
    }

    getProgramInfoLog(program) {
        return this.GL20.glGetProgramInfoLog(program ? program : 0);
    }

    linkProgram(program) {
        this.GL20.glLinkProgram(program ? program : 0);
    }

    shaderSource(shader, source) {
        this.GL20.glShaderSource(shader ? shader : 0, source ? source : "");
    }

    texImage2D(target, level, internalformat, width, height, border, format, type, pixels) {
        if (arguments.length === 6) {
            // texImage2D(target, level, internalformat, format, type, source)
            // For the 6-parameter version, pass null for pixels
            this.adapter.glTexImage2D(target ? target : 0, level ? level : 0, internalformat ? internalformat : 0, width ? width : 0, height ? height : 0, format ? format : 0, null);
        } else {
            // For null/undefined pixels, pass null directly
            if (pixels === null || pixels === undefined) {
                this.adapter.glTexImage2D(target ? target : 0, level ? level : 0, internalformat ? internalformat : 0, 
                    width ? width : 0, height ? height : 0, border ? border : 0, format ? format : 0, type ? type : 0, null);
            } else {
                // Convert JavaScript typed array to Java ByteBuffer using BufferUtils
                let buffer = null;
                if (pixels && typeof pixels === 'object' && pixels.length !== undefined) {
                    // Handle JavaScript typed arrays by converting to ByteBuffer
                    buffer = this.BufferUtils.newByteBuffer(pixels.length * 4); // Assume 4 bytes per element for safety
                    for (let i = 0; i < pixels.length; i++) {
                        buffer.putFloat(pixels[i]);
                    }
                    buffer.flip();
                }
                this.adapter.glTexImage2D(target ? target : 0, level ? level : 0, internalformat ? internalformat : 0, 
                    width ? width : 0, height ? height : 0, border ? border : 0, format ? format : 0, type ? type : 0, buffer);
            }
        }
    }

    texParameteri(target, pname, param) {
        this.GL11.glTexParameteri(target ? target : 0, pname ? pname : 0, param ? param : 0);
    }

    uniform1f(location, value) {
        this.GL20.glUniform1f(location ? location : -1, value ? value : 0.0);
    }

    uniform1i(location, value) {
        this.GL20.glUniform1i(location ? location : -1, value ? value : 0);
    }

    uniform2f(location, x, y) {
        this.GL20.glUniform2f(location ? location : -1, x ? x : 0.0, y ? y : 0.0);
    }

    uniform3f(location, x, y, z) {
        this.GL20.glUniform3f(location ? location : -1, x ? x : 0.0, y ? y : 0.0, z ? z : 0.0);
    }

    uniform4f(location, x, y, z, w) {
        this.GL20.glUniform4f(location ? location : -1, x ? x : 0.0, y ? y : 0.0, z ? z : 0.0, w ? w : 0.0);
    }

    uniformMatrix4fv(location, transpose, value) {
        // LWJGL glUniformMatrix4fv takes (location, count, transpose, value)
        // WebGL uniformMatrix4fv takes (location, transpose, value)
        this.GL20.glUniformMatrix4fv(location ? location : -1, transpose ? transpose : false, value);
    }

    useProgram(program) {
        this.GL20.glUseProgram(program ? program : 0);
    }

    vertexAttribPointer(index, size, type, normalized, stride, offset) {
        this.GL20.glVertexAttribPointer(index ? index : 0, size ? size : 0, type ? type : 0, normalized ? normalized : false, stride ? stride : 0, offset ? offset : 0);
    }

    viewport(x, y, width, height) {
        this.GL11.glViewport(x ? x : 0, y ? y : 0, width ? width : 0, height ? height : 0);
    }

    // WebGL2 specific functions
    bindVertexArray(vertexArray) {
        this.GL30.glBindVertexArray(vertexArray ? vertexArray : 0);
    }

    createVertexArray() {
        return this.GL30.glGenVertexArrays();
    }

    deleteVertexArray(vertexArray) {
        this.GL30.glDeleteVertexArrays(vertexArray ? vertexArray : 0);
    }

    texImage3D(target, level, internalformat, width, height, depth, border, format, type, pixels) {
        // For null/undefined pixels, pass null directly
        if (pixels === null || pixels === undefined) {
            this.adapter.glTexImage3D(target ? target : 0, level ? level : 0, internalformat ? internalformat : 0, 
                width ? width : 0, height ? height : 0, depth ? depth : 0, border ? border : 0, format ? format : 0, type ? type : 0, null);
        } else {
            // Convert JavaScript typed array to Java ByteBuffer using BufferUtils
            let buffer = null;
            if (pixels && typeof pixels === 'object' && pixels.length !== undefined) {
                // Handle JavaScript typed arrays by converting to ByteBuffer
                buffer = this.BufferUtils.newByteBuffer(pixels.length * 4); // Assume 4 bytes per element for safety
                for (let i = 0; i < pixels.length; i++) {
                    buffer.putFloat(pixels[i]);
                }
                buffer.flip();
            }
            this.adapter.glTexImage3D(target ? target : 0, level ? level : 0, internalformat ? internalformat : 0, 
                width ? width : 0, height ? height : 0, depth ? depth : 0, border ? border : 0, format ? format : 0, type ? type : 0, buffer);
        }
    }

    texSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels) {
        // WebGL2 3D texture support - use GL12 for 3D textures
        this.GL12 = this.GL12 || Java.type('org.lwjgl.opengl.GL12');
        this.GL12.glTexSubImage3D(target ? target : 0, level ? level : 0, xoffset ? xoffset : 0, yoffset ? yoffset : 0, zoffset ? zoffset : 0, width ? width : 0, height ? height : 0, depth ? depth : 0, format ? format : 0, type ? type : 0, pixels);
    }

    drawArraysInstanced(mode, first, count, instanceCount) {
        // WebGL2 instanced rendering - use GL31 for instanced drawing
        this.GL31 = this.GL31 || Java.type('org.lwjgl.opengl.GL31');
        this.GL31.glDrawArraysInstanced(mode ? mode : 0, first ? first : 0, count ? count : 0, instanceCount ? instanceCount : 0);
    }

    drawElementsInstanced(mode, count, type, offset, instanceCount) {
        // WebGL2 instanced rendering - use GL31 for instanced drawing
        this.GL31 = this.GL31 || Java.type('org.lwjgl.opengl.GL31');
        this.GL31.glDrawElementsInstanced(mode ? mode : 0, count ? count : 0, type ? type : 0, offset ? offset : 0, instanceCount ? instanceCount : 0);
    }

    // Transform feedback
    beginTransformFeedback(primitiveMode) {
        // WebGL2 transform feedback - use GL30 for transform feedback
        this.GL30.glBeginTransformFeedback(primitiveMode ? primitiveMode : 0);
    }

    endTransformFeedback() {
        // WebGL2 transform feedback - use GL30 for transform feedback
        this.GL30.glEndTransformFeedback();
    }

    // Uniform buffer objects
    bindBufferBase(target, index, buffer) {
        // WebGL2 uniform buffer objects - use GL30 for UBOs
        this.GL30.glBindBufferBase(target ? target : 0, index ? index : 0, buffer ? buffer : 0);
    }

    bindBufferRange(target, index, buffer, offset, size) {
        // WebGL2 uniform buffer objects - use GL30 for UBOs
        this.GL30.glBindBufferRange(target ? target : 0, index ? index : 0, buffer ? buffer : 0, offset ? offset : 0, size ? size : 0);
    }

    getUniformBlockIndex(program, uniformBlockName) {
        // WebGL2 uniform buffer objects - use GL31 for uniform blocks
        this.GL31 = this.GL31 || Java.type('org.lwjgl.opengl.GL31');
        return this.GL31.glGetUniformBlockIndex(program ? program : 0, uniformBlockName ? uniformBlockName : "");
    }

    uniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding) {
        // WebGL2 uniform buffer objects - use GL31 for uniform blocks
        this.GL31 = this.GL31 || Java.type('org.lwjgl.opengl.GL31');
        this.GL31.glUniformBlockBinding(program ? program : 0, uniformBlockIndex ? uniformBlockIndex : 0, uniformBlockBinding ? uniformBlockBinding : 0);
    }

    // HIGH PRIORITY: Buffer operations
    bufferSubData(target, offset, data) {
        if (data instanceof Float32Array) {
            const buffer = this.BufferUtils.newFloatBuffer(data.length);
            for (let i = 0; i < data.length; i++) {
                buffer.put(i, data[i]);
            }
            this.GL15.glBufferSubData(target ? target : 0, offset ? offset : 0, buffer);
        } else if (data instanceof Uint16Array) {
            const buffer = this.BufferUtils.newShortBuffer(data.length);
            for (let i = 0; i < data.length; i++) {
                buffer.put(i, data[i]);
            }
            this.GL15.glBufferSubData(target ? target : 0, offset ? offset : 0, buffer);
        } else if (data instanceof ArrayBuffer) {
            const buffer = this.BufferUtils.newByteBuffer(data.byteLength);
            const view = new Uint8Array(data);
            for (let i = 0; i < view.length; i++) {
                buffer.put(i, view[i]);
            }
            this.GL15.glBufferSubData(target ? target : 0, offset ? offset : 0, buffer);
        }
    }

    getBufferSubData(target, offset, returnedData) {
        // WebGL2 buffer data readback - use GL15
        if (returnedData instanceof Float32Array) {
            const buffer = this.BufferUtils.newFloatBuffer(returnedData.length);
            this.GL15.glGetBufferSubData(target ? target : 0, offset ? offset : 0, buffer);
            for (let i = 0; i < returnedData.length; i++) {
                returnedData[i] = buffer.get(i);
            }
        } else if (returnedData instanceof Uint16Array) {
            const buffer = this.BufferUtils.newShortBuffer(returnedData.length);
            this.GL15.glGetBufferSubData(target ? target : 0, offset ? offset : 0, buffer);
            for (let i = 0; i < returnedData.length; i++) {
                returnedData[i] = buffer.get(i);
            }
        } else if (returnedData instanceof ArrayBuffer) {
            const buffer = this.BufferUtils.newByteBuffer(returnedData.byteLength);
            this.GL15.glGetBufferSubData(target ? target : 0, offset ? offset : 0, buffer);
            const view = new Uint8Array(returnedData);
            for (let i = 0; i < view.length; i++) {
                view[i] = buffer.get(i);
            }
        }
    }

    copyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size) {
        // WebGL2 buffer copy - use GL31
        this.GL31 = this.GL31 || Java.type('org.lwjgl.opengl.GL31');
        this.GL31.glCopyBufferSubData(readTarget ? readTarget : 0, writeTarget ? writeTarget : 0, 
            readOffset ? readOffset : 0, writeOffset ? writeOffset : 0, size ? size : 0);
    }

    // HIGH PRIORITY: Drawing functions
    drawRangeElements(mode, start, end, count, type, offset) {
        // WebGL2 optimized indexed drawing - use GL12
        this.GL12 = this.GL12 || Java.type('org.lwjgl.opengl.GL12');
        this.GL12.glDrawRangeElements(mode ? mode : 0, start ? start : 0, end ? end : 0, 
            count ? count : 0, type ? type : 0, offset ? offset : 0);
    }

    drawBuffers(buffers) {
        // WebGL2 multiple render targets - use GL20
        if (buffers && buffers.length) {
            const buffer = this.BufferUtils.newIntBuffer(buffers.length);
            for (let i = 0; i < buffers.length; i++) {
                buffer.put(i, buffers[i]);
            }
            this.GL20.glDrawBuffers(buffer);
        }
    }

    clearBufferfv(buffer, drawbuffer, value) {
        // WebGL2 clear specific buffer with float values - use GL30
        if (value && value.length) {
            const floatBuffer = this.BufferUtils.newFloatBuffer(value.length);
            for (let i = 0; i < value.length; i++) {
                floatBuffer.put(i, value[i]);
            }
            this.GL30.glClearBufferfv(buffer ? buffer : 0, drawbuffer ? drawbuffer : 0, floatBuffer);
        }
    }

    clearBufferiv(buffer, drawbuffer, value) {
        // WebGL2 clear specific buffer with int values - use GL30
        if (value && value.length) {
            const intBuffer = this.BufferUtils.newIntBuffer(value.length);
            for (let i = 0; i < value.length; i++) {
                intBuffer.put(i, value[i]);
            }
            this.GL30.glClearBufferiv(buffer ? buffer : 0, drawbuffer ? drawbuffer : 0, intBuffer);
        }
    }

    clearBufferuiv(buffer, drawbuffer, value) {
        // WebGL2 clear specific buffer with uint values - use GL30
        if (value && value.length) {
            const intBuffer = this.BufferUtils.newIntBuffer(value.length);
            for (let i = 0; i < value.length; i++) {
                intBuffer.put(i, value[i]);
            }
            this.GL30.glClearBufferuiv(buffer ? buffer : 0, drawbuffer ? drawbuffer : 0, intBuffer);
        }
    }

    clearBufferfi(buffer, drawbuffer, depth, stencil) {
        // WebGL2 clear depth-stencil buffer - use GL30
        this.GL30.glClearBufferfi(buffer ? buffer : 0, drawbuffer ? drawbuffer : 0, 
            depth !== undefined ? depth : 1.0, stencil !== undefined ? stencil : 0);
    }

    // HIGH PRIORITY: Vertex attributes
    vertexAttribDivisor(index, divisor) {
        // WebGL2 instanced vertex attributes - use GL33
        this.GL33 = this.GL33 || Java.type('org.lwjgl.opengl.GL33');
        this.GL33.glVertexAttribDivisor(index ? index : 0, divisor ? divisor : 0);
    }

    vertexAttribIPointer(index, size, type, stride, offset) {
        // WebGL2 integer vertex attributes - use GL30
        this.GL30.glVertexAttribIPointer(index ? index : 0, size ? size : 0, type ? type : 0, 
            stride ? stride : 0, offset ? offset : 0);
    }

    vertexAttribI4i(index, x, y, z, w) {
        // WebGL2 integer vertex attribute values - use GL30
        this.GL30.glVertexAttribI4i(index ? index : 0, x ? x : 0, y ? y : 0, z ? z : 0, w ? w : 0);
    }

    vertexAttribI4ui(index, x, y, z, w) {
        // WebGL2 unsigned integer vertex attribute values - use GL30
        this.GL30.glVertexAttribI4ui(index ? index : 0, x ? x : 0, y ? y : 0, z ? z : 0, w ? w : 0);
    }

    vertexAttribI4iv(index, v) {
        // WebGL2 integer vertex attribute vector - use GL30
        if (v && v.length >= 4) {
            this.GL30.glVertexAttribI4i(index ? index : 0, v[0], v[1], v[2], v[3]);
        }
    }

    vertexAttribI4uiv(index, v) {
        // WebGL2 unsigned integer vertex attribute vector - use GL30
        if (v && v.length >= 4) {
            this.GL30.glVertexAttribI4ui(index ? index : 0, v[0], v[1], v[2], v[3]);
        }
    }

    // HIGH PRIORITY: Validation functions
    isVertexArray(vertexArray) {
        // WebGL2 VAO validation - use GL30
        return this.GL30.glIsVertexArray(vertexArray ? vertexArray : 0);
    }

    getIndexedParameter(target, index) {
        // WebGL2 indexed parameter queries - use GL30
        const buffer = this.BufferUtils.newIntBuffer(1);
        this.GL30.glGetIntegeri_v(target ? target : 0, index ? index : 0, buffer);
        return buffer.get(0);
    }

    // MISSING BASIC WebGL functions that Three.js requires
    frontFace(mode) {
        this.GL11.glFrontFace(mode ? mode : 0);
    }

    cullFace(mode) {
        this.GL11.glCullFace(mode ? mode : 0);
    }

    blendFunc(sfactor, dfactor) {
        this.GL11.glBlendFunc(sfactor ? sfactor : 0, dfactor ? dfactor : 0);
    }

    blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha) {
        this.GL14 = this.GL14 || Java.type('org.lwjgl.opengl.GL14');
        this.GL14.glBlendFuncSeparate(srcRGB ? srcRGB : 0, dstRGB ? dstRGB : 0, srcAlpha ? srcAlpha : 0, dstAlpha ? dstAlpha : 0);
    }

    blendEquation(mode) {
        this.GL14 = this.GL14 || Java.type('org.lwjgl.opengl.GL14');
        this.GL14.glBlendEquation(mode ? mode : 0);
    }

    blendEquationSeparate(modeRGB, modeAlpha) {
        this.GL20.glBlendEquationSeparate(modeRGB ? modeRGB : 0, modeAlpha ? modeAlpha : 0);
    }

    blendColor(red, green, blue, alpha) {
        this.GL14 = this.GL14 || Java.type('org.lwjgl.opengl.GL14');
        this.adapter.glBlendColor(red, green, blue, alpha);
    }

    depthMask(flag) {
        this.GL11.glDepthMask(flag ? flag : false);
    }

    colorMask(red, green, blue, alpha) {
        this.GL11.glColorMask(red ? red : false, green ? green : false, blue ? blue : false, alpha ? alpha : false);
    }

    stencilFunc(func, ref, mask) {
        // Convert mask to proper Java int using Java Integer class
        const Integer = Java.type('java.lang.Integer');
        const intMask = mask ? Integer.valueOf(Math.floor(mask) & 0xFFFFFFFF) : 0;
        this.GL11.glStencilFunc(func ? func : 0, ref ? ref : 0, intMask);
    }

    stencilFuncSeparate(face, func, ref, mask) {
        // Convert mask to proper Java int using Java Integer class
        const Integer = Java.type('java.lang.Integer');
        const intMask = mask ? Integer.valueOf(Math.floor(mask) & 0xFFFFFFFF) : 0;
        this.GL20.glStencilFuncSeparate(face ? face : 0, func ? func : 0, ref ? ref : 0, intMask);
    }

    stencilOp(fail, zfail, zpass) {
        this.GL11.glStencilOp(fail ? fail : 0, zfail ? zfail : 0, zpass ? zpass : 0);
    }

    stencilOpSeparate(face, fail, zfail, zpass) {
        this.GL20.glStencilOpSeparate(face ? face : 0, fail ? fail : 0, zfail ? zfail : 0, zpass ? zpass : 0);
    }

    stencilMask(mask) {
        // Convert JavaScript number to proper Java int using Java Integer class
        const Integer = Java.type('java.lang.Integer');
        const intMask = mask ? Integer.valueOf(Math.floor(mask) & 0xFFFFFFFF) : 0;
        this.GL11.glStencilMask(intMask);
    }

    stencilMaskSeparate(face, mask) {
        // Convert JavaScript number to proper Java int using Java Integer class
        const Integer = Java.type('java.lang.Integer');
        const intMask = mask ? Integer.valueOf(Math.floor(mask) & 0xFFFFFFFF) : 0;
        this.GL20.glStencilMaskSeparate(face, intMask);
    }

    depthRange(zNear, zFar) {
        this.GL11.glDepthRange(zNear !== undefined ? zNear : 0.0, zFar !== undefined ? zFar : 1.0);
    }

    polygonOffset(factor, units) {
        this.GL11.glPolygonOffset(factor !== undefined ? factor : 0.0, units !== undefined ? units : 0.0);
    }

    sampleCoverage(value, invert) {
        this.GL13 = this.GL13 || Java.type('org.lwjgl.opengl.GL13');
        this.GL13.glSampleCoverage(value !== undefined ? value : 1.0, invert ? invert : false);
    }

    scissor(x, y, width, height) {
        this.GL11.glScissor(x ? x : 0, y ? y : 0, width ? width : 0, height ? height : 0);
    }

    lineWidth(width) {
        this.GL11.glLineWidth(width !== undefined ? width : 1.0);
    }

    pixelStorei(pname, param) {
        this.GL11.glPixelStorei(pname ? pname : 0, param ? param : 0);
    }

    readPixels(x, y, width, height, format, type, pixels) {
        if (pixels && pixels.length) {
            if (pixels instanceof Uint8Array) {
                const buffer = this.BufferUtils.newByteBuffer(pixels.length);
                this.GL11.glReadPixels(x ? x : 0, y ? y : 0, width ? width : 0, height ? height : 0, 
                    format ? format : 0, type ? type : 0, buffer);
                for (let i = 0; i < pixels.length; i++) {
                    pixels[i] = buffer.get(i);
                }
            } else if (pixels instanceof Float32Array) {
                const buffer = this.BufferUtils.newFloatBuffer(pixels.length);
                this.GL11.glReadPixels(x ? x : 0, y ? y : 0, width ? width : 0, height ? height : 0, 
                    format ? format : 0, type ? type : 0, buffer);
                for (let i = 0; i < pixels.length; i++) {
                    pixels[i] = buffer.get(i);
                }
            }
        } else {
            this.GL11.glReadPixels(x ? x : 0, y ? y : 0, width ? width : 0, height ? height : 0, 
                format ? format : 0, type ? type : 0, 0);
        }
    }

    // MEDIUM PRIORITY: Query objects (performance monitoring)
    createQuery() {
        this.GL15 = this.GL15 || Java.type('org.lwjgl.opengl.GL15');
        return this.GL15.glGenQueries();
    }

    deleteQuery(query) {
        this.GL15 = this.GL15 || Java.type('org.lwjgl.opengl.GL15');
        this.GL15.glDeleteQueries(query ? query : 0);
    }

    isQuery(query) {
        this.GL15 = this.GL15 || Java.type('org.lwjgl.opengl.GL15');
        return this.GL15.glIsQuery(query ? query : 0);
    }

    beginQuery(target, query) {
        this.GL15 = this.GL15 || Java.type('org.lwjgl.opengl.GL15');
        this.GL15.glBeginQuery(target ? target : 0, query ? query : 0);
    }

    endQuery(target) {
        this.GL15 = this.GL15 || Java.type('org.lwjgl.opengl.GL15');
        this.GL15.glEndQuery(target ? target : 0);
    }

    getQuery(target, pname) {
        this.GL15 = this.GL15 || Java.type('org.lwjgl.opengl.GL15');
        const buffer = this.BufferUtils.newIntBuffer(1);
        this.GL15.glGetQueryiv(target ? target : 0, pname ? pname : 0, buffer);
        return buffer.get(0);
    }

    getQueryParameter(query, pname) {
        this.GL15 = this.GL15 || Java.type('org.lwjgl.opengl.GL15');
        const buffer = this.BufferUtils.newIntBuffer(1);
        this.GL15.glGetQueryObjectiv(query ? query : 0, pname ? pname : 0, buffer);
        return buffer.get(0);
    }

    // MEDIUM PRIORITY: Sampler objects (advanced texture sampling)
    createSampler() {
        this.GL33 = this.GL33 || Java.type('org.lwjgl.opengl.GL33');
        return this.GL33.glGenSamplers();
    }

    deleteSampler(sampler) {
        this.GL33 = this.GL33 || Java.type('org.lwjgl.opengl.GL33');
        this.GL33.glDeleteSamplers(sampler ? sampler : 0);
    }

    bindSampler(unit, sampler) {
        this.GL33 = this.GL33 || Java.type('org.lwjgl.opengl.GL33');
        this.GL33.glBindSampler(unit ? unit : 0, sampler ? sampler : 0);
    }

    isSampler(sampler) {
        this.GL33 = this.GL33 || Java.type('org.lwjgl.opengl.GL33');
        return this.GL33.glIsSampler(sampler ? sampler : 0);
    }

    samplerParameteri(sampler, pname, param) {
        this.GL33 = this.GL33 || Java.type('org.lwjgl.opengl.GL33');
        this.GL33.glSamplerParameteri(sampler ? sampler : 0, pname ? pname : 0, param ? param : 0);
    }

    samplerParameterf(sampler, pname, param) {
        this.GL33 = this.GL33 || Java.type('org.lwjgl.opengl.GL33');
        this.GL33.glSamplerParameterf(sampler ? sampler : 0, pname ? pname : 0, param !== undefined ? param : 0.0);
    }

    getSamplerParameter(sampler, pname) {
        this.GL33 = this.GL33 || Java.type('org.lwjgl.opengl.GL33');
        const buffer = this.BufferUtils.newIntBuffer(1);
        this.GL33.glGetSamplerParameteriv(sampler ? sampler : 0, pname ? pname : 0, buffer);
        return buffer.get(0);
    }

    // MEDIUM PRIORITY: Texture storage and operations
    texStorage2D(target, levels, internalformat, width, height) {
        this.GL42 = this.GL42 || Java.type('org.lwjgl.opengl.GL42');
        this.GL42.glTexStorage2D(target ? target : 0, levels ? levels : 0, internalformat ? internalformat : 0, 
            width ? width : 0, height ? height : 0);
    }

    texStorage3D(target, levels, internalformat, width, height, depth) {
        this.GL42 = this.GL42 || Java.type('org.lwjgl.opengl.GL42');
        this.GL42.glTexStorage3D(target ? target : 0, levels ? levels : 0, internalformat ? internalformat : 0, 
            width ? width : 0, height ? height : 0, depth ? depth : 0);
    }

    copyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height) {
        this.GL12 = this.GL12 || Java.type('org.lwjgl.opengl.GL12');
        this.GL12.glCopyTexSubImage3D(target ? target : 0, level ? level : 0, xoffset ? xoffset : 0, 
            yoffset ? yoffset : 0, zoffset ? zoffset : 0, x ? x : 0, y ? y : 0, width ? width : 0, height ? height : 0);
    }

    compressedTexImage3D(target, level, internalformat, width, height, depth, border, imageSize, data) {
        this.GL13 = this.GL13 || Java.type('org.lwjgl.opengl.GL13');
        if (data && data.length) {
            const buffer = this.BufferUtils.newByteBuffer(data.length);
            for (let i = 0; i < data.length; i++) {
                buffer.put(i, data[i]);
            }
            this.GL13.glCompressedTexImage3D(target ? target : 0, level ? level : 0, internalformat ? internalformat : 0, 
                width ? width : 0, height ? height : 0, depth ? depth : 0, border ? border : 0, buffer);
        } else {
            this.GL13.glCompressedTexImage3D(target ? target : 0, level ? level : 0, internalformat ? internalformat : 0, 
                width ? width : 0, height ? height : 0, depth ? depth : 0, border ? border : 0, imageSize ? imageSize : 0, 0);
        }
    }

    compressedTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, imageSize, data) {
        this.GL13 = this.GL13 || Java.type('org.lwjgl.opengl.GL13');
        if (data && data.length) {
            const buffer = this.BufferUtils.newByteBuffer(data.length);
            for (let i = 0; i < data.length; i++) {
                buffer.put(i, data[i]);
            }
            this.GL13.glCompressedTexSubImage3D(target ? target : 0, level ? level : 0, xoffset ? xoffset : 0, 
                yoffset ? yoffset : 0, zoffset ? zoffset : 0, width ? width : 0, height ? height : 0, 
                depth ? depth : 0, format ? format : 0, buffer);
        } else {
            this.GL13.glCompressedTexSubImage3D(target ? target : 0, level ? level : 0, xoffset ? xoffset : 0, 
                yoffset ? yoffset : 0, zoffset ? zoffset : 0, width ? width : 0, height ? height : 0, 
                depth ? depth : 0, format ? format : 0, imageSize ? imageSize : 0, 0);
        }
    }

    // MEDIUM PRIORITY: Programs and shaders
    getFragDataLocation(program, name) {
        this.GL30.glGetFragDataLocation(program ? program : 0, name ? name : "");
    }

    // MEDIUM PRIORITY: Uniform buffer objects
    getUniformIndices(program, uniformNames) {
        this.GL31 = this.GL31 || Java.type('org.lwjgl.opengl.GL31');
        if (uniformNames && uniformNames.length) {
            const buffer = this.BufferUtils.newIntBuffer(uniformNames.length);
            // Note: LWJGL requires different approach for string arrays
            // This is a simplified implementation
            for (let i = 0; i < uniformNames.length; i++) {
                const index = this.GL31.glGetUniformIndex(program ? program : 0, uniformNames[i]);
                buffer.put(i, index);
            }
            const result = new Array(uniformNames.length);
            for (let i = 0; i < uniformNames.length; i++) {
                result[i] = buffer.get(i);
            }
            return result;
        }
        return [];
    }

    getActiveUniforms(program, uniformIndices, pname) {
        this.GL31 = this.GL31 || Java.type('org.lwjgl.opengl.GL31');
        if (uniformIndices && uniformIndices.length) {
            const indexBuffer = this.BufferUtils.newIntBuffer(uniformIndices.length);
            for (let i = 0; i < uniformIndices.length; i++) {
                indexBuffer.put(i, uniformIndices[i]);
            }
            const resultBuffer = this.BufferUtils.newIntBuffer(uniformIndices.length);
            this.GL31.glGetActiveUniformsiv(program ? program : 0, indexBuffer, pname ? pname : 0, resultBuffer);
            const result = new Array(uniformIndices.length);
            for (let i = 0; i < uniformIndices.length; i++) {
                result[i] = resultBuffer.get(i);
            }
            return result;
        }
        return [];
    }

    getActiveUniformBlockParameter(program, uniformBlockIndex, pname) {
        this.GL31 = this.GL31 || Java.type('org.lwjgl.opengl.GL31');
        const buffer = this.BufferUtils.newIntBuffer(1);
        this.GL31.glGetActiveUniformBlockiv(program ? program : 0, uniformBlockIndex ? uniformBlockIndex : 0, pname ? pname : 0, buffer);
        return buffer.get(0);
    }

    getActiveUniformBlockName(program, uniformBlockIndex) {
        this.GL31 = this.GL31 || Java.type('org.lwjgl.opengl.GL31');
        return this.GL31.glGetActiveUniformBlockName(program ? program : 0, uniformBlockIndex ? uniformBlockIndex : 0);
    }

    // LOW PRIORITY: Transform feedback (advanced geometry processing)
    createTransformFeedback() {
        this.GL40 = this.GL40 || Java.type('org.lwjgl.opengl.GL40');
        return this.GL40.glGenTransformFeedbacks();
    }

    deleteTransformFeedback(transformFeedback) {
        this.GL40 = this.GL40 || Java.type('org.lwjgl.opengl.GL40');
        this.GL40.glDeleteTransformFeedbacks(transformFeedback ? transformFeedback : 0);
    }

    isTransformFeedback(transformFeedback) {
        this.GL40 = this.GL40 || Java.type('org.lwjgl.opengl.GL40');
        return this.GL40.glIsTransformFeedback(transformFeedback ? transformFeedback : 0);
    }

    bindTransformFeedback(target, transformFeedback) {
        this.GL40 = this.GL40 || Java.type('org.lwjgl.opengl.GL40');
        this.GL40.glBindTransformFeedback(target ? target : 0, transformFeedback ? transformFeedback : 0);
    }

    transformFeedbackVaryings(program, varyings, bufferMode) {
        this.GL30.glTransformFeedbackVaryings(program ? program : 0, varyings ? varyings : [], bufferMode ? bufferMode : 0);
    }

    getTransformFeedbackVarying(program, index) {
        this.GL30.glGetTransformFeedbackVarying(program ? program : 0, index ? index : 0);
    }

    pauseTransformFeedback() {
        this.GL40 = this.GL40 || Java.type('org.lwjgl.opengl.GL40');
        this.GL40.glPauseTransformFeedback();
    }

    resumeTransformFeedback() {
        this.GL40 = this.GL40 || Java.type('org.lwjgl.opengl.GL40');
        this.GL40.glResumeTransformFeedback();
    }

    // LOW PRIORITY: Sync objects (GPU synchronization)
    fenceSync(condition, flags) {
        this.GL32 = this.GL32 || Java.type('org.lwjgl.opengl.GL32');
        return this.GL32.glFenceSync(condition ? condition : 0, flags ? flags : 0);
    }

    isSync(sync) {
        this.GL32 = this.GL32 || Java.type('org.lwjgl.opengl.GL32');
        return this.GL32.glIsSync(sync ? sync : 0);
    }

    deleteSync(sync) {
        this.GL32 = this.GL32 || Java.type('org.lwjgl.opengl.GL32');
        this.GL32.glDeleteSync(sync ? sync : 0);
    }

    clientWaitSync(sync, flags, timeout) {
        this.GL32 = this.GL32 || Java.type('org.lwjgl.opengl.GL32');
        return this.GL32.glClientWaitSync(sync ? sync : 0, flags ? flags : 0, timeout ? timeout : 0);
    }

    waitSync(sync, flags, timeout) {
        this.GL32 = this.GL32 || Java.type('org.lwjgl.opengl.GL32');
        this.GL32.glWaitSync(sync ? sync : 0, flags ? flags : 0, timeout ? timeout : 0);
    }

    getSyncParameter(sync, pname) {
        this.GL32 = this.GL32 || Java.type('org.lwjgl.opengl.GL32');
        const buffer = this.BufferUtils.newIntBuffer(1);
        this.GL32.glGetSynciv(sync ? sync : 0, pname ? pname : 0, buffer);
        return buffer.get(0);
    }

    // LOW PRIORITY: Framebuffer operations
    blitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter) {
        this.GL30.glBlitFramebuffer(srcX0 ? srcX0 : 0, srcY0 ? srcY0 : 0, srcX1 ? srcX1 : 0, srcY1 ? srcY1 : 0,
            dstX0 ? dstX0 : 0, dstY0 ? dstY0 : 0, dstX1 ? dstX1 : 0, dstY1 ? dstY1 : 0, mask ? mask : 0, filter ? filter : 0);
    }

    framebufferTextureLayer(target, attachment, texture, level, layer) {
        this.GL30.glFramebufferTextureLayer(target ? target : 0, attachment ? attachment : 0, texture ? texture : 0, 
            level ? level : 0, layer ? layer : 0);
    }

    invalidateFramebuffer(target, attachments) {
        this.GL43 = this.GL43 || Java.type('org.lwjgl.opengl.GL43');
        if (attachments && attachments.length) {
            const buffer = this.BufferUtils.newIntBuffer(attachments.length);
            for (let i = 0; i < attachments.length; i++) {
                buffer.put(i, attachments[i]);
            }
            this.GL43.glInvalidateFramebuffer(target ? target : 0, buffer);
        }
    }

    invalidateSubFramebuffer(target, attachments, x, y, width, height) {
        this.GL43 = this.GL43 || Java.type('org.lwjgl.opengl.GL43');
        if (attachments && attachments.length) {
            const buffer = this.BufferUtils.newIntBuffer(attachments.length);
            for (let i = 0; i < attachments.length; i++) {
                buffer.put(i, attachments[i]);
            }
            this.GL43.glInvalidateSubFramebuffer(target ? target : 0, buffer, x ? x : 0, y ? y : 0, 
                width ? width : 0, height ? height : 0);
        }
    }

    readBuffer(mode) {
        this.GL11.glReadBuffer(mode ? mode : 0);
    }

    // LOW PRIORITY: Renderbuffer operations
    getInternalformatParameter(target, internalformat, pname) {
        this.GL42 = this.GL42 || Java.type('org.lwjgl.opengl.GL42');
        const buffer = this.BufferUtils.newIntBuffer(1);
        this.GL42.glGetInternalformativ(target ? target : 0, internalformat ? internalformat : 0, pname ? pname : 0, buffer);
        return buffer.get(0);
    }

    renderbufferStorageMultisample(target, samples, internalformat, width, height) {
        this.GL30.glRenderbufferStorageMultisample(target ? target : 0, samples ? samples : 0, 
            internalformat ? internalformat : 0, width ? width : 0, height ? height : 0);
    }

    // LOW PRIORITY: Additional uniform functions
    uniform1ui(location, v0) {
        this.GL30.glUniform1ui(location ? location : -1, v0 ? v0 : 0);
    }

    uniform2ui(location, v0, v1) {
        this.GL30.glUniform2ui(location ? location : -1, v0 ? v0 : 0, v1 ? v1 : 0);
    }

    uniform3ui(location, v0, v1, v2) {
        this.GL30.glUniform3ui(location ? location : -1, v0 ? v0 : 0, v1 ? v1 : 0, v2 ? v2 : 0);
    }

    uniform4ui(location, v0, v1, v2, v3) {
        this.GL30.glUniform4ui(location ? location : -1, v0 ? v0 : 0, v1 ? v1 : 0, v2 ? v2 : 0, v3 ? v3 : 0);
    }

    uniform1uiv(location, value) {
        if (value && value.length) {
            const buffer = this.BufferUtils.newIntBuffer(value.length);
            for (let i = 0; i < value.length; i++) {
                buffer.put(i, value[i]);
            }
            this.GL30.glUniform1uiv(location ? location : -1, buffer);
        }
    }

    uniform2uiv(location, value) {
        if (value && value.length) {
            const buffer = this.BufferUtils.newIntBuffer(value.length);
            for (let i = 0; i < value.length; i++) {
                buffer.put(i, value[i]);
            }
            this.GL30.glUniform2uiv(location ? location : -1, buffer);
        }
    }

    uniform3uiv(location, value) {
        if (value && value.length) {
            const buffer = this.BufferUtils.newIntBuffer(value.length);
            for (let i = 0; i < value.length; i++) {
                buffer.put(i, value[i]);
            }
            this.GL30.glUniform3uiv(location ? location : -1, buffer);
        }
    }

    uniform4uiv(location, value) {
        if (value && value.length) {
            const buffer = this.BufferUtils.newIntBuffer(value.length);
            for (let i = 0; i < value.length; i++) {
                buffer.put(i, value[i]);
            }
            this.GL30.glUniform4uiv(location ? location : -1, buffer);
        }
    }

    // LOW PRIORITY: Additional matrix uniform functions
    uniformMatrix2x3fv(location, transpose, data) {
        const buffer = this.convertToFloatBuffer(data);
        this.GL21.glUniformMatrix2x3fv(location, transpose, buffer);
    }

    uniformMatrix3x2fv(location, transpose, data) {
        const buffer = this.convertToFloatBuffer(data);
        this.GL21.glUniformMatrix3x2fv(location, transpose, buffer);
    }

    uniformMatrix2x4fv(location, transpose, data) {
        const buffer = this.convertToFloatBuffer(data);
        this.GL21.glUniformMatrix2x4fv(location, transpose, buffer);
    }

    uniformMatrix4x2fv(location, transpose, data) {
        const buffer = this.convertToFloatBuffer(data);
        this.GL21.glUniformMatrix4x2fv(location, transpose, buffer);
    }

    uniformMatrix3x4fv(location, transpose, data) {
        const buffer = this.convertToFloatBuffer(data);
        this.GL21.glUniformMatrix3x4fv(location, transpose, buffer);
    }

    uniformMatrix4x3fv(location, transpose, data) {
        const buffer = this.convertToFloatBuffer(data);
        this.GL21.glUniformMatrix4x3fv(location, transpose, buffer);
    }

    // ===== FRAMEBUFFER AND RENDERBUFFER FUNCTIONS =====
    
    // Framebuffer functions
    createFramebuffer() {
        return this.GL30.glGenFramebuffers();
    }

    deleteFramebuffer(framebuffer) {
        if (framebuffer) {
            this.GL30.glDeleteFramebuffers(framebuffer);
        }
    }

    bindFramebuffer(target, framebuffer) {
        this.GL30.glBindFramebuffer(target, framebuffer || 0);
    }

    isFramebuffer(framebuffer) {
        return framebuffer ? this.GL30.glIsFramebuffer(framebuffer) : false;
    }

    framebufferTexture2D(target, attachment, textarget, texture, level) {
        this.GL30.glFramebufferTexture2D(target, attachment, textarget, texture || 0, level);
    }

    framebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer) {
        this.GL30.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer || 0);
    }

    checkFramebufferStatus(target) {
        return this.GL30.glCheckFramebufferStatus(target);
    }

    getFramebufferAttachmentParameter(target, attachment, pname) {
        const buffer = this.BufferUtils.newIntBuffer(1);
        this.GL30.glGetFramebufferAttachmentParameteriv(target, attachment, pname, buffer);
        return buffer.get(0);
    }

    // Renderbuffer functions
    createRenderbuffer() {
        return this.GL30.glGenRenderbuffers();
    }

    deleteRenderbuffer(renderbuffer) {
        if (renderbuffer) {
            this.GL30.glDeleteRenderbuffers(renderbuffer);
        }
    }

    bindRenderbuffer(target, renderbuffer) {
        this.GL30.glBindRenderbuffer(target, renderbuffer || 0);
    }

    isRenderbuffer(renderbuffer) {
        return renderbuffer ? this.GL30.glIsRenderbuffer(renderbuffer) : false;
    }

    renderbufferStorage(target, internalformat, width, height) {
        this.GL30.glRenderbufferStorage(target, internalformat, width, height);
    }

    getRenderbufferParameter(target, pname) {
        const buffer = this.BufferUtils.newIntBuffer(1);
        this.GL30.glGetRenderbufferParameteriv(target, pname, buffer);
        return buffer.get(0);
    }

    // ===== SHADER UNIFORM AND ATTRIBUTE FUNCTIONS =====
    
    getActiveUniform(program, index) {
        const nameBuffer = this.BufferUtils.newByteBuffer(256);
        const lengthBuffer = this.BufferUtils.newIntBuffer(1);
        const sizeBuffer = this.BufferUtils.newIntBuffer(1);
        const typeBuffer = this.BufferUtils.newIntBuffer(1);
        
        this.GL20.glGetActiveUniform(program, index, lengthBuffer, sizeBuffer, typeBuffer, nameBuffer);
        
        const nameBytes = new Array(lengthBuffer.get(0));
        for (let i = 0; i < lengthBuffer.get(0); i++) {
            nameBytes[i] = nameBuffer.get(i);
        }
        const name = String.fromCharCode.apply(null, nameBytes);
        
        return {
            name: name,
            size: sizeBuffer.get(0),
            type: typeBuffer.get(0)
        };
    }

    getActiveAttrib(program, index) {
        const nameBuffer = this.BufferUtils.newByteBuffer(256);
        const lengthBuffer = this.BufferUtils.newIntBuffer(1);
        const sizeBuffer = this.BufferUtils.newIntBuffer(1);
        const typeBuffer = this.BufferUtils.newIntBuffer(1);
        
        this.GL20.glGetActiveAttrib(program, index, lengthBuffer, sizeBuffer, typeBuffer, nameBuffer);
        
        const nameBytes = new Array(lengthBuffer.get(0));
        for (let i = 0; i < lengthBuffer.get(0); i++) {
            nameBytes[i] = nameBuffer.get(i);
        }
        const name = String.fromCharCode.apply(null, nameBytes);
        
        return {
            name: name,
            size: sizeBuffer.get(0),
            type: typeBuffer.get(0)
        };
    }

    getUniformLocation(program, name) {
        return this.GL20.glGetUniformLocation(program, name);
    }

    getAttribLocation(program, name) {
        return this.GL20.glGetAttribLocation(program, name);
    }

    getProgramParameter(program, pname) {
        const buffer = this.BufferUtils.newIntBuffer(1);
        this.GL20.glGetProgramiv(program, pname, buffer);
        return buffer.get(0);
    }

    getShaderParameter(shader, pname) {
        const buffer = this.BufferUtils.newIntBuffer(1);
        this.GL20.glGetShaderiv(shader, pname, buffer);
        return buffer.get(0);
    }

    getProgramInfoLog(program) {
        const length = this.getProgramParameter(program, this.GL20.GL_INFO_LOG_LENGTH);
        if (length <= 0) return "";
        
        const buffer = this.BufferUtils.newByteBuffer(length);
        const lengthBuffer = this.BufferUtils.newIntBuffer(1);
        this.GL20.glGetProgramInfoLog(program, lengthBuffer, buffer);
        
        const logBytes = new Array(lengthBuffer.get(0));
        for (let i = 0; i < lengthBuffer.get(0); i++) {
            logBytes[i] = buffer.get(i);
        }
        return String.fromCharCode.apply(null, logBytes);
    }

    getShaderInfoLog(shader) {
        const length = this.getShaderParameter(shader, this.GL20.GL_INFO_LOG_LENGTH);
        if (length <= 0) return "";
        
        const buffer = this.BufferUtils.newByteBuffer(length);
        const lengthBuffer = this.BufferUtils.newIntBuffer(1);
        this.GL20.glGetShaderInfoLog(shader, lengthBuffer, buffer);
        
        const logBytes = new Array(lengthBuffer.get(0));
        for (let i = 0; i < lengthBuffer.get(0); i++) {
            logBytes[i] = buffer.get(i);
        }
        return String.fromCharCode.apply(null, logBytes);
    }

    // ===== BUFFER CONVERSION HELPER =====
    
    convertToFloatBuffer(data) {
        if (!data) return null;
        
        const buffer = this.BufferUtils.newFloatBuffer(data.length);
        for (let i = 0; i < data.length; i++) {
            buffer.put(i, data[i]);
        }
        return buffer;
    }

    // ===== BASIC UNIFORM MATRIX FUNCTIONS =====
    
    uniformMatrix2fv(location, transpose, data) {
        const buffer = this.convertToFloatBuffer(data);
        this.GL20.glUniformMatrix2fv(location, transpose, buffer);
    }

    uniformMatrix3fv(location, transpose, data) {
        const buffer = this.convertToFloatBuffer(data);
        this.GL20.glUniformMatrix3fv(location, transpose, buffer);
    }

    uniformMatrix4fv(location, transpose, data) {
        const buffer = this.convertToFloatBuffer(data);
        this.GL20.glUniformMatrix4fv(location, transpose, buffer);
    }
}

// Export for use in JavaScript environments
if (typeof module !== 'undefined' && module.exports) {
    module.exports = WebGL2Bridge;
} else if (typeof window !== 'undefined') {
    window.WebGL2Bridge = WebGL2Bridge;
}
