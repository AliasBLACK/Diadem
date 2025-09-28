const GL11 = Java.type('org.lwjgl.opengl.GL11');
const GL12 = Java.type('org.lwjgl.opengl.GL12');
const GL13 = Java.type('org.lwjgl.opengl.GL13');
const GL15 = Java.type('org.lwjgl.opengl.GL15');
const GL20 = Java.type('org.lwjgl.opengl.GL20');
const GL30 = Java.type('org.lwjgl.opengl.GL30');
const GL31 = Java.type('org.lwjgl.opengl.GL31');
const GL32 = Java.type('org.lwjgl.opengl.GL32');
const GL33 = Java.type('org.lwjgl.opengl.GL33');
const GL40 = Java.type('org.lwjgl.opengl.GL40');
const GL42 = Java.type('org.lwjgl.opengl.GL42');
const GL43 = Java.type('org.lwjgl.opengl.GL43');
const glAdapter = Java.type('black.alias.diadem.Utils.GLAdapter');
const bufferUtils = Java.type('org.lwjgl.BufferUtils');

const ImageIO = Java.type('javax.imageio.ImageIO');
const ByteArrayInputStream = Java.type('java.io.ByteArrayInputStream');
const BufferedImage = Java.type('java.awt.image.BufferedImage');

/**
 * WebGL2 Renderer
 * Maps WebGL2 API calls to LWJGL OpenGL 4.3 implementation.
 */
