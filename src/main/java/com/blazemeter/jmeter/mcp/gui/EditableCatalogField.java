package com.blazemeter.jmeter.mcp.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

/**
 * Editable text field with an optional dropdown of synced catalog values and a
 * Sync action button.
 */
public final class EditableCatalogField extends JPanel {

    private final JComboBox<String> combo;
    private final JButton syncButton;

    public EditableCatalogField(int columns, String syncLabel) {
        super(new GridBagLayout());
        combo = new JComboBox<>();
        combo.setEditable(true);
        if (combo.getEditor().getEditorComponent() instanceof javax.swing.JTextField editor) {
            editor.setColumns(columns);
        }
        syncButton = new JButton(syncLabel);
        GridBagConstraints c = GridBagForm.horizontalRowConstraints();
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(combo, c);
        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        add(syncButton, c);
    }

    public JButton getSyncButton() {
        return syncButton;
    }

    public String getText() {
        Object selected = combo.getSelectedItem();
        if (selected != null) {
            return selected.toString();
        }
        return "";
    }

    public void setText(String value) {
        String text = value == null ? "" : value;
        combo.removeAllItems();
        if (!text.isEmpty()) {
            combo.addItem(text);
        }
        combo.setSelectedItem(text);
        if (combo.getSelectedItem() == null && !text.isEmpty()) {
            combo.addItem(text);
            combo.setSelectedItem(text);
        }
    }

    public void setChoices(List<String> choices, String selectValue) {
        String current = getText();
        combo.removeAllItems();
        if (choices != null) {
            for (String choice : choices) {
                combo.addItem(choice);
            }
        }
        String target = selectValue != null ? selectValue : current;
        if (target != null && !target.isEmpty()) {
            boolean found = false;
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (target.equals(combo.getItemAt(i))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                combo.addItem(target);
            }
            combo.setSelectedItem(target);
        } else if (combo.getItemCount() > 0) {
            combo.setSelectedIndex(0);
        }
    }
}
