package org.polygon.engine.core.scene;

public class IBLData {
    private final String environmentMapPath;
    private int irradianceMapTextureId = -1; // Store generated ID
    // TODO: Add prefilterMapTextureId, brdfLUTTextureId later

    public IBLData(String environmentMapPath) {
        this.environmentMapPath = environmentMapPath;
        // Placeholder: Add logic to load/process HDR map here or elsewhere
    }

    public String getEnvironmentMapPath() {
        return environmentMapPath;
    }

    // Setter for irradiance map ID (called after generation)
    public void setIrradianceMapTextureId(int irradianceMapTextureId) {
        this.irradianceMapTextureId = irradianceMapTextureId;
    }

    public int getIrradianceMapTextureId() {
        return irradianceMapTextureId;
    }

    // Add getters for other maps later
}
