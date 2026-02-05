package metroline.util.ui;

import metroline.util.localizate.ITranslatable;
import metroline.util.localizate.LngUtil;

import javax.swing.*;

public class MetrolineLabel extends JLabel implements ITranslatable {
    private final String translationKey;

    public MetrolineLabel(String translationKey) {
        super(LngUtil.translatable(translationKey), 0);
        this.translationKey = translationKey;
    }
    public MetrolineLabel(String translationKey, int alignment) {
        super(LngUtil.translatable(translationKey), alignment);
        this.translationKey = translationKey;
    }

    @Override
    public void updateTranslation() {
        setText(LngUtil.translatable(translationKey));
    }
}