// Essential browser APIs for Three.js with LWJGL Assimp GLTF Loader
globalThis.window = globalThis;
globalThis.self = globalThis;
globalThis.navigator = {
    userAgent: 'GraalVM',
    platform: 'Java'
};

// Fake document object
globalThis.document = {
    createElement: function(tagName) {
        if (tagName === 'canvas') {
            return canvas;
        }
        return {
            tagName: tagName.toUpperCase(),
            style: {},
            setAttribute: function() {},
            getAttribute: function() { return null; },
            appendChild: function() {},
            removeChild: function() {}
        };
    },
    createElementNS: function(namespace, tagName) {
        return this.createElement(tagName);
    },
    body: {
        appendChild: function() {},
        removeChild: function() {}
    }
};

// Fake canvas and WebGL context
globalThis.canvas = new (class HTMLCanvasElement {
    constructor() {
        this.width = 800;
        this.height = 600;
        this.style = {};
        this.eventListeners = {};
    }
    getContext(type) {
        if (type === 'webgl2' || type === 'webgl') {
            return gl; // Return our WebGL2 context
        }
        return null;
    }
    setAttribute() {}
    getAttribute() { return null; }
    addEventListener(event, callback, options) {
        if (!this.eventListeners[event]) {
            this.eventListeners[event] = [];
        }
        this.eventListeners[event].push(callback);
    }
    removeEventListener(event, callback) {
        if (this.eventListeners[event]) {
            const index = this.eventListeners[event].indexOf(callback);
            if (index > -1) {
                this.eventListeners[event].splice(index, 1);
            }
        }
    }
    dispatchEvent(event) {
        if (this.eventListeners[event.type]) {
            this.eventListeners[event.type].forEach(callback => callback(event));
        }
    }
})();

// AbortSignal class
globalThis.AbortSignal = class AbortSignal {};
globalThis.AbortController = class AbortController {};

// Animation frame handling for Three.js
var currentAnimationFrameId = 0;
const animationCallbacks = {};
globalThis.requestAnimationFrame = (callback) => {
    animationCallbacks[currentAnimationFrameId] = callback;
    return currentAnimationFrameId++;
};
globalThis.cancelAnimationFrame = (id) => {
    delete animationCallbacks[id];
}

// Update callbacks
var currentUpdateId = 0;
const updateCallbacks = {}
globalThis.requestUpdate = (callback) => {
    updateCallbacks[currentUpdateId] = callback;
    return currentUpdateId++;
}
globalThis.cancelUpdate = (id) => {
    delete updateCallbacks[id];
}

// Run update callbacks at maximum FPS, run animation callbacks at 60 FPS.
var timeLastFrame = Date.now();
var frameProgress = 0;
globalThis.runCallbacks = () => {
    let currentTime = Date.now();
    let delta = currentTime - timeLastFrame;
    timeLastFrame = currentTime;
    frameProgress += delta;
    Object.values(updateCallbacks).forEach(callback => callback(delta / 1000));
    if (frameProgress >= 16) {
        frameProgress = 0;
        Object.values(animationCallbacks).forEach(callback => callback());
    }
}
