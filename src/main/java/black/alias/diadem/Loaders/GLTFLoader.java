package black.alias.diadem.Loaders;

import org.lwjgl.assimp.*;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.Context;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;

/**
 * Custom GLTF loader using LWJGL Assimp that creates Three.js compatible objects
 */
public class GLTFLoader {
    
    private final Context jsContext;
    private final Value threeJS;
    private final TextureLoader textureLoader;
    private String modelBaseDir = ""; // path relative to assets/ for resolving textures
    
    public GLTFLoader(Context jsContext, Value threeJS, TextureLoader textureLoader) {
        this.jsContext = jsContext;
        this.threeJS = threeJS;
        this.textureLoader = textureLoader;
    }

    // Compute base directory inside assets/ for resolving texture files referenced by the model
    private String computeBaseDirRelativeToAssets(String filePath) {
        if (filePath == null) return "";
        String p = filePath.replace('\\', '/');
        // Expect paths like "assets/models/.../file.gltf" in dev mode
        final String assetsPrefix = "assets/";
        if (p.startsWith(assetsPrefix)) {
            p = p.substring(assetsPrefix.length());
        }
        int idx = p.lastIndexOf('/');
        if (idx >= 0) {
            return p.substring(0, idx);
        }
        return "";
    }
    
    /**
     * Load a GLTF file and return a Three.js compatible scene object
     */
    public Value loadGLTF(String filePath) {
        // Load the scene using Assimp
        AIScene scene = aiImportFile(filePath, 
            aiProcess_Triangulate | 
            aiProcess_FlipUVs | // Re-add UV flipping for Assimp
            aiProcess_CalcTangentSpace |
            aiProcess_JoinIdenticalVertices);
            
        if (scene == null) {
            throw new RuntimeException("Failed to load GLTF file: " + filePath + " - " + aiGetErrorString());
        }
        // Compute base directory relative to assets/ for resolving textures
        this.modelBaseDir = computeBaseDirRelativeToAssets(filePath);

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
            float minU = Float.MAX_VALUE, maxU = Float.MIN_VALUE;
            float minV = Float.MAX_VALUE, maxV = Float.MIN_VALUE;
            
            for (int i = 0; i < mesh.mNumVertices(); i++) {
                AIVector3D texCoord = texCoords.get(i);
                float u = texCoord.x();
                float v = texCoord.y();
                
                // Fix V coordinate offset - normalize to [0,1] range
                if (v > 1.0f) {
                    v = v - 1.0f; // Shift V coordinates down by 1.0
                }
                
                uvArray[i * 2] = u;
                uvArray[i * 2 + 1] = v;
                
                // Track UV bounds for debugging
                minU = Math.min(minU, u); maxU = Math.max(maxU, u);
                minV = Math.min(minV, v); maxV = Math.max(maxV, v);
            }
            
            System.out.println("DEBUG: UV bounds - U: [" + minU + ", " + maxU + "], V: [" + minV + ", " + maxV + "]");
            
            uvAttribute = Float32BufferAttribute.newInstance(uvArray, 2);
            geometry.invokeMember("setAttribute", "uv", uvAttribute);
            // Duplicate uv to uv2 so aoMap can work if present
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
        Value MeshStandardMaterial = threeJS.getMember("MeshStandardMaterial");
        Value material = MeshStandardMaterial.newInstance();
        // Reasonable defaults if no textures
        material.putMember("metalness", 0.2);
        material.putMember("roughness", 0.6);
        
        // PBR texture set
        assignPBRTextures(material, mesh, scene);
        
        
        // Create mesh
        Value Mesh = threeJS.getMember("Mesh");
        Value threeMesh = Mesh.newInstance(geometry, material);
        threeMesh.putMember("name", "DuckMesh");
        
        return threeMesh;
    }
    
