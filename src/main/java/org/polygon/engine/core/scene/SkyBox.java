package org.polygon.engine.core.scene;

import org.joml.Matrix4f;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.polygon.engine.core.graph.Mesh;
import org.polygon.engine.core.graph.Model;
import org.polygon.engine.core.graph.ShaderProgram;
import org.polygon.engine.core.graph.TextureCache;
import org.polygon.engine.core.utils.ShapeGenerator;
import org.polygon.engine.core.graph.UniformMap;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.stb.STBImage.stbi_loadf;
import static org.lwjgl.stb.STBImage.stbi_image_free;

public class SkyBox {
    // The class loads the skybox model and hold an entity reference of the model to be rendered.
    private Model skyBoxModel;
    private Entity skyBoxEntity;
    // placeholder for IBL integration
    private IBLData iblData;
    private int environmentMapTextureId = -1;
    private static final int CUBEMAP_RESOLUTION = 1024;
    private static final int IRRADIANCE_MAP_RESOLUTION = 32;
    private static final int PREFILTER_MAP_RESOLUTION = 128;
    private static ShaderProgram equirectangularToCubemapShader;
    private static ShaderProgram irradianceConvolutionShader;
    private static ShaderProgram prefilterShader;
    private static UniformMap equirectangularToCubemapUniformMap;
    private static UniformMap irradianceConvolutionUniformMap;
    private static UniformMap prefilterUniformMap;
    private static Mesh cubeMesh;
    private static int captureFBO = -1, captureRBO = -1;
    private static Matrix4f captureProjection = new Matrix4f().perspective((float) Math.toRadians(90.0f), 1.0f, 0.1f, 10.0f);
    private static Matrix4f[] captureViews = {
            new Matrix4f().lookAt(0.0f, 0.0f, 0.0f,  1.0f,  0.0f,  0.0f, 0.0f, -1.0f,  0.0f),
            new Matrix4f().lookAt(0.0f, 0.0f, 0.0f, -1.0f,  0.0f,  0.0f, 0.0f, -1.0f,  0.0f),
            new Matrix4f().lookAt(0.0f, 0.0f, 0.0f,  0.0f,  1.0f,  0.0f, 0.0f,  0.0f,  1.0f),
            new Matrix4f().lookAt(0.0f, 0.0f, 0.0f,  0.0f, -1.0f,  0.0f, 0.0f,  0.0f, -1.0f),
            new Matrix4f().lookAt(0.0f, 0.0f, 0.0f,  0.0f,  0.0f,  1.0f, 0.0f, -1.0f,  0.0f),
            new Matrix4f().lookAt(0.0f, 0.0f, 0.0f,  0.0f,  0.0f, -1.0f, 0.0f, -1.0f,  0.0f)
    };

    public SkyBox(String skyBoxModelPath, TextureCache textureCache) {
        this.skyBoxModel = ModelLoader.loadModel("skybox-model", skyBoxModelPath, textureCache, false);
        this.skyBoxEntity = new Entity("skybox-entity", this.skyBoxModel.getModelId());
        this.iblData = null;
    }


