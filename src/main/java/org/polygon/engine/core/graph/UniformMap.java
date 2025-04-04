package org.polygon.engine.core.graph;

import org.joml.*;
import org.lwjgl.system.MemoryStack;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL40.*;

// This class will create uniform references and sets up their values
public class UniformMap {
    private int programId;
    private Map<String, Integer> uniformReferences;

    public UniformMap(int programId) {
        // Initialize a uniform references map and store the received programId
        this.programId = programId;
        uniformReferences = new HashMap<>();
    }

    // Create a uniform reference in the uniformReferences map
    public void createUniform(String uniformName) {
        // If that uniform is found in the shader it will get stored in the uniformMap
        int uniformLocation = glGetUniformLocation(programId, uniformName);
        if(uniformLocation < 0) {
            throw new RuntimeException("Could not find uniform [" + uniformName + "] in shader program ["
                    + programId + "]");
        }
        uniformReferences.put(uniformName, uniformLocation);
    }

    // Get uniform location for setUniform methods
    private Integer getUniformLocation(String uniformName) {
        Integer location = uniformReferences.get(uniformName);
        if(location == null) {
            throw new RuntimeException("Could not set value to uniform ["
                    + uniformName + "], Uniform not found!");
        }

        return location;
    }

    // set's the uniform reference with value.
    public void setUniform(String uniformName, Matrix4f value) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(getUniformLocation(uniformName), false, value.get(stack.mallocFloat(16)));
        }
    }

    public void setUniform(String uniformName, boolean value) {
        if(value == true) {
            setUniform(uniformName, 1);
        } else {
            setUniform(uniformName, 0);
        }
    }
    public void setUniform(String uniformName, int value) {
        glUniform1i(getUniformLocation(uniformName), value);
    }

    public void setUniform(String uniformName, float value) {
        glUniform1f(getUniformLocation(uniformName), value);
    }

    public void setUniform(String uniformName, Vector4f value) {
        glUniform4f(getUniformLocation(uniformName), value.x, value.y, value.z, value.w);
    }
    public void setUniform(String uniformName, Vector3f value) {
        glUniform3f(getUniformLocation(uniformName), value.x, value.y, value.z);
    }

    public void setUniform(String uniformName, Vector2f value) {
        glUniform2f(getUniformLocation(uniformName), value.x, value.y);
    }
}
