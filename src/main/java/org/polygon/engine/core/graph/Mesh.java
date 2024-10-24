package org.polygon.engine.core.graph;

import org.lwjgl.opengl.GL40;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class Mesh {
    private int vaoId;
    private List<Integer> vboIdList;
    private int numVertices;

    public Mesh(float[] positions, float[] colors, int[] indexArray) {
        // Create a temp vboId reference
        // We don't need to store individual vbo references, instead we have an arrayList to store instead
        int vboId;
        // Creating off-heap memory to be accessed by OpenGL native API
        // This will get cleaned up automatically with try-with-resource after the try scope ends
        try(MemoryStack stack = MemoryStack.stackPush()) {
            // Track number of vertices for draw calls later
            // Initializing VBO collections array
            this.numVertices = indexArray.length;
            vboIdList = new ArrayList<>();

            // Creates a Vertex Array Object and binds it
            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            // Create Positions VBO reference
            vboId = glGenBuffers();
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
            // Enable index 0 in the VAO attribute array, index 0 is positions of each vertex in vertex shader
            glEnableVertexAttribArray(0);
            // create the index 0 attribute pointer
            // 0 is index, 3 is the size of each vertex information
            // this vertex information is stored in float
            // normalized is false for legacy OpenGL support
            // no strides as positions array only contains positions data for each vertex
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

            // Create Color VBO reference
            vboId = glGenBuffers();
            vboIdList.add(vboId);
            FloatBuffer colorsBuffer = stack.callocFloat(colors.length);
            colorsBuffer.put(0, colors);
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, colorsBuffer, GL_STATIC_DRAW);
            // Enable index 1 in the VAO attribute array, index 1 is colors for each vertex in vertex shader
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);

            // Create Index VBO
            vboId = glGenBuffers();
            vboIdList.add(vboId);
            IntBuffer indexArrayBuffer = stack.callocInt(indexArray.length);
            indexArrayBuffer.put(0, indexArray);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexArrayBuffer, GL_STATIC_DRAW);


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
