package black.alias.diadem.Loaders;
import org.lwjgl.assimp.*;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.Context;
import org.lwjgl.PointerBuffer;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.lwjgl.assimp.Assimp.*;
public class ModelLoader {
    
    private final Value threeJS;
    private final TextureLoader textureLoader;
    private String modelBaseDir = "";
    private AIPropertyStore store = aiCreatePropertyStore();
    private final Map<String, Value> nameToObject = new HashMap<>();

    private Value Array, Object;

    public ModelLoader(Context jsContext, Value threeJS, TextureLoader textureLoader) {
        this.threeJS = threeJS;
        this.textureLoader = textureLoader;

        // Cache JS types
        this.Array = jsContext.eval("js", "Array");
        this.Object = jsContext.eval("js", "Object");

        // Configure Assimp
        aiSetImportPropertyInteger(store, AI_CONFIG_IMPORT_FBX_EMBEDDED_TEXTURES_LEGACY_NAMING, 1);
    }

    // Convert Assimp AIMatrix4x4 to Three.js Matrix4
    private Value toThreeMatrix(AIMatrix4x4 m) {
        Value Matrix4 = threeJS.getMember("Matrix4");
        Value mat = Matrix4.newInstance();
        mat.invokeMember("set",
            (double)m.a1(), (double)m.a2(), (double)m.a3(), (double)m.a4(),
            (double)m.b1(), (double)m.b2(), (double)m.b3(), (double)m.b4(),
            (double)m.c1(), (double)m.c2(), (double)m.c3(), (double)m.c4(),
            (double)m.d1(), (double)m.d2(), (double)m.d3(), (double)m.d4()
        );
        return mat;
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

        Value Group = threeJS.getMember("Group");
        Value rootGroup = Group.newInstance();
        rootGroup.putMember("name", "Scene");
        
        // Pre-scan meshes to collect bone names for building proper Bone nodes
        Map<String, Value> boneOffsets = new HashMap<>();
        parseBones(scene, boneOffsets);
        
        // This map will be populated during hierarchy traversal
        Map<String, Integer> boneNameToIndex = new HashMap<>();

        // Skeletal Animation Setup
        Value bones = Array.newInstance();
        Value skinnedMeshes = Array.newInstance();
        
        AINode rootNode = scene.mRootNode();
        if (rootNode != null) {
            Value nodeObject = buildObjectFromNode(rootNode, scene, bones, boneOffsets, boneNameToIndex, skinnedMeshes);
            if (nodeObject != null) {
                // Add bone hierarchy to scene
                rootGroup.invokeMember("add", nodeObject);
                
                // Update world matrices to establish bone positions
                rootGroup.invokeMember("updateMatrixWorld", true);
                
                // Create skeleton
                Value Skeleton = threeJS.getMember("Skeleton");
                Value skeleton = Skeleton.newInstance(bones, boneOffsets);
                
                for (int i = 0; i < skinnedMeshes.getArraySize(); i++) {
                    Value skinnedMesh = skinnedMeshes.getArrayElement(i);
                    skinnedMesh.invokeMember("bind", skeleton);
                    rootGroup.invokeMember("add", skinnedMesh);
                }
            }
        }
        
        // Create GLTF-compatible result object
        Value result = Object.newInstance();
        
        // Set properties to match GLTFLoader structure
        result.putMember("scene", rootGroup);
        
        // Create scenes array
        Value scenes = Array.newInstance();
        scenes.setArrayElement(0, rootGroup);
        result.putMember("scenes", scenes);
        
        // Animations
        Value animations = buildAnimations(scene);
        result.putMember("animations", animations);
        
        // Cameras
        Value cameras = Array.newInstance();
        result.putMember("cameras", cameras);
        
        // Asset info
        Value asset = Object.newInstance();
        asset.putMember("generator", "LWJGL Assimp Loader");
        asset.putMember("version", "2.0");
        result.putMember("asset", asset);
        
        // User data
        Value userData = Object.newInstance();
        result.putMember("userData", userData);
        
        return result;
    }

