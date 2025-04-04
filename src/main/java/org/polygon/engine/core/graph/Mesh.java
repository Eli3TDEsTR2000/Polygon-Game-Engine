package org.polygon.engine.core.graph;

import org.lwjgl.opengl.GL40;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class Mesh {
    private int vaoId;
    private List<Integer> vboIdList;
    private int numVertices;

    public Mesh(float[] positions, float[] normals, float[] tangents, float[] bitangents
            , float[] textCoords, int[] indexArray) {
        // Create a temp vboId reference
        // We don't need to store individual vbo references, instead we have an arrayList to store instead
        int vboId;
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
        FloatBuffer positionsBuffer = MemoryUtil.memCallocFloat(positions.length);
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

        // Create Normals VBO reference
        vboId = glGenBuffers();
        vboIdList.add(vboId);
        FloatBuffer normalsBuffer = MemoryUtil.memCallocFloat(normals.length);
        normalsBuffer.put(0, normals);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, normalsBuffer, GL_STATIC_DRAW);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);

        // Create Tangents VBO reference
        vboId = glGenBuffers();
        vboIdList.add(vboId);
        FloatBuffer tangentsBuffer = MemoryUtil.memCallocFloat(tangents.length);
        tangentsBuffer.put(0, tangents);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, tangentsBuffer, GL_STATIC_DRAW);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);

        // Create Bitangents VBO reference
        vboId = glGenBuffers();
        vboIdList.add(vboId);
        FloatBuffer bitangentsBuffer = MemoryUtil.memCallocFloat(bitangents.length);
        bitangentsBuffer.put(0, bitangents);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, bitangentsBuffer, GL_STATIC_DRAW);
        glEnableVertexAttribArray(3);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, 0, 0);


        // Create Texture coordinates VBO reference
        vboId = glGenBuffers();
        vboIdList.add(vboId);
        FloatBuffer textCoordsBuffer = MemoryUtil.memCallocFloat(textCoords.length);
        textCoordsBuffer.put(0, textCoords);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, textCoordsBuffer, GL_STATIC_DRAW);
        // Enable index 2 in the VAO attribute array
        // Texture coordinates are 2D coordinate that takes a size of 2 in the buffer for X and Y
        glEnableVertexAttribArray(4);
        glVertexAttribPointer(4, 2, GL_FLOAT, false, 0, 0);


        // Create Index VBO
        vboId = glGenBuffers();
        vboIdList.add(vboId);
        IntBuffer indexArrayBuffer = MemoryUtil.memCallocInt(indexArray.length);
        indexArrayBuffer.put(0, indexArray);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexArrayBuffer, GL_STATIC_DRAW);


        // Unbind both VAO and VBO
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        // Free the off-heap allocated memory.
        MemoryUtil.memFree(positionsBuffer);
        MemoryUtil.memFree(normalsBuffer);
        MemoryUtil.memFree(tangentsBuffer);
        MemoryUtil.memFree(bitangentsBuffer);
        MemoryUtil.memFree(textCoordsBuffer);
        MemoryUtil.memFree(indexArrayBuffer);
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
