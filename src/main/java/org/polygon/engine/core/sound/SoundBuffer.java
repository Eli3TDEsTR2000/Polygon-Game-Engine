package org.polygon.engine.core.sound;

import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.stb.STBVorbis.*;
import static org.lwjgl.system.MemoryUtil.NULL;

// Holds reference to the OpenAL buffer.
// Imports .OGG files to the buffer's data.
// Used as a raw sound data to the SoundSource
public class SoundBuffer {
    private final int bufferId;
    private ShortBuffer pcm;

    public SoundBuffer(String path) {
        this.bufferId = alGenBuffers();
        try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
            pcm = readVorbis(path, info);

            alBufferData(bufferId, info.channels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16
                    , pcm, info.sample_rate());
        }
    }

    public void cleanup() {
        alDeleteBuffers(bufferId);
        if(pcm != null) {
            MemoryUtil.memFree(pcm);
        }
    }

    public int getBufferId() {
        return bufferId;
    }

    private ShortBuffer readVorbis(String path, STBVorbisInfo info) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            long decoder = stb_vorbis_open_filename(path, error, null);
            if(decoder == NULL) {
                throw new RuntimeException("Failed to open OGG Vorbis file. Error: " + error.get(0));
            }

            stb_vorbis_get_info(decoder, info);

            int channels = info.channels();

            int lengthSamples = stb_vorbis_stream_length_in_samples(decoder);

            ShortBuffer result = MemoryUtil.memAllocShort(lengthSamples * channels);

            result.limit(stb_vorbis_get_samples_short_interleaved(decoder, channels, result) * channels);

            stb_vorbis_close(decoder);

            return result;
        }
    }
}
