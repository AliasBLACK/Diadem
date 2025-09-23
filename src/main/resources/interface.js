// Yoga Bindings
const Yoga = Java.type("org.lwjgl.util.yoga.Yoga");
globalThis.GUI = {

    // Edge constants
    EDGE_LEFT: Yoga.YGEdgeLeft,
    EDGE_TOP: Yoga.YGEdgeTop,
    EDGE_RIGHT: Yoga.YGEdgeRight,
    EDGE_BOTTOM: Yoga.YGEdgeBottom,
    EDGE_START: Yoga.YGEdgeStart,
    EDGE_END: Yoga.YGEdgeEnd,
    EDGE_HORIZONTAL: Yoga.YGEdgeHorizontal,
    EDGE_VERTICAL: Yoga.YGEdgeVertical,
    EDGE_ALL: Yoga.YGEdgeAll,
    
    // Justify content constants
    JUSTIFY_FLEX_START: Yoga.YGJustifyFlexStart,
    JUSTIFY_CENTER: Yoga.YGJustifyCenter,
    JUSTIFY_FLEX_END: Yoga.YGJustifyFlexEnd,
    JUSTIFY_SPACE_BETWEEN: Yoga.YGJustifySpaceBetween,
    JUSTIFY_SPACE_AROUND: Yoga.YGJustifySpaceAround,
    JUSTIFY_SPACE_EVENLY: Yoga.YGJustifySpaceEvenly,
    
    // Align constants
    ALIGN_AUTO: Yoga.YGAlignAuto,
    ALIGN_FLEX_START: Yoga.YGAlignFlexStart,
    ALIGN_CENTER: Yoga.YGAlignCenter,
    ALIGN_FLEX_END: Yoga.YGAlignFlexEnd,
    ALIGN_STRETCH: Yoga.YGAlignStretch,
    ALIGN_BASELINE: Yoga.YGAlignBaseline,
    ALIGN_SPACE_BETWEEN: Yoga.YGAlignSpaceBetween,
    ALIGN_SPACE_AROUND: Yoga.YGAlignSpaceAround,
    
    // Flex direction constants
    FLEX_DIRECTION_COLUMN: Yoga.YGFlexDirectionColumn,
    FLEX_DIRECTION_COLUMN_REVERSE: Yoga.YGFlexDirectionColumnReverse,
    FLEX_DIRECTION_ROW: Yoga.YGFlexDirectionRow,
    FLEX_DIRECTION_ROW_REVERSE: Yoga.YGFlexDirectionRowReverse,
    
    // Position type constants
    POSITION_TYPE_STATIC: Yoga.YGPositionTypeStatic,
    POSITION_TYPE_RELATIVE: Yoga.YGPositionTypeRelative,
    POSITION_TYPE_ABSOLUTE: Yoga.YGPositionTypeAbsolute,
    
    // Direction constants
    DIRECTION_LTR: Yoga.YGDirectionLTR,
    DIRECTION_RTL: Yoga.YGDirectionRTL,

    // Render mode constants
    RENDER_MODE_COLOR_RECT: 0,
    RENDER_MODE_STRETCH_TEXTURE: 1,

    CONFIG: Yoga.YGConfigNew(),
    // Safe defaults so engine calls like GUI.Render() do not crash before init
    SCENE: undefined,
    ROOT_NODE: undefined,
    Render: function() { /* no-op until initialized */ },
    Finalize: () => {
        try { if (GUI.ROOT_NODE) GUI.ROOT_NODE.free(); } catch (e) {}
        try { if (GUI.CONFIG) Yoga.YGConfigFree(GUI.CONFIG); } catch (e) {}
    }
}

// UI System class
class System extends Entity {
    constructor(width, height) {
        super(width, height);

        // Store references.
        this.width = width;
        this.height = height;
        
        // Create orthographic camera for UI rendering
        this.camera = new THREE.OrthographicCamera(
            -width / 2, width / 2, 
            height / 2, -height / 2, 
            0.01, 100
        );
        this.camera.position.z = 100;
        
        // Create UI scene
        GUI.SCENE = new THREE.Scene();
        
        // Root Yoga node
        GUI.ROOT_NODE = new Node()
            .setWidthPercent(100)
            .setHeightPercent(100)
            .setFlexDirection(GUI.FLEX_DIRECTION_ROW)
            .setJustifyContent(GUI.JUSTIFY_CENTER)
            .setAlignItems(GUI.ALIGN_CENTER);
        
        GUI.Render = (() => {
            DIADEM.Renderer.clearDepth();
            DIADEM.Renderer.render(GUI.SCENE, this.camera);
        }).bind(this);
    }
    
