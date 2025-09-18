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
const animationCallbacks = [];
globalThis.requestAnimationFrame = (callback) => {
    let id = animationCallbacks.indexOf(callback);
    if (id >= 0)
        return id;
    animationCallbacks.push(callback);
    return animationCallbacks.length - 1;   
};
globalThis.cancelAnimationFrame = (id) => {
    animationCallbacks.splice(id, 1);
}
globalThis.runCallbacks = () => {
    animationCallbacks.forEach(callback => callback());
}
