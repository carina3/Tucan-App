package moca.clockdraw;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pen.Spen;
import com.samsung.android.sdk.pen.SpenSettingPenInfo;
import com.samsung.android.sdk.pen.document.SpenNoteDoc;
import com.samsung.android.sdk.pen.document.SpenObjectBase;
import com.samsung.android.sdk.pen.document.SpenObjectStroke;
import com.samsung.android.sdk.pen.document.SpenPageDoc;
import com.samsung.android.sdk.pen.engine.SpenHoverListener;
import com.samsung.android.sdk.pen.engine.SpenSurfaceView;
import com.samsung.android.sdk.pen.engine.SpenTouchListener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class tremoranalyse1 extends Activity {

    TextView tv_aufgabe;
    private SpenObjectStroke currentHoverStroke;
    private int recordedHoverPoints = 0;
    private ArrayList<Integer> events;
    private ArrayList<Integer> eventTimeStamps;

    private Rect rect;
    private Context context;
    private SpenSurfaceView sView;
    private SpenNoteDoc noteDoc;
    private SpenPageDoc pageDoc;
    private SpenSettingPenInfo penSettings;

    private int canvasWidth, canvasHeight;

    private ArrayList<SpenObjectBase> strokes;
    private ProgressDialog spinner;
    private Handler handler;
    private File file_vpOrdner;

    public static long timeInAir;
    public static long timeOnSurface;
    public static long timeOffScreen;
    private long globalStart;
    private long lastTransitionTime;
    private long globalDuration;
    private boolean isFirstScreenContact;
    private boolean hasTaskEnded;

    private int resetNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set vpOrdner
        file_vpOrdner = (File) getIntent().getExtras().get("ordner");

        // Set layout to optimized or original version depending on preference:
        final SharedPreferences prefs = getSharedPreferences("com.moca.MODE_SETTINGS", Context.MODE_PRIVATE);
        final boolean shallOptimize = prefs.getBoolean("isTabletOptimized", false);
        final boolean rightHander = prefs.getBoolean("reightHander", false);

        String aufgabePortrait = "Zeichnen Sie eine Spirale";
        String aufgabeLandscape = "Zeichnen Sie\neine Spirale";

        if(shallOptimize) {
            if (rightHander){
                setContentView(R.layout.activity_task3_optimized_rechts);
            }else {
                setContentView(R.layout.activity_task3_optimized_links);
            }

        } else {
            // Deactivate Androids' status bar in the drawing Activities:
            View osUiView = getWindow().getDecorView();
            osUiView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN + View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY +
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

            setContentView(R.layout.activity_task3_original);
        }
        tv_aufgabe = (TextView) findViewById(R.id.aufgabe_tv);

        // Optimize layout for current screen rotation (i.e. landscape or portrait):
        int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
        if(shallOptimize && (displayRotation == Surface.ROTATION_90 || displayRotation == Surface.ROTATION_270)) {
            tv_aufgabe.setText(aufgabeLandscape);
        } else {
            tv_aufgabe.setText(aufgabePortrait);
        }

        context = this;

        canvasHeight = 0;
        canvasWidth = 0;

        resetNumber = 0;

        // set all spinner properties and the handler for the saving progress animation:
        spinner = new ProgressDialog(context);
        spinner.setIndeterminate(true);
        spinner.setCancelable(true);
        spinner.setMessage("Bitte warten...");
        spinner.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        Spen pen = new Spen();
        try {
            pen.initialize(this);
        } catch (SsdkUnsupportedException e) {
            Toast.makeText(context, "Fehler bei Initialisierung von SPen: Gerät nicht unterstützt", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }

        // Create SPen Surface:
        final LinearLayout currLayout = (LinearLayout) findViewById(R.id.spenViewLayout);
        sView = new SpenSurfaceView(context);

        // Set pen setting:
        penSettings = new SpenSettingPenInfo(sView.getPenSettingInfo());
        penSettings.name = "com.samsung.android.sdk.pen.pen.preload.Pencil";
        penSettings.size = MemoryHelper.actualPenSize;
        sView.setPenSettingInfo(penSettings);

        currentHoverStroke = new SpenObjectStroke(sView.getPenSettingInfo().name);

        //Initialize Time variables
        initTimeCounters();

        events = new ArrayList<>();
        eventTimeStamps = new ArrayList<>();

        // Add touch listener:
        sView.setTouchListener(new SpenTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
                    return false;
                }

                int currentEventTime = (int) motionEvent.getEventTime();

                switch (motionEvent.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        finishHoverStroke();

                        lastTransitionTime = currentEventTime;

                        break;
                    case MotionEvent.ACTION_UP:
                        timeOnSurface += (currentEventTime - lastTransitionTime);

                        break;
                    case MotionEvent.ACTION_MOVE:
                        for (int i = 0; i < motionEvent.getHistorySize(); i++) {
                            events.add(MotionEvent.ACTION_MOVE);
                            eventTimeStamps.add((int) motionEvent.getHistoricalEventTime(i));
                        }
                        break;
                }

                events.add(motionEvent.getActionMasked()); // add event type of actual event
                eventTimeStamps.add(currentEventTime);

                return true;
            }
        });

        // Add hover listener:
        sView.setHoverListener(new SpenHoverListener() {
            @Override
            public boolean onHover(View view, MotionEvent motionEvent) {
                int currentEventTime = (int) motionEvent.getEventTime();

                switch (motionEvent.getActionMasked()) {
                    case MotionEvent.ACTION_HOVER_ENTER:
                        //If Hover occurs the first time start record
                        if (isFirstScreenContact) {
                            globalStart = currentEventTime;
                            isFirstScreenContact = false;
                        }

                        lastTransitionTime = currentEventTime;

                        finishHoverStroke();
                        break;

                    case MotionEvent.ACTION_HOVER_EXIT:
                        finishHoverStroke();
                        timeInAir += (currentEventTime - lastTransitionTime);

                        break;

                    case MotionEvent.ACTION_HOVER_MOVE:
                        // save all historical pen positions as well:
                        for (int i = 0; i < motionEvent.getHistorySize(); i++) {
                            events.add(MotionEvent.ACTION_HOVER_MOVE);
                            float histX = motionEvent.getHistoricalX(i);
                            float histY = motionEvent.getHistoricalY(i);
                            int histTime = (int) motionEvent.getHistoricalEventTime(i);
                            PointF histPoint = new PointF(histX, histY);
                            currentHoverStroke.addPoint(histPoint, 0.0f, histTime);
                            eventTimeStamps.add(histTime);
                        }

                        break;
                }

                // add actual motion event point:
                PointF point = new PointF(motionEvent.getX(), motionEvent.getY());
                float pressure = 0.0f;
                currentHoverStroke.addPoint(point, pressure, currentEventTime);
                recordedHoverPoints++;

                events.add(motionEvent.getActionMasked());
                eventTimeStamps.add(currentEventTime);

                return true;
            }
        });

        /*
        Reset Button stuff:
        */
        final Button resetButton = (Button) findViewById(R.id.resetButton);
        resetButton.setClickable(true);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // calculate the total time needed:
                if (!hasTaskEnded) {
                    hasTaskEnded = true;
                    if (globalStart > 0) {
                        globalDuration = SystemClock.uptimeMillis() - globalStart;
                    } else {
                        globalDuration = 0;
                    }
                }

                // Disable the buttons for the runtime of this function to prevent multiple clicks
                // in case the save process needs ist time:
                deactivateButton(resetButton);

                spinner.setTitle("Zurücksetzen");
                spinner.show(); // start the saving progress animation

                //Save image
                //File file_vpOrdner = (File) getIntent().getExtras().get("ordner");
                File file = new File(file_vpOrdner, getLocalClassName() + "/resets");
                strokes = pageDoc.getObjectList();

                timeOffScreen = globalDuration - (timeInAir + timeOnSurface);

                int optimiert = 0;
                //shellOptimized wird nicht als übergeben daher hier stupiede gesetzt
                boolean shallOptimize = false;
                if (shallOptimize) {
                    optimiert = 1;
                }

                handler = new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        String msgString = (String) msg.obj;
                        if(!msgString.equals("saving_finished")) return false;

                        spinner.dismiss();

                        eventTimeStamps.clear();
                        events.clear();

                        //reset the recorded times:
                        initTimeCounters();

                        // remove all objects aka strokes already been drawn:
                        pageDoc.removeAllObject();
                        pageDoc.clearHistory();
                        pageDoc.getObjectList().clear();
                        sView.update();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                activateButton(resetButton);
                            }
                        });

                        return true;
                    }
                });

                resetNumber++;

                //Let a thread do the saving stuff => great user experience improvement
                SaveThread saveThread =
                        new SaveThread(file, context, noteDoc, sView, events, strokes, getLocalClassName() + "_" + resetNumber,
                                timeInAir, timeOnSurface, timeOffScreen, optimiert, eventTimeStamps, handler);
                // Needs to be done because the next activity will override the values:
                saveThread.setOrigScreenDimensions(MemoryHelper.originalCanvasWidth, MemoryHelper.originalCanvasHeight);
                saveThread.start();
            }
        });

        /*
        Save Button
         */
        final Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setClickable(true);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // calculate the total time needed:
                if (!hasTaskEnded) {
                    hasTaskEnded = true;
                    if (globalStart > 0) {
                        globalDuration = SystemClock.uptimeMillis() - globalStart;
                    } else {
                        globalDuration = 0;
                    }
                }

                // Disable the buttons for the runtime of this function to prevent multiple clicks
                // in case the save process needs its time:
                deactivateButton(saveButton);
                deactivateButton(resetButton);

                spinner.setTitle("Speichern");
                spinner.show(); // start the saving progress animation

              //  file_vpOrdner = (File) getIntent().getExtras().get("ordner");
                final File file = new File(file_vpOrdner, getLocalClassName());
                ArrayList<SpenObjectBase> strokes = pageDoc.getObjectList();

                timeOffScreen = globalDuration - (timeInAir + timeOnSurface);

                int optimiert = 0;
                if (shallOptimize){ optimiert = 1;}

                handler = new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        String msgString = (String) msg.obj;
                        if(!msgString.equals("saving_finished")) return false;

                        spinner.dismiss();
                        Intent intent = new Intent(tremoranalyse1.this, tremoranalyse2.class);
                        intent.putExtra("ordner", file_vpOrdner);
                        startActivity(intent);

                        return true;
                    }
                });

                //Let a thread do the saving stuff => great user experience improvement
                SaveThread saveThread =
                        new SaveThread(file, context, noteDoc, sView, events, strokes, getLocalClassName(),
                                timeInAir, timeOnSurface, timeOffScreen, optimiert, eventTimeStamps, handler);
                // Needs to be done because the next activity will override the values:
                saveThread.setOrigScreenDimensions(MemoryHelper.originalCanvasWidth, MemoryHelper.originalCanvasHeight);
                saveThread.start();
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
            noteDoc = new SpenNoteDoc(context, rect.width(), rect.height());
        } catch (IOException e) {
            Toast.makeText(context, "Fehler beim Doc", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }
        pageDoc = noteDoc.appendPage();
        pageDoc.setBackgroundColor(0xFFFFFFFF);

        sView.setPageDoc(pageDoc, true);
        sView.setToolTypeAction(SpenSurfaceView.TOOL_FINGER, SpenSurfaceView.ACTION_NONE);

        sView.update();

        // Set the layout listener to get the actual size of the sView drawing area:
        sView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                canvasWidth = sView.getWidth();
                canvasHeight = sView.getHeight();

                MemoryHelper.originalCanvasWidth = canvasWidth;
                MemoryHelper.originalCanvasHeight = canvasHeight;

                saveCanvasSize();

                sView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    /**
     * Initializes all variables needed to obtain the times (onScreen, inAir, ...).
     * Needs to be called on reset of the drawing area.
     */
    private void initTimeCounters() {
        timeOnSurface = 0;
        timeInAir = 0;
        timeOffScreen = 0;
        globalStart = 0;
        globalDuration = 0;
        lastTransitionTime = 0;
        isFirstScreenContact = true;
        hasTaskEnded = false;
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
     * Stores the last hover stroke and starts a new one.
     */
    private void finishHoverStroke() {
        if (recordedHoverPoints > 0) { // else no points where recorded
            currentHoverStroke.setColor(Color.RED);
            currentHoverStroke.setVisibility(false);
            currentHoverStroke.setPenSize(5.0f);
            currentHoverStroke.setAdvancedPenSetting("2");

            //add to strokelist
            pageDoc.appendObject(currentHoverStroke);
            currentHoverStroke = null;
            currentHoverStroke = new SpenObjectStroke(sView.getPenSettingInfo().name);
            recordedHoverPoints = 0;

            sView.update();
        }
    }

    /**
     * Save the size of the current sView canvas to the vpData.csv File.
     * Existing values in the file will be overwritten.
     */
    private void saveCanvasSize() {
        //String vpFolder = "MOCA/" + "vp " + MemoryHelper.currentVpNumber;
        //File ordner = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), vpFolder);
        BufferedWriter buffWriter;
        BufferedReader buffReader;

        //Speicher auf ext. Speicher:
        if (!file_vpOrdner.exists()) {
            if (!file_vpOrdner.mkdirs()) {
                Toast.makeText(context, "Fehler im Dateipfad!", Toast.LENGTH_LONG).show();
            }
        }
        try {
            //Read the two lines:
            buffReader = new BufferedReader(new FileReader(file_vpOrdner + "/vpData.csv"));
            String headerData = buffReader.readLine();
            String valueLine = buffReader.readLine();
            buffReader.close();

            // Get the values and change the last two values:
            String[] values = valueLine.split(";");
            values[values.length - 2] = String.valueOf(canvasWidth);
            values[values.length - 1] = String.valueOf(canvasHeight);
            valueLine = "";
            for(int i = 0; i < values.length; i++) {
                valueLine += values[i];
                if(i < values.length - 1) {
                    valueLine += ";";
                }
            }

            // write the lines back to the file:
            buffWriter = new BufferedWriter(new FileWriter(file_vpOrdner + "/vpData.csv"));
            buffWriter.write(headerData);
            buffWriter.newLine();
            buffWriter.write(valueLine);

            buffWriter.flush();
            buffWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Can be used to reactivate a button that has been deactivated via {@link #deactivateButton(Button)}.
     * @param but The button to reactivate
     */
    private void activateButton(Button but) {
        but.setEnabled(true);
        but.setClickable(true);
        but.setAlpha(1.0f);
    }

    /**
     * Deactivates a button by disabling its clickable attribute and changing its appearance.
     * @param but The button that shall be deactivated
     */
    private void deactivateButton(Button but) {
        but.setAlpha(0.5f);
        but.setEnabled(false);
        but.setClickable(false);
    }

    @Override
    public void onBackPressed() {
        if(MemoryHelper.debugMode) super.onBackPressed();
    }
}


