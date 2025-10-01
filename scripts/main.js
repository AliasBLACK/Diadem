export default class Main extends Entity {

	// Called at the start of the Entity lifecycle.
	Start () {
		this.scene = new THREE.Scene();
		this.camera = new THREE.PerspectiveCamera(60, 1920/1080, 0.1, 1000);
		this.renderer = new THREE.WebGLRenderer({ context: gl });
		this.renderer.outputColorSpace = THREE.SRGBColorSpace;
		this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
		this.renderer.toneMappingExposure = 1.0;
		this.renderer.physicallyCorrectLights = true;

		// Environment
		const hdrCube = loadCubeTexture([
			'textures/cube/pisaHDR/px.hdr',
			'textures/cube/pisaHDR/nx.hdr',
			'textures/cube/pisaHDR/py.hdr',
			'textures/cube/pisaHDR/ny.hdr',
			'textures/cube/pisaHDR/pz.hdr',
			'textures/cube/pisaHDR/nz.hdr',
		]);
		this.scene.background = hdrCube;

		// Environment mapping
		const pmremGenerator = new THREE.PMREMGenerator(this.renderer);
		pmremGenerator.compileEquirectangularShader();
		const envMap = pmremGenerator.fromCubemap(hdrCube);
		this.scene.environment = envMap.texture;
		pmremGenerator.dispose();

		// Model
		// const gltf = loadModel('models/damagedHelmet/DamagedHelmet.gltf')
		const gltf = loadModel('assets/models/mixamo/SambaDancing.fbx');
		if (gltf && gltf.scene) {
			this.model = gltf.scene;
			this.scene.add(this.model);
			this.model.position.set(0, 0, 0);
			this.model.rotation.set(0, 0, 0);
			this.model.scale.set(1, 1, 1);

			// Animation
			this.mixer = new THREE.AnimationMixer(this.model);
			this.action = this.mixer.clipAction(gltf.animations[0]);
			this.action.play();

			// Optional: Add SkeletonHelper to visualize bone animation
			const helper = new THREE.SkeletonHelper(this.model);
			helper.visible = true;
			this.scene.add(helper);
		} else {
			throw new Error('Model returned no scene');
		}

		// Camera
		this.camera.position.set(0, 0, 500);
	}
	
	Update (delta) {
		if (this.mixer) {
			this.mixer.update(delta);
		}
		this.renderer.render(this.scene, this.camera);
	}
}