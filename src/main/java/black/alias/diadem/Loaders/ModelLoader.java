package black.alias.diadem.Loaders;
import org.lwjgl.assimp.*;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.Context;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;
public class ModelLoader {
	
	private final Context jsContext;
	private final Value threeJS;
	private final TextureLoader textureLoader;
	private String modelBaseDir = "";
	private AIPropertyStore store = aiCreatePropertyStore();
	
	public ModelLoader(Context jsContext, Value threeJS, TextureLoader textureLoader) {
		this.jsContext = jsContext;
		this.threeJS = threeJS;
		this.textureLoader = textureLoader;
		aiSetImportPropertyInteger(store, AI_CONFIG_IMPORT_FBX_EMBEDDED_TEXTURES_LEGACY_NAMING, 1);
	}

	// Compute base directory inside assets/ for resolving texture files referenced by the model
	private String computeBaseDirRelativeToAssets(String filePath) {
		if (filePath == null) return "";
		int idx = filePath.lastIndexOf('/');
		if (idx >= 0) {
			return filePath.substring(0, idx);
		}
		return "";
	}
	
	/**
	 * Load a GLTF file and return a Three.js compatible scene object
	 */
	public Value load(String filePath) {
		final int flags =
			aiProcess_Triangulate |
			aiProcess_FlipUVs |
			aiProcess_CalcTangentSpace |
			aiProcess_JoinIdenticalVertices;

		// Normalize file path
		String rel = filePath.replace('\\', '/');
		if (rel.startsWith("/")) rel = rel.substring(1);
		if (rel.startsWith("assets/")) rel = rel.substring("assets/".length());

		// Compute base directory relative to assets/ for resolving textures
		this.modelBaseDir = computeBaseDirRelativeToAssets(rel);

		// Load main model bytes from /assets
		AIScene scene = null;
		try (java.io.InputStream is = ModelLoader.class.getResourceAsStream("/assets/" + rel)) {
			if (is != null) {
				byte[] mainBytes = is.readAllBytes();
				ClasspathAssimpIO cpIO = new ClasspathAssimpIO(this.modelBaseDir, rel, mainBytes);
				AIFileIO io = cpIO.get();
				scene = aiImportFileExWithProperties(cpIO.getMainVirtualPath(), flags, io, store);
				io.free();
			}
		} catch (Exception ignore) {}
		
		// Throw error if unable to load
		if (scene == null) {
			throw new RuntimeException("Failed to load GLTF file: " + filePath + " - " + aiGetErrorString());
		}

		// Create Three.js Group for the scene
		Value Group = threeJS.getMember("Group");
		Value rootGroup = Group.newInstance();
		rootGroup.putMember("name", "Scene");
		
		// Process meshes
		if (scene.mNumMeshes() > 0) {
			for (int i = 0; i < scene.mNumMeshes(); i++) {
				AIMesh mesh = AIMesh.create(scene.mMeshes().get(i));
				Value threeMesh = convertMesh(mesh, scene);
				if (threeMesh != null) {
					rootGroup.invokeMember("add", threeMesh);
				}
			}
		}
		
		// Create GLTF-compatible result object
		Value Object = jsContext.eval("js", "Object");
		Value gltfResult = Object.newInstance();
		
		// Set properties to match GLTFLoader structure
		gltfResult.putMember("scene", rootGroup);
		
		// Create scenes array
		Value Array = jsContext.eval("js", "Array");
		Value scenes = Array.newInstance();
		scenes.setArrayElement(0, rootGroup);
		gltfResult.putMember("scenes", scenes);
		
		// Empty arrays for animations and cameras (can be extended later)
		Value animations = Array.newInstance();
		gltfResult.putMember("animations", animations);
		
		Value cameras = Array.newInstance();
		gltfResult.putMember("cameras", cameras);
		
		// Asset info
		Value asset = Object.newInstance();
		asset.putMember("generator", "LWJGL Assimp Loader");
		asset.putMember("version", "2.0");
		gltfResult.putMember("asset", asset);
		
		// User data
		Value userData = Object.newInstance();
		gltfResult.putMember("userData", userData);
		
		return gltfResult;
	}
	