    // if the user called SkyBox(String environmentMapPath) then they want to set up an HDR for IBL
    public SkyBox(String environmentMapPath, int envMapRes, int irradianceMapRes, int prefilterMapRes) {
        // Create IBLData instance first
        this.iblData = new IBLData(environmentMapPath);
        this.skyBoxModel = null;
        this.skyBoxEntity = null;
        int generatedIrradianceMapId = -1;
        int generatedPrefilterMapId = -1;

        try {
            // Load HDR into environment cubemap
            this.environmentMapTextureId = loadHDRTexture(environmentMapPath, envMapRes > 0 ? envMapRes : CUBEMAP_RESOLUTION);
            
            // Create Irradiance Map from environment map
            if (this.environmentMapTextureId != -1) { 
                generatedIrradianceMapId = createIrradianceMap(this.environmentMapTextureId, 
                        irradianceMapRes > 0 ? irradianceMapRes : IRRADIANCE_MAP_RESOLUTION);
                generatedPrefilterMapId = createPrefilterMap(this.environmentMapTextureId
                        , prefilterMapRes > 0 ? prefilterMapRes : PREFILTER_MAP_RESOLUTION);
                // Store the generated ID in the IBLData object
                this.iblData.setIrradianceMapTextureId(generatedIrradianceMapId);
                this.iblData.setPrefilterMapTextureId(generatedPrefilterMapId);
            } else {
                 this.iblData.setIrradianceMapTextureId(-1);
                 this.iblData.setPrefilterMapTextureId(-1);
            }

        } catch (Exception e) {
            System.err.println("Failed during IBL map generation for: " + environmentMapPath);
            e.printStackTrace();
            // Clean up partially created resources
            if(this.environmentMapTextureId != -1) {
                glDeleteTextures(this.environmentMapTextureId);
            }
            if(generatedIrradianceMapId != -1) {
                glDeleteTextures(generatedIrradianceMapId); // Use local var for cleanup
            }
            if(generatedPrefilterMapId != -1) {
                glDeleteTextures(generatedPrefilterMapId);
            }
            this.environmentMapTextureId = -1;
            // Ensure IBLData reflects failure
            if (this.iblData != null) {
                this.iblData.setIrradianceMapTextureId(-1);
                this.iblData.setPrefilterMapTextureId(-1);
            }
        }
    }

    public IBLData getIBLData() {
        // If this returns a valid IBLData object it means the user used the second SkyBox constructor
        // that imports an .hdr environment map and generated an IBLData object.
        // The returned IBLData will then be used in the lights.frag for diffuse and specular IBL.
        // If it returns null, the user used the first constructor (3D model skybox).
        return iblData;
    }

    public int getEnvironmentMapTextureId() {
        return environmentMapTextureId;
    }

    public Model getSkyBoxModel() {
        return skyBoxModel;
    }

    public Entity getSkyBoxEntity() {
        return skyBoxEntity;
    }

    // Static method to initialize shared resources
    public static void setupIBLResources() {
        // Equirectangular to Cubemap Shader
        if (equirectangularToCubemapShader == null) {
            List<ShaderProgram.ShaderModuleData> shaderModules = new ArrayList<>();
            shaderModules.add(new ShaderProgram.ShaderModuleData("resources/shaders/equirectangular_to_cubemap.vert", GL_VERTEX_SHADER));
            shaderModules.add(new ShaderProgram.ShaderModuleData("resources/shaders/equirectangular_to_cubemap.frag", GL_FRAGMENT_SHADER));
            equirectangularToCubemapShader = new ShaderProgram(shaderModules);
            equirectangularToCubemapUniformMap = new UniformMap(equirectangularToCubemapShader.getProgramId());
            equirectangularToCubemapUniformMap.createUniform("equirectangularMap");
            equirectangularToCubemapUniformMap.createUniform("projection");
            equirectangularToCubemapUniformMap.createUniform("view");
        }
        // Irradiance Convolution Shader
        if (irradianceConvolutionShader == null) {
            List<ShaderProgram.ShaderModuleData> shaderModules = new ArrayList<>();
            shaderModules.add(new ShaderProgram.ShaderModuleData("resources/shaders/irradiance_convolution.vert", GL_VERTEX_SHADER));
            shaderModules.add(new ShaderProgram.ShaderModuleData("resources/shaders/irradiance_convolution.frag", GL_FRAGMENT_SHADER));
            irradianceConvolutionShader = new ShaderProgram(shaderModules);
            irradianceConvolutionUniformMap = new UniformMap(irradianceConvolutionShader.getProgramId());
            irradianceConvolutionUniformMap.createUniform("environmentMap");
            irradianceConvolutionUniformMap.createUniform("projection");
            irradianceConvolutionUniformMap.createUniform("view");
        }
        // prefilterMap Shader
        if (prefilterShader == null) {
            List<ShaderProgram.ShaderModuleData> shaderModules = new ArrayList<>();
            shaderModules.add(new ShaderProgram.ShaderModuleData("resources/shaders/prefilter.vert", GL_VERTEX_SHADER));
            shaderModules.add(new ShaderProgram.ShaderModuleData("resources/shaders/prefilter.frag", GL_FRAGMENT_SHADER));
            prefilterShader = new ShaderProgram(shaderModules);
            prefilterUniformMap = new UniformMap(prefilterShader.getProgramId());
            prefilterUniformMap.createUniform("environmentMap");
            prefilterUniformMap.createUniform("roughness");
            prefilterUniformMap.createUniform("projection");
            prefilterUniformMap.createUniform("view");
        }
        // Shared Mesh and FBO/RBO
        if (cubeMesh == null) {
            cubeMesh = ShapeGenerator.generateCube(); 
        }
        if (captureFBO == -1) {
            captureFBO = glGenFramebuffers();
            captureRBO = glGenRenderbuffers();
        }
    }

