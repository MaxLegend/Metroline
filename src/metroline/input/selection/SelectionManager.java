package metroline.input.selection;

import java.util.ArrayList;
import java.util.List;

import static metroline.MainFrame.SOUND_ENGINE;

// FIX Баги с выбиралкой. Перепроверить все ситуации
// MAYBE Сделать подсветку по наведению
public class SelectionManager {
    private static SelectionManager instance;
    private Selectable selectedObject;
    private final List<SelectionListener> listeners = new ArrayList<>();

    private SelectionManager() {}

    public static SelectionManager getInstance() {
        if (instance == null) {
            instance = new SelectionManager();
        }
        return instance;
    }

    public void select(Selectable object) {
        Selectable previous = this.selectedObject;
        this.selectedObject = object;

        // Уведомляем слушателей
        for (SelectionListener listener : listeners) {
            listener.onSelectionChanged(previous, object);
        }
    }

    public void deselect() {
        Selectable previous = this.selectedObject;
        this.selectedObject = null;

        for (SelectionListener listener : listeners) {
            listener.onSelectionChanged(previous, null);
        }
    }

    public Selectable getSelected() {
        return selectedObject;
    }

    public boolean isSelected(Selectable object) {
        return selectedObject != null &&
                selectedObject.getSelectionId().equals(object.getSelectionId());
    }

    public void addListener(SelectionListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SelectionListener listener) {
        listeners.remove(listener);
    }
}
