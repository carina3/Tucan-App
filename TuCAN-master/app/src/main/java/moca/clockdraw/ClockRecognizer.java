package moca.clockdraw;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.samsung.android.sdk.pen.document.SpenObjectBase;
import com.samsung.android.sdk.pen.document.SpenObjectStroke;
import com.samsung.android.sdk.pen.document.SpenObjectTextBox;
import com.samsung.android.sdk.pen.document.SpenPageDoc;
import com.samsung.android.sdk.pen.engine.SpenSurfaceView;
import com.samsung.android.sdk.pen.recognition.SpenRecognitionBase;
import com.samsung.android.sdk.pen.recognition.SpenRecognitionInfo;
import com.samsung.android.sdk.pen.recognition.SpenTextRecognition;
import com.samsung.android.sdk.pen.recognition.SpenTextRecognitionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ClockRecognizer {

    private Context context;
    private SpenSurfaceView sView;
    private SpenPageDoc pageDoc;

    private int canvasWidth, canvasHeight;

    // TEXT RECOGNITION STUFF:
    private SpenTextRecognitionManager textRecManager;
    private List<SpenRecognitionInfo> infoList;
    private SpenTextRecognition textRec;
    private boolean isProcessingText = false;
    private Handler textRecHandler;
    private int strokePairIndex = 0;
    private ArrayList<SpenObjectBase> actStrokes;
    private ArrayList<ArrayList<SpenObjectBase>> strokePairs = new ArrayList<>();
    private boolean isInsideStrokeGroup = false;
    private ArrayList<ArrayList<SpenObjectBase>> groupCombinations = new ArrayList<>();
    private ArrayList<RecognizedStroke> recognizedGroupObjects = new ArrayList<>();
    private ArrayList<RecognizedStroke> recognizedDigits = new ArrayList<>();

    // recognized strokes:
    private ExtendedStroke circle = null;
    private ExtendedStroke minuteHand = null;
    private ExtendedStroke hourHand = null;

    private RectF circleRect = null;
    private PointF circleCenter = null;
    private double meanCircleRadius = -1.0;

    private LinkedList<RecognizedStroke>[] finalNumbers = new LinkedList[13];
    private boolean[] isFinalNumberPosCorrect = new boolean[finalNumbers.length];
    private ArrayList<RecognizedStroke> unidentifiedStrokes = new ArrayList<>();
    private ArrayList<ExtendedStroke> possibleHands = new ArrayList<>();

    private String timeToDraw = "Zehn nach elf";

    // mean of stroke lengths:
    private double meanStrokeLength = 0.0;
    private ArrayList<ExtendedStroke> recStrokes = new ArrayList<>(); // saves all valid recorded strokes

    // recognized parts:
    private boolean foundCircleStroke = false;
    private boolean isCircleStrokeValid = false; // is neither "egg-shaped" nor contains major distortions
    private boolean foundValidHands = false;

    private Handler finHandler;

    String htmlOut = "EMPTY";

    ClockRecognizer(Context context, SpenSurfaceView sView, SpenPageDoc pageDoc, String drawnTime,
                    Handler finHandler, int canvasWidth, int canvasHeight) {
        this.context = context;
        this.sView = sView;
        this.pageDoc = pageDoc;
        this.timeToDraw = drawnTime;
        this.finHandler = finHandler;
        this.canvasHeight = canvasHeight;
        this.canvasWidth = canvasWidth;

        setRecognitionHandler();

        initTextRecognition();
    }

    /**
     * Draw the bounding box of a stroke onto the SPen Canvas.
     * @param stroke
     * @param color
     */
    private void drawStrokeRect(SpenObjectBase stroke, int color) {
        if(!(stroke instanceof SpenObjectStroke)) return;
        RectF strokeRect = stroke.getRect();
        drawStrokeRect(strokeRect, color);
    }

    /**
     * Draw the given bounding box onto the canvas.
     * @param bBox
     * @param color
     */
    private void drawStrokeRect(RectF bBox, int color) {
        RectF strokeRect = bBox;
        SpenObjectStroke newStroke = new SpenObjectStroke(sView.getPenSettingInfo().name);
        newStroke.setVisibility(true);
        newStroke.setColor(color);
        newStroke.setAdvancedPenSetting(sView.getPenSettingInfo().advancedSetting);
        PointF[] rectPoints = new PointF[5];
        rectPoints[0] = new PointF(strokeRect.left, strokeRect.top);
        rectPoints[1] = new PointF(strokeRect.left, strokeRect.bottom);
        rectPoints[2] = new PointF(strokeRect.right, strokeRect.bottom);
        rectPoints[3] = new PointF(strokeRect.right, strokeRect.top);
        rectPoints[4] = new PointF(strokeRect.left, strokeRect.top);
        float[] pressures = {0.6f, 0.6f, 0.6f, 0.6f, 0.6f};
        int currTime = (int) SystemClock.uptimeMillis();
        int[] times = {currTime, currTime + 100, currTime + 200, currTime + 300, currTime + 400};
        newStroke.setPoints(rectPoints, pressures, times);
        newStroke.setCurveEnabled(false);

        pageDoc.appendObject(newStroke);

        sView.closeControl();
        sView.update();
    }

    /**
     * Determines the best guess out of a number of equally detected numbers.
     */
    private void refineDigitMatching() {
        // first round: match all recognized digits that are non-existant yet:
        ArrayList<RecognizedStroke> unspecifiedDigits = new ArrayList<>(recognizedDigits.size());
        for(RecognizedStroke str : recognizedDigits) {
            int numValue = Integer.parseInt(str.getValue());

            if(numValue == 17 || numValue == 71 || numValue == 77) numValue = 11;
            else if(numValue == 72) numValue = 12;

            if(numValue > 0 && numValue < 13) {
                if(finalNumbers[numValue] == null) {
                    LinkedList<RecognizedStroke> newMatch = new LinkedList<>();
                    newMatch.add(new RecognizedStroke(str.getValue(), str.getStrokes()));
                    finalNumbers[numValue] = newMatch;
                } else {
                    // check if the second occurence fits better than the first one:
                    RecognizedStroke oldVal = finalNumbers[numValue].get(0);
                    if(oldVal != null) {
                        double oldAngle = calcAbsAngle(circleCenter, oldVal.getRectCenter());
                        double newAngle = calcAbsAngle(circleCenter, str.getRectCenter());
                        double actualNumberAngle = numValue * 30.0;
                        double actualNumberAngleOld = numValue * 30.0;
                        if(numValue == 12 && newAngle < 180.0) actualNumberAngle = 0.0;
                        if(numValue == 12 && oldAngle < 180.0) actualNumberAngleOld = 0.0;
                        double oldDelta = Math.abs(oldAngle - actualNumberAngleOld);
                        double newDelta = Math.abs(newAngle - actualNumberAngle);

                        if(newDelta < oldDelta) {
                            finalNumbers[numValue].remove(oldVal);
                            finalNumbers[numValue].add(0, str);
                        }
                    } else {
                        finalNumbers[numValue].add(0, str);
                    }
                }
            } else {
                unspecifiedDigits.add(str);
            }
        }
    }


    /**
     * Creates a circular shaped stroke for the spen canvas.
     * @param circleCenter
     * @param circleRadius
     * @param circumferencePoints
     * @return
     */
    private SpenObjectStroke createCircleStroke(PointF circleCenter, double circleRadius, int circumferencePoints) {
        double pointGranularity = 2.0 * Math.PI / (circumferencePoints * 1.0);
        SpenObjectStroke estimCircle = new SpenObjectStroke(sView.getPenSettingInfo().name);
        estimCircle.setVisibility(true);
        estimCircle.setColor(Color.GREEN);
        estimCircle.setAdvancedPenSetting(sView.getPenSettingInfo().advancedSetting);
        PointF[] points = new PointF[circumferencePoints];
        int time = (int) SystemClock.currentThreadTimeMillis();
        float[] press = new float[circumferencePoints];
        int[] timestamps = new int[circumferencePoints];
        double currAngle = 0.0;
        for(int i = 0; i < circumferencePoints; i++) {
            float x = (float) (circleCenter.x + circleRadius * Math.cos(currAngle));
            float y = (float) (circleCenter.y + circleRadius * Math.sin(currAngle));
            points[i] = new PointF(x,y);
            timestamps[i] = time + 100 * i;
            press[i] = 0.5f;

            if(i == circumferencePoints - 2) currAngle = 2.0 * Math.PI;
            else currAngle += pointGranularity;
        }
        estimCircle.setPoints(points, press, timestamps);

        return estimCircle;
    }

    /**
     * Calculates the euclidian distance betwewn two points a and b.
     * @param a
     * @param b
     * @return
     */
    private double euclidDist(PointF a, PointF b) {
        double dist = Math.pow(a.x - b.x, 2.0) + Math.pow(a.y - b.y, 2.0);

        return Math.sqrt(dist);
    }

    /**
     * Checks if a given stroke is not distorted.
     * @param circleStroke
     * @return
     */
    private boolean isNotDistorted(ExtendedStroke circleStroke) {
        // CIRCLE DETECTION FOLLOWING:
        RectF circleBbox = circleStroke.actualStroke.getRect();
        PointF circleCenter = new PointF(circleBbox.centerX(), circleBbox.centerY());

        double circleRadius = 0.0;
        double maxRadius = -1.0;
        double minRadius = 1000000.0;
        for (PointF point : circleStroke.actualStroke.getPoints()) {
            double currDist = circleStroke.euclidDist(point, circleCenter);
            circleRadius += currDist;
            if (currDist > maxRadius) maxRadius = currDist;
            if (currDist < minRadius) minRadius = currDist;
        }
        circleRadius /= circleStroke.actualStroke.getPoints().length * 1.0;

        double circWidth = circleStroke.actualStroke.getRect().width();
        double circHeight = circleStroke.actualStroke.getRect().height();

        // circle detection criteria from DELCODE slides:
        boolean seemsToBeProperCircle = false;
        if (Math.max(circHeight, circWidth) / Math.min(circHeight, circWidth) > 1.5 ||
                maxRadius / minRadius > 1.5) {
            seemsToBeProperCircle = false;
        } else {
            seemsToBeProperCircle = true;
            this.meanCircleRadius = circleRadius;
        }

        return seemsToBeProperCircle;
    }

    /**
     * Checks if a stroke is a straight line.
     * @param lineStroke
     * @param printDebugOut
     * @return
     */
    private boolean isStraightLine(ExtendedStroke lineStroke, boolean printDebugOut){
        ArrayList<Double> directions = lineStroke.directions;

        // calculate a histogram of direction values of all points:
        int histogramBins = 15;
        double binSeparator = 2 * Math.PI / histogramBins;

        // init the histogram:
        int[] histogram = new int[histogramBins];
        for(int i = 0; i < histogram.length; i++){
            histogram[i] = 0;
        }

        // fill the bins of the histogram:
        int countedValues = 0;
        for(Double d : directions) {
            for(int i = 0; i < histogramBins; i++) {
                if(d <= (-Math.PI + (i+1) * binSeparator)) { // directions values range from -PI to +PI
                    histogram[i]++;
                    countedValues++;
                    break;
                }
            }
        }

        /*
        Check deviation of values in the histogram:
            In straight lines there is usually 1-3 adjacent bins holding most of the mass.
            Search for such "peaks" and consider it to be a straight line if the mass value
            exceeds a certain threshold:
         */
        int maxValue = 0;
        int maxIndex = 0;
        for(int i = 0; i < histogram.length; i++) {
            if(histogram[i] > maxValue) {
                maxValue = histogram[i];
                maxIndex = i;
            }
        }

        // add both adjacent bins of the max bin as well, if existent:
        // (the first and the last bin are considered to be adjacent as well)
        if(maxIndex > 0 && maxIndex < histogram.length - 1) {
            maxValue += histogram[maxIndex - 1] + histogram[maxIndex + 1];
        } else if(maxIndex == 0 && maxIndex < histogram.length - 1) {
            maxValue += histogram[histogram.length - 1] + histogram[1];
        } else if(maxIndex > 0 && maxIndex == histogram.length - 1) {
            maxValue += histogram[histogram.length - 2] + histogram[0];
        }

        // dynamically set a decision threshold depending on the length of the line
        // stroke and the mean(width, height) of the canvas:
        double recognitionThreshold;
        double relativeLineLength;
        double meanCanvasLength = (canvasWidth + canvasHeight) / 2.0;
        relativeLineLength = lineStroke.strokeLength / meanCanvasLength;

        if(printDebugOut) Log.d("ClockRecognition", "rel Line length: " + relativeLineLength + "; " + lineStroke.strokeLength +
                "; " + meanCanvasLength);

        if (relativeLineLength <= (4.0 / 12.0)) {
            recognitionThreshold = 0.5;
        } else if(relativeLineLength <= (6.0 / 12.0)){
            recognitionThreshold = 0.6;
        } else {
            recognitionThreshold = 0.75;
        }

        if(printDebugOut) Log.d("ClockRecognition", "line thresh: " + recognitionThreshold);

        boolean isStraightLine = (1.0 * maxValue / countedValues) >= recognitionThreshold;

        if(isStraightLine) return true;

        // Many users tend to overtrace the straight lines that shall be clock hands.
        // Overtracing results in two maxima within the histogram, with the indices delta
        // of the 2 maxima accounting to a value of 7 or 8 and the summed values of the 2 bins
        // matching the same threshold as above.
        // Check this criteria in the following if the above fails:
        int max = histogram[maxIndex];
        int secMax = 0;
        int secMaxIndex = 0;
        for(int i = 0; i < histogram.length; i++) {
            if(i == maxIndex) continue;

            if(histogram[i] > secMax) {
                secMax = histogram[i];
                secMaxIndex = i;
            }
        }

        int indexDelta = Math.abs(maxIndex - secMaxIndex);
        boolean isCorrectDelta = (indexDelta == 7 || indexDelta == 8);

        return isCorrectDelta && (((max + secMax) * 1.0 / countedValues) >= 0.5);
    }

    private boolean isStraightLine(ExtendedStroke lineStroke) {
        return isStraightLine(lineStroke, false);
    }

    /**
     * Check if a point is in a circular range defined by the raoius around another point.
     * @param point
     * @param circleCenter
     * @param radius
     * @return
     */
    private boolean isPointInCircularArea(PointF point, PointF circleCenter, double radius) {
        double pointDist = Math.pow(point.x - circleCenter.x, 2.0) + Math.pow(point.y - circleCenter.y, 2.0);

        return pointDist <= Math.pow(radius, 2.0);
    }

    /**
     * Calculates the angle of a straight line in a circle.
     *
     * @param origin The origin of the circle
     * @param lineStroke The straight line
     * @param useStrokeOrigin Set this to true to use the circle origin as the starting point of
     *                       the straight line or the straight line itself otherwise
     *
     * @return The absolute angle in a circle with value range 0° - 360°.
     */
    private double calcAbsAngle(PointF origin, ExtendedStroke lineStroke, boolean useStrokeOrigin) {
        PointF[] strokePoints = lineStroke.actualStroke.getPoints();
        PointF firstPoint = strokePoints[0];
        PointF lastPoint = strokePoints[strokePoints.length-1];

        PointF rootOrigin = origin;

        PointF endPoint;
        // Set the actual endPoint to the one further away from origin:
        if(euclidDist(firstPoint, origin) > euclidDist(lastPoint, origin)) {
            endPoint = firstPoint;
            if(useStrokeOrigin) origin = endPoint;
        } else {
            endPoint = lastPoint;
            if(useStrokeOrigin) origin = firstPoint;
        }

        //Calculate the angle to the y-axis
        double opposite = Math.abs(endPoint.x - origin.x);
        double hypotenuse = euclidDist(endPoint, origin);

        double angleRad = Math.asin(opposite / hypotenuse);
        double angleDeg = Math.toDegrees(angleRad);

        // calculate the angle in a whole circle given the angle to the y-axis:
        if(endPoint.x >= rootOrigin.x && endPoint.y < rootOrigin.y) angleDeg += 0;
        else if(endPoint.x >= rootOrigin.x && endPoint.y >= rootOrigin.y) angleDeg = 180.0 - angleDeg;
        else if(endPoint.x < rootOrigin.x && endPoint.y >= rootOrigin.y) angleDeg = 180.0 + angleDeg;
        else angleDeg = 360.0 - angleDeg;

        return angleDeg;
    }

    /**
     * Calculates the angle of an imaginary line given by two points.
     * @param origin
     * @param endPoint
     * @return
     */
    private double calcAbsAngle(PointF origin, PointF endPoint) {
        PointF rootOrigin = origin;

        //Calculate the angle to the y-axis
        double opposite = Math.abs(endPoint.x - origin.x);
        double hypotenuse = euclidDist(endPoint, origin);

        double angleRad = Math.asin(opposite / hypotenuse);
        double angleDeg = Math.toDegrees(angleRad);

        // calculate the angle in a whole circle given the angle to the y-axis:
        if(endPoint.x >= rootOrigin.x && endPoint.y < rootOrigin.y) angleDeg += 0;
        else if(endPoint.x >= rootOrigin.x && endPoint.y >= rootOrigin.y) angleDeg = 180.0 - angleDeg;
        else if(endPoint.x < rootOrigin.x && endPoint.y >= rootOrigin.y) angleDeg = 180.0 + angleDeg;
        else angleDeg = 360.0 - angleDeg;

        return angleDeg;
    }


    private String createFinalOutput(String numbOut, String handOut) {
        // Create final output:
        String output = "Bewertung: \n\n";
        if(foundCircleStroke && isCircleStrokeValid) {
            output += "Kontur: 1 Punkt (Kreis vorhanden und nicht verformt)\n";
        } else {
            output += "Kontur: 0 Punkte\n";
            if(foundCircleStroke) {
                output += "(Kreis vorhanden aber unförmig oder nicht richtig geschlossen)\n";
            } else {
                output += "(Kreis konnte nicht gefunden werden)\n";
            }
        }
        output += "\n" + numbOut  + "\n" + handOut;

        return output;
    }

    private String createHTMLoutput(String numbOut, String handOut) {
        // Create final output:
        String output = "<h1>Bewertung </h1> <h2> Uhr: " + timeToDraw + "</h2>";
        if(foundCircleStroke && isCircleStrokeValid) {
            output += "<b>Kontur: 1 Punkt</b> <br> (Kreis vorhanden und nicht stark verformt) <br>";
        } else {
            output += "<b>Kontur: 0 </b> Punkte <br>";
            if(foundCircleStroke) {
                output += "(Kreis vorhanden aber unförmig oder nicht richtig geschlossen)<br>";
            } else {
                output += "(Kreis konnte nicht gefunden werden) <br>";
            }
        }
        output += "<br>" + numbOut  + "<br>" + handOut;

        return output;
    }

    public void reset() {
        strokePairIndex = 0;
        strokePairs.clear();

        recStrokes.clear();
        meanStrokeLength = 0.0;

        groupCombinations.clear();
        recognizedDigits.clear();
        recognizedGroupObjects.clear();

        Arrays.fill(finalNumbers, null);

        circle = null;
        minuteHand = null;
        hourHand = null;

        circleRect = null;
        circleCenter = null;

        foundCircleStroke = false;
        isCircleStrokeValid = false;
        foundValidHands = false;
        possibleHands.clear();
        possibleHands = new ArrayList<>();

        // remove all objects aka strokes already been drawn:
        pageDoc.removeAllObject();
        pageDoc.clearHistory();
        pageDoc.getObjectList().clear();
        sView.update();
    }

    protected void startRecognition() {

       /*----------------------------------------------------------------------------------------------
         The whole clock recognition process starts here:
        ------------------------------------------------------------------------------------------------
       */
        actStrokes = pageDoc.getObjectList(); // all strokes and objects contained in the pageDoc canvas

        // PREPROCESSING: remove all non-strokes, hover strokes and strokes added afterwards:
        List<SpenObjectBase> filterList = new LinkedList<>(); // store all strokes that can be deleted
        for(SpenObjectBase obj : actStrokes) {
            if(obj instanceof SpenObjectStroke) {
                SpenObjectStroke str = (SpenObjectStroke) obj;
                if(str.getColor() == Color.RED  || !str.isVisible()) {
                    str = null;
                    filterList.add(obj);

                } else {
                    ExtendedStroke exStroke = new ExtendedStroke(str);
                    recStrokes.add(exStroke);
                    meanStrokeLength += exStroke.strokeLength; // sum up all stroke lengths
                }
            } else {
                filterList.add(obj);
            }
        }
        // remove all strokes from 'actStrokes' that are contained in the filter list:
        actStrokes.removeAll(filterList);
        filterList.clear();
        filterList = null;

        meanStrokeLength /= recStrokes.size(); // divide the sum of lengths by the number of strokes to get the mean

        // determine an alternative bounding box containing all other strokes, which can be
        // used alternatively in case there is no circle being drawn:
        float minY = 100000000.0f; // "Infinity"
        float minX = minY;
        float maxY = -1000000.0f; // "Negative Infinity"
        float maxX = maxY;

        // find a CIRCLE first and set needed PARAMETERS:
        double circleLengthThresh = 5.0;
        if(recStrokes.size() <= 5) circleLengthThresh = 0.5;
        else if(recStrokes.size() <= 10) circleLengthThresh = 1.0;

        int runIndex = 0;
        List<ExtendedStroke> exFilter = new LinkedList<>();
        for(ExtendedStroke str : recStrokes){
            double lengthRatio = str.strokeLength / meanStrokeLength;
            RectF strokeRect = str.actualStroke.getRect();
            if(strokeRect.right > maxX) maxX = strokeRect.right;
            if(strokeRect.left < minX) minX = strokeRect.left;
            if(strokeRect.top < minY) minY = strokeRect.top;
            if(strokeRect.bottom > maxY) maxY = strokeRect.bottom;

            PointF[] points = str.actualStroke.getPoints();
            PointF startPoint = points[0];
            PointF endPoint = str.actualStroke.getPoints()[points.length - 1];

            double centerPointRadiusThresh = str.strokeLength / 15.0;

            // In most cases the circle is being drawn as a single stroke and is thus a stroke which is much longer than
            // the average:
            if(lengthRatio >= circleLengthThresh && isPointInCircularArea(endPoint, startPoint, centerPointRadiusThresh)) {
                foundCircleStroke = true; //found a long stroke with its start and end points close to each other

                if(circle == null && isNotDistorted(str)) {
                    circle = str;
                    SpenObjectStroke circleStroke = circle.actualStroke;
                    circleRect = circleStroke.getRect();
                    circleCenter = new PointF(circleRect.centerX(), circleRect.centerY());

                    isCircleStrokeValid = true;
                }

                // If there is no circle matching the above criteria, try to find other strokes, that, "glued" together,
                // form a proper circle:
            } else if(lengthRatio >= circleLengthThresh && !foundCircleStroke) {
                ArrayList<PointF> gluedPoints = new ArrayList<>();
                gluedPoints.addAll(Arrays.asList(points));
                List<ExtendedStroke> gluedStrokes = new LinkedList<>();
                gluedStrokes.add(str);
                for (int i = runIndex + 1; i < recStrokes.size(); i++) {
                    ExtendedStroke nextStroke = recStrokes.get(i);
                    double nextStrLengthRatio = nextStroke.strokeLength / meanStrokeLength;
                    PointF nexStrStartPoint = nextStroke.actualStroke.getPoints()[0];

                    //Uncomment for debugging:
//                    SpenObjectStroke cStr = createCircleStroke(gluedPoints.get(gluedPoints.size() - 1), str.strokeLength / 20.0, 200);
//                    cStr.setColor(Color.BLUE);
//                    pageDoc.appendObject(cStr);
//                    sView.closeControl();
//                    sView.update();

                    if (nextStrLengthRatio >= circleLengthThresh &&
                            isPointInCircularArea(nexStrStartPoint, gluedPoints.get(gluedPoints.size() - 1), str.strokeLength / 20.0)) {
                        gluedPoints.addAll(Arrays.asList(nextStroke.actualStroke.getPoints()));
                        gluedStrokes.add(nextStroke);

                        //DEBUGGING:
//                        cStr = createCircleStroke(startPoint, str.strokeLength / 20.0, 200);
//                        cStr.setColor(Color.BLUE);
//                        pageDoc.appendObject(cStr);
//                        sView.closeControl();
//                        sView.update();

                        if (isPointInCircularArea(gluedPoints.get(gluedPoints.size() - 1), startPoint, str.strokeLength / 20.0)) {
                            foundCircleStroke = true;
                            break;
                        }
                    }
                }

                // Actually connect the single strokes to a new stroke and remove the single
                // strokes afterwards:
                if(foundCircleStroke) {
                    int totLength = 0;
                    for(ExtendedStroke glStroke : gluedStrokes) {
                        totLength += glStroke.actualStroke.getPoints().length;
                        exFilter.add(glStroke);
                    }

                    int[] gluedTimes = new int[totLength];
                    float[] gluedPressures = new float[totLength];
                    PointF[] gluedPointsArr = new PointF[10];
                    gluedPointsArr = gluedPoints.toArray(gluedPointsArr);

                    for(ExtendedStroke glStroke : gluedStrokes) {
                        int[] strTimes = glStroke.actualStroke.getTimeStamps();
                        float[] strPressures = glStroke.actualStroke.getPressures();

                        for(int i = 0; i < strTimes.length; i++) {
                            gluedTimes[i] = strTimes[i];
                            gluedPressures[i] = strPressures[i];
                        }
                    }

                    SpenObjectStroke newCircleStroke = new SpenObjectStroke(str.actualStroke.getPenName());
                    newCircleStroke.setPoints(gluedPointsArr, gluedPressures, gluedTimes);

                    ExtendedStroke newCircExStroke = new ExtendedStroke(newCircleStroke);

                    if(circle == null && isNotDistorted(newCircExStroke)) {
                        circle = newCircExStroke;
                        SpenObjectStroke circleStroke = circle.actualStroke;
                        circleRect = circleStroke.getRect();
                        circleCenter = new PointF(circleRect.centerX(), circleRect.centerY());

                        isCircleStrokeValid = true;
                    }
                }
            }
            runIndex++;
        }
        recStrokes.removeAll(exFilter); // remove the single strokes

        if(circle == null) { // there is most likely no circle in the sketch
            // set an alternative bounding box instead of the circle, that is, a Bbox containing
            // all strokes in the drawing:
            circleRect = new RectF(minX, minY, maxX, maxY);
            circleCenter = new PointF(circleRect.centerX(), circleRect.centerY());
        } else {
            recStrokes.remove(circle);
        }

        //DEBUG:
        //drawStrokeRect(circleRect, Color.RED);

        // Set the threshold circle around the center circle, which can be accepted as 'the middle' of the circle:
        double centerThreshArea = (circleRect.width() + circleRect.height()) / (2.0 * 8.0);

        // Make the circular threshold visible for DEBUGGING
//        SpenObjectStroke circArea = createCircleStroke(circleCenter, centerThreshArea, 200);
//        pageDoc.appendObject(circArea);
//        sView.closeControl();
//        sView.update();

        // Filter the circle center point or mini circle, that most users draw as origin of the hands.
        // Criteria: a stroke, where more than 90 percent of the points are in the circular
        //              area around the actual circle center
        List<ExtendedStroke> exFilterList = new LinkedList<>();
        for(ExtendedStroke exStr : recStrokes) {
            // filter very short strokes as well ( crit: < 0.05 * average stroke length)
            if(exStr.strokeLength / meanStrokeLength < 0.05) {
                exFilterList.add(exStr);
            } else {
                PointF[] strPoints = exStr.actualStroke.getPoints();
                int pointsOutsideCenterArea = 0;
                boolean doFilter = true;
                for(PointF p : strPoints) {
                    if(!isPointInCircularArea(p, circleCenter, centerThreshArea)) {
                        pointsOutsideCenterArea++;
                    }
                    if(pointsOutsideCenterArea * 1.0 / strPoints.length > 0.1) {
                        doFilter = false;
                        break;
                    }
                }
                if(doFilter) exFilterList.add(exStr);
            }
        }
        recStrokes.removeAll(exFilterList);
        exFilterList.clear();
        exFilterList = null;

        // CLASSIFICATION of remaining strokes:
        for(ExtendedStroke str : recStrokes) {
            if(str == circle) continue;

            // check if the current strokes is possibly a clock hand:
            // criteria: clock hands start or end in the threshold circle around the
            // actual circle center and are straight lines:
            PointF[] strokePoints = str.actualStroke.getPoints();

            if(isPointInCircularArea(strokePoints[0], circleCenter, centerThreshArea) ||
                    isPointInCircularArea(strokePoints[strokePoints.length - 1], circleCenter, centerThreshArea)) {

                if(isStraightLine(str, true)){
                    possibleHands.add(str);

                } else { // both else cases assume the current stroke is not a hand and thus marked as a number
                    //drawStrokeRect(str.actualStroke, Color.BLUE);
                }
            } else {
                //drawStrokeRect(str.actualStroke, Color.BLUE); // uncomment for DEBUGGING
            }
        }

        // strokes that are most likely hands, do not have to be processed any further
        // -> remove the hands from recStrokes list:
        recStrokes.removeAll(possibleHands);

        // NUMBER grouping and detection starts here
        float multiStrokeNumberHorizThres = circleRect.width() / 20.0f;
        float multiStrokeNumberVertiThres = multiStrokeNumberHorizThres / 4.0f;

        // search for intersecting strokes and put them together in one detection list:
        for(int i = 0; i < recStrokes.size(); i++) {
            ExtendedStroke str = recStrokes.get(i);
            SpenObjectStroke currStroke = str.actualStroke;
            RectF currStrokeRect = currStroke.getRect();
            ArrayList<SpenObjectBase> intersectionList = new ArrayList<>();
            intersectionList.add(currStroke);

            ExtendedStroke predecessor = null;
            ExtendedStroke successor = null;

            if(i > 0) predecessor = recStrokes.get(i-1);
            if(i < recStrokes.size() - 1) successor = recStrokes.get(i+1);

            // "inset" extends the bounding box of a stroke if the according horizontal and vertical
            // parameters are given as negative values:
            currStrokeRect.inset(-multiStrokeNumberHorizThres, -multiStrokeNumberVertiThres);
//            drawStrokeRect(currStrokeRect, Color.MAGENTA);
//            sView.closeControl();
//            sView.update();

            if(predecessor != null && currStrokeRect.intersect(predecessor.actualStroke.getRect())) {
                intersectionList.add(predecessor.actualStroke);
            }
            if(successor != null && currStrokeRect.intersect(successor.actualStroke.getRect())){
                intersectionList.add(successor.actualStroke);
            }

            // Reverse the above insetting:
            currStrokeRect.inset(multiStrokeNumberHorizThres, multiStrokeNumberVertiThres);

            strokePairs.add(intersectionList);
        }

        if(strokePairs.size() > 0)
            recognizeDigits(strokePairs.get(strokePairIndex)); // parallel execution, following code is executed directly
        else {
            Message finMsg = Message.obtain();
            finMsg.obj = "processing_finished";
            textRecHandler.dispatchMessage(finMsg);
        }
    }

    private void setRecognitionHandler() {
        this.textRecHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                String msgString = (String) msg.obj;
                if (!msgString.equals("processing_finished")) return false;
                else {
                    if(isInsideStrokeGroup) {
                        recognizeDigits(new ArrayList<SpenObjectBase>(0)); //dummy list, the function handles everything
                    } else {
                        strokePairIndex++;
                        if (strokePairIndex < strokePairs.size()) {
                            recognizeDigits(strokePairs.get(strokePairIndex));
                        }
                        else {
                            for(RecognizedStroke str : recognizedDigits) {
                                Log.d("ClockRecognition", "detected: " + str.getValue());
                            }

                            //finalNumbers arrays security reset:
                            Arrays.fill(finalNumbers, null);
                            Arrays.fill(isFinalNumberPosCorrect, false);

                            refineDigitMatching();

                            //output final numbers:
                            // check if the numbers are drawn approx. at the correct positions:
                            int correctAngles = 0; // cunt how many numbers are drawn approximately at the right position...
                            int incorrectAngles = 0; //... or at a wrong position
                            String foundNumbers = "";
                            String wrongPosition = "";
                            boolean missingNumbers = false;
                            for(int i = 1; i < finalNumbers.length; i++) {
                                LinkedList<RecognizedStroke> elems = finalNumbers[i];
                                if(elems != null) {
                                    RecognizedStroke currStr = elems.get(0);
                                    PointF rectCenter = currStr.getRectCenter();
                                    double numbAngle = calcAbsAngle(circleCenter, rectCenter);
                                    double correctAngle = i * 30.0;

                                    double lowerBound = correctAngle - 20.0;
                                    double upperBound = correctAngle + 20.0;
                                    if(i == 12) upperBound = 20.0;

                                    if(i != 1 && !foundNumbers.isEmpty()) foundNumbers += ", ";
                                    foundNumbers += i;

                                    if(numbAngle >= lowerBound && numbAngle <= upperBound) {
                                        correctAngles++;
                                        isFinalNumberPosCorrect[i] = true;
                                    } else if(i==12 && (numbAngle >= lowerBound && numbAngle <= 360.0 ||
                                            numbAngle <= upperBound && numbAngle >= 0.0)){
                                        // special case for number 12, which is where the degree scale starts over:
                                        correctAngles++;
                                        isFinalNumberPosCorrect[i] = true;
                                    } else {
                                        incorrectAngles++;
                                        if(!wrongPosition.isEmpty()) wrongPosition += ", ";
                                        wrongPosition += i;
                                    }

                                } else {
                                    missingNumbers = true;
                                }
                            }

                            String numbersOutput;
                            int totalNumbsFound = incorrectAngles + correctAngles;
                            if(!missingNumbers && incorrectAngles == 0) {
                                numbersOutput = "<b>Zahlen: 1 Punkt</b><br> (Alle vorhanden und an korrekter Position)<br>";
                            } else if(missingNumbers && incorrectAngles == 0) {
                                numbersOutput = "<b>Zahlen: 0 Punkte</b><br> (Zahlen fehlen oder wurden nicht erkannt)<br>";
                                numbersOutput += "Erkannt: "  + totalNumbsFound + " von 12 Zahlen (" + foundNumbers + ")<br>";
                                if(correctAngles > 0)
                                    numbersOutput += "Davon an richtiger Position: Alle<br>";
                            } else if(!missingNumbers && incorrectAngles > 0) {
                                numbersOutput = "<b>Zahlen: 0 Punkte</b><br> (Alle Zahlen vorhanden, aber nicht an richtiger Position)<br>";
                                numbersOutput += "Davon an richtiger Position: " + correctAngles + " von " + totalNumbsFound + " Zahlen<br>";
                                numbersOutput += "Falsch positioniert: " + wrongPosition + "<br>";
                            } else {
                                numbersOutput = "<b>Zahlen: 0 Punkte</b><br> (Zahlen fehlen oder wurden nicht erkannt)<br>";
                                numbersOutput += "Erkannt: "  + totalNumbsFound + " von 12 Zahlen (" + foundNumbers + ")<br>";
                                numbersOutput += "Davon an richtiger Position: " + correctAngles + " von " + totalNumbsFound + " Zahlen<br>";
                                numbersOutput += "Falsch positioniert: " + wrongPosition + "<br>";
                            }

                            // set desired hand angles depending on time set:
                            boolean isNonStandardTime = false;

                            RecognizedStroke minuteStroke = null;
                            RecognizedStroke hourStroke = null;
                            double correctHourAngle = 0.0;
                            double correctMinuteAngle = 0.0;
                            switch (timeToDraw) {
                                case "Zehn nach elf":
                                    correctHourAngle = 11.0 * 30.0;
                                    correctMinuteAngle = 2.0 * 30.0;

                                    hourStroke = finalNumbers[11] != null ? finalNumbers[11].get(0) : null;
                                    minuteStroke = finalNumbers[2] != null ? finalNumbers[2].get(0) : null;

                                    if(!isFinalNumberPosCorrect[11]) hourStroke = null;
                                    if(!isFinalNumberPosCorrect[2]) minuteStroke = null;
                                    break;

                                case "Fünf nach vier":
                                    correctHourAngle = 4.0 * 30.0;
                                    correctMinuteAngle = 1.0 * 30.0;

                                    hourStroke = finalNumbers[4] != null ? finalNumbers[4].get(0) : null;
                                    minuteStroke = finalNumbers[1] != null ? finalNumbers[1].get(0) : null;

                                    if(!isFinalNumberPosCorrect[4]) hourStroke = null;
                                    if(!isFinalNumberPosCorrect[1]) minuteStroke = null;
                                    break;

                                case "Zehn nach neun":
                                    correctHourAngle = 9.0 * 30.0;
                                    correctMinuteAngle = 2.0 * 30.0;

                                    hourStroke = finalNumbers[9] != null ? finalNumbers[9].get(0) : null;
                                    minuteStroke = finalNumbers[2] != null ? finalNumbers[2].get(0) : null;

                                    if(!isFinalNumberPosCorrect[9]) hourStroke = null;
                                    if(!isFinalNumberPosCorrect[2]) minuteStroke = null;
                                    break;

                                default: // A non-standard time has been chosen. No hand validation can be performed
                                    isNonStandardTime = true;
                                    break;
                            }

                            String handOutput = "";

                            if(isNonStandardTime) {
                                handOutput = "Keine Standard-MOCA-Zeit gewählt und somit keine Erkennung möglich.";
                                createFinalOutput(numbersOutput, handOutput);
                                return true;
                            }

                            // update the hand lengths to the distance of the point closest and farest
                            // away from the circle center (necessary for overtraced hands):
                            for(ExtendedStroke hand : possibleHands) {
                                PointF[] points = hand.actualStroke.getPoints();
                                PointF startPoint = points[0];
                                PointF endPoint = points[points.length - 1];

                                PointF closestPoint;
                                PointF farestPoint;
                                if(euclidDist(startPoint, circleCenter) <= euclidDist(endPoint, circleCenter)) {
                                    closestPoint = startPoint;
                                    farestPoint = endPoint;
                                } else{
                                    closestPoint = endPoint;
                                    farestPoint = startPoint;
                                }

                                double farestDistance = euclidDist(farestPoint, circleCenter);
                                for(int i = 0; i < points.length; i++) {
                                    double currDist = euclidDist(points[i], circleCenter);
                                    if(currDist > farestDistance) {
                                        farestDistance = currDist;
                                        farestPoint = points[i];
                                    }
                                }

                                hand.handLength = euclidDist(closestPoint, farestPoint);
                                hand.farestPoint = new PointF(farestPoint.x, farestPoint.y);
                                hand.closestPoint = closestPoint;
                            }

                            if(possibleHands.size() == 2) {
                                if(possibleHands.get(0).strokeLength >= possibleHands.get(1).strokeLength){
                                    minuteHand = possibleHands.get(0);
                                    hourHand = possibleHands.get(1);
                                } else {
                                    minuteHand = possibleHands.get(1);
                                    hourHand = possibleHands.get(0);
                                }

                                double lengthDelta = Math.abs(minuteHand.strokeLength - hourHand.strokeLength);
                                if(lengthDelta >= 0.125 * minuteHand.strokeLength) {
                                    foundValidHands = true;

                                } else {
                                    foundValidHands = false;
                                    handOutput = "<b>Zeiger: 0 Punkte</b> <br>(2 Zeiger gefunden, aber Längenunterschied nicht erkennbar)";
                                }

                            } else {
                                foundValidHands = false;
                                if(possibleHands.size() > 2)
                                    handOutput = "<b>Zeiger: 0 Punkte</b> <br> (Mehr als zwei Zeiger vorhanden)";
                                else if(possibleHands.size() == 1)
                                    handOutput = "<b>Zeiger: 0 Punkte</b> <br>(Nur ein mittig platzierter Zeiger vorhanden)";
                                else
                                    handOutput = "<b>Zeiger: 0 Punkte</b> <br> (Kein Zeiger erkannt oder die Zeiger liegen deutlich außerhalb der Kreismitte)";
                            }

                            // found 2 hands and now check their angle or if they point at
                            // the right number respectively:
                            if(foundValidHands) {
                                double mAngle = calcAbsAngle(minuteHand.closestPoint, minuteHand.farestPoint);
                                double hAngle = calcAbsAngle(hourHand.closestPoint, hourHand.farestPoint);

                                boolean hourCorrect = false;
                                boolean minuteCorrect = false;

                                if(minuteStroke != null) {
                                    PointF mStrokeCenter = minuteStroke.getRectCenter();
                                    double mStrokeAngle = calcAbsAngle(circleCenter, mStrokeCenter);

                                    if(Math.abs(mAngle - mStrokeAngle) <= 16.0)
                                        minuteCorrect = true;

                                } else {
                                    // in case the number the hand should point at is missing, measure the angle of the hand:
                                    if(Math.abs(mAngle - correctMinuteAngle) <= 20.0)
                                        minuteCorrect = true;
                                }

                                if(hourStroke != null) {
                                    PointF hStrokeCenter = hourStroke.getRectCenter();
                                    double hStrokeAngle = calcAbsAngle(circleCenter, hStrokeCenter);

                                    if(Math.abs(hAngle - hStrokeAngle) <= 15.0)
                                        hourCorrect = true;
                                    else if((hAngle - hStrokeAngle <= 20.0) && (hAngle - hStrokeAngle >= 0.0))
                                        hourCorrect = true; // hour hands may be positioned a bit after the actual number

                                } else {
                                    // in case the number the hand should point at is missing, measure the angle of the hand:
                                    if(Math.abs(hAngle - correctHourAngle) <= 20.0)
                                        hourCorrect = true;
                                }

                                if(hourCorrect && minuteCorrect) {
                                    handOutput = "<b>Zeiger: 1 Punkt</b> <br> (Zeiger vorhanden und auf richtige Zeit gesetzt)";
                                } else {
                                    handOutput = "<b>Zeiger: 0 Punkte</b> <br>";
                                    if(!hourCorrect) handOutput += "(Stundenzeiger nicht auf richtiger Position)<br>";
                                    if(!minuteCorrect) handOutput += "(Minutenzeiger nicht auf richtiger Position)";
                                }

                            }

                            createFinalOutput(numbersOutput, handOutput);
                            htmlOut = createHTMLoutput(numbersOutput, handOutput);

                            Message finMsg = Message.obtain();
                            finMsg.obj = "ai_finished";
                            finHandler.dispatchMessage(finMsg);
                        }
                    }
                    return true;
                }
            }
        });

    }

    private void initTextRecognition() {
        textRecManager = new SpenTextRecognitionManager(context);
        infoList = textRecManager.getInfoList(SpenObjectBase.TYPE_STROKE, SpenObjectBase.TYPE_CONTAINER);

        textRec = null;

        for (SpenRecognitionInfo info : infoList) {
            if (info.name.equalsIgnoreCase("SpenText")) {
                try {
                    textRec = textRecManager.createRecognition(info);
                } catch (Exception e) {
                    Toast.makeText(context, "Initialisierungsfehler. Bitte als Hauptbenutzer dieses Gerätes anmelden!",
                            Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                    Log.e("ClockRecognition", "ERROR in text recognition init!");
                }
            }
        }

        if (textRec != null) {
            List<String> languages = textRec.getSupportedLanguage();
            for (String lang : languages) {
                if(lang.equalsIgnoreCase("eng")) {
                    textRec.setLanguage(lang);
                    Log.d("ClockRecognition", "Language has been set.");
                }
            }

            try {
                textRec.setResultListener(new SpenRecognitionBase.ResultListener() {
                    @Override
                    public void onResult(List<SpenObjectBase> in, List<SpenObjectBase> out) {
                        for (SpenObjectBase outT : out) {
                            SpenObjectTextBox text;
                            if (outT instanceof SpenObjectTextBox) {
                                String recText = ((SpenObjectTextBox) outT).getText();
                                RecognizedStroke groupStroke = new RecognizedStroke(recText, in);

                                recognizedGroupObjects.add(groupStroke);
                            }
                        }

                        isProcessingText = false; // finished processing current input
                        if(groupCombinations.size() == 0) { // detection on current group has been finished
                            isInsideStrokeGroup = false;

                            // finish the current group:
                            double minAngleDelta = 100000.0;
                            RecognizedStroke bestGuess = null;
                            int digitValue;
                            for(RecognizedStroke currGroup : recognizedGroupObjects) {
                                String strValue = currGroup.getValue();

                                double actualAngle = calcAbsAngle(circleCenter, currGroup.getRectCenter());

                                try {
                                    if(strValue.equals("g") || strValue.equalsIgnoreCase("s") && in.size() == 1)  {
                                        strValue = "9";
                                        currGroup.setValue(strValue);
                                    } else if(strValue.equalsIgnoreCase("s")) {
                                        strValue = "5";
                                        currGroup.setValue(strValue);
                                    } else if(strValue.equals("N")) {
                                        strValue = "11";
                                        currGroup.setValue(strValue);
                                    }

                                    double numberAngle, angleDelta;
                                    digitValue = Integer.parseInt(strValue);

                                    // these are often detected instead of 11:
                                    if(digitValue == 17 || digitValue == 71 || digitValue == 77) {
                                        digitValue = 11;
                                        currGroup.setValue("11");

                                    } else if(digitValue == 72) { // same procedure for 12 ...
                                        digitValue = 12;
                                        currGroup.setValue("12");

                                    } else if (digitValue == 70) { // ... and 10
                                        digitValue = 10;
                                        currGroup.setValue("10");
                                    }

                                    if(digitValue > 0 && digitValue < 13) {
                                        numberAngle = digitValue * 30.0;

                                        if(digitValue == 12 && actualAngle < 180.0) numberAngle = 0.0;
                                        angleDelta = Math.abs(actualAngle - numberAngle);

                                        if(angleDelta < minAngleDelta) {
                                            minAngleDelta = angleDelta;
                                            bestGuess = new RecognizedStroke(currGroup.getValue(), currGroup.getStrokes());
                                        }

                                    } else {
                                        //Log.e("ClockRecognition", "Verworfen: " + digitValue + " an " + actualAngle);
                                    }
                                } catch (NumberFormatException e) {
                                    //Log.e("ClockRecognition", "filtered: " + currGroup.getValue() + " at " + actualAngle);
                                }
                            }

                            if(bestGuess != null) recognizedDigits.add(bestGuess);
                            recognizedGroupObjects.clear();
                        }

                        Message finMsg = Message.obtain();
                        finMsg.obj = "processing_finished";
                        textRecHandler.dispatchMessage(finMsg);
                    }
                });
                Log.d("ClockRecognition", "result listener has been set!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void recognizeDigits(ArrayList<SpenObjectBase> strokes) {
        if(isInsideStrokeGroup) {
            ArrayList<SpenObjectBase> currGroup = groupCombinations.get(0);
            groupCombinations.remove(0);

            isProcessingText = true;
            try {
                textRec.request(currGroup);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("ClockRecognition", "Recognition Error!");
            }

        } else if(strokes.size() > 1) {
            // create all possible subsets from the current set of strokes:
            // resource: http://www.geeksforgeeks.org/finding-all-subsets-of-a-given-set-in-java/
            ArrayList<SpenObjectBase> currSubset;
            for(int i = 0; i < (1 << strokes.size()); i++) {
                currSubset = new ArrayList<>();
                for(int j = 0; j < strokes.size(); j++) {
                    if((i & (1 << j)) > 0) {
                        currSubset.add(strokes.get(j));
                    }
                }
                if(currSubset.size() > 0) groupCombinations.add(currSubset); // filter empty set
            }

            isInsideStrokeGroup = true;
            ArrayList<SpenObjectBase> currGroup = groupCombinations.get(0);
            groupCombinations.remove(0);

            isProcessingText = true;
            try {
                textRec.request(currGroup);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("ClockRecognition", "Recognition Error!");
            }
        } else {
            isProcessingText = true;
            try {
                if(strokes != null) textRec.request(strokes);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("ClockRecognition", "Recognition Error!");
            }
        }
    }

    /**
     * Needs to be called before this ClockRecognizer Object instance is destroyed
     * to prevent memory leaks!
     */
    public void close() {
        if(textRec != null) {
            textRecManager.destroyRecognition(textRec);
            textRecManager.close();
        }
    }

}
