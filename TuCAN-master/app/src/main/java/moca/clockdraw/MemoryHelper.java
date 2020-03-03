package moca.clockdraw;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Environment;
import android.view.MotionEvent;
import android.widget.Toast;

import com.samsung.android.sdk.pen.document.SpenNoteDoc;
import com.samsung.android.sdk.pen.document.SpenObjectBase;
import com.samsung.android.sdk.pen.document.SpenObjectStroke;
import com.samsung.android.sdk.pen.engine.SpenSurfaceView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class MemoryHelper {
    final static boolean debugMode = false;

    static String currentVpNumber = "-1"; // initial value
    static final File mocaPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MOCA");
    static File vpFolder;

    // Actual size (in pixel) of the canvas that records or recorded the task accordingly:
    static int originalCanvasWidth;
    static int originalCanvasHeight;

    // Variables needed to automatically adjust the pen size according to screen resolution:
    final static int referenceScreenResolution = 2048 * 1536; // screen resolution of the Samsung Tab S3; used as reference
    final static float referencePenSize = 4.0f;
    static int actualScreenResolution;
    static float actualPenSize;

    public static ArrayList<SPenData> task1data = new ArrayList<>();
    public static ArrayList<SPenData> task2data = new ArrayList<>();
    public static ArrayList<SPenData> task3data = new ArrayList<>();
    public static ArrayList<SPenData> tremoranalyse1data = new ArrayList<>();
    public static ArrayList<SPenData> tremoranalyse2data = new ArrayList<>();

    //Time
    public static TimeAndScreenData task1TimeAndScreenData = new TimeAndScreenData();
    public static TimeAndScreenData task2TimeAndScreenData = new TimeAndScreenData();
    public static TimeAndScreenData task3TimeAndScreenData = new TimeAndScreenData();
    public static TimeAndScreenData tremor1TimeAndScreenData = new TimeAndScreenData();
    public static TimeAndScreenData tremor2TimeAndScreenData = new TimeAndScreenData();


    /**
     * Checks if file tree for the specified VP exists. The file tree is created in case it does not.
     *
     * @param context
     * @param vpNumber
     * @return
     */
    static boolean initVpFileTree(Context context, int vpNumber) {
        currentVpNumber = String.valueOf(vpNumber);
        if (!mocaPath.exists()) {
            if (!mocaPath.mkdirs()) {
                Toast.makeText(context, "Zugriff auf Zielordner nicht möglich!", Toast.LENGTH_LONG).show();
            }
        }
        vpFolder = new File(mocaPath.getPath() + "/VP_" + currentVpNumber);

        return true;
    }

    static void saveCSVneu(File ordner, Context context, SpenNoteDoc noteDoc, SpenSurfaceView sView, ArrayList<Integer> events,
                           ArrayList<SpenObjectBase> strokeList, String classname, long timeInAir, long timeOnSurface,
                           long timeOffScreen, int tabletOptimized) {
        //Speicher auf ext. Speicher:
        if (!ordner.exists()) {
            if (!ordner.mkdirs()) {
                Toast.makeText(context, "Fehler beim Zugriff auf Speicher!", Toast.LENGTH_LONG).show();
            }
        }
        String saveFilePath = ordner.getPath() + "/" + classname + ".spd";

        try {
            noteDoc.save(saveFilePath, false);

            //Toast.makeText(context, "Datei gespeichert nach" + saveFilePath, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(context, "Fehler beim Speichern!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        //Save as BMP
        String saveFilePathBmp = ordner.getPath() + "/" + classname + ".png";
        captureSpenSurfaceView(saveFilePathBmp, sView, noteDoc, context);

        BufferedWriter buffWriter;
        //Save on external storage
        if (!ordner.exists()) {
            if (!ordner.mkdirs()) {
                Toast.makeText(context, "Fehler im Dateipfad!", Toast.LENGTH_LONG).show();
            }
        }
        try {

            //Building the .csv- File with the different Pen-Data

            buffWriter = new BufferedWriter(new FileWriter(ordner + "/" + classname + "coordinaten.csv"));

            // write line with Headings
            String header = "timestamp;X;Y;pressure;motionEvent;velocity";
            buffWriter.write(header);
            buffWriter.write("\n");

            String outStr;
            int eventIndex = 0; // the current element of "events" to print out
            //strokeList = pageDoc.getObjectList();
            for (SpenObjectBase obj : strokeList) {
                SpenObjectStroke stroke = (SpenObjectStroke) obj;
                float[] xPoints = stroke.getXPoints();
                float[] yPoints = stroke.getYPoints();
                int[] timestamps = stroke.getTimeStamps();
                float[] pressures = stroke.getPressures();

                for (int i = 0; i < xPoints.length; i++) {
                    outStr = timestamps[i] + ";" + xPoints[i] + ";" + yPoints[i] + ";" + pressures[i] + ";";

                    switch (events.get(eventIndex)) {
                        case MotionEvent.ACTION_DOWN:
                            outStr += "DOWN";
                            break;

                        case MotionEvent.ACTION_UP:
                            outStr += "UP";
                            break;

                        case MotionEvent.ACTION_MOVE:
                            outStr += "MOVE";
                            break;

                        case MotionEvent.ACTION_HOVER_ENTER:
                            outStr += "HOVER_ENTER";
                            break;

                        case MotionEvent.ACTION_HOVER_EXIT:
                            outStr += "HOVER_EXIT";
                            break;

                        case MotionEvent.ACTION_HOVER_MOVE:
                            outStr += "HOVER_MOVE";
                            break;
                    }

                    // Calculate and add the velocity to the output string line:
                    if(i > 0) {
                        SPenData lastPoint = new SPenData(timestamps[i-1], xPoints[i-1], yPoints[i-1],
                                pressures[i-1], events.get(i-1), 0);
                        SPenData currentPoint = new SPenData(timestamps[i], xPoints[i], yPoints[i],
                                pressures[i], events.get(i),0);

                        double velocity = Datenverarbeitung.berechneGewindigkeit(lastPoint, currentPoint);
                        outStr += ";" + String.valueOf(velocity);
                    } else {
                        outStr += ";0.0";
                    }

                    buffWriter.write(outStr);
                    buffWriter.write("\n");

                    eventIndex++;
                }
            }

            buffWriter.write("\n");
            buffWriter.write(timeInAir + ";" + timeOnSurface + ";" + timeOffScreen + ";" + tabletOptimized + ";" + originalCanvasWidth + ";" + originalCanvasHeight);

            buffWriter.flush();
            buffWriter.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void saveCSVneu(File ordner, Context context, SpenNoteDoc noteDoc, SpenSurfaceView sView, ArrayList<Integer> events,
                           ArrayList<SpenObjectBase> strokeList, String classname, long timeInAir, long timeOnSurface,
                           long timeOffScreen, int tabletOptimized, int timeOnPoints, TMT tmt) {
        saveCSVneu(ordner, context, noteDoc, sView, events,
                strokeList, classname, timeInAir, timeOnSurface,
                timeOffScreen, tabletOptimized);

        try {
            //Build .csv- File for TMT-Data (Time variables with additional timeonPoints, Coordinates of the Circles, diameter, width, height)
            BufferedWriter buffWriter = new BufferedWriter(new FileWriter(ordner + "/" + classname + "tmtData.csv"));
            buffWriter.write("timeinair; timeonsurface; timeoffscreen; timeonpoints; point1x; point1y; pointax; pointay;" +
                    "point2x; point2y; pointbx; pointby; point3x; point3y; pointcx; pointcy; point4x; point4y; pointdx; pointdy; point5x; point5y; pointex; pointey; diameter; originalheight; originalwidth");
            buffWriter.write("\n");
            buffWriter.write(timeInAir + ";" + timeOnSurface + ";" + timeOffScreen + ";" + timeOnPoints);
            ArrayList<PointF> circles = tmt.getCirclePoints();
            for (int i = 0; i<circles.size(); i++){
                float x = circles.get(i).x;
                float y = circles.get(i).y;
                buffWriter.write(";" + x + ";" + y);
            }
            buffWriter.write(";" + tmt.diameter);
            buffWriter.write(";"+ originalCanvasHeight);
            buffWriter.write(";" + originalCanvasWidth);

            buffWriter.flush();
            buffWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

//    static void datenHinzufuegen(String selection, File file){
//        spalteHinzufuegen(file + "/" + selection + "/task3coordinaten.csv", "velocity", Datenverarbeitung.GeschwindigkeitTask(task3data));
//    }
//
//    static void spalteHinzufuegen(String file, String spaltenUeberschrift, ArrayList spaltenDaten){
//        BufferedReader reader;
//        FileWriter writer;
//        //Speicher auf ext. Speicher:
//        try {
//            reader = new BufferedReader(new FileReader(file));
//
//            String line = reader.readLine();
//
//            if (!line.contains(spaltenUeberschrift)) {
//                String result = line + spaltenUeberschrift + ";\n";
//                for (int counter = 0; counter < spaltenDaten.size(); counter++) {
//                    line = reader.readLine();
//                    result += line + ";" + spaltenDaten.get(counter) + ";\n";
//                }
//
//                writer = new FileWriter(file);
//                writer.write(result);
//                writer.write("\n");
//                reader.skip(1);
//                writer.write(reader.readLine());
//                writer.close();
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    static void loadCSV(String selection, File file, Context context) {
        // Delete all points in the lists before there are new values put into the lists:
        task1data.clear();
        task2data.clear();
        task3data.clear();
        tremoranalyse1data.clear();
        tremoranalyse2data.clear();


        // Load new data points into the lists from  the coordinate csv files:
        loadCSVhelp(file + "/" + selection + "/task1/", "task1", task1data, task1TimeAndScreenData, context);
        loadCSVhelp(file + "/" + selection + "/task2/", "task2", task2data, task2TimeAndScreenData, context);
        loadCSVhelp(file + "/" + selection + "/task3/", "task3", task3data, task3TimeAndScreenData, context);
        loadCSVhelp(file + "/" + selection + "/tremoranalyse1/", "tremoranalyse1", tremoranalyse1data, tremor1TimeAndScreenData, context);
        loadCSVhelp(file + "/" + selection + "/tremoranalyse2/", "tremoranalyse2", tremoranalyse2data, tremor2TimeAndScreenData, context);
    }

    private static void loadCSVhelp(String path, String taskName, ArrayList daten, TimeAndScreenData timeAndScreenData, Context context) {
        // Do the actual read of the coordinate data:
        BufferedReader reader;
        try {

            reader = new BufferedReader(new FileReader(path + taskName + "coordinaten.csv"));

            // read the header line and discard it
            reader.readLine();

            String[] data;
            String zeile;
            int timestamp, motionEvent;
            float x, y, pressure, velocity;

            while ((zeile = reader.readLine()) != null) {

                if (zeile.isEmpty()) {
                    //Read TimeInAIr, TimeOnSurface and TimeOffScreen from CSV and save them in TimeAndScreenData object
                    String times = reader.readLine();
                    data = times.split(";");
                    long timeInAir = Long.parseLong(data[0]);
                    long timeOnSurface = Long.parseLong(data[1]);
                    long timeOffScreen = Long.parseLong(data[2]);
                    int tabletOptimized = Integer.parseInt(data[3]);
                    int originalViewWidth = Integer.parseInt(data[4]);
                    int originalViewHeight = Integer.parseInt(data[5]);

                    timeAndScreenData.setTimeInAir(timeInAir);
                    timeAndScreenData.setTimeOnSurface(timeOnSurface);
                    timeAndScreenData.setTimeOffScreen(timeOffScreen);
                    timeAndScreenData.setTabletOptimized(tabletOptimized);
                    timeAndScreenData.setOriginalViewWidth(originalViewWidth);
                    timeAndScreenData.setOriginalViewHeight(originalViewHeight);

                    break;
                }
                data = zeile.split(";");

                timestamp = Integer.parseInt(data[0]);
                x = Float.parseFloat(data[1]);
                y = Float.parseFloat(data[2]);
                pressure = Float.parseFloat(data[3]);
                velocity = Float.parseFloat(data[5]);

                motionEvent = -1;

                switch (data[4]) {
                    case "HOVER_MOVE":
                        motionEvent = MotionEvent.ACTION_HOVER_MOVE;
                        break;
                    case "HOVER_EXIT":
                        motionEvent = MotionEvent.ACTION_HOVER_EXIT;
                        break;
                    case "HOVER_ENTER":
                        motionEvent = MotionEvent.ACTION_HOVER_ENTER;
                        break;
                    case "DOWN":
                        motionEvent = MotionEvent.ACTION_DOWN;
                        break;
                    case "MOVE":
                        motionEvent = MotionEvent.ACTION_MOVE;
                        break;
                    case "UP":
                        motionEvent = MotionEvent.ACTION_UP;
                        break;
                }

                SPenData dataPoint = new SPenData(timestamp, x, y, pressure, motionEvent, velocity);
                daten.add(dataPoint);

            }

            reader.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //The Code for this function is available in the Spen- SDK-Documentation
    protected static void captureSpenSurfaceView(String strFileName, SpenSurfaceView sView, SpenNoteDoc noteDoc, Context context) {
        // Capture the view
        Bitmap imgBitmap = sView.captureCurrentView(true);
        if (imgBitmap == null) {
            Toast.makeText(context, "Capture failed." + strFileName, Toast.LENGTH_SHORT).show();
            return;
        }
        OutputStream out;
        try {
            // Create FileOutputStream and save the captured image.
            out = new FileOutputStream(strFileName);
            imgBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // Save the note information.
            noteDoc.save(out, false);
            out.close();
            //Toast.makeText(context, "Captured images were stored in the file" + strFileName, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            File tmpFile = new File(strFileName);
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
            Toast.makeText(context, "Failed to save the file.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (Exception e) {
            File tmpFile = new File(strFileName);
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
            Toast.makeText(context, "Failed to save the file.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        imgBitmap.recycle();
    }

    /**
     * Thread-safe wrapper function for saving a task via thread.
     * Locks whole MemoryHelper.class to ensure consistency of the original screen width and height
     * values.
     *
     * @param ordner
     * @param context
     * @param noteDoc
     * @param sView
     * @param events
     * @param strokeList
     * @param classname
     * @param timeInAir
     * @param timeOnSurface
     * @param timeOffScreen
     * @param tabletOptimized
     * @param originalScreenWidth
     * @param originalScreenHeight
     */

    //saving Task 2/Task 3
    static synchronized void saveCSVinThread(File ordner, Context context, SpenNoteDoc noteDoc, SpenSurfaceView sView, ArrayList<Integer> events,
                                             ArrayList<SpenObjectBase> strokeList, String classname, long timeInAir, long timeOnSurface,
                                             long timeOffScreen, int tabletOptimized, int originalScreenWidth, int originalScreenHeight) {

        MemoryHelper.originalCanvasWidth = originalScreenWidth;
        MemoryHelper.originalCanvasHeight = originalScreenHeight;

        saveCSVneu(ordner, context, noteDoc, sView, events, strokeList, classname, timeInAir, timeOnSurface, timeOffScreen, tabletOptimized);
    }

    //saving Task 1 (TMT)
    static synchronized void saveCSVinThread(File ordner, Context context, SpenNoteDoc noteDoc, SpenSurfaceView sView, ArrayList<Integer> events,
                                             ArrayList<SpenObjectBase> strokeList, String classname, long timeInAir, long timeOnSurface,
                                             long timeOffScreen, int tabletOptimized, int originalScreenWidth, int originalScreenHeight, int timeOnPoints, TMT tmt) {

        MemoryHelper.originalCanvasWidth = originalScreenWidth;
        MemoryHelper.originalCanvasHeight = originalScreenHeight;

        saveCSVneu(ordner, context, noteDoc, sView, events, strokeList, classname, timeInAir, timeOnSurface, timeOffScreen, tabletOptimized, timeOnPoints, tmt);
    }
}
