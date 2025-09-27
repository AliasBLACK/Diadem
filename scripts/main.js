export default class Main extends Entity {

    // Called at the start of the Entity lifecycle.
    Start () {
        try {
            this.scene = new THREE.Scene();
            this.camera = new THREE.PerspectiveCamera(60, 1920/1080, 0.1, 1000);
            this.renderer = new THREE.WebGLRenderer({ context: gl });
            this.renderer.outputColorSpace = THREE.SRGBColorSpace;
            this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
            this.renderer.toneMappingExposure = 1.0;
            this.renderer.physicallyCorrectLights = true;

            // Environment
            try {
                const hdrFaces = [
                    'textures/cube/pisaHDR/px.hdr',
                    'textures/cube/pisaHDR/nx.hdr',
                    'textures/cube/pisaHDR/py.hdr',
                    'textures/cube/pisaHDR/ny.hdr',
                    'textures/cube/pisaHDR/pz.hdr',
                    'textures/cube/pisaHDR/nz.hdr',
                ];
                const hdrCube = loadCubeTexture(hdrFaces);
                this.scene.background = hdrCube;
                // this.scene.environment = hdrCube;
            } catch (e) {
                console.error('Failed to load cube textures:', e);
                this.scene.background = new THREE.Color(0x222222);
                this.scene.environment = null;
            }

            // Lighting
            const hemi = new THREE.HemisphereLight(0xbfd8ff, 0xe0c199, 0.55);
            this.scene.add(hemi);
            const sun = new THREE.DirectionalLight(0xffe4c4, 1.1);
            sun.position.set(4, 8, 2);
            sun.castShadow = false;
            this.scene.add(sun);

            // Model
            try {
                const gltf = loadGLTF('assets/models/damagedHelmet/DamagedHelmet.gltf');
                if (gltf && gltf.scene) {
                    this.model = gltf.scene;
                    this.scene.add(this.model);
                    this.model.position.set(0, 0, 0);
                    this.model.rotation.set(Math.PI * .5, 0, 0);
                    this.model.scale.set(.75, .75, .75);
                } else {
                    throw new Error('GLTF returned no scene');
                }
            } catch (e) {
                console.error('Failed to load GLTF:', e);
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

            this.camera.position.set(0, 0, 2.2);
        } catch (fatal) {
            console.error('Fatal error in Main.Start:', fatal);
            try {
                this.scene = this.scene || new THREE.Scene();
                this.camera = this.camera || new THREE.PerspectiveCamera(60, 1920/1080, 0.1, 1000);
                this.renderer = this.renderer || new THREE.WebGLRenderer({ context: gl });
                this.scene.add(new THREE.AxesHelper(1));
                this.camera.position.set(0, 0, 3);
            } catch (ignore) {}
        }
    }
    
    Update (delta) {
        if (this.model) {
            this.model.rotation.z += 0.25 * delta;
        }
        this.renderer.render(this.scene, this.camera);
    }
}