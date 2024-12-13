/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arcadeflex036;

//import javax.sound.sampled.*;
import static arcadeflex056.settings.current_platform_configuration;
import static mame056.mame.Machine;

/**
 *
 * @author shadow
 */
public class SoundPlayer {

    public static final int MAX_BUFFER_SIZE = 128 * 1024;
    //private DynamicSoundEffectInstance soundInstance;
    private byte[] waveBuffer;
    int/*uint*/ stream_buffer_size;
    

    public SoundPlayer(int sampleRate, int stereo, int framesPerSecond) {
        System.out.println(stereo);
        //soundInstance = new DynamicSoundEffectInstance(sampleRate, stereo ? AudioChannels.Stereo : AudioChannels.Mono);
        current_platform_configuration.get_SoundPlayer_class().createAudioFormat(stereo);

        
        stream_buffer_size = (int) (((long) MAX_BUFFER_SIZE * (long) sampleRate) / 22050);
        int wBitsPerSample = 16;
        int nChannels = stereo!=0 ? 2 : 1;
        int nBlockAlign = wBitsPerSample * nChannels / 8;
        stream_buffer_size = (int) (stream_buffer_size * nBlockAlign) / 4;
        stream_buffer_size = (int) ((stream_buffer_size * 30) / framesPerSecond);
        stream_buffer_size = (stream_buffer_size / 1024) * 1024;

        waveBuffer = new byte[stream_buffer_size];//soundInstance.GetSampleSizeInBytes(TimeSpan.FromMilliseconds(25))];



        if (!current_platform_configuration.get_SoundPlayer_class().isLineSupported()) {
            System.err.println("Unsupported audio: " + current_platform_configuration.get_SoundPlayer_class().getAudioFormat());
            return;
        }

        try {
            current_platform_configuration.get_SoundPlayer_class().getLine();
        } catch (Exception lue) {
            System.err.println("Unavailable data line");
            return;
        }

        current_platform_configuration.get_SoundPlayer_class().Play();
        //soundInstance.Play();
    }

    public int GetStreamBufferSize() {
        return stream_buffer_size;
    }

    /*public int GetSampleSizeInBytes(TimeSpan duration) {
        return soundInstance.GetSampleSizeInBytes(duration);
    }*/

    public void Play() {
        current_platform_configuration.get_SoundPlayer_class().Play();
    }

    public void Stop() {
        current_platform_configuration.get_SoundPlayer_class().Stop();
    }

    public void WriteSample(int index, short sample) {
            waveBuffer[index] = (byte) (sample&0xFF);
            waveBuffer[index + 1] = (byte) (sample >> 8);
        //waveBuffer[index] = (byte)(sample >> 8);
        //waveBuffer[index + 1] = (byte)(sample & 0xFF);
        //waveBuffer,  bi, (short)((data[p++]*master_volume/256)));
    }

    public void SubmitBuffer(int offset, int length) {
        if(waveBuffer!=null)
        current_platform_configuration.get_SoundPlayer_class().write(waveBuffer, offset, length);
    }

}
