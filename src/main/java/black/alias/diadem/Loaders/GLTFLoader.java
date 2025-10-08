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
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GLB loader for embedded binary GLTF files.
 * Supports PBR materials with embedded textures.
 * Note: Does not support Draco compression.
 */
public class GLTFLoader {
    
    private final Value threeJS;
    private final TextureLoader textureLoader;
    private final Value Object;
    private final Value Array;
    private final Value Float32Array;
    private final Value Uint16Array;
    private final Value Uint32Array;
    
    public GLTFLoader(Context jsContext, Value threeJS, TextureLoader textureLoader) {
        this.threeJS = threeJS;
        this.textureLoader = textureLoader;

        // Javascript objects
        this.Object = jsContext.eval("js", "Object");
        this.Array = jsContext.eval("js", "Array");
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

            // Parse skeleton.
            HashMap<NodeModel, List<String>> boneNames = new HashMap<>();
            HashMap<NodeModel, HashMap<String, Value>> boneInverses = new HashMap<>();
            for (SkinModel skin : gltfModel.getSkinModels()) {

                // Get bone names.
                NodeModel root = getSkeletonReference(skin);
                boneNames.put(root, new ArrayList<>());
                boneInverses.put(root, new HashMap<>());
                float[][] inverseBindMatrices = getFloatArray2D(skin.getInverseBindMatrices());
                int index = 0;
                for (NodeModel node : skin.getJoints()) {
                    if (node.getName() != null) {
                        boneNames.get(root).add(node.getName());
                        boneInverses.get(root).put(node.getName(), toThreeMatrix(inverseBindMatrices[index]));
                        index++;
                    }
                }
            }

            // Create bone HashMap.
            HashMap<NodeModel, Value> bones = new HashMap<>();
            for (NodeModel key : boneNames.keySet()) bones.put(key, Array.newInstance());

            // Create skinned groups HashMap.
            HashMap<NodeModel, List<Value>> skinnedGroups = new HashMap<>();
            for (NodeModel key : boneNames.keySet()) skinnedGroups.put(key, new ArrayList<>());

            // Create ordered bone inverses.
            HashMap<NodeModel, Value> orderedBoneInverses = new HashMap<>();
            for (NodeModel key : boneNames.keySet()) orderedBoneInverses.put(key, Array.newInstance());

            // Create bone name to uuid map.
            HashMap<String, String> boneNameToUuid = new HashMap<>();

            // Traverse scenes and build node hierarchy with correct transforms
            Value scenes = Array.newInstance();
            for (SceneModel scene : gltfModel.getSceneModels()) {
                Value rootGroup = threeJS.getMember("Group").newInstance();
                for (NodeModel rootNode : scene.getNodeModels()) {
                    Value node = addNodeRecursive(
                        rootNode,
                        boneNames,
                        boneInverses,
                        bones,
                        skinnedGroups,
                        orderedBoneInverses,
                        boneNameToUuid
                    );
                    rootGroup.invokeMember("add", node);
                }
                rootGroup.invokeMember("updateMatrixWorld", true);
                scenes.invokeMember("push", rootGroup);
            }

            // Create skeletons.
            HashMap<NodeModel, Value> skeletons = new HashMap<>();
            for (NodeModel key : boneNames.keySet()) {
                Value skeleton = threeJS.getMember("Skeleton").newInstance(bones.get(key), orderedBoneInverses.get(key));
                skeletons.put(key, skeleton);
            }

            // Bind skeleton to skinned groups.
            for (NodeModel rootNode : skinnedGroups.keySet()) {
                List<Value> skinnedGroup = skinnedGroups.get(rootNode);
                Value skeleton = skeletons.get(rootNode);
                for (Value skinnedMesh : skinnedGroup) {
                    if (skinnedMesh == null) continue;
                    Value material = skinnedMesh.getMember("material");
                    if (material != null) material.putMember("skinning", true);
                    skinnedMesh.invokeMember("bind", skeleton);
                    if (skinnedMesh.hasMember("normalizeSkinWeights"))
                        skinnedMesh.invokeMember("normalizeSkinWeights");
                }
            }
            
            // Force matrix update after binding
            for (int s = 0; s < scenes.getArraySize(); s++) {
                scenes.getArrayElement(s).invokeMember("updateMatrixWorld", true);
            }
            
            // Build animations (clips)
            Value animations = buildAnimations(gltfModel, boneNameToUuid);

            Value gltfObject = Object.newInstance();
            gltfObject.putMember("scene", scenes.getArrayElement(0));
            gltfObject.putMember("animations", animations);
            gltfObject.putMember("scenes", scenes);
            gltfObject.putMember("cameras", Array.newInstance());
            
            return gltfObject;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load GLB: " + filePath, e);
        }
    }

    private Value addNodeRecursive(
        NodeModel node,
        HashMap<NodeModel, List<String>> boneNames,
        HashMap<NodeModel, HashMap<String, Value>> boneInverses,
        HashMap<NodeModel, Value> bones,
        HashMap<NodeModel, List<Value>> skinnedGroups,
        HashMap<NodeModel, Value> orderedBoneInverses,
        HashMap<String, String> boneNameToUuid
    ) {
        Value obj;

        // If bone, create bone.
        NodeModel boneRoot = getBoneArrayKey(node.getName(), boneNames);
        if (boneRoot != null && node.getName() != null) {
            obj = threeJS.getMember("Bone").newInstance();
            applyNodeTransform(obj, node);
            bones.get(boneRoot).invokeMember("push", obj);
            Value inverse = boneInverses.get(boneRoot).get(node.getName());
            if (inverse == null) {
                System.out.println("Inverse for " + node.getName() + " is null");
                inverse = threeJS.getMember("Matrix4").newInstance();
            }
            orderedBoneInverses.get(boneRoot).invokeMember("push", inverse);
            boneNameToUuid.put(node.getName(), obj.getMember("uuid").asString());
        }

        else {

            List<MeshModel> meshes = node.getMeshModels();

            // If no meshes, create group.
            if (meshes.isEmpty()) {
                obj = threeJS.getMember("Group").newInstance();
                applyNodeTransform(obj, node);
            }

            // If single mesh, create mesh.
            else if (meshes.size() == 1) {
                obj = createMesh(meshes.get(0));
                applyNodeTransform(obj, node);
                if (obj.hasMember("isSkinnedMesh") && obj.getMember("isSkinnedMesh").asBoolean())
                    skinnedGroups.get(getSkeletonReference(node.getSkinModel())).add(obj);
            }

            // If multiple meshes, create group and add meshes.
            else {
                obj = threeJS.getMember("Group").newInstance();
                applyNodeTransform(obj, node);
                for (MeshModel mesh : meshes) {
                    Value meshObj = createMesh(mesh);
                    if (obj.hasMember("isSkinnedMesh") && obj.getMember("isSkinnedMesh").asBoolean())
                        skinnedGroups.get(getSkeletonReference(node.getSkinModel())).add(meshObj);
                    obj.invokeMember("add", meshObj);
                }
            }
        }

        // Set name.
        String name = node.getName();
        if (name == null) name = obj.getMember("type").toString();
        obj.putMember("name", name);

        // Recurse to children.
        for (NodeModel child : node.getChildren()) {
            Value childObj = addNodeRecursive(
                child,
                boneNames,
                boneInverses,
                bones,
                skinnedGroups,
                orderedBoneInverses,
                boneNameToUuid
            );
            obj.invokeMember("add", childObj);
        }
        return obj;
    }

    private void applyNodeTransform(Value obj3d, NodeModel node) {
        float[] m = node.computeLocalTransform(null);
        Value mat = toThreeMatrix(m);
        Value position = obj3d.getMember("position");
        Value quaternion = obj3d.getMember("quaternion");
        Value scale = obj3d.getMember("scale");
        mat.invokeMember("decompose", position, quaternion, scale);
    }

    private Value createMesh(MeshModel meshModel) {
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
        AccessorModel jointsAccessor = attributes.get("JOINTS_0");
        if (jointsAccessor != null) {
            int[] joints = getIntComponentsArray(jointsAccessor);
            Value jsJoints = Uint16Array.newInstance(joints);
            int itemSize = jointsAccessor.getElementType().getNumComponents();
            Value jointsAttr = threeJS.getMember("Uint16BufferAttribute").newInstance(jsJoints, itemSize);
            geometry.invokeMember("setAttribute", "skinIndex", jointsAttr);
        }
        AccessorModel weightsAccessor = attributes.get("WEIGHTS_0");
        if (weightsAccessor != null) {
            float[] weights = getFloatArray(weightsAccessor);
            Value jsWeights = Float32Array.newInstance(weights);
            int itemSize = weightsAccessor.getElementType().getNumComponents();
            Value weightsAttr = threeJS.getMember("Float32BufferAttribute").newInstance(jsWeights, itemSize);
            geometry.invokeMember("setAttribute", "skinWeight", weightsAttr);
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

        // Decide Mesh or SkinnedMesh
        boolean isSkinned = (jointsAccessor != null && weightsAccessor != null);
        Value MeshClass = threeJS.getMember(isSkinned ? "SkinnedMesh" : "Mesh");
        Value mesh = MeshClass.newInstance(geometry, createMaterial(primitive.getMaterialModel(), hasVertexColors));
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
    
    private Value createMaterial(MaterialModel materialModel, boolean hasVertexColors) {
        Value materialOptions = Object.newInstance();
        materialOptions.putMember("color", 0xffffff);
        materialOptions.putMember("metalness", 1.0);
        materialOptions.putMember("roughness", 1.0);
        materialOptions.putMember("side", 2);
        if (hasVertexColors) materialOptions.putMember("vertexColors", true);
        
        if (materialModel != null) loadMaterialTextures(materialOptions, materialModel);
        
        return threeJS.getMember("MeshStandardMaterial").newInstance(materialOptions);
    }
    
    private void loadMaterialTextures(Value materialOptions, MaterialModel materialModel) {
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
        
        Value baseColor = loadTexture(mat.getBaseColorTexture(), true);
        if (baseColor != null) materialOptions.putMember("map", baseColor);
        
        Value metallicRoughness = loadTexture(mat.getMetallicRoughnessTexture(), false);
        if (metallicRoughness != null) {
            materialOptions.putMember("metalnessMap", metallicRoughness);
            materialOptions.putMember("roughnessMap", metallicRoughness);
        }
        
        Value normal = loadTexture(mat.getNormalTexture(), false);
        if (normal != null) materialOptions.putMember("normalMap", normal);
        
        Value occlusion = loadTexture(mat.getOcclusionTexture(), false);
        if (occlusion != null) materialOptions.putMember("aoMap", occlusion);
        
        Value emissive = loadTexture(mat.getEmissiveTexture(), true);
        if (emissive != null) {
            materialOptions.putMember("emissiveMap", emissive);
            materialOptions.putMember("emissive", 0xffffff);
        }
        
        Float metalness = mat.getMetallicFactor();
        Float roughness = mat.getRoughnessFactor();
        if (metalness != null) materialOptions.putMember("metalness", metalness.doubleValue());
        if (roughness != null) materialOptions.putMember("roughness", roughness.doubleValue());
    }
    
    private Value loadTexture(TextureModel textureModel, boolean sRGB) {
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

    /*
     * Returns the skeleton reference node.
     */
    private NodeModel getSkeletonReference(SkinModel skin) {
        NodeModel root = skin.getSkeleton();
        if (root == null) root = skin.getJoints().get(0);
        return root;
    }

    /**
     * Returns the key of the bone with the given name.
     */
    private NodeModel getBoneArrayKey(String boneName, HashMap<NodeModel, List<String>> boneNames) {
        for (NodeModel key : boneNames.keySet()) {
            if (boneNames.get(key).contains(boneName)) return key;
        }
        return null;
    }

    /**
     * Converts a float array to a Three.js Matrix4.
     */
    private Value toThreeMatrix(float[] matrix) {
        Value Matrix4 = threeJS.getMember("Matrix4");
        Value mat = Matrix4.newInstance();
        Value jsArray = Float32Array.newInstance(matrix);
        mat.invokeMember("fromArray", jsArray);
        return mat;
    }
    
    /**
     * Returns accessor data as a 1D float array grouped per element.
     * Shape: [numElements*numComponentsPerElement]
     */
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
    
    /**
     * Returns accessor data as a 2D float array grouped per element.
     * Shape: [numElements][numComponentsPerElement]
     */
    private float[][] getFloatArray2D(AccessorModel accessor) {
        AccessorFloatData afd = AccessorDatas.createFloat(accessor);
        int elements = afd.getNumElements();
        int comps = afd.getNumComponentsPerElement();
        float[][] out = new float[elements][comps];
        for (int e = 0; e < elements; e++) {
            for (int c = 0; c < comps; c++) {
                out[e][c] = afd.get(e, c);
            }
        }
        return out;
    }
    
    /**
     * Returns accessor data as a 1D int array grouped per element.
     * Shape: [numElements]
     */
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

    /**
     * Read vector integer accessor (e.g., JOINTS_0) flattened into count*components length
     */
    private int[] getIntComponentsArray(AccessorModel accessor) {
        ByteBuffer buf = accessor.getAccessorData().createByteBuffer();
        buf.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        int count = accessor.getCount();
        int comps = accessor.getElementType().getNumComponents();
        int type = accessor.getComponentType();
        int total = count * comps;
        int[] out = new int[total];
        if (type == 5125) { // UNSIGNED_INT
            java.nio.IntBuffer ib = buf.asIntBuffer();
            for (int i = 0; i < total; i++) out[i] = ib.get();
        } else if (type == 5123) { // UNSIGNED_SHORT
            ShortBuffer sb = buf.asShortBuffer();
            for (int i = 0; i < total; i++) out[i] = sb.get() & 0xFFFF;
        } else if (type == 5121) { // UNSIGNED_BYTE
            for (int i = 0; i < total; i++) out[i] = buf.get() & 0xFF;
        } else if (type == 5122) { // SHORT
            ShortBuffer sb = buf.asShortBuffer();
            for (int i = 0; i < total; i++) out[i] = sb.get();
        } else if (type == 5120) { // BYTE
            for (int i = 0; i < total; i++) out[i] = buf.get();
        } else {
            // Fallback: try float conversion and cast
            float[] floats = getFloatArray(accessor);
            for (int i = 0; i < total && i < floats.length; i++) out[i] = (int) floats[i];
        }
        return out;
    }

    // Build Three.js AnimationClips from glTF animations
    private Value buildAnimations(GltfModel gltfModel, HashMap<String, String> boneNameToUuid) {
        Value clips = Array.newInstance();
        List<AnimationModel> anims = gltfModel.getAnimationModels();
        if (anims == null || anims.isEmpty()) return clips;

        for (AnimationModel anim : anims) {
            Value tracks = Array.newInstance();
            List<AnimationModel.Channel> channels = anim.getChannels();
            if (channels == null) continue;
            double maxTime = 0.0;

            for (AnimationModel.Channel ch : channels) {
                NodeModel target = ch.getNodeModel();
                if (target == null) continue;
                String nodeName = target.getName();
                if (nodeName == null || nodeName.isEmpty()) continue; // need a target path

                AnimationModel.Sampler sampler = ch.getSampler();
                if (sampler == null) continue;
                AccessorModel input = sampler.getInput();
                AccessorModel output = sampler.getOutput();
                if (input == null || output == null) continue;

                float[] times = getFloatArray(input);
                float[] values = getFloatArray(output);
                Value jsTimes = Float32Array.newInstance(times);
                Value jsValues = Float32Array.newInstance(values);

                String pathKey = ch.getPath();
                if (pathKey != null) pathKey = pathKey.toLowerCase();
                
                switch (pathKey) {
                  case "translation": {
                    if (values.length % 3 != 0) break;
                    String path = boneNameToUuid.get(nodeName) + ".position";
                    Value track = threeJS.getMember("VectorKeyframeTrack").newInstance(path, jsTimes, jsValues);
                    tracks.invokeMember("push", track);
                    break;
                  }
                  case "rotation": {
                    if (values.length % 4 != 0) break;
                    String path = boneNameToUuid.get(nodeName) + ".quaternion";
                    Value track = threeJS.getMember("QuaternionKeyframeTrack").newInstance(path, jsTimes, jsValues);
                    tracks.invokeMember("push", track);
                    break;
                  }
                  case "scale": {
                    if (values.length % 3 != 0) break;
                    String path = boneNameToUuid.get(nodeName) + ".scale";
                    Value track = threeJS.getMember("VectorKeyframeTrack").newInstance(path, jsTimes, jsValues);
                    tracks.invokeMember("push", track);
                    break;
                  }
                  case "weights":
                  default:
                    break;
                }

                maxTime = 0.0;
                if (times.length > 0) {
                double last = times[times.length - 1];
                if (last > maxTime) maxTime = last;
                }
            }

            String name = anim.getName();
            if (name == null || name.isEmpty()) name = "clip";
            Value clip = threeJS.getMember("AnimationClip").newInstance(name, maxTime, tracks);
            clips.invokeMember("push", clip);
        }

        return clips;
    }
}
