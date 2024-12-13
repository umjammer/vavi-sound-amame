/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arcoflex056.platform.awt;

import static arcoflex056.platform.platformConfigurator.*;
import java.awt.Component;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author chusogar
 */
public class awt_filemngrClass implements i_filemngr_class {
    
    private JFileChooser jf = new JFileChooser();
    
    public awt_filemngrClass(){
        super();
    }

    @Override
    public void setCurrentDirectory(File file) {
        jf.setCurrentDirectory(file);
    }

    @Override
    public void setFileFilter(String _supFilesStr, String[] _arrExtensions) {
        FileFilter filter = new FileNameExtensionFilter(_supFilesStr, _arrExtensions);
        jf.setFileFilter(filter);
        jf.addChoosableFileFilter(filter);
    }

    @Override
    public File getCurrentDirectory() {
        return jf.getCurrentDirectory();
    }

    @Override
    public int showOpenDialog(Object obj) {
        return jf.showOpenDialog((Component) obj);
    }

    @Override
    public Object getSelectedFile() {
        return jf.getSelectedFile();
    }
    
}
