package black.alias.diadem.Loaders;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.msdfgen.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.lwjgl.util.freetype.FreeType.*;

/**
 * FontLoader converts a TrueType font (TTF) into a Three.js Font object.
 *
 * Implementation notes:
 * - Uses FreeType via LWJGL to load glyph outlines in font units (FT_LOAD_NO_SCALE)
 * - Decomposes outlines into move/line/conic/cubic commands and encodes into the 'o' string
 *   using the same conventions as Three.js TTFLoader (m, l, q, b)
 * - Populates glyph metrics (ha, x_min, x_max) and global font metrics
 */
public class FontLoader {

    private final Context jsContext;
    private final Value threeJS;
    private final Value fontClass;
    private final Path fallbackAssetsDirectory;

    public FontLoader(Context jsContext) {
        this(jsContext, null);
    }

    public FontLoader(Context jsContext, Value threeJS) {
        this.jsContext = jsContext;
        this.threeJS = threeJS != null ? threeJS : jsContext.getBindings("js").getMember("THREE");
        this.fallbackAssetsDirectory = Paths.get("assets");

        // Load Font class from three.font.js
        try {
            InputStream is = getClass().getResourceAsStream("/three.font.js");
            String script = new String(is.readAllBytes());
            this.fontClass = (jsContext.eval(org.graalvm.polyglot.Source.newBuilder("js", script, "module.mjs")
                .mimeType("application/javascript+module")
                .build())).getMember("Font");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FontLoader script", e);
        }
    }

