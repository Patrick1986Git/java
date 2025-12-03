package GUI;

/**
 * Bazowy controller dla formularzy.
 */
public abstract class AbstractFormController<T> {
    public abstract void onSave(T entity);
    public abstract void onCancel();
}