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

        // Create your 3D objects
        const geometry = new THREE.BoxGeometry();
        const material = new THREE.MeshNormalMaterial();
        this.cube = new THREE.Mesh(geometry, material);
        this.scene.add(this.cube);

        // Position camera
        this.camera.position.z = 3;
        
        // Create example UI
        let Container = new GUI.Panel()
            .setWidthPercent(100)
            .setHeightPercent(100)
            .setPaddingPercent(GUI.EDGE_ALL, .5)
            .setFlexDirection(GUI.FLEX_DIRECTION_ROW)
            .setJustifyContent(GUI.JUSTIFY_FLEX_START)
            .setAlignItems(GUI.ALIGN_FLEX_START)
            .setChildOf(GUI.ROOT_NODE);
        
        let UIPanel1 = new GUI.Panel()
            .setWidthPercent(30)
            .setHeightPercent(10)
            .setRenderMode(GUI.RENDER_MODE_COLOR_RECT)
            .setMarginPercent(GUI.EDGE_RIGHT, .5)
            .setTexture("grey")
            .setChildOf(Container);
        
        let UIPanel2 = new GUI.Panel()
            .setWidthPercent(10)
            .setHeightPercent(10)
            .setRenderMode(GUI.RENDER_MODE_COLOR_RECT)
            .setTexture("grey")
            .setChildOf(Container);
    }
    
    // Called every frame, with `delta` time since last frame.
    Update (delta) {

        // Rotate cube
        this.cube.rotation.x += 0.6 * delta;
        this.cube.rotation.y += 0.6 * delta;

        // Render.
        DIADEM.Render(this.scene, this.camera);
    }
}