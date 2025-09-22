/*
 Quickstart: Main Entity
 - This class is your app/game root. It runs automatically as an Entity.
 - Three.js is available as `THREE`; the WebGL context is `gl`.
 - Add scene setup in the constructor. Animate/render in `update(delta)`.
 - Relative imports (e.g. `./test.js`) work in dev and packaged builds.
*/
export default class Main extends Entity {

    // Called at the start of the Entity lifecycle.
    Start () {

        // Full Three.js API available
        this.scene = new THREE.Scene();
        this.camera = new THREE.PerspectiveCamera(75, 1920/1080, 0.1, 1000);
        this.renderer = new THREE.WebGLRenderer({ context: gl });

        // Create your 3D objects
        const geometry = new THREE.BoxGeometry();
        const material = new THREE.MeshNormalMaterial();
        this.cube = new THREE.Mesh(geometry, material);
        this.scene.add(this.cube);

        // Position camera
        this.camera.position.z = 3;
    }
    
    // Called every frame, with `delta` time since last frame.
    Update (delta) {

        // Rotate cube
        this.cube.rotation.x += 0.6 * delta;
        this.cube.rotation.y += 0.6 * delta;

        // Render scene
        this.renderer.render(this.scene, this.camera);
    }
}