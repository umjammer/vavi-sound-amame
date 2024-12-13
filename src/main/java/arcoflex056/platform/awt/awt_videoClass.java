/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arcoflex056.platform.awt;

import arcadeflex036.software_gfx;
import arcadeflex056.settings;
import static arcadeflex056.settings.current_platform_configuration;
import static arcadeflex056.video.osd_refresh;
import static arcadeflex056.video.scanlines;
import static arcadeflex056.video.screen;
import static arcoflex056.platform.platformConfigurator.*;
import java.awt.Color;

import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import static mame056.mame.Machine;
import static mame056.version.build_version;

/**
 *
 * @author chusogar
 */
public class awt_videoClass implements i_video_class {

    @Override
    public double getWidth() {
        java.awt.Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        
        return d.getWidth();
    }

    @Override
    public double getHeight() {
        java.awt.Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        
        return d.getHeight();
    }

    @Override
    public void tempCreation() {
            screen = new software_gfx(settings.version + " (based on mame v" + build_version + ")");
            screen.initScreen();            
    }
    
        
}
