package moca.clockdraw;

import android.graphics.PointF;
import com.samsung.android.sdk.pen.document.SpenObjectStroke;
import java.util.ArrayList;

/**
 * Created by Jannik on 12.12.2017.
 */

/**
 * Represents a stroke with computated features as the direction graph and length.
 */
public class ExtendedStroke {
    SpenObjectStroke actualStroke;
    double strokeLength;
    ArrayList<Double> directions;

    // only needed for clock hands:
    PointF farestPoint;
    PointF closestPoint;
    double handLength;

    public ExtendedStroke(SpenObjectStroke actualStroke) {
        this.actualStroke = actualStroke;
        this.strokeLength = calculateStrokeLength(this.actualStroke);
        directions = calculateAllPointDirections();
    }

    /**
     * Calculates the total length of a stroke by summing up the Euclidian distances of subsequent points.
     *
     * @param stroke The stroke whose length shall be calculated.
     * @return The length value as double representation.
     */
    private double calculateStrokeLength(SpenObjectStroke stroke) {
        PointF[] strokePoints = stroke.getPoints();
        double totalLength = 0.0;
        PointF currPoint;
        PointF prePoint;
        for(int i = 1; i < strokePoints.length; i++) {
            currPoint = strokePoints[i];
            prePoint = strokePoints[i-1];
            // Calculate the euclidian distance:
            totalLength += euclidDist(currPoint, prePoint); // add the point distance to total stroke length
        }

        return totalLength;
    }

    /**
     * Calculates the Euclidian Distance between two points a and b.
     *
     * @param a Point a
     * @param b Point b
     * @return Double value representing the Euclidian Distance
     */
    public double euclidDist(PointF a, PointF b) {
        double dist = Math.pow(a.x - b.x, 2.0) + Math.pow(a.y - b.y, 2.0);
        dist = Math.sqrt(dist);

        return dist;
    }

    /**
     * Calculates a direction value between two points p0 and p1 as proposed in:
     * Yu and Cai, 2003: "A domain-independent system for sketch recognition"
     *
     * @param p0
     * @param p1
     * @return The distance value as double representation.
     */
    private double pointDirection(PointF p0, PointF p1) {
        // atan2 returns more precise values in this case than atan
        return Math.atan2((p1.y - p0.y), (p1.x - p0.x));
    }

    /**
     * Calculate the direction value for all consecutive points in the stroke.
     *
     * @return An ArrayList of Doubles representing the direction values.
     */
    private ArrayList<Double> calculateAllPointDirections() {
        PointF[] points = actualStroke.getPoints();
        ArrayList<Double> dirs = new ArrayList<>(points.length-1);
        for(int i = 1; i < points.length; i++) {
            PointF currPoint = points[i];
            PointF lastPoint = points[i-1];
            if(currPoint.equals(lastPoint)) continue; //skip equal successive points
            dirs.add(pointDirection(lastPoint, currPoint));
        }

        return dirs;
    }
}
