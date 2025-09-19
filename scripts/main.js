// Full Three.js API available
const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(75, 1024/576, 0.1, 1000);
const renderer = new THREE.WebGLRenderer({ context: gl });

// Create your 3D objects
const geometry = new THREE.BoxGeometry();
const material = new THREE.MeshNormalMaterial();
const cube = new THREE.Mesh(geometry, material);
scene.add(cube);

camera.position.z = 3;

// Animation loop using requestAnimationFrame
function animate() {
    cube.rotation.x += 0.01;
    cube.rotation.y += 0.01;
    
    renderer.render(scene, camera);
}

requestAnimationFrame(animate);