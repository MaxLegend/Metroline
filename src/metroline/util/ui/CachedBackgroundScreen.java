package metroline.util.ui;

import java.awt.*;

public interface CachedBackgroundScreen {
    void setBackgroundImage(Image image);
    void setBackgroundLoaded(boolean loaded);
    void setBackgroundSize(int width, int height);
    Image getBackgroundImage();
    boolean isBackgroundLoaded();
    int getBackgroundWidth();
    int getBackgroundHeight();
}
