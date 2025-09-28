# Diadem

<div align="center">
  <img width="400" height="314" alt="image" style="max-width: 100%; height: auto;" src="https://github.com/user-attachments/assets/e47582d4-24ca-453e-a9ff-3b7801f877bb" />
</div>

<br>

A minimal desktop **[Three.js](https://github.com/mrdoob/three.js)** implementation powered by **[LWJGL](https://github.com/LWJGL/lwjgl3)** and **[GraalJS](https://github.com/oracle/graaljs)**, providing native WebGL2 rendering without bulky webviews. Perfect for desktop applications that need 3D graphics with JavaScript flexibility, including potential integrations with native libraries like Steam and Twitch APIs.

## Philosophy

This project is a proof of concept for running Three.js applications natively on desktop without the overhead of browser engines or webviews. By bridging JavaScript WebGL2 calls directly to native OpenGL through LWJGL, we achieve:

- **Lightweight**: No Chromium/WebView dependencies
- **Native Performance**: Direct OpenGL calls via LWJGL
- **Desktop Integration**: Easy access to native APIs (Steam, Twitch, file system)
- **AOT Ready**: Designed for GraalVM Native Image compilation

## Requirements

- GraalVM 21 or higher (Community or Enterprise Edition)
- Maven 3.6+
- LWJGL 3.3.3 (OpenGL, GLFW)
- GraalJS 24.2.2
- Windows (configured for Windows natives, but can be adapted for other platforms)

## Quickstart

Write your Three.js applications in `src/main/src/main.js`:

```javascript
// Full Three.js API available
const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(75, 800/600, 0.1, 1000);
const renderer = new THREE.WebGLRenderer({ context: gl });

// Create your 3D objects
const geometry = new THREE.BoxGeometry();
const material = new THREE.MeshNormalMaterial();
const cube = new THREE.Mesh(geometry, material);
scene.add(cube);

camera.position.z = 3;

// Animation loop using requestAnimationFrame
function animate() {
	cube.rotation.x += 0.01;
	cube.rotation.y += 0.01;
	
	renderer.render(scene, camera);
}

requestAnimationFrame(animate);
```

## Project Structure

```
src/main/
├── java/black/alias/diadem/
│   ├── JSInit.java				# Main application launcher
│   ├── JSContext.java			 # JavaScript execution context
│   ├── Math/					  # Matrix and vector math utilities
│   └── Utils/					 # OpenGL adapters and buffer utilities
├── lib/
│   ├── three.cjs				  # Three.js CommonJS build
│   ├── renderer.js				# WebGL2 to LWJGL bridge
│   └── polyfills.js			   # Browser API polyfills
└── src/
	└── main.js					# Client application entry point
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

   The application will:
   - Initialize LWJGL OpenGL context
   - Load WebGL2 bridge and browser polyfills
   - Execute `src/main/src/main.js` with Three.js support
   - Render a rotating cube in a native window

## GraalVM Native Image

For AOT compilation:
```bash
native-image --no-fallback -cp target/classes black.alias.diadem.JSInit
```

## License

Apache License 2.0 - See LICENSE file for details.
