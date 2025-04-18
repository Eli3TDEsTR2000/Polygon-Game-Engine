package org.polygon.engine.core.sound;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.openal.*;
import org.polygon.engine.core.scene.Camera;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class SoundManager {
    private final List<SoundBuffer> soundBufferList;
    private final Map<String, SoundSource> soundSourceMap;
    private SoundListener soundListener;
    private long context;
    private long device;

    public SoundManager() {
        soundBufferList = new ArrayList<>();
        soundSourceMap = new HashMap<>();

        device = alcOpenDevice((ByteBuffer) null);
        if(device == NULL) {
            throw new IllegalStateException("Failed to open the default OpenAL device.");
        }

        ALCCapabilities deviceCaps = ALC.createCapabilities(device);
        context = alcCreateContext(device, (IntBuffer) null);
        if(context == NULL) {
            throw new IllegalStateException("Failed to create OpenAL context.");
        }
        alcMakeContextCurrent(context);
        AL.createCapabilities(deviceCaps);
    }

    public void cleanup() {
        soundBufferList.forEach(SoundBuffer::cleanup);
        soundBufferList.clear();
        soundSourceMap.values().forEach(SoundSource::cleanup);
        soundSourceMap.clear();

        if(context != NULL) {
            alcDestroyContext(context);
        }

        if(device != NULL) {
            alcCloseDevice(device);
        }
    }

    public SoundListener getSoundListener() {
        return soundListener;
    }

    public SoundSource getSoundSource(String name) {
        return soundSourceMap.get(name);
    }

    public void setAttenuationModel(int model) {
        alDistanceModel(model);
    }

    public void setSoundListener(SoundListener soundListener) {
        this.soundListener = soundListener;
    }

    public void addSoundSource(String name, SoundSource soundSource) {
        soundSourceMap.put(name, soundSource);
    }

    public void addSoundBuffer(SoundBuffer soundBuffer) {
        soundBufferList.add(soundBuffer);
    }

    public void playSoundSource(String name) {
        SoundSource soundSource = soundSourceMap.get(name);
        if(soundSource != null && !soundSource.isPlaying()) {
            soundSource.play();
        }
    }

    public void removeSoundSource(String name) {
        soundSourceMap.remove(name);
    }

    public void updateListenerPosition(Camera camera) {
        Matrix4f viewMatrix = camera.getViewMatrix();
        soundListener.setPosition(camera.getPosition());
        Vector3f at = new Vector3f();
        viewMatrix.positiveZ(at).negate();
        Vector3f up = new Vector3f();
        viewMatrix.positiveY(up);
        soundListener.setOrientation(at, up);
    }
}
