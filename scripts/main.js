export default class Main extends Entity {

	Start () {
		this.scene = new THREE.Scene();
		this.camera = new THREE.PerspectiveCamera(60, 1920/1080, 0.1, 1000);
		this.renderer = new THREE.WebGLRenderer({ context: gl });
		this.renderer.outputColorSpace = THREE.SRGBColorSpace;
		this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
		this.renderer.toneMappingExposure = 1.0;
		this.renderer.physicallyCorrectLights = true;

		const hdrCube = loadCubeTexture([
			'textures/cube/pisaHDR/px.hdr',
			'textures/cube/pisaHDR/nx.hdr',
			'textures/cube/pisaHDR/py.hdr',
			'textures/cube/pisaHDR/ny.hdr',
			'textures/cube/pisaHDR/pz.hdr',
			'textures/cube/pisaHDR/nz.hdr',
		]);
		this.scene.background = hdrCube;

		const pmremGenerator = new THREE.PMREMGenerator(this.renderer);
		pmremGenerator.compileEquirectangularShader();
		const envMap = pmremGenerator.fromCubemap(hdrCube);
		this.scene.environment = envMap.texture;
		pmremGenerator.dispose();

		const gltf = loadGLTF('assets/models/mixamo/untitled.glb');
		if (gltf && gltf.scene) {
			this.model = gltf.scene;
			this.model.position.set(0, -1, 0);
			this.model.rotation.set(0, 0, 0);
			this.model.scale.set(1, 1, 1);
			this.scene.add(this.model);

			this.mixer = new THREE.AnimationMixer(this.model);
			const action = this.mixer.clipAction(gltf.animations[0]);
			action.play();

			this.skeletonHelper = new THREE.SkeletonHelper(this.model);
			this.skeletonHelper.visible = true;
			this.scene.add(this.skeletonHelper);
		} else {
			throw new Error('Model returned no scene');
		}

		this.camera.position.set(0, 0, 1);
	}
	
	Update (delta) {
		// if (this.model) {
		// 	this.model.rotation.x += delta * 0.1;
		// }
		if (this.mixer) {
			this.mixer.update(delta);
		}
		this.renderer.render(this.scene, this.camera);
	}
}