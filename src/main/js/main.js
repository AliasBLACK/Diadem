import * as THREE from 'three';

// Test that imports work

// Full Three.js API available
const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(75, 800/600, 0.1, 1000);
const renderer = new THREE.WebGLRenderer({ context: gl });

// Set clear color to light gray so we can see dark objects
renderer.setClearColor(0xcccccc, 1.0); // Light gray background

// Add proper lighting for textured models
const ambientLight = new THREE.AmbientLight(0x404040, 0.4);
scene.add(ambientLight);

const directionalLight = new THREE.DirectionalLight(0xffffff, 1.0);
directionalLight.position.set(1, 1, 1);
scene.add(directionalLight);

// Add a second light from a different angle
const directionalLight2 = new THREE.DirectionalLight(0xffffff, 0.5);
directionalLight2.position.set(-1, 0.5, -1);
scene.add(directionalLight2);

// Load the duck model using our custom GLTF loader

try {
    // Use our custom GLTF loader
    const gltf = loadGLTF('src/main/assets/duck.gltf');
    
    // Scale the duck way down - it must be huge
    gltf.scene.scale.set(0.01, 0.01, 0.01);
    
    // Center the duck at origin
    gltf.scene.position.set(0, 0, 0);
    
    // Add the duck to the scene (use gltf.scene like GLTFLoader)
    scene.add(gltf.scene);
    
    // Compute bounding boxes for proper rendering
    gltf.scene.traverse(function(child) {
        if (child.isMesh) {
            child.geometry.computeBoundingBox();
        }
    });
    
    // Position camera back to normal distance
    camera.position.set(0, 0, 5);
    camera.lookAt(0, 0, 0);
} catch (error) {
    console.error('Error loading duck model with Assimp:', error);
    
    // Fallback to regular GLTFLoader
    const loader = new GLTFLoader();
    loader.load('../assets/duck.gltf', function(gltf) {
        scene.add(gltf.scene);
        camera.position.set(0, 0, 3);
        camera.lookAt(0, 0, 0);
    }, undefined, function(error) {
        console.error('Error loading duck model:', error);
    });
}

camera.position.z = 3;

// Animation loop using requestAnimationFrame
function animate() {
    // Rotate the scene slowly to see the duck
    scene.rotation.y += 0.005;
    
    renderer.render(scene, camera);
}

requestAnimationFrame(animate);