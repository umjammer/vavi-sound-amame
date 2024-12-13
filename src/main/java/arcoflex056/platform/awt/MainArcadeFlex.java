/**
 * ported to 0.56
 */
package arcoflex056.platform.awt;

import static common.util.*;
import arcadeflex056.osdepend;
import static arcoflex056.platform.platformConfigurator.*;

public class MainArcadeFlex {

    public static void main(String[] args) {
        ConfigurePlatform((i_platform_configurator)new arcoflex056.platform.awt.awt_Configurator());
        ConvertArguments("arcadeflex", args);
        System.exit(osdepend.main(argc, argv));
    }

}