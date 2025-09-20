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
    
    private Context jsContext;
    private Value threeJS;
    
    public GLTFLoader(Context jsContext, Value threeJS) {
        this.jsContext = jsContext;
        this.threeJS = threeJS;
    }
    
    /**
     * Load a GLTF file and return a Three.js compatible scene object
     */
    public Value loadGLTF(String filePath) {
        // Load the scene using Assimp
        AIScene scene = aiImportFile(filePath, 
            aiProcess_Triangulate | 
            aiProcess_FlipUVs | 
            aiProcess_CalcTangentSpace |
            aiProcess_JoinIdenticalVertices);
            
        if (scene == null) {
            throw new RuntimeException("Failed to load GLTF file: " + filePath + " - " + aiGetErrorString());
        }
        
        try {
            return convertToThreeJS(scene);
        } finally {
            aiReleaseImport(scene);
        }
    }
    
    /**
     * Convert Assimp scene to Three.js GLTF-compatible object
     */
    private Value convertToThreeJS(AIScene scene) {
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
        if (texCoords != null) {
            float[] uvArray = new float[mesh.mNumVertices() * 2];
            for (int i = 0; i < mesh.mNumVertices(); i++) {
                AIVector3D texCoord = texCoords.get(i);
                uvArray[i * 2] = texCoord.x();
                uvArray[i * 2 + 1] = texCoord.y();
            }
            Value uvAttribute = Float32BufferAttribute.newInstance(uvArray, 2);
            geometry.invokeMember("setAttribute", "uv", uvAttribute);
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
        
        // Create material (use MeshLambertMaterial for proper lighting)
        Value MeshLambertMaterial = threeJS.getMember("MeshLambertMaterial");
        Value material = MeshLambertMaterial.newInstance();
        
        // Try to load texture if available
        Value texture = loadMeshTexture(mesh, scene);
        if (texture != null) {
            material.putMember("map", texture);
        } else {
            // Set a visible color if no texture
            Value Color = threeJS.getMember("Color");
            Value color = Color.newInstance(0xffffff); // White color to show lighting properly
            material.putMember("color", color);
        }
        
        
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
                
                // Try to get diffuse texture
                AIString texturePath = AIString.create();
                int result = aiGetMaterialTexture(material, aiTextureType_DIFFUSE, 0, texturePath, (int[])null, null, null, null, null, null);
                
                if (result == aiReturn_SUCCESS) {
                    String textureFile = texturePath.dataString();
                    
                    // Load texture file using Java ImageIO (like we did before)
                    return createThreeJSTexture(textureFile);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading texture: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Create Three.js texture from image file
     */
    private Value createThreeJSTexture(String textureFile) {
        try {
            // Load image using Java ImageIO
            java.nio.file.Path imagePath = java.nio.file.Paths.get("src/main/assets/" + textureFile);
            if (!java.nio.file.Files.exists(imagePath)) {
                System.err.println("Texture file not found: " + imagePath);
                return null;
            }
            
            java.awt.image.BufferedImage bufferedImage = javax.imageio.ImageIO.read(imagePath.toFile());
            if (bufferedImage == null) {
                System.err.println("Failed to load image: " + imagePath);
                return null;
            }
            
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            
            // Extract RGBA pixel data
            int[] pixels = new int[width * height];
            bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);
            
            // Convert to Uint8Array format (like we did successfully before)
            int[] pixelData = new int[width * height * 4];
            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                pixelData[i * 4] = (pixel >> 16) & 0xFF; // R
                pixelData[i * 4 + 1] = (pixel >> 8) & 0xFF;  // G
                pixelData[i * 4 + 2] = pixel & 0xFF;         // B
                pixelData[i * 4 + 3] = (pixel >> 24) & 0xFF; // A
            }
            
            // Create Uint8Array in JavaScript (this worked before)
            Value Uint8Array = jsContext.eval("js", "Uint8Array");
            Value imageData = Uint8Array.newInstance(pixelData);
            
            // Create Three.js DataTexture
            Value DataTexture = threeJS.getMember("DataTexture");
            Value UnsignedByteType = threeJS.getMember("UnsignedByteType");
            Value RGBAFormat = threeJS.getMember("RGBAFormat");
            
            Value texture = DataTexture.newInstance(imageData, width, height, RGBAFormat, UnsignedByteType);
            texture.putMember("needsUpdate", true);
            
            return texture;
            
        } catch (Exception e) {
            System.err.println("Error creating Three.js texture: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
