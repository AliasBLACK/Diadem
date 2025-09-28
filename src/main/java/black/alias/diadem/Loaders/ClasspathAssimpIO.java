package black.alias.diadem.Loaders;

import org.lwjgl.assimp.AIFile;
import org.lwjgl.assimp.AIFileIO;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classpath-backed Assimp IO that serves a main GLTF/GLB and any dependent files (bin/textures)
 * from classpath under /assets/<baseDir>/ using AIFileIO callbacks.
 */
public class ClasspathAssimpIO {
    private final String baseDir;           // e.g. "models/duck"
    private final String relMain;           // e.g. "models/duck/scene.gltf" (relative to assets/)
    private final byte[] mainBytes;         // main glTF/glb bytes from classpath
    private final String virtualRoot;       // e.g. "memory:/models/duck/"
    private final String mainVirtualPath;   // e.g. "memory:/models/duck/scene.gltf"

    // Track per-file ByteBuffers so we can free them in CloseProc
    private final Map<Long, ByteBuffer> fileBuffers = new ConcurrentHashMap<>();

    public ClasspathAssimpIO(String baseDir, String relMain, byte[] mainBytes) {
        this.baseDir = baseDir == null ? "" : baseDir;
        this.relMain = relMain;
        this.mainBytes = mainBytes;
        this.virtualRoot = "memory:/" + (this.baseDir.isEmpty() ? "" : (this.baseDir + "/"));
        String mainName;
        int slash = relMain.lastIndexOf('/');
        mainName = (slash >= 0) ? relMain.substring(slash + 1) : relMain;
        this.mainVirtualPath = virtualRoot + mainName;
    }

    public String getMainVirtualPath() { return mainVirtualPath; }

    public AIFileIO get() {
        AIFileIO io = AIFileIO.calloc();
        io.OpenProc((_, fileNamePtr, _) -> {
            String req = MemoryUtil.memUTF8(fileNamePtr);
            String norm = req.replace('\\', '/');

            // Map requested path to an asset-relative path (under assets/)
            String assetRel = norm.startsWith("memory:/") ? norm.substring("memory:/".length()) :
                              (norm.startsWith("/") ? norm.substring(1) : norm);
            if (assetRel.startsWith("assets/")) assetRel = assetRel.substring("assets/".length());

            // Handle ./ and ../ relative segments against baseDir
            String relPath = assetRel;
            if (relPath.startsWith("./")) relPath = relPath.substring(2);
            String base = baseDir == null ? "" : baseDir;
            while (relPath.startsWith("../")) {
                relPath = relPath.substring(3);
                int cut = base.lastIndexOf('/');
                base = (cut >= 0) ? base.substring(0, cut) : "";
            }

            // If request already contains base prefix, don't prepend again
            if (!base.isEmpty() && !(relPath.startsWith(base + "/") || relPath.equals(base))) {
                relPath = base + "/" + relPath;
            }
            assetRel = relPath;

            byte[] content;
            try {
                if (norm.equals(mainVirtualPath) || assetRel.equals(relMain)) {
                    content = mainBytes;
                } else {
                    try (InputStream ris = ClasspathAssimpIO.class.getResourceAsStream("/assets/" + assetRel)) {
                        if (ris == null) return 0L;
                        content = ris.readAllBytes();
                    }
                }
            } catch (IOException e) {
                return 0L;
            }

            final ByteBuffer data = MemoryUtil.memAlloc(content.length);
            data.put(content).flip();

            final long[] pos = new long[]{0L};
            AIFile f = AIFile.calloc();
            f.ReadProc((_, pBuffer, size, count) -> {
                long max = size * count;
                long remaining = data.remaining();
                long toRead = Math.min(remaining, max);
                if (toRead <= 0) return 0;
                MemoryUtil.memCopy(MemoryUtil.memAddress(data) + pos[0], pBuffer, toRead);
                pos[0] += toRead;
                data.position((int) pos[0]);
                return toRead / size;
            });
            f.SeekProc((_, offset, origin) -> {
                long newPos = pos[0];
                switch ((int) origin) {
                    case Assimp.aiOrigin_SET: newPos = offset; break;
                    case Assimp.aiOrigin_CUR: newPos = pos[0] + offset; break;
                    case Assimp.aiOrigin_END: newPos = data.capacity() + offset; break;
                    default: break;
                }
                if (newPos < 0) newPos = 0;
                if (newPos > data.capacity()) newPos = data.capacity();
                pos[0] = newPos;
                data.position((int) newPos);
                return 0;
            });
            f.FileSizeProc((_) -> data.capacity());
            f.TellProc((_) -> pos[0]);

            long addr = f.address();
            fileBuffers.put(addr, data);
            return addr;
        });

        io.CloseProc((_, pFile) -> {
            if (pFile != 0L) {
                AIFile.create(pFile).free();
                ByteBuffer buf = fileBuffers.remove(pFile);
                if (buf != null) MemoryUtil.memFree(buf);
            }
        });

        return io;
    }
}
