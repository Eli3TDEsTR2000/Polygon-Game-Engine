package org.polygon.engine.core.graph;

import org.lwjgl.opengl.GL40;
import org.polygon.engine.core.Utils;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class ShaderProgram {
    private final int programId;

    public ShaderProgram(List<ShaderModuleData> shaderModuleDataList) {
        // Creates a shader program, we keep a reference to that program in programId
        programId = glCreateProgram();
        if(programId == 0) {
            throw new RuntimeException("Couldn't create Shader");
        }

        // Creates a list of shader Ids referencing all compiled shaders
        List<Integer> shaderModuleIdsList = new ArrayList<>();
        shaderModuleDataList.forEach((shaderModule) -> shaderModuleIdsList.add(
                createShader(Utils.readFiles(shaderModule.shaderFilePath), shaderModule.shaderType)));

        // Links all shaders and free them from memory
        link(shaderModuleIdsList);
    }

    public void bind() {
        // Mounts the shader program to use in rendering
        glUseProgram(programId);
    }

    public void unbind() {
        // Remove current shader program object from use in rendering
        glUseProgram(0);
    }

    public void cleanup() {
        // Remove current shader program and deletes it from the GPU
        unbind();
        if(programId != 0) {
            glDeleteProgram(programId);
        }
    }

    public int getProgramId() {
        return programId;
    }

    public void validate() {
        glValidateProgram(programId);
        if(glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            throw new RuntimeException("Error validation shader program: "
                    + glGetProgramInfoLog(programId, 1024));
        }
    }

    protected int createShader(String shaderCode, int shaderType) {
        // Create a shader, we keep a reference for it stored in shaderId
        int shaderId = glCreateShader(shaderType);
        if(shaderId == 0) {
            throw new RuntimeException("Error creating shader. Type: " + shaderType);
        }

        // input a shader code for the created shader and then compile it
        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);

        if(glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Error compiling shader code: "
                    + glGetShaderInfoLog(shaderId, 1024));
        }

        // Attach the compiled shader to the shader program
        glAttachShader(programId, shaderId);

        return shaderId;
    }

    private void link(List<Integer> shaderModuleIdsList) {
        // After Attaching shaders to the shader program, link shader program
        glLinkProgram(programId);
        if(glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Error linking shader code: "
                    + glGetProgramInfoLog(programId, 1024));
        }

        // Detach every shader and deletes them
        shaderModuleIdsList.forEach((shaderId) -> glDetachShader(programId, shaderId));
        shaderModuleIdsList.forEach(GL40::glDeleteShader);
    }
    public record ShaderModuleData(String shaderFilePath, int shaderType) {
    }
}
