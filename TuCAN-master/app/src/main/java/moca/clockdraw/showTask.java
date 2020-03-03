package moca.clockdraw;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pen.Spen;
import com.samsung.android.sdk.pen.document.SpenNoteDoc;
import com.samsung.android.sdk.pen.document.SpenObjectBase;
import com.samsung.android.sdk.pen.document.SpenObjectStroke;
import com.samsung.android.sdk.pen.document.SpenPageDoc;
import com.samsung.android.sdk.pen.document.SpenUnsupportedTypeException;
import com.samsung.android.sdk.pen.document.SpenUnsupportedVersionException;
import com.samsung.android.sdk.pen.engine.SpenLayeredReplayListener;
import com.samsung.android.sdk.pen.engine.SpenSurfaceView;

import java.io.IOException;
import java.util.ArrayList;

import static com.samsung.android.sdk.pen.document.SpenPageDoc.BACKGROUND_IMAGE_MODE_TILE;

public class showTask extends Activity {
    private Rect rect;
    private Context context;
    private SpenSurfaceView sView;
    private SpenNoteDoc noteDoc;
    private SpenPageDoc pageDoc;

    private int canvasHeight, canvasWidth, originalCanvasHeight, originalCanvasWidth;

    private LinearLayout currLayout;
    private Button replayButton;
    Button back;

    private long totalDrawingDuration;

    private double processedReplayTime = 0.0d;
    private int replayPercentage = 0;
    TextView timeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String taskString = (String) getIntent().getExtras().get("task");

        int isTabletOptimized = 0;

