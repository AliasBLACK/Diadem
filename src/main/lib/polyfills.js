// Fake browser APIs for Three.js
globalThis.window = globalThis;
globalThis.self = globalThis;
globalThis.navigator = {
    userAgent: 'GraalVM',
    platform: 'Java'
};

// Set up CommonJS-like environment for three.cjs
globalThis.module = { exports: {} };
globalThis.exports = globalThis.module.exports;

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

// Local file fetch API for GLTFLoader
const FileSystemUtils = Java.type('black.alias.diadem.Utils.FileSystemUtils');

globalThis.fetch = function(input, options = {}) {
    return new Promise((resolve, reject) => {
        try {
            // Extract URL from input (could be string or Request object)
            let url;
            if (typeof input === 'string') {
                url = input;
            } else if (input && typeof input.url === 'string') {
                url = input.url;
            } else if (input && typeof input === 'object' && input.toString) {
                url = input.toString();
            } else {
                url = String(input);
            }
            
            // Only support local files - reject HTTP URLs
            if (url.includes('://')) {
                reject(new Error(`HTTP requests not supported. Only local files are allowed: ${url}`));
                return;
            }
            
            // Handle local file paths relative to main.js
            let relativePath = url;
            if (url.startsWith('./')) {
                relativePath = url.substring(2);
            }
            
            if (FileSystemUtils.fileExists(relativePath)) {
                const bytes = FileSystemUtils.readFileBytes(relativePath);
                const mimeType = FileSystemUtils.getMimeType(relativePath);
                
                resolve(new Response(bytes, {
                    status: 200,
                    statusText: 'OK',
                    headers: {
                        'Content-Type': mimeType,
                        'Content-Length': bytes.length.toString()
                    }
                }));
            } else {
                reject(new Error(`File not found: ${url}`));
            }
        } catch (error) {
            reject(new Error(`Fetch failed: ${error.message}`));
        }
    });
};

// Headers class for fetch API
globalThis.Headers = class Headers {
    constructor(init = {}) {
        this._headers = new Map();
        
        if (init) {
            if (init instanceof Headers) {
                init._headers.forEach((value, key) => {
                    this._headers.set(key.toLowerCase(), value);
                });
            } else if (Array.isArray(init)) {
                init.forEach(([key, value]) => {
                    this._headers.set(key.toLowerCase(), String(value));
                });
            } else if (typeof init === 'object') {
                Object.entries(init).forEach(([key, value]) => {
                    this._headers.set(key.toLowerCase(), String(value));
                });
            }
        }
    }
    
    append(name, value) {
        const existing = this._headers.get(name.toLowerCase());
        if (existing) {
            this._headers.set(name.toLowerCase(), `${existing}, ${value}`);
        } else {
            this._headers.set(name.toLowerCase(), String(value));
        }
    }
    
    delete(name) {
        this._headers.delete(name.toLowerCase());
    }
    
    get(name) {
        return this._headers.get(name.toLowerCase()) || null;
    }
    
    has(name) {
        return this._headers.has(name.toLowerCase());
    }
    
    set(name, value) {
        this._headers.set(name.toLowerCase(), String(value));
    }
    
    entries() {
        return this._headers.entries();
    }
    
    keys() {
        return this._headers.keys();
    }
    
    values() {
        return this._headers.values();
    }
    
    forEach(callback, thisArg) {
        this._headers.forEach(callback, thisArg);
    }
};

// Request class for fetch API
globalThis.Request = class Request {
    constructor(input, init = {}) {
        if (typeof input === 'string') {
            this.url = input;
        } else if (input && typeof input.url === 'string') {
            this.url = input.url;
        } else {
            this.url = String(input);
        }
        
        this.method = init.method || 'GET';
        this.headers = new Headers(init.headers || {});
        this.body = init.body || null;
        this.mode = init.mode || 'cors';
        this.credentials = init.credentials || 'same-origin';
        this.cache = init.cache || 'default';
        this.redirect = init.redirect || 'follow';
        this.referrer = init.referrer || '';
        this.integrity = init.integrity || '';
    }
    
    clone() {
        return new Request(this.url, {
            method: this.method,
            headers: this.headers,
            body: this.body,
            mode: this.mode,
            credentials: this.credentials,
            cache: this.cache,
            redirect: this.redirect,
            referrer: this.referrer,
            integrity: this.integrity
        });
    }
};

// Response class for fetch API
globalThis.Response = class Response {
    constructor(body, options = {}) {
        this.body = body;
        this.status = options.status || 200;
        this.statusText = options.statusText || 'OK';
        this.headers = new Headers(options.headers || {});
        this.ok = this.status >= 200 && this.status < 300;
    }
    
    async arrayBuffer() {
        if (this.body instanceof Java.type('byte[]')) {
            // Convert Java byte array to JavaScript ArrayBuffer
            const buffer = new ArrayBuffer(this.body.length);
            const view = new Uint8Array(buffer);
            for (let i = 0; i < this.body.length; i++) {
                view[i] = this.body[i] & 0xFF; // Ensure unsigned byte
            }
            return buffer;
        }
        return this.body;
    }
    
    async text() {
        if (this.body instanceof Java.type('byte[]')) {
            // Use Java String constructor properly
            const StringClass = Java.type('java.lang.String');
            return new StringClass(this.body, 'UTF-8');
        }
        return this.body.toString();
    }
    
    async json() {
        const text = await this.text();
        return JSON.parse(text);
    }
    
    async blob() {
        const buffer = await this.arrayBuffer();
        return new Blob([buffer], { type: this.headers.get('Content-Type') || 'application/octet-stream' });
    }
};