    // Update layout
    Update(_delta) {
        if (GUI.ROOT_NODE && GUI.ROOT_NODE.checkIfDirtied()) {
            GUI.ROOT_NODE.calculateLayout(this.width, this.height, GUI.DIRECTION_LTR);
            GUI.ROOT_NODE.updateFromLayout(0, 0);
        }
    }
    
    // Handle window resize
    resize(width, height) {
        this.width = width;
        this.height = height;
        
        // Update camera
        this.camera.left = -width / 2;
        this.camera.right = width / 2;
        this.camera.top = height / 2;
        this.camera.bottom = -height / 2;
        this.camera.updateProjectionMatrix();
        
        // Update root node
        if (GUI.ROOT_NODE) {
            GUI.ROOT_NODE.setWidth(width);
            GUI.ROOT_NODE.setHeight(height);
        }
    }
}

// Node constructor
const allNodes = {};
class Node {
    constructor(depth = 0) {

        // Screen-space coordinates
        this.x = 0;
        this.y = 0;
        this.xOffset = 0;
        this.yOffset = 0;
        this.width = 0;
        this.height = 0;

        // Render depth (for sorting)
        this.depth = depth;

        // Yoga node reference
        this.nodeRef = Yoga.YGNodeNewWithConfig(GUI.CONFIG);
        allNodes[this.nodeRef] = this;

        // Return self for method chaining
        return this;
    }

    // Use child.setChildOf(parent) whenever possible
    // Child node may have additional properties that need to be set.
    insertChild(child, index) {
        Yoga.YGNodeInsertChild(this.nodeRef, child.nodeRef, index);
        child.depth = this.depth + .001;
        return this;
    }

    getChild(index) {
        return allNodes[Yoga.YGNodeGetChild(this.nodeRef, index)];
    }

    getParent() {
        return allNodes[Yoga.YGNodeGetParent(this.nodeRef)];
    }

    getRoot() {
        let parent = this.getParent();
        if (parent) return parent.getRoot();
        return this;
    }

    setChildOf(parent) {
        parent.insertChild(this, parent.getChildCount());
        return this;
    }

    detach() {
        let parent = this.getParent();
        if (parent) parent.removeChild(this);
        return this;
    }

    checkIfDirtied() {
		if (this.isDirty())
			return true
		else {
			for (let i = 0; i < this.getChildCount(); i++) {
				if (this.getChild(i).checkIfDirtied())
					return true
			}
			return false
		}
	}

    // Update layout
    updateFromLayout(xOffset = 0, yOffset = 0) {
        this.x = this.layoutGetLeft() + xOffset;
        this.y = this.layoutGetTop() + yOffset;
        this.width = this.layoutGetWidth();
        this.height = this.layoutGetHeight();
        for (let i = 0; i < this.getChildCount(); i++) {
            this.getChild(i).updateFromLayout(xOffset + this.x, yOffset + this.y);
        }
    }
    
    // Cleanup
    free() {
        for (let i = this.getChildCount() - 1; i >= 0; i--) {
            this.getChild(i).free();
        }
        if (this.nodeRef) {
            delete allNodes[this.nodeRef];
            Yoga.YGNodeFree(this.nodeRef);
            this.nodeRef = null;
        }
    }
}

// Add node methods to Node class
{
    let requiredFunctions = [
        "YGNodeCalculateLayout",
        "YGNodeGetChildCount",
        "YGNodeRemoveAllChildren",
        "YGNodeRemoveChild",
        "YGNodeIsDirty"
    ];
    for (const method in Yoga)
        if (typeof Yoga[method] == 'function' &&
            (
                requiredFunctions.includes(method) ||
                method.startsWith('YGNodeStyleSet') ||
                method.startsWith('YGNodeLayoutGet')
            )
        )
        {
            let methodName = method
                .replace('YGNodeStyle', '')
                .replace('YGNode', '');
            methodName = methodName.charAt(0).toLowerCase() + methodName.slice(1);
            Node.prototype[methodName] = function(...args) {
                let result = Yoga[method](this.nodeRef, ...args);
                return method.indexOf("Get") == -1 ? this : result;
            }
        }
}