        switch (taskString) {
            case "task1":
                isTabletOptimized = MemoryHelper.task1TimeAndScreenData.TabletOptimized;

                originalCanvasHeight = MemoryHelper.task1TimeAndScreenData.getOriginalViewHeight();
                originalCanvasWidth = MemoryHelper.task1TimeAndScreenData.getOriginalViewWidth();

                totalDrawingDuration = MemoryHelper.task1TimeAndScreenData.getTimeInAir() +
                        MemoryHelper.task1TimeAndScreenData.getTimeOnSurface();
                if (isTabletOptimized == 1) {
                    setContentView(R.layout.activity_show_task_optimized);
                } else {
                    setContentView(R.layout.activity_show_task1);
                }

                currLayout = (LinearLayout) findViewById(R.id.spenViewLayout);
                replayButton = (Button) findViewById(R.id.replayButton);
                back = (Button) findViewById(R.id.backButton);
                timeView = (TextView) findViewById(R.id.timeView);
                break;

            case "task2":
                isTabletOptimized = MemoryHelper.task2TimeAndScreenData.TabletOptimized;

                originalCanvasHeight = MemoryHelper.task2TimeAndScreenData.getOriginalViewHeight();
                originalCanvasWidth = MemoryHelper.task2TimeAndScreenData.getOriginalViewWidth();

                totalDrawingDuration = MemoryHelper.task2TimeAndScreenData.getTimeInAir() +
                        MemoryHelper.task2TimeAndScreenData.getTimeOnSurface();

                //Checking if o tablet optimzied Version is chosen
                if (isTabletOptimized == 1) {
                    setContentView(R.layout.activity_show_task_optimized);
                } else {
                    setContentView(R.layout.activity_show_task);
                }

                currLayout = (LinearLayout) findViewById(R.id.spenViewLayout);
                replayButton = (Button) findViewById(R.id.replayButton);
                back = (Button) findViewById(R.id.backButton);
                timeView = (TextView) findViewById(R.id.timeView);
                break;

            case "task3":
                isTabletOptimized = MemoryHelper.task3TimeAndScreenData.TabletOptimized;

                originalCanvasHeight = MemoryHelper.task3TimeAndScreenData.getOriginalViewHeight();
                originalCanvasWidth = MemoryHelper.task3TimeAndScreenData.getOriginalViewWidth();

                totalDrawingDuration = MemoryHelper.task3TimeAndScreenData.getTimeInAir() +
                        MemoryHelper.task3TimeAndScreenData.getTimeOnSurface();

                //Checking if o tablet optimzied Version is chosen
                if (isTabletOptimized == 1) {
                    setContentView(R.layout.activity_show_task_optimized);
                } else {
                    setContentView(R.layout.activity_show_task);
                }

                currLayout = (LinearLayout) findViewById(R.id.spenViewLayout);
                replayButton = (Button) findViewById(R.id.replayButton);
                back = (Button) findViewById(R.id.backButton);
                timeView = (TextView) findViewById(R.id.timeView);
                break;

            case "tremoranalyse1":
                isTabletOptimized = MemoryHelper.tremor1TimeAndScreenData.TabletOptimized;

                totalDrawingDuration = MemoryHelper.tremor1TimeAndScreenData.getTimeInAir() +
                        MemoryHelper.tremor1TimeAndScreenData.getTimeOnSurface();

                originalCanvasHeight = MemoryHelper.tremor1TimeAndScreenData.getOriginalViewHeight();
                originalCanvasWidth = MemoryHelper.tremor1TimeAndScreenData.getOriginalViewWidth();

                //Checking if o tablet optimzied Version is chosen
                if (isTabletOptimized == 1) {
                    setContentView(R.layout.activity_show_task_optimized);
                } else {
                    setContentView(R.layout.activity_show_tremoranalyse);
                }

                currLayout = (LinearLayout) findViewById(R.id.spenViewLayout);
                replayButton = (Button) findViewById(R.id.replayButton);
                back = (Button) findViewById(R.id.backButton);
                timeView = (TextView) findViewById(R.id.timeView);
                break;

            case "tremoranalyse2":
                isTabletOptimized = MemoryHelper.tremor2TimeAndScreenData.TabletOptimized;

                originalCanvasHeight = MemoryHelper.tremor2TimeAndScreenData.getOriginalViewHeight();
                originalCanvasWidth = MemoryHelper.tremor2TimeAndScreenData.getOriginalViewWidth();

                totalDrawingDuration = MemoryHelper.tremor2TimeAndScreenData.getTimeInAir() +
                        MemoryHelper.tremor2TimeAndScreenData.getTimeOnSurface();

                //Checking if o tablet optimzied Version is chosen
                if (isTabletOptimized == 1) {
                    setContentView(R.layout.activity_show_task_optimized);
                } else {
                    setContentView(R.layout.activity_show_tremoranalyse);
                }

                currLayout = (LinearLayout) findViewById(R.id.spenViewLayout);
                replayButton = (Button) findViewById(R.id.replayButton);
                back = (Button) findViewById(R.id.backButton);
                timeView = (TextView) findViewById(R.id.timeView);
                break;
        }

        context = this;

        canvasHeight = 0;
        canvasWidth = 0;

        Spen pen = new Spen();
        try {
            pen.initialize(this);
        } catch (SsdkUnsupportedException e) {
            Toast.makeText(context, "Fehler bei Initialisierung von SPen: Gerät nicht unterstützt", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }

        // Create SPen Surface:
        sView = new SpenSurfaceView(context);

        /*
        Add Replay Stuff:
         */
        replayButton.setClickable(true);
        replayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //deactivateButton(replayButton); // to avoid unexpected behavior from multiple clicks

                // Actually set the replay speed for safety reasons:
                //Speeds: 0 = SLOW, 1 = NORMAL, 2 = FAST
                sView.setReplaySpeed(1);

                ArrayList<SpenObjectBase> strokes = pageDoc.getObjectList();
                for (SpenObjectBase obj : strokes) {
                    SpenObjectStroke stroke = (SpenObjectStroke) obj;
                    if (!stroke.isVisible()) {
                        stroke.setVisibility(true);
                        stroke.setAdvancedPenSetting("2");
                        // the following makes the hover line thinner: cant't explain why, but does the trick
                        stroke.setPenName("com.samsung.android.sdk.pen.pen.preload.Marker");
                        stroke.setPenSize(0.1f);
                    }
                }

                // reactivate the buttons as soon as the replay has finished:
                sView.setReplayListener(new SpenLayeredReplayListener() {
                    @Override
                    public void onProgressChanged(int i, int i1, int i2) {
                        // i = 'percentage of already played replay animation'
                        // i1 = 'no fucking clue'
                        // i2 = 'object ID that is currently being drawn. ID seems to be arbitrary. Could be useful to distinguish between objects'
                        if (i != replayPercentage) {
                            processedReplayTime += (double) totalDrawingDuration / 100.0;
                            replayPercentage = i;

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String timeString = String.valueOf(Math.round(processedReplayTime));
                                    timeString += " ms";

                                    timeView.setText(timeString);
                                }
                            });
                        }
                    }