// Blob class for binary data
globalThis.Blob = class Blob {
    constructor(parts = [], options = {}) {
        this.parts = parts;
        this.type = options.type || '';
        this.size = parts.reduce((total, part) => {
            if (part instanceof ArrayBuffer) return total + part.byteLength;
            if (typeof part === 'string') return total + part.length;
            return total;
        }, 0);
    }
    
    arrayBuffer() {
        return Promise.resolve(this.parts[0]);
    }
    
    text() {
        return Promise.resolve(this.parts[0].toString());
    }
};

// XMLHttpRequest polyfill for Three.js FileLoader
globalThis.XMLHttpRequest = class XMLHttpRequest {
    constructor() {
        this.readyState = 0; // UNSENT
        this.response = null;
        this.responseText = '';
        this.responseType = '';
        this.status = 0;
        this.statusText = '';
        this.onload = null;
        this.onerror = null;
        this.onprogress = null;
        this.onreadystatechange = null;
        this._url = '';
        this._method = '';
        this._headers = {};
    }
    
    open(method, url, async = true) {
        this._method = method;
        this._url = url;
        this.readyState = 1; // OPENED
        if (this.onreadystatechange) this.onreadystatechange();
    }
    
    setRequestHeader(name, value) {
        this._headers[name] = value;
    }
    
    send(data = null) {
        this.readyState = 2; // HEADERS_RECEIVED
        if (this.onreadystatechange) this.onreadystatechange();
        
        // Use our fetch polyfill
        fetch(this._url)
            .then(response => {
                this.status = response.status;
                this.statusText = response.statusText;
                this.readyState = 3; // LOADING
                if (this.onreadystatechange) this.onreadystatechange();
                
                // Handle different response types
                if (this.responseType === 'arraybuffer') {
                    return response.arrayBuffer();
                } else if (this.responseType === 'json') {
                    return response.json();
                } else {
                    return response.text();
                }
            })
            .then(data => {
                this.response = data;
                if (this.responseType === '' || this.responseType === 'text') {
                    this.responseText = typeof data === 'string' ? data : JSON.stringify(data);
                }
                this.readyState = 4; // DONE
                
                if (this.onreadystatechange) this.onreadystatechange();
                if (this.onload) this.onload();
            })
            .catch(error => {
                this.status = 404;
                this.statusText = 'Not Found';
                this.readyState = 4; // DONE
                
                if (this.onreadystatechange) this.onreadystatechange();
                if (this.onerror) this.onerror(error);
            });
    }
    
    abort() {
        this.readyState = 0;
        if (this.onreadystatechange) this.onreadystatechange();
    }
};

// AbortSignal class
globalThis.AbortSignal = class AbortSignal {
    constructor() {
        this.aborted = false;
        this.reason = undefined;
        this._listeners = [];
    }
    
    addEventListener(type, listener) {
        if (type === 'abort') {
            this._listeners.push(listener);
        }
    }
    
    removeEventListener(type, listener) {
        if (type === 'abort') {
            const index = this._listeners.indexOf(listener);
            if (index > -1) {
                this._listeners.splice(index, 1);
            }
        }
    }
    
    dispatchEvent(event) {
        if (event.type === 'abort') {
            this._listeners.forEach(listener => {
                try {
                    listener(event);
                } catch (error) {
                    console.error('Error in abort listener:', error);
                }
            });
        }
    }
    
    throwIfAborted() {
        if (this.aborted) {
            throw new Error('AbortError: The operation was aborted');
        }
    }
    
    static abort(reason) {
        const signal = new AbortSignal();
        signal.aborted = true;
        signal.reason = reason;
        return signal;
    }
    
    static timeout(delay) {
        const signal = new AbortSignal();
        setTimeout(() => {
            signal.aborted = true;
            signal.reason = new Error('TimeoutError: The operation timed out');
            signal.dispatchEvent({ type: 'abort' });
        }, delay);
        return signal;
    }
};

globalThis.AbortController = class AbortController {
    constructor() {
        this.signal = new AbortSignal();
    }
    
    abort(reason) {
        if (!this.signal.aborted) {
            this.signal.aborted = true;
            this.signal.reason = reason;
            this.signal.dispatchEvent({ type: 'abort' });
        }
    }
};

globalThis.FileReader = class FileReader {
    constructor() {
        this.result = null;
        this.error = null;
        this.readyState = 0; // EMPTY
        this.onload = null;
        this.onerror = null;
        this.onloadend = null;
    }
    
    readAsDataURL(blob) {
        setTimeout(() => {
            try {
                this.readyState = 2; // DONE
                // For GLTF, we typically don't need data URLs, but provide basic support
                this.result = `data:${blob.type};base64,${this._arrayBufferToBase64(blob.parts[0])}`;
                if (this.onload) this.onload({ target: this });
                if (this.onloadend) this.onloadend({ target: this });
            } catch (error) {
                this.error = error;
                if (this.onerror) this.onerror({ target: this });
                if (this.onloadend) this.onloadend({ target: this });
            }
        }, 0);
    }
    
    readAsArrayBuffer(blob) {
        setTimeout(() => {
            try {
                this.readyState = 2; // DONE
                this.result = blob.parts[0];
                if (this.onload) this.onload({ target: this });
                if (this.onloadend) this.onloadend({ target: this });
            } catch (error) {
                this.error = error;
                if (this.onerror) this.onerror({ target: this });
                if (this.onloadend) this.onloadend({ target: this });
            }
        }, 0);
    }
    
    _arrayBufferToBase64(buffer) {
        const bytes = new Uint8Array(buffer);
        let binary = '';
        for (let i = 0; i < bytes.byteLength; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return Java.type('java.util.Base64').getEncoder().encodeToString(Java.to(binary.split('').map(c => c.charCodeAt(0)), 'byte[]'));
    }
};

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

console.log('Browser API polyfills loaded');