package org.polygon.engine.core.sound;

import org.joml.Vector3f;

import static org.lwjgl.openal.AL10.*;

// Represents a sound listener.
public class SoundListener {

    public SoundListener(Vector3f position) {
        alListener3f(AL_POSITION, position.x, position.y, position.z);
        alListener3f(AL_VELOCITY, 0, 0, 0);
    }

    public void setOrientation(Vector3f at, Vector3f up) {
        float[] data = new float[6];
        data[0] = at.x;
        data[1] = at.y;
        data[2] = at.z;
        data[3] = up.x;
        data[4] = up.y;
        data[5] = up.z;

        alListenerfv(AL_ORIENTATION, data);
    }

    public void setPosition(Vector3f position) {
        alListener3f(AL_POSITION, position.x, position.y ,position.z);
    }

    public void setVelocity(Vector3f velocity) {
        alListener3f(AL_VELOCITY, velocity.x, velocity.y, velocity.z);
    }
}
