package black.alias.diadem.Loaders;

import java.io.IOException;
import java.nio.file.*;
import black.alias.diadem.JSContext;
import black.alias.diadem.Settings;

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
		
		Path mainScript = fallbackScriptsDirectory.resolve(scriptName);
		if (Files.exists(mainScript)) {
			// Development mode: use file:// URL so relative imports resolve against the file location
			String fileUrl = mainScript.toAbsolutePath().toUri().toString();
			String importAndInstantiate = String.format(
				"import Main from '%s'; globalThis.mainEntity = new Main();",
				fileUrl
			);
			jsContext.executeModule(importAndInstantiate);
		} else if (useResources) {
			loadScriptFromResource(scriptName);
		} else {
			throw new IOException("Script not found: " + mainScript);
		}
	}
	
	private void loadScriptFromResource(String scriptName) throws IOException {
		try {
			// Packaged mode: import using an absolute resource path so relative imports resolve
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