globalThis.gl = {

	activeTexture: (texture) => {
		GL13.glActiveTexture(texture ? texture : 0);
	},

	attachShader: (program, shader) => {
		GL20.glAttachShader(program ? program : 0, shader ? shader : 0);
	},

	bindBuffer: (target, buffer) => {
		GL15.glBindBuffer(target ? target : 0, buffer ? buffer : 0);
	},

	bindTexture: (target, texture) => {
		GL11.glBindTexture(target ? target : 0, texture ? texture : 0);
	},

	bufferData: (target, data, usage) => {
		if (data instanceof ArrayBuffer || data instanceof Float32Array || data instanceof Uint16Array) {
			if (data instanceof Float32Array) {
				const buffer = bufferUtils.createFloatBuffer(data.length);
				for (let i = 0; i < data.length; i++) {
					buffer.put(i, data[i]);
				}
				GL15.glBufferData(target ? target : 0, buffer, usage ? usage : 0);
			} else if (data instanceof Uint16Array) {
				const buffer = bufferUtils.createShortBuffer(data.length);
				for (let i = 0; i < data.length; i++) {
					buffer.put(i, data[i]);
				}
				GL15.glBufferData(target ? target : 0, buffer, usage ? usage : 0);
			} else {
				const buffer = bufferUtils.createByteBuffer(data.byteLength);
				const view = new Uint8Array(data);
				for (let i = 0; i < view.length; i++) {
					buffer.put(i, view[i]);
				}
				GL15.glBufferData(target ? target : 0, buffer, usage ? usage : 0);
			}
		} else if (typeof data === 'number') {
			GL15.glBufferData(target ? target : 0, data, usage ? usage : 0);
		}
	},

	clear: (mask) => {
		GL11.glClear(mask ? mask : 0);
	},

	clearColor: (red, green, blue, alpha) => {
		glAdapter.glClearColor(red, green, blue, alpha);
	},

	clearDepth: (depth) => {
		GL11.glClearDepth(depth !== undefined ? depth : 1.0);
	},

	clearStencil: (stencil) => {
		GL11.glClearStencil(stencil !== undefined ? stencil : 0);
	},

	depthFunc: (func) => {
		GL11.glDepthFunc(func ? func : 0);
	},

	compileShader: (shader) => {
		GL20.glCompileShader(shader ? shader : 0);
	},

	createBuffer: () => {
		return GL15.glGenBuffers();
	},

	createProgram: () => {
		return GL20.glCreateProgram();
	},

	createShader: (type) => {
		return GL20.glCreateShader(type ? type : 0);
	},

	createTexture: () => {
		return GL11.glGenTextures();
	},

	deleteBuffer: (buffer) => {
		GL15.glDeleteBuffers(buffer ? buffer : 0);
	},

	deleteProgram: (program) => {
		GL20.glDeleteProgram(program ? program : 0);
	},

	deleteShader: (shader) => {
		GL20.glDeleteShader(shader ? shader : 0);
	},

	deleteTexture: (texture) => {
		GL11.glDeleteTextures(texture ? texture : 0);
	},

	drawArrays: (mode, first, count) => {
		GL11.glDrawArrays(mode ? mode : 0, first ? first : 0, count ? count : 0);
	},

	drawElements: (mode, count, type, offset) => {
		GL11.glDrawElements(mode ? mode : 0, count ? count : 0, type ? type : 0, offset ? offset : 0);
	},

	enable: (cap) => {
		GL11.glEnable(cap ? cap : 0);
	},

	disable: (cap) => {
		GL11.glDisable(cap ? cap : 0);
	},

	enableVertexAttribArray: (index) => {
		GL20.glEnableVertexAttribArray(index ? index : 0);
	},

	disableVertexAttribArray: (index) => {
		GL20.glDisableVertexAttribArray(index ? index : 0);
	},

	getAttribLocation: (program, name) => {
		return GL20.glGetAttribLocation(program ? program : 0, name ? name : "");
	},

	getUniformLocation: (program, name) => {
		return GL20.glGetUniformLocation(program ? program : 0, name ? name : "");
	},

	getShaderParameter: (shader, pname) => {
		const buffer = bufferUtils.createIntBuffer(1);
		GL20.glGetShaderiv(shader ? shader : 0, pname ? pname : 0, buffer);
		return buffer.get(0);
	},

	getProgramParameter: (program, pname) => {
		const buffer = bufferUtils.createIntBuffer(1);
		GL20.glGetProgramiv(program ? program : 0, pname ? pname : 0, buffer);
		return buffer.get(0);
	},

	getShaderInfoLog: (shader) => {
		return GL20.glGetShaderInfoLog(shader ? shader : 0);
	},

	getProgramInfoLog: (program) => {
		return GL20.glGetProgramInfoLog(program ? program : 0);
	},

	linkProgram: (program) => {
		GL20.glLinkProgram(program ? program : 0);
	},

	shaderSource: (shader, source) => {
		if (source) {
			const platform = Java.type('java.lang.System').getProperty('os.name').toLowerCase();
			
			if (platform.includes('mac')) {
				source = source.replace(/#version 300 es/g, '#version 330 core');
				source = source.replace(/precision\s+(lowp|mediump|highp)\s+float\s*;/g, '');
				source = source.replace(/precision\s+(lowp|mediump|highp)\s+int\s*;/g, '');
				
				if (source.includes('attribute ')) {
					source = source.replace(/attribute /g, 'in ');
				}
				if (source.includes('varying ')) {
					if (source.includes('gl_FragColor')) {
						source = source.replace(/varying /g, 'in ');
					} else {
						source = source.replace(/varying /g, 'out ');
					}
				}
				
				if (source.includes('gl_FragColor')) {
					source = 'out vec4 fragColor;\n' + source;
					source = source.replace(/gl_FragColor/g, 'fragColor');
				}
			} else {
				source = source.replace(/#version 300 es/g, '#version 430 core');
				source = source.replace(/precision\s+(lowp|mediump|highp)\s+float\s*;/g, '');
				source = source.replace(/precision\s+(lowp|mediump|highp)\s+int\s*;/g, '');
			}
		}
		
		GL20.glShaderSource(shader ? shader : 0, source ? source : "");
	},

	texImage2D: (target, level, internalformat, width, height, border, format, type, pixels) => {
		if (arguments.length === 6) {
			glAdapter.glTexImage2D(target ? target : 0, level ? level : 0, internalformat ? internalformat : 0, width ? width : 0, height ? height : 0, format ? format : 0, null);
		} else {
			if (pixels === null || pixels === undefined) {
				glAdapter.glTexImage2D(target ? target : 0, level ? level : 0, internalformat ? internalformat : 0, 
					width ? width : 0, height ? height : 0, border ? border : 0, format ? format : 0, type ? type : 0, null);
			} else {
				let buffer = null;
				if (pixels && typeof pixels === 'object' && pixels.length !== undefined) {
					buffer = bufferUtils.createByteBuffer(pixels.length * 4);
					for (let i = 0; i < pixels.length; i++) {
						buffer.putFloat(pixels[i]);
					}
					buffer.flip();
				}
				glAdapter.glTexImage2D(target ? target : 0, level ? level : 0, internalformat ? internalformat : 0, 
					width ? width : 0, height ? height : 0, border ? border : 0, format ? format : 0, type ? type : 0, buffer);
			}
		}
	},

	uniform1f: (location, value) => {
		glAdapter.glUniform1f(location, value !== undefined ? value : 0.0);
	},

	uniform1i: (location, value) => {
		// Convert boolean to int: false->0, true->1
		let intValue = value;
		if (typeof value === 'boolean') {
			intValue = value ? 1 : 0;
		} else if (value === undefined || value === null) {
			intValue = 0;
		}
		GL20.glUniform1i(location, intValue);
	},

	uniform2f: (location, x, y) => {
		glAdapter.glUniform2f(location, x !== undefined ? x : 0.0, y !== undefined ? y : 0.0);
	},

	uniform3f: (location, x, y, z) => {
		glAdapter.glUniform3f(location, x !== undefined ? x : 0.0, y !== undefined ? y : 0.0, z !== undefined ? z : 0.0);
	},

	uniform4f: (location, x, y, z, w) => {
		glAdapter.glUniform4f(location, x !== undefined ? x : 0.0, y !== undefined ? y : 0.0, z !== undefined ? z : 0.0, w !== undefined ? w : 0.0);
	},

	uniform1fv: (location, value) => {
		if (value && value.length) {
			const buffer = glAdapter.createFloatBuffer(value);
			glAdapter.glUniform1fv(location, buffer);
		}
	},

	uniform2fv: (location, value) => {
		if (value && value.length) {
			const buffer = glAdapter.createFloatBuffer(value);
			glAdapter.glUniform2fv(location, buffer);
		}
	},

	uniform3fv: (location, value) => {
		if (value && value.length) {
			const buffer = glAdapter.createFloatBuffer(value);
			glAdapter.glUniform3fv(location, buffer);
		}
	},

	uniform4fv: (location, value) => {
		if (value && value.length) {
			const buffer = glAdapter.createFloatBuffer(value);
			glAdapter.glUniform4fv(location, buffer);
		}
	},

	uniform1iv: (location, value) => {
		if (value && value.length) {
			const buffer = glAdapter.createIntBuffer(value);
			glAdapter.glUniform1iv(location, buffer);
		}
	},

	uniform2iv: (location, value) => {
		if (value && value.length) {
			const buffer = glAdapter.createIntBuffer(value);
			glAdapter.glUniform2iv(location, buffer);
		}
	},

	uniform4iv: (location, value) => {
		if (value && value.length) {
			const buffer = glAdapter.createIntBuffer(value);
			glAdapter.glUniform4iv(location, buffer);
		}
	},

	uniformMatrix2fv: (location, transpose, value) => {
		if (value && value.length) {
			const buffer = glAdapter.createFloatBuffer(value);
			glAdapter.glUniformMatrix2fv(location, transpose !== undefined ? transpose : false, buffer);
		}
	},

	uniformMatrix3fv: (location, transpose, value) => {
		if (value && value.length) {
			const buffer = glAdapter.createFloatBuffer(value);
			glAdapter.glUniformMatrix3fv(location, transpose !== undefined ? transpose : false, buffer);
		}
	},

	uniformMatrix4fv: (location, transpose, value) => {
		if (value && value.length) {
			const buffer = glAdapter.createFloatBuffer(value);
			glAdapter.glUniformMatrix4fv(location, transpose !== undefined ? transpose : false, buffer);
		}
	},

	useProgram: (program) => {
		GL20.glUseProgram(program ? program : 0);
	},

	vertexAttribPointer: (index, size, type, normalized, stride, offset) => {
		GL20.glVertexAttribPointer(index ? index : 0, size ? size : 0, type ? type : 0, normalized ? normalized : false, stride ? stride : 0, offset ? offset : 0);
	},

	texImage2D: function() {
		if (arguments.length === 6) {
			const target = arguments[0] || 0;
			const level = arguments[1] || 0;
			let internalformat = arguments[2];
			let format = arguments[3];
			let type = arguments[4];
			const source = arguments[5];

			// Defensive defaults
			if (format === undefined || format === null) format = GL11.GL_RGBA;
			if (type === undefined || type === null) type = GL11.GL_UNSIGNED_BYTE;
			if (internalformat === undefined || internalformat === null) {
				internalformat = (format === GL11.GL_RGB) ? GL11.GL_RGB8 : GL11.GL_RGBA8;
			}
			
			if (source && source.width && source.height && (source.data || source.constructor?.name === 'ImageBitmap')) {
				const textureData = new Uint8Array(source.width * source.height * 4);
				
				const imageData = source.data;
				if (imageData) {
					for (let y = 0; y < source.height; y++) {
						for (let x = 0; x < source.width; x++) {
							const pixelIndex = (y * source.width + x) * 4;
							const dataIndex = ((y * source.width + x) % imageData.length);
							
							const r = imageData[dataIndex % imageData.length];
							const g = imageData[(dataIndex + 1) % imageData.length];
							const b = imageData[(dataIndex + 2) % imageData.length];
							
							textureData[pixelIndex] = r;	 // R
							textureData[pixelIndex + 1] = g; // G
							textureData[pixelIndex + 2] = b; // B
							textureData[pixelIndex + 3] = 255; // A (fully opaque)
						}
					}
				} else {
					for (let i = 0; i < textureData.length; i += 4) {
						textureData[i] = 255;
						textureData[i + 1] = 0;
						textureData[i + 2] = 255;
						textureData[i + 3] = 255;
					}
				}
				
				// Convert to Java buffer
				const buffer = bufferUtils.createByteBuffer(textureData.length);
				for (let i = 0; i < textureData.length; i++) {
					buffer.put(i, textureData[i]);
				}
				
				GL11.glTexImage2D(target, level, internalformat, source.width || 0, source.height || 0, 0, format, type, buffer);
				return;
			}
		} else if (arguments.length === 9) {
			const target = arguments[0] || 0;
			const level = arguments[1] || 0;
			let internalformat = arguments[2];
			let width = arguments[3] || 0;
			let height = arguments[4] || 0;
			const border = arguments[5] || 0;
			let format = arguments[6];
			let type = arguments[7];
			const pixels = arguments[8];

			// Defensive defaults for enums
			if (format === undefined || format === null) format = GL11.GL_RGBA;
			if (type === undefined || type === null) type = GL11.GL_UNSIGNED_BYTE;
			if (internalformat === undefined || internalformat === null) {
				internalformat = (format === GL11.GL_RGB) ? GL11.GL_RGB8 : GL11.GL_RGBA8;
			}
			
			if (pixels === null || pixels === undefined) {
				try {
					glAdapter.glTexImage2D(target, level, internalformat, width, height, border, format, type, null);
				} catch (ex) {
					const comp = (format === GL11.GL_RGB) ? 3 : 4;
					const zero = bufferUtils.createByteBuffer(width * height * comp);
					GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, zero);
				}
			} else if (pixels && typeof pixels === 'object' && pixels.length !== undefined) {
				const buffer = bufferUtils.createByteBuffer(pixels.length);
				for (let i = 0; i < pixels.length; i++) {
					buffer.put(i, pixels[i] & 0xFF);
				}
				try {
					glAdapter.glTexImage2D(target, level, internalformat, width, height, border, format, type, buffer);
				} catch (ex) {
					GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, buffer);
				}
			} else {
				try {
					glAdapter.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
				} catch (ex) {
					GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
				}
			}

			return;
		}
		
		GL11.glTexImage2D(0, 0, 0, 0, 0, 0, 0, 0, null);
	},

	texParameteri: (target, pname, param) => {
		GL11.glTexParameteri(target ? target : 0, pname ? pname : 0, param ? param : 0);
	},

	generateMipmap: (target) => {
		GL30.glGenerateMipmap(target ? target : 0);
	},

	texSubImage2D: function() {
		if (arguments.length === 0) return;
		
		if (arguments.length === 7) {
			const target = arguments[0];
			const level = arguments[1];
			const xoffset = arguments[2];
			const yoffset = arguments[3];
			const format = arguments[4];
			const type = arguments[5];
			const source = arguments[6];
			
			let width = source.width || source.naturalWidth;
			let height = source.height || source.naturalHeight;
			
			if (width && height && source._data) {
				let textureData = null;
				
				try {
					if (source._data instanceof ArrayBuffer) {
						const uint8Array = new Uint8Array(source._data);
						const signedBytes = Array.from(uint8Array).map(b => b > 127 ? b - 256 : b);
						const javaBytes = Java.to(signedBytes, 'byte[]');
						const inputStream = new ByteArrayInputStream(javaBytes);
						const bufferedImage = ImageIO.read(inputStream);
						
						if (bufferedImage) {
							const imgWidth = bufferedImage.getWidth();
							const imgHeight = bufferedImage.getHeight();
							const pixelCount = imgWidth * imgHeight;
							textureData = new Uint8Array(pixelCount * 4);
							
							for (let y = 0; y < imgHeight; y++) {
								for (let x = 0; x < imgWidth; x++) {
									const rgb = bufferedImage.getRGB(x, y);
									const pixelIndex = (y * imgWidth + x) * 4;
									const a = (rgb >> 24) & 0xFF;
									const r = (rgb >> 16) & 0xFF;
									const g = (rgb >> 8) & 0xFF;
									const b = rgb & 0xFF;
									
									textureData[pixelIndex] = r;
									textureData[pixelIndex + 1] = g;
									textureData[pixelIndex + 2] = b;
									textureData[pixelIndex + 3] = a || 255;
								}
							}
							width = imgWidth;
							height = imgHeight;
						}
					}
				} catch (e) {
					const pixelCount = width * height;
					textureData = new Uint8Array(pixelCount * 4);
					const checkSize = 32;
					for (let y = 0; y < height; y++) {
						for (let x = 0; x < width; x++) {
							const pixelIndex = (y * width + x) * 4;
							const checkX = Math.floor(x / checkSize);
							const checkY = Math.floor(y / checkSize);
							const isEven = (checkX + checkY) % 2 === 0;
							
							if (isEven) {
								textureData[pixelIndex] = 255; textureData[pixelIndex + 1] = 0; textureData[pixelIndex + 2] = 0; textureData[pixelIndex + 3] = 255;
							} else {
								textureData[pixelIndex] = 0; textureData[pixelIndex + 1] = 0; textureData[pixelIndex + 2] = 255; textureData[pixelIndex + 3] = 255;
							}
						}
					}
				}
				
				if (textureData && textureData instanceof Uint8Array) {
					
					// Create a Java primitive byte[] and fill it with signed bytes
					const ByteArr = Java.type('byte[]');
					const javaBytes = new ByteArr(textureData.length);
				
					for (let i = 0; i < textureData.length; i++) {
						let v = textureData[i];
						if (v > 127) v -= 256; // signed conversion
						javaBytes[i] = v;	  // Graal will store as Java byte
					}
				
					// Create a direct ByteBuffer and bulk-copy the Java byte[]
					const buffer = bufferUtils.createByteBuffer(textureData.length);
					buffer.clear();
					buffer.put(javaBytes);   // uses ByteBuffer.put(byte[])
					buffer.position(0);
				
					// --- Setup texture storage ---
					let components, internalFormat;
					if (format === GL11.GL_RGB) {
						components = 3; internalFormat = GL11.GL_RGB8;
					} else if (format === GL11.GL_RGBA) {
						components = 4; internalFormat = GL11.GL_RGBA8;
					} else {
						throw new Error("Unsupported format: " + format);
					}
				
					// allocate zeroed storage
					const ByteArr2 = Java.type('byte[]');
					const nullBytes = new ByteArr2(width * height * components); // default 0s
					const nullBuffer = bufferUtils.createByteBuffer(nullBytes.length);
					nullBuffer.clear();
					nullBuffer.put(nullBytes);
					nullBuffer.position(0);
	
					// Important: alignment and binding
					GL11.glTexImage2D(target, level, internalFormat, width, height, 0, format, type, nullBuffer);
					GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, buffer);
				}
			}
		} else if (arguments.length === 9) {
			const target = arguments[0];
			const level = arguments[1];
			const xoffset = arguments[2];
			const yoffset = arguments[3];
			const width = arguments[4];
			const height = arguments[5];
			const format = arguments[6];
			let type = arguments[7];
			const pixels = arguments[8];

			// condensed: no logging

			// Clear any pre-existing GL errors to avoid confusing diagnostics
			let _e = GL11.glGetError();
			let clearedCount = 0;
			while (_e !== GL11.GL_NO_ERROR && clearedCount < 16) {
				clearedCount++;
				_e = GL11.glGetError();
			}
			// (Suppress verbose cleared-count logging)

			// Log the currently bound cube map texture (if any)
			try {
				const GL_TEXTURE_BINDING_CUBE_MAP = 0x8514; // GL13.GL_TEXTURE_BINDING_CUBE_MAP
				GL11.glGetInteger(GL_TEXTURE_BINDING_CUBE_MAP);
			} catch (ex) { /* optional on some drivers */ }

			// Log immutable flag on the bound cubemap (if available)
			let immutableFlag = 0;
			try {
				const GL_TEXTURE_CUBE_MAP = 0x8513;
				const GL_TEXTURE_IMMUTABLE_FORMAT = 0x82DF;
				immutableFlag = GL11.glGetTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_IMMUTABLE_FORMAT) | 0;
			} catch (ex) { /* may not be supported on some paths */ }
			
			// HALF_FLOAT support: when pixels is Uint16Array, allocate 16F storage and upload with GL_HALF_FLOAT
			if (pixels && pixels instanceof Uint16Array) {
				const GL_HALF_FLOAT = 0x140B;
				const GL_RGB16F = 0x881B;
				const GL_RGBA16F = 0x881A;
				let components = 0;
				let internalFormat = 0;
				if (format === GL11.GL_RGB) { components = 3; internalFormat = GL_RGB16F; }
				else if (format === GL11.GL_RGBA) { components = 4; internalFormat = GL_RGBA16F; }
				else { throw new Error("Unsupported format for Uint16Array HALF_FLOAT: " + format); }

				// Prepare ShortBuffer for data
				const shortBuffer = bufferUtils.createShortBuffer(pixels.length);
				for (let i = 0; i < pixels.length; i++) { shortBuffer.put(i, pixels[i] & 0xFFFF); }

				// Allocate storage with null data first
				const ByteArr2 = Java.type('byte[]');
				const nullBytes = new ByteArr2(width * height * components * 2);
				const nullBuffer = bufferUtils.createByteBuffer(nullBytes.length);
				nullBuffer.clear();
				nullBuffer.put(nullBytes);
				nullBuffer.position(0);
				try { GL11.glPixelStorei(0x0CF5, 1); } catch (ex) { /* GL_UNPACK_ALIGNMENT */ }
				try { glAdapter.glTexImage2D(target, level, internalFormat, width, height, 0, format, GL_HALF_FLOAT, null); }
				catch (ex) { GL11.glTexImage2D(target, level, internalFormat, width, height, 0, format, GL_HALF_FLOAT, nullBuffer); }
				GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, GL_HALF_FLOAT, shortBuffer);
				return;
			}

			if (pixels && pixels instanceof Uint8Array) {
				// Create a Java primitive byte[] and fill it with signed bytes
				const ByteArr = Java.type('byte[]');
				const javaBytes = new ByteArr(pixels.length);
			
				for (let i = 0; i < pixels.length; i++) {
					let v = pixels[i];
					if (v > 127) v -= 256; // signed conversion
					javaBytes[i] = v;	  // Graal will store as Java byte
				}
			
				// Create a direct ByteBuffer and bulk-copy the Java byte[]
				const buffer = bufferUtils.createByteBuffer(pixels.length);
				buffer.clear();
				buffer.put(javaBytes);   // uses ByteBuffer.put(byte[])
				buffer.position(0);
			
				// --- Setup texture storage ---
				let components, internalFormat;
				if (format === GL11.GL_RGB) {
					components = 3; internalFormat = GL11.GL_RGB8;
				} else if (format === GL11.GL_RGBA) {
					components = 4; internalFormat = GL11.GL_RGBA8;
				} else {
					throw new Error("Unsupported format: " + format);
				}
			
				// allocate zeroed storage
				const ByteArr2 = Java.type('byte[]');
				const nullBytes = new ByteArr2(width * height * components); // default 0s
				const nullBuffer = bufferUtils.createByteBuffer(nullBytes.length);
				nullBuffer.clear();
				nullBuffer.put(nullBytes);
				nullBuffer.position(0);

				// Important: alignment and binding
				// Ensure tight packing for any width
				try { GL11.glPixelStorei(0x0CF5, 1); } catch (ex) { /* GL_UNPACK_ALIGNMENT */ }

				// Check if storage already exists for this face/level
				const GL_TEXTURE_WIDTH = 0x1000;
				const GL_TEXTURE_HEIGHT = 0x1001;
				const GL_TEXTURE_INTERNAL_FORMAT = 0x1003;
				let existingW = 0, existingH = 0, existingIF = 0;
				try {
					existingW = GL11.glGetTexLevelParameteri(target, level, GL_TEXTURE_WIDTH);
					existingH = GL11.glGetTexLevelParameteri(target, level, GL_TEXTURE_HEIGHT);
					existingIF = GL11.glGetTexLevelParameteri(target, level, GL_TEXTURE_INTERNAL_FORMAT);
					// no-op
				} catch (ex) {
					// Some drivers may not report per-face queries until after first definition
				}

				// If immutable storage is present, never attempt per-face allocation.
				if (immutableFlag) {
					// alloc skipped due to immutable storage
				} else if (!(existingW > 0 && existingH > 0)) {
					// Allocate storage for this face only if not already defined
					// Important: allocate with NULL data to avoid driver validating a dummy client buffer
					try { glAdapter.glTexImage2D(target, level, internalFormat, width, height, 0, format, type, null); } catch (ex) {
						// Fallback: if null overload is not resolvable, use zero-sized direct buffer
						GL11.glTexImage2D(target, level, internalFormat, width, height, 0, format, type, nullBuffer);
					}
					// suppress allocation error logging
				} else {
					// storage already defined
				}

				// Upload subimage
				GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, buffer);

			} else if (pixels) {
				GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
			}

			// (suppress per-call GL error prints here to reduce noise)
		}
	},

	compressedTexSubImage2D: function() {
	},

	viewport: (x, y, width, height) => {
		GL11.glViewport(x ? x : 0, y ? y : 0, width ? width : 0, height ? height : 0);
	},

	bindVertexArray: (vertexArray) => {
		GL30.glBindVertexArray(vertexArray ? vertexArray : 0);
	},

	createVertexArray: () => {
		return GL30.glGenVertexArrays();
	},

	deleteVertexArray: (vertexArray) => {
		GL30.glDeleteVertexArrays(vertexArray ? vertexArray : 0);
	},

	texImage3D: (target, level, internalformat, width, height, depth, border, format, type, pixels) => {
		// For null/undefined pixels, pass null directly
		if (pixels === null || pixels === undefined) {
			glAdapter.glTexImage3D(target ? target : 0, level ? level : 0, internalformat ? internalformat : 0, 
				width ? width : 0, height ? height : 0, depth ? depth : 0, border ? border : 0, format ? format : 0, type ? type : 0, null);
		} else {
			// Convert JavaScript typed array to Java ByteBuffer using proper signed byte conversion
			let buffer = null;
			if (pixels && typeof pixels === 'object' && pixels.length !== undefined) {
				if (pixels instanceof Uint8Array) {
					// Handle Uint8Array with proper signed byte conversion (same as texSubImage2D)
					buffer = bufferUtils.createByteBuffer(pixels.length);
					for (let i = 0; i < pixels.length; i++) {
						let byteValue = pixels[i] & 0xFF;
						if (byteValue > 127) {
							byteValue = byteValue - 256;
						}
						buffer.put(i, byteValue);
					}
				} else {
					// Handle other typed arrays by converting to ByteBuffer
					buffer = bufferUtils.createByteBuffer(pixels.length * 4); // Assume 4 bytes per element for safety
					for (let i = 0; i < pixels.length; i++) {
						buffer.putFloat(pixels[i]);
					}
					buffer.flip();
				}
			}
			glAdapter.glTexImage3D(target ? target : 0, level ? level : 0, internalformat ? internalformat : 0, 
				width ? width : 0, height ? height : 0, depth ? depth : 0, border ? border : 0, format ? format : 0, type ? type : 0, buffer);
		}
	},

	texStorage2D: (target, levels, internalformat, width, height) => {
		// Allocate immutable storage when available; otherwise gracefully fall back.
		let _levels = (levels && levels > 0) ? levels : 1;
		let _internal = internalformat ? internalformat : 0;
		if (_internal === 0) {
			// Defensive fallback: WebGL2 requires a valid sized internal format. Default to RGBA8.
			_internal = GL11.GL_RGBA8;
		}
		try {
			GL42.glTexStorage2D(target ? target : 0, _levels, _internal, width ? width : 0, height ? height : 0);
			let e = GL11.glGetError();
			if (e !== GL11.GL_NO_ERROR) {
				// Driver reported error; use mutable fallback silently
				try {
					glAdapter.glTexImage2D(target ? target : 0, 0, _internal, width ? width : 0, height ? height : 0, 0, _internal === GL11.GL_RGBA8 ? GL11.GL_RGBA : GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, null);
				} catch (ex) {
					const comp = (_internal === GL11.GL_RGBA8) ? 4 : 3;
					const nullBuf = bufferUtils.createByteBuffer((width ? width : 0) * (height ? height : 0) * comp);
					GL11.glTexImage2D(target ? target : 0, 0, _internal, width ? width : 0, height ? height : 0, 0,
						_internal === GL11.GL_RGBA8 ? GL11.GL_RGBA : GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, nullBuf);
				}
			}
		} catch (e) {
			// Extension not available; use mutable fallback.
			try {
				glAdapter.glTexImage2D(target ? target : 0, 0, _internal, width ? width : 0, height ? height : 0, 0, _internal === GL11.GL_RGBA8 ? GL11.GL_RGBA : GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, null);
			} catch (ex) {
				const comp = (_internal === GL11.GL_RGBA8) ? 4 : 3;
				const nullBuf = bufferUtils.createByteBuffer((width ? width : 0) * (height ? height : 0) * comp);
				GL11.glTexImage2D(target ? target : 0, 0, _internal, width ? width : 0, height ? height : 0, 0,
					_internal === GL11.GL_RGBA8 ? GL11.GL_RGBA : GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, nullBuf);
			}
		}
	},

	texStorage3D: (target, levels, internalformat, width, height, depth) => {
		try {
			GL42.glTexStorage3D(target ? target : 0, levels ? levels : 1, internalformat ? internalformat : 0, width ? width : 0, height ? height : 0, depth ? depth : 0);
		} catch (e) {
			try {
				glAdapter.glTexImage3D(target ? target : 0, 0, internalformat ? internalformat : 0, width ? width : 0, height ? height : 0, depth ? depth : 0, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null);
			} catch (ex) {
				const comp = 4;
				const nullBuf = bufferUtils.createByteBuffer((width ? width : 0) * (height ? height : 0) * (depth ? depth : 0) * comp);
				GL12.glTexImage3D(target ? target : 0, 0, internalformat ? internalformat : 0, width ? width : 0, height ? height : 0, depth ? depth : 0, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, nullBuf);
			}
		}
	},

	drawArraysInstanced: (mode, first, count, instanceCount) => {
		GL31.glDrawArraysInstanced(mode ? mode : 0, first ? first : 0, count ? count : 0, instanceCount ? instanceCount : 0);
	},

	drawElementsInstanced: (mode, count, type, offset, instanceCount) => {
		GL31.glDrawElementsInstanced(mode ? mode : 0, count ? count : 0, type ? type : 0, offset ? offset : 0, instanceCount ? instanceCount : 0);
	},

	beginTransformFeedback: (primitiveMode) => {
		GL30.glBeginTransformFeedback(primitiveMode ? primitiveMode : 0);
	},

	endTransformFeedback: () => {
		GL30.glEndTransformFeedback();
	},

	bindBufferBase: (target, index, buffer) => {
		GL30.glBindBufferBase(target ? target : 0, index ? index : 0, buffer ? buffer : 0);
	},

	bindBufferRange: (target, index, buffer, offset, size) => {
		GL30.glBindBufferRange(target ? target : 0, index ? index : 0, buffer ? buffer : 0, offset ? offset : 0, size ? size : 0);
	},

	getUniformBlockIndex: (program, uniformBlockName) => {
		return GL31.glGetUniformBlockIndex(program ? program : 0, uniformBlockName ? uniformBlockName : "");
	},

	uniformBlockBinding: (program, uniformBlockIndex, uniformBlockBinding) => {
		GL31.glUniformBlockBinding(program ? program : 0, uniformBlockIndex ? uniformBlockIndex : 0, uniformBlockBinding ? uniformBlockBinding : 0);
	},

	bufferSubData: (target, offset, data) => {
		if (data instanceof Float32Array) {
			const buffer = bufferUtils.createFloatBuffer(data.length);
			for (let i = 0; i < data.length; i++) {
				buffer.put(i, data[i]);
			}
			GL15.glBufferSubData(target ? target : 0, offset ? offset : 0, buffer);
		} else if (data instanceof Uint16Array) {
			const buffer = bufferUtils.createShortBuffer(data.length);
			for (let i = 0; i < data.length; i++) {
				buffer.put(i, data[i]);
			}
			GL15.glBufferSubData(target ? target : 0, offset ? offset : 0, buffer);
		} else if (data instanceof ArrayBuffer) {
			const buffer = bufferUtils.createByteBuffer(data.byteLength);
			const view = new Uint8Array(data);
			for (let i = 0; i < view.length; i++) {
				buffer.put(i, view[i]);
			}
			GL15.glBufferSubData(target ? target : 0, offset ? offset : 0, buffer);
		}
	},

	getBufferSubData: (target, offset, returnedData) => {
		// WebGL2 buffer data readback - use GL15
		if (returnedData instanceof Float32Array) {
			const buffer = bufferUtils.createFloatBuffer(returnedData.length);
			GL15.glGetBufferSubData(target ? target : 0, offset ? offset : 0, buffer);
			for (let i = 0; i < returnedData.length; i++) {
				returnedData[i] = buffer.get(i);
			}
		} else if (returnedData instanceof Uint16Array) {
			const buffer = bufferUtils.createShortBuffer(returnedData.length);
			GL15.glGetBufferSubData(target ? target : 0, offset ? offset : 0, buffer);
			for (let i = 0; i < returnedData.length; i++) {
				returnedData[i] = buffer.get(i);
			}
		} else if (returnedData instanceof ArrayBuffer) {
			const buffer = bufferUtils.createByteBuffer(returnedData.byteLength);
			GL15.glGetBufferSubData(target ? target : 0, offset ? offset : 0, buffer);
			const view = new Uint8Array(returnedData);
			for (let i = 0; i < view.length; i++) {
				view[i] = buffer.get(i);
			}
		}
	},

	copyBufferSubData: (readTarget, writeTarget, readOffset, writeOffset, size) => {
		GL31.glCopyBufferSubData(readTarget ? readTarget : 0, writeTarget ? writeTarget : 0, 
			readOffset ? readOffset : 0, writeOffset ? writeOffset : 0, size ? size : 0);
	},

	drawRangeElements: (mode, start, end, count, type, offset) => {
		
		GL12.glDrawRangeElements(mode ? mode : 0, start ? start : 0, end ? end : 0, 
			count ? count : 0, type ? type : 0, offset ? offset : 0);
	},

	drawBuffers: (buffers) => {
		if (buffers && buffers.length) {
			const buffer = bufferUtils.createIntBuffer(buffers.length);
			for (let i = 0; i < buffers.length; i++) {
				buffer.put(i, buffers[i]);
			}
			GL20.glDrawBuffers(buffer);
		}
	},

	clearBufferfv: (buffer, drawbuffer, value) => {
		if (value && value.length) {
			const floatBuffer = bufferUtils.createFloatBuffer(value.length);
			for (let i = 0; i < value.length; i++) {
				floatBuffer.put(i, value[i]);
			}
			GL30.glClearBufferfv(buffer ? buffer : 0, drawbuffer ? drawbuffer : 0, floatBuffer);
		}
	},

	clearBufferiv: (buffer, drawbuffer, value) => {
		if (value && value.length) {
			const intBuffer = bufferUtils.createIntBuffer(value.length);
			for (let i = 0; i < value.length; i++) {
				intBuffer.put(i, value[i]);
			}
			GL30.glClearBufferiv(buffer ? buffer : 0, drawbuffer ? drawbuffer : 0, intBuffer);
		}
	},

	clearBufferuiv: (buffer, drawbuffer, value) => {
		if (value && value.length) {
			const intBuffer = bufferUtils.createIntBuffer(value.length);
			for (let i = 0; i < value.length; i++) {
				intBuffer.put(i, value[i]);
			}
			GL30.glClearBufferuiv(buffer ? buffer : 0, drawbuffer ? drawbuffer : 0, intBuffer);
		}
	},

	clearBufferfi: (buffer, drawbuffer, depth, stencil) => {
		GL30.glClearBufferfi(buffer ? buffer : 0, drawbuffer ? drawbuffer : 0, 
			depth !== undefined ? depth : 1.0, stencil !== undefined ? stencil : 0);
	},

	vertexAttribDivisor: (index, divisor) => {
		GL33.glVertexAttribDivisor(index ? index : 0, divisor ? divisor : 0);
	},

	vertexAttribIPointer: (index, size, type, stride, offset) => {
		GL30.glVertexAttribIPointer(index ? index : 0, size ? size : 0, type ? type : 0, 
			stride ? stride : 0, offset ? offset : 0);
	},

	vertexAttribI4i: (index, x, y, z, w) => {
		GL30.glVertexAttribI4i(index ? index : 0, x ? x : 0, y ? y : 0, z ? z : 0, w ? w : 0);
	},

	vertexAttribI4ui: (index, x, y, z, w) => {
		GL30.glVertexAttribI4ui(index ? index : 0, x ? x : 0, y ? y : 0, z ? z : 0, w ? w : 0);
	},

	vertexAttribI4iv: (index, v) => {
		if (v && v.length >= 4) {
			GL30.glVertexAttribI4i(index ? index : 0, v[0], v[1], v[2], v[3]);
		}
	},

	vertexAttribI4uiv: (index, v) => {
		if (v && v.length >= 4) {
			GL30.glVertexAttribI4ui(index ? index : 0, v[0], v[1], v[2], v[3]);
		}
	},

	isVertexArray: (vertexArray) => {
		return GL30.glIsVertexArray(vertexArray ? vertexArray : 0);
	},

	getIndexedParameter: (target, index) => {
		const buffer = bufferUtils.createIntBuffer(1);
		GL30.glGetIntegeri_v(target ? target : 0, index ? index : 0, buffer);
		return buffer.get(0);
	},

	frontFace: (mode) => {
		GL11.glFrontFace(mode ? mode : 0);
	},

	cullFace: (mode) => {
		GL11.glCullFace(mode ? mode : 0);
	},

	blendFunc: (sfactor, dfactor) => {
		GL11.glBlendFunc(sfactor ? sfactor : 0, dfactor ? dfactor : 0);
	},

	blendFuncSeparate: (srcRGB, dstRGB, srcAlpha, dstAlpha) => {
		
		GL14.glBlendFuncSeparate(srcRGB ? srcRGB : 0, dstRGB ? dstRGB : 0, srcAlpha ? srcAlpha : 0, dstAlpha ? dstAlpha : 0);
	},

	blendEquation: (mode) => {
		
		GL14.glBlendEquation(mode ? mode : 0);
	},

	blendEquationSeparate: (modeRGB, modeAlpha) => {
		GL20.glBlendEquationSeparate(modeRGB ? modeRGB : 0, modeAlpha ? modeAlpha : 0);
	},

	blendColor: (red, green, blue, alpha) => {
		
		glAdapter.glBlendColor(red, green, blue, alpha);
	},

	depthMask: (flag) => {
		GL11.glDepthMask(flag ? flag : false);
	},

	colorMask: (red, green, blue, alpha) => {
		GL11.glColorMask(red ? red : false, green ? green : false, blue ? blue : false, alpha ? alpha : false);
	},

	stencilFunc: (func, ref, mask) => {
		// Convert mask to proper Java int using Java Integer class
		const Integer = Java.type('java.lang.Integer');
		const intMask = mask ? Integer.valueOf(Math.floor(mask) & 0xFFFFFFFF) : 0;
		GL11.glStencilFunc(func ? func : 0, ref ? ref : 0, intMask);
	},

	stencilFuncSeparate: (face, func, ref, mask) => {
		// Convert mask to proper Java int using Java Integer class
		const Integer = Java.type('java.lang.Integer');
		const intMask = mask ? Integer.valueOf(Math.floor(mask) & 0xFFFFFFFF) : 0;
		GL20.glStencilFuncSeparate(face ? face : 0, func ? func : 0, ref ? ref : 0, intMask);
	},

	stencilOp: (fail, zfail, zpass) => {
		GL11.glStencilOp(fail ? fail : 0, zfail ? zfail : 0, zpass ? zpass : 0);
	},

	stencilOpSeparate: (face, fail, zfail, zpass) => {
		GL20.glStencilOpSeparate(face ? face : 0, fail ? fail : 0, zfail ? zfail : 0, zpass ? zpass : 0);
	},

	stencilMask: (mask) => {
		const Integer = Java.type('java.lang.Integer');
		const intMask = mask ? Integer.valueOf(Math.floor(mask) & 0xFFFFFFFF) : 0;
		GL11.glStencilMask(intMask);
	},

	stencilMaskSeparate: (face, mask) => {
		const Integer = Java.type('java.lang.Integer');
		const intMask = mask ? Integer.valueOf(Math.floor(mask) & 0xFFFFFFFF) : 0;
		GL20.glStencilMaskSeparate(face, intMask);
	},

	depthRange: (zNear, zFar) => {
		GL11.glDepthRange(zNear !== undefined ? zNear : 0.0, zFar !== undefined ? zFar : 1.0);
	},

	polygonOffset: (factor, units) => {
		GL11.glPolygonOffset(factor !== undefined ? factor : 0.0, units !== undefined ? units : 0.0);
	},

	sampleCoverage: (value, invert) => {
		GL13.glSampleCoverage(value !== undefined ? value : 1.0, invert ? invert : false);
	},

	scissor: (x, y, width, height) => {
		GL11.glScissor(x ? x : 0, y ? y : 0, width ? width : 0, height ? height : 0);
	},

	lineWidth: (width) => {
		GL11.glLineWidth(width !== undefined ? width : 1.0);
	},

	pixelStorei: (pname, param) => {
		let intParam = param;
		if (typeof param === 'boolean') {
			intParam = param ? 1 : 0;
		}
		GL11.glPixelStorei(pname ? pname : 0, intParam ? intParam : 0);
	},

	readPixels: (x, y, width, height, format, type, pixels) => {
		if (pixels && pixels.length) {
			if (pixels instanceof Uint8Array) {
				const buffer = bufferUtils.createByteBuffer(pixels.length);
				GL11.glReadPixels(x ? x : 0, y ? y : 0, width ? width : 0, height ? height : 0, 
					format ? format : 0, type ? type : 0, buffer);
				for (let i = 0; i < pixels.length; i++) {
					pixels[i] = buffer.get(i);
				}
			} else if (pixels instanceof Float32Array) {
				const buffer = bufferUtils.createFloatBuffer(pixels.length);
				GL11.glReadPixels(x ? x : 0, y ? y : 0, width ? width : 0, height ? height : 0, 
					format ? format : 0, type ? type : 0, buffer);
				for (let i = 0; i < pixels.length; i++) {
					pixels[i] = buffer.get(i);
				}
			}
		} else {
			GL11.glReadPixels(x ? x : 0, y ? y : 0, width ? width : 0, height ? height : 0, 
				format ? format : 0, type ? type : 0, 0);
		}
	},

	createQuery: () => {
		return GL15.glGenQueries();
	},

	deleteQuery: (query) => {
		GL15.glDeleteQueries(query ? query : 0);
	},

	isQuery: (query) => {
		return GL15.glIsQuery(query ? query : 0);
	},

	beginQuery: (target, query) => {
		GL15.glBeginQuery(target ? target : 0, query ? query : 0);
	},

	endQuery: (target) => {
		GL15.glEndQuery(target ? target : 0);
	},

	getQuery: (target, pname) => {
		const buffer = bufferUtils.createIntBuffer(1);
		GL15.glGetQueryiv(target ? target : 0, pname ? pname : 0, buffer);
		return buffer.get(0);
	},

	getQueryParameter: (query, pname) => {
		const buffer = bufferUtils.createIntBuffer(1);
		GL15.glGetQueryObjectiv(query ? query : 0, pname ? pname : 0, buffer);
		return buffer.get(0);
	},

	createSampler: () => {
		return GL33.glGenSamplers();
	},

	deleteSampler: (sampler) => {
		GL33.glDeleteSamplers(sampler ? sampler : 0);
	},

	bindSampler: (unit, sampler) => {
		GL33.glBindSampler(unit ? unit : 0, sampler ? sampler : 0);
	},

	isSampler: (sampler) => {
		return GL33.glIsSampler(sampler ? sampler : 0);
	},

	samplerParameteri: (sampler, pname, param) => {
		GL33.glSamplerParameteri(sampler ? sampler : 0, pname ? pname : 0, param ? param : 0);
	},

	samplerParameterf: (sampler, pname, param) => {
		GL33.glSamplerParameterf(sampler ? sampler : 0, pname ? pname : 0, param !== undefined ? param : 0.0);
	},

	getSamplerParameter: (sampler, pname) => {
		const buffer = bufferUtils.createIntBuffer(1);
		GL33.glGetSamplerParameteriv(sampler ? sampler : 0, pname ? pname : 0, buffer);
		return buffer.get(0);
	},

	copyTexSubImage3D: (target, level, xoffset, yoffset, zoffset, x, y, width, height) => {
		
		GL12.glCopyTexSubImage3D(target ? target : 0, level ? level : 0, xoffset ? xoffset : 0, 
			yoffset ? yoffset : 0, zoffset ? zoffset : 0, x ? x : 0, y ? y : 0, width ? width : 0, height ? height : 0);
	},

	compressedTexImage3D: (target, level, internalformat, width, height, depth, border, imageSize, data) => {
		if (data && data.length) {
			const buffer = bufferUtils.createByteBuffer(data.length);
			for (let i = 0; i < data.length; i++) {
				buffer.put(i, data[i]);
			}
			GL13.glCompressedTexImage3D(target ? target : 0, level ? level : 0, internalformat ? internalformat : 0, 
				width ? width : 0, height ? height : 0, depth ? depth : 0, border ? border : 0, buffer);
		} else {
			GL13.glCompressedTexImage3D(target ? target : 0, level ? level : 0, internalformat ? internalformat : 0, 
				width ? width : 0, height ? height : 0, depth ? depth : 0, border ? border : 0, imageSize ? imageSize : 0, 0);
		}
	},

	compressedTexSubImage3D: (target, level, xoffset, yoffset, zoffset, width, height, depth, format, imageSize, data) => {
		if (data && data.length) {
			const buffer = bufferUtils.createByteBuffer(data.length);
			for (let i = 0; i < data.length; i++) {
				buffer.put(i, data[i]);
			}
			GL13.glCompressedTexSubImage3D(target ? target : 0, level ? level : 0, xoffset ? xoffset : 0, 
				yoffset ? yoffset : 0, zoffset ? zoffset : 0, width ? width : 0, height ? height : 0, 
				depth ? depth : 0, format ? format : 0, buffer);
		} else {
			GL13.glCompressedTexSubImage3D(target ? target : 0, level ? level : 0, xoffset ? xoffset : 0, 
				yoffset ? yoffset : 0, zoffset ? zoffset : 0, width ? width : 0, height ? height : 0, 
				depth ? depth : 0, format ? format : 0, imageSize ? imageSize : 0, 0);
		}
	},

	getFragDataLocation: (program, name) => {
		GL30.glGetFragDataLocation(program ? program : 0, name ? name : "");
	},

	getUniformIndices: (program, uniformNames) => {
		if (uniformNames && uniformNames.length) {
			const buffer = bufferUtils.createIntBuffer(uniformNames.length);
			for (let i = 0; i < uniformNames.length; i++) {
				const index = GL31.glGetUniformIndex(program ? program : 0, uniformNames[i]);
				buffer.put(i, index);
			}
			const result = new Array(uniformNames.length);
			for (let i = 0; i < uniformNames.length; i++) {
				result[i] = buffer.get(i);
			}
			return result;
		}
		return [];
	},

	getActiveUniforms: (program, uniformIndices, pname) => {
		if (uniformIndices && uniformIndices.length) {
			const indexBuffer = bufferUtils.createIntBuffer(uniformIndices.length);
			for (let i = 0; i < uniformIndices.length; i++) {
				indexBuffer.put(i, uniformIndices[i]);
			}
			const resultBuffer = bufferUtils.createIntBuffer(uniformIndices.length);
			GL31.glGetActiveUniformsiv(program ? program : 0, indexBuffer, pname ? pname : 0, resultBuffer);
			const result = new Array(uniformIndices.length);
			for (let i = 0; i < uniformIndices.length; i++) {
				result[i] = resultBuffer.get(i);
			}
			return result;
		}
		return [];
	},

	getActiveUniformBlockParameter: (program, uniformBlockIndex, pname) => {
		const buffer = bufferUtils.createIntBuffer(1);
		GL31.glGetActiveUniformBlockiv(program ? program : 0, uniformBlockIndex ? uniformBlockIndex : 0, pname ? pname : 0, buffer);
		return buffer.get(0);
	},

	getActiveUniformBlockName: (program, uniformBlockIndex) => {
		return GL31.glGetActiveUniformBlockName(program ? program : 0, uniformBlockIndex ? uniformBlockIndex : 0);
	},

	createTransformFeedback: () => {
		return GL40.glGenTransformFeedbacks();
	},

	deleteTransformFeedback: (transformFeedback) => {
		GL40.glDeleteTransformFeedbacks(transformFeedback ? transformFeedback : 0);
	},

	isTransformFeedback: (transformFeedback) => {
		return GL40.glIsTransformFeedback(transformFeedback ? transformFeedback : 0);
	},

	bindTransformFeedback: (target, transformFeedback) => {
		GL40.glBindTransformFeedback(target ? target : 0, transformFeedback ? transformFeedback : 0);
	},

	transformFeedbackVaryings: (program, varyings, bufferMode) => {
		GL30.glTransformFeedbackVaryings(program ? program : 0, varyings ? varyings : [], bufferMode ? bufferMode : 0);
	},

	getTransformFeedbackVarying: (program, index) => {
		GL30.glGetTransformFeedbackVarying(program ? program : 0, index ? index : 0);
	},

	pauseTransformFeedback: () => {
		GL40.glPauseTransformFeedback();
	},

	resumeTransformFeedback: () => {
		GL40.glResumeTransformFeedback();
	},

	fenceSync: (condition, flags) => {
		return GL32.glFenceSync(condition ? condition : 0, flags ? flags : 0);
	},

	isSync: (sync) => {
		return GL32.glIsSync(sync ? sync : 0);
	},

	deleteSync: (sync) => {
		GL32.glDeleteSync(sync ? sync : 0);
	},

	clientWaitSync: (sync, flags, timeout) => {
		return GL32.glClientWaitSync(sync ? sync : 0, flags ? flags : 0, timeout ? timeout : 0);
	},

	waitSync: (sync, flags, timeout) => {
		GL32.glWaitSync(sync ? sync : 0, flags ? flags : 0, timeout ? timeout : 0);
	},

	getSyncParameter: (sync, pname) => {
		const buffer = bufferUtils.createIntBuffer(1);
		GL32.glGetSynciv(sync ? sync : 0, pname ? pname : 0, buffer);
		return buffer.get(0);
	},

	blitFramebuffer: (srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter) => {
		GL30.glBlitFramebuffer(srcX0 ? srcX0 : 0, srcY0 ? srcY0 : 0, srcX1 ? srcX1 : 0, srcY1 ? srcY1 : 0,
			dstX0 ? dstX0 : 0, dstY0 ? dstY0 : 0, dstX1 ? dstX1 : 0, dstY1 ? dstY1 : 0, mask ? mask : 0, filter ? filter : 0);
	},

	framebufferTextureLayer: (target, attachment, texture, level, layer) => {
		GL30.glFramebufferTextureLayer(target ? target : 0, attachment ? attachment : 0, texture ? texture : 0, 
			level ? level : 0, layer ? layer : 0);
	},

	invalidateFramebuffer: (target, attachments) => {
		if (attachments && attachments.length) {
			const buffer = bufferUtils.createIntBuffer(attachments.length);
			for (let i = 0; i < attachments.length; i++) {
				buffer.put(i, attachments[i]);
			}
			GL43.glInvalidateFramebuffer(target ? target : 0, buffer);
		}
	},

	invalidateSubFramebuffer: (target, attachments, x, y, width, height) => {
		if (attachments && attachments.length) {
			const buffer = bufferUtils.createIntBuffer(attachments.length);
			for (let i = 0; i < attachments.length; i++) {
				buffer.put(i, attachments[i]);
			}
			GL43.glInvalidateSubFramebuffer(target ? target : 0, buffer, x ? x : 0, y ? y : 0, 
				width ? width : 0, height ? height : 0);
		}
	},

	readBuffer: (mode) => {
		GL11.glReadBuffer(mode ? mode : 0);
	},

	getInternalformatParameter: (target, internalformat, pname) => {
		const buffer = bufferUtils.createIntBuffer(1);
		GL42.glGetInternalformativ(target ? target : 0, internalformat ? internalformat : 0, pname ? pname : 0, buffer);
		return buffer.get(0);
	},

	renderbufferStorageMultisample: (target, samples, internalformat, width, height) => {
		GL30.glRenderbufferStorageMultisample(target ? target : 0, samples ? samples : 0, 
			internalformat ? internalformat : 0, width ? width : 0, height ? height : 0);
	},

	uniform1ui: (location, v0) => {
		GL30.glUniform1ui(location ? location : -1, v0 ? v0 : 0);
	},

	uniform2ui: (location, v0, v1) => {
		GL30.glUniform2ui(location ? location : -1, v0 ? v0 : 0, v1 ? v1 : 0);
	},

	uniform3ui: (location, v0, v1, v2) => {
		GL30.glUniform3ui(location ? location : -1, v0 ? v0 : 0, v1 ? v1 : 0, v2 ? v2 : 0);
	},

	uniform4ui: (location, v0, v1, v2, v3) => {
		GL30.glUniform4ui(location ? location : -1, v0 ? v0 : 0, v1 ? v1 : 0, v2 ? v2 : 0, v3 ? v3 : 0);
	},

	uniform1uiv: (location, value) => {
		if (value && value.length) {
			const buffer = bufferUtils.createIntBuffer(value.length);
			for (let i = 0; i < value.length; i++) {
				buffer.put(i, value[i]);
			}
			GL30.glUniform1uiv(location ? location : -1, buffer);
		}
	},

	uniform2uiv: (location, value) => {
		if (value && value.length) {
			const buffer = bufferUtils.createIntBuffer(value.length);
			for (let i = 0; i < value.length; i++) {
				buffer.put(i, value[i]);
			}
			GL30.glUniform2uiv(location ? location : -1, buffer);
		}
	},

	uniform3uiv: (location, value) => {
		if (value && value.length) {
			const buffer = bufferUtils.createIntBuffer(value.length);
			for (let i = 0; i < value.length; i++) {
				buffer.put(i, value[i]);
			}
			GL30.glUniform3uiv(location ? location : -1, buffer);
		}
	},

	uniform4uiv: (location, value) => {
		if (value && value.length) {
			const buffer = bufferUtils.createIntBuffer(value.length);
			for (let i = 0; i < value.length; i++) {
				buffer.put(i, value[i]);
			}
			GL30.glUniform4uiv(location ? location : -1, buffer);
		}
	},

	uniformMatrix2x3fv: (location, transpose, data) => {
		const buffer = gl.convertToFloatBuffer(data);
		GL21.glUniformMatrix2x3fv(location, transpose, buffer);
	},

	uniformMatrix3x2fv: (location, transpose, data) => {
		const buffer = gl.convertToFloatBuffer(data);
		GL21.glUniformMatrix3x2fv(location, transpose, buffer);
	},

	uniformMatrix2x4fv: (location, transpose, data) => {
		const buffer = gl.convertToFloatBuffer(data);
		GL21.glUniformMatrix2x4fv(location, transpose, buffer);
	},

	uniformMatrix4x2fv: (location, transpose, data) => {
		const buffer = gl.convertToFloatBuffer(data);
		GL21.glUniformMatrix4x2fv(location, transpose, buffer);
	},

	uniformMatrix3x4fv: (location, transpose, data) => {
		const buffer = gl.convertToFloatBuffer(data);
		GL21.glUniformMatrix3x4fv(location, transpose, buffer);
	},

	uniformMatrix4x3fv: (location, transpose, data) => {
		const buffer = gl.convertToFloatBuffer(data);
		GL21.glUniformMatrix4x3fv(location, transpose, buffer);
	},

	
	createFramebuffer: () => {
		return GL30.glGenFramebuffers();
	},

	deleteFramebuffer: (framebuffer) => {
		if (framebuffer) {
			GL30.glDeleteFramebuffers(framebuffer);
		}
	},

	bindFramebuffer: (target, framebuffer) => {
		GL30.glBindFramebuffer(target, framebuffer || 0);
	},

	isFramebuffer: (framebuffer) => {
		return framebuffer ? GL30.glIsFramebuffer(framebuffer) : false;
	},

	framebufferTexture2D: (target, attachment, textarget, texture, level) => {
		GL30.glFramebufferTexture2D(target, attachment, textarget, texture || 0, level);
	},

	framebufferRenderbuffer: (target, attachment, renderbuffertarget, renderbuffer) => {
		GL30.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer || 0);
	},

	checkFramebufferStatus: (target) => {
		return GL30.glCheckFramebufferStatus(target);
	},

	getFramebufferAttachmentParameter: (target, attachment, pname) => {
		const buffer = bufferUtils.createIntBuffer(1);
		GL30.glGetFramebufferAttachmentParameteriv(target, attachment, pname, buffer);
		return buffer.get(0);
	},

	createRenderbuffer: () => {
		return GL30.glGenRenderbuffers();
	},

	deleteRenderbuffer: (renderbuffer) => {
		if (renderbuffer) {
			GL30.glDeleteRenderbuffers(renderbuffer);
		}
	},

	bindRenderbuffer: (target, renderbuffer) => {
		GL30.glBindRenderbuffer(target, renderbuffer || 0);
	},

	isRenderbuffer: (renderbuffer) => {
		return renderbuffer ? GL30.glIsRenderbuffer(renderbuffer) : false;
	},

	renderbufferStorage: (target, internalformat, width, height) => {
		GL30.glRenderbufferStorage(target, internalformat, width, height);
	},

	getRenderbufferParameter: (target, pname) => {
		const buffer = bufferUtils.createIntBuffer(1);
		GL30.glGetRenderbufferParameteriv(target, pname, buffer);
		return buffer.get(0);
	},

	
	getActiveUniform: (program, index) => {
		const nameBuffer = bufferUtils.createByteBuffer(256);
		const lengthBuffer = bufferUtils.createIntBuffer(1);
		const sizeBuffer = bufferUtils.createIntBuffer(1);
		const typeBuffer = bufferUtils.createIntBuffer(1);
		
		GL20.glGetActiveUniform(program, index, lengthBuffer, sizeBuffer, typeBuffer, nameBuffer);
		
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
	},

	getActiveAttrib: (program, index) => {
		const nameBuffer = bufferUtils.createByteBuffer(256);
		const lengthBuffer = bufferUtils.createIntBuffer(1);
		const sizeBuffer = bufferUtils.createIntBuffer(1);
		const typeBuffer = bufferUtils.createIntBuffer(1);
		
		GL20.glGetActiveAttrib(program, index, lengthBuffer, sizeBuffer, typeBuffer, nameBuffer);
		
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
	},

	getUniformLocation: (program, name) => {
		return GL20.glGetUniformLocation(program, name);
	},

	getAttribLocation: (program, name) => {
		return GL20.glGetAttribLocation(program, name);
	},

	getProgramParameter: (program, pname) => {
		const buffer = bufferUtils.createIntBuffer(1);
		GL20.glGetProgramiv(program, pname, buffer);
		return buffer.get(0);
	},

	getShaderParameter: (shader, pname) => {
		const buffer = bufferUtils.createIntBuffer(1);
		GL20.glGetShaderiv(shader, pname, buffer);
		return buffer.get(0);
	},

	getProgramInfoLog: (program) => {
		const length = gl.getProgramParameter(program, GL20.GL_INFO_LOG_LENGTH);
		if (length <= 0) return "";
		
		const buffer = bufferUtils.createByteBuffer(length);
		const lengthBuffer = bufferUtils.createIntBuffer(1);
		GL20.glGetProgramInfoLog(program, lengthBuffer, buffer);
		
		const logBytes = new Array(lengthBuffer.get(0));
		for (let i = 0; i < lengthBuffer.get(0); i++) {
			logBytes[i] = buffer.get(i);
		}
		return String.fromCharCode.apply(null, logBytes);
	},

	getShaderInfoLog: (shader) => {
		const length = gl.getShaderParameter(shader, GL20.GL_INFO_LOG_LENGTH);
		if (length <= 0) return "";
		
		const buffer = bufferUtils.createByteBuffer(length);
		const lengthBuffer = bufferUtils.createIntBuffer(1);
		GL20.glGetShaderInfoLog(shader, lengthBuffer, buffer);
		
		const logBytes = new Array(lengthBuffer.get(0));
		for (let i = 0; i < lengthBuffer.get(0); i++) {
			logBytes[i] = buffer.get(i);
		}
		return String.fromCharCode.apply(null, logBytes);
	},

	
	convertToFloatBuffer: (data) => {
		if (!data) return null;
		
		const buffer = bufferUtils.createFloatBuffer(data.length);
		for (let i = 0; i < data.length; i++) {
			buffer.put(i, data[i]);
		}
		return buffer;
	},

	
	uniformMatrix2fv: (location, transpose, data) => {
		const buffer = gl.convertToFloatBuffer(data);
		GL20.glUniformMatrix2fv(location, transpose, buffer);
	},

	uniformMatrix3fv: (location, transpose, data) => {
		const buffer = gl.convertToFloatBuffer(data);
		GL20.glUniformMatrix3fv(location, transpose, buffer);
	},

	uniformMatrix4fv: (location, transpose, data) => {
		const buffer = gl.convertToFloatBuffer(data);
		GL20.glUniformMatrix4fv(location, transpose, buffer);
	},

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
	
	getExtension: (name) => {
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
			case 'EXT_color_buffer_float':
				return {}; // Renderable float color buffers
			case 'EXT_color_buffer_half_float':
				return {}; // Renderable half-float color buffers
			case 'OES_texture_float_linear':
				return {}; // Linear filtering for float textures
			case 'OES_texture_half_float_linear':
				return {};
			case 'OES_texture_float':
				return {};
			case 'OES_texture_half_float':
				return {};
			default:
				return null; // Extension not supported
		}
	},
	
	getShaderPrecisionFormat: (shaderType, precisionType) => {
		return {
			rangeMin: 127,
			rangeMax: 127,
			precision: 23
		};
	},
	
	getParameter: (pname) => {
		if (pname === undefined || pname === null) {
			return 0;
		}
		switch(pname) {
			case 0x1F00:
				return 'LWJGL';
			case 0x1F01:
				return 'LWJGL OpenGL Renderer';
			case 0x1F02:
				return 'WebGL 2.0 (OpenGL ES 3.0 Chromium)';
			case 0x8B8C:
				return 'WebGL GLSL ES 3.00 (OpenGL ES GLSL ES 3.0 Chromium)';
			case 0x0D33:
				return 16384;
			case 0x851C:
				return 16384;
			case 0x80E9:
				return 32;
			case 0x8872:
				return 32;
			case 0x8B4D:
				return 1024;
			case 0x8DFD:
				return 30;
			case 0x8B4C:
				return 1024;
			case 0x8869:
				return 16;
			case 0x8DFB:
				return new Int32Array([16384, 16384]);
			case 0x846D:
				return new Float32Array([1, 1024]);
			case 0x846E:
				return new Float32Array([1, 1]);
			case 0x8073:
				return 24;
			case 0x8D48:
				return 8;
			case 0x8D50:
			case 0x8D51:
			case 0x8D52:
				return 8;
			case 0x8D53:
				return 8;
			case 0x8B8D:
				return 0;
			case 0x8CA6:
			case 0x8CA7:
				return 0;
			case 0x8069:
				return 0;
			case 0x84E0:
				return 0x84C0;
			default:
				result = 0;
				break;
		}
		return result;
	},


	DEPTH_BUFFER_BIT: 0x00000100,
	STENCIL_BUFFER_BIT: 0x00000400,
	COLOR_BUFFER_BIT: 0x00004000,
	
	FALSE: 0,
	TRUE: 1,
	
	POINTS: 0x0000,
	LINES: 0x0001,
	LINE_LOOP: 0x0002,
	LINE_STRIP: 0x0003,
	TRIANGLES: 0x0004,
	TRIANGLE_STRIP: 0x0005,
	TRIANGLE_FAN: 0x0006,
	
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
	
	FRONT: 0x0404,
	BACK: 0x0405,
	FRONT_AND_BACK: 0x0408,
	CULL_FACE: 0x0B44,
	
	TEXTURE_2D: 0x0DE1,
	BLEND: 0x0BE2,
	DITHER: 0x0BD0,
	STENCIL_TEST: 0x0B90,
	DEPTH_TEST: 0x0B71,
	SCISSOR_TEST: 0x0C11,
	POLYGON_OFFSET_FILL: 0x8037,
	SAMPLE_ALPHA_TO_COVERAGE: 0x809E,
	SAMPLE_COVERAGE: 0x80A0,
	
	NO_ERROR: 0,
	INVALID_ENUM: 0x0500,
	INVALID_VALUE: 0x0501,
	INVALID_OPERATION: 0x0502,
	OUT_OF_MEMORY: 0x0505,
	
	CW: 0x0900,
	CCW: 0x0901,
	
	BYTE: 0x1400,
	UNSIGNED_BYTE: 0x1401,
	SHORT: 0x1402,
	UNSIGNED_SHORT: 0x1403,
	INT: 0x1404,
	UNSIGNED_INT: 0x1405,
	FLOAT: 0x1406,
	FIXED: 0x140C,
	
	DEPTH_COMPONENT: 0x1902,
	ALPHA: 0x1906,
	RGB: 0x1907,
	RGBA: 0x1908,
	LUMINANCE: 0x1909,
	LUMINANCE_ALPHA: 0x190A,
	UNSIGNED_SHORT_4_4_4_4: 0x8033,
	UNSIGNED_SHORT_5_5_5_1: 0x8034,
	UNSIGNED_SHORT_5_6_5: 0x8363,
	
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
	
	VENDOR: 0x1F00,
	RENDERER: 0x1F01,
	VERSION: 0x1F02,
	EXTENSIONS: 0x1F03,
	
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
	
	REPEAT: 0x2901,
	CLAMP_TO_EDGE: 0x812F,
	MIRRORED_REPEAT: 0x8370,
	
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
	
	VERTEX_ATTRIB_ARRAY_ENABLED: 0x8622,
	VERTEX_ATTRIB_ARRAY_SIZE: 0x8623,
	VERTEX_ATTRIB_ARRAY_STRIDE: 0x8624,
	VERTEX_ATTRIB_ARRAY_TYPE: 0x8625,
	VERTEX_ATTRIB_ARRAY_NORMALIZED: 0x886A,
	VERTEX_ATTRIB_ARRAY_POINTER: 0x8645,
	VERTEX_ATTRIB_ARRAY_BUFFER_BINDING: 0x889F,
	
	MAX_TEXTURE_SIZE: 0x0D33,
	MAX_VIEWPORT_DIMS: 0x0D3A,
	SUBPIXEL_BITS: 0x0D50,
	RED_BITS: 0x0D52,
	GREEN_BITS: 0x0D53,
	BLUE_BITS: 0x0D54,
	ALPHA_BITS: 0x0D55,
	DEPTH_BITS: 0x0D56,
	STENCIL_BITS: 0x0D57,
	
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
	
	TEXTURE_3D: 0x806F,
	TEXTURE_2D_ARRAY: 0x8C1A,
	UNIFORM_BUFFER: 0x8A11,
	TRANSFORM_FEEDBACK_BUFFER: 0x8C8E,
}