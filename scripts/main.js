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

        // Load the TTF and build a Font class (shared for UI text)
        this.uiFont = new FontSDF('Montserrat-Regular.ttf', 100);
        this.uiFont.isEastAsian = false;
        this.uiFont.setUseAlphaSDF(false);
        this.uiFont.setUvInsetTexels(0.25); // try 0.125–0.5
        // console.log('SDF atlas size:', this.uiFont.atlasWidth, 'x', this.uiFont.atlasHeight);

        // --- Performance test: swap sizable text (100 words) every second ---
        // this.testTextParent = new THREE.Group();
        // GUI.SCENE.add(this.testTextParent);
        // this._loremWords = (
        //     'ABC'
        // ).split(' ');
        // this._swapInterval = 1.0; // seconds
        // this._swapAccum = 0.0;
        // this._currentTextMesh = null;
        // this._swapSizableText(); // create initial

        // // Create your 3D objects
        const geometry = new THREE.BoxGeometry();
        const material = new THREE.MeshNormalMaterial();
        this.cube = new THREE.Mesh(geometry, material);
        this.scene.add(this.cube);

        // // Position camera
        // this.camera.position.z = 3;

        // // --- FPS Counter (top-right-ish) ---
        // this._fpsFrames = 0;
        // this._fpsTime = 0;
        // this._fps = 0;
        // this._fpsMesh = this.uiFont.generateMesh('FPS: --', 0x00ff88, new THREE.Vector3(800, 420, 0));
        // GUI.SCENE.add(this._fpsMesh);

        const singleA = this.uiFont.generateMesh('ABC', 0xffffff, new THREE.Vector3(0, 0, 0));
        if (singleA) GUI.SCENE.add(singleA);
    }
    
    // Called every frame, with `delta` time since last frame.
    Update (delta) {

        // Rotate cube
        this.cube.rotation.x += 0.6 * delta;
        this.cube.rotation.y += 0.6 * delta;

        // Render.
        Render(this.scene, this.camera);

        // --- Update FPS counter and swap test text ---
        // this._fpsFrames++;
        // this._fpsTime += delta;
        // if (this._fpsTime >= 0.5) {
        //     this._fps = Math.round(this._fpsFrames / this._fpsTime);
        //     this._fpsFrames = 0;
        //     this._fpsTime = 0;
        //     // replace fps mesh text
        //     GUI.SCENE.remove(this._fpsMesh);
        //     if (this._fpsMesh.geometry) this._fpsMesh.geometry.dispose();
        //     if (this._fpsMesh.material) this._fpsMesh.material.dispose();
        //     this._fpsMesh = this.uiFont.generateMesh(`FPS: ${this._fps}`, 0x00ff88, new THREE.Vector3(800, 420, 0));
        //     GUI.SCENE.add(this._fpsMesh);
        // }

        // this._swapAccum += delta;
        // if (this._swapAccum >= this._swapInterval) {
        //     this._swapAccum = 0.0;
        //     this._swapSizableText();
        // }
    }

    _swapSizableText() {
        // Remove previous
        if (this._currentTextMesh) {
            this.testTextParent.remove(this._currentTextMesh);
            if (this._currentTextMesh.geometry) this._currentTextMesh.geometry.dispose();
            if (this._currentTextMesh.material) this._currentTextMesh.material.dispose();
        }
        // Build ~100-word random text
        const count = 100;
        let words = [];
        for (let i = 0; i < count; i++) {
            words.push(this._loremWords[Math.floor(Math.random() * this._loremWords.length)]);
        }
        const text = words.join(' ');
        // Generate new text mesh with wrapping width (pixels-ish in our world units)
        // Adjust width as needed for your GUI scaling
        const width = 700;
        this._currentTextMesh = this.uiFont.generateMesh(text, new THREE.Color("purple"), new THREE.Vector3(0, 200, 0), width);
        this.testTextParent.add(this._currentTextMesh);
    }
}