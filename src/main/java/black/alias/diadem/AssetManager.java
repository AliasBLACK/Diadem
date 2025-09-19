package black.alias.diadem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

public class AssetManager {
    private final JSContext jsContext;
    private final Path fallbackAssetsDirectory;
    private final boolean useResources;
    
    public AssetManager(JSContext jsContext) {
        this.jsContext = jsContext;
        this.fallbackAssetsDirectory = Paths.get("assets");
        
        // Check if we can load assets from resources (compiled/packaged mode)
        this.useResources = getClass().getResourceAsStream("/assets/test-module.js") != null;
        
        System.out.println("AssetManager initialized - using " + 
                          (useResources ? "resources" : "filesystem") + " mode");
    }
    
    public String loadAssetAsString(String assetPath) throws IOException {
        if (useResources) {
            // Load from classpath resources (compiled/packaged mode)
            return loadAssetFromResource(assetPath);
        } else {
            // Fall back to project root assets folder (development mode)
            Path assetFile = fallbackAssetsDirectory.resolve(assetPath);
            if (Files.exists(assetFile)) {
                return Files.readString(assetFile);
            } else {
                throw new IOException("Asset not found: " + assetFile);
            }
        }
    }
    
    private String loadAssetFromResource(String assetPath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/assets/" + assetPath)) {
            if (is == null) {
                throw new IOException("Asset resource not found: /assets/" + assetPath);
            }
            return new String(is.readAllBytes());
        }
    }
    
    public boolean assetExists(String assetPath) {
        if (useResources) {
            return getClass().getResourceAsStream("/assets/" + assetPath) != null;
        } else {
            return Files.exists(fallbackAssetsDirectory.resolve(assetPath));
        }
    }
    
    public void setupAssetLoader() {
        try {
            // Expose asset loading function to JavaScript
            jsContext.getJavaScriptContext().getBindings("js").putMember("loadAsset", 
                new Function<String, String>() {
                    @Override
                    public String apply(String assetPath) {
                        try {
                            return loadAssetAsString(assetPath);
                        } catch (IOException e) {
                            System.err.println("Failed to load asset: " + assetPath + " - " + e.getMessage());
                            return null;
                        }
                    }
                });
                
            System.out.println("Asset loader function exposed to JavaScript as 'loadAsset(path)'");
        } catch (Exception e) {
            System.err.println("Failed to setup asset loader: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean isUsingResources() {
        return useResources;
    }
}
