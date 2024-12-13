/*
 This file is part of Arcadeflex.

 Arcadeflex is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Arcadeflex is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Arcadeflex.  If not, see <http://www.gnu.org/licenses/>.
 */
package arcadeflex036;

import static arcadeflex056.settings.current_platform_configuration;
import arcoflex056.platform.platformConfigurator;
import arcoflex056.platform.platformConfigurator.i_software_gfx_class;
/*import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;*/

import static mame056.mame.Machine;

public class software_gfx {
    /* Data. */

    boolean _scanlines;
    int _width;
    int _height;
    float ratio;
    int oldWidth;
    int oldHeight;
    
    public int[] _pixels;
    
    public boolean[] key = new boolean[1024];
    public int readkey = 0;
    
    
    //public i_software_gfx_class myGFXClass;

    public software_gfx(String title) {
        current_platform_configuration.get_software_gfx_class().setTitle(title);
    }
    
    public void initScreen(){
        current_platform_configuration.get_software_gfx_class().initScreen();
    }

    public int[] resizeBilinear(int[] pixels, int w, int h, int w2, int h2) {
        int[] temp = new int[w2 * h2];
        int a, b, c, d, x, y, index;
        float x_ratio = ((float) (w - 1)) / w2;
        float y_ratio = ((float) (h - 1)) / h2;
        float x_diff, y_diff, blue, red, green;
        int offset = 0;
        for (int i = 0; i < h2; i++) {
            for (int j = 0; j < w2; j++) {
                x = (int) (x_ratio * j);
                y = (int) (y_ratio * i);
                x_diff = (x_ratio * j) - x;
                y_diff = (y_ratio * i) - y;
                index = (y * w + x);
                a = pixels[index];
                b = pixels[index + 1];
                c = pixels[index + w];
                d = pixels[index + w + 1];

            // blue element
                // Yb = Ab(1-w)(1-h) + Bb(w)(1-h) + Cb(h)(1-w) + Db(wh)
                blue = (a & 0xff) * (1 - x_diff) * (1 - y_diff) + (b & 0xff) * (x_diff) * (1 - y_diff)
                        + (c & 0xff) * (y_diff) * (1 - x_diff) + (d & 0xff) * (x_diff * y_diff);

            // green element
                // Yg = Ag(1-w)(1-h) + Bg(w)(1-h) + Cg(h)(1-w) + Dg(wh)
                green = ((a >> 8) & 0xff) * (1 - x_diff) * (1 - y_diff) + ((b >> 8) & 0xff) * (x_diff) * (1 - y_diff)
                        + ((c >> 8) & 0xff) * (y_diff) * (1 - x_diff) + ((d >> 8) & 0xff) * (x_diff * y_diff);

            // red element
                // Yr = Ar(1-w)(1-h) + Br(w)(1-h) + Cr(h)(1-w) + Dr(wh)
                red = ((a >> 16) & 0xff) * (1 - x_diff) * (1 - y_diff) + ((b >> 16) & 0xff) * (x_diff) * (1 - y_diff)
                        + ((c >> 16) & 0xff) * (y_diff) * (1 - x_diff) + ((d >> 16) & 0xff) * (x_diff * y_diff);

                temp[offset++]
                        = /*0xff000000 | // hardcode alpha*/ ((((int) red) << 16) & 0xff0000)
                        | ((((int) green) << 8) & 0xff00)
                        | ((int) blue);
            }
        }
        return temp;
    }
    
    
    public synchronized void blit() {
        
        current_platform_configuration.get_software_gfx_class().blit();
        
        
    }


    public synchronized void setSize(boolean scanlines, int width, int height) {

        //System.out.println("Width="+width);
        /* Setup options. */
        _scanlines = false;
        
        // hack
        //_scanlines = false;

        /* Setup pixel buffer and dimensions. */
        ratio = ((float) width) / ((float) height);

        this._width = width;
        
        /*if (_scanlines) {
            width *= 2;
            height *= 2;
        }*/
        this._height = height;
        
        _pixels = new int[width * height];
        /* Setup frame dimensions. */
        
        current_platform_configuration.get_software_gfx_class().setSize(scanlines, width, height);
    }
    
    public int getWidth(){
        return _width;
    }
    
    public int getHeight(){
        return _height;
    }

    public void resizeVideo() {
        /*
         int i = getWidth() - this._insets.left - this._insets.right;
         int j = getHeight() - this._insets.top - this._insets.bottom;
         System.out.println("before width=" + i + " height="+ j);
         if ((j < this._height) || (i < this._width))
         {
         i = this._width;
         j = this._height;
         }
         if ((j != this.oldHeight) || (i != this.oldWidth))
         {
         //keep aspect ratio
         if (Math.abs(this.oldHeight - j) >= Math.abs(this.oldWidth - i))
         {
         this.oldHeight = j;
         this.oldWidth = (int)(this.ratio * j);
         }
         else
         {
         this.oldWidth = i;
         this.oldHeight = (int)(i / this.ratio);
         }
         }

         System.out.println("after  width=" + this.oldWidth + " height="+ this.oldHeight);
           
         setSize(this.oldWidth + this._insets.left + this._insets.right, this.oldHeight + this._insets.top + this._insets.bottom);
         */
    }

    public void run() {
        
        current_platform_configuration.get_software_gfx_class().run();
        
    }

    public synchronized void reinit() {
        current_platform_configuration.get_software_gfx_class().reint();
        
    }

    

    
    

    

    

    

    

    

    

    

}
