package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class GameJFrame extends JFrame implements PropertyChangeListener {

    private JPanel contentPane;
    private BorderLayout borderLayout1 = new BorderLayout();

    public GameJFrame() {
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        try {
            initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Component initialization
    private void initialize() throws Exception {
        contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout(borderLayout1);
    }

    @Override
    //Overridden so we can exit when window is closed
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            System.exit(0);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

    }
}
