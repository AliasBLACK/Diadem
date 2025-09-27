// Extensions
globalThis.loadCubeTexture = function(filenames)
{
    // Validate arguments
    if (!(filenames && Array.isArray(filenames) && filenames.length == 6))
        throw new Error("loadCubeTexture requires 6 face paths in order: +X, -X, +Y, -Y, +Z, -Z");
    
    // Load faces
    const faces = filenames.map(path => loadTexture(path));

    // Validate that all faces were loaded successfully
    for (let i = 0; i < faces.length; i++) {
        if (!faces[i]) {
            const p = filenames[i];
            throw new Error(`loadCubeTexture: failed to load face[${i}] => ${p}`);
        }
    }

    // Create cube texture
    const cubeTexture = new THREE.CubeTexture(faces);
    
    // Set HDR-friendly parameters if any face is HDR
    const hasHDR = faces.some(t => t && t.type === THREE.HalfFloatType);
    if (hasHDR) {
        cubeTexture.type = THREE.HalfFloatType;
        cubeTexture.format = THREE.RGBFormat;
        cubeTexture.colorSpace = THREE.LinearSRGBColorSpace;
        cubeTexture.minFilter = THREE.LinearFilter;
        cubeTexture.magFilter = THREE.LinearFilter;
        cubeTexture.generateMipmaps = false;
    }

    // Mark as needsUpdate
    cubeTexture.needsUpdate = true;

    // Return cube texture
    return cubeTexture;
}

// Entity management
globalThis.activeEntities = {}
globalThis.inactiveEntities = {}
globalThis.Entity = class {

    constructor(...args) {
        this.id = null;
        inactiveEntities[this.constructor] = inactiveEntities[this.constructor] || [];
        inactiveEntities[this.constructor].push(this);
        this.init(...args);
    }
    
    init(...args)
    {
        this.Start(...args);
        activeEntities[this.constructor] = activeEntities[this.constructor] || [];
        activeEntities[this.constructor].push(this);
        inactiveEntities[this.constructor].splice(inactiveEntities[this.constructor].indexOf(this), 1);
        this.id = requestUpdate(this.Update.bind(this));
    }

    stop()
    {
        this.shutdown();
        inactiveEntities[this.constructor].push(this);
        activeEntities[this.constructor].splice(activeEntities[this.constructor].indexOf(this), 1);
        cancelUpdate(this.id);
        this.id = null;
    }

    // For overriding
    Update(delta) {}
    Start(...args) {}
    shutdown() {}
}

// Entity factory
globalThis.CreateEntity = function(entityClass, ...args) {
    if (entityClass in inactiveEntities && inactiveEntities[entityClass].length > 0)
    {
        let entity = inactiveEntities[entityClass].pop();
        entity.start(...args);
        return entity;
    }
    return new entityClass(...args);
}