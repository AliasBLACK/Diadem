/*
Quickstart: Main Entity
- This class is your app/game root. It runs automatically as an Entity.
- Three.js is available as `THREE`; the WebGL context is `gl`.
- Add scene setup in the constructor. Animate/render in `update(delta)`.
*/
export default class Main extends Entity {

    // Called at the start of the Entity lifecycle.
    Start () {
        try {
            // Full Three.js API available
            this.scene = new THREE.Scene();
            this.camera = new THREE.PerspectiveCamera(60, 1920/1080, 0.1, 1000);
            this.renderer = new THREE.WebGLRenderer({ context: gl });

            // Setup environment: LDR background and HDR environment map
            try {
                const ldrFaces = [
                    'textures/cube/pisa/px.png',
                    'textures/cube/pisa/nx.png',
                    'textures/cube/pisa/py.png',
                    'textures/cube/pisa/ny.png',
                    'textures/cube/pisa/pz.png',
                    'textures/cube/pisa/nz.png',
                ];
                const hdrFaces = [
                    'textures/cube/pisaHDR/px.hdr',
                    'textures/cube/pisaHDR/nx.hdr',
                    'textures/cube/pisaHDR/py.hdr',
                    'textures/cube/pisaHDR/ny.hdr',
                    'textures/cube/pisaHDR/pz.hdr',
                    'textures/cube/pisaHDR/nz.hdr',
                ];

                // loadCubeTexture auto-detects HDR via .hdr and configures HalfFloatType
                const ldrCube = loadCubeTexture(ldrFaces);
                // const hdrCube = loadCubeTexture(hdrFaces);
                // Temporary: PMREM (HDR environment processing) causes "Invalid value used as weak map key" 
                // in GraalVM due to WeakMap/render target limitations in our WebGL bridge.
                // Use LDR for both background and environment until PMREM is fully supported.
                const envCube = ldrCube; // TODO: Switch back to hdrCube when PMREM works

                // Use LDR for a nicer background, disable environment to avoid PMREM
                this.scene.background = ldrCube;
                // this.scene.environment = envCube; // Disabled: ANY environment triggers PMREM WeakMap errors
            } catch (e) {
                console.error('Failed to load cube textures:', e);
                this.scene.background = new THREE.Color(0x222222);
                this.scene.environment = null;
            }

            // Lighting for PBR
            const ambient = new THREE.AmbientLight(0xffffff, 0.3);
            this.scene.add(ambient);
            const dirLight = new THREE.DirectionalLight(0xffffff, 1.0);
            dirLight.position.set(5, 10, 5);
            this.scene.add(dirLight);

            // Load GLTF model (DamagedHelmet) - use filesystem path under assets
            try {
                const gltf = loadGLTF('assets/models/damagedHelmet/DamagedHelmet.gltf');
                if (gltf && gltf.scene) {
                    this.model = gltf.scene;
                    this.scene.add(this.model);
                    // Optional: adjust transform for presentation
                    this.model.position.set(0, 0, 0);
                    this.model.rotation.set(Math.PI * .5, 0, 0);
                    this.model.scale.set(.75, .75, .75);
                } else {
                    throw new Error('GLTF returned no scene');
                }
            } catch (e) {
                console.error('Failed to load GLTF:', e);
                // Fallback: simple PBR sphere to keep demo running
                const geo = new THREE.SphereGeometry(0.6, 48, 32);
                const mat = new THREE.MeshPhysicalMaterial({
                    color: 0x888888,
                    metalness: 0.5,
                    roughness: 0.2,
                    clearcoat: 0.6,
                    envMap: this.scene.environment
                });
                this.model = new THREE.Mesh(geo, mat);
                this.scene.add(this.model);
            }

            // Camera position
            this.camera.position.set(0, 0, 2.2);
        } catch (fatal) {
            // Ensure constructor never throws so module instantiation succeeds
            console.error('Fatal error in Main.Start:', fatal);
            try {
                // Create a minimal scene to keep app running
                this.scene = this.scene || new THREE.Scene();
                this.camera = this.camera || new THREE.PerspectiveCamera(60, 1920/1080, 0.1, 1000);
                this.renderer = this.renderer || new THREE.WebGLRenderer({ context: gl });
                this.scene.add(new THREE.AxesHelper(1));
                this.camera.position.set(0, 0, 3);
            } catch (ignore) {}
        }
    }
    
    // Called every frame, with `delta` time since last frame.
    Update (delta) {
        // Gentle turntable for the model if present
        if (this.model) {
            this.model.rotation.z += 0.25 * delta;
        }

        // Render scene
        this.renderer.render(this.scene, this.camera);
    }
}