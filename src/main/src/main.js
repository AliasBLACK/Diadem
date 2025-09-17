// Three.js minimal example implementation
var scene = new THREE.Scene();
var camera = new THREE.PerspectiveCamera(
    75, // Field of View
    800 / 600, // aspect ratio (window.innerWidth / window.innerHeight)
    0.1, // near clipping plane
    1000 // far clipping plane
);
var renderer = new THREE.WebGLRenderer({
    alpha: true, // transparent background
    antialias: true, // smooth edges
    context: gl // Use our WebGL2 context
});
renderer.setSize(800, 600); // window.innerWidth, window.innerHeight

var geometry = new THREE.BoxGeometry(1, 1, 1);
var material = new THREE.MeshNormalMaterial();
var cube = new THREE.Mesh(geometry, material);
scene.add(cube);

camera.position.z = 3; // move camera back so we can see the cube

// Implement the render function from minimal example
var frameCount = 0;
var render = function() {
    frameCount++;

    // Animate cube rotation
    cube.rotation.x += 0.05;
    cube.rotation.y += 0.05;
    
    // Use Three.js WebGLRenderer to render the scene
    renderer.render(scene, camera);
};

requestAnimationFrame(render);