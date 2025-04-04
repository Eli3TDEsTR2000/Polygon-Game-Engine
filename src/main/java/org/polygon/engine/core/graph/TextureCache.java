package org.polygon.engine.core.graph;

import java.util.HashMap;
import java.util.Map;

// This class handles loaded textures to load each texture just once
public class TextureCache {
    // Default texture to load when the texture is not found inside the texture map
    public static final String DEFAULT_TEXTURE = "resources/models/default/default_texture.png";
    // Hashmap to check if texture was already generated, stores texture path and texture object
    private Map<String, Texture> textureMap;

    public TextureCache() {
        textureMap = new HashMap<>();
        textureMap.put(DEFAULT_TEXTURE, new Texture(DEFAULT_TEXTURE));
    }

    public void cleanup() {
        textureMap.values().forEach(Texture::cleanup);
    }

    public Texture createTexture(String texturePath) {
        return textureMap.computeIfAbsent(texturePath, Texture::new);
    }

    public Texture getTexture(String texturePath) {
        Texture texture = null;
        if(texturePath != null) {
            texture = textureMap.get(texturePath);
        }

        // If the texture is not found we return a default texture
        if(texture == null) {
            texture = textureMap.get(DEFAULT_TEXTURE);
        }

        return texture;
    }
}
