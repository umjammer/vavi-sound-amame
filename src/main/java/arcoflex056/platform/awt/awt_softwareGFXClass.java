/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arcoflex056.platform.awt;

import static arcadeflex056.settings.current_platform_configuration;
import static arcadeflex056.video.osd_refresh;
import static arcadeflex056.video.scanlines;
import static arcadeflex056.video.screen;
import static arcoflex056.platform.platformConfigurator.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import static mame056.mame.Machine;

public class awt_softwareGFXClass extends java.awt.Frame implements Runnable, ImageProducer, KeyListener, MouseListener, MouseMotionListener, i_software_gfx_class {
    
    ImageConsumer _consumer;
    Insets _insets;
    Image _image;
    DirectColorModel _model;
    BufferStrategy _strategy;
    
    int _width;
    int _height;
    
    public Thread _thread;
    
    
    public awt_softwareGFXClass(){
        super();
    }
    
    /*public void setTitle(String title){
        this.setTitle(title);
    }*/
    
    public void initScreen(){
        this.pack();
            //screen.setSize((scanlines==1),gfx_width,gfx_height);//this???
            //screen.setSize((scanlines==1),width,height);//this???
            
            screen.setSize((scanlines == 0), Machine.scrbitmap.width, Machine.scrbitmap.height);
            _width = Machine.scrbitmap.width;
            _height = Machine.scrbitmap.height;
            this.setBackground(Color.BLACK);
            this.start();
            this.run();
            this.setLocation((int) ((current_platform_configuration.get_video_class().getWidth() - screen.getWidth()) / 2.0D), (int) ((current_platform_configuration.get_video_class().getHeight() - screen.getHeight()) / 2.0D));
            this.setVisible(true);
            this.setResizable((scanlines == 1));

            this.addWindowListener(new WindowAdapter() {

                public void windowClosing(WindowEvent evt) {
                    screen.readkey = KeyEvent.VK_ESCAPE;
                    screen.key[KeyEvent.VK_ESCAPE] = true;
                    osd_refresh();
                    if (this != null) {
                        screen.key[KeyEvent.VK_ESCAPE] = false;
                    }
                }
            });

            this.addComponentListener(new ComponentAdapter() {

                public void componentResized(ComponentEvent evt) {
                    screen.resizeVideo();
                }
            });

            this.addKeyListener(this);
            this.setFocusTraversalKeysEnabled(false);
            this.requestFocus();
    }
    
