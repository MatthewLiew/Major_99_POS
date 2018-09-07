/*
**    Chromis POS  - The New Face of Open Source POS
**    Copyright (c)2015-2016
**    http://www.chromis.co.uk
**
**    This file is part of Chromis POS Version V0.60.2 beta
**
**    Chromis POS is free software: you can redistribute it and/or modify
**    it under the terms of the GNU General Public License as published by
**    the Free Software Foundation, either version 3 of the License, or
**    (at your option) any later version.
**
**    Chromis POS is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**    GNU General Public License for more details.
**
**    You should have received a copy of the GNU General Public License
**    along with Chromis POS.  If not, see <http://www.gnu.org/licenses/>
**
**
*/

package uk.chromis.pos.cleandb;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import uk.chromis.basic.BasicException;
import uk.chromis.pos.forms.AppConfig;
import uk.chromis.pos.forms.AppLocal;
import uk.chromis.pos.forms.AppProperties;
import uk.chromis.pos.forms.JRootFrame;
import uk.chromis.pos.forms.StartPOS;


public class JFrmCreateClean extends javax.swing.JFrame {
    
    private JPanelCleandb config;
    
    public JFrmCreateClean(AppProperties props) {
        initComponents();
        
        try {
            this.setIconImage(ImageIO.read(JRootFrame.class.getResourceAsStream("/uk/chromis/fixedimages/smllogo.png")));
        } catch (IOException e) {
        }   
        setTitle(AppLocal.APP_NAME + " - " + AppLocal.APP_VERSION + " - " + AppLocal.getIntString("Menu.CreateCleanDB"));     
        addWindowListener(new MyFrameListener()); 
        
        config = new JPanelCleandb(props);        
        getContentPane().add(config, BorderLayout.CENTER);
       
        try {
            config.activate();
        } catch (BasicException e) { // never thrown ;-)
        }
    }
    
    private class MyFrameListener extends WindowAdapter{
        
        @Override
        public void windowClosing(WindowEvent evt) {
            if (config.deactivate()) {
                dispose();
            }
        }
        @Override
        public void windowClosed(WindowEvent evt) {
            System.exit(0);
        }
    }    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);

        setSize(new java.awt.Dimension(702, 591));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents
    
    /**
     * @param args the command line arguments
     */
    public static void main(final String args[]) {
     String currentPath = null;

        currentPath = System.getProperty("user.dir");

        //delet log files older than 50 days
        File folder = new File(currentPath+"/cssStyles");
        if (folder.exists()) {
            File[] listFiles = folder.listFiles();            
            long eligibleForDeletion = System.currentTimeMillis() - 432000000L;
            for (File listFile : listFiles) {
                if (listFile.getName().endsWith("log")
                        && listFile.lastModified() < eligibleForDeletion) {
                    if (!listFile.delete()) {
                        System.out.println("Sorry Unable to Delete Files..");
                    }
                }
            }
        }

        //send output to log files
        try {
            System.setOut(new PrintStream(new FileOutputStream(currentPath + "/Logs/Cleandb.log")));
            System.setErr(new PrintStream(new FileOutputStream(currentPath + "/Logs/Cleandbserror.log")));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(StartPOS.class.getName()).log(Level.SEVERE, null, ex);
        }

        File newIcons = null;
        if (AppConfig.getInstance().getProperty("icon.colour") == null || AppConfig.getInstance().getProperty("icon.colour").equals("")) {
            newIcons = new File(currentPath + "/Icon sets/blue/images.jar");
        } else {
            newIcons = new File(currentPath + "/Icon sets/" + AppConfig.getInstance().getProperty("icon.colour") + "/images.jar");
        }
        // File icons = new File(currentPath + "/lib");
        try {
            URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Method m = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
            m.setAccessible(true);
            m.invoke(urlClassLoader, newIcons.toURI().toURL());
            String cp = System.getProperty("java.class.path");
            if (cp != null) {
                cp += File.pathSeparatorChar + newIcons.getCanonicalPath();
            } else {
                cp = newIcons.toURI().getPath();
            }
            System.setProperty("java.class.path", cp);
        } catch (Exception ex) {
        }
        
        
        
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                
                AppConfig config = AppConfig.getInstance();
                new JFrmCreateClean(config).setVisible(true);                
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    
}