    /**
     * Load a TTF and build an SDF atlas for ASCII glyphs. Returns a JS object with:
     * { width, height, pixels(Uint8Array RGBA), glyphs{ ch: {advance,w,h,offsetX,offsetY,u0,v0,u1,v1} }, ascender, descender, lineHeight }
     */
    public Value LoadFontSDF(String filePath, int pixelSize) {
        // Generate an MSDF atlas using LWJGL msdfgen bindings. We avoid direct FreeType rasterization.
        PointerBuffer ftHandlePB = null;
        PointerBuffer fontHandlePB = null;
        ByteBuffer fontDataBuffer = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Init msdfgen FreeType helper
            ftHandlePB = stack.mallocPointer(1);
            int err = MSDFGenExt.msdf_ft_init(ftHandlePB);
            if (err != MSDFGen.MSDF_SUCCESS) throw new RuntimeException("msdf_ft_init failed: " + err);
            long ftHandle = ftHandlePB.get(0);

            // Load font: prefer classpath (packaged), fallback to filesystem (dev)
            fontHandlePB = stack.mallocPointer(1);
            String relPath = filePath.replaceAll("^[/]*assets/", "");
            Path fsPath = fallbackAssetsDirectory.resolve(relPath);
            boolean loaded = false;
            try (InputStream is = getClass().getResourceAsStream("/assets/" + relPath)) {
                if (is != null) {
                    byte[] bytes = is.readAllBytes();
                    fontDataBuffer = MemoryUtil.memAlloc(bytes.length);
                    fontDataBuffer.put(bytes).flip();
                    err = MSDFGenExt.msdf_ft_load_font_data(ftHandle, fontDataBuffer, fontHandlePB);
                    if (err != MSDFGen.MSDF_SUCCESS) throw new RuntimeException("msdf_ft_load_font_data failed: " + err);
                    System.err.println("[FontLoader] MSDF font loaded from classpath: /assets/" + relPath + " (" + bytes.length + " bytes)");
                    loaded = true;
                }
            }
            if (!loaded && Files.exists(fsPath)) {
                err = MSDFGenExt.msdf_ft_load_font(ftHandle, fsPath.toString(), fontHandlePB);
                if (err != MSDFGen.MSDF_SUCCESS) throw new RuntimeException("msdf_ft_load_font failed: " + err);
                System.err.println("[FontLoader] MSDF font loaded from filesystem: " + fsPath);
                loaded = true;
            }
            if (!loaded) {
                throw new RuntimeException("MSDF font not found in filesystem or classpath: " + relPath);
            }
            long fontHandle = fontHandlePB.get(0);

            // Configure atlas building
            final int firstChar = 32, lastChar = 126;
            // Allow tweaking via JVM props or environment without code changes
            double pxRangeCfg = 4.0;
            try {
                String sysPx = System.getProperty("diadem.msdf.pxRange");
                if (sysPx == null || sysPx.isEmpty()) sysPx = System.getenv("DIADEM_MSDF_PXRANGE");
                if (sysPx != null && !sysPx.isEmpty()) pxRangeCfg = Double.parseDouble(sysPx);
            } catch (Throwable ignore) {}
            int padding = 8;
            try {
                String sysPad = System.getProperty("diadem.msdf.padding");
                if (sysPad == null || sysPad.isEmpty()) sysPad = System.getenv("DIADEM_MSDF_PADDING");
                if (sysPad != null && !sysPad.isEmpty()) padding = Integer.parseInt(sysPad);
            } catch (Throwable ignore) {}
            if (padding < (int)Math.ceil(pxRangeCfg) + 2) padding = (int)Math.ceil(pxRangeCfg) + 2;
            System.err.println(String.format("[MSDF CFG] pxRange=%.2f padding=%d", pxRangeCfg, padding));
            class GlyphMSDF { byte[] data; int w,h; int left, top; int advance; int offsetX, offsetY; int planeL, planeB, planeR, planeT; float planeLEm, planeBEm, planeREm, planeTEm; float advanceEm; }
            java.util.Map<Integer, GlyphMSDF> glyphMap = new java.util.HashMap<>();

            // Generate per-glyph MSDF bitmaps
            for (int c = firstChar; c <= lastChar; c++) {
                // Load glyph shape in EM-normalized coordinates for stable scaling
                PointerBuffer shapePB = stack.mallocPointer(1);
                err = MSDFGenExt.msdf_ft_font_load_glyph(fontHandle, c, MSDFGenExt.MSDF_FONT_SCALING_EM_NORMALIZED, shapePB);
                if (err != MSDFGen.MSDF_SUCCESS) continue;
                long shape = shapePB.get(0);

                // Normalize, orient and color edges for MSDF
                MSDFGen.msdf_shape_normalize(shape);
                MSDFGen.msdf_shape_orient_contours(shape);
                MSDFGen.msdf_shape_edge_colors_simple(shape, 3.0);
                // Optional: validate; skip if invalid
                try (var stack2 = MemoryStack.stackPush()) {
                    java.nio.IntBuffer valid = stack2.mallocInt(1);
                    MSDFGen.msdf_shape_validate(shape, valid);
                    if (valid.get(0) == 0) {
                        MSDFGen.msdf_shape_free(shape);
                        continue;
                    }
                }

                // Compute bounds to choose transform
                MSDFGenBounds bounds = MSDFGenBounds.malloc(stack);
                MSDFGen.msdf_shape_get_bounds(shape, bounds);
                if (!Float.isFinite((float)bounds.l()) || !Float.isFinite((float)bounds.r()) ||
                    !Float.isFinite((float)bounds.b()) || !Float.isFinite((float)bounds.t())) {
                    MSDFGen.msdf_shape_free(shape);
                    continue;
                }
                int outW = Math.max(1, pixelSize);
                int outH = Math.max(1, pixelSize);
                double shapeW = bounds.r() - bounds.l();
                double shapeH = bounds.t() - bounds.b();

                // Build transform: bounds-fit with pixel margin (pxRange) and EM mapping
                MSDFGenTransform xf = MSDFGenTransform.calloc(stack);
                double pxRange = pxRangeCfg; // pixel distance range (safety band)
                double margin = pxRange;
                double s = Math.min(
                        (outW - 2*margin) / Math.max(1e-6, shapeW),
                        (outH - 2*margin) / Math.max(1e-6, shapeH)
                );
                xf.scale().set((float)s, (float)s); // pixels per EM
                // translation in EM units positions the glyph min bounds at pixel 'margin'
                float txEm = (float)(-bounds.l() + margin / s);
                float tyEm = (float)(-bounds.b() + margin / s);
                xf.translation().set(txEm, tyEm);
                // distance mapping range in EM units
                MSDFGenRange range = MSDFGenRange.calloc(stack);
                float emRange = (float)(pxRange / s);
                range.lower(-emRange); range.upper(emRange);
                xf.distance_mapping(range);

                // Allocate MSDF bitmap (3 channels)
                MSDFGenBitmap bmp = MSDFGenBitmap.malloc(stack);
                err = MSDFGen.msdf_bitmap_alloc(MSDFGen.MSDF_BITMAP_TYPE_MSDF, outW, outH, bmp);
                if (err != MSDFGen.MSDF_SUCCESS) { MSDFGen.msdf_shape_free(shape); continue; }

                // Generate MSDF
                err = MSDFGen.msdf_generate_msdf(bmp, shape, xf);
                if (err != MSDFGen.MSDF_SUCCESS) { MSDFGen.msdf_shape_free(shape); continue; }

                // Debug transform info
                System.err.println(String.format("[MSDF DBG] glyph=%c bounds l=%.3f b=%.3f r=%.3f t=%.3f out=%dx%d s=%.4f pxRange=%.2f emRange=%.6f tx=(%.3f,%.3f)",
                        c, bounds.l(), bounds.b(), bounds.r(), bounds.t(), outW, outH, s, pxRange, emRange, xf.translation().x(), xf.translation().y()));

                // Read pixels as float32 and convert to 0..255 bytes per channel
                PointerBuffer pPixels = stack.mallocPointer(1);
                MSDFGen.msdf_bitmap_get_pixels(bmp, pPixels);
                long pixPtr = pPixels.get(0);
                PointerBuffer pSize = stack.mallocPointer(1);
                MSDFGen.msdf_bitmap_get_byte_size(bmp, pSize);
                int byteSize = (int)pSize.get(0);
                // msdfgen stores floating point channels; convert properly
                java.nio.ByteBuffer raw = MemoryUtil.memByteBuffer(pixPtr, byteSize).order(java.nio.ByteOrder.nativeOrder());
                int floatCount = byteSize / 4;
                java.nio.FloatBuffer fb = raw.asFloatBuffer();
                if (fb.remaining() != floatCount) {
                    // Adjust limit if necessary
                    fb.limit(floatCount);
                }
                // Optional: log min/max for 'A' to verify range
                if (c == 'A') {
                    float minV = Float.POSITIVE_INFINITY, maxV = Float.NEGATIVE_INFINITY;
                    for (int i = 0; i < floatCount; i++) {
                        float v = fb.get(i);
                        if (v < minV) minV = v;
                        if (v > maxV) maxV = v;
                    }
                    System.err.println(String.format("[MSDF DBG] glyph=A raw float range: min=%.6f max=%.6f count=%d", minV, maxV, floatCount));
                }
                fb.rewind();
                // Convert interleaved float channels (RGBRGB...) to interleaved bytes using pxRange in PIXELS
                byte[] rgb = new byte[floatCount];
                for (int i = 0; i < floatCount; i += 3) {
                    float rF = fb.get(i);
                    float gF = fb.get(i + 1);
                    float bF = fb.get(i + 2);
                    // Map signed distance in PIXELS to [0,1] using generator pxRange
                    float rN = 0.5f + 0.5f * (rF / (float)pxRange);
                    float gN = 0.5f + 0.5f * (gF / (float)pxRange);
                    float bN = 0.5f + 0.5f * (bF / (float)pxRange);
                    if (rN < 0f) rN = 0f; else if (rN > 1f) rN = 1f;
                    if (gN < 0f) gN = 0f; else if (gN > 1f) gN = 1f;
                    if (bN < 0f) bN = 0f; else if (bN > 1f) bN = 1f;
                    rgb[i]     = (byte)(Math.round(rN * 255f) & 0xFF);
                    rgb[i + 1] = (byte)(Math.round(gN * 255f) & 0xFF);
                    rgb[i + 2] = (byte)(Math.round(bN * 255f) & 0xFF);
                }

                GlyphMSDF g = new GlyphMSDF();
                g.w = outW; g.h = outH;
                // Compute plane bounds in EM (include distance range)
                double leftEm = bounds.l() - emRange;
                double rightEm = bounds.r() + emRange;
                double bottomEm = bounds.b() - emRange;
                double topEm = bounds.t() + emRange;
                g.planeL = (int)Math.floor(leftEm * s);
                g.planeB = (int)Math.floor(bottomEm * s);
                g.planeR = (int)Math.ceil(rightEm * s);
                g.planeT = (int)Math.ceil(topEm * s);
                g.planeLEm = (float)leftEm;
                g.planeBEm = (float)bottomEm;
                g.planeREm = (float)rightEm;
                g.planeTEm = (float)topEm;
                // For backward compatibility, set offsets so that x=penX+offsetX, y=penY+offsetY matches plane min
                g.offsetX = g.planeL;
                g.offsetY = g.planeB;
                // Advance in EM and pixels (for backward compat)
                g.advanceEm = (float)(shapeW + 2*emRange);
                g.advance = (int)Math.round(g.advanceEm * s);
                g.data = rgb;
                glyphMap.put(c, g);

                // Debug: save one sample glyph (e.g., 'A') to project root
                if (c == 'A') {
                    try {
                        BufferedImage img = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_RGB);
                        int idx = 0;
                        for (int y = 0; y < outH; y++) {
                            for (int x = 0; x < outW; x++) {
                                int r = rgb[idx++] & 0xFF;
                                int gg = rgb[idx++] & 0xFF;
                                int b = rgb[idx++] & 0xFF;
                                int rgbInt = (r << 16) | (gg << 8) | b;
                                img.setRGB(x, y, rgbInt);
                            }
                        }
                        ImageIO.write(img, "png", new File("msdf_glyph_A.png"));
                        // Also save per-channel images for diagnosis (from interleaved rgb[])
                        BufferedImage rImg = new BufferedImage(outW, outH, BufferedImage.TYPE_BYTE_GRAY);
                        BufferedImage gImg = new BufferedImage(outW, outH, BufferedImage.TYPE_BYTE_GRAY);
                        BufferedImage bImg = new BufferedImage(outW, outH, BufferedImage.TYPE_BYTE_GRAY);
                        int p = 0;
                        for (int y = 0; y < outH; y++) {
                            for (int x = 0; x < outW; x++) {
                                int r = rgb[p++] & 0xFF;
                                int gch = rgb[p++] & 0xFF;
                                int bch = rgb[p++] & 0xFF;
                                int gr = (r << 16) | (r << 8) | r;
                                int gg = (gch << 16) | (gch << 8) | gch;
                                int gb = (bch << 16) | (bch << 8) | bch;
                                rImg.setRGB(x, y, gr);
                                gImg.setRGB(x, y, gg);
                                bImg.setRGB(x, y, gb);
                            }
                        }
                        ImageIO.write(rImg, "png", new File("msdf_glyph_A_r.png"));
                        ImageIO.write(gImg, "png", new File("msdf_glyph_A_g.png"));
                        ImageIO.write(bImg, "png", new File("msdf_glyph_A_b.png"));
                    } catch (Throwable ignore) {}
                }

                // Additional debug: generate a monochrome SDF for 'A' and save
                if (c == 'A') {
                    try (var dbg = MemoryStack.stackPush()) {
                        MSDFGenBitmap sdfBmp = MSDFGenBitmap.malloc(dbg);
                        int e2 = MSDFGen.msdf_bitmap_alloc(MSDFGen.MSDF_BITMAP_TYPE_SDF, outW, outH, sdfBmp);
                        if (e2 == MSDFGen.MSDF_SUCCESS) {
                            int e3 = MSDFGen.msdf_generate_sdf(sdfBmp, shape, xf);
                            if (e3 == MSDFGen.MSDF_SUCCESS) {
                                PointerBuffer pPix = dbg.mallocPointer(1);
                                MSDFGen.msdf_bitmap_get_pixels(sdfBmp, pPix);
                                long ptr = pPix.get(0);
                                PointerBuffer pSz = dbg.mallocPointer(1);
                                MSDFGen.msdf_bitmap_get_byte_size(sdfBmp, pSz);
                                int sz = (int)pSz.get(0);
                                java.nio.FloatBuffer sf = MemoryUtil.memByteBuffer(ptr, sz).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
                                BufferedImage sdfImg = new BufferedImage(outW, outH, BufferedImage.TYPE_BYTE_GRAY);
                                for (int y = 0; y < outH; y++) {
                                    for (int x = 0; x < outW; x++) {
                                        float dval = sf.get(y * outW + x);
                                        float n = 0.5f + 0.5f * (dval / emRange);
                                        if (n < 0f) n = 0f; else if (n > 1f) n = 1f;
                                        int v = Math.round(n * 255f);
                                        int gray = (v << 16) | (v << 8) | v;
                                        sdfImg.setRGB(x, y, gray);
                                    }
                                }
                                ImageIO.write(sdfImg, "png", new File("sdf_glyph_A.png"));
                            }
                        }
                    } catch (Throwable ignore) {}
                }
                // Free resources
                MSDFGen.msdf_shape_free(shape);
                MSDFGen.msdf_bitmap_free(bmp);
            }