	/**
	 * Convert Assimp mesh to Three.js Mesh
	 */
	private Value convertMesh(AIMesh mesh, AIScene scene) {
		// Create BufferGeometry
		Value BufferGeometry = threeJS.getMember("BufferGeometry");
		Value geometry = BufferGeometry.newInstance();
		
		// Extract vertices
		AIVector3D.Buffer vertices = mesh.mVertices();
		float[] vertexArray = new float[mesh.mNumVertices() * 3];
		for (int i = 0; i < mesh.mNumVertices(); i++) {
			AIVector3D vertex = vertices.get(i);
			vertexArray[i * 3] = vertex.x();
			vertexArray[i * 3 + 1] = vertex.y();
			vertexArray[i * 3 + 2] = vertex.z();
		}
		
		// Create Three.js Float32BufferAttribute for positions
		Value Float32BufferAttribute = threeJS.getMember("Float32BufferAttribute");
		Value positionAttribute = Float32BufferAttribute.newInstance(vertexArray, 3);
		geometry.invokeMember("setAttribute", "position", positionAttribute);
		
		// Extract normals if available
		AIVector3D.Buffer normals = mesh.mNormals();
		if (normals != null) {
			float[] normalArray = new float[mesh.mNumVertices() * 3];
			for (int i = 0; i < mesh.mNumVertices(); i++) {
				AIVector3D normal = normals.get(i);
				normalArray[i * 3] = normal.x();
				normalArray[i * 3 + 1] = normal.y();
				normalArray[i * 3 + 2] = normal.z();
			}
			Value normalAttribute = Float32BufferAttribute.newInstance(normalArray, 3);
			geometry.invokeMember("setAttribute", "normal", normalAttribute);
		}
		
		// Extract UV coordinates if available
		AIVector3D.Buffer texCoords = mesh.mTextureCoords(0);
		Value uvAttribute = null;
		if (texCoords != null) {
			float[] uvArray = new float[mesh.mNumVertices() * 2];
			
			for (int i = 0; i < mesh.mNumVertices(); i++) {
				AIVector3D texCoord = texCoords.get(i);
				float u = texCoord.x();
				float v = texCoord.y();
				
				// Normalize V to [0,1]
				if (v > 1.0f) {
					v = v - 1.0f;
				}
				
				uvArray[i * 2] = u;
				uvArray[i * 2 + 1] = v;
			}
			
			uvAttribute = Float32BufferAttribute.newInstance(uvArray, 2);
			geometry.invokeMember("setAttribute", "uv", uvAttribute);
			geometry.invokeMember("setAttribute", "uv2", uvAttribute);
		}
		
		// Extract indices
		AIFace.Buffer faces = mesh.mFaces();
		List<Integer> indexList = new ArrayList<>();
		for (int i = 0; i < mesh.mNumFaces(); i++) {
			AIFace face = faces.get(i);
			IntBuffer indices = face.mIndices();
			for (int j = 0; j < face.mNumIndices(); j++) {
				indexList.add(indices.get(j));
			}
		}
		
		if (!indexList.isEmpty()) {
			int[] indexArray = indexList.stream().mapToInt(Integer::intValue).toArray();
			Value Uint16BufferAttribute = threeJS.getMember("Uint16BufferAttribute");
			Value indexAttribute = Uint16BufferAttribute.newInstance(indexArray, 1);
			geometry.invokeMember("setIndex", indexAttribute);
		}
		
		// Create material (use MeshStandardMaterial for PBR)
		Value MeshPhysicalMaterial = threeJS.getMember("MeshPhysicalMaterial");
		Value material = MeshPhysicalMaterial.newInstance();
		material.putMember("metalness", 0.2);
		material.putMember("roughness", 0.6);
		assignPBRTextures(material, mesh, scene);
		
		// Create mesh
		Value Mesh = threeJS.getMember("Mesh");
		Value threeMesh = Mesh.newInstance(geometry, material);
		threeMesh.putMember("name", "Mesh");
		
		return threeMesh;
	}
	

