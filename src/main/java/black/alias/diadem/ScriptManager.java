package black.alias.diadem;

import java.io.IOException;
import java.nio.file.*;

public class ScriptManager {
    private final JSContext jsContext;
    private final Path scriptsDirectory;
    
    public ScriptManager(JSContext jsContext) {
        this.jsContext = jsContext;
        String jpackageAppPath = System.getProperty("jpackage.app-path");
        this.scriptsDirectory = jpackageAppPath != null ?
            Paths.get(jpackageAppPath).getParent().resolve("scripts") :
            Paths.get("src/main/resources/scripts");
    }
    
    public void loadMainScript() throws IOException {
        Path mainScript = scriptsDirectory.resolve("main.js");
        jsContext.executeModuleFile(mainScript.toString());
    }
    
    public Path getScriptsDirectory() {
        return scriptsDirectory;
    }
    
    public boolean scriptExists(String scriptPath) {
        return Files.exists(scriptsDirectory.resolve(scriptPath));
    }
}
