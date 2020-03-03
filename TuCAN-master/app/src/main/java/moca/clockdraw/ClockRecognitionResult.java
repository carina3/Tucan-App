package moca.clockdraw;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pen.Spen;
import com.samsung.android.sdk.pen.document.SpenNoteDoc;
import com.samsung.android.sdk.pen.document.SpenObjectBase;
import com.samsung.android.sdk.pen.document.SpenObjectStroke;
import com.samsung.android.sdk.pen.document.SpenPageDoc;
import com.samsung.android.sdk.pen.document.SpenUnsupportedTypeException;
import com.samsung.android.sdk.pen.document.SpenUnsupportedVersionException;
import com.samsung.android.sdk.pen.engine.SpenSurfaceView;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Activity that shows the clock on the left side of the screen. On the right side is a html
 * view displayling the recognition result.
 */
public class ClockRecognitionResult extends Activity {
    private Rect rect;
    private Context context;
    private SpenSurfaceView sView;
    private SpenNoteDoc noteDoc;
    private SpenPageDoc pageDoc;
    private WebView resultView;

    private int canvasHeight, canvasWidth, originalCanvasHeight, originalCanvasWidth;

    private LinearLayout currLayout;

    private Handler recognitionFinishHandler;
    private ProgressDialog spinner;

    private String timeString;

    private ClockRecognizer clockRater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // string which had to be drawn in the clock test:
        timeString = getIntent().getExtras().getString("timeString");

        int isTabletOptimized = 0;
        isTabletOptimized = MemoryHelper.task3TimeAndScreenData.TabletOptimized;

        originalCanvasHeight = MemoryHelper.task3TimeAndScreenData.getOriginalViewHeight();
        originalCanvasWidth = MemoryHelper.task3TimeAndScreenData.getOriginalViewWidth();

        //Checking if tablet optimzied Version is chosen
        if (isTabletOptimized == 1) {
            setContentView(R.layout.activity_clockdraw_ai_optimized);
        } else {
            setContentView(R.layout.activity_clockdraw_ai);
        }

        currLayout = (LinearLayout) findViewById(R.id.spenViewLayout);
        resultView = (WebView) findViewById(R.id.ai_result_view);

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

        currLayout.addView(sView);

        Display currDisplay = getWindowManager().getDefaultDisplay();
        rect = new Rect();
        currDisplay.getRectSize(rect);

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
                    ClockRecognitionResult.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
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

                // start the recognition process when the building of the layout has been finished:
                clockRater = new ClockRecognizer(context, sView, pageDoc, timeString, recognitionFinishHandler, canvasWidth, canvasHeight);
                spinner.show();
                clockRater.startRecognition();

            }
        });

        spinner = new ProgressDialog(context);
        spinner.setIndeterminate(true);
        spinner.setCancelable(true);
        spinner.setTitle("Uhrenerkennung läuft");
        spinner.setMessage("Bitte warten...");
        spinner.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        // init the handler that updates the view as soon as the recognition has finished its work:
        recognitionFinishHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if(!msg.obj.equals("ai_finished"))
                    return false;

                else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String outputFormatted = "<html><body>" + clockRater.htmlOut + "</body></html>";
                            resultView.loadData(outputFormatted, "text/html", null);
                        }
                    });

                    clockRater.close();
                    clockRater = null;

                    spinner.dismiss();

                    return true;
                }
            }
        });

    }

    /**
     * Scales the drawing to fit the current SPenSurfaceView canvas area.
     *
     */
    private void scaleDrawing() {
        float xScaleFactor = (float) canvasWidth / originalCanvasWidth * 1.0f;
        float yScaleFactor = (float) canvasHeight / originalCanvasHeight * 1.0f;

        ArrayList<SPenData> currTaskData = null;

        currTaskData = MemoryHelper.task3data;

        int taskDataIndex = 0;
        if (xScaleFactor != 1.0f || yScaleFactor != 1.0f) {
            for (SpenObjectBase obj : pageDoc.getObjectList()) {
                SpenObjectStroke stroke = (SpenObjectStroke) obj;
                PointF[] points = stroke.getPoints();
                for (PointF point : points) {
                    point.set(currTaskData.get(taskDataIndex).getxCoord(), currTaskData.get(taskDataIndex).getyCoord());
                    float x = point.x;
                    float y = point.y;
                    point.set(point.x * xScaleFactor, point.y * yScaleFactor);
                    taskDataIndex++;
                }
                stroke.setPoints(points, stroke.getPressures(), stroke.getTimeStamps());
            }
            sView.update();
            Toast.makeText(context, "Skalierung auf veränderte Pixeldichte abgeschlossen!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(clockRater != null) {
            clockRater.close();
            clockRater = null;
        }
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

    @Override
    public void onBackPressed() {
        if(clockRater != null) {
            clockRater.reset();
        }
        super.onBackPressed();
    }

}