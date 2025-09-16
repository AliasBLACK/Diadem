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
                const buffer = BufferUtils.newFloatBuffer(data.length);
                for (let i = 0; i < data.length; i++) {
                    buffer.put(i, data[i]);
                }
                this.GL15.glBufferData(target ? target : 0, buffer, usage ? usage : 0);
            } else if (data instanceof Uint16Array) {
                const buffer = BufferUtils.newShortBuffer(data.length);
                for (let i = 0; i < data.length; i++) {
                    buffer.put(i, data[i]);
                }
                this.GL15.glBufferData(target ? target : 0, buffer, usage ? usage : 0);
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
            this.GL11.glTexImage2D(target ? target : 0, level ? level : 0, internalformat ? internalformat : 0, width ? width : 0, height ? height : 0, format ? format : 0);
        } else {
            this.GL11.glTexImage2D(target ? target : 0, level ? level : 0, internalformat ? internalformat : 0, width ? width : 0, height ? height : 0, border ? border : 0, format ? format : 0, type ? type : 0, pixels);
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
        this.GL20.glUniformMatrix4fv(location ? location : -1, 1, transpose ? transpose : false, value);
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
        // WebGL2 3D texture support - use GL12 for 3D textures
        this.GL12 = this.GL12 || Java.type('org.lwjgl.opengl.GL12');
        this.GL12.glTexImage3D(target ? target : 0, level ? level : 0, internalformat ? internalformat : 0, width ? width : 0, height ? height : 0, depth ? depth : 0, border ? border : 0, format ? format : 0, type ? type : 0, pixels);
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
}

// Export for use in JavaScript environments
if (typeof module !== 'undefined' && module.exports) {
    module.exports = WebGL2Bridge;
} else if (typeof window !== 'undefined') {
    window.WebGL2Bridge = WebGL2Bridge;
}
