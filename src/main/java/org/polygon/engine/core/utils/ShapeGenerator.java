package org.polygon.engine.core.utils;

import org.joml.Vector3f;
import org.polygon.engine.core.graph.Mesh;

import java.util.ArrayList;
import java.util.List;

public class ShapeGenerator {
    public static Mesh generateSphere(float radius, int sectors, int stacks) {
        List<Float> positionsList = new ArrayList<>();
        List<Float> normalsList = new ArrayList<>();
        List<Float> texCoordsList = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();

        float sectorStep = (float) (2 * Math.PI / sectors);
        float stackStep = (float) (Math.PI / stacks);

        for (int i = 0; i <= stacks; ++i) {
            float stackAngle = (float) (Math.PI / 2 - i * stackStep);
            float xy = radius * (float) Math.cos(stackAngle);
            float z = radius * (float) Math.sin(stackAngle);

            for (int j = 0; j <= sectors; ++j) {
                float sectorAngle = j * sectorStep;

                // Vertex position (x, y, z)
                float x = xy * (float) Math.cos(sectorAngle);
                float y = xy * (float) Math.sin(sectorAngle);
                positionsList.add(x);
                positionsList.add(y);
                positionsList.add(z);

                Vector3f normal = new Vector3f(x, y, z).normalize();
                normalsList.add(normal.x);
                normalsList.add(normal.y);
                normalsList.add(normal.z);

                float u = (float) j / sectors;
                float v = (float) i / stacks;
                texCoordsList.add(u);
                texCoordsList.add(v);
            }
        }

        for (int i = 0; i < stacks; ++i) {
            int k1 = i * (sectors + 1);
            int k2 = k1 + sectors + 1;

            for (int j = 0; j < sectors; ++j, ++k1, ++k2) {
                if (i != 0) {
                    indicesList.add(k1);
                    indicesList.add(k2);
                    indicesList.add(k1 + 1);
                }

                if (i != (stacks - 1)) {
                    indicesList.add(k1 + 1);
                    indicesList.add(k2);
                    indicesList.add(k2 + 1);
                }
            }
        }

        float[] positions = toFloatArray(positionsList);
        float[] normals = toFloatArray(normalsList);
        float[] texCoords = toFloatArray(texCoordsList);
        int[] indices = toIntArray(indicesList);

        float[] tangents = new float[0];
        float[] bitangents = new float[0];
        int[] boneIndices = new int[0];
        float[] weights = new float[0];

        Vector3f aabbMin = new Vector3f(-radius, -radius, -radius);
        Vector3f aabbMax = new Vector3f(radius, radius, radius);

        return new Mesh(positions, normals, tangents, bitangents, texCoords, indices, boneIndices
                , weights, aabbMin, aabbMax);
    }

    private static float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
} 