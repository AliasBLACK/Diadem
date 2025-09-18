
package black.alias.diadem;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.io.IOAccess;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;
import java.util.function.Function;
import black.alias.diadem.Utils.GLTFLoader;

/**
 * WebGL2 Context that bridges JavaScript WebGL2 API calls to Java GLES implementation.
 * Provides a complete WebGL2-compatible interface for JavaScript applications.
 */
public class JSContext implements AutoCloseable {
    private final Context jsContext;
    
    private final Path THREE_MODULE_PATH = Paths.get("/virtual/three");
    
    public JSContext() {
        this.jsContext = Context.newBuilder("js")
            .allowAllAccess(true)
            .allowExperimentalOptions(true)
            .allowIO(IOAccess.newBuilder()
                .fileSystem(new CustomFileSystem())
                .build())
            .option("js.esm-eval-returns-exports", "true")
            .option("js.ecmascript-version", "2022")
            .build();
        
        // Load and initialize the WebGL2 bridge
        try {
            String bridgeScript = loadBridgeScript();
            jsContext.eval("js", bridgeScript);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to load WebGL2 bridge script", e);
        }
    }
    
    private GLTFLoader gltfLoaderInstance = null;
    
    /**
     * Setup GLTF Loader in the JavaScript context
     */
    public void setupGLTFLoader() {
        try {
            // Expose the loader function directly to JavaScript
            jsContext.getBindings("js").putMember("loadGLTF", new Function<String, Value>() {
                @Override
                public Value apply(String filePath) {
                    // Get Three.js when the function is called
                    Value threeJS = jsContext.getBindings("js").getMember("THREE");
                    if (threeJS == null) {
                        throw new RuntimeException("THREE.js is not loaded yet");
                    }
                    
                    // Create GLTFLoader instance only once (singleton pattern)
                    if (gltfLoaderInstance == null) {
                        gltfLoaderInstance = new GLTFLoader(jsContext, threeJS);
                    }
                    return gltfLoaderInstance.loadGLTF(filePath);
                }
            });
            
            
        } catch (Exception e) {
            System.err.println("Failed to setup GLTF Loader: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Custom FileSystem that maps 'three' module to the actual three.module.js file
     */
    private class CustomFileSystem implements FileSystem {
        private final FileSystem defaultFS = FileSystem.newDefaultFileSystem();
        
        @Override
        public Path parsePath(URI uri) {
            String uriStr = uri.toString();
            if ("three".equals(uriStr)) {
                return THREE_MODULE_PATH;
            }
            if (uriStr.startsWith("three.") && uriStr.endsWith(".js")) {
                return Paths.get("/virtual/" + uriStr);
            }
            return defaultFS.parsePath(uri);
        }
        
        @Override
        public Path parsePath(String path) {
            if ("three".equals(path)) {
                return THREE_MODULE_PATH;
            }
            if ("three.core.js".equals(path)) {
                return Paths.get("/virtual/three.core.js");
            }
            return defaultFS.parsePath(path);
        }
        
        @Override
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
            if (THREE_MODULE_PATH.equals(path)) {
                String content = new String(Files.readAllBytes(Paths.get("src/main/lib/three.module.js")));
                return createByteChannelFrom(content);
            }
            
            String pathStr = path.toString().replace('\\', '/');
            
            // Handle three.core.js (required by three.module.js)
            if ("/virtual/three.core.js".equals(pathStr)) {
                String content = new String(Files.readAllBytes(Paths.get("src/main/lib/three.core.js")));
                return createByteChannelFrom(content);
            }
            
            return defaultFS.newByteChannel(path, options, attrs);
        }
        
        private SeekableByteChannel createByteChannelFrom(String content) {
            byte[] bytes = content.getBytes();
            
            return new SeekableByteChannel() {
                private int position = 0;
                
                @Override
                public int read(ByteBuffer dst) throws IOException {
                    if (position >= bytes.length) {
                        return -1;
                    }
                    int remaining = Math.min(dst.remaining(), bytes.length - position);
                    dst.put(bytes, position, remaining);
                    position += remaining;
                    return remaining;
                }
                
                @Override
                public int write(ByteBuffer src) throws IOException {
                    throw new UnsupportedOperationException("Write not supported");
                }
                
                @Override
                public long position() throws IOException {
                    return position;
                }
                
                @Override
                public SeekableByteChannel position(long newPosition) throws IOException {
                    this.position = (int) newPosition;
                    return this;
                }
                
                @Override
                public long size() throws IOException {
                    return bytes.length;
                }
                
                @Override
                public SeekableByteChannel truncate(long size) throws IOException {
                    throw new UnsupportedOperationException("Truncate not supported");
                }
                
                @Override
                public boolean isOpen() {
                    return true;
                }
                
                @Override
                public void close() throws IOException {
                    // Nothing to close
                }
            };
        }
        
        // Delegate other methods to default FileSystem
        @Override
        public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
            String pathStr = path.toString().replace('\\', '/');
            if (THREE_MODULE_PATH.equals(path) || "/virtual/three.core.js".equals(pathStr)) {
                return;
            }
            defaultFS.checkAccess(path, modes, linkOptions);
        }
        
        @Override
        public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
            defaultFS.createDirectory(dir, attrs);
        }
        
        @Override
        public void delete(Path path) throws IOException {
            defaultFS.delete(path);
        }
        
        @Override
        public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
            return defaultFS.newDirectoryStream(dir, filter);
        }
        
        @Override
        public Path toAbsolutePath(Path path) {
            return defaultFS.toAbsolutePath(path);
        }
        
        @Override
        public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
            return defaultFS.toRealPath(path, linkOptions);
        }
        
        @Override
        public java.util.Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
            return defaultFS.readAttributes(path, attributes, options);
        }
    }
    
    private String loadBridgeScript() throws IOException {
        // Load the bridge script from resources
        return new String(Files.readAllBytes(Paths.get("src/main/lib/renderer.js")));
    }
    
    /**
     * Execute JavaScript code with WebGL2 context available
     */
    public Value executeScript(String script) {
        return jsContext.eval("js", script);
    }
    
    /**
     * Execute ES6 module code with WebGL2 context available
     */
    public Value executeModule(String moduleCode) {
        try {
            return jsContext.eval(org.graalvm.polyglot.Source.newBuilder("js", moduleCode, "module.mjs")
                .mimeType("application/javascript+module")
                .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute ES6 module", e);
        }
    }
    
    /**
     * Execute JavaScript code from a file
     */
    public Value executeScriptFile(String filename) throws IOException {
        String script = new String(Files.readAllBytes(Paths.get(filename)));
        return executeScript(script);
    }
    
    /**
     * Execute ES6 module from a file
     */
    public Value executeModuleFile(String filename) throws IOException {
        String moduleCode = new String(Files.readAllBytes(Paths.get(filename)));
        return executeModule(moduleCode);
    }
    
    /**
     * Get the JavaScript context for advanced operations
     */
    public Context getJavaScriptContext() {
        return jsContext;
    }
    
    /**
     * Close the context and free resources
     */
    public void close() {
        jsContext.close();
    }
}
