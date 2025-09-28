// Robust WeakMap shim installed early (force override)
// Three.js PMREM uses WeakMap heavily; GraalVM bridges may pass non-plain objects.
// This shim tolerates primitives/null by boxing to hidden objects.
globalThis.WeakMap = class WeakMap {
	constructor() {
		this._k = '__wm_' + Math.random().toString(36).slice(2);
		this._fk = []; // fallback keys
		this._fv = []; // fallback values
		this._boxes = Object.create(null);
		this._null = { __wm_null__: true };
	}
	_box(k) {
		if (k === null || k === undefined) return this._null;
		const t = typeof k;
		if (t === 'object' || t === 'function') return k;
		const id = t + ':' + String(k);
		return this._boxes[id] || (this._boxes[id] = { __boxed__: true, t, v: k });
	}
	set(k, v) {
		k = this._box(k);
		try {
			Object.defineProperty(k, this._k, { value: v, writable: true, enumerable: false, configurable: true });
			return this;
		} catch (_) {}
		const i = this._fk.indexOf(k);
		if (i >= 0) this._fv[i] = v; else { this._fk.push(k); this._fv.push(v); }
		return this;
	}
	get(k) {
		k = this._box(k);
		if (this._k in k) return k[this._k];
		const i = this._fk.indexOf(k);
		return i >= 0 ? this._fv[i] : undefined;
	}
	has(k) {
		k = this._box(k);
		return (this._k in k) || this._fk.indexOf(k) >= 0;
	}
	delete(k) {
		k = this._box(k);
		let d = false;
		if (this._k in k) { delete k[this._k]; d = true; }
		const i = this._fk.indexOf(k);
		if (i >= 0) { this._fk.splice(i,1); this._fv.splice(i,1); d = true; }
		return d;
	}
};

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
globalThis.VideoFrame = class VideoFrame {};

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
