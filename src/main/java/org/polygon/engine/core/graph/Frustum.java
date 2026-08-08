package org.polygon.engine.core.graph;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Frustum {
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private static final int BOTTOM = 2;
    private static final int TOP = 3;
    private static final int NEAR = 4;
    private static final int FAR = 5;

    private final Vector4f[] planes;

    public Frustum() {
        planes = new Vector4f[6];
        for(int i = 0; i < planes.length; i++) {
            planes[i] = new Vector4f();
        }
    }

    private void setPlane(int index, float a, float b, float c, float d) {
        float length = (float) Math.sqrt(a * a + b * b + c * c);
        planes[index].set(a / length, b / length, c / length, d / length);
    }

    public void update(Matrix4f projViewMatrix) {
        // left plane = row3 + row0
        setPlane(LEFT,
                projViewMatrix.m03() + projViewMatrix.m00(),
                projViewMatrix.m13() + projViewMatrix.m10(),
                projViewMatrix.m23() + projViewMatrix.m20(),
                projViewMatrix.m33() + projViewMatrix.m30());

        // right plane = row3 - row0
        setPlane(RIGHT,
                projViewMatrix.m03() - projViewMatrix.m00(),
                projViewMatrix.m13() - projViewMatrix.m10(),
                projViewMatrix.m23() - projViewMatrix.m20(),
                projViewMatrix.m33() - projViewMatrix.m30());

        // bottom plane = row3 + row1
        setPlane(BOTTOM,
                projViewMatrix.m03() + projViewMatrix.m01(),
                projViewMatrix.m13() + projViewMatrix.m11(),
                projViewMatrix.m23() + projViewMatrix.m21(),
                projViewMatrix.m33() + projViewMatrix.m31());

        // top plane = row3 - row1
        setPlane(TOP,
                projViewMatrix.m03() - projViewMatrix.m01(),
                projViewMatrix.m13() - projViewMatrix.m11(),
                projViewMatrix.m23() - projViewMatrix.m21(),
                projViewMatrix.m33() - projViewMatrix.m31());

        // near plane = row3 + row2;
        setPlane(NEAR,
                projViewMatrix.m03() + projViewMatrix.m02(),
                projViewMatrix.m13() + projViewMatrix.m12(),
                projViewMatrix.m23() + projViewMatrix.m22(),
                projViewMatrix.m33() + projViewMatrix.m32());

        // far plane = row3 - row2
        setPlane(FAR,
                projViewMatrix.m03() - projViewMatrix.m02(),
                projViewMatrix.m13() - projViewMatrix.m12(),
                projViewMatrix.m23() - projViewMatrix.m22(),
                projViewMatrix.m33() - projViewMatrix.m32());
    }

    public boolean isBoxVisible(Vector3f localMin, Vector3f localMax, Matrix4f modelMatrix) {
        Vector3f worldMin = new Vector3f();
        Vector3f worldMax = new Vector3f();
        transformAABB(localMin, localMax, modelMatrix, worldMin, worldMax);
        return isAABBVisible(worldMin, worldMax);
    }

    public static void transformAABB(Vector3f localMin, Vector3f localMax, Matrix4f modelMatrix,
                                     Vector3f outMin, Vector3f outMax) {
        Vector3f center = new Vector3f(localMin).add(localMax).mul(0.5f);
        Vector3f extent = new Vector3f(localMax).sub(localMin).mul(0.5f);

        Vector4f worldCenter4 = modelMatrix.transform(new Vector4f(center, 1.0f));

        float newExtentX = Math.abs(modelMatrix.m00()) * extent.x
                + Math.abs(modelMatrix.m10()) * extent.y
                + Math.abs(modelMatrix.m20()) * extent.z;

        float newExtentY = Math.abs(modelMatrix.m01()) * extent.x
                + Math.abs(modelMatrix.m11()) * extent.y
                + Math.abs(modelMatrix.m21()) * extent.z;

        float newExtentZ = Math.abs(modelMatrix.m02()) * extent.x
                + Math.abs(modelMatrix.m12()) * extent.y
                + Math.abs(modelMatrix.m22()) * extent.z;

        outMin.set(worldCenter4.x - newExtentX, worldCenter4.y - newExtentY, worldCenter4.z - newExtentZ);
        outMax.set(worldCenter4.x + newExtentX, worldCenter4.y + newExtentY, worldCenter4.z + newExtentZ);
    }

    public boolean isAABBVisible(Vector3f worldMin, Vector3f worldMax) {
        for(Vector4f plane : planes) {
            float px = plane.x >= 0 ? worldMax.x : worldMin.x;
            float py = plane.y >= 0 ? worldMax.y : worldMin.y;
            float pz = plane.z >= 0 ? worldMax.z : worldMin.z;

            if(plane.x * px + plane.y * py + plane.z * pz + plane.w < 0) {
                return false;
            }
        }
        return true;
    }
}