// Panel class
const rectVertices = new Float32Array([
    0.0, 0.0, 0.0,
    1.0, 0.0, 0.0,
    1.0, 1.0, 0.0,
    1.0, 1.0, 0.0,
    0.0, 1.0, 0.0,
    0.0, 0.0, 0.0,
]);
class Panel extends Node {
    constructor() {
        super();
        
        this.renderMode = null;
        this.texture = null;
        this.mesh = null;
        this.material = null;
        this.geometry = null;
        
        // For method chaining
        return this;
    }
    
    // Fluent API methods
    setRenderMode(mode) {
        this.renderMode = mode;
        this.createMesh();
        return this;
    }
    
    setTexture(textureOrColor) {
        // Accept THREE.Color instance, hex number, or CSS color string/name
        if (textureOrColor instanceof THREE.Color) {
            this.texture = textureOrColor;
        } else if (typeof textureOrColor === 'number' || typeof textureOrColor === 'string') {
            this.texture = new THREE.Color(textureOrColor);
        } else {
            this.texture = textureOrColor; // fallback (e.g., a Texture)
        }
        this.updateMaterial();
        return this;
    }
    
    setChildOf(parent) {
        GUI.SCENE.add(this.mesh);
        super.setChildOf(parent);
        return this;
    }

    detach() {
        GUI.SCENE.remove(this.mesh);
        super.detach();
        return this;
    }

    updateFromLayout(xOffset = 0, yOffset = 0) {
        super.updateFromLayout(xOffset, yOffset);
        
        // Update mesh.
        if (!this.mesh) return;
        this.mesh.scale.set(this.width, this.height, 1);

        // Map Yoga (top-left origin, +y down) to Three (center origin, +y up)
        const screenW = GUI.ROOT_NODE ? GUI.ROOT_NODE.layoutGetWidth() : 1920;
        const screenH = GUI.ROOT_NODE ? GUI.ROOT_NODE.layoutGetHeight() : 1080;

        // Our geometry is anchored at its bottom-left (0..1 quad). To place visually by Yoga's top-left:
        // - Bottom-left point in Yoga space = (left, top + height)
        // - Convert to centered coords: (x - W/2, H/2 - y)
        const bottomLeftY = this.y + this.height;
        const posX = this.x - screenW / 2;
        const posY = (screenH / 2) - bottomLeftY;
        this.mesh.position.set(posX, posY, this.depth);
    }
    
    // Create Three.js mesh based on render mode
    createMesh() {

        // Clean up existing mesh
        if (this.mesh) {
            if (this.mesh.parent) {
                this.mesh.parent.remove(this.mesh);
            }
        }
        
        // Create new mesh
        switch (this.renderMode) {
            case GUI.RENDER_MODE_COLOR_RECT:
            case GUI.RENDER_MODE_STRETCH_TEXTURE:
                this.geometry = new THREE.BufferGeometry();
                this.geometry.setAttribute('position', new THREE.BufferAttribute(rectVertices, 3));
                this.material = new THREE.MeshBasicMaterial({
                    depthTest: false,
                    depthWrite: false,
                    transparent: true,
                    opacity: 1.0,
                    side: THREE.DoubleSide,
                });
                break;
        }
        
        if (this.geometry && this.material) {
            this.mesh = new THREE.Mesh(this.geometry, this.material);
            this.mesh.position.z = 1; // inside ortho frustum
            this.mesh.renderOrder = 1000; // draw on top of 3D scene
            this.updateMaterial();
        }
    }
    
    // Update material based on texture and render mode
    updateMaterial() {
        if (!this.material) return;
        
        switch (this.renderMode) {
            case GUI.RENDER_MODE_COLOR_RECT:
                if (this.texture instanceof THREE.Color) {
                    this.material.color = this.texture;
                    this.material.map = null;
                }
                break;
                
            case GUI.RENDER_MODE_STRETCH_TEXTURE:
                if (this.texture && this.texture.isTexture) {
                    this.material.map = this.texture;
                    this.material.color = new THREE.Color(0xffffff);
                }
                break;
        }
    }
}

GUI.System = System;
GUI.Panel = Panel;

// Instantiate the GUI system after classes are available
new GUI.System(1920, 1080);