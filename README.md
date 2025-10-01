# Diadem

<div align="center">
  <img width="100%" alt="Screenshot 2025-10-01 111510" src="https://github.com/user-attachments/assets/7d3a17e4-31d1-4dda-818c-30292f8e4f86" />
</div>

<br>

A minimal desktop **[Three.js](https://github.com/mrdoob/three.js)** implementation powered by **[LWJGL](https://github.com/LWJGL/lwjgl3)** and **[GraalJS](https://github.com/oracle/graaljs)**, providing native WebGL2 rendering without bulky webviews. Features custom **[Assimp](https://github.com/assimp/assimp)**-based model loading and HDR texture support for PBR workflows. Perfect for desktop applications that need 3D graphics with JavaScript flexibility, including potential integrations with native libraries like Steam and Twitch APIs.

## Philosophy

This project is a proof of concept for running Three.js applications natively on desktop without the overhead of browser engines or webviews. By bridging JavaScript WebGL2 calls directly to native OpenGL through LWJGL, we achieve:

- **Lightweight**: No Chromium/WebView dependencies
- **Native Performance**: Direct OpenGL calls via LWJGL
- **Assimp Model Loading**: Load GLTF, FBX, OBJ and 40+ formats via custom Java loaders
- **HDR Texture Support**: Native HDR/EXR loading for PBR environment maps
- **Desktop Integration**: Easy access to native APIs (Steam, Twitch, file system)
- **AOT Ready**: Designed for GraalVM Native Image compilation

## Requirements

- GraalVM 21 or higher (Community or Enterprise Edition)
- Maven 3.6+
- LWJGL 3.3.3 (OpenGL, Assimp, STB)
- GraalJS 24.2.2
- Windows (configured for Windows natives, but can be adapted for other platforms)

**Note:** Uses AWT for windowing instead of GLFW for better Java integration.

## Quickstart

Write your Three.js applications in `scripts/main.js`:

```javascript
// Full Three.js API available
const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(75, 800/600, 0.1, 1000);
const renderer = new THREE.WebGLRenderer({ context: gl });

// Load models using Assimp
const gltf = loadModel('models/damagedHelmet/DamagedHelmet.gltf');
if (gltf && gltf.scene) {
	scene.add(gltf.scene);
}

// Load HDR environment maps
const hdrCube = loadCubeTexture([
	'textures/cube/pisaHDR/px.hdr',
	'textures/cube/pisaHDR/nx.hdr',
	'textures/cube/pisaHDR/py.hdr',
	'textures/cube/pisaHDR/ny.hdr',
	'textures/cube/pisaHDR/pz.hdr',
	'textures/cube/pisaHDR/nz.hdr',
]);
scene.background = hdrCube;

camera.position.z = 3;

// Animation loop
function animate() {
	renderer.render(scene, camera);
}

requestAnimationFrame(animate);
```

## Project Structure

```
src/main/
├── java/black/alias/diadem/
│   ├── JSInit.java              # Main application launcher
│   ├── JSContext.java           # JavaScript execution context
│   └── Loaders/                 # Assimp model & HDR texture loaders
├── resources/
│   ├── three.js                 # Three.js core build
│   ├── renderer.js              # WebGL2 to LWJGL bridge
│   └── polyfills.js             # Browser API polyfills
│
scripts/                         # Client application scripts
└── main.js                      # Application entry point

assets/                          # Client application assets
├── models/                      # 3D models (GLTF, FBX, OBJ, etc.)
└── textures/                    # Textures and HDR environment maps
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
   - Initialize LWJGL OpenGL context with AWT windowing
   - Load WebGL2 bridge and browser polyfills
   - Execute `scripts/main.js` with Three.js support
   - Load and render 3D models with PBR materials and HDR lighting

## License

Apache License 2.0 - See LICENSE file for details.
