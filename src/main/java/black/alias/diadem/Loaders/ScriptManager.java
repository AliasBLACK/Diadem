package black.alias.diadem.Loaders;

import java.io.IOException;
import java.nio.file.Path;
import black.alias.diadem.JSContext;
import black.alias.diadem.Settings;

public class ScriptManager {
    private final JSContext jsContext;
    private final Settings settings;
    
    public ScriptManager(JSContext jsContext) {
        this.jsContext = jsContext;
        this.settings = Settings.load();
    }
    
    public void loadMainScript() throws IOException {
        String scriptName = settings.getMainScript();
        loadScriptFromResource(scriptName);
    }
    
    private void loadScriptFromResource(String scriptName) throws IOException {
        try {
            // Import using an absolute resource path so relative imports resolve
            String resourcePath = "/scripts/" + scriptName;
            String importAndInstantiate = String.format(
                "import Main from '%s'; globalThis.mainEntity = new Main();",
                resourcePath
            );
            jsContext.executeModule(importAndInstantiate);
        } catch (Exception e) {
            throw new IOException("Failed to load and instantiate Main entity from resource /scripts/" + scriptName + ": " + e.getMessage(), e);
        }
    }
    
    public Path getScriptsDirectory() {
        return null;
    }
    
    public boolean scriptExists(String scriptPath) {
        return getClass().getResourceAsStream("/scripts/" + scriptPath) != null;
    }
    
    public boolean isUsingResources() {
        return true;
    }
    
    public Settings getSettings() {
        return settings;
    }
}
