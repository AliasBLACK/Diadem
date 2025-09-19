package black.alias.diadem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

public class ScriptManager {
    private final JSContext jsContext;
    private final Path fallbackScriptsDirectory;
    private final boolean useResources;
    private final Settings settings;
    
    public ScriptManager(JSContext jsContext) {
        this.jsContext = jsContext;
        this.fallbackScriptsDirectory = Paths.get("scripts");
        this.settings = Settings.load();
        
        // Check if we can load scripts from resources (compiled/packaged mode)
        this.useResources = getClass().getResourceAsStream("/scripts/" + settings.getMainScript()) != null;
    }
    
    public void loadMainScript() throws IOException {
        String scriptName = settings.getMainScript();
        // Always try to load from filesystem first for proper module resolution
        Path mainScript = fallbackScriptsDirectory.resolve(scriptName);
        if (Files.exists(mainScript)) {
            // Load from filesystem (development mode) - this gives proper module context
            jsContext.executeModuleFromFile(mainScript.toString());
        } else if (useResources) {
            // Fallback to resources (compiled/packaged mode) - limited module resolution
            loadScriptFromResource(scriptName);
        } else {
            throw new IOException("Script not found: " + mainScript);
        }
    }
    
    private void loadScriptFromResource(String scriptName) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/scripts/" + scriptName)) {
            if (is == null) {
                throw new IOException("Script resource not found: /scripts/" + scriptName);
            }
            String scriptContent = new String(is.readAllBytes());
            jsContext.executeModule(scriptContent);
        }
    }
    
    public Path getScriptsDirectory() {
        return useResources ? null : fallbackScriptsDirectory;
    }
    
    public boolean scriptExists(String scriptPath) {
        if (useResources) {
            return getClass().getResourceAsStream("/scripts/" + scriptPath) != null;
        } else {
            return Files.exists(fallbackScriptsDirectory.resolve(scriptPath));
        }
    }
    
    public boolean isUsingResources() {
        return useResources;
    }
    
    public Settings getSettings() {
        return settings;
    }
}