    public static void cleanup() {
        if (equirectangularToCubemapShader != null) {
            equirectangularToCubemapShader.cleanup();
            equirectangularToCubemapShader = null;
        }
        if (irradianceConvolutionShader != null) {
            irradianceConvolutionShader.cleanup();
            irradianceConvolutionShader = null;
        }
        if(prefilterShader != null) {
            prefilterShader.cleanup();
            prefilterShader = null;
        }
        if (cubeMesh != null) {
            cubeMesh.cleanup();
            cubeMesh = null;
        }
        if (captureFBO != -1) {
            glDeleteFramebuffers(captureFBO);
            glDeleteRenderbuffers(captureRBO);
            captureFBO = -1;
            captureRBO = -1;
        }
    }

    private int loadHDRTexture(String path, int resolution) throws Exception {
        setupIBLResources(); // Ensure shared resources are ready

        int hdrTexture;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer components = stack.mallocInt(1);

            // Load HDR image data using stb_image
            FloatBuffer hdrImageData = stbi_loadf(path, width, height, components, 3); // Load RGB
            if (hdrImageData == null) {
                throw new Exception("Failed to load HDR image: " + path + " - " + STBImage.stbi_failure_reason());
            }

            // Create a 2D texture from the loaded HDR data
            int hdrTexture2D = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, hdrTexture2D);
            // Use GL_RGB32F for HDR
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB32F, width.get(0), height.get(0), 0, GL_RGB, GL_FLOAT, hdrImageData);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            stbi_image_free(hdrImageData);

            // Create the destination Cubemap texture
            hdrTexture = glGenTextures();
            glBindTexture(GL_TEXTURE_CUBE_MAP, hdrTexture);
            for (int i = 0; i < 6; ++i) {
                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGB32F, resolution, resolution, 0, GL_RGB, GL_FLOAT, (ByteBuffer) null);
            }
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            // Setup Framebuffer
            glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);
            glBindRenderbuffer(GL_RENDERBUFFER, captureRBO); 
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, resolution, resolution); 
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, captureRBO); 

            // Ensure framebuffer is complete
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                glBindFramebuffer(GL_FRAMEBUFFER, 0); 
                throw new IllegalStateException(" SkyBox Framebuffer is not complete!");
            }

            // Render each face of the cubemap
            equirectangularToCubemapShader.bind();
            equirectangularToCubemapUniformMap.setUniform("equirectangularMap", 0);
            equirectangularToCubemapUniformMap.setUniform("projection", captureProjection);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, hdrTexture2D); 

            glViewport(0, 0, resolution, resolution);
            glBindVertexArray(cubeMesh.getVaoId()); 


            for (int i = 0; i < 6; ++i) {
                equirectangularToCubemapUniformMap.setUniform("view", captureViews[i]);
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, hdrTexture, 0);

                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); 
                glDrawElements(GL_TRIANGLES, cubeMesh.getNumVertices(), GL_UNSIGNED_INT, 0);
            }

            glBindVertexArray(0);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            equirectangularToCubemapShader.unbind();

            glDeleteTextures(hdrTexture2D);

            // Mipmaps for the cubemap
            glBindTexture(GL_TEXTURE_CUBE_MAP, hdrTexture);
            glGenerateMipmap(GL_TEXTURE_CUBE_MAP);
            glBindTexture(GL_TEXTURE_CUBE_MAP, 0);

        }

        return hdrTexture;
    }

    // Method to create the irradiance map
    private int createIrradianceMap(int environmentMapId, int resolution) throws Exception {
        setupIBLResources(); 

        int irradianceMap = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, irradianceMap);
        for (int i = 0; i < 6; ++i) {
            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGB16F, resolution, resolution, 0, GL_RGB, GL_FLOAT, (ByteBuffer) null);
        }
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR); 
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);
        glBindRenderbuffer(GL_RENDERBUFFER, captureRBO);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, resolution, resolution);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, captureRBO);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
             throw new Exception("Irradiance Framebuffer is not complete!");
        }
        
        irradianceConvolutionShader.bind();
        irradianceConvolutionUniformMap.setUniform("environmentMap", 0);
        irradianceConvolutionUniformMap.setUniform("projection", captureProjection);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_CUBE_MAP, environmentMapId); 

        glViewport(0, 0, resolution, resolution); 
        glBindVertexArray(cubeMesh.getVaoId());

        glDisable(GL_DEPTH_TEST); 

        for (int i = 0; i < 6; ++i) {
            irradianceConvolutionUniformMap.setUniform("view", captureViews[i]);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, irradianceMap, 0);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); 
            glDrawElements(GL_TRIANGLES, cubeMesh.getNumVertices(), GL_UNSIGNED_INT, 0);
        }

        glEnable(GL_DEPTH_TEST); 

        glBindVertexArray(0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        irradianceConvolutionShader.unbind();
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
        
        return irradianceMap;
    }

    private int createPrefilterMap(int environmentMapId, int resolution){
        setupIBLResources();

        int prefilterMap = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, prefilterMap);
        for (int i = 0; i < 6; ++i)
        {
            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGB16F, resolution, resolution, 0, GL_RGB, GL_FLOAT, (ByteBuffer) null);
        }
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR); // be sure to set minification filter to mip_linear
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        // generate mipmaps for the cubemap so OpenGL automatically allocates the required memory.
        glGenerateMipmap(GL_TEXTURE_CUBE_MAP);

        // pbr: run a quasi monte-carlo simulation on the environment lighting to create a prefilter (cube)map.
        // ----------------------------------------------------------------------------------------------------
        prefilterShader.bind();
        prefilterUniformMap.setUniform("environmentMap", 0);
        prefilterUniformMap.setUniform("projection", captureProjection);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_CUBE_MAP, environmentMapId);

        glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);
        int maxMipLevels = 5;
        for (int mip = 0; mip < maxMipLevels; ++mip)
        {
            // reisze framebuffer according to mip-level size.
            int mipWidth = (int)(resolution * Math.pow(0.5, mip));
            int mipHeight = (int)(resolution * Math.pow(0.5, mip));
            glBindRenderbuffer(GL_RENDERBUFFER, captureRBO);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, mipWidth, mipHeight);
            glViewport(0, 0, mipWidth, mipHeight);

            float roughness = (float)mip / (float)(maxMipLevels - 1);
            prefilterUniformMap.setUniform("roughness", roughness);
            for (int i = 0; i < 6; ++i)
            {
                prefilterUniformMap.setUniform("view", captureViews[i]);
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, prefilterMap, mip);

                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                glBindVertexArray(cubeMesh.getVaoId());
                glDrawElements(GL_TRIANGLES, cubeMesh.getNumVertices(), GL_UNSIGNED_INT, 0);
            }
        }
        glBindVertexArray(0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        prefilterShader.unbind();
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);

        return prefilterMap;
    }
}
