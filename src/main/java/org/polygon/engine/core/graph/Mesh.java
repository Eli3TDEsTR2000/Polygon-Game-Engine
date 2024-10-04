package org.polygon.engine.core.graph;

import org.lwjgl.opengl.GL40;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class Mesh {
    private int vaoId;
    private List<Integer> vboIdList;
    private int numVertices;

    public Mesh(float[] positions, int numVertices) {
        // Creating off-heap memory to be accessed by OpenGL native API
        // This will get cleaned up automatically with try-with-resource after the try scope ends
        try(MemoryStack stack = MemoryStack.stackPush()) {
            // Track number of vertices for draw calls later
            // Initializing VBO collections array
            this.numVertices = numVertices;
            vboIdList = new ArrayList<>();

            // Creates a Vertex Array Object and binds it
            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            // Create Positions VBO reference
            int vboId = glGenBuffers();
            // Add it to the VBO reference list
            vboIdList.add(vboId);
            // Create an off-heap float array and put the passed positions array inside it
            // to be accessed by OpenGl native API
            FloatBuffer positionsBuffer = stack.callocFloat(positions.length);
            positionsBuffer.put(0, positions);
            // First we bind that VBO reference
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            // Make that reference point to a positions array
            glBufferData(GL_ARRAY_BUFFER, positionsBuffer, GL_STATIC_DRAW);
            // Enable index 0 in the VAO attribute array, index 0 is positions in shader
            glEnableVertexAttribArray(0);
            // create the index 0 attribute pointer
            // 0 is index, 3 is the size of each vertex information
            // this vertex information is stored in float
            // normalized is false for legacy OpenGL support
            // no strides as positions array only contains positions data for each vertex
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

            // Unbind both VAO and VBO
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
        }
    }

    public void cleanup() {
        vboIdList.forEach(GL40::glDeleteBuffers);
        glDeleteVertexArrays(vaoId);
    }

    public int getNumVertices() {
        return numVertices;
    }

    public int getVaoId() {
        return vaoId;
    }
}
