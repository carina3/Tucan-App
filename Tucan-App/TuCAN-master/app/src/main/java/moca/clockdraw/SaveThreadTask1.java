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
 * Created by Sylvia on 04.12.2017.
 */

public class SaveThreadTask1 extends SaveThread {
    private int timeOnPoints;
    private  TMT tmt;
    SaveThreadTask1(File ordner, Context context, SpenNoteDoc noteDoc, SpenSurfaceView sView, ArrayList<Integer> events, ArrayList<SpenObjectBase> strokeList, String classname, long timeInAir, long timeOnSurface, long timeOffScreen, int tabletOptimized, ArrayList<Integer> eventTimeStamps, Handler handler, int timeOnPoints, TMT tmt) {
        super(ordner, context, noteDoc, sView, events, strokeList, classname, timeInAir, timeOnSurface, timeOffScreen, tabletOptimized, eventTimeStamps, handler);
        this.timeOnPoints = timeOnPoints;
        this.tmt = tmt;
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
                timeOnSurface, timeOffScreen, tabletOptimized, this.origScreenWidth, this.origScreenHeight, timeOnPoints, tmt);

        // Inform the parent thread that saving is complete and thus processing to the next
        // activity is necessary:
        Message message = Message.obtain();
        message.obj = "saving_finished";
        parentHandler.dispatchMessage(message);
    }

}