	// Populate PBR texture slots on a MeshStandardMaterial
	private void assignPBRTextures(Value material, AIMesh mesh, AIScene scene) {
		if (mesh.mMaterialIndex() < 0 || mesh.mMaterialIndex() >= scene.mNumMaterials()) return;
		AIMaterial aiMat = AIMaterial.create(scene.mMaterials().get(mesh.mMaterialIndex()));

		// Helper to query a texture of a given type
		java.util.function.Function<Integer, String> queryTex = (type) -> {
			AIString path = AIString.create();
			int res = aiGetMaterialTexture(aiMat, type, 0, path, (int[])null, null, null, null, null, null);
			if (res == aiReturn_SUCCESS) return path.dataString();
			return null;
		};

		// Base color (albedo)
		String baseColorFile = queryTex.apply(aiTextureType_BASE_COLOR);
		if (baseColorFile == null) baseColorFile = queryTex.apply(aiTextureType_DIFFUSE);
		
		// Base color texture
		if (baseColorFile != null) {
			Value tex = loadTextureReference(baseColorFile, scene);
			if (tex != null) {
				Value ClampToEdgeWrapping = threeJS.getMember("ClampToEdgeWrapping");
				if (ClampToEdgeWrapping != null) {
					tex.putMember("wrapS", ClampToEdgeWrapping);
					tex.putMember("wrapT", ClampToEdgeWrapping);
				}
				tex.putMember("flipY", false);
				tex.putMember("generateMipmaps", false);
				Value LinearFilter = threeJS.getMember("LinearFilter");
				if (LinearFilter != null) {
					tex.putMember("minFilter", LinearFilter);
					tex.putMember("magFilter", LinearFilter);
				}
				Value SRGBColorSpace = threeJS.getMember("SRGBColorSpace");
				if (SRGBColorSpace != null) tex.putMember("colorSpace", SRGBColorSpace);
				tex.putMember("needsUpdate", true);
				material.putMember("map", tex);
			}
		}
		
		// Normal map
		String normalFile = queryTex.apply(aiTextureType_NORMALS);
		if (normalFile == null) normalFile = queryTex.apply(aiTextureType_HEIGHT);
		if (normalFile != null) {
			Value tex = loadTextureReference(normalFile, scene);
			if (tex != null) {
				applyPBRTextureSettings(tex, false);
				material.putMember("normalMap", tex);
			}
		}

		// Metalness & Roughness
		String mrFile = queryTex.apply(aiTextureType_METALNESS);
		if (mrFile == null) mrFile = queryTex.apply(aiTextureType_DIFFUSE_ROUGHNESS);
		if (mrFile == null) mrFile = queryTex.apply(aiTextureType_UNKNOWN);
		if (mrFile != null) {
			Value tex = loadTextureReference(mrFile, scene);
			if (tex != null) {
				applyPBRTextureSettings(tex, false);
				material.putMember("metalnessMap", tex);
				material.putMember("roughnessMap", tex);
			}
		}

		// Ambient Occlusion
		String aoFile = queryTex.apply(aiTextureType_AMBIENT_OCCLUSION);
		if (aoFile == null) aoFile = queryTex.apply(aiTextureType_LIGHTMAP);
		if (aoFile != null) {
			Value tex = loadTextureReference(aoFile, scene);
			if (tex != null) {
				applyPBRTextureSettings(tex, false);
				material.putMember("aoMap", tex);
			}
		}

		// Emissive
		String emissiveFile = queryTex.apply(aiTextureType_EMISSIVE);
		if (emissiveFile != null) {
			Value tex = loadTextureReference(emissiveFile, scene);
			if (tex != null) {
				applyPBRTextureSettings(tex, true);
				material.putMember("emissiveMap", tex);
				Value Color = threeJS.getMember("Color");
				if (Color != null) {
					Value emissiveColor = Color.newInstance(1.0, 1.0, 1.0);
					material.putMember("emissive", emissiveColor);
					material.putMember("emissiveIntensity", 1.0);
				}
			}
		}
	}