    public void blit(){
        if (this._consumer != null)/* Check consumer. */ {
            /* Set dimensions. */
            this._consumer.setDimensions(this._width, this._height);
            /* Copy integer pixel data to image consumer. */
            //int[] px = resizeBilinear(_pixels, this._width, this._height, this._width, this._height);
            this._consumer.setPixels(0, 0, this._width, this._height, this._model, screen._pixels /*px*/, 0, this._width);
            /* Notify image consumer that the frame is done. */
            this._consumer.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
        }
        /* Handle resize events. */
        int i = getWidth() - this._insets.left - this._insets.right;
        int j = getHeight() - this._insets.top - this._insets.bottom;
        /* Draw image to graphics context. */
        Graphics2D localGraphics2D = (Graphics2D) this._strategy.getDrawGraphics();
        if (Machine.gamedrv.source_file.equals("kyugo.java")) {
            if (Machine.gamedrv.name.equals("airwolf") || Machine.gamedrv.name.equals("flashgal") || Machine.gamedrv.name.equals("skywolf") || Machine.gamedrv.name.equals("skywolf2")) {//temp hack for airwolf and flashgal
                localGraphics2D.drawImage(this._image, this._insets.left, this._insets.top, i + (int) (i * 0.78), j, null);
            } else {
                localGraphics2D.drawImage(this._image, this._insets.left, this._insets.top, i, j + (int) (i * 0.78), null);

            }
        } else if (Machine.gamedrv.source_file.equals("system1.java")) {
            if (Machine.gamedrv.name.equals("starjack") || Machine.gamedrv.name.equals("starjacs") || Machine.gamedrv.name.equals("regulus") || Machine.gamedrv.name.equals("regulusu") || Machine.gamedrv.name.equals("upndown") || Machine.gamedrv.name.equals("mrviking") || Machine.gamedrv.name.equals("mrvikinj") || Machine.gamedrv.name.equals("swat")) {
                localGraphics2D.drawImage(this._image, this._insets.left, this._insets.top, i + (int) (i * 0.15), j, null);
            } else {
                localGraphics2D.drawImage(this._image, this._insets.left, this._insets.top, i, j + (int) (j * 0.14), null);
            }
        } else if (Machine.gamedrv.name.equals("tnk3") || Machine.gamedrv.name.equals("tnk3j")) {
            localGraphics2D.drawImage(this._image, this._insets.left - (int) (i * 0.02), this._insets.top, i, j, null);
        } else if (Machine.gamedrv.name.equals("tdfever") || Machine.gamedrv.name.equals("tdfeverj") || Machine.gamedrv.name.equals("chopper") || Machine.gamedrv.name.equals("legofair") || Machine.gamedrv.name.equals("gwar") || Machine.gamedrv.name.equals("gwarj") || Machine.gamedrv.name.equals("gwara") || Machine.gamedrv.name.equals("gwarb")) {
            localGraphics2D.drawImage(this._image, this._insets.left + (int) (i * 0.03), this._insets.top, i, j, null);
        } else if (Machine.gamedrv.source_file.equals("srumbler.java")) {
            localGraphics2D.drawImage(this._image, this._insets.left - (int) (i * 0.035), this._insets.top, i + (int) (i * 0.07), j, null);
        } else if (Machine.gamedrv.source_file.equals("simpsons.java")) {
            localGraphics2D.drawImage(this._image, this._insets.left - (int) (i * 0.39), this._insets.top, i + (int) (i * 0.78), j, null);
        } else if (Machine.gamedrv.source_file.equals("vendetta.java")) {
            localGraphics2D.drawImage(this._image, this._insets.left - (int) (i * 0.35), this._insets.top, i + (int) (i * 0.70), j, null);
        } else if (Machine.gamedrv.source_file.equals("surpratk.java")) {
            localGraphics2D.drawImage(this._image, this._insets.left - (int) (i * 0.39), this._insets.top, i + (int) (i * 0.78), j, null);
        } else if (Machine.gamedrv.source_file.equals("aliens.java")) {
            localGraphics2D.drawImage(this._image, this._insets.left - (int) (i * 0.39), this._insets.top, i + (int) (i * 0.78), j, null);
        } else if (Machine.gamedrv.source_file.equals("crimfght.java")) {
            localGraphics2D.drawImage(this._image, this._insets.left - (int) (i * 0.35), this._insets.top, i + (int) (i * 0.70), j, null);
        } else if (Machine.gamedrv.source_file.equals("m72.java")) {
            if (Machine.gamedrv.name.equals("imgfight")) {
                localGraphics2D.drawImage(this._image, this._insets.left - (int) (i * 0.18), this._insets.top - (int) (j * 0.18), i + (int) (i * 0.35), j + (int) (j * 0.35), null);
            } else if (Machine.gamedrv.name.equals("gallop")) {
                localGraphics2D.drawImage(this._image, this._insets.left - (int) (i * 0.17), this._insets.top - (int) (j * 0.25), i + (int) (i * 0.34), j + (int) (j * 0.50), null);
            } else {
                localGraphics2D.drawImage(this._image, this._insets.left - (int) (i * 0.18), this._insets.top - (int) (j * 0.25), i + (int) (i * 0.35), j + (int) (j * 0.50), null);
            }
        } else if (Machine.gamedrv.source_file.equals("raiden.java")) {
            localGraphics2D.drawImage(this._image, this._insets.left - (int) (i * 0.07), this._insets.top, i + (int) (i * 0.14), j, null);
        } else if (Machine.gamedrv.source_file.equals("cps1.java")) {
            localGraphics2D.drawImage(this._image, this._insets.left - (int) (i * 0.085), this._insets.top - (int) (j * 0.215), i + (int) (i * 0.167), j + (int) (j * 0.43), null);
        //} else if (Machine.gamedrv.source_file.equals("amstrad.java")) {
        //    localGraphics2D.drawImage(this._image, this._insets.left - (int) (i * 0.085), this._insets.top - (int) (j * 0.215), i + (int) (i * 0.167), j + (int) (j * 0.43), null);
        } else {
            localGraphics2D.drawImage(this._image, this._insets.left, this._insets.top, i, j, null);
        }
        this._strategy.show();
        Toolkit.getDefaultToolkit().sync();
    }
    
    public void start() {
        /* Check if thread exists. */
        if (_thread != null) {
            return;
        }

        /* Create and start thread. */
        _thread = new Thread(this);
        _thread.start();
    }

    public void stop() {
        /* Destroy thread. */
        _thread = null;
    }
    
    @Override
    public synchronized void addConsumer(ImageConsumer ic) {
        /* Register image consumer. */
        _consumer = ic;

        /* Set image dimensions. */
        _consumer.setDimensions(_width * 2, _height * 2);

        /* Set image consumer hints for speed. */
        _consumer.setHints(ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES | ImageConsumer.SINGLEPASS | ImageConsumer.SINGLEFRAME);

        /* Set image color model. */
        _consumer.setColorModel(_model);
    }


    @Override
    public synchronized boolean isConsumer(ImageConsumer ic) {
        /* Check if consumer is registered. */
        return true;
    }

    @Override
    public synchronized void removeConsumer(ImageConsumer ic) {
        /* Remove image consumer. */
    }

    @Override
    public void startProduction(ImageConsumer ic) {
        /* Add consumer. */
        addConsumer(ic);
    }

    @Override
    public void requestTopDownLeftRightResend(ImageConsumer ic) {
        /* Ignore resend request. */
    }

    /**
     * Handle the key released event from the text field.
     */
    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    /**
     * Handle the key pressed event from the text field.
     */
    public void keyPressed(KeyEvent e) {
        screen.readkey = e.getKeyCode();
        screen.key[screen.readkey] = true;
        e.consume();
    }

