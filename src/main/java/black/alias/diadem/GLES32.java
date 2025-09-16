/*******************************************************************************
 * Copyright 2022 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

 package black.alias.diadem;

 import java.nio.Buffer;
 import java.nio.ByteBuffer;
 import java.nio.FloatBuffer;
 import java.nio.IntBuffer;
 import java.nio.ShortBuffer;
 
 import org.lwjgl.PointerBuffer;
 import org.lwjgl.opengl.GL30;
 import org.lwjgl.opengl.GL31;
 import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
 import org.lwjgl.opengl.GL40;
 import org.lwjgl.opengl.GL43;
 import org.lwjgl.opengl.GL45;
 import org.lwjgl.opengl.GLDebugMessageCallbackI;
 import org.lwjgl.opengl.KHRBlendEquationAdvanced;
 import org.lwjgl.system.MemoryUtil;

import static black.alias.diadem.GLENUMS.*;

 public class GLES32 extends GLES31 {
 
     private static final PointerBuffer pb = PointerBuffer.allocateDirect(16);
 
     public void glBlendBarrier () {
         // when available, this extension is enabled by default.
         // see https://registry.khronos.org/OpenGL/extensions/KHR/KHR_blend_equation_advanced.txt
         KHRBlendEquationAdvanced.glBlendBarrierKHR();
     }
 
     public void glCopyImageSubData (int srcName, int srcTarget, int srcLevel, int srcX, int srcY, int srcZ, int dstName,
         int dstTarget, int dstLevel, int dstX, int dstY, int dstZ, int srcWidth, int srcHeight, int srcDepth) {
         GL43.glCopyImageSubData(srcName, srcTarget, srcLevel, srcX, srcY, srcZ, dstName, dstTarget, dstLevel, dstX, dstY, dstZ,
             srcWidth, srcHeight, srcDepth);
     }
 
     public void glDebugMessageControl (int source, int type, int severity, IntBuffer ids, boolean enabled) {
         GL43.glDebugMessageControl(source, type, severity, ids, enabled);
     }
 
     public void glDebugMessageInsert (int source, int type, int id, int severity, String buf) {
         GL43.glDebugMessageInsert(source, type, id, severity, buf);
     }
 
     public void glDebugMessageCallback () {
        GL43.glDebugMessageCallback(new GLDebugMessageCallbackI() {
            public void invoke (int source, int type, int id, int severity, int length, long message, long userParam) {
                String messageStr = MemoryUtil.memUTF8(message, length);
                String severityStr = getSeverityString(severity);
                String typeStr = getTypeString(type);
                String sourceStr = getSourceString(source);
                
                System.err.println("OpenGL Debug [" + severityStr + "] " + typeStr + " from " + sourceStr + " (ID: " + id + "): " + messageStr);
            }
        }, 0);
    }
    
    private static String getSeverityString(int severity) {
        switch (severity) {
            case 0x9146: return "HIGH";      // GL_DEBUG_SEVERITY_HIGH
            case 0x9147: return "MEDIUM";    // GL_DEBUG_SEVERITY_MEDIUM
            case 0x9148: return "LOW";       // GL_DEBUG_SEVERITY_LOW
            case 0x826B: return "NOTIFICATION"; // GL_DEBUG_SEVERITY_NOTIFICATION
            default: return "UNKNOWN";
        }
    }
    
    private static String getTypeString(int type) {
        switch (type) {
            case 0x824C: return "ERROR";           // GL_DEBUG_TYPE_ERROR
            case 0x824D: return "DEPRECATED";      // GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR
            case 0x824E: return "UNDEFINED";       // GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR
            case 0x824F: return "PORTABILITY";     // GL_DEBUG_TYPE_PORTABILITY
            case 0x8250: return "PERFORMANCE";     // GL_DEBUG_TYPE_PERFORMANCE
            case 0x8251: return "OTHER";           // GL_DEBUG_TYPE_OTHER
            default: return "UNKNOWN";
        }
    }
    
    private static String getSourceString(int source) {
        switch (source) {
            case 0x8246: return "API";             // GL_DEBUG_SOURCE_API
            case 0x8247: return "WINDOW_SYSTEM";   // GL_DEBUG_SOURCE_WINDOW_SYSTEM
            case 0x8248: return "SHADER_COMPILER"; // GL_DEBUG_SOURCE_SHADER_COMPILER
            case 0x8249: return "THIRD_PARTY";     // GL_DEBUG_SOURCE_THIRD_PARTY
            case 0x824A: return "APPLICATION";     // GL_DEBUG_SOURCE_APPLICATION
            case 0x824B: return "OTHER";           // GL_DEBUG_SOURCE_OTHER
            default: return "UNKNOWN";
        }
    }
    
    public void glDebugMessageCallbackDisable () {
        GL43.glDebugMessageCallback(null, 0);
    }
 
     public int glGetDebugMessageLog (int count, IntBuffer sources, IntBuffer types, IntBuffer ids, IntBuffer severities,
         IntBuffer lengths, ByteBuffer messageLog) {
         return GL43.glGetDebugMessageLog(count, sources, types, ids, severities, lengths, messageLog);
     }
 
     public void glPushDebugGroup (int source, int id, String message) {
         GL43.glPushDebugGroup(source, id, message);
     }
 
     public void glPopDebugGroup () {
         GL43.glPopDebugGroup();
     }
 
     public void glObjectLabel (int identifier, int name, String label) {
         GL43.glObjectLabel(identifier, name, label);
     }
 
     public String glGetObjectLabel (int identifier, int name) {
         return GL43.glGetObjectLabel(identifier, name);
     }
 
     public long glGetPointerv (int pname) {
         pb.reset();
         GL43.glGetPointerv(pname, pb);
         return pb.get();
     }
 
     public void glEnablei (int target, int index) {
         GL30.glEnablei(target, index);
     }
 
     public void glDisablei (int target, int index) {
         GL30.glDisablei(target, index);
     }
 
     public void glBlendEquationi (int buf, int mode) {
         GL40.glBlendEquationi(buf, mode);
     }
 
     public void glBlendEquationSeparatei (int buf, int modeRGB, int modeAlpha) {
         GL40.glBlendEquationSeparatei(buf, modeRGB, modeAlpha);
     }
 
     public void glBlendFunci (int buf, int src, int dst) {
         GL40.glBlendFunci(buf, src, dst);
     }
 
     public void glBlendFuncSeparatei (int buf, int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
         GL40.glBlendFuncSeparatei(buf, srcRGB, dstRGB, srcAlpha, dstAlpha);
     }
 
     public void glColorMaski (int index, boolean r, boolean g, boolean b, boolean a) {
         GL30.glColorMaski(index, r, g, b, a);
     }
 
     public boolean glIsEnabledi (int target, int index) {
         return GL30.glIsEnabledi(target, index);
     }
 
     public void glDrawElementsBaseVertex (int mode, int count, int type, Buffer indices, int basevertex) {
         if (indices instanceof ShortBuffer && type == GL_UNSIGNED_SHORT) {
             ShortBuffer sb = (ShortBuffer)indices;
             int position = sb.position();
             int oldLimit = sb.limit();
             sb.limit(position + count);
             org.lwjgl.opengl.GL32.glDrawElementsBaseVertex(mode, sb, basevertex);
             sb.limit(oldLimit);
         } else if (indices instanceof ByteBuffer && type == GL_UNSIGNED_SHORT) {
             ShortBuffer sb = ((ByteBuffer)indices).asShortBuffer();
             int position = sb.position();
             int oldLimit = sb.limit();
             sb.limit(position + count);
             org.lwjgl.opengl.GL32.glDrawElementsBaseVertex(mode, sb, basevertex);
             sb.limit(oldLimit);
         } else if (indices instanceof ByteBuffer && type == GL_UNSIGNED_BYTE) {
             ByteBuffer bb = (ByteBuffer)indices;
             int position = bb.position();
             int oldLimit = bb.limit();
             bb.limit(position + count);
             GL32.glDrawElementsBaseVertex(mode, bb, basevertex);
             bb.limit(oldLimit);
         } else
             System.err.println("Error: Can't use " + indices.getClass().getName() + " with this method. Use ShortBuffer or ByteBuffer instead.");
     }
 
     public void glDrawRangeElementsBaseVertex (int mode, int start, int end, int count, int type, Buffer indices, int basevertex) {
         if (indices instanceof ShortBuffer && type == GL_UNSIGNED_SHORT) {
             ShortBuffer sb = (ShortBuffer)indices;
             int position = sb.position();
             int oldLimit = sb.limit();
             sb.limit(position + count);
             GL32.glDrawRangeElementsBaseVertex(mode, start, end, sb, basevertex);
             sb.limit(oldLimit);
         } else if (indices instanceof ByteBuffer && type == GL_UNSIGNED_SHORT) {
             ShortBuffer sb = ((ByteBuffer)indices).asShortBuffer();
             int position = sb.position();
             int oldLimit = sb.limit();
             sb.limit(position + count);
             GL32.glDrawRangeElementsBaseVertex(mode, start, end, sb, basevertex);
             sb.limit(oldLimit);
         } else if (indices instanceof ByteBuffer && type == GL_UNSIGNED_BYTE) {
             ByteBuffer bb = (ByteBuffer)indices;
             int position = bb.position();
             int oldLimit = bb.limit();
             bb.limit(position + count);
             GL32.glDrawRangeElementsBaseVertex(mode, start, end, bb, basevertex);
             bb.limit(oldLimit);
         } else
             System.err.println("Error: Can't use " + indices.getClass().getName() + " with this method. Use ShortBuffer or ByteBuffer instead.");
     }
 
     public void glDrawElementsInstancedBaseVertex (int mode, int count, int type, Buffer indices, int instanceCount,
         int basevertex) {
         if (indices instanceof ShortBuffer && type == GL_UNSIGNED_SHORT) {
             ShortBuffer sb = (ShortBuffer)indices;
             int position = sb.position();
             int oldLimit = sb.limit();
             sb.limit(position + count);
             GL32.glDrawElementsInstancedBaseVertex(mode, sb, instanceCount, basevertex);
             sb.limit(oldLimit);
         } else if (indices instanceof ByteBuffer && type == GL_UNSIGNED_SHORT) {
             ShortBuffer sb = ((ByteBuffer)indices).asShortBuffer();
             int position = sb.position();
             int oldLimit = sb.limit();
             sb.limit(position + count);
             GL32.glDrawElementsInstancedBaseVertex(mode, sb, instanceCount, basevertex);
             sb.limit(oldLimit);
         } else if (indices instanceof ByteBuffer && type == GL_UNSIGNED_BYTE) {
             ByteBuffer bb = (ByteBuffer)indices;
             int position = bb.position();
             int oldLimit = bb.limit();
             bb.limit(position + count);
             GL32.glDrawElementsInstancedBaseVertex(mode, bb, instanceCount, basevertex);
             bb.limit(oldLimit);
         } else
             System.err.println("Error: Can't use " + indices.getClass().getName() + " with this method. Use ShortBuffer or ByteBuffer instead.");
     }
 
     public void glDrawElementsInstancedBaseVertex (int mode, int count, int type, int indicesOffset, int instanceCount,
         int basevertex) {
         GL32.glDrawElementsInstancedBaseVertex(mode, count, type, indicesOffset, instanceCount, basevertex);
     }
 
     public void glFramebufferTexture (int target, int attachment, int texture, int level) {
         org.lwjgl.opengl.GL32.glFramebufferTexture(target, attachment, texture, level);
     }
 
     public int glGetGraphicsResetStatus () {
         return GL45.glGetGraphicsResetStatus();
     }
 
     public void glReadnPixels (int x, int y, int width, int height, int format, int type, int bufSize, Buffer data) {
         if (data == null) {
             GL45.glReadnPixels(x, y, width, height, format, type, bufSize, 0L);
         } else {
             int oldLimit = data.limit();
             data.limit(bufSize);
             if (data instanceof ByteBuffer) {
                 GL45.glReadnPixels(x, y, width, height, format, type, (ByteBuffer)data);
             } else if (data instanceof IntBuffer) {
                 GL45.glReadnPixels(x, y, width, height, format, type, (IntBuffer)data);
             } else if (data instanceof ShortBuffer) {
                 GL45.glReadnPixels(x, y, width, height, format, type, (ShortBuffer)data);
             } else if (data instanceof FloatBuffer) {
                 GL45.glReadnPixels(x, y, width, height, format, type, (FloatBuffer)data);
             } else {
                 System.err.println("Error: buffer type not supported");
             }
             data.limit(oldLimit);
         }
     }
 
     public void glGetnUniformfv (int program, int location, FloatBuffer params) {
         GL45.glGetnUniformfv(program, location, params);
     }
 
     public void glGetnUniformiv (int program, int location, IntBuffer params) {
         GL45.glGetnUniformiv(program, location, params);
     }
 
     public void glGetnUniformuiv (int program, int location, IntBuffer params) {
         GL45.glGetnUniformuiv(program, location, params);
     }
 
     public void glMinSampleShading (float value) {
         GL40.glMinSampleShading(value);
     }
 
     public void glPatchParameteri (int pname, int value) {
         GL40.glPatchParameteri(pname, value);
     }
 
     public void glTexParameterIiv (int target, int pname, IntBuffer params) {
         GL30.glTexParameterIiv(target, pname, params);
     }
 
     public void glTexParameterIuiv (int target, int pname, IntBuffer params) {
         GL30.glTexParameterIuiv(target, pname, params);
     }
 
     public void glGetTexParameterIiv (int target, int pname, IntBuffer params) {
         GL30.glGetTexParameterIiv(target, pname, params);
     }
 
     public void glGetTexParameterIuiv (int target, int pname, IntBuffer params) {
         GL30.glGetTexParameterIuiv(target, pname, params);
     }
 
     public void glSamplerParameterIiv (int sampler, int pname, IntBuffer param) {
         GL33.glSamplerParameterIiv(sampler, pname, param);
     }
 
     public void glSamplerParameterIuiv (int sampler, int pname, IntBuffer param) {
         GL33.glSamplerParameterIuiv(sampler, pname, param);
     }
 
     public void glGetSamplerParameterIiv (int sampler, int pname, IntBuffer params) {
         GL33.glGetSamplerParameterIiv(sampler, pname, params);
     }
 
     public void glGetSamplerParameterIuiv (int sampler, int pname, IntBuffer params) {
         GL33.glGetSamplerParameterIuiv(sampler, pname, params);
     }
 
     public void glTexBuffer (int target, int internalformat, int buffer) {
         GL31.glTexBuffer(target, internalformat, buffer);
     }
 
     public void glTexBufferRange (int target, int internalformat, int buffer, int offset, int size) {
         GL43.glTexBufferRange(target, internalformat, buffer, offset, size);
     }
 
     public void glTexStorage3DMultisample (int target, int samples, int internalformat, int width, int height, int depth,
         boolean fixedsamplelocations) {
         GL43.glTexStorage3DMultisample(target, samples, internalformat, width, height, depth, fixedsamplelocations);
     }
 
 }
 