	// Load either an external texture (relative to assets/) or an embedded Assimp texture ("*index")
	private Value loadTextureReference(String ref, AIScene scene) {
		if (ref == null) return null;
		String tf = ref.trim();
		if (tf.startsWith("*")) {
			int idx = parseEmbeddedIndex(tf);
			if (idx >= 0 && scene.mNumTextures() > idx) {
				AITexture aiTex = AITexture.create(scene.mTextures().get(idx));
				return textureLoader.createDataTextureFromAITexture(aiTex);
			}
			return null;
		}
		String resolved = resolveTextureRelativeToAssets(tf);
		return textureLoader.loadTexture(resolved);
	}

	private int parseEmbeddedIndex(String ref) {
		// ref like "*0" or "*0.jpg"; parse consecutive digits after '*'
		int i = 1;
		int len = ref.length();
		int val = 0;
		boolean found = false;
		while (i < len) {
			char c = ref.charAt(i);
			if (c >= '0' && c <= '9') {
				found = true;
				val = val * 10 + (c - '0');
				i++;
			} else {
				break;
			}
		}
		return found ? val : -1;
	}

	// Apply consistent PBR texture settings (same as base color texture)
	private void applyPBRTextureSettings(Value tex, boolean sRGB) {
		if (tex == null) return;
		
		// Same settings as base color texture for consistency
		Value ClampToEdgeWrapping = threeJS.getMember("ClampToEdgeWrapping");
		if (ClampToEdgeWrapping != null) {
			tex.putMember("wrapS", ClampToEdgeWrapping);
			tex.putMember("wrapT", ClampToEdgeWrapping);
		}
		tex.putMember("flipY", false); // GLTF standard
		tex.putMember("generateMipmaps", false); // Keep consistent with base color
		Value LinearFilter = threeJS.getMember("LinearFilter");
		if (LinearFilter != null) {
			tex.putMember("minFilter", LinearFilter);
			tex.putMember("magFilter", LinearFilter);
		}
		
		// Color space
		if (sRGB) {
			Value SRGBColorSpace = threeJS.getMember("SRGBColorSpace");
			if (SRGBColorSpace != null) tex.putMember("colorSpace", SRGBColorSpace);
		} else {
			Value LinearSRGBColorSpace = threeJS.getMember("LinearSRGBColorSpace");
			if (LinearSRGBColorSpace != null) tex.putMember("colorSpace", LinearSRGBColorSpace);
		}
		
		tex.putMember("needsUpdate", true);
	}


	private String resolveTextureRelativeToAssets(String textureFile) {
		if (textureFile == null || textureFile.isEmpty()) return textureFile;
		String tf = textureFile.replace('\\', '/');
		// If the path already starts with assets/, strip it so TextureLoader treats it as relative to assets/
		if (tf.startsWith("assets/")) {
			return tf.substring("assets/".length());
		}
		// If it's already relative (contains directories), join with model base dir if not already rooted
		if (tf.startsWith("/")) {
			// Remove leading slash and treat as relative to assets
			tf = tf.substring(1);
		}
		if (modelBaseDir == null || modelBaseDir.isEmpty()) {
			return tf; // use as-is relative to assets/
		}
		// Join base dir with texture filename
		if (tf.contains("/")) {
			// If tf already has directories, assume it's correct relative to base or assets
			return modelBaseDir + "/" + tf;
		} else {
			return modelBaseDir + "/" + tf;
		}
	}

	public void close() {
		aiReleasePropertyStore(store);
	}
}
