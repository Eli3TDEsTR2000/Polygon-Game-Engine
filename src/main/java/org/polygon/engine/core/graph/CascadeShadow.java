package org.polygon.engine.core.graph;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.polygon.engine.core.scene.Scene;

import java.util.List;

public class CascadeShadow {
    public final static int SHADOW_MAP_CASCADE_COUNT = 3;
    private Matrix4f projViewMatrix;
    private float splitDistance;

    public CascadeShadow() {
        projViewMatrix = new Matrix4f();
    }

    public Matrix4f getProjViewMatrix() {
        return projViewMatrix;
    }

    public float getSplitDistance() {
        return splitDistance;
    }

    public static void updateCascadeShadows(List<CascadeShadow> cascadeShadowList, Scene scene) {
        if(scene.getSceneLights() == null) {
            return;
        }

        Matrix4f viewMatrix = scene.getCamera().getViewMatrix();
        Matrix4f projectionMatrix = scene.getProjection().getMatrix();
        Vector4f directionalLightPosition =
                new Vector4f(scene.getSceneLights().getDirectionalLight().getDirection(), 0);

        float cascadeSplitLambda = 0.95f;

        float[] cascadeSplits = new float[SHADOW_MAP_CASCADE_COUNT];

        float nearClip = projectionMatrix.perspectiveNear();
        float farClip = projectionMatrix.perspectiveFar();
        float clipRange = farClip - nearClip;

        float minZ = nearClip;
        float maxZ = nearClip + clipRange;

        float range = maxZ - minZ;
        float ratio = maxZ / minZ;

        // Calculate split depths based on view camera frustum
        // Based on method presented in https://developer.nvidia.com/gpugems/GPUGems3/gpugems3_ch10.html
        for (int i = 0; i < SHADOW_MAP_CASCADE_COUNT; i++) {
            float p = (i + 1) / (float) (SHADOW_MAP_CASCADE_COUNT);
            float log = (float) (minZ * java.lang.Math.pow(ratio, p));
            float uniform = minZ + range * p;
            float d = cascadeSplitLambda * (log - uniform) + uniform;
            cascadeSplits[i] = (d - nearClip) / clipRange;
        }

        // Calculate orthographic projection matrix for each cascade.
        float lastSplitDistance = 0.0f;
        for(int i = 0; i < SHADOW_MAP_CASCADE_COUNT; i++) {
            float splitDistance = cascadeSplits[i];
            Vector3f[] frustumCorners = new Vector3f[]{
                    new Vector3f(-1.0f, 1.0f, -1.0f),
                    new Vector3f(1.0f, 1.0f, -1.0f),
                    new Vector3f(1.0f, -1.0f, -1.0f),
                    new Vector3f(-1.0f, -1.0f, -1.0f),
                    new Vector3f(-1.0f, 1.0f, 1.0f),
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    new Vector3f(1.0f, -1.0f, 1.0f),
                    new Vector3f(-1.0f, -1.0f, 1.0f),
            };

            // Project frustum corners into world space
            Matrix4f invProjViewMatrix = (new Matrix4f(projectionMatrix).mul(viewMatrix)).invert();
            for (int j = 0; j < 8; j++) {
                Vector4f invCorner = new Vector4f(frustumCorners[j], 1.0f).mul(invProjViewMatrix);
                frustumCorners[j] = new Vector3f(invCorner.x / invCorner.w, invCorner.y / invCorner.w
                        , invCorner.z / invCorner.w);
            }

            for (int j = 0; j < 4; j++) {
                Vector3f dist = new Vector3f(frustumCorners[j + 4]).sub(frustumCorners[j]);
                frustumCorners[j + 4] = new Vector3f(frustumCorners[j]).add(new Vector3f(dist).mul(splitDistance));
                frustumCorners[j] = new Vector3f(frustumCorners[j]).add(new Vector3f(dist).mul(lastSplitDistance));
            }

            // Calculate frustum center.
            Vector3f frustumCenter = new Vector3f(0.0f);
            for (int j = 0; j < 8; j++) {
                frustumCenter.add(frustumCorners[j]);
            }
            frustumCenter.div(8.0f);

            // Calculate frustum radius.
            float radius = 0.0f;
            for (int j = 0; j < 8; j++) {
                float distance = (new Vector3f(frustumCorners[j]).sub(frustumCenter)).length();
                radius = Math.max(radius, distance);
            }
            radius = (float) Math.ceil(radius * 16.0f) / 16.0f;

            // Calculate the viewMatrix from the directional light's POV.
            // Calculate the projection matrix which is in orthographic
            // to calculate the 2D cascade shadow maps from the light's POV using the calculated viewMatrix.
            Vector3f maxExtents = new Vector3f(radius);
            Vector3f minExtents = new Vector3f(maxExtents).mul(-1);

            Vector3f lightDirection = (new Vector3f(directionalLightPosition.x
                    , directionalLightPosition.y, directionalLightPosition.z).mul(-1).normalize());
            Vector3f eye = new Vector3f(frustumCenter).sub(new Vector3f(lightDirection).mul(-minExtents.z));
            Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
            Matrix4f lightViewMatrix = new Matrix4f().lookAt(eye, frustumCenter, up);
            Matrix4f lightProjMatrix = new Matrix4f().ortho(minExtents.x, maxExtents.x
                    , minExtents.y, maxExtents.y, 0.0f, maxExtents.z - minExtents.z, true);

            // Store split distance and projViewMatrix in each cascade.
            CascadeShadow cascadeShadow = cascadeShadowList.get(i);
            cascadeShadow.splitDistance = (nearClip + splitDistance * clipRange) * -1.0f;
            cascadeShadow.projViewMatrix = lightProjMatrix.mul(lightViewMatrix);

            lastSplitDistance = cascadeSplits[i];
        }
    }
}
