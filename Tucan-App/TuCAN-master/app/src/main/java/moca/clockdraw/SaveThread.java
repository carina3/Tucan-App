package moca.clockdraw;

import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.samsung.android.sdk.pen.document.SpenNoteDoc;
import com.samsung.android.sdk.pen.document.SpenObjectBase;
import com.samsung.android.sdk.pen.document.SpenObjectStroke;
import com.samsung.android.sdk.pen.engine.SpenSurfaceView;

import java.io.File;
import java.util.ArrayList;

/**
 * A Thread that manages the saving of all necessary task data to .csv file.
 * Improves performance and thus user experience compared to perform saving on main thread.
 * Informs the callee (parent activity) via Handler when it has finished.
 *
 * Created by Jannik on 20.11.2017.
 */

public class SaveThread extends Thread {
    protected File ordner;
    protected Context context;
    protected SpenNoteDoc noteDoc;
    protected SpenSurfaceView sView;
    protected ArrayList<Integer> events;
    protected ArrayList<SpenObjectBase> strokeList;
    protected String classname;
    protected long timeInAir;
    protected long timeOnSurface;
    protected long timeOffScreen;
    protected int tabletOptimized;
    protected ArrayList<Integer> eventTimeStamps;
    protected Handler parentHandler;

    protected int origScreenWidth;
    protected int origScreenHeight;


    void setOrigScreenDimensions(int origScreenWidth, int origScreenHeight) {
        this.origScreenWidth = origScreenWidth;
        this.origScreenHeight = origScreenHeight;
    }

    SaveThread(File ordner, Context context, SpenNoteDoc noteDoc, SpenSurfaceView sView, ArrayList<Integer> events,
               ArrayList<SpenObjectBase> strokeList, String classname, long timeInAir, long timeOnSurface, long timeOffScreen,
               int tabletOptimized, ArrayList<Integer> eventTimeStamps, Handler handler) {
        this.ordner = ordner;
        this.context = context;
        this.noteDoc = noteDoc;
        this.sView = sView;
        this.events = events;
        this.classname = classname;
        this.timeInAir = timeInAir;
        this.timeOnSurface = timeOnSurface;
        this.timeOffScreen = timeOffScreen;
        this.tabletOptimized = tabletOptimized;
        this.eventTimeStamps = eventTimeStamps;
        this.parentHandler = handler;
        this.strokeList = strokeList;
    }

    @Override
    public void run(){
        Looper.prepare();

        // Set the actually obtained timestamps of all stroke points:
        int currentTimeStampsIndex = 0;
        SpenObjectStroke str;
        int[] newTimestamps;
        for (int strokeIndex = 0; strokeIndex < strokeList.size(); strokeIndex++) {
            str = (SpenObjectStroke) strokeList.get(strokeIndex);
            PointF[] oldPoints = str.getPoints();

            if (!str.isVisible()) { // => hover stoke. Hover strokes already do have the correct timestamps
                currentTimeStampsIndex += oldPoints.length;
                continue;
            }

            // else get the according timestamps from the list and set them to the onscreen stroke:
            newTimestamps = new int[oldPoints.length];
            for (int i = 0; i < newTimestamps.length; i++) {
                newTimestamps[i] = eventTimeStamps.get(currentTimeStampsIndex);
                currentTimeStampsIndex++;
            }

            str.setPoints(oldPoints, str.getPressures(), newTimestamps);
        }

        // Save all data to .csv file:
        MemoryHelper.saveCSVinThread(ordner, context, noteDoc, sView, events, strokeList, classname, timeInAir,
                timeOnSurface, timeOffScreen, tabletOptimized, this.origScreenWidth, this.origScreenHeight);

        // Inform the parent thread that saving is complete and thus processing to the next
        // activity is necessary:
        Message message = Message.obtain();
        message.obj = "saving_finished";
        parentHandler.dispatchMessage(message);
    }
}
