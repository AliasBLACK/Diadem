// Diadem Game Engine - Default Entry Point
// This is the default main.js

// Create a simple Three.js scene
const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(75, 800/600, 0.1, 1000);
const renderer = new THREE.WebGLRenderer();

// Set clear color to light gray so we can see dark objects
renderer.setClearColor(0xcccccc, 1.0); // Light gray background
renderer.setSize(800, 600);

// Add proper lighting for materials
const ambientLight = new THREE.AmbientLight(0x404040, 0.4);
scene.add(ambientLight);

const directionalLight = new THREE.DirectionalLight(0xffffff, 1.0);
directionalLight.position.set(1, 1, 1);
scene.add(directionalLight);

// Add a second light from a different angle
const directionalLight2 = new THREE.DirectionalLight(0xffffff, 0.5);
directionalLight2.position.set(-1, 0.5, -1);
scene.add(directionalLight2);

// Add a simple test cube to verify rendering
const geometry = new THREE.BoxGeometry(1, 1, 1);
const material = new THREE.MeshLambertMaterial({ color: 0x00ff00 });
const cube = new THREE.Mesh(geometry, material);
scene.add(cube);

camera.position.z = 3;

// Animation loop
function animate() {
    cube.rotation.x += 0.01;
    cube.rotation.y += 0.01;
    
    renderer.render(scene, camera);
}
requestAnimationFrame(animate);
