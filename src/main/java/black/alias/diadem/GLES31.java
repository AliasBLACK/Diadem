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

 import java.nio.ByteBuffer;
 import java.nio.FloatBuffer;
 import java.nio.IntBuffer;
 
 import org.lwjgl.opengl.GL11;
 import org.lwjgl.opengl.GL32;
 import org.lwjgl.opengl.GL40;
 import org.lwjgl.opengl.GL41;
 import org.lwjgl.opengl.GL42;
 import org.lwjgl.opengl.GL43;
 import org.lwjgl.opengl.GL46;

import static black.alias.diadem.GLENUMS.*;

 public class GLES31 extends GLES30 {
 
     private final static ByteBuffer tmpByteBuffer = BufferUtils.newByteBuffer(16);
 
     public void glDispatchCompute (int num_groups_x, int num_groups_y, int num_groups_z) {
         GL43.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z);
     }
 
     public void glDispatchComputeIndirect (long indirect) {
         GL43.glDispatchComputeIndirect(indirect);
     }
 
     public void glDrawArraysIndirect (int mode, long indirect) {
         GL40.glDrawArraysIndirect(mode, indirect);
     }
 
     public void glDrawElementsIndirect (int mode, int type, long indirect) {
         GL40.glDrawElementsIndirect(mode, type, indirect);
     }
 
     public void glFramebufferParameteri (int target, int pname, int param) {
         GL43.glFramebufferParameteri(target, pname, param);
     }
 
     public void glGetFramebufferParameteriv (int target, int pname, IntBuffer params) {
         GL43.glGetFramebufferParameteriv(target, pname, params);
     }
 
     public void glGetProgramInterfaceiv (int program, int programInterface, int pname, IntBuffer params) {
         GL43.glGetProgramInterfaceiv(program, programInterface, pname, params);
     }
 
     public int glGetProgramResourceIndex (int program, int programInterface, String name) {
         return GL43.glGetProgramResourceIndex(program, programInterface, name);
     }
 
     public String glGetProgramResourceName (int program, int programInterface, int index) {
         return GL43.glGetProgramResourceName(program, programInterface, index);
     }
 
     public void glGetProgramResourceiv (int program, int programInterface, int index, IntBuffer props, IntBuffer length,
         IntBuffer params) {
         GL43.glGetProgramResourceiv(program, programInterface, index, props, length, params);
     }
 
     public int glGetProgramResourceLocation (int program, int programInterface, String name) {
         return GL43.glGetProgramResourceLocation(program, programInterface, name);
     }
 
     public void glUseProgramStages (int pipeline, int stages, int program) {
         GL41.glUseProgramStages(pipeline, stages, program);
     }
 
     public void glActiveShaderProgram (int pipeline, int program) {
         GL41.glActiveShaderProgram(pipeline, program);
     }
 
     public int glCreateShaderProgramv (int type, String[] strings) {
         return GL41.glCreateShaderProgramv(type, strings);
     }
 
     public void glBindProgramPipeline (int pipeline) {
         GL41.glBindProgramPipeline(pipeline);
     }
 
     public void glDeleteProgramPipelines (int count, IntBuffer pipelines) {
         int oldLimit = pipelines.limit();
         pipelines.limit(count);
         GL41.glDeleteProgramPipelines(pipelines);
         pipelines.limit(oldLimit);
     }
 
     public void glGenProgramPipelines (int count, IntBuffer pipelines) {
         int oldLimit = pipelines.limit();
         pipelines.limit(count);
         GL41.glGenProgramPipelines(pipelines);
         pipelines.limit(oldLimit);
     }
 
     public boolean glIsProgramPipeline (int pipeline) {
         return GL41.glIsProgramPipeline(pipeline);
     }
 
     public void glGetProgramPipelineiv (int pipeline, int pname, IntBuffer params) {
         GL41.glGetProgramPipelineiv(pipeline, pname, params);
     }
 
     public void glProgramUniform1i (int program, int location, int v0) {
         GL41.glProgramUniform1i(program, location, v0);
     }
 
     public void glProgramUniform2i (int program, int location, int v0, int v1) {
         GL41.glProgramUniform2i(program, location, v0, v1);
     }
 
     public void glProgramUniform3i (int program, int location, int v0, int v1, int v2) {
         GL41.glProgramUniform3i(program, location, v0, v1, v2);
     }
 
     public void glProgramUniform4i (int program, int location, int v0, int v1, int v2, int v3) {
         GL41.glProgramUniform4i(program, location, v0, v1, v2, v3);
     }
 
     public void glProgramUniform1ui (int program, int location, int v0) {
         GL41.glProgramUniform1ui(program, location, v0);
     }
 
     public void glProgramUniform2ui (int program, int location, int v0, int v1) {
         GL41.glProgramUniform2ui(program, location, v0, v1);
     }
 
     public void glProgramUniform3ui (int program, int location, int v0, int v1, int v2) {
         GL41.glProgramUniform3ui(program, location, v0, v1, v2);
     }
 
     public void glProgramUniform4ui (int program, int location, int v0, int v1, int v2, int v3) {
         GL41.glProgramUniform4ui(program, location, v0, v1, v2, v3);
     }
 
     public void glProgramUniform1f (int program, int location, float v0) {
         GL41.glProgramUniform1f(program, location, v0);
     }
 
     public void glProgramUniform2f (int program, int location, float v0, float v1) {
         GL41.glProgramUniform2f(program, location, v0, v1);
     }
 
     public void glProgramUniform3f (int program, int location, float v0, float v1, float v2) {
         GL41.glProgramUniform3f(program, location, v0, v1, v2);
     }
 
     public void glProgramUniform4f (int program, int location, float v0, float v1, float v2, float v3) {
         GL41.glProgramUniform4f(program, location, v0, v1, v2, v3);
     }
 
     public void glProgramUniform1iv (int program, int location, IntBuffer value) {
         GL41.glProgramUniform1iv(program, location, value);
     }
 
     public void glProgramUniform2iv (int program, int location, IntBuffer value) {
         GL41.glProgramUniform2iv(program, location, value);
     }
 
     public void glProgramUniform3iv (int program, int location, IntBuffer value) {
         GL41.glProgramUniform3iv(program, location, value);
     }
 
     public void glProgramUniform4iv (int program, int location, IntBuffer value) {
         GL41.glProgramUniform4iv(program, location, value);
     }
 
     public void glProgramUniform1uiv (int program, int location, IntBuffer value) {
         GL41.glProgramUniform1uiv(program, location, value);
     }
 
     public void glProgramUniform2uiv (int program, int location, IntBuffer value) {
         GL41.glProgramUniform2uiv(program, location, value);
     }
 
     public void glProgramUniform3uiv (int program, int location, IntBuffer value) {
         GL41.glProgramUniform3uiv(program, location, value);
     }
 
     public void glProgramUniform4uiv (int program, int location, IntBuffer value) {
         GL41.glProgramUniform4uiv(program, location, value);
     }
 
     public void glProgramUniform1fv (int program, int location, FloatBuffer value) {
         GL41.glProgramUniform1fv(program, location, value);
     }
 
     public void glProgramUniform2fv (int program, int location, FloatBuffer value) {
         GL41.glProgramUniform2fv(program, location, value);
     }
 
     public void glProgramUniform3fv (int program, int location, FloatBuffer value) {
         GL41.glProgramUniform3fv(program, location, value);
     }
 
     public void glProgramUniform4fv (int program, int location, FloatBuffer value) {
         GL41.glProgramUniform4fv(program, location, value);
     }
 
     public void glProgramUniformMatrix2fv (int program, int location, boolean transpose, FloatBuffer value) {
         GL41.glProgramUniformMatrix2fv(program, location, transpose, value);
     }
 
     public void glProgramUniformMatrix3fv (int program, int location, boolean transpose, FloatBuffer value) {
         GL41.glProgramUniformMatrix3fv(program, location, transpose, value);
     }
 
     public void glProgramUniformMatrix4fv (int program, int location, boolean transpose, FloatBuffer value) {
         GL41.glProgramUniformMatrix4fv(program, location, transpose, value);
     }
 
     public void glProgramUniformMatrix2x3fv (int program, int location, boolean transpose, FloatBuffer value) {
         GL41.glProgramUniformMatrix2x3fv(program, location, transpose, value);
     }
 
     public void glProgramUniformMatrix3x2fv (int program, int location, boolean transpose, FloatBuffer value) {
         GL41.glProgramUniformMatrix3x2fv(program, location, transpose, value);
     }
 
     public void glProgramUniformMatrix2x4fv (int program, int location, boolean transpose, FloatBuffer value) {
         GL41.glProgramUniformMatrix2x4fv(program, location, transpose, value);
     }
 
     public void glProgramUniformMatrix4x2fv (int program, int location, boolean transpose, FloatBuffer value) {
         GL41.glProgramUniformMatrix4x2fv(program, location, transpose, value);
     }
 
     public void glProgramUniformMatrix3x4fv (int program, int location, boolean transpose, FloatBuffer value) {
         GL41.glProgramUniformMatrix3x4fv(program, location, transpose, value);
     }
 
     public void glProgramUniformMatrix4x3fv (int program, int location, boolean transpose, FloatBuffer value) {
         GL41.glProgramUniformMatrix4x3fv(program, location, transpose, value);
     }
 
     public void glValidateProgramPipeline (int pipeline) {
         GL41.glValidateProgramPipeline(pipeline);
     }
 
     public String glGetProgramPipelineInfoLog (int program) {
         return GL41.glGetProgramPipelineInfoLog(program);
     }
 
     public void glBindImageTexture (int unit, int texture, int level, boolean layered, int layer, int access, int format) {
         GL42.glBindImageTexture(unit, texture, level, layered, layer, access, format);
     }
 
     public void glGetBooleani_v (int target, int index, IntBuffer data) {
         GL46.glGetBooleani_v(target, index, tmpByteBuffer);
         data.put(tmpByteBuffer.asIntBuffer());
     }
 
     public void glMemoryBarrier (int barriers) {
         GL42.glMemoryBarrier(barriers);
     }
 
     public void glMemoryBarrierByRegion (int barriers) {
         GL46.glMemoryBarrierByRegion(barriers);
     }
 
     public void glTexStorage2DMultisample (int target, int samples, int internalformat, int width, int height,
         boolean fixedsamplelocations) {
         GL43.glTexStorage2DMultisample(target, samples, internalformat, width, height, fixedsamplelocations);
     }
 
     public void glGetMultisamplefv (int pname, int index, FloatBuffer val) {
         GL32.glGetMultisamplefv(pname, index, val);
     }
 
     public void glSampleMaski (int maskNumber, int mask) {
         GL32.glSampleMaski(maskNumber, mask);
     }
 
     public void glGetTexLevelParameteriv (int target, int level, int pname, IntBuffer params) {
         GL11.glGetTexLevelParameteriv(target, level, pname, params);
     }
 
     public void glGetTexLevelParameterfv (int target, int level, int pname, FloatBuffer params) {
         GL11.glGetTexLevelParameterfv(target, level, pname, params);
     }
 
     public void glBindVertexBuffer (int bindingindex, int buffer, long offset, int stride) {
         GL43.glBindVertexBuffer(bindingindex, buffer, offset, stride);
     }
 
     public void glVertexAttribFormat (int attribindex, int size, int type, boolean normalized, int relativeoffset) {
         GL43.glVertexAttribFormat(attribindex, size, type, normalized, relativeoffset);
     }
 
     public void glVertexAttribIFormat (int attribindex, int size, int type, int relativeoffset) {
         GL43.glVertexAttribIFormat(attribindex, size, type, relativeoffset);
     }
 
     public void glVertexAttribBinding (int attribindex, int bindingindex) {
         GL43.glVertexAttribBinding(attribindex, bindingindex);
     }
 
     public void glVertexBindingDivisor (int bindingindex, int divisor) {
         GL43.glVertexBindingDivisor(bindingindex, divisor);
     }
 
 }
 