package org.polygon.engine.core.graph;

import imgui.ImDrawData;

import static org.lwjgl.opengl.GL40.*;

public class GuiMesh {
    private int vaoId;
    private int verticesVBO;
    private int indexArrayVBO;

    public GuiMesh() {
        // Create and hold reference to the vertex array object created for rendering ImGui.
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Create and hold reference to the vertices VBO.
        // pointer 0 will point to the vertices positions.
        // pointer 8 will point to the vertices texture coordinates.
        // pointer 16 will point to the color.
        // The data for the VBO are set in the GuiRender.
        verticesVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, verticesVBO);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, ImDrawData.sizeOfImDrawVert(), 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, ImDrawData.sizeOfImDrawVert(), 8);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 4, GL_UNSIGNED_BYTE, true
                , ImDrawData.sizeOfImDrawVert(), 16);

        // Generate a buffer for the index array, the data is set in the GuiRender.
        indexArrayVBO = glGenBuffers();

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteBuffers(verticesVBO);
        glDeleteBuffers(indexArrayVBO);
        glDeleteVertexArrays(vaoId);
    }

    public int getVaoId() {
        return vaoId;
    }

    public int getVerticesVBO() {
        return verticesVBO;
    }

    public int getIndexArrayVBO() {
        return indexArrayVBO;
    }
}
