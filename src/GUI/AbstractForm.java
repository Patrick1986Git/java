package GUI;

import javax.swing.*;
import java.awt.*;

/**
 * Bazowy formularz — podstawowe ustawienia wygląda i układu.
 */
public abstract class AbstractForm<T> extends JPanel {
    protected final JFrame owner;

    protected AbstractForm(JFrame owner) {
        this.owner = owner;
        initLayout();
    }

    private void initLayout() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    public abstract void setEntity(T entity);
    public abstract T getEntity();
}