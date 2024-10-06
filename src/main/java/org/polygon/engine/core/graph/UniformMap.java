package org.polygon.engine.core.graph;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL40.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL40.glGetUniformLocation;

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

    // set's the uniform reference with value. TODO LATER - this only supports mat4 uniforms
    public void setUniform(String uniformName, Matrix4f value) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            Integer location = uniformReferences.get(uniformName);
            if(location == null) {
                throw new RuntimeException("Could not set value to uniform ["
                        + uniformName + "], Uniform not found!");
            }
            glUniformMatrix4fv(location, false, value.get(stack.mallocFloat(16)));
        }
    }
}
