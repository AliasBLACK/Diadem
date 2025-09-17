package black.alias.diadem.Utils;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

/**
 * File system utilities for loading local GLTF files and resources
 * Provides secure file access with path validation
 */
public class FileSystemUtils {
    
    private static final String MAIN_JS_DIR = "src/main/src/";
    private static final String[] ALLOWED_EXTENSIONS = {".gltf", ".glb", ".bin", ".jpg", ".jpeg", ".png", ".webp", ".ktx2", ".txt"};
    
    /**
     * Read a file as bytes relative to main.js directory
     * @param relativePath Relative path from main.js location
     * @return File contents as byte array
     * @throws IOException if file cannot be read
     */
    public static byte[] readFileBytes(String relativePath) throws IOException {
        Path filePath = validateFilePath(relativePath);
        return Files.readAllBytes(filePath);
    }
    
    /**
     * Read a file as text relative to main.js directory
     * @param relativePath Relative path from main.js location
     * @return File contents as string
     * @throws IOException if file cannot be read
     */
    public static String readFileText(String relativePath) throws IOException {
        Path filePath = validateFilePath(relativePath);
        return Files.readString(filePath);
    }
    
    /**
     * Asynchronously read a file as bytes
     * @param relativePath Relative path from main.js location
     * @return CompletableFuture containing file bytes
     */
    public static CompletableFuture<byte[]> readFileBytesAsync(String relativePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return readFileBytes(relativePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Asynchronously read a file as text
     * @param relativePath Relative path from main.js location
     * @return CompletableFuture containing file text
     */
    public static CompletableFuture<String> readFileTextAsync(String relativePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return readFileText(relativePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Check if a file exists relative to main.js
     * @param relativePath Relative path from main.js location
     * @return true if file exists and is readable
     */
    public static boolean fileExists(String relativePath) {
        try {
            Path filePath = validateFilePath(relativePath);
            return Files.exists(filePath) && Files.isReadable(filePath);
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Get the MIME type for a file based on its extension
     * @param filename The filename or path
     * @return MIME type string
     */
    public static String getMimeType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        
        switch (extension) {
            case ".gltf":
                return "model/gltf+json";
            case ".glb":
                return "model/gltf-binary";
            case ".bin":
                return "application/octet-stream";
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".webp":
                return "image/webp";
            case ".ktx2":
                return "image/ktx2";
            case ".txt":
                return "text/plain";
            default:
                return "application/octet-stream";
        }
    }
    
    /**
     * Create the main.js directory if it doesn't exist
     * @throws IOException if directory cannot be created
     */
    public static void ensureMainJsDirectory() throws IOException {
        Path mainJsPath = Paths.get(MAIN_JS_DIR);
        if (!Files.exists(mainJsPath)) {
            Files.createDirectories(mainJsPath);
        }
    }
    
    /**
     * List all GLTF files relative to main.js directory
     * @return Array of relative paths to GLTF files
     * @throws IOException if directory cannot be read
     */
    public static String[] listGltfFiles() throws IOException {
        Path mainJsPath = Paths.get(MAIN_JS_DIR);
        if (!Files.exists(mainJsPath)) {
            return new String[0];
        }
        
        return Files.walk(mainJsPath)
                .filter(Files::isRegularFile)
                .map(path -> mainJsPath.relativize(path).toString())
                .filter(filename -> filename.toLowerCase().endsWith(".gltf") || filename.toLowerCase().endsWith(".glb"))
                .toArray(String[]::new);
    }
    
    /**
     * Validate and resolve a file path relative to main.js
     * @param relativePath Relative path from main.js location
     * @return Validated absolute path
     * @throws IOException if path is invalid or unsafe
     */
    private static Path validateFilePath(String relativePath) throws IOException {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            throw new IOException("File path cannot be null or empty");
        }
        
        // Normalize path - allow parent directory references for relative paths
        String normalizedPath = Paths.get(relativePath).normalize().toString();
        
        // Check file extension
        String extension = getFileExtension(normalizedPath);
        boolean allowedExtension = false;
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(extension)) {
                allowedExtension = true;
                break;
            }
        }
        if (!allowedExtension) {
            throw new IOException("File extension not allowed: " + extension);
        }
        
        // Resolve full path relative to main.js directory
        Path basePath = Paths.get(MAIN_JS_DIR).toAbsolutePath().normalize();
        Path fullPath = basePath.resolve(normalizedPath).normalize();
        
        // Basic security check - ensure we're not going too far up the directory tree
        Path projectRoot = Paths.get(".").toAbsolutePath().normalize();
        if (!fullPath.startsWith(projectRoot)) {
            throw new IOException("File path resolves outside of project directory: " + relativePath);
        }
        
        return fullPath;
    }
    
    /**
     * Get file extension from filename
     * @param filename The filename
     * @return File extension including the dot, or empty string if no extension
     */
    private static String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot);
        }
        return "";
    }
}
