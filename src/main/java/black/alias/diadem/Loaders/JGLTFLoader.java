package black.alias.diadem.Loaders;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.Context;
import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.io.*;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import de.javagl.jgltf.impl.v2.GlTF;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

/**
 * GLB loader for embedded binary GLTF files.
 * Supports PBR materials with embedded textures.
 * Note: Does not support Draco compression.
 */
public class JGLTFLoader {
    
    private final Value threeJS;
    private final TextureLoader textureLoader;
    private final Context jsContext;
    private final Value Object;
    private final Value Float32Array;
    private final Value Uint16Array;
    private final Value Uint32Array;
    
    public JGLTFLoader(Context jsContext, Value threeJS, TextureLoader textureLoader) {
        this.jsContext = jsContext;
        this.threeJS = threeJS;
        this.textureLoader = textureLoader;

        // Javascript objects
        this.Object = jsContext.eval("js", "Object");
        this.Float32Array = jsContext.eval("js", "Float32Array");
        this.Uint16Array = jsContext.eval("js", "Uint16Array");
        this.Uint32Array = jsContext.eval("js", "Uint32Array");
    }
    
    public Value load(String filePath) {
        try {
            String rel = filePath.replace('\\', '/');
            if (rel.startsWith("/")) rel = rel.substring(1);
            if (rel.startsWith("assets/")) rel = rel.substring("assets/".length());
            
            GltfModel gltfModel = loadGLB(rel);
            Value rootGroup = threeJS.getMember("Group").newInstance();
            
            for (NodeModel node : gltfModel.getSceneModels().get(0).getNodeModels()) {
                Value nodeObject = processNode(node, gltfModel);
                if (nodeObject != null) {
                    rootGroup.invokeMember("add", nodeObject);
                }
            }
            
            Value gltfObject = Object.newInstance();
            gltfObject.putMember("scene", rootGroup);
            Value Array = jsContext.eval("js", "Array");
            gltfObject.putMember("animations", Array.newInstance());
            gltfObject.putMember("scenes", Array.newInstance(rootGroup));
            gltfObject.putMember("cameras", Array.newInstance());
            
            return gltfObject;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load GLB: " + filePath, e);
        }
    }
    
    private Value processNode(NodeModel node, GltfModel gltfModel) {
        Value Group = threeJS.getMember("Group");
        Value nodeObject = Group.newInstance();
        
        if (node.getName() != null) {
            nodeObject.putMember("name", node.getName());
        }
        
        // Apply transformation matrix
        float[] matrix = node.getMatrix();
        if (matrix != null) {
            Value Matrix4 = threeJS.getMember("Matrix4");
            Value mat = Matrix4.newInstance(
                (double)matrix[0], (double)matrix[4], (double)matrix[8],  (double)matrix[12],
                (double)matrix[1], (double)matrix[5], (double)matrix[9],  (double)matrix[13],
                (double)matrix[2], (double)matrix[6], (double)matrix[10], (double)matrix[14],
                (double)matrix[3], (double)matrix[7], (double)matrix[11], (double)matrix[15]
            );
            
            nodeObject.invokeMember("applyMatrix4", mat);
        }
        
        // Process mesh if present
        List<MeshModel> meshModels = node.getMeshModels();
        if (meshModels != null && !meshModels.isEmpty()) {
            for (MeshModel meshModel : meshModels) {
                Value meshObject = processMesh(meshModel, gltfModel);
                if (meshObject != null) {
                    nodeObject.invokeMember("add", meshObject);
                }
            }
        }
        
        // Process children recursively
        List<NodeModel> children = node.getChildren();
        if (children != null) {
            for (NodeModel child : children) {
                Value childObject = processNode(child, gltfModel);
                if (childObject != null) {
                    nodeObject.invokeMember("add", childObject);
                }
            }
        }
        
        return nodeObject;
    }
    
