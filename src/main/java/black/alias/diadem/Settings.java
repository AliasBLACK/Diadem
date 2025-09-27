package black.alias.diadem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Settings {
    private String windowTitle = "Diadem Game Engine";
    private int resolutionWidth = 1024;
    private int resolutionHeight = 576;
    private boolean fullscreen = false;
    private boolean vsync = true;
    private String savePrefix = "save";
    private String saveEncryptionKey = "625";
    private String mainScript = "main.js";
    
    public static Settings load() {
        Settings settings = new Settings();
        
        try {
            String jsonContent = null;
            
            // Try to load from resources first (compiled/packaged mode)
            try (InputStream is = Settings.class.getResourceAsStream("/scripts/settings.json")) {
                if (is != null) {
                    jsonContent = new String(is.readAllBytes());
                }
            } catch (IOException e) {
                // Ignore, will try file system fallback
            }
            
            // Fall back to project root scripts folder (development mode)
            if (jsonContent == null) {
                Path settingsFile = Paths.get("scripts/settings.json");
                if (Files.exists(settingsFile)) {
                    jsonContent = Files.readString(settingsFile);
                }
            }
            
            if (jsonContent != null) {
                settings.parseJson(jsonContent);
            }
            
        } catch (Exception e) {
            System.err.println("Warning: Could not load settings.json, using defaults: " + e.getMessage());
        }
        
        return settings;
    }
    
    private void parseJson(String jsonContent) {
        // Simple JSON parsing (avoiding external dependencies)
        // Remove comments and whitespace for easier parsing
        String cleanJson = jsonContent.replaceAll("//.*", "").replaceAll("\\s+", " ");
        
        windowTitle = extractStringValue(cleanJson, "windowTitle", windowTitle);
        resolutionWidth = extractIntValue(cleanJson, "resolutionWidth", resolutionWidth);
        resolutionHeight = extractIntValue(cleanJson, "resolutionHeight", resolutionHeight);
        fullscreen = extractBooleanValue(cleanJson, "fullscreen", fullscreen);
        vsync = extractBooleanValue(cleanJson, "vsync", vsync);
        savePrefix = extractStringValue(cleanJson, "savePrefix", savePrefix);
        saveEncryptionKey = extractStringValue(cleanJson, "saveEncryptionKey", saveEncryptionKey);
        mainScript = extractStringValue(cleanJson, "mainScript", mainScript);
    }
    
    private String extractStringValue(String json, String key, String defaultValue) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            // Ignore parsing errors, use default
        }
        return defaultValue;
    }
    
    private int extractIntValue(String json, String key, int defaultValue) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Exception e) {
            // Ignore parsing errors, use default
        }
        return defaultValue;
    }
    
    private boolean extractBooleanValue(String json, String key, boolean defaultValue) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*(true|false)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return Boolean.parseBoolean(m.group(1));
            }
        } catch (Exception e) {
            // Ignore parsing errors, use default
        }
        return defaultValue;
    }
    
    // Getters
    public String getWindowTitle() { return windowTitle; }
    public int getResolutionWidth() { return resolutionWidth; }
    public int getResolutionHeight() { return resolutionHeight; }
    public boolean isFullscreen() { return fullscreen; }
    public boolean isVsync() { return vsync; }
    public String getSavePrefix() { return savePrefix; }
    public String getSaveEncryptionKey() { return saveEncryptionKey; }
    public String getMainScript() { return mainScript; }
}
