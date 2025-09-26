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
     * Load texture from image file and create Three.js DataTexture
     */
    public Value loadTexture(String texturePath) {
        try {
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
                        pixelData[index] = 255;     // R
                        pixelData[index + 1] = 0;   // G
                        pixelData[index + 2] = 0;   // B
                        pixelData[index + 3] = 255; // A
                    } else {
                        pixelData[index] = 0;       // R
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
        texturePath = texturePath.replaceAll("^[/]*assets/", "");
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
            pixelData[i * 4 + 2] = pixel & 0xFF;         // B
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
     * Public static utility to create a DataTexture from RGBA pixel data using a provided JS Context and THREE object.
     * This allows other loaders (e.g., FontLoader) to convert CPU pixel buffers to GPU textures consistently.
     */
    public static Value createDataTextureFromPixelData(Context jsContext, Value threeJS, int[] pixelData, int width, int height) {
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
        texture.putMember("flipY", false);
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