    private Value processMesh(MeshModel meshModel, GltfModel gltfModel) {
        List<MeshPrimitiveModel> primitives = meshModel.getMeshPrimitiveModels();
        if (primitives.isEmpty()) return null;
        
        MeshPrimitiveModel primitive = primitives.get(0);
        Value geometry = threeJS.getMember("BufferGeometry").newInstance();
        Map<String, AccessorModel> attributes = primitive.getAttributes();
        
        AccessorModel positionAccessor = attributes.get("POSITION");
        if (positionAccessor != null) {
            Value jsPositions = Float32Array.newInstance(getFloatArray(positionAccessor));
            Value posAttr = threeJS.getMember("Float32BufferAttribute").newInstance(jsPositions, 3);
            geometry.invokeMember("setAttribute", "position", posAttr);
        }
        
        AccessorModel normalAccessor = attributes.get("NORMAL");
        if (normalAccessor != null) {
            Value jsNormals = Float32Array.newInstance(getFloatArray(normalAccessor));
            Value normAttr = threeJS.getMember("Float32BufferAttribute").newInstance(jsNormals, 3);
            geometry.invokeMember("setAttribute", "normal", normAttr);
        }
        
        AccessorModel uvAccessor = attributes.get("TEXCOORD_0");
        if (uvAccessor != null) {
            Value jsUvs = Float32Array.newInstance(getFloatArray(uvAccessor));
            Value uvAttr = threeJS.getMember("Float32BufferAttribute").newInstance(jsUvs, 2);
            geometry.invokeMember("setAttribute", "uv", uvAttr);
            geometry.invokeMember("setAttribute", "uv2", uvAttr);
        }
        
        boolean hasVertexColors = false;
        AccessorModel colorAccessor = attributes.get("COLOR_0");
        if (colorAccessor != null) {
            Value jsColors = Float32Array.newInstance(getFloatArray(colorAccessor));
            int itemSize = colorAccessor.getElementType().getNumComponents();
            Value colorAttr = threeJS.getMember("Float32BufferAttribute").newInstance(jsColors, itemSize);
            geometry.invokeMember("setAttribute", "color", colorAttr);
            hasVertexColors = true;
        }
        
        AccessorModel indicesAccessor = primitive.getIndices();
        if (indicesAccessor != null) {
            int[] indices = getIntArray(indicesAccessor);
            int highestValue = Arrays.stream(indices).max().orElse(0);
            Value typedArray = (highestValue > 65535 ? Uint32Array : Uint16Array).newInstance(indices);
            Value indexAttr = threeJS.getMember("BufferAttribute").newInstance(typedArray, 1);
            geometry.invokeMember("setIndex", indexAttr);
        }
        
        geometry.invokeMember("computeBoundingSphere");
        geometry.invokeMember("computeBoundingBox");
        
        Value mesh = threeJS.getMember("Mesh").newInstance(geometry, createMaterial(primitive.getMaterialModel(), gltfModel, hasVertexColors));
        if (meshModel.getName() != null) mesh.putMember("name", meshModel.getName());
        
        return mesh;
    }
    
    private GltfModel loadGLB(String rel) throws Exception {
        try (java.io.InputStream is = getClass().getResourceAsStream("/assets/" + rel)) {
            if (is == null) throw new RuntimeException("GLB not found: /assets/" + rel);
            
            RawGltfData rawData = RawGltfDataReader.read(is);
            ByteBuffer jsonBuffer = rawData.getJsonData();
            byte[] jsonBytes = new byte[jsonBuffer.remaining()];
            jsonBuffer.get(jsonBytes);
            
            GlTF gltf = new ObjectMapper().readValue(jsonBytes, GlTF.class);
            ByteBuffer binaryData = rawData.getBinaryData();
            if (binaryData != null) binaryData.rewind();
            
            return GltfModels.create(new GltfAssetV2(gltf, binaryData));
        }
    }
    
    private Value createMaterial(MaterialModel materialModel, GltfModel gltfModel, boolean hasVertexColors) {
        Value materialOptions = Object.newInstance();
        materialOptions.putMember("color", 0xffffff);
        materialOptions.putMember("metalness", 1.0);
        materialOptions.putMember("roughness", 1.0);
        materialOptions.putMember("side", 2);
        if (hasVertexColors) materialOptions.putMember("vertexColors", true);
        
        if (materialModel != null) loadMaterialTextures(materialOptions, materialModel, gltfModel);
        
        return threeJS.getMember("MeshStandardMaterial").newInstance(materialOptions);
    }
    
