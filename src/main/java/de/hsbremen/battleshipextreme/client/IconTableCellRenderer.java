package de.hsbremen.battleshipextreme.client;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Created on 02.06.2015.
 * Wird ben�tigt im die Schlossicons in dem JTable Eintr�gen anzuzeigen
 */
class IconTableCellRenderer extends DefaultTableCellRenderer {
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof ImageIcon) {
            label.setText(null);
            label.setIcon((ImageIcon) value);
        } else {
            label.setIcon(null);
        }
        return label;
    }
}
