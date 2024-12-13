/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arcoflex056.platform.awt;

import arcoflex056.platform.platformConfigurator;
import arcoflex056.platform.platformConfigurator.i_SoundPlayer_class;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import static mame056.mame.Machine;

/**
 *
 * @author chusogar
 */
public class awt_SoundPlayerClass implements i_SoundPlayer_class {
    
    private SourceDataLine m_line;
    private AudioFormat format;
    private DataLine.Info info;

    @Override
    public void createAudioFormat(int stereo) {
        
        format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                Machine.sample_rate,
                16,
                (stereo!=0 ? 2:1),
                (stereo!=0 ? 4:2),
                Machine.sample_rate,
                false);
    }

    @Override
    public boolean isLineSupported() {
        System.out.println("frameSize " + format.getFrameSize());
        info = new DataLine.Info(SourceDataLine.class, format);
        //DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        
        return AudioSystem.isLineSupported(info);
    }

    @Override
    public Object getAudioFormat() {
        return format;
    }

    @Override
    public void getLine() throws Exception {
        m_line = (SourceDataLine) AudioSystem.getLine(info);
        m_line.open(format);
    }

    @Override
    public void Play() {
        m_line.start();
    }

    @Override
    public void Stop() {
        m_line.stop();
    }

    @Override
    public void write(byte[] waveBuffer, int offset, int length) {
        m_line.write(waveBuffer, offset, length);
    }
    
}