    private void loadMaterialTextures(Value materialOptions, MaterialModel materialModel, GltfModel gltfModel) {
        if (!(materialModel instanceof MaterialModelV2)) return;
        
        MaterialModelV2 mat = (MaterialModelV2) materialModel;
        
        float[] baseColorFactor = mat.getBaseColorFactor();
        if (baseColorFactor != null && baseColorFactor.length >= 3) {
            int color = ((int)(baseColorFactor[0] * 255) << 16) | 
                       ((int)(baseColorFactor[1] * 255) << 8) | 
                       (int)(baseColorFactor[2] * 255);
            materialOptions.putMember("color", color);
            if (baseColorFactor.length >= 4 && baseColorFactor[3] < 1.0f) {
                materialOptions.putMember("opacity", (double)baseColorFactor[3]);
                materialOptions.putMember("transparent", true);
            }
        }
        
        Value baseColor = loadTexture(mat.getBaseColorTexture(), gltfModel, true);
        if (baseColor != null) materialOptions.putMember("map", baseColor);
        
        Value metallicRoughness = loadTexture(mat.getMetallicRoughnessTexture(), gltfModel, false);
        if (metallicRoughness != null) {
            materialOptions.putMember("metalnessMap", metallicRoughness);
            materialOptions.putMember("roughnessMap", metallicRoughness);
        }
        
        Value normal = loadTexture(mat.getNormalTexture(), gltfModel, false);
        if (normal != null) materialOptions.putMember("normalMap", normal);
        
        Value occlusion = loadTexture(mat.getOcclusionTexture(), gltfModel, false);
        if (occlusion != null) materialOptions.putMember("aoMap", occlusion);
        
        Value emissive = loadTexture(mat.getEmissiveTexture(), gltfModel, true);
        if (emissive != null) {
            materialOptions.putMember("emissiveMap", emissive);
            materialOptions.putMember("emissive", 0xffffff);
        }
        
        Float metalness = mat.getMetallicFactor();
        Float roughness = mat.getRoughnessFactor();
        if (metalness != null) materialOptions.putMember("metalness", metalness.doubleValue());
        if (roughness != null) materialOptions.putMember("roughness", roughness.doubleValue());
    }
    
    private Value loadTexture(TextureModel textureModel, GltfModel gltfModel, boolean sRGB) {
        if (textureModel == null) return null;
        
        ImageModel imageModel = textureModel.getImageModel();
        if (imageModel == null) return null;
        
        ByteBuffer imageData = imageModel.getImageData();
        if (imageData == null || imageData.remaining() == 0) return null;
        
        byte[] imageBytes = new byte[imageData.remaining()];
        imageData.get(imageBytes);
        imageData.rewind();
        
        Value texture = textureLoader.createDataTextureFromImageBytes(imageBytes);
        if (texture != null) configureTexture(texture, sRGB);
        
        return texture;
    }
    
    private void configureTexture(Value texture, boolean sRGB) {
        texture.putMember("colorSpace", threeJS.getMember(sRGB ? "SRGBColorSpace" : "LinearSRGBColorSpace"));
        texture.putMember("wrapS", threeJS.getMember("RepeatWrapping"));
        texture.putMember("wrapT", threeJS.getMember("RepeatWrapping"));
        texture.putMember("minFilter", threeJS.getMember("LinearFilter"));
        texture.putMember("magFilter", threeJS.getMember("LinearFilter"));
        texture.putMember("flipY", false);
        texture.putMember("generateMipmaps", false);
        texture.putMember("needsUpdate", true);
    }
    
    private float[] getFloatArray(AccessorModel accessor) {
        AccessorFloatData afd = AccessorDatas.createFloat(accessor);
        int elements = afd.getNumElements();
        int comps = afd.getNumComponentsPerElement();
        float[] out = new float[elements * comps];
        int idx = 0;
        for (int e = 0; e < elements; e++) {
            for (int c = 0; c < comps; c++) {
                out[idx++] = afd.get(e, c);
            }
        }
        return out;
    }
    
    private int[] getIntArray(AccessorModel accessor) {
        ByteBuffer buf = accessor.getAccessorData().createByteBuffer();
        buf.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        int count = accessor.getCount();
        int type = accessor.getComponentType();
        int[] array = new int[count];
        
        if (type == 5123) {
            ShortBuffer sb = buf.asShortBuffer();
            for (int i = 0; i < count; i++) array[i] = sb.get() & 0xFFFF;
        } else if (type == 5125) {
            buf.asIntBuffer().get(array);
        } else if (type == 5121) {
            for (int i = 0; i < count; i++) array[i] = buf.get() & 0xFF;
        }
        
        return array;
    }
}