    /**
     * Load texture for a mesh using Assimp material information
     */
    private Value loadMeshTexture(AIMesh mesh, AIScene scene) {
        try {
            // Get material for this mesh
            if (mesh.mMaterialIndex() >= 0 && mesh.mMaterialIndex() < scene.mNumMaterials()) {
                AIMaterial material = AIMaterial.create(scene.mMaterials().get(mesh.mMaterialIndex()));
                
                // Try base color first (glTF2), then diffuse as fallback
                AIString texturePath = AIString.create();
                int result = aiGetMaterialTexture(material, aiTextureType_BASE_COLOR, 0, texturePath, (int[])null, null, null, null, null, null);
                if (result != aiReturn_SUCCESS) {
                    result = aiGetMaterialTexture(material, aiTextureType_DIFFUSE, 0, texturePath, (int[])null, null, null, null, null, null);
                }
                
                if (result == aiReturn_SUCCESS) {
                    String textureFile = texturePath.dataString();
                    String resolved = resolveTextureRelativeToAssets(textureFile);
                    return textureLoader.loadTexture(resolved);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading texture: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
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

        // Base color (albedo) - DIAGNOSTIC: Force simple color instead of texture
        String baseColorFile = queryTex.apply(aiTextureType_BASE_COLOR);
        if (baseColorFile == null) baseColorFile = queryTex.apply(aiTextureType_DIFFUSE);
        System.out.println("DEBUG: Base color texture file: " + baseColorFile);
        
        // Re-enable base color texture with corrected settings
        if (baseColorFile != null) {
            String resolved = resolveTextureRelativeToAssets(baseColorFile);
            System.out.println("DEBUG: Loading texture: " + resolved);
            Value tex = textureLoader.loadTexture(resolved);
            if (tex != null) {
                // Try clamped wrapping to prevent UV issues
                Value ClampToEdgeWrapping = threeJS.getMember("ClampToEdgeWrapping");
                if (ClampToEdgeWrapping != null) {
                    tex.putMember("wrapS", ClampToEdgeWrapping);
                    tex.putMember("wrapT", ClampToEdgeWrapping);
                }
                tex.putMember("flipY", false); // Back to GLTF standard
                tex.putMember("generateMipmaps", false); // Disable mipmaps for now
                Value LinearFilter = threeJS.getMember("LinearFilter");
                if (LinearFilter != null) {
                    tex.putMember("minFilter", LinearFilter);
                    tex.putMember("magFilter", LinearFilter);
                }
                Value SRGBColorSpace = threeJS.getMember("SRGBColorSpace");
                if (SRGBColorSpace != null) tex.putMember("colorSpace", SRGBColorSpace);
                tex.putMember("needsUpdate", true);
                material.putMember("map", tex);
                System.out.println("DEBUG: Base color texture applied with clamp wrapping");
            } else {
                System.out.println("DEBUG: Failed to load texture: " + resolved);
            }
        }

        // Re-enable all PBR texture maps with corrected UV coordinates
        System.out.println("DEBUG: Re-enabling all PBR texture maps");
        
        // Normal map
        String normalFile = queryTex.apply(aiTextureType_NORMALS);
        if (normalFile == null) normalFile = queryTex.apply(aiTextureType_HEIGHT); // sometimes used
        if (normalFile != null) {
            String resolved = resolveTextureRelativeToAssets(normalFile);
            Value tex = textureLoader.loadTexture(resolved);
            if (tex != null) {
                applyPBRTextureSettings(tex, false); // Linear color space for normal maps
                material.putMember("normalMap", tex);
                System.out.println("DEBUG: Normal map applied: " + normalFile);
            }
        }

        // Metalness & Roughness (glTF packs in one texture). Try METALNESS, then DIFFUSE_ROUGHNESS, then SPECULAR
        String mrFile = queryTex.apply(aiTextureType_METALNESS);
        if (mrFile == null) mrFile = queryTex.apply(aiTextureType_DIFFUSE_ROUGHNESS);
        if (mrFile == null) mrFile = queryTex.apply(aiTextureType_UNKNOWN); // some importers tag as UNKNOWN
        if (mrFile != null) {
            String resolved = resolveTextureRelativeToAssets(mrFile);
            Value tex = textureLoader.loadTexture(resolved);
            if (tex != null) {
                applyPBRTextureSettings(tex, false); // Linear color space for metallic/roughness
                material.putMember("metalnessMap", tex);
                material.putMember("roughnessMap", tex);
                System.out.println("DEBUG: Metallic/Roughness map applied: " + mrFile);
            }
        }

        // Ambient Occlusion
        String aoFile = queryTex.apply(aiTextureType_AMBIENT_OCCLUSION);
        if (aoFile == null) aoFile = queryTex.apply(aiTextureType_LIGHTMAP);
        if (aoFile != null) {
            String resolved = resolveTextureRelativeToAssets(aoFile);
            Value tex = textureLoader.loadTexture(resolved);
            if (tex != null) {
                applyPBRTextureSettings(tex, false); // Linear color space for AO
                material.putMember("aoMap", tex);
                System.out.println("DEBUG: AO map applied: " + aoFile);
            }
        }

        // Emissive
        String emissiveFile = queryTex.apply(aiTextureType_EMISSIVE);
        if (emissiveFile != null) {
            String resolved = resolveTextureRelativeToAssets(emissiveFile);
            Value tex = textureLoader.loadTexture(resolved);
            if (tex != null) {
                applyPBRTextureSettings(tex, true); // sRGB color space for emissive
                material.putMember("emissiveMap", tex);
                // Set emissive intensity and color to make it visible
                Value Color = threeJS.getMember("Color");
                if (Color != null) {
                    Value emissiveColor = Color.newInstance(1.0, 1.0, 1.0); // White emissive
                    material.putMember("emissive", emissiveColor);
                    material.putMember("emissiveIntensity", 1.0); // Full intensity
                }
                System.out.println("DEBUG: Emissive map applied with intensity: " + emissiveFile);
            }
        }
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

    // Configure wrapping, color space, and mipmap filtering for a texture
    private void configureTextureColor(Value tex, boolean sRGB, boolean mipmaps) {
        if (tex == null) return;
        // Wrapping: clamp to avoid streaks if UV slightly out of [0,1]
        Value ClampToEdgeWrapping = threeJS.getMember("ClampToEdgeWrapping");
        if (ClampToEdgeWrapping != null) {
            tex.putMember("wrapS", ClampToEdgeWrapping);
            tex.putMember("wrapT", ClampToEdgeWrapping);
        }
        // For GLTF convention, use unflipped textures (flipY=false). We also removed aiProcess_FlipUVs.
        tex.putMember("flipY", false);
        // Reset transform to defaults
        Value Vector2 = threeJS.getMember("Vector2");
        if (Vector2 != null) {
            Value oneOne = Vector2.newInstance(1.0, 1.0);
            Value zeroZero = Vector2.newInstance(0.0, 0.0);
            tex.putMember("repeat", oneOne);
            tex.putMember("offset", zeroZero);
            tex.putMember("center", zeroZero);
        }
        // Color space
        if (sRGB) {
            Value SRGB = threeJS.getMember("SRGBColorSpace");
            if (SRGB != null) tex.putMember("colorSpace", SRGB);
        } else {
            Value Linear = threeJS.getMember("LinearSRGBColorSpace");
            if (Linear != null) tex.putMember("colorSpace", Linear);
        }
        // Filters and mipmaps
        Value LinearFilter = threeJS.getMember("LinearFilter");
        Value LinearMipmapLinearFilter = threeJS.getMember("LinearMipmapLinearFilter");
        if (mipmaps && LinearMipmapLinearFilter != null) {
            tex.putMember("generateMipmaps", true);
            tex.putMember("minFilter", LinearMipmapLinearFilter);
        } else if (LinearFilter != null) {
            tex.putMember("generateMipmaps", false);
            tex.putMember("minFilter", LinearFilter);
        }
        if (LinearFilter != null) tex.putMember("magFilter", LinearFilter);
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
}