            // Simple atlas packer (row-wise)
            final int maxRowWidth = 2048;
            int atlasW = 0, atlasH = padding; // start with top padding
            int rowW = padding, rowH = 0;     // start with left padding
            java.util.Map<Integer, int[]> placements = new java.util.HashMap<>();
            for (int c = firstChar; c <= lastChar; c++) {
                GlyphMSDF g = glyphMap.get(c);
                if (g == null) continue;
                int needW = g.w + padding;
                if (rowW + needW > maxRowWidth && rowW > padding) {
                    atlasW = Math.max(atlasW, rowW);
                    atlasH += rowH + padding; // add row height and bottom padding for row
                    rowW = padding; rowH = 0;  // new row starts with left padding
                }
                placements.put(c, new int[]{rowW, atlasH});
                rowW += needW;
                if (g.h > rowH) rowH = g.h;
            }
            atlasW = Math.max(atlasW, rowW);
            atlasH += rowH + padding; // add bottom padding at atlas end
            if (atlasW <= 0) atlasW = 2; if (atlasH <= 0) atlasH = pixelSize;

            // Compose RGBA atlas: copy MSDF RGB, set A=255
            int[] rgba = new int[atlasW * atlasH * 4];
            // Prefill background with neutral 0.5 gray (128) to avoid false edges between glyph rects
            for (int y = 0; y < atlasH; y++) {
                for (int x = 0; x < atlasW; x++) {
                    int dst = (y * atlasW + x) * 4;
                    rgba[dst] = 128;
                    rgba[dst + 1] = 128;
                    rgba[dst + 2] = 128;
                    rgba[dst + 3] = 255;
                }
            }
            java.util.Map<Integer, int[]> uvMap = new java.util.HashMap<>();
            for (int c = firstChar; c <= lastChar; c++) {
                GlyphMSDF g = glyphMap.get(c);
                if (g == null) continue;
                int[] place = placements.get(c);
                int x0 = place[0], y0 = place[1];
                for (int y = 0; y < g.h; y++) {
                    for (int x = 0; x < g.w; x++) {
                        int dst = ((y0 + y) * atlasW + (x0 + x)) * 4;
                        int src = (y * g.w + x) * 3;
                        int r = g.data[src] & 0xFF;
                        int gg = g.data[src + 1] & 0xFF;
                        int b = g.data[src + 2] & 0xFF;
                        rgba[dst] = r;
                        rgba[dst + 1] = gg;
                        rgba[dst + 2] = b;
                        rgba[dst + 3] = 255;
                    }
                }
                uvMap.put(c, new int[]{x0, y0, x0 + g.w, y0 + g.h});
            }