    private Value buildObjectFromNode(AINode node, AIScene scene, Value bones, Map<String, Value> offsets, Map<String, Integer> boneNameToIndex, Value skinnedMeshes) {
        if (node == null) return null;
        Value obj;
        String name = node.mName().dataString();

        // If is bone
        if (name != null && !name.isEmpty() && offsets.containsKey(name)) {
            Value Bone = threeJS.getMember("Bone");
            obj = Bone.newInstance();

            // Push bone to array and record its index
            int boneIndex = (int) bones.getArraySize();
            boneNameToIndex.put(name, boneIndex);
            bones.invokeMember("push", obj);

        // If is not bone
        } else {
            Value Group = threeJS.getMember("Group");
            obj = Group.newInstance();
        }

        // Set name
        if (name != null && !name.isEmpty()) {
            obj.putMember("name", name);
            nameToObject.put(name, obj);
        }

        // Apply node's local transformation to ALL nodes
        AIMatrix4x4 transform = node.mTransformation();
        Value matrix = toThreeMatrix(transform);
        
        // Decompose to populate position/quaternion/scale
        Value position = obj.getMember("position");
        Value quaternion = obj.getMember("quaternion");
        Value scale = obj.getMember("scale");
        matrix.invokeMember("decompose", position, quaternion, scale);

        // Attach meshes for this node
        int numNodeMeshes = node.mNumMeshes();
        if (numNodeMeshes > 0) {
            java.nio.IntBuffer meshIdxBuf = node.mMeshes();
            for (int i = 0; i < numNodeMeshes; i++) {
                int meshIndex = meshIdxBuf.get(i);
                AIMesh mesh = AIMesh.create(scene.mMeshes().get(meshIndex));
                Value threeMesh = convertMesh(mesh, scene, boneNameToIndex, skinnedMeshes);
                if (threeMesh != null) {
                    boolean isSkinned = threeMesh.hasMember("isSkinnedMesh") && threeMesh.getMember("isSkinnedMesh").asBoolean();
                    if (!isSkinned) {
                        // Non-skinned meshes can be added normally (skinned meshes are collected in convertMesh)
                        obj.invokeMember("add", threeMesh);
                    }
                }
            }
        }

        // Recurse children
        int childCount = node.mNumChildren();
        PointerBuffer children = node.mChildren();
        if (children != null) {
            for (int i = 0; i < childCount; i++) {
                AINode child = AINode.create(children.get(i));
                Value childObj = buildObjectFromNode(child, scene, bones, offsets, boneNameToIndex, skinnedMeshes);
                if (childObj != null) obj.invokeMember("add", childObj);
            }
        }
        return obj;
    }

    // Build AnimationClips from Assimp animations
    private Value buildAnimations(AIScene scene) {
        Value animations = Array.newInstance();
        if (scene.mNumAnimations() == 0) return animations;
        Value VectorKeyframeTrack = threeJS.getMember("VectorKeyframeTrack");
        Value QuaternionKeyframeTrack = threeJS.getMember("QuaternionKeyframeTrack");
        Value AnimationClip = threeJS.getMember("AnimationClip");

        for (int a = 0; a < scene.mNumAnimations(); a++) {
            AIAnimation aiAnim = AIAnimation.create(scene.mAnimations().get(a));
            String animName = aiAnim.mName().dataString();
            if (animName == null || animName.isEmpty()) animName = "Anim_" + a;
            double tps = aiAnim.mTicksPerSecond();
            if (tps == 0.0) tps = 30.0; // default

            // Collect tracks
            Value tracks = Array.newInstance();
            int trackIndex = 0;
            PointerBuffer channels = aiAnim.mChannels();
            if (channels != null) {
                for (int c = 0; c < aiAnim.mNumChannels(); c++) {
                    AINodeAnim ch = AINodeAnim.create(channels.get(c));
                    String nodeName = ch.mNodeName().dataString();
                    if (nodeName == null) continue;

                    // Prefer UUID-based target if available; fallback to normalized name
                    String targetPathPrefix = nodeName;
                    Value targetObj = nameToObject.get(nodeName);
                    if (targetObj != null) {
                        Value uuidVal = targetObj.getMember("uuid");
                        if (uuidVal != null && !uuidVal.isNull())
                            targetPathPrefix = uuidVal.asString();
                    }

                    // Positions
                    AIVectorKey.Buffer posKeys = ch.mPositionKeys();
                    if (posKeys != null && ch.mNumPositionKeys() > 0) {
                        double[] times = new double[ch.mNumPositionKeys()];
                        double[] values = new double[ch.mNumPositionKeys() * 3];

                        for (int i = 0; i < ch.mNumPositionKeys(); i++) {
                            AIVectorKey k = posKeys.get(i);
                            times[i] = k.mTime() / tps;
                            AIVector3D v = k.mValue();
                            values[i * 3]     = v.x();
                            values[i * 3 + 1] = v.y();
                            values[i * 3 + 2] = v.z();
                        }
                        Value track = VectorKeyframeTrack.newInstance(targetPathPrefix + ".position", times, values);
                        tracks.setArrayElement(trackIndex++, track);
                    }

                    // Rotations
                    AIQuatKey.Buffer rotKeys = ch.mRotationKeys();
                    if (rotKeys != null && ch.mNumRotationKeys() > 0) {
                        double[] times = new double[ch.mNumRotationKeys()];
                        double[] values = new double[ch.mNumRotationKeys() * 4];
                        for (int i = 0; i < ch.mNumRotationKeys(); i++) {
                            AIQuatKey k = rotKeys.get(i);
                            times[i] = k.mTime() / tps;
                            AIQuaternion q = k.mValue();
                            values[i * 4] = q.x();
                            values[i * 4 + 1] = q.y();
                            values[i * 4 + 2] = q.z();
                            values[i * 4 + 3] = q.w();
                        }
                        Value track = QuaternionKeyframeTrack.newInstance(targetPathPrefix + ".quaternion", times, values);
                        tracks.setArrayElement(trackIndex++, track);
                    }

                    // Scales
                    AIVectorKey.Buffer sclKeys = ch.mScalingKeys();
                    if (sclKeys != null && ch.mNumScalingKeys() > 0) {
                        double[] times = new double[ch.mNumScalingKeys()];
                        double[] values = new double[ch.mNumScalingKeys() * 3];
                        for (int i = 0; i < ch.mNumScalingKeys(); i++) {
                            AIVectorKey k = sclKeys.get(i);
                            times[i] = k.mTime() / tps;
                            AIVector3D v = k.mValue();
                            values[i * 3] = v.x();
                            values[i * 3 + 1] = v.y();
                            values[i * 3 + 2] = v.z();
                        }
                        Value track = VectorKeyframeTrack.newInstance(targetPathPrefix + ".scale", times, values);
                        tracks.setArrayElement(trackIndex++, track);
                    }
                }
            }

            Value clip = AnimationClip.newInstance(animName, -1, tracks);
            animations.setArrayElement(a, clip);
        }
        return animations;
    }
    
