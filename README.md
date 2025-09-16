# Three.js LWJGL Integration - Proof of Concept

This project demonstrates running Three.js JavaScript code on a native LWJGL OpenGL ES context using GraalVM Community and GraalJS Community. It creates a fake Canvas API that bridges JavaScript WebGL calls to native OpenGL ES calls through LWJGL.

## Features

- **GraalJS Integration**: Uses GraalVM's JavaScript engine to execute Three.js code
- **Fake Canvas API**: Implements a JavaScript-compatible Canvas and WebGL context that maps to LWJGL OpenGL ES
- **Three.js Compatibility**: Includes a minimal Three.js implementation for basic 3D rendering
- **Cube Rendering**: Demonstrates rendering a rotating green cube as a proof of concept

## Requirements

- Java 17 or higher
- GraalVM Community Edition (recommended)
- Maven 3.6+
- Windows (configured for Windows natives, but can be adapted for other platforms)

## Project Structure

```
src/main/java/com/example/
├── ThreeJSLWJGLApp.java           # Main application class
├── canvas/
│   ├── FakeCanvas.java            # Fake HTML5 Canvas implementation
│   └── FakeWebGLRenderingContext.java  # WebGL context that maps to OpenGL ES
└── js/
    ├── JavaScriptBridge.java      # GraalJS integration and Three.js loader
    ├── WindowObject.java          # Fake window object
    ├── DocumentObject.java        # Fake document object
    ├── ConsoleObject.java         # Console implementation for debugging
    └── PerformanceObject.java     # Performance API stub
```

## Building and Running

1. **Build the project:**
   ```bash
   mvn clean compile
   ```

2. **Run the application:**
   ```bash
   mvn exec:java
   ```

   Or alternatively:
   ```bash
   mvn exec:java -Dexec.mainClass="com.example.ThreeJSLWJGLApp"
   ```

## How It Works

1. **LWJGL Window**: Creates an OpenGL ES context using GLFW
2. **Fake Canvas**: Implements Canvas and WebGL APIs that JavaScript expects
3. **GraalJS Context**: Sets up a JavaScript execution environment with browser-like globals
4. **Three.js Bridge**: Loads a minimal Three.js implementation that works with the fake Canvas
5. **Rendering Loop**: Executes JavaScript animation code that renders through native OpenGL ES

## Key Components

### FakeWebGLRenderingContext
Maps WebGL API calls to LWJGL OpenGL ES calls:
- `gl.createShader()` → `GLES20.glCreateShader()`
- `gl.bufferData()` → `GLES20.glBufferData()`
- `gl.drawElements()` → `GLES20.glDrawElements()`

### JavaScriptBridge
- Creates GraalJS context with browser-like environment
- Injects fake `window`, `document`, `console` objects
- Loads minimal Three.js implementation
- Executes JavaScript animation code

### Minimal Three.js
Implements core Three.js classes:
- `THREE.Scene`, `THREE.PerspectiveCamera`
- `THREE.BoxGeometry`, `THREE.MeshBasicMaterial`
- `THREE.Mesh`, `THREE.WebGLRenderer`

## Limitations

This is a proof of concept with several limitations:
- Minimal Three.js implementation (only basic cube rendering)
- No texture support
- Simplified matrix operations
- No advanced Three.js features (lighting, shadows, etc.)
- Basic animation loop

## Extending the Project

To extend this proof of concept:
1. Add more Three.js features to the minimal implementation
2. Implement texture loading and binding
3. Add more geometry types and materials
4. Implement proper matrix transformations
5. Add support for Three.js loaders and utilities

## Troubleshooting

- Ensure GraalVM is properly installed and configured
- Check that LWJGL natives match your platform
- Verify OpenGL ES 2.0 support on your graphics card
- Enable verbose logging for debugging JavaScript execution

## License

This is a proof of concept project for educational purposes.
