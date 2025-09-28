package black.alias.diadem.Loaders;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.Context;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.stb.STBImage;

/**
 * Dedicated texture loading utility for creating Three.js DataTextures from image files
 * Supports both resource-based loading (compiled mode) and filesystem loading (development mode)
 */
public class TextureLoader {
	private final Context jsContext;
	private final Value threeJS;
	private final Path fallbackAssetsDirectory;
	private final boolean useResources;
	
	public TextureLoader(Context jsContext, Value threeJS) {
		this.jsContext = jsContext;
		this.threeJS = threeJS;
		this.fallbackAssetsDirectory = Paths.get("assets");
		
		// Check if we can load assets from resources (compiled/packaged mode)
		this.useResources = getClass().getResourceAsStream("/assets/test-module.js") != null;
	}
	
	/**
	 * Load HDR texture (.hdr) using STB and create a Three.js DataTexture with HalfFloatType (RGB)
	 */
	public Value loadHDRTexture(String texturePath) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			ByteBuffer fileBytes = readFileToByteBuffer(texturePath);
			if (fileBytes == null) {
				System.err.println("HDR file not found: " + texturePath);
				return null;
			}

			// Query image info
			var w = stack.mallocInt(1);
			var h = stack.mallocInt(1);
			var comp = stack.mallocInt(1);
			if (!STBImage.stbi_info_from_memory(fileBytes, w, h, comp)) {
				System.err.println("stbi_info_from_memory failed: " + STBImage.stbi_failure_reason());
				return null;
			}

			// Load as float RGB data
			FloatBuffer hdrData = STBImage.stbi_loadf_from_memory(fileBytes, w, h, comp, 3);
			if (hdrData == null) {
				System.err.println("stbi_loadf_from_memory failed: " + STBImage.stbi_failure_reason());
				return null;
			}
			int width = w.get(0);
			int height = h.get(0);

