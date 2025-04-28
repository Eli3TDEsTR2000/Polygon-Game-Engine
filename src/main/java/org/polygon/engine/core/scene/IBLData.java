package org.polygon.engine.core.scene;

public class IBLData {
    private String environmentMapPath;
    // Add fields for irradiance map, prefilter map, brdfLUT later

    public IBLData(String environmentMapPath) {
        this.environmentMapPath = environmentMapPath;
        // Placeholder: Add logic to load/process HDR map here or elsewhere
    }

    public String getEnvironmentMapPath() {
        return environmentMapPath;
    }

    // Add getters for other maps later
}
