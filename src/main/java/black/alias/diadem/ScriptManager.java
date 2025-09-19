package black.alias.diadem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

public class ScriptManager {
    private final JSContext jsContext;
    private final Path fallbackScriptsDirectory;
    private final boolean useResources;
    
    public ScriptManager(JSContext jsContext) {
        this.jsContext = jsContext;
        this.fallbackScriptsDirectory = Paths.get("scripts");
        
        // Check if we can load scripts from resources (compiled/packaged mode)
        this.useResources = getClass().getResourceAsStream("/scripts/main.js") != null;
    }
    
    public void loadMainScript() throws IOException {
        if (useResources) {
            // Load from classpath resources (compiled/packaged mode)
            loadScriptFromResource("main.js");
        } else {
            // Fall back to project root scripts folder (development mode)
            Path mainScript = fallbackScriptsDirectory.resolve("main.js");
            if (Files.exists(mainScript)) {
                jsContext.executeModuleFile(mainScript.toString());
            } else {
                throw new IOException("Script not found: " + mainScript);
            }
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
}