            // Build JS result object
            Value Object = jsContext.eval("js", "Object");
            Value result = Object.newInstance();
            Value Uint8Array = jsContext.eval("js", "Uint8Array");
            Value pixels = Uint8Array.newInstance(rgba);
            result.putMember("width", atlasW);
            result.putMember("height", atlasH);
            result.putMember("pixels", pixels);

            // Debug: save atlas to project root as PNG
            try {
                BufferedImage atlas = new BufferedImage(atlasW, atlasH, BufferedImage.TYPE_INT_ARGB);
                int idx = 0;
                for (int y = 0; y < atlasH; y++) {
                    for (int x = 0; x < atlasW; x++) {
                        int r = rgba[idx++] & 0xFF;
                        int gg = rgba[idx++] & 0xFF;
                        int b = rgba[idx++] & 0xFF;
                        int a = rgba[idx++] & 0xFF;
                        int argb = (a << 24) | (r << 16) | (gg << 8) | b;
                        atlas.setRGB(x, y, argb);
                    }
                }
                ImageIO.write(atlas, "png", new File("msdf_atlas.png"));

                // Also dump debug crops for select glyphs to validate placements and UVs
                int[] testChars = new int[]{'A','B','C','X','Y','Z'};
                for (int ch : testChars) {
                    int[] uvb = uvMap.get(ch);
                    if (uvb == null) continue;
                    int x0 = uvb[0], y0 = uvb[1];
                    int x1 = uvb[2], y1 = uvb[3];
                    int w = Math.max(1, x1 - x0);
                    int h = Math.max(1, y1 - y0);
                    try {
                        BufferedImage crop = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                        for (int yy = 0; yy < h; yy++) {
                            for (int xx = 0; xx < w; xx++) {
                                int argb = atlas.getRGB(x0 + xx, y0 + yy);
                                crop.setRGB(xx, yy, argb);
                            }
                        }
                        ImageIO.write(crop, "png", new File("msdf_atlas_glyph_" + (char)ch + ".png"));
                    } catch (Throwable ignore) {}
                }
            } catch (Throwable ignore) {}
            // Also provide a ready DataTexture if THREE is available
            try {
                if (this.threeJS != null) {
                    Value tex = TextureLoader.createDataTextureFromPixelData(jsContext, this.threeJS, rgba, atlasW, atlasH);
                    result.putMember("texture", tex);
                }
            } catch (Throwable ignore) {}

