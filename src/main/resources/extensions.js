globalThis.loadCubeTexture = function(filenames)
{
    if (!(filenames && Array.isArray(filenames) && filenames.length == 6))
        throw new Error("loadCubeTexture requires 6 face paths in order: +X, -X, +Y, -Y, +Z, -Z");
    const cubeTexture = new THREE.CubeTexture(filenames.map(f => loadTexture(f)));
    cubeTexture.needsUpdate = true;
    return cubeTexture;
}