    @Override
    /**
     * Handle the key released event from the text field.
     */
    public void keyReleased(KeyEvent e) {
        screen.key[e.getKeyCode()] = false;
        e.consume();
    }

    public void mouseClicked(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mousePressed(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mouseReleased(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mouseEntered(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mouseExited(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mouseDragged(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mouseMoved(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setSize(boolean scanlines, int width, int height) {
        _insets = getInsets();
        // super.setSize(width+ this._insets.left + this._insets.right, height + this._insets.top + this._insets.bottom);
        //hacked width height x2
        if (Machine.gamedrv.source_file.equals("kyugo.java")) {
            if (Machine.gamedrv.name.equals("airwolf") || Machine.gamedrv.name.equals("flashgal") || Machine.gamedrv.name.equals("skywolf") || Machine.gamedrv.name.equals("skywolf2")) {//temp hack for airwolf and flashgal
                super.setSize(width + this._insets.left + this._insets.right, height * 2 + this._insets.top + this._insets.bottom);
            } else {
                super.setSize(width * 2 + this._insets.left + this._insets.right, height + this._insets.top + this._insets.bottom);
            }
        } else if (Machine.gamedrv.source_file.equals("srumbler.java")) {
            super.setSize(width * 2 + this._insets.left + this._insets.right, height + (int) (height * 0.50) + this._insets.top + this._insets.bottom);
        } else if (Machine.gamedrv.source_file.equals("simpsons.java")) {
            super.setSize(width * 2 + this._insets.left + this._insets.right - (int) (width * 0.78), height * 2 + this._insets.top + this._insets.bottom);
        } else if (Machine.gamedrv.source_file.equals("vendetta.java")) {
            super.setSize(width * 2 + this._insets.left + this._insets.right - (int) (width * 0.70), height * 2 + this._insets.top + this._insets.bottom);
        } else if (Machine.gamedrv.source_file.equals("surpratk.java")) {
            super.setSize(width * 2 + this._insets.left + this._insets.right - (int) (width * 0.70), height * 2 + this._insets.top + this._insets.bottom);
        } else if (Machine.gamedrv.source_file.equals("aliens.java")) {
            super.setSize(width * 2 + this._insets.left + this._insets.right - (int) (width * 0.70), height * 2 + this._insets.top + this._insets.bottom);
        } else if (Machine.gamedrv.source_file.equals("crimfght.java")) {
            super.setSize(width * 2 + this._insets.left + this._insets.right - (int) (width * 0.78), height * 2 + this._insets.top + this._insets.bottom);
        } else if (Machine.gamedrv.source_file.equals("m72.java")) {
            if (Machine.gamedrv.name.equals("imgfight")) {
                super.setSize(width * 2 + this._insets.left + this._insets.right - (int) (width * 0.35), height + (int) (height * 0.35) + this._insets.top + this._insets.bottom);
            } else if (Machine.gamedrv.name.equals("gallop")) {
                super.setSize(width * 2 + this._insets.left + this._insets.right - (int) (width * 0.34), height + (int) (height * 0.50) + this._insets.top + this._insets.bottom);
            } else {
                super.setSize(width * 2 + this._insets.left + this._insets.right - (int) (width * 0.35), height + (int) (height * 0.50) + this._insets.top + this._insets.bottom);
            }
        } else if (Machine.gamedrv.source_file.equals("raiden.java")) {
            super.setSize(width * 2 + this._insets.left + this._insets.right - (int) (width * 0.14), height * 2 + this._insets.top + this._insets.bottom);
        } else if (Machine.gamedrv.source_file.equals("amstrad.java")) {
            super.setSize(width + this._insets.left + this._insets.right, height * 2 + this._insets.top + this._insets.bottom);
        } else if ((Machine.gamedrv.name.equals("msx2")) || (Machine.gamedrv.name.equals("msx2a"))) {
            super.setSize(width + this._insets.left + this._insets.right, height + this._insets.top + this._insets.bottom);
        //} else if ((Machine.gamedrv.name.equals("gameboy"))) {
        //    super.setSize(width + this._insets.left + this._insets.right, height + this._insets.top + this._insets.bottom);
        } else {
            super.setSize(width * 2 + this._insets.left + this._insets.right, height * 2 + this._insets.top + this._insets.bottom);
        }
        
        //System.out.println("Width2="+width);
        System.out.println(this.getWidth() + "x" + this.getHeight());
        super.createBufferStrategy(2);//double buffering
        this._strategy = super.getBufferStrategy();
    }

    @Override
    public void run() {
        /* Setup color model. */
        this._model = new DirectColorModel(32, 0x00FF0000, 0x000FF00, 0x000000FF, 0);
        /* Create image using default toolkit. */
        this._image = Toolkit.getDefaultToolkit().createImage(this);
    }

    @Override
    public void reint() {
        /* Recreate image using default toolkit. */
        _image = Toolkit.getDefaultToolkit().createImage(this);
        _consumer = null;
    }
    
}
