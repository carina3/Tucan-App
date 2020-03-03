package moca.clockdraw;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.constraint.ConstraintLayout;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class task1 extends Activity {

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
    DisplayMetrics mDisplayMetrics;

    private int canvasWidth, canvasHeight;
    private String screenRotation;

    boolean shallOptimize;

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
    private static String tmtName;
    private TMT tmt;
    private float factor;
    private int leaveTime;
    private int enterTime;
    private boolean enterPoint;
    private boolean leavePoint;
    private boolean onLastPoint;
    private boolean onPoint;
    private int timeOnPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set VP Ordner
        file_vpOrdner = (File) getIntent().getExtras().get("ordner");


        context = this;

        canvasHeight = 0;
        canvasWidth = 0;
        screenRotation = "";
        resetNumber = 0;

        //Saving the Screen Rotation as String, which will be added to the Name of the TMT-Background Picture
        final int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getOrientation();
        switch (rotation) {
            case Surface.ROTATION_0:
                screenRotation = "portrait";
                break;
            case Surface.ROTATION_90:
                screenRotation = "landscape";
                break;
            case Surface.ROTATION_270:
                screenRotation = "landscape2";
                break;
        }


        //Getting Information from Shared Preferences(Optimized vs. original screen size, version of the TMT, right vs. left handed layout)
        SharedPreferences prefs = getSharedPreferences("com.moca.MODE_SETTINGS", Context.MODE_PRIVATE);
        shallOptimize = prefs.getBoolean("isTabletOptimized", false);
        tmtName = prefs.getString("pfad", "fehler");
        final boolean rightHander = prefs.getBoolean("reightHander", false);

        if (shallOptimize) {
            if (rightHander) {
                setContentView(R.layout.activity_task1_optimized_rechts);
            } else {
                setContentView(R.layout.activity_task1_optimized_links);
            }

        } else {
            // Deactivate Androids' status bar in the drawing Activities:
            View osUiView = getWindow().getDecorView();
            osUiView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN + View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY +
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

            setContentView(R.layout.activity_task1);
        }
        final LinearLayout currLayout = (LinearLayout) findViewById(R.id.spenViewLayout);


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
        //final


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

        //Get Screen data
        mDisplayMetrics = getResources().getDisplayMetrics();

        //set scale factor for TMT
        factor = 1;


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
                            updateTimeOnPoints(motionEvent.getX(), motionEvent.getY(), (int) motionEvent.getHistoricalEventTime(i));
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

                        //if the Pen has just been on a point this last interval is added to timeOnPoints, bool onPoint -> false
                        if (onPoint) {
                            leaveTime = currentEventTime;
                            timeOnPoints += leaveTime - enterTime;
                            leavePoint = false;
                            enterPoint = true;
                            onLastPoint = false;
                            onPoint = false;

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
                //shellOptimized wird nicht als übergeben daher hier stupide gesetzt
                boolean shallOptimize = false;
                if (shallOptimize) {
                    optimiert = 1;
                }

                timeOnPoints = 0;
                leaveTime = 0;
                enterTime = 0;

                handler = new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        String msgString = (String) msg.obj;
                        if (!msgString.equals("saving_finished")) return false;

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

                //In this task, the time-on-points and the TMT Object(with positions of the circles etc.) are saved additionally
                SaveThread saveThread =
                        new SaveThreadTask1(file, context, noteDoc, sView, events, strokes, getLocalClassName(),
                                timeInAir, timeOnSurface, timeOffScreen, optimiert, eventTimeStamps, handler, timeOnPoints, tmt);
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

                //file_vpOrdner = (File) getIntent().getExtras().get("ordner");
                final File file = new File(file_vpOrdner, getLocalClassName());
                ArrayList<SpenObjectBase> strokes = pageDoc.getObjectList();

                timeOffScreen = globalDuration - (timeInAir + timeOnSurface);

                int optimiert = 0;
                if (shallOptimize) {
                    optimiert = 1;
                }

                handler = new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        String msgString = (String) msg.obj;
                        if (!msgString.equals("saving_finished")) return false;

                        spinner.dismiss();
                        Intent intent = new Intent(task1.this, task2.class);
                        intent.putExtra("ordner", file_vpOrdner);
                        startActivity(intent);

                        return true;
                    }
                });

                //Let a thread do the saving stuff => great user experience improvement
                SaveThread saveThread =
                        new SaveThreadTask1(file, context, noteDoc, sView, events, strokes, getLocalClassName(),
                                timeInAir, timeOnSurface, timeOffScreen, optimiert, eventTimeStamps, handler, timeOnPoints, tmt);
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
        sView.setZOrderOnTop(true); //added
        SurfaceHolder sfhTrackHolder = sView.getHolder(); //makes Background visible
        sfhTrackHolder.setFormat(PixelFormat.TRANSPARENT);

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
        pageDoc.setBackgroundColor(Color.WHITE);//0xFFFFFFFF);

        pageDoc.clearHistory();


        sView.setPageDoc(pageDoc, true);
        sView.setToolTypeAction(SpenSurfaceView.TOOL_FINGER, SpenSurfaceView.ACTION_NONE);


        sView.update();

        // Set the layout listener to get the actual size of the sView drawing area:
        sView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {


                canvasWidth = sView.getWidth();
                canvasHeight = sView.getHeight();

                if (shallOptimize) {
                    //squared Canvas
                    //-> set fitting Layout depending on screen rotation
                    switch (screenRotation) {
                        case "portrait":
                            canvasHeight = canvasWidth;
                            break;
                        case "landscape":
                            canvasWidth = canvasHeight;
                            break;
                        case "landscape2":
                            canvasWidth = canvasHeight;
                    }


                    if (shallOptimize) {
                        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) currLayout.getLayoutParams();
                        params.height = canvasHeight;
                        params.width = canvasWidth;
                        currLayout.setLayoutParams(params);
                    } else {
                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) currLayout.getLayoutParams();
                        params.height = canvasHeight;
                        params.width = canvasWidth;
                        currLayout.setLayoutParams(params);
                    }
                }

                setBackgroundtoTMT();
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

        //Initialize TMT Variables (to measure timeOnPoints)
        enterPoint = true;
        leavePoint = false;
        onLastPoint = false;
        onPoint = false;
        timeOnPoints = 0;
        leaveTime = 0;
        enterTime = 0;
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
            for (int i = 0; i < values.length; i++) {
                valueLine += values[i];
                if (i < values.length - 1) {
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


    public void setBackgroundtoTMT() {
        String optimized = "";
        if (shallOptimize) {
            optimized = "optimized";
        }

        String imageName = tmtName + "_" + screenRotation + "_" + optimized + ".png";

        File ordner = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MOCA_background_images/" + imageName);

        if (shallOptimize) {
            //calculate scaling for optimized TMT
            float mm70 = mmtoPixels(70);
            factor = canvasWidth / mm70;
        }

//Check which TMT has to be drawn
        if (tmtName.contains("1")) {
            tmt = TMT.createTMT1(factor);

        } else if (tmtName.contains("2")) {
            tmt = TMT.createTMT2(factor);
        } else if (tmtName.contains("3")) {
            tmt = TMT.createTMT3(factor);

        } else {
            tmt = TMT.createTMT1(factor);
            //Toast.makeText(this.tas, "Fehler beim Laden des TMT", Toast.LENGTH_SHORT).show();
        }

        if (!ordner.exists()) {
            drawTMT();
        }
        if (ordner.exists()) {


            String path = ordner.getPath();

            pageDoc.setBackgroundImageMode(SpenPageDoc.BACKGROUND_IMAGE_MODE_CENTER); //TODO: richtigen Modus finden
            pageDoc.setBackgroundImage(path);

            sView.update();
        }

    }


    public void drawTMT() {

        //Create Bitmap with size of the screen, set Background Color
        Bitmap tmtBitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
        Canvas tmtCanvas = new Canvas(tmtBitmap);
        Paint screenPaint = new Paint();
        screenPaint.setColor(Color.GRAY);
        tmtCanvas.drawRect(rect, screenPaint);

        //Draw Task Rectangle
        Paint TaskPaint = new Paint();
        TaskPaint.setColor(Color.WHITE);

        tmtCanvas.drawRect(0, 0, canvasWidth, canvasHeight, TaskPaint);

        //Call Draw Circles Method
        drawCircles(tmtCanvas);

        String optimized = "";
        if (shallOptimize) {
            optimized = "optimized";
        }

        saveImage(tmtBitmap, tmtName + "_" + screenRotation + "_" + optimized + ".png");


    }

    private void drawCircles(Canvas canvas) {
        float correctAngle1 = 0f;
        float correctAngle2 = 0f;

        if (tmtName.contains("3")) {
            //The angle of the arrows in TMT3 need correction, otherwise they will not be drawn properly.
            correctAngle1 = -5;
            correctAngle2 = -27;
        }


        //Getting data from TMT object
        ArrayList<PointF> circlePoints = tmt.getCirclePoints();
        float diameter = tmt.getDiameter();
        float radius = diameter / 2;
        ArrayList<String> strings = tmt.stringList;

        //Create ShapeDrawable Circle
        ShapeDrawable myCircle = new ShapeDrawable(new OvalShape());
        myCircle.getPaint().setColor(Color.BLACK);
        myCircle.getPaint().setStyle(Paint.Style.STROKE);
        float strokeWidth = 0.5f; //Stroke Width in mm
        myCircle.getPaint().setStrokeWidth(mmtoPixels(strokeWidth));

        //Settings for Characters
        Paint charPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        charPaint.setColor(Color.BLACK);
        charPaint.setTextSize(mmtoPixels(5.556f) * factor);
        charPaint.setShadowLayer(1f, 0f, 1f, Color.WHITE);
        charPaint.setStrokeWidth(mmtoPixels(0.5f));

        for (int i = 0; i < circlePoints.size(); i++) {
            float x = circlePoints.get(i).x;
            float y = circlePoints.get(i).y;

            //Draw Circles
            myCircle.setBounds((int) mmtoPixels(x - radius), (int) mmtoPixels(y - radius), (int) mmtoPixels(x + radius), (int) mmtoPixels(y + radius));
            myCircle.draw(canvas);

            //Draw Letters and Numbers
            String sign = strings.get(i);
            canvas.drawText(sign, mmtoPixels(x - 1.85f * factor), mmtoPixels(y + 1.8f * factor), charPaint);


        }

        //Draw Beginning and Ending Text
        Paint textPaint = new Paint();
        textPaint.setTextSize(mmtoPixels(3) * factor);
        textPaint.setTypeface(Typeface.create("Arial", Typeface.BOLD));
        PointF point1 = circlePoints.get(0);
        PointF pointE = circlePoints.get(circlePoints.size() - 1);
        canvas.drawText("Beginn", mmtoPixels(point1.x - 4 * factor), mmtoPixels(point1.y + 7 * factor), textPaint);
        canvas.drawText("Ende", mmtoPixels(pointE.x - 4 * factor), mmtoPixels(pointE.y + 7 * factor), textPaint);


        //Drawing the Arrows-------------------------------------------------------------
        Paint arrowPaint = new Paint();
        arrowPaint.setColor(Color.BLACK);
        arrowPaint.setARGB(255, 0, 0, 0);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(mmtoPixels(0.5f));
        arrowPaint.setPathEffect(new DashPathEffect(new float[]{mmtoPixels(1), mmtoPixels(1)}, 0));

        //Draw the Lines
        //PointF point1 = circlePoints.get(0);
        PointF pointA = circlePoints.get(1);
        PointF point2 = circlePoints.get(2);


        PointF start = getCircleLineIntersectionPoint(point1, pointA, point1, radius);
        start.x = mmtoPixels(start.x);
        start.y = mmtoPixels(start.y);
        PointF end = getCircleLineIntersectionPoint(point1, pointA, pointA, radius);
        end.x = mmtoPixels(end.x);
        end.y = mmtoPixels(end.y);


        canvas.drawLine(start.x, start.y, end.x, end.y, arrowPaint);

        PointF start2 = getCircleLineIntersectionPoint(pointA, point2, pointA, radius);
        PointF end2 = getCircleLineIntersectionPoint(point2, pointA, point2, radius);
        start2.x = mmtoPixels(start2.x);
        start2.y = mmtoPixels(start2.y);
        end2.x = mmtoPixels(end2.x);
        end2.y = mmtoPixels(end2.y);

        canvas.drawLine(start2.x, start2.y, end2.x, end2.y, arrowPaint);

        //-------------------------------------------------------------------------
//Draw arrows
        Drawable arrow = getDrawable(R.drawable.arrow2);
        //calculate rotating angle
        float degrees = angleOf(start, end) + correctAngle1;

        canvas.save();
        canvas.rotate(degrees, end.x, end.y);
        arrow.setBounds((int) (end.x - mmtoPixels(2.1f)), (int) (end.y - mmtoPixels(0f)), (int) (end.x + mmtoPixels(0.9f)), (int) (end.y + mmtoPixels(3f)));
        arrow.draw(canvas);

        canvas.restore();
        //Calculate rotating angle
        float degrees2 = angleOf(start2, end2) + correctAngle2;


        canvas.rotate(degrees2 + 190, end2.x, end2.y); //+190 better
        arrow.setBounds((int) (end2.x - mmtoPixels(2f)), (int) (end2.y - mmtoPixels(0f)), (int) (end2.x + mmtoPixels(1f)), (int) (end2.y + mmtoPixels(3f)));
        arrow.draw(canvas);

//------------------------------------------------------------


    }

    private static void saveImage(Bitmap bitmap, String name) {

        File ordner = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        String path = ordner.getPath();
        File myDir = new File(path + "/MOCA_background_images");
        myDir.mkdirs();

        File file = new File(myDir, name);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public float mmtoPixels(float mm) {
        float pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, mm, mDisplayMetrics);
        return pixels;
    }

    //This function is based on code on: https://stackoverflow.com/a/13055116 (21.01.18)
//The Circle Line Intersection Points are used later to draw the arrows in the TMT picture.
    public static PointF getCircleLineIntersectionPoint(PointF point1,
                                                        PointF pointA, PointF center, float radius) {
        float baX = pointA.x - point1.x;
        float baY = pointA.y - point1.y;
        float caX = center.x - point1.x;
        float caY = center.y - point1.y;

        float a = baX * baX + baY * baY;
        float bBy2 = baX * caX + baY * caY;
        float c = caX * caX + caY * caY - radius * radius;

        float pBy2 = bBy2 / a;
        float q = c / a;

        float disc = pBy2 * pBy2 - q;

        float tmpSqrt = (float) Math.sqrt(disc);
        float abScalingFactor1 = -pBy2 + tmpSqrt;
        float abScalingFactor2 = -pBy2 - tmpSqrt;

        PointF p1 = new PointF((float) (point1.x - baX * abScalingFactor1), (float) (point1.y - baY * abScalingFactor1));
        PointF p2 = new PointF((float) (point1.x - baX * abScalingFactor2), (float) (point1.y
                - baY * abScalingFactor2));
        if (Math.abs(pointA.x - p1.x) > Math.abs(pointA.x - p2.x)) {
            return p2;
        } else {
            return p1;
        }

    }

    //Based on the following Code: https://stackoverflow.com/a/27481611 (21.01.18)
    public static float angleOf(PointF p1, PointF p2) {
        final float deltaY = (p1.y - p2.y);
        final float deltaX = (p2.x - p1.x);
        final float result = (float) Math.toDegrees(Math.atan2(deltaY, deltaX));
        //return (result < 0) ? (360f + result) : result;
        return result;
    }


    //Updates TimeOnPoints if the pen is m2oving on a Circle
    private void updateTimeOnPoints(float x, float y, int time) {
        onPoint = false;
        PointF currentPoint = new PointF(x, y);
        ArrayList<PointF> circlePoints = tmt.getCirclePoints();
        for (int i = 0; i < circlePoints.size(); i++) {
            PointF center = circlePoints.get(i);
            //Check if current Point is in a Circle of the TMT (Check within the radius of the central point)
            float dx = currentPoint.x - mmtoPixels(center.x);
            float dy = currentPoint.y - mmtoPixels(center.y);
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance < mmtoPixels(tmt.diameter / 2)) {
                //if this is the case, a point is entered, measuring Time on Points starts
                if (enterPoint) {
                    enterTime = time;
                    leavePoint = true;
                    enterPoint = false;
                    //timeOnThisCircle += time;
                }
                if (i == (circlePoints.size() - 1)) {
                    onLastPoint = true;
                }
                onPoint = true;
                break;

            } else {
                onPoint = false;
            }

        }

        //Initialize TMT Time Variables when a point was left
        if (!onPoint && leavePoint) {
            leaveTime = time;
            timeOnPoints += leaveTime - enterTime;
            leavePoint = false;
            enterPoint = true;
        }
    }


    /**
     * Can be used to reactivate a button that has been deactivated via {@link #deactivateButton(Button)}.
     *
     * @param but The button to reactivate
     */
    private void activateButton(Button but) {
        but.setEnabled(true);
        but.setEnabled(true);
        but.setClickable(true);
        but.setAlpha(1.0f);
    }

    /**a
     * Deactivates a button by disabling its clickable attribute and changing its appearance.
     *
     * @param but The button that shall be deactivated
     */
    private void deactivateButton(Button but) {
        but.setEnabled(false);
        but.setClickable(false);
        but.setAlpha(0.5f);
    }
}