                    @Override
                    public void onCompleted() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //activateButton(replayButton);
                                replayButton.setText("Play");
                                processedReplayTime = 0;
                                replayPercentage = 0;
                            }
                        });
                    }
                });

                sView.update();

                if (sView.getReplayState() == SpenSurfaceView.REPLAY_STATE_STOPPED) {
                    replayButton.setText("Pause");
                    sView.startReplay();
                } else if (sView.getReplayState() == SpenSurfaceView.REPLAY_STATE_PLAYING) {
                    replayButton.setText("Play");
                    sView.pauseReplay();
                } else {
                    replayButton.setText("Pause");
                    sView.resumeReplay();
                }
            }
        });


        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sView.getReplayState() != SpenSurfaceView.REPLAY_STATE_STOPPED) {
                    sView.stopReplay();
                }

                finish();
            }
        });

        /*
        End of actual initialization:
            -add spensurfaceView to current activitys' view and display everything
            -add the current pageDoc to save all strokes about to be drawn
            -disallow any action triggered by finger touch events
         */
        currLayout.addView(sView);

        Display currDisplay = getWindowManager().getDefaultDisplay();
        rect = new Rect();
        currDisplay.getRectSize(rect);

        // Create SPen Doc to save drawing data:
        try {
            // dummy-init noteDoc to avoid null pointers:
            noteDoc = new SpenNoteDoc(context, rect.width(), rect.height());
            try {
                String spdFile = (String) getIntent().getExtras().get("ordner");
                noteDoc = new SpenNoteDoc(context, spdFile, rect.width(), SpenNoteDoc.MODE_READ_ONLY);

                /*
                A noteDoc recorded in landscape screen mode does not at all scale while being viewed
                in portrait mode. However, vice-versa seems to be fine. No idea why.
                The following prevents major view issues by forcing landscape orientation while
                viewing landscape-recorded noteDocs:
                 */
                if (noteDoc.getOrientation() == SpenNoteDoc.ORIENTATION_LANDSCAPE) {
                    showTask.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                }

            } catch (SpenUnsupportedTypeException | SpenUnsupportedVersionException e) {
                e.printStackTrace();
            }

            pageDoc = noteDoc.getPage(noteDoc.getLastEditedPageIndex());

        } catch (IOException e) {
            Toast.makeText(context, "Task Daten nicht vorhanden", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }

        sView.setPageDoc(pageDoc, true);
        sView.setToolTypeAction(SpenSurfaceView.TOOL_FINGER, SpenSurfaceView.ACTION_NONE);
        sView.setToolTypeAction(SpenSurfaceView.TOOL_SPEN, SpenSurfaceView.ACTION_NONE);
        sView.setToolTypeAction(SpenSurfaceView.TOOL_PEN_BUTTON, SpenSurfaceView.ACTION_NONE);

        sView.update();

        // Set the layout listener to get the actual size of the sView drawing area:
        sView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                canvasWidth = sView.getWidth();
                canvasHeight = sView.getHeight();

                if (canvasHeight > 0 && canvasHeight > 0) {
                    scaleDrawing();
                }

                sView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    /**
     * Scales the drawing to fit the current SPenSurfaceView canvas area.
     */
    private void scaleDrawing() {
        float xScaleFactor = (float) canvasWidth / originalCanvasWidth * 1.0f;
        float yScaleFactor = (float) canvasHeight / originalCanvasHeight * 1.0f;

        ArrayList<SPenData> currTaskData = null;
        String taskString = (String) getIntent().getExtras().get("task");
        switch (taskString) {
            case "task1":
                currTaskData = MemoryHelper.task1data;

                /*
                If Task 1 was optimized, the Background Image needs also to be rescaled:
                */
                int isTabletOptimized = MemoryHelper.task1TimeAndScreenData.TabletOptimized;
                if (isTabletOptimized == 1){
                pageDoc.setBackgroundImageMode(BACKGROUND_IMAGE_MODE_TILE);// Keeps original size of image, repeats image if neccessary
                Bitmap bImage = pageDoc.getBackgroundImage();
                int oldWidth = bImage.getWidth();
                int oldHeight = bImage.getHeight();
                //Rescale Backgroungs Image
                Bitmap resizedbImage = Bitmap.createScaledBitmap(bImage, (int) (oldWidth*xScaleFactor), (int) (oldHeight*yScaleFactor), false);

                //Change Background Image (only in view, not in spd)
                pageDoc.setVolatileBackgroundImage(resizedbImage);}
                break;

            case "task2":
                currTaskData = MemoryHelper.task2data;
                break;

            case "task3":
                currTaskData = MemoryHelper.task3data;
                break;

            case "tremoranalyse1":
                currTaskData = MemoryHelper.tremoranalyse1data;
                break;

            case "tremoranalyse2":
                currTaskData = MemoryHelper.tremoranalyse2data;
                break;
        }

        int taskDataIndex = 0;
        if (xScaleFactor != 1.0f || yScaleFactor != 1.0f) {
            for (SpenObjectBase obj : pageDoc.getObjectList()) {
                SpenObjectStroke stroke = (SpenObjectStroke) obj;
                PointF[] points = stroke.getPoints();
                for (PointF point : points) {
                    point.set(currTaskData.get(taskDataIndex).getxCoord(), currTaskData.get(taskDataIndex).getyCoord());
                    point.set(point.x * xScaleFactor, point.y * yScaleFactor);
                    taskDataIndex++;
                }
                stroke.setPoints(points, stroke.getPressures(), stroke.getTimeStamps());
            }
            sView.update();
            Toast.makeText(context, "Skalierung auf veränderte Pixeldichte abgeschlossen.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sView != null) {
            sView.close();
            sView = null;
        }
        if (noteDoc != null) {
            try {
                noteDoc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            noteDoc = null;
        }
    }

    /**
     * Can be used to reactivate a button that has been deactivated via {@link #deactivateButton(Button)}.
     *
     * @param but The button to reactivate
     */
    private void activateButton(Button but) {
        but.setEnabled(true);
        but.setClickable(true);
        but.setAlpha(1.0f);
    }

    /**
     * Deactivates a button by disabling its clickable attribute and changing its appearance.
     *
     * @param but The button that shall be deactivated
     */
    private void deactivateButton(Button but) {
        but.setEnabled(false);
        but.setClickable(false);
        but.setAlpha(0.5f);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.recreate();
    }

    /**
     * Make all Hover lines (red-colored strokes) invisible
     */
    private void unvisualizeHoverLines() {
        for(SpenObjectBase obj : pageDoc.getObjectList()) {
            if(obj instanceof SpenObjectStroke) {
                SpenObjectStroke str = (SpenObjectStroke) obj;

                if(str.getColor() == Color.RED) {
                    str.setVisibility(false);
                }
            }
        }

        sView.closeControl();
        sView.update();
    }



    @Override
    public void onBackPressed() {
        unvisualizeHoverLines();
        super.onBackPressed();
    }
}