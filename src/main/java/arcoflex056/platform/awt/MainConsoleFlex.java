/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arcoflex056.platform.awt;

import static common.util.*;
import arcadeflex056.osdepend;
import static arcoflex056.platform.platformConfigurator.*;

/**
 *
 * @author chusogar
 */
public class MainConsoleFlex {

    public static void main(String[] args) {
        ConfigurePlatform(new arcoflex056.platform.awt.awt_Configurator());
        ConvertArguments("consoleflex", args);
        System.exit(osdepend.main(argc, argv));
    }
    
}
