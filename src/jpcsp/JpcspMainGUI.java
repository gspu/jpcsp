/*
    This file is part of jpcsp.

    Jpcsp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Jpcsp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */

package jpcsp;

import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import jpcsp.Debugger.Disasembler;

/**
 *
 * @author  George
 */
public class JpcspMainGUI extends javax.swing.JFrame {
    ElfHeaderInfo elfinfo; 
    Disasembler dis;
    /** Creates new form JpcspMainGUI */
    public JpcspMainGUI() {
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        desktopPane = new javax.swing.JDesktopPane();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        Windows = new javax.swing.JMenu();
        Disasembler = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Jpcsp v0.01");
        setLocationByPlatform(true);

        desktopPane.setBackground(new java.awt.Color(204, 204, 255));

        fileMenu.setText("File");

        openMenuItem.setText("Open Elf File");
        openMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openMenuItem);

        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        Windows.setText("Windows");
        Windows.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                WindowsActionPerformed(evt);
            }
        });

        Disasembler.setText("Disasembler");
        Disasembler.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DisasemblerActionPerformed(evt);
            }
        });
        Windows.add(Disasembler);

        menuBar.add(Windows);

        helpMenu.setText("Help");

        aboutMenuItem.setText("About");
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(desktopPane, javax.swing.GroupLayout.DEFAULT_SIZE, 779, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(desktopPane, javax.swing.GroupLayout.DEFAULT_SIZE, 557, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

private void openMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuItemActionPerformed
    boolean isloaded = false; // variable to check if user at least choose something   
    final JFileChooser fc = new JFileChooser();
       fc.setDialogTitle("Open Elf File");
       fc.setCurrentDirectory(new java.io.File("."));
       int returnVal = fc.showOpenDialog(desktopPane);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            
            //This is where a real application would open the file.   
          try {
            ElfHeader.readHeader(file.getPath());
            isloaded=true; //TODO check if it a valid file
          }
          catch(IOException e)
          {
            e.printStackTrace();
          }
        } else {
           //user cancel the action
           
        }
       if(isloaded)
       {
        //TODO: ADD CHECK IF window is already open.
        //elf info window
         elfinfo = new ElfHeaderInfo();
        elfinfo.setLocation(0, 0);      
        elfinfo.setVisible(true);
        
       desktopPane.add( elfinfo);
       try {
             elfinfo.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {}
       //disassembler window
       dis=new Disasembler();
       dis.setLocation(300, 0); 
       dis.setVisible(true);
       desktopPane.add(dis);
       try {
             dis.setSelected(true);
             dis.RefreshDebugger();
        } catch (java.beans.PropertyVetoException e) {}
       }
}//GEN-LAST:event_openMenuItemActionPerformed

private void DisasemblerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DisasemblerActionPerformed
        //TODO: ADD CHECK IF window is already open.
        dis.setLocation(300, 0); 
        if(dis.isVisible())
        dis.setVisible(false);
        else
          dis.setVisible(true);  
}//GEN-LAST:event_DisasemblerActionPerformed

private void WindowsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_WindowsActionPerformed
// TODO add your handling code here:
}//GEN-LAST:event_WindowsActionPerformed

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      }
       catch (Exception e) {
          e.printStackTrace();
        }
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new JpcspMainGUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem Disasembler;
    private javax.swing.JMenu Windows;
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JDesktopPane desktopPane;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem openMenuItem;
    // End of variables declaration//GEN-END:variables

}
