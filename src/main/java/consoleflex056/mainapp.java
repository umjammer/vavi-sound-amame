/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package consoleflex056;

import static common.util.*;

import arcadeflex056.osdepend;

/**
 *
 * @author shadow-laptop
 */
public class mainapp {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ConvertArguments("consoleflex", args);
        //args = null;
        System.exit(osdepend.main(argc, argv));
    }

}
