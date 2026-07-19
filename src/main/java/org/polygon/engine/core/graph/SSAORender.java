package org.polygon.engine.core.graph;

import org.joml.Random;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.polygon.engine.core.scene.Scene;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class SSAORender {
    private Vector3f[] kernel;
    private int noiseTextureId;
    private final Vector2f noiseScale;

    private float radius = 0.5f;
    private float bias = 0.025f;

    private ShaderProgram ssaoShaderProgram;
    private UniformMap ssaoUniformMap;

    private ShaderProgram blurShaderProgram;
    private UniformMap blurUniformMap;

    private QuadMesh quadMesh;

    private static final int SSAO_KERNEL_SIZE = 32;
    private static final int SSAO_NOISE_SIZE = 4;

    public SSAORender() {
        List<ShaderProgram.ShaderModuleData> ssaoModuleData = new ArrayList<>();
        ssaoModuleData.add(new ShaderProgram.ShaderModuleData("resources/shaders/lights.vert"
                , GL_VERTEX_SHADER));
        ssaoModuleData.add(new ShaderProgram.ShaderModuleData("resources/shaders/ssao.frag"
                , GL_FRAGMENT_SHADER));
        ssaoShaderProgram = new ShaderProgram(ssaoModuleData);
        createSSAOUniforms();

        List<ShaderProgram.ShaderModuleData> blurModuleData = new ArrayList<>();
        blurModuleData.add(new ShaderProgram.ShaderModuleData("resources/shaders/lights.vert"
                , GL_VERTEX_SHADER));
        blurModuleData.add(new ShaderProgram.ShaderModuleData("resources/shaders/ssao_blur.frag"
                , GL_FRAGMENT_SHADER));
        blurShaderProgram = new ShaderProgram(blurModuleData);
        createBlurUniforms();

        quadMesh = new QuadMesh();
        noiseScale = new Vector2f();

        kernel = generateKernel(SSAO_KERNEL_SIZE);
        noiseTextureId = generateNoiseTexture(SSAO_NOISE_SIZE);

        ssaoShaderProgram.bind();
        ssaoUniformMap.setUniform("samples", kernel);
        ssaoShaderProgram.unbind();
    }

    public void cleanup() {
        glDeleteTextures(noiseTextureId);
        ssaoShaderProgram.cleanup();
        blurShaderProgram.cleanup();
        quadMesh.cleanup();
    }

    private void createSSAOUniforms() {
        ssaoUniformMap = new UniformMap(ssaoShaderProgram.getProgramId());
        ssaoUniformMap.createUniform("depthSampler");
        ssaoUniformMap.createUniform("normalSampler");
        ssaoUniformMap.createUniform("noiseSampler");
        ssaoUniformMap.createUniform("samples");
        ssaoUniformMap.createUniform("projectionMatrix");
        ssaoUniformMap.createUniform("invProjectionMatrix");
        ssaoUniformMap.createUniform("noiseScale");
        ssaoUniformMap.createUniform("radius");
        ssaoUniformMap.createUniform("bias");
    }

    private void createBlurUniforms() {
        blurUniformMap = new UniformMap(blurShaderProgram.getProgramId());
        blurUniformMap.createUniform("ssaoSampler");
    }

    private static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    private Vector3f[] generateKernel(int kernelSize) {
        Random random = new Random();
        Vector3f[] kernel = new Vector3f[kernelSize];

        for(int i = 0; i < kernelSize; i++) {
            Vector3f sample = new Vector3f(
                    random.nextFloat() * 2.0f - 1.0f,   // x in [-1, 1]
                    random.nextFloat() * 2.0f - 1.0f,      // y in [-1, 1]
                    random.nextFloat()                     // z in [0, 1] -> hemisphere not full sphere
            );
            // to push it onto the hemisphere's outer shell.
            sample.normalize();
            // then pull it back inward to a random depth to fill the volume not just the surface of the hemisphere
            sample.mul(random.nextFloat());

            // This is to bias the samples toward the origin so more is close to the fragment for fine detail
            // few further away from the fragment for broad detail
            float scale = (float) i / kernelSize;
            scale = lerp(0.1f, 1.0f, scale * scale);
            sample.mul(scale);

            kernel[i] = sample;
        }

        return kernel;
    }

    private int generateNoiseTexture(int noiseSize) {
        int totalTexels = noiseSize * noiseSize;
        Random random = new Random();
        int textureId;

        try(MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer noiseData = stack.mallocFloat(totalTexels * 3);
            for(int i = 0; i < totalTexels; i++) {
                noiseData.put(random.nextFloat() * 2.0f - 1.0f);
                noiseData.put(random.nextFloat() * 2.0f - 1.0f);
                noiseData.put(0.0f); // we only rotate around tangent-space normal (z);
            }
            noiseData.flip();

            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F, noiseSize, noiseSize, 0, GL_RGB, GL_FLOAT, noiseData);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            // repeat the texel tiles across the entire screen
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

            glBindTexture(GL_TEXTURE_2D, 0);
        }

        return textureId;
    }

    public void render(Scene scene, GBuffer gBuffer, SSAOBuffer ssaoBuffer) {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);

        // PASS 1 : raw ssao
        glBindFramebuffer(GL_FRAMEBUFFER, ssaoBuffer.getSSAOFramebufferId());
        glViewport(0, 0, ssaoBuffer.getWidth(), ssaoBuffer.getHeight());
        glClear(GL_COLOR_BUFFER_BIT);

        ssaoShaderProgram.bind();

        int[] textureIds = gBuffer.getTextureIds();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureIds[GBuffer.GBUFFER_IDX_DEPTH]);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, textureIds[GBuffer.GBUFFER_IDX_NORMAL]);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, noiseTextureId);

        ssaoUniformMap.setUniform("depthSampler", 0);
        ssaoUniformMap.setUniform("normalSampler", 1);
        ssaoUniformMap.setUniform("noiseSampler", 2);

        ssaoUniformMap.setUniform("projectionMatrix", scene.getProjection().getProjMatrix());
        ssaoUniformMap.setUniform("invProjectionMatrix", scene.getProjection().getInvProjMatrix());

        noiseScale.set((float) gBuffer.getWidth() / SSAO_NOISE_SIZE, (float) gBuffer.getHeight() / SSAO_NOISE_SIZE);
        ssaoUniformMap.setUniform("noiseScale", noiseScale);
        ssaoUniformMap.setUniform("radius", radius);
        ssaoUniformMap.setUniform("bias", bias);

        glBindVertexArray(quadMesh.getVaoId());
        glDrawElements(GL_TRIANGLES, quadMesh.getNumVertices(), GL_UNSIGNED_INT, 0);

        ssaoShaderProgram.unbind();

        // PASS 2 : blur. Note: same quad is still in bind mode no need to rebind the quad mesh again.
        glBindFramebuffer(GL_FRAMEBUFFER, ssaoBuffer.getBlurFramebufferId());
        glClear(GL_COLOR_BUFFER_BIT);

        blurShaderProgram.bind();

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, ssaoBuffer.getSSAOTextureId());
        blurUniformMap.setUniform("ssaoSampler", 0);

        glDrawElements(GL_TRIANGLES, quadMesh.getNumVertices(), GL_UNSIGNED_INT, 0);

        glBindVertexArray(0);
        blurShaderProgram.unbind();

        glEnable(GL_DEPTH_TEST);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
}