            // Glyphs map
            Value glyphsObj = Object.newInstance();
            for (int c = firstChar; c <= lastChar; c++) {
                GlyphMSDF g = glyphMap.get(c);
                if (g == null) continue;
                Value token = Object.newInstance();
                token.putMember("advance", g.advance);
                token.putMember("w", g.w);
                token.putMember("h", g.h);
                token.putMember("offsetX", g.offsetX);
                token.putMember("offsetY", g.offsetY);
                token.putMember("planeLeft", g.planeL);
                token.putMember("planeBottom", g.planeB);
                token.putMember("planeRight", g.planeR);
                token.putMember("planeTop", g.planeT);
                token.putMember("planeLeftEm", g.planeLEm);
                token.putMember("planeBottomEm", g.planeBEm);
                token.putMember("planeRightEm", g.planeREm);
                token.putMember("planeTopEm", g.planeTEm);
                token.putMember("advanceEm", g.advanceEm);
                int[] uvbox = uvMap.get(c);
                double u0 = (double)uvbox[0] / atlasW;
                double u1 = (double)uvbox[2] / atlasW;
                // Flip V: atlas y increases downward; WebGL UV v increases upward
                double v0 = 1.0 - ((double)uvbox[3] / atlasH); // bottom of glyph rect
                double v1 = 1.0 - ((double)uvbox[1] / atlasH); // top of glyph rect
                token.putMember("u0", u0);
                token.putMember("v0", v0);
                token.putMember("u1", u1);
                token.putMember("v1", v1);
                String ch = new String(Character.toChars(c));
                glyphsObj.putMember(ch, token);
            }
            result.putMember("glyphs", glyphsObj);

