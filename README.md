# Diadem

<div align="center">
  <img width="100%" alt="Screenshot 2025-10-01 111510" src="https://github.com/user-attachments/assets/7d3a17e4-31d1-4dda-818c-30292f8e4f86" />
</div>

<br>

A minimal desktop **[Three.js](https://github.com/mrdoob/three.js)** implementation powered by **[LWJGL](https://github.com/LWJGL/lwjgl3)** and **[GraalJS](https://github.com/oracle/graaljs)**, providing native WebGL2 rendering without bulky webviews. Features custom texture loading and HDR environment map support for PBR workflows. Perfect for desktop applications that need 3D graphics with JavaScript flexibility, including potential integrations with native libraries like Steam and Twitch APIs.

## Philosophy

This project is a proof of concept for running Three.js applications natively on desktop without the overhead of browser engines or webviews. By bridging JavaScript WebGL2 calls directly to native OpenGL through LWJGL, we achieve:

- **Lightweight**: No Chromium/WebView dependencies
- **Cross Platform**: Runs on Windows, Mac and Linux
- **Polyglot Ready**: Write in any language supported by [Polyglot](https://www.graalvm.org/latest/reference-manual/polyglot-programming/).
- **Desktop Focused**: Easy access to native APIs (Steam, Twitch, file system)

## Requirements

- GraalVM 25.0.0 (Community or Enterprise Edition)
- Maven 3.6+
  - LWJGL 3.3.6 (OpenGL)
  - GraalJS 25.0.0 (Community or Enterprise Edition)
- Windows, Mac or Linux (only tested on Windows)

## Quickstart

Write your Three.js applications in `scripts/main.js`:

```javascript
// Full Three.js API available
const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(75, 800/600, 0.1, 1000);
const renderer = new THREE.WebGLRenderer({ context: gl });

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

// Load model
const model = loadGLTF('models/damagedHelmet/DamagedHelmet.glb');
scene.add(model.scene)

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
│   └── Loaders/                 # Texture loaders and helpers
├── resources/
│   ├── three.js                 # Three.js core build
│   ├── renderer.js              # WebGL2 to LWJGL bridge
│   └── polyfills.js             # Browser API polyfills
│
scripts/                         # Client application scripts
├── main.js                      # Application entry point
└── settings.json                # Application settings

assets/                          # Client application assets
├── models/                      # 3D models (GLTF, FBX, OBJ, etc.)
└── textures/                    # Textures and HDR environment maps
```

## Building and Running

1. **Build and run the project:**
   ```bash
   mvn clean exec:java
   ```

   The application will:
   - Initialize LWJGL OpenGL context with AWT windowing
   - Load WebGL2 bridge and browser polyfills
   - Execute `scripts/main.js` with Three.js support
   - Load and render 3D scenes with PBR materials and HDR lighting

## Packaging for Release

1. **Build the project:**
   ```bash
   mvn clean verify
   ```

   The application will be packaged into a minimal executable for the target platform by [JPackage](https://docs.oracle.com/en/java/javase/17/docs/specs/man/jpackage.html).

## License

Apache License 2.0 - See LICENSE file for details.
