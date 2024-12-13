/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arcoflex056.platform.awt;

import arcadeflex056.fucPtr.WriteHandlerPtr;
import static arcadeflex056.video.screen;
import static arcoflex056.platform.platformConfigurator.*;
import java.awt.event.KeyEvent;
import static mame056.inputH.*;
import mame056.inputH.KeyboardInfo;

/**
 *
 * @author chusogar
 */
public class awt_Configurator implements i_platform_configurator {
    
    public awt_inputClass current_awt_input_class = new awt_inputClass();
    public awt_softwareGFXClass current_awt_softwareGFX_class = new awt_softwareGFXClass();
    public awt_urlDownloadProgress current_awt_urlDownloadProgress_class = new awt_urlDownloadProgress();
    public awt_videoClass current_awt_video_class = new awt_videoClass();
    public awt_SoundPlayerClass current_awt_SoundPlayer_class = new awt_SoundPlayerClass();
    public awt_filemngrClass current_filemngr_class = new awt_filemngrClass();
    
    @Override
    public String getPlatformName() {
        return "AWT/Swing";
    }

    @Override
    public i_input_class get_input_class() {
        return current_awt_input_class;
    }

    @Override
    public i_software_gfx_class get_software_gfx_class() {
        return current_awt_softwareGFX_class;
    }

    @Override
    public i_URLDownloadProgress_class get_URLDownloadProgress_class() {
        return current_awt_urlDownloadProgress_class;
    }

    @Override
    public i_video_class get_video_class() {
        return current_awt_video_class;
    }

    @Override
    public i_SoundPlayer_class get_SoundPlayer_class() {
        return current_awt_SoundPlayer_class;
    }

    @Override
    public i_filemngr_class get_filemngr_class() {
        return current_filemngr_class;
    }

        
}
