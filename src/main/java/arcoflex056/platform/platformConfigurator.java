/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arcoflex056.platform;

import static arcadeflex056.settings.*;
import java.io.File;
import mame056.inputH;
import mame056.inputH.KeyboardInfo;


/**
 *
 * @author chusogar
 */
public class platformConfigurator {
    
    public interface i_platform_configurator {
        
        public String getPlatformName();        
        public i_input_class get_input_class();
        public i_software_gfx_class get_software_gfx_class();
        public i_URLDownloadProgress_class get_URLDownloadProgress_class();
        public i_video_class get_video_class();
        public i_SoundPlayer_class get_SoundPlayer_class();
        public i_filemngr_class get_filemngr_class();
        
    };
    
    public interface i_input_class {
        
        public abstract KeyboardInfo[] osd_get_key_list();
        public abstract int osd_is_key_pressed(int keycode);
    };

    public interface i_software_gfx_class {
        public abstract void setTitle(String title);
        public abstract void blit();
        public abstract void initScreen();
        public abstract void setSize(boolean scanlines, int width, int height);
        public abstract void run();
        public abstract void reint();
    };

    public interface i_URLDownloadProgress_class {
        public abstract void setVersion(String _version);
        public abstract void setVisible(boolean _visible);
        public abstract void setRomName(String _romName);
        public abstract void setFileName(String _fileName);
    };

    public interface i_video_class {
        public abstract double getWidth();
        public abstract double getHeight();
        public abstract void tempCreation();
    };

    public interface i_SoundPlayer_class {
        public abstract void createAudioFormat(int stereo);
        public abstract boolean isLineSupported();
        public abstract Object getAudioFormat();
        public abstract void getLine() throws Exception;
        public abstract void Play();
        public abstract void Stop();
        public abstract void write(byte[] waveBuffer, int offset, int length);
        
    };

    public interface i_filemngr_class {
        public abstract void setCurrentDirectory(java.io.File file);
        public abstract void setFileFilter(String _supFilesStr, String[] _arrExtensions);
        public abstract File getCurrentDirectory();
        public abstract int showOpenDialog(Object obj);
        public abstract Object getSelectedFile();
    };
    
    public static void ConfigurePlatform(i_platform_configurator platform){
        current_platform_configuration = platform;        
    };
    
}
