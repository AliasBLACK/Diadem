
package black.alias.diadem;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.io.IOAccess;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;
import java.util.function.Function;
import black.alias.diadem.Loaders.GLTFLoader;
import black.alias.diadem.Loaders.TextureLoader;

public class JSContext implements AutoCloseable {
    private final Context jsContext;
    private final Path THREE_MODULE_PATH = Paths.get("/virtual/three");
    private GLTFLoader gltfLoaderInstance = null;
    private TextureLoader textureLoaderInstance = null;
    
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
        
        try {
            String bridgeScript = loadBridgeScript();
            jsContext.eval("js", bridgeScript);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load WebGL2 bridge script", e);
        }
    }
    
    public void setupGLTFLoader() {
        try {
            bindGLTFFunction("loadGLTF", args -> {
                String filePath = (String) args[0];
                return getGLTFLoader().loadGLTF(filePath);
            });
        } catch (Exception e) {
            System.err.println("Failed to setup GLTF Loader: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private GLTFLoader getGLTFLoader() {
        if (gltfLoaderInstance == null) {
            Value threeJS = jsContext.getBindings("js").getMember("THREE");
            if (threeJS == null) {
                throw new RuntimeException("THREE.js is not loaded yet");
            }
            gltfLoaderInstance = new GLTFLoader(jsContext, threeJS, getTextureLoader());
        }
        return gltfLoaderInstance;
    }
    
    private void bindGLTFFunction(String functionName, java.util.function.Function<Object[], Object> handler) {
        jsContext.getBindings("js").putMember(functionName, new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] args) {
                return handler.apply(args);
            }
        });
    }
    
    public void setupTextureLoader() {
        try {
            bindTextureFunction("loadTexture", args -> {
                String texturePath = (String) args[0];
                return getTextureLoader().loadTexture(texturePath);
            });
            
            bindTextureFunction("loadTextureWithSize", args -> {
                if (args.length != 3) {
                    throw new IllegalArgumentException("loadTextureWithSize requires 3 arguments: texturePath, width, height");
                }
                String texturePath = (String) args[0];
                int width = ((Number) args[1]).intValue();
                int height = ((Number) args[2]).intValue();
                return getTextureLoader().loadTextureWithSize(texturePath, width, height);
            });
            
            bindTextureFunction("createCheckerboardTexture", args -> {
                if (args.length != 3) {
                    throw new IllegalArgumentException("createCheckerboardTexture requires 3 arguments: width, height, checkerSize");
                }
                int width = ((Number) args[0]).intValue();
                int height = ((Number) args[1]).intValue();
                int checkerSize = ((Number) args[2]).intValue();
                return getTextureLoader().createCheckerboardTexture(width, height, checkerSize);
            });
            
            bindTextureFunction("textureExists", args -> {
                String texturePath = (String) args[0];
                return getTextureLoader().textureExists(texturePath);
            });
            
        } catch (Exception e) {
            System.err.println("Failed to setup Texture Loader: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private TextureLoader getTextureLoader() {
        if (textureLoaderInstance == null) {
            Value threeJS = jsContext.getBindings("js").getMember("THREE");
            if (threeJS == null) {
                throw new RuntimeException("THREE.js is not loaded yet");
            }
            textureLoaderInstance = new TextureLoader(jsContext, threeJS);
        }
        return textureLoaderInstance;
    }
    
    private void bindTextureFunction(String functionName, java.util.function.Function<Object[], Object> handler) {
        jsContext.getBindings("js").putMember(functionName, new org.graalvm.polyglot.proxy.ProxyExecutable() {
            @Override
            public Object execute(Value... args) {
                // Convert GraalVM Value[] to Object[]
                Object[] argsArray = new Object[args.length];
                for (int i = 0; i < args.length; i++) {
                    if (args[i].isString()) {
                        argsArray[i] = args[i].asString();
                    } else if (args[i].isNumber()) {
                        argsArray[i] = args[i].asInt();
                    } else {
                        argsArray[i] = args[i];
                    }
                }
                return handler.apply(argsArray);
            }
        });
    }
    
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
            // Support resource-style absolute URIs for packaged mode
            if (uriStr.startsWith("/scripts/") || uriStr.startsWith("/assets/")) {
                return Paths.get(uriStr);
            }
            return defaultFS.parsePath(uri);
        }
        
        @Override
        public Path parsePath(String path) {
            if ("three".equals(path)) {
                return THREE_MODULE_PATH;
            }
            if ("three.core.min.js".equals(path)) {
                return Paths.get("/virtual/three.core.min.js");
            }
            // Support resource-style absolute paths for packaged mode
            if (path != null && (path.startsWith("/scripts/") || path.startsWith("/assets/"))) {
                return Paths.get(path);
            }
            return defaultFS.parsePath(path);
        }
        
        @Override
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
            if (THREE_MODULE_PATH.equals(path)) {
                String content = loadResourceAsString("/three.module.min.js");
                return createByteChannelFrom(content);
            }
            
            String pathStr = path.toString().replace('\\', '/');
            if ("/virtual/three.core.min.js".equals(pathStr)) {
                String content = loadResourceAsString("/three.core.min.js");
                return createByteChannelFrom(content);
            }
            // Serve packaged resources for scripts and assets
            if (pathStr.startsWith("/scripts/") || pathStr.startsWith("/assets/")) {
                String content = loadResourceAsString(pathStr);
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
                }
            };
        }
        
        @Override
        public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
            String pathStr = path.toString().replace('\\', '/');
            if (THREE_MODULE_PATH.equals(path) || "/virtual/three.core.min.js".equals(pathStr)) {
                return;
            }
            if (pathStr.startsWith("/scripts/") || pathStr.startsWith("/assets/")) {
                return; // classpath resources are readable
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
        return loadResourceAsString("/renderer.js");
    }
    
    private String loadResourceAsString(String resourcePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(is.readAllBytes());
        }
    }
    
    public Value executeScript(String script) {
        return jsContext.eval("js", script);
    }
    
    public Value executeModule(String moduleCode) {
        try {
            return jsContext.eval(org.graalvm.polyglot.Source.newBuilder("js", moduleCode, "module.mjs")
                .mimeType("application/javascript+module")
                .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute ES6 module", e);
        }
    }
    
    public Value executeModuleFromFile(String filePath) throws IOException {
        try {
            Path path = Paths.get(filePath);
            return jsContext.eval(org.graalvm.polyglot.Source.newBuilder("js", path.toFile())
                .mimeType("application/javascript+module")
                .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute ES6 module from file: " + filePath, e);
        }
    }
    
    public Value executeScriptFile(String filename) throws IOException {
        String script;
        if (filename.startsWith("/")) {
            script = loadResourceAsString(filename);
        } else {
            script = new String(Files.readAllBytes(Paths.get(filename)));
        }
        return executeScript(script);
    }
    
    public Value executeModuleFile(String filename) throws IOException {
        String moduleCode;
        if (filename.startsWith("/")) {
            moduleCode = loadResourceAsString(filename);
        } else {
            moduleCode = new String(Files.readAllBytes(Paths.get(filename)));
        }
        return executeModule(moduleCode);
    }
    
    public Context getJavaScriptContext() {
        return jsContext;
    }
    
    public void close() {
        jsContext.close();
    }
}
