// Extensions
globalThis.loadCubeTexture = function(filenames)
{
    if (!(filenames && Array.isArray(filenames) && filenames.length == 6))
        throw new Error("loadCubeTexture requires 6 face paths in order: +X, -X, +Y, -Y, +Z, -Z");
    const cubeTexture = new THREE.CubeTexture(filenames.map(f => loadTexture(f)));
    cubeTexture.needsUpdate = true;
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