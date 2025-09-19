# Assets Directory

This directory contains game assets that will be bundled as resources in the compiled application.

## Structure
- `textures/` - Texture files (.png, .jpg, etc.)
- `models/` - 3D model files (.gltf, .glb, etc.)
- `sounds/` - Audio files (.wav, .ogg, etc.)
- `fonts/` - Font files (.ttf, .otf, etc.)

## Usage
Assets in this directory are automatically copied to the JAR resources during compilation and can be accessed via:
- Java: `getClass().getResourceAsStream("/assets/filename")`
- JavaScript: Through the asset loading system