			try {
				// Convert float32 to 16-bit half floats as per Three.js RGBELoader pattern
				int numPixels = width * height;
				int[] halfFloatData = new int[numPixels * 3]; // RGB
				for (int i = 0; i < numPixels; i++) {
					float r = hdrData.get(i * 3);
					float g = hdrData.get(i * 3 + 1);
					float b = hdrData.get(i * 3 + 2);
					halfFloatData[i * 3] = floatToHalf(r);
					halfFloatData[i * 3 + 1] = floatToHalf(g);
					halfFloatData[i * 3 + 2] = floatToHalf(b);
				}

				// Create Uint16Array in JS and then DataTexture(HalfFloatType, RGB)
				Value Uint16Array = jsContext.eval("js", "Uint16Array");
				Value imageData = Uint16Array.newInstance(halfFloatData);

				Value DataTexture = threeJS.getMember("DataTexture");
				Value HalfFloatType = threeJS.getMember("HalfFloatType");
				Value RGBFormat = threeJS.getMember("RGBFormat");
				Value LinearSRGBColorSpace = threeJS.getMember("LinearSRGBColorSpace");
				Value LinearFilter = threeJS.getMember("LinearFilter");

				Value texture = DataTexture.newInstance(imageData, width, height, RGBFormat, HalfFloatType);
				texture.putMember("needsUpdate", true);
				texture.putMember("flipY", false);
				texture.putMember("colorSpace", LinearSRGBColorSpace);
				texture.putMember("generateMipmaps", false);
				texture.putMember("minFilter", LinearFilter);
				texture.putMember("magFilter", LinearFilter);
				return texture;

			} finally {
				STBImage.stbi_image_free(hdrData);
			}
		} catch (Exception e) {
			System.err.println("Error loading HDR texture: " + texturePath + " - " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	private ByteBuffer readFileToByteBuffer(String texturePath) throws IOException {
		byte[] bytes;
		if (useResources) {
			try (InputStream is = getClass().getResourceAsStream("/assets/" + texturePath)) {
				if (is == null) return null;
				bytes = is.readAllBytes();
			}
		} else {
			Path imagePath = fallbackAssetsDirectory.resolve(texturePath);
			if (!Files.exists(imagePath)) return null;
			bytes = Files.readAllBytes(imagePath);
		}
		ByteBuffer buf = BufferUtils.createByteBuffer(bytes.length);
		buf.put(bytes);
		buf.flip();
		return buf;
	}

	// Float32 to Half-float (IEEE 754 binary16) conversion
	private int floatToHalf(float val) {
		int floatBits = Float.floatToIntBits(val);
		int sign = (floatBits >>> 16) & 0x8000;
		int mant = (floatBits & 0x007FFFFF);
		int exp  = (floatBits >>> 23) & 0xFF;

		if (exp < 103) {
			return sign;
		}
		if (exp > 142) {
			int bits = sign | 0x7C00;
			bits |= (exp == 255 && mant != 0) ? 1 : 0;
			return bits;
		}
		if (exp < 113) {
			mant |= 0x00800000;
			int t = 113 - exp;
			int bits = sign | (mant >> (t + 13));
			if (((mant >> (t + 12)) & 1) != 0) bits++;
			return bits;
		}
		int bits = sign | ((exp - 112) << 10) | (mant >> 13);
		if ((mant & 0x00001000) != 0) bits++;
		return bits & 0xFFFF;
	}
	/**
	 * Load texture from image file and create Three.js DataTexture
	 */
	public Value loadTexture(String texturePath) {
		try {
			// Auto-detect HDR by file extension and delegate
			if (texturePath != null) {
				String lower = texturePath.toLowerCase();
				if (lower.endsWith(".hdr")) {
					return loadHDRTexture(texturePath);
				}
			}
			// Load image using Java ImageIO
			BufferedImage bufferedImage = loadImageFromPath(texturePath);
			
			if (bufferedImage == null) {
				return null;
			}
			
			return createDataTextureFromImage(bufferedImage, texturePath);
			
		} catch (Exception e) {
			System.err.println("Error loading texture: " + texturePath + " - " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Load texture with specific dimensions (useful for procedural textures)
	 */
	public Value loadTextureWithSize(String texturePath, int targetWidth, int targetHeight) {
		try {
			BufferedImage originalImage = loadImageFromPath(texturePath);
			
			if (originalImage == null) {
				return null;
			}
			
			// Resize image if needed
			BufferedImage resizedImage = resizeImage(originalImage, targetWidth, targetHeight);
			return createDataTextureFromImage(resizedImage, texturePath);
			
		} catch (Exception e) {
			System.err.println("Error loading texture with size: " + texturePath + " - " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Create a procedural checkerboard texture (useful for testing)
	 */
	public Value createCheckerboardTexture(int width, int height, int checkerSize) {
		try {
			// Create checkerboard pattern
			int[] pixelData = new int[width * height * 4];
			
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int index = (y * width + x) * 4;
					
					// Determine if this pixel should be red or blue based on checkerboard pattern
					boolean isRed = ((x / checkerSize) + (y / checkerSize)) % 2 == 0;
					
					if (isRed) {
						pixelData[index] = 255;	 // R
						pixelData[index + 1] = 0;   // G
						pixelData[index + 2] = 0;   // B
						pixelData[index + 3] = 255; // A
					} else {
						pixelData[index] = 0;	   // R
						pixelData[index + 1] = 0;   // G
						pixelData[index + 2] = 255; // B
						pixelData[index + 3] = 255; // A
					}
				}
			}
			
			return createDataTextureFromPixelData(pixelData, width, height, "checkerboard");
			
		} catch (Exception e) {
			System.err.println("Error creating checkerboard texture: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Load image from either resources or filesystem based on mode
	 */
	private BufferedImage loadImageFromPath(String texturePath) throws IOException {
		BufferedImage bufferedImage;
		
		if (useResources) {
			try (InputStream is = getClass().getResourceAsStream("/assets/" + texturePath)) {
				if (is == null) {
					return null;
				}
				bufferedImage = ImageIO.read(is);
			}
		} else {
			Path imagePath = fallbackAssetsDirectory.resolve(texturePath);
			if (!Files.exists(imagePath)) {
				return null;
			}
			bufferedImage = ImageIO.read(imagePath.toFile());
		}	  
		return bufferedImage;
	}
	
	/**
	 * Create Three.js DataTexture from BufferedImage
	 */
	private Value createDataTextureFromImage(BufferedImage bufferedImage, String texturePath) {
		int width = bufferedImage.getWidth();
		int height = bufferedImage.getHeight();
		
		// Extract RGBA pixel data
		int[] pixels = new int[width * height];
		bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);
		
		// Convert to Uint8Array format
		int[] pixelData = new int[width * height * 4];
		for (int i = 0; i < pixels.length; i++) {
			int pixel = pixels[i];
			pixelData[i * 4] = (pixel >> 16) & 0xFF; // R
			pixelData[i * 4 + 1] = (pixel >> 8) & 0xFF;  // G
			pixelData[i * 4 + 2] = pixel & 0xFF;		 // B
			pixelData[i * 4 + 3] = (pixel >> 24) & 0xFF; // A
		}
		
		return createDataTextureFromPixelData(pixelData, width, height, texturePath);
	}
	
	/**
	 * Create Three.js DataTexture from raw pixel data
	 */
	private Value createDataTextureFromPixelData(int[] pixelData, int width, int height, String name) {
		// Create Uint8Array in JavaScript
		Value Uint8Array = jsContext.eval("js", "Uint8Array");
		Value imageData = Uint8Array.newInstance(pixelData);
		
		// Get Three.js classes
		Value DataTexture = threeJS.getMember("DataTexture");
		Value UnsignedByteType = threeJS.getMember("UnsignedByteType");
		Value RGBAFormat = threeJS.getMember("RGBAFormat");
		
		// Create Three.js DataTexture
		Value texture = DataTexture.newInstance(imageData, width, height, RGBAFormat, UnsignedByteType);
		texture.putMember("needsUpdate", true);
		texture.putMember("flipY", false); // Important for proper orientation
		
		return texture;
	}
	
	/**
	 * Resize image to target dimensions
	 */
	private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
		BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D g2d = resizedImage.createGraphics();
		
		// Use high-quality rendering hints
		g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, 
						   java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, 
						   java.awt.RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, 
						   java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
		
		g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
		g2d.dispose();
		
		return resizedImage;
	}
	
	/**
	 * Check if texture exists at the given path
	 */
	public boolean textureExists(String texturePath) {
		if (useResources) {
			return getClass().getResourceAsStream("/assets/" + texturePath) != null;
		} else {
			return Files.exists(fallbackAssetsDirectory.resolve(texturePath));
		}
	}
	
	/**
	 * Get texture loading mode (resources vs filesystem)
	 */
	public boolean isUsingResources() {
		return useResources;
	}
}
