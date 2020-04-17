package org.jsun.stax.ext;

import com.ibm.staf.service.stax.STAXActionDefaultImpl;
import com.ibm.staf.service.stax.STAXCondition;
import com.ibm.staf.service.stax.STAXPythonEvaluationException;
import com.ibm.staf.service.stax.STAXThread;
import org.pmw.tinylog.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class ExtDialogAction extends STAXActionDefaultImpl {

    static final int INIT = 0;

    static final int WAIT = 1;

    static final int COMPLETE = 2;

    // JDialog attrs
    private String title = "";
    private String label;
    private String width;
    private String height;
    private String x;
    private String y;
    private String text;

    private String currentBlockName = "";

    private ExtDialogActionFactory actionFactory;
    private STAXThread staxThread = null;

    private int state = INIT;

    private ExtDialog dialog;

    private List<String> buttons = new ArrayList<>();

    public ExtDialogAction() {
    }


    @Override
    public void handleCondition(STAXThread thread, STAXCondition condition) {
        thread.popAction();
    }

    @Override
    public synchronized void execute(STAXThread thread) {
        if (state == INIT) {
            if (thread != null) {
                staxThread = thread;
                try {
                    currentBlockName = thread.pyStringEval("STAXCurrentBlock");
                } catch (STAXPythonEvaluationException e) {
                    currentBlockName = "";  //Shouldn't happen
                }
            }

            setLookAndFeel();
            /* Create and display the dialog */
            showDialog(buttons.stream().map(text -> new JButton(text)).collect(Collectors.toList()));
        }
    }

    private void showDialog(List<JButton> buttonList) {
        EventQueue.invokeLater(() -> {
            dialog = new ExtDialog(buttonList);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    //System.exit(0);
                }
            });
            dialog.setVisible(true);
        });
    }

    private void setLookAndFeel() {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ExtDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ExtDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ExtDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ExtDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }

    public void addButton(String button) {
        this.buttons.add(button);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ExtDialogActionFactory getActionFactory() {
        return actionFactory;
    }

    public void setActionFactory(ExtDialogActionFactory actionFactory) {
        this.actionFactory = actionFactory;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public static void main(String args[]) {
        ExtDialogAction extDialogAction = new ExtDialogAction();
        extDialogAction.addButton("btn2");
        extDialogAction.addButton("btn3");
        extDialogAction.addButton("btn4");
        extDialogAction.addButton("btn1");
        extDialogAction.execute(null);
    }
}
