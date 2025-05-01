package org.polygon.engine.core.scene;

public class IBLData {
    private final String environmentMapPath;
    private int irradianceMapTextureId = -1;
    private int prefilterMapTextureId = -1;

    public IBLData(String environmentMapPath) {
        this.environmentMapPath = environmentMapPath;
    }

    public String getEnvironmentMapPath() {
        return environmentMapPath;
    }

    public void setIrradianceMapTextureId(int irradianceMapTextureId) {
        this.irradianceMapTextureId = irradianceMapTextureId;
    }

    public int getIrradianceMapTextureId() {
        return irradianceMapTextureId;
    }

    public void setPrefilterMapTextureId(int prefilterMapTextureId) {
        this.prefilterMapTextureId = prefilterMapTextureId;
    }

    public int getPrefilterMapTextureId() {
        return prefilterMapTextureId;
    }
}
