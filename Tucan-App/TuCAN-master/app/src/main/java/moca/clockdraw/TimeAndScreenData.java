package moca.clockdraw;

public class TimeAndScreenData {
    public long TimeOnSurface;
    public long TimeInAir;
    public long TimeOffScreen;
    public int TabletOptimized;

    private int originalViewWidth;
    private int originalViewHeight;


    public TimeAndScreenData() {    }

    public TimeAndScreenData(long timeOnSurface, long timeInAir, long timeOffScreen, int tabletOptimized) {
        this.TimeOnSurface = timeOnSurface;
        this.TimeInAir = timeInAir;
        this.TimeOffScreen = timeOffScreen;
        this.TabletOptimized = tabletOptimized;
    }

    public long getTimeOnSurface() { return TimeOnSurface; }

    public long getTimeInAir() {
        return TimeInAir;
    }

    public long getTimeOffScreen() {
        return TimeOffScreen;
    }

    public int getTabletOptimized() {
        return TabletOptimized;
    }



    public void setTimeOnSurface(long timeOnSurface) {
        TimeOnSurface = timeOnSurface;
    }

    public void setTimeInAir(long timeInAir) {
        TimeInAir = timeInAir;
    }

    public void setTimeOffScreen(long timeOffScreen) {
        TimeOffScreen = timeOffScreen;
    }

    public void setTabletOptimized(int tabletOptimized) { TabletOptimized = tabletOptimized; }

    public int getOriginalViewHeight() {
        return originalViewHeight;
    }

    public void setOriginalViewHeight(int originalViewHeight) {
        this.originalViewHeight = originalViewHeight;
    }

    public int getOriginalViewWidth() {
        return originalViewWidth;
    }

    public void setOriginalViewWidth(int originalViewWidth) {
        this.originalViewWidth = originalViewWidth;
    }


}
