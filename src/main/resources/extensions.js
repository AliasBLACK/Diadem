// Internal renderer
const renderer = new THREE.WebGLRenderer({ context: gl });
renderer.autoClear = false;
renderer.setPixelRatio(1);
renderer.setSize(1920, 1080);

// Render functions
globalThis.Render = function (scene, camera, clear = true) { if (clear) renderer.clear(); renderer.render(scene, camera); };
globalThis.ResizeWindow = function (w, h) { renderer.setSize(w, h); };
globalThis.ClearDepth = function() { renderer.clearDepth(); };

// Loaders
globalThis.LoadCubeTexture = function(filenames)
{
    if (!(filenames && Array.isArray(filenames) && filenames.length == 6))
        throw new Error("loadCubeTexture requires 6 face paths in order: +X, -X, +Y, -Y, +Z, -Z");
    const cubeTexture = new THREE.CubeTexture(filenames.map(f => loadTexture(f)));
    cubeTexture.needsUpdate = true;
    return cubeTexture;
}

// SDF-based text rendering using a single atlas and shader
globalThis.FontSDF = class {
    constructor(filePath, size) {
        this.size = (size == null ? 48 : size);
        this.baseSize = 32;
        // Load or build SDF atlas & metrics via Java side at baseSize
        const sdf = LoadFontSDF(filePath, this.baseSize);
        if (!sdf) throw new Error('LoadFontSDF failed for ' + filePath);

        // Use Java-created DataTexture when available, else build one from pixels
        this.atlasWidth = sdf.width;
        this.atlasHeight = sdf.height;
        this.pxRange = sdf.pxRange || 4.0; // MSDF pixel range in texels
        this.glyphs = sdf.glyphs; // per-char metrics and UVs
        this.kerning = sdf.kerning || null; // optional kerning pairs
        if (sdf.texture) {
            this.texture = sdf.texture;
        } else {
            const pixels = sdf.pixels; // Uint8Array length = width*height*4
            this.texture = new THREE.DataTexture(pixels, this.atlasWidth, this.atlasHeight, THREE.RGBAFormat);
            this.texture.needsUpdate = true;
        }
        // Important: our UVs are already flipped for WebGL; keep flipY disabled on the texture
        this.texture.flipY = false;
        // Diagnostics: confirm texture size matches atlas
        try {
            const img = this.texture.image;
            console.log('[MSDF DBG] texture size:', img && (img.width + 'x' + img.height));
            console.log('[MSDF DBG] atlas size  :', this.atlasWidth + 'x' + this.atlasHeight);
        } catch (e) { /* ignore */ }
        // Treat MSDF atlas as raw data (no color transforms), linear sampling, no mipmaps
        if (this.texture.colorSpace !== undefined && THREE.NoColorSpace !== undefined) {
            this.texture.colorSpace = THREE.NoColorSpace;
        }
        this.texture.generateMipmaps = false;
        // Use linear filtering for smooth MSDF sampling (mipmaps remain disabled)
        this.texture.minFilter = THREE.LinearFilter;
        this.texture.magFilter = THREE.LinearFilter;
        // Avoid tiling if UVs touch borders – clamp prevents repeating patterns
        this.texture.wrapS = THREE.ClampToEdgeWrapping;
        this.texture.wrapT = THREE.ClampToEdgeWrapping;
        this._baseMaterialFactory = () => this._createSDFMaterial();
        this._materialCache = {}; // colorHex -> MeshBasicMaterial (derived)
        this.ascender = sdf.ascender || this.size;
        this.descender = sdf.descender || 0;
        this.lineHeight = (sdf.lineHeight || (this.ascender - this.descender));
        // Tuning flags
        this.useAlphaSDF = false;        // when true, use A channel as single-channel SDF instead of MSDF median(R,G,B)
        this.uvInsetTexels = 0.125;      // how many texels to inset glyph UVs on each edge
        this.edgeSharpness = 1.0;        // 1.0 = default; <1 sharper, >1 softer
        // One-time constructor sanity checks for common glyphs
        try {
            const gA = this.glyphs['A'];
            const gB = this.glyphs['B'];
            const gC = this.glyphs['C'];
            const fmt = (g)=> g ? JSON.stringify({u0:g.u0,v0:g.v0,u1:g.u1,v1:g.v1, plane:[g.planeLeft,g.planeBottom,g.planeRight,g.planeTop], adv:g.advance}) : 'null';
            console.log('[MSDF DBG] glyphs[A]:', fmt(gA));
            console.log('[MSDF DBG] glyphs[B]:', fmt(gB));
            console.log('[MSDF DBG] glyphs[C]:', fmt(gC));
        } catch (e) { /* ignore */ }
        this._loggedGlyphsOnce = false;
        // Debug: show raw MSDF RGB without shader modifications
        this.debugMSDF = false;
        // Debug: show glyph silhouette (grayscale) using median of RGB channels (or a specific channel)
        this.showSilhouette = false;            // set false to disable
        this.silhouetteChannel = 'median';     // 'median' | 'r' | 'g' | 'b'
    }

    get height() { return this.lineHeight; }

    widthOf(text) {
        const scale = this.size / this.baseSize;
        let maxW = 0, lineW = 0;
        for (let i = 0; i < text.length; i++) {
            const ch = text[i];
            if (ch === '\n') { if (lineW > maxW) maxW = lineW; lineW = 0; continue; }
            const g = this.glyphs[ch];
            lineW += g ? (g.advance * scale) : this.size * 0.5;
        }
        if (lineW > maxW) maxW = lineW;
        return maxW;
    }

    _wordWrap(text, maxWidth) {
        if (maxWidth == null || maxWidth <= 0) return text.split('\n');
        const lines = [];
        const srcLines = text.split('\n');
        for (const src of srcLines) {
            const words = src.split(/(\s+)/);
            let cur = '';
            for (const w of words) {
                const t = cur + w;
                if (this.widthOf(t.trimEnd()) > maxWidth && cur.trim().length > 0) {
                    lines.push(cur.trimEnd());
                    cur = w.trimStart();
                } else cur = t;
            }
            if (cur.length > 0) lines.push(cur.trimEnd());
        }
        return lines;
    }

    _createSDFMaterial() {
        if (this.debugMSDF) {
            // Show raw MSDF RGB data directly
            const mat = new THREE.MeshBasicMaterial({
                color: 0xffffff,
                map: this.texture,
                transparent: false,
                depthTest: false,
                depthWrite: false,
            });
            mat.toneMapped = false;
            return mat;
        } else if (this.showSilhouette) {
            // Show glyph silhouette by rendering median(rgb) or chosen channel as grayscale
            const mat = new THREE.MeshBasicMaterial({
                color: 0xffffff,
                map: this.texture,
                transparent: false,
                depthTest: false,
                depthWrite: false,
            });
            mat.toneMapped = false;
            mat.defines = mat.defines || {};
            if (this.silhouetteChannel === 'r') mat.defines.DEBUG_CHANNEL_R = 1;
            else if (this.silhouetteChannel === 'g') mat.defines.DEBUG_CHANNEL_G = 1;
            else if (this.silhouetteChannel === 'b') mat.defines.DEBUG_CHANNEL_B = 1;
            mat.onBeforeCompile = (shader) => {
                const hook = '#include <map_fragment>';
                const chunk = `
                    #ifdef USE_MAP
                        vec4 texelColor = texture2D( map, vMapUv );
                        float r = texelColor.r;
                        float g = texelColor.g;
                        float b = texelColor.b;
                        float value = max(min(r, g), min(max(r, g), b)); // median(r,g,b)
                        #ifdef DEBUG_CHANNEL_R
                            value = r;
                        #elif defined(DEBUG_CHANNEL_G)
                            value = g;
                        #elif defined(DEBUG_CHANNEL_B)
                            value = b;
                        #endif
                        diffuseColor.rgb = vec3(value);
                        diffuseColor.a = 1.0;
                    #endif
                `;
                shader.fragmentShader = shader.fragmentShader.replace(hook, chunk);
            };
            return mat;
        } else {
            // Proper MSDF decode using median of RGB with screen-space derivatives
            const mat = new THREE.MeshBasicMaterial({
                color: 0xffffff,
                map: this.texture,
                transparent: true,
                depthTest: false,
                depthWrite: false,
            });
            mat.toneMapped = false;
            mat.onBeforeCompile = (shader) => {
                shader.fragmentShader = `#extension GL_OES_standard_derivatives : enable\n` + shader.fragmentShader;
                // uniforms for atlas size, px range, and sharpness control
                shader.uniforms.uTexSize = { value: new THREE.Vector2(this.atlasWidth, this.atlasHeight) };
                shader.uniforms.uPxRange = { value: this.pxRange };
                shader.uniforms.uEdgeSharpness = { value: this.edgeSharpness };
                shader.fragmentShader = shader.fragmentShader.replace(
                    '#include <common>',
                    `#include <common>\nuniform vec2 uTexSize;\nuniform float uPxRange;\nuniform float uEdgeSharpness;`
                );
                // Replace map_fragment include with MSDF decode using screen-space derivatives scaled by atlas size
                const inject = (src) => src.replace(/#include \<map_fragment\>/g, `
                    #ifdef USE_MAP
                        vec4 texelColor = texture2D( map, vMapUv );
                        #ifdef USE_ALPHA_SDF
                            float dist = texelColor.a;
                        #else
                            // median of r,g,b per Chlumsky MSDF
                            float r = texelColor.r;
                            float g = texelColor.g;
                            float b = texelColor.b;
                            float dist = max(min(r, g), min(max(r, g), b));
                        #endif
                        float sd = dist - 0.5; // signed distance in [ -0.5, 0.5 ]
                        // convert uv derivatives to pixels
                        vec2 uvDeriv = fwidth(vMapUv) * uTexSize;
                        float w = max(0.5 * (uvDeriv.x + uvDeriv.y), max(uvDeriv.x, uvDeriv.y));
                        // scale by generator pxRange and runtime sharpness
                        float width = (w / max(uPxRange, 1.0)) * max(uEdgeSharpness, 1e-6);
                        float a = clamp(sd / width + 0.5, 0.0, 1.0);
                        diffuseColor.a *= a;
                    #endif
                `);
                shader.fragmentShader = inject(shader.fragmentShader);
                // Toggle alpha SDF at compile time
                if (this.useAlphaSDF) {
                    shader.defines = shader.defines || {};
                    shader.defines.USE_ALPHA_SDF = 1;
                }
            };
            return mat;
        }
    }

    _getMaterialForColor(color) {
        // Normalize color to hex string key
        const key = (typeof color === 'number') ? ('#' + ('000000' + color.toString(16)).slice(-6)) : (new THREE.Color(color).getHexString());
        if (this._materialCache[key]) return this._materialCache[key];
        const mat = this._baseMaterialFactory();
        mat.color = new THREE.Color(color);
        this._materialCache[key] = mat;
        return mat;
    }

    generateMesh(text, color, position, width = null) {
        // Build one quad per glyph and pack into a single BufferGeometry
        const lines = (width != null) ? this._wordWrap(text, width) : text.split('\n');
        const material = this._getMaterialForColor(color);

        // Count vertices
        let quadCount = 0;
        for (const line of lines) quadCount += line.length;
        const pos = new Float32Array(quadCount * 6 * 3); // 2 triangles per quad
        const uv = new Float32Array(quadCount * 6 * 2);

        const scale = this.size / this.baseSize;
        const lh = this.lineHeight * scale;
        let glyphIndex = 0;
        let prevCh = null;
        for (let li = 0; li < lines.length; li++) {
            const line = lines[li];
            const lineWidth = this.widthOf(line);
            let penX = -lineWidth / 2;
            const penY = -li * lh;
            for (let i = 0; i < line.length; i++) {
                const ch = line[i];
                const g = this.glyphs[ch];
                if (!this._loggedGlyphsOnce) {
                    if (g) console.log('[MSDF DBG] render ch="' + ch + '" code=' + ch.charCodeAt(0), JSON.stringify({u0:g.u0,v0:g.v0,u1:g.u1,v1:g.v1, plane:[g.planeLeft,g.planeBottom,g.planeRight,g.planeTop], adv:g.advance}));
                    else console.warn('[MSDF DBG] render ch missing token:', ch, 'code=', ch ? ch.charCodeAt(0) : '');
                }
                // Apply kerning with previous glyph if available
                let kern = 0;
                if (this.kerning && prevCh && this.kerning[prevCh] && typeof this.kerning[prevCh][ch] === 'number') {
                    kern = this.kerning[prevCh][ch] * scale;
                }
                if (kern) penX += kern;
                const advance = g ? (g.advance * scale) : this.size * 0.5;
                if (g) {
                    // Prefer plane bounds if provided by MSDF generator to align with pen/baseline
                    let x, y, w, h;
                    if (g.planeLeft !== undefined && g.planeBottom !== undefined && g.planeRight !== undefined && g.planeTop !== undefined) {
                        const l = g.planeLeft * scale;
                        const b = g.planeBottom * scale;
                        const r = g.planeRight * scale;
                        const t = g.planeTop * scale;
                        x = penX + l;
                        y = penY + b;
                        w = (r - l);
                        h = (t - b);
                    } else {
                        // Fallback to legacy offset/size
                        x = penX + ((g.offsetX || 0) * scale);
                        y = penY + ((g.offsetY || 0) * scale);
                        w = g.w * scale;
                        h = g.h * scale;
                    }
                    // Inset UVs slightly to prevent sampling neighboring glyphs
                    const epsU = this.uvInsetTexels / this.atlasWidth;
                    const epsV = this.uvInsetTexels / this.atlasHeight;
                    const u0 = g.u0 + epsU, v0 = g.v0 + epsV, u1 = g.u1 - epsU, v1 = g.v1 - epsV;
                    // Two triangles (x,y) -> (x+w,y+h)
                    const pBase = glyphIndex * 18; // 6 verts * 3
                    const uBase = glyphIndex * 12; // 6 verts * 2
                    // Standard mapping
                    // tri 1: (x,y) (x+w,y) (x+w,y+h)
                    pos.set([x, y, 0,  x+w, y, 0,  x+w, y+h, 0], pBase);
                    uv.set([u0, v0,  u1, v0,  u1, v1], uBase);
                    // tri 2: (x,y) (x+w,y+h) (x,y+h)
                    pos.set([x, y, 0,  x+w, y+h, 0,  x, y+h, 0], pBase + 9);
                    uv.set([u0, v0,  u1, v1,  u0, v1], uBase + 6);
                    glyphIndex++;
                }
                penX += advance;
                prevCh = ch;
            }
            this._loggedGlyphsOnce = true;
        }

        const geo = new THREE.BufferGeometry();
        geo.setAttribute('position', new THREE.BufferAttribute(pos, 3));
        geo.setAttribute('uv', new THREE.BufferAttribute(uv, 2));
        geo.computeBoundingBox();
        geo.computeBoundingSphere();

        // Diagnostics: verify material map is our texture and report size
        try {
            const mapImg = material.map && material.map.image;
            console.log('[MSDF DBG] mat.map size:', mapImg ? (mapImg.width + 'x' + mapImg.height) : '(no image)');
            console.log('[MSDF DBG] mat.map===this.texture:', material.map === this.texture);
        } catch (e) { /* ignore */ }

        const mesh = new THREE.Mesh(geo, material);
        mesh.position.set(position.x, position.y, position.z);
        mesh.renderOrder = 1000;
        return mesh;
    }

    // Public: change whether to use alpha-only SDF decode and rebuild materials
    setUseAlphaSDF(flag) { this.useAlphaSDF = !!flag; this._rebuildMaterialCache(); }
    // Public: change UV inset in texels and use for subsequent meshes
    setUvInsetTexels(texels) { this.uvInsetTexels = Math.max(0, texels || 0); }
    // Public: adjust MSDF edge sharpness (1.0 default; <1 sharper, >1 softer). Rebuild materials to push new uniform.
    setEdgeSharpness(value) { this.edgeSharpness = Math.max(0.01, value || 1.0); this._rebuildMaterialCache(); }
    // Internal: clear and rebuild cached materials with current flags
    _rebuildMaterialCache() { this._materialCache = {}; }

    // Create a simple quad mesh that displays the atlas texture for inspection
    createAtlasDebugQuad(size = 256) {
        const geo = new THREE.PlaneGeometry(2035, 69);
        const mat = new THREE.MeshBasicMaterial({ map: this.texture, transparent: false });
        mat.toneMapped = false;
        const mesh = new THREE.Mesh(geo, mat);
        mesh.renderOrder = 999; // draw on top
        return mesh;
    }

    // Helper: build a single quad for a specific glyph to isolate issues
    generateSingleGlyphDebug(ch, color, position) {
        const g = this.glyphs[ch];
        if (!g) return null;
        const material = this._getMaterialForColor(color);
        const scale = this.size / this.baseSize;
        const l = (g.planeLeft !== undefined ? g.planeLeft : (g.offsetX||0)) * scale;
        const b = (g.planeBottom !== undefined ? g.planeBottom : (g.offsetY||0)) * scale;
        const r = (g.planeRight !== undefined ? g.planeRight : ((g.offsetX||0)+g.w)) * scale;
        const t = (g.planeTop !== undefined ? g.planeTop : ((g.offsetY||0)+g.h)) * scale;
        const x = position.x + l;
        const y = position.y + b;
        const w = (r - l);
        const h = (t - b);
        const epsU = this.uvInsetTexels / this.atlasWidth;
        const epsV = this.uvInsetTexels / this.atlasHeight;
        const u0 = g.u0 + epsU, v0 = g.v0 + epsV, u1 = g.u1 - epsU, v1 = g.v1 - epsV;
        const pos = new Float32Array(6*3);
        const uv = new Float32Array(6*2);
        pos.set([x, y, 0,  x+w, y, 0,  x+w, y+h, 0], 0);
        uv.set([u0, v0,  u1, v0,  u1, v1], 0);
        pos.set([x, y, 0,  x+w, y+h, 0,  x, y+h, 0], 9);
        uv.set([u0, v0,  u1, v1,  u0, v1], 6);
        const geo = new THREE.BufferGeometry();
        geo.setAttribute('position', new THREE.BufferAttribute(pos, 3));
        geo.setAttribute('uv', new THREE.BufferAttribute(uv, 2));
        const mesh = new THREE.Mesh(geo, material);
        // Diagnostics: confirm this material map
        try {
            const mapImg = material.map && material.map.image;
            console.log('[MSDF DBG] single glyph mat.map size:', mapImg ? (mapImg.width + 'x' + mapImg.height) : '(no image)');
            console.log('[MSDF DBG] single glyph mat.map===this.texture:', material.map === this.texture);
            console.log('[MSDF DBG] single glyph', ch, {u0,v0,u1,v1, plane:[g.planeLeft,g.planeBottom,g.planeRight,g.planeTop]});
        } catch (e) { /* ignore */ }
        mesh.position.set(0, 0, position.z);
        mesh.renderOrder = 1000;
        return mesh;
    }
}

const loadedFonts = {}
const curveSegments = 16;
// Geometry-based text rendering
globalThis.FontGeometry = class {
    constructor(filePath, size)
    {
        this.data = filePath in loadedFonts ? loadedFonts[filePath] : LoadFont(filePath);
        if (!(filePath in loadedFonts)) loadedFonts[filePath] = this.data;
        this.glyphs = {};
        this.size = (size == null ? 48 : size);
        this.isEastAsian = false; // set true to enable East Asian wrapping rules
        // cache of per-character geometries for this size to avoid regenerating shapes
        this._geomCache = {}; // char -> BufferGeometry (non-indexed), positioned at its intrinsic shape origin
    }
    // Width of a text string at this.size, using font metrics (ha) scaled by resolution
    widthOf(text) {
        if (!text || text.length === 0) return 0;
        const data = this.data.data || this.data; // THREE.Font stores metrics under .data
        const glyphs = data.glyphs || {};
        const res = data.resolution || 1000;
        const scale = this.size / res;
        let width = 0;
        let maxWidth = 0;
        for (let i = 0; i < text.length; i++) {
            const ch = text[i];
            if (ch === '\n') {
                if (width > maxWidth) maxWidth = width;
                width = 0;
                continue;
            }
            const g = glyphs[ch];
            if (g && typeof g.ha === 'number') width += g.ha * scale;
            else width += 0.5 * this.size; // fallback advance for missing glyphs/spaces
        }
        if (width > maxWidth) maxWidth = width;
        return maxWidth;
    }

    // Line height derived from font ascender/descender metrics at this.size
    get height() {
        const data = this.data.data || this.data;
        const res = data.resolution || 1000;
        const asc = (data.ascender ?? this.size);
        const desc = (data.descender ?? 0);
        return (asc - desc) * (this.size / res);
    }

    // Internal: word wrapping returning array of lines
    _wordWrap(text, maxWidth) {
        if (maxWidth == null || maxWidth <= 0) return text.split('\n');
        const lines = [];
        if (this.isEastAsian) {
            // Simple East Asian wrapping: break at character boundaries, avoid starting line with closing punctuation
            const forbidStart = new Set([')', ']', '}', '、', '。', '，', '．', '〕', '》', '」', '』', '】', '〉', '－', '：', '；', '！', '？', '…']);
            let cur = '';
            for (let i = 0; i < text.length; i++) {
                const ch = text[i];
                if (ch === '\n') { lines.push(cur); cur = ''; continue; }
                const tentative = cur + ch;
                if (this.widthOf(tentative) > maxWidth && cur.length > 0) {
                    // Avoid pushing a line that ends with opening punctuation
                    if (forbidStart.has(ch)) {
                        // try include previous char instead
                        lines.push(cur);
                        cur = ch; // start new line with this char regardless
                    } else {
                        lines.push(cur);
                        cur = ch;
                    }
                } else {
                    cur = tentative;
                }
            }
            if (cur.length > 0) lines.push(cur);
        } else {
            // Western: space-based wrapping
            const srcLines = text.split('\n');
            for (const src of srcLines) {
                const words = src.split(/(\s+)/); // keep spaces as tokens
                let cur = '';
                for (const w of words) {
                    const tentative = cur + w;
                    if (this.widthOf(tentative.trimEnd()) > maxWidth && cur.trim().length > 0) {
                        lines.push(cur.trimEnd());
                        cur = w.trimStart();
                    } else {
                        cur = tentative;
                    }
                }
                if (cur.length > 0) lines.push(cur.trimEnd());
            }
        }
        return lines;
    }

    generateMesh(text, color, position, width = null) {
        // Wrap text if width is specified
        const lines = (width != null) ? this._wordWrap(text, width) : text.split('\n');

        // Material shared for text
        const textMat = new THREE.MeshBasicMaterial({
            color: color,
            depthTest: false,
            depthWrite: false,
        });

        // Lightweight geometry merge utility (non-indexed concatenation)
        const mergeGeometries = (geos) => {
            const out = new THREE.BufferGeometry();
            const attrNames = new Set();
            for (const g of geos) {
                for (const name of Object.keys(g.attributes)) attrNames.add(name);
            }
            for (const name of attrNames) {
                const arrays = [];
                let itemSize = null;
                let arrayType = Float32Array;
                for (const g of geos) {
                    const a = g.getAttribute(name);
                    if (!a) continue;
                    if (itemSize == null) itemSize = a.itemSize;
                    if (a.array.constructor !== Float32Array) arrayType = a.array.constructor;
                    arrays.push(a.array);
                }
                if (arrays.length === 0) continue;
                let total = 0;
                for (const arr of arrays) total += arr.length;
                const merged = new arrayType(total);
                let offset = 0;
                for (const arr of arrays) { merged.set(arr, offset); offset += arr.length; }
                out.setAttribute(name, new THREE.BufferAttribute(merged, itemSize));
            }
            // Compute bounding box/sphere
            out.computeBoundingBox();
            out.computeBoundingSphere();
            // Dispose temps
            for (const g of geos) g.dispose();
            return out;
        };

        // Build geometry by cloning cached glyph geometries and translating to layout positions, then merge all
        const lh = this.height; // line height in world units
        const data = this.data.data || this.data;
        const glyphs = data.glyphs || {};
        const res = data.resolution || 1000;
        const scale = this.size / res;
        const geos = [];

        const getGlyphGeometry = (ch) => {
            if (this._geomCache[ch]) return this._geomCache[ch];
            // Generate shapes for this single character
            const shapes = this.data.generateShapes(ch, this.size);
            if (!shapes || shapes.length === 0) {
                this._geomCache[ch] = null; // e.g., space
                return null;
            }
            const eg = new THREE.ExtrudeGeometry(shapes, {
                depth: 0,
                bevelEnabled: false,
                curveSegments: curveSegments,
            });
            // Convert to non-indexed BufferGeometry for fast merging and cache it
            if (eg.index) {
                const bg = eg.toNonIndexed();
                eg.dispose();
                this._geomCache[ch] = bg;
                return bg;
            }
            this._geomCache[ch] = eg;
            return eg;
        };

        for (let li = 0; li < lines.length; li++) {
            const line = lines[li];
            if (line.length === 0) continue;
            // Compute total width via metrics to center without needing a bounding box
            const lineWidth = this.widthOf(line);
            let x = -lineWidth / 2;
            const y = -li * lh;
            for (let i = 0; i < line.length; i++) {
                const ch = line[i];
                const g = glyphs[ch];
                const adv = (g && typeof g.ha === 'number') ? (g.ha * scale) : (0.5 * this.size);
                const src = getGlyphGeometry(ch);
                if (src) {
                    const clone = src.clone();
                    clone.translate(x, y, 0);
                    geos.push(clone);
                }
                x += adv;
            }
        }

        const merged = geos.length === 1 ? geos[0] : mergeGeometries(geos);
        // Dispose intermediate cloned geos after merge
        if (geos.length > 1) { for (const g of geos) g.dispose(); }
        const mesh = new THREE.Mesh(merged, textMat);
        mesh.renderOrder = 1000;
        mesh.position.set(position.x, position.y, position.z);
        return mesh;
    }
}

// Entity management
globalThis.activeEntities = {}
globalThis.inactiveEntities = {}
globalThis.Entity = class {

    constructor(...args) {
        this.id = null;
        inactiveEntities[this.constructor] = inactiveEntities[this.constructor] || [];
        inactiveEntities[this.constructor].push(this);
        this.init(...args);
    }
    
    init(...args)
    {
        this.Start(...args);
        activeEntities[this.constructor] = activeEntities[this.constructor] || [];
        activeEntities[this.constructor].push(this);
        inactiveEntities[this.constructor].splice(inactiveEntities[this.constructor].indexOf(this), 1);
        this.id = requestUpdate(this.Update.bind(this));
    }

    stop()
    {
        this.shutdown();
        inactiveEntities[this.constructor].push(this);
        activeEntities[this.constructor].splice(activeEntities[this.constructor].indexOf(this), 1);
        cancelUpdate(this.id);
        this.id = null;
    }

    // For overriding
    Update(delta) {}
    Start(...args) {}
    shutdown() {}
}

// Entity factory
globalThis.CreateEntity = function(entityClass, ...args) {
    if (entityClass in inactiveEntities && inactiveEntities[entityClass].length > 0)
    {
        let entity = inactiveEntities[entityClass].pop();
        entity.start(...args);
        return entity;
    }
    return new entityClass(...args);
}