    // Parse bone offsets from mesh data
    private void parseBones(AIScene scene, Map<String, Value> offsets) {
        if (scene == null || scene.mNumMeshes() == 0) return;
        PointerBuffer meshes = scene.mMeshes();
        if (meshes == null) return;
        for (int i = 0; i < scene.mNumMeshes(); i++) {
            AIMesh m = AIMesh.create(meshes.get(i));
            int nb = m.mNumBones();
            if (nb <= 0) continue;
            PointerBuffer bones = m.mBones();
            if (bones == null) continue;
            for (int b = 0; b < nb; b++) {
                AIBone bone = AIBone.create(bones.get(b));
                String n = bone.mName().dataString();
                if (n != null && !n.isEmpty()) {
                    if (!offsets.containsKey(n)) offsets.put(n, toThreeMatrix(bone.mOffsetMatrix()));
                }
            }
        }
    }


    /**
     * Convert Assimp mesh to Three.js Mesh
     */
    private Value convertMesh(AIMesh mesh, AIScene scene, Map<String, Integer> boneNameToIndex, Value skinnedMeshes) {
        // Create BufferGeometry
        Value BufferGeometry = threeJS.getMember("BufferGeometry");
        Value geometry = BufferGeometry.newInstance();
        
        // Extract vertices
        AIVector3D.Buffer vertices = mesh.mVertices();
        int numVertices = mesh.mNumVertices();
        float[] vertexArray = new float[numVertices * 3];
        for (int i = 0; i < numVertices; i++) {
            AIVector3D vertex = vertices.get(i);
            int baseIndex = i * 3;
            vertexArray[baseIndex] = vertex.x();
            vertexArray[baseIndex + 1] = vertex.y();
            vertexArray[baseIndex + 2] = vertex.z();
        }
        
        // Create Three.js Float32BufferAttribute for positions
        Value Float32BufferAttribute = threeJS.getMember("Float32BufferAttribute");
        Value positionAttribute = Float32BufferAttribute.newInstance(vertexArray, 3);
        geometry.invokeMember("setAttribute", "position", positionAttribute);
        
        // Extract normals if available
        AIVector3D.Buffer normals = mesh.mNormals();
        if (normals != null) {
            float[] normalArray = new float[numVertices * 3];
            for (int i = 0; i < numVertices; i++) {
                AIVector3D normal = normals.get(i);
                int baseIndex = i * 3;
                normalArray[baseIndex] = normal.x();
                normalArray[baseIndex + 1] = normal.y();
                normalArray[baseIndex + 2] = normal.z();
            }
            Value normalAttribute = Float32BufferAttribute.newInstance(normalArray, 3);
            geometry.invokeMember("setAttribute", "normal", normalAttribute);
        }

        // Extract vertex colors if available (color set 0)
        boolean hasVertexColors = false;
        AIColor4D.Buffer colors = mesh.mColors(0);
        if (colors != null) {
            float[] colorArray = new float[numVertices * 3];
            for (int i = 0; i < numVertices; i++) {
                AIColor4D c = colors.get(i);
                int baseIndex = i * 3;
                colorArray[baseIndex] = c.r();
                colorArray[baseIndex + 1] = c.g();
                colorArray[baseIndex + 2] = c.b();
            }
            Value colorAttribute = Float32BufferAttribute.newInstance(colorArray, 3);
            geometry.invokeMember("setAttribute", "color", colorAttribute);
            hasVertexColors = true;
        }
        
        // Extract UV coordinates if available
        AIVector3D.Buffer texCoords = mesh.mTextureCoords(0);
        Value uvAttribute = null;
        if (texCoords != null) {
            float[] uvArray = new float[numVertices * 2];
            
            for (int i = 0; i < numVertices; i++) {
                AIVector3D texCoord = texCoords.get(i);
                float u = texCoord.x();
                float v = texCoord.y();
                
                // Normalize V to [0,1]
                if (v > 1.0f) {
                    v = v - 1.0f;
                }
                
                int baseIndex = i * 2;
                uvArray[baseIndex] = u;
                uvArray[baseIndex + 1] = v;
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
        if (hasVertexColors) {
            material.putMember("vertexColors", true);
        }
        assignPBRTextures(material, mesh, scene);

        // If the mesh is skinned, add skin attributes.
        if (mesh.mNumBones() > 0) {
            int vcount = mesh.mNumVertices();
            int[] skinIndex = new int[vcount * 4];
            float[] skinWeight = new float[vcount * 4];
        
            // Fill weights with top-4 influences per vertex
            for (int b = 0; b < mesh.mNumBones(); b++) {
                AIBone aiBone = AIBone.create(mesh.mBones().get(b));
                String boneName = aiBone.mName().dataString();
                
                // Get global bone index from skeleton
                Integer globalBoneIndex = boneNameToIndex.get(boneName);
                if (globalBoneIndex == null) {
                    System.err.println("WARNING: Bone '" + boneName + "' not found in skeleton!");
                    continue; // Skip if bone not in skeleton
                }
                
                AIVertexWeight.Buffer weights = aiBone.mWeights();
                if (weights != null) {
                    for (int w = 0; w < aiBone.mNumWeights(); w++) {
                        AIVertexWeight vw = weights.get(w);
                        int vid = vw.mVertexId();
                        float wt = vw.mWeight();
                        int base = vid * 4;
        
                        // choose slot with smallest weight
                        int minIdx = 0;
                        for (int k = 1; k < 4; k++) {
                            if (skinWeight[base + k] < skinWeight[base + minIdx]) {
                                minIdx = k;
                            }
                        }
        
                        if (wt > skinWeight[base + minIdx]) {
                            skinWeight[base + minIdx] = wt;
                            skinIndex[base + minIdx] = globalBoneIndex; // Use global index
                        }
                    }
                }
            }
        
            // Normalize weights so each vertex sums to 1.0
            for (int v = 0; v < vcount; v++) {
                int base = v * 4;
                float sum = skinWeight[base] + skinWeight[base+1] +
                            skinWeight[base+2] + skinWeight[base+3];
                if (sum > 0.0f) {
                    for (int k = 0; k < 4; k++) {
                        skinWeight[base + k] /= sum;
                    }
                }
            }

            // Attach skin attributes
            Value Uint16BufferAttribute = threeJS.getMember("Uint16BufferAttribute");
            Value skinIndexAttr = Uint16BufferAttribute.newInstance(skinIndex, 4);
            Value skinWeightAttr = Float32BufferAttribute.newInstance(skinWeight, 4);
            geometry.invokeMember("setAttribute", "skinIndex", skinIndexAttr);
            geometry.invokeMember("setAttribute", "skinWeight", skinWeightAttr);

            // Create SkinnedMesh
            material.putMember("skinning", true);
            Value SkinnedMesh = threeJS.getMember("SkinnedMesh");
            Value skinned = SkinnedMesh.newInstance(geometry, material);
            skinned.putMember("name", "SkinnedMesh");

            // Collect the skinned mesh for later binding
            skinnedMeshes.invokeMember("push", skinned);
            return skinned;
        }

        // Non-skinned mesh
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

        // Material base color (set regardless of presence of texture; map multiplies with color)
        AIColor4D baseCol = AIColor4D.create();
        boolean hasBaseColor = aiGetMaterialColor(aiMat, "$clr.base", 0, 0, baseCol) == aiReturn_SUCCESS
            || aiGetMaterialColor(aiMat, "$clr.diffuse", 0, 0, baseCol) == aiReturn_SUCCESS;
        if (hasBaseColor) {
            Value Color = threeJS.getMember("Color");
            if (Color != null) {
                Value matColor = Color.newInstance((double) baseCol.r(), (double) baseCol.g(), (double) baseCol.b());
                material.putMember("color", matColor);
            }
        }

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

    // ref like "*0" or "*0.jpg"; parse consecutive digits after '*'
    private int parseEmbeddedIndex(String ref) {
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