            // Approximate metrics
            result.putMember("ascender", pixelSize);
            result.putMember("descender", 0);
            result.putMember("lineHeight", pixelSize);

            return result;
        } catch (Throwable t) {
            throw new RuntimeException("LoadFontSDF(msdf) failed: " + t.getMessage(), t);
        } finally {
            try {
                if (fontHandlePB != null && fontHandlePB.get(0) != 0L) {
                    MSDFGenExt.msdf_ft_font_destroy(fontHandlePB.get(0));
                }
            } catch (Throwable ignore) {}
            try {
                if (ftHandlePB != null && ftHandlePB.get(0) != 0L) {
                    MSDFGenExt.msdf_ft_deinit(ftHandlePB.get(0));
                }
            } catch (Throwable ignore) {}
            if (fontDataBuffer != null) {
                try { MemoryUtil.memFree(fontDataBuffer); } catch (Throwable ignore) {}
            }
        }
    }

    private static class FaceLoadResult {
        LongBuffer1 face;        // pointer holder for FT_Face*
        ByteBuffer memoryBuffer; // non-null when using FT_New_Memory_Face; must live until FT_Done_Face
    }

    private FaceLoadResult loadFontFromPath(long library, String fontPath) throws IOException {
        int error;
        FaceLoadResult res = new FaceLoadResult();
        res.face = new LongBuffer1();
        fontPath = fontPath.replaceAll("^[/]*assets/", "");

        // 1) Try classpath resource first (packaged mode)
        try (InputStream is = getClass().getResourceAsStream("/assets/" + fontPath)) {
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                ByteBuffer direct = MemoryUtil.memAlloc(bytes.length);
                direct.put(bytes);
                direct.flip();
                res.memoryBuffer = direct; // keep alive until FT_Done_Face
                error = FT_New_Memory_Face(library, direct, 0, res.face);
                if (error != 0) {
                    throw new RuntimeException("Font loading failed (memory face): error " + error + " for resource '" + fontPath + "'");
                }
                return res;
            }
        }

        // 2) Fallback to filesystem (development mode)
        Path fontJavaPath = fallbackAssetsDirectory.resolve(fontPath);
        if (!Files.exists(fontJavaPath)) {
            return null;
        }
        error = FT_New_Face(library, fontJavaPath.toString(), 0, res.face);
        if (error != 0) {
            throw new RuntimeException("Font loading failed (file face): error " + error + " for file " + fontJavaPath);
        }
        return res;
    }

    /**
     * Load a TTF from a file path and return a JS object compatible with THREE.Font constructor.
     */
    public Value LoadFont(String filePath) {
        long library = 0L;
        FT_Face face = null;
        FaceLoadResult faceRes = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Init FreeType
            LongBuffer1 libOut = new LongBuffer1();
            int error = FT_Init_FreeType(libOut);
            if (error != 0) {
                throw new RuntimeException("FT_Init_FreeType failed: error " + error);
            }
            library = libOut.value;

            // Load face
            faceRes = loadFontFromPath(library, filePath);
            if (faceRes == null || faceRes.face == null) {
                throw new RuntimeException("Font not found: " + filePath);
            }
            face = FT_Face.create(faceRes.face.value);

            // We want font units (no scale) for outline and metrics to match Three.js conversion
            // Load flags for glyphs later: FT_LOAD_NO_SCALE | FT_LOAD_NO_BITMAP

            // Global metrics in font units
            int unitsPerEM = face.units_per_EM();
            int ascender = face.ascender();
            int descender = face.descender();
            FT_BBox fontBBox = face.bbox();
            String familyName = null;
            java.nio.ByteBuffer familyBuf = face.family_name();
            if (familyBuf != null) {
                familyName = MemoryUtil.memASCII(familyBuf);
            }
            int underlinePos = (int) face.underline_position();
            int underlineThickness = (int) face.underline_thickness();

            // Scale factor per TTFLoader.js: 100000 / ((unitsPerEm || 2048) * 72)
            double scale = 100000.0 / ((unitsPerEM != 0 ? unitsPerEM : 2048) * 72.0);

            // Build JS objects
            Value Object = jsContext.eval("js", "Object");
            Value result = Object.newInstance();
            Value glyphsObj = Object.newInstance();

            // Iterate all characters in cmap
            CharIter iter = new CharIter(face);
            while (iter.next()) {
                int codepoint = iter.charCode;
                int glyphIndex = iter.glyphIndex;
                if (glyphIndex == 0) continue;

                // Load glyph outline in font units
                int flags = FT_LOAD_NO_BITMAP | FT_LOAD_NO_SCALE;
                error = FT_Load_Glyph(face, glyphIndex, flags);
                if (error != 0) continue;

                FT_GlyphSlot slot = face.glyph();
                FT_Outline outline = slot.outline();

                // Skip if no outline
                if (outline == null || outline.n_contours() <= 0) {
                    // Still create advance metrics if available
                    addGlyphEntry(glyphsObj, codepoint, slot, null, scale);
                    continue;
                }

                // Decompose outline to path commands and compute bbox
                OutlinePath path = OutlinePath.decompose(outline);

                addGlyphEntry(glyphsObj, codepoint, slot, path, scale);
            }

            // Fill result fields
            result.putMember("glyphs", glyphsObj);
            result.putMember("familyName", familyName != null ? familyName : "");
            result.putMember("ascender", (int) Math.round(ascender * scale));
            result.putMember("descender", (int) Math.round(descender * scale));
            result.putMember("underlinePosition", underlinePos);
            result.putMember("underlineThickness", underlineThickness);

            Value bboxObj = Object.newInstance();
            bboxObj.putMember("xMin", fontBBox.xMin());
            bboxObj.putMember("xMax", fontBBox.xMax());
            bboxObj.putMember("yMin", fontBBox.yMin());
            bboxObj.putMember("yMax", fontBBox.yMax());
            result.putMember("boundingBox", bboxObj);

            result.putMember("resolution", 1000); // Three.js expects 1000
            result.putMember("original_font_information", Object.newInstance());

            return this.fontClass.newInstance(result);

        } catch (Throwable t) {
            throw new RuntimeException("LoadFont failed: " + t.getMessage(), t);
        } finally {
            // Ensure we clean up face and library
            if (face != null) FT_Done_Face(face);
            if (library != 0L) FT_Done_FreeType(library);
            // Free the direct memory buffer if we used FT_New_Memory_Face
            if (faceRes != null && faceRes.memoryBuffer != null) {
                try { MemoryUtil.memFree(faceRes.memoryBuffer); } catch (Throwable ignore) {}
            }
        }
    }

    private void addGlyphEntry(Value glyphsObj, int codepoint, FT_GlyphSlot slot, OutlinePath path, double scale) {
        Value Object = jsContext.eval("js", "Object");
        Value token = Object.newInstance();

        // Advance width in font units; FreeType metrics for NO_SCALE are in font units
        int advanceFU;
        if (slot.metrics() != null) {
            advanceFU = (int) slot.metrics().horiAdvance();
        } else {
            // Fallback: slot.advance is in 26.6 units even with NO_SCALE; approximate
            advanceFU = (int) (slot.advance().x() >> 6);
        }
        token.putMember("ha", (int) Math.round(advanceFU * scale));

        // x_min/x_max from outline path or metrics bearing/width
        int xMinFU;
        int xMaxFU;
        if (path != null && path.hasBounds) {
            xMinFU = path.minX;
            xMaxFU = path.maxX;
        } else if (slot.metrics() != null) {
            xMinFU = (int) slot.metrics().horiBearingX();
            xMaxFU = (int) (slot.metrics().horiBearingX() + slot.metrics().width());
        } else {
            xMinFU = 0;
            xMaxFU = advanceFU;
        }
        token.putMember("x_min", (int) Math.round(xMinFU * scale));
        token.putMember("x_max", (int) Math.round(xMaxFU * scale));

        String o = (path != null) ? path.toCommandString(scale) : "";
        token.putMember("o", o);

        // Map char to token
        String ch = new String(Character.toChars(codepoint));
        glyphsObj.putMember(ch, token);
    }

    // Helper: iterate cmap using FreeType API
    private static class CharIter {
        final FT_Face face;
        int charCode;
        int glyphIndex;
        private long c;
        private boolean started = false;

        CharIter(FT_Face face) { this.face = face; }
        boolean next() {
            if (!started) {
                long[] out = FT_Get_First_Char(face);
                c = out[0];
                glyphIndex = (int) out[1];
                started = true;
            } else {
                long[] out = FT_Get_Next_Char(face, c);
                c = out[0];
                glyphIndex = (int) out[1];
            }
            if (glyphIndex == 0) return false;
            if (c > Integer.MAX_VALUE) return false;
            charCode = (int) c;
            return true;
        }
    }

    // Outline path and decomposition using FreeType
    private static class OutlinePath {
        final StringBuilder sb = new StringBuilder();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean hasBounds = false;

        static OutlinePath decompose(FT_Outline outline) {
            OutlinePath path = new OutlinePath();

            FT_Outline_MoveToFuncI move = (to, _) -> {
                FT_Vector vec = FT_Vector.create(to);
                int x = (int) vec.x();
                int y = (int) vec.y();
                
                path.point(x, y);
                path.sb.append('m').append(' ').append(x).append(' ').append(y).append(' ');
                
                return 0;
            };

            FT_Outline_LineToFuncI line = (to, _) -> {
                FT_Vector vec = FT_Vector.create(to);
                int x = (int) vec.x();
                int y = (int) vec.y();
                
                path.point(x, y);
                path.sb.append('l').append(' ').append(x).append(' ').append(y).append(' ');
                
                return 0;
            };

            FT_Outline_ConicToFuncI conic = (control, to, _) -> {
                FT_Vector cvec = FT_Vector.create(control);
                FT_Vector tvec = FT_Vector.create(to);
                int cx = (int) cvec.x();
                int cy = (int) cvec.y();
                int tx = (int) tvec.x();
                int ty = (int) tvec.y();
                
                path.sb.append('q').append(' ')
                    .append(tx).append(' ')
                    .append(ty).append(' ')
                    .append(cx).append(' ')
                    .append(cy).append(' ');
                
                return 0;
            };

            FT_Outline_CubicToFuncI cubic = (control1, control2, to, _) -> {
                FT_Vector c1 = FT_Vector.create(control1);
                FT_Vector c2 = FT_Vector.create(control2);
                FT_Vector tv = FT_Vector.create(to);
                int c1x = (int) c1.x();
                int c1y = (int) c1.y();
                int c2x = (int) c2.x();
                int c2y = (int) c2.y();
                int tx = (int) tv.x();
                int ty = (int) tv.y();

                path.sb.append('b').append(' ')
                    .append(tx).append(' ')
                    .append(ty).append(' ')
                    .append(c1x).append(' ')
                    .append(c1y).append(' ')
                    .append(c2x).append(' ')
                    .append(c2y).append(' ');
                
                return 0;
            };

            FT_Outline_Funcs funcs = FT_Outline_Funcs.calloc();
            funcs.move_to(move);
            funcs.line_to(line);
            funcs.conic_to(conic);
            funcs.cubic_to(cubic);
            funcs.shift(0);
            funcs.delta(0);

            int err = FT_Outline_Decompose(outline, funcs, 0L);
            funcs.free();
            if (err != 0) {
                return path;
            }
            path.hasBounds = path.minX != Integer.MAX_VALUE;
            return path;
        }

        void point(int x, int y) {
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }

        String toCommandString(double scale) {
            if (sb.length() == 0) return "";
            String raw = sb.toString().trim();
            String[] tokens = raw.split(" ");
            StringBuilder out = new StringBuilder();
            for (String tok : tokens) {
                if (tok.isEmpty()) continue;
                char c = tok.charAt(0);
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                    out.append(Character.toLowerCase(c)).append(' ');
                } else {
                    try {
                        int v = Integer.parseInt(tok);
                        int scaled = (int) Math.round(v * scale);
                        out.append(scaled).append(' ');
                    } catch (NumberFormatException e) {
                        // should not happen
                    }
                }
            }
            return out.toString().trim();
        }
    }

    // Minimal helpers to pass/receive pointers from LWJGL C APIs
    private static class LongBuffer1 {
        long value;
    }

    // Wrappers around LWJGL functions that expect buffers
    private static int FT_Init_FreeType(LongBuffer1 out) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var p = stack.mallocPointer(1);
            int err = FreeType.FT_Init_FreeType(p);
            out.value = p.get(0);
            return err;
        }
    }

    private static int FT_New_Face(long library, String filepathname, int face_index, LongBuffer1 out) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var p = stack.mallocPointer(1);
            int err = FreeType.FT_New_Face(library, filepathname, face_index, p);
            out.value = p.get(0);
            return err;
        }
    }

    private static int FT_New_Memory_Face(long library, ByteBuffer file_base, int face_index, LongBuffer1 out) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var p = stack.mallocPointer(1);
            int err = FreeType.FT_New_Memory_Face(library, file_base, face_index, p);
            out.value = p.get(0);
            return err;
        }
    }

    private static void FT_Done_Face(FT_Face face) { FreeType.FT_Done_Face(face); }
    private static void FT_Done_FreeType(long library) { FreeType.FT_Done_FreeType(library); }

    private static long[] FT_Get_First_Char(FT_Face face) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer gidx = stack.mallocInt(1);
            long c = FreeType.FT_Get_First_Char(face, gidx);
            return new long[]{c, Integer.toUnsignedLong(gidx.get(0))};
        }
    }

    private static long[] FT_Get_Next_Char(FT_Face face, long charcode) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer gidx = stack.mallocInt(1);
            long c = FreeType.FT_Get_Next_Char(face, charcode, gidx);
            return new long[]{c, Integer.toUnsignedLong(gidx.get(0))};
        }
    }

    private static int FT_Load_Glyph(FT_Face face, int glyph_index, int load_flags) {
        return FreeType.FT_Load_Glyph(face, glyph_index, load_flags);
    }
}
