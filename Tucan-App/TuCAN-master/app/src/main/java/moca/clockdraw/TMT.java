package moca.clockdraw;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Sylvia on 16.11.2017.
 */

public class TMT {

    //The MoCA-TMT consists of 10 Points with the letters A-E and the numbers 1-5.
    //These Points are additionally kept in a list in that has already the correct path order.
    //-> so checking if path was correct will be easy

    public ArrayList<PointF> circlePoints;
    public ArrayList<String> stringList;
    public float diameter; //Diameter of circles
    public PointF pointA;
    public PointF pointB;
    public PointF pointC;
    public PointF pointD;
    public PointF pointE;
    public PointF point1;
    public PointF point2;
    public PointF point3;
    public PointF point4;
    public PointF point5;

    public TMT(ArrayList<PointF> circlePoints, ArrayList<String> stringList, float diameter) {
        this.circlePoints = circlePoints;
        this.stringList = stringList;
        this.diameter = diameter;


        this.point1 = circlePoints.get(0);
        this.pointA = circlePoints.get(1);
        this.point2 = circlePoints.get(2);
        this.pointB = circlePoints.get(3);
        this.point3 = circlePoints.get(4);
        this.pointC = circlePoints.get(5);
        this.point4 = circlePoints.get(6);
        this.pointD = circlePoints.get(7);
        this.point5 = circlePoints.get(8);
        this.pointE = circlePoints.get(9);


    }

    public TMT(ArrayList<PointF> circlePoints, ArrayList<String> stringList, float diameter, float factor) {
        for (int i = 0; i< circlePoints.size(); i++){
            circlePoints.set(i, new PointF(circlePoints.get(i).x *factor, circlePoints.get(i).y * factor));
        }
        this.circlePoints = circlePoints;
        this.stringList = stringList;
        this.diameter = diameter*factor;


        this.point1 = circlePoints.get(0);
        this.pointA = circlePoints.get(1);
        this.point2 = circlePoints.get(2);
        this.pointB = circlePoints.get(3);
        this.point3 = circlePoints.get(4);
        this.pointC = circlePoints.get(5);
        this.point4 = circlePoints.get(6);
        this.pointD = circlePoints.get(7);
        this.point5 = circlePoints.get(8);
        this.pointE = circlePoints.get(9);


    }

    /*
    Constructors for the 3 different versions. coordinates were measured in mm.
    */

//Constructor TMT 1
    public static TMT createTMT1(float factor) {

        final PointF pointD = new PointF(12.5f, 45);
        final PointF point5 = new PointF(9.5f, 21);
        final PointF pointB = new PointF(43, 25.5f);
        final PointF point2 = new PointF(60, 26);
        final PointF point4 = new PointF(39, 45);
        final PointF point1 = new PointF(25, 32);
        final PointF pointE = new PointF(24.5f, 9.5f);
        final PointF pointA = new PointF(47.5f, 10);
        final PointF point3 = new PointF(59.5f, 48);
        final PointF pointC = new PointF(24, 58);

        final float diameter = 6.9f;


        final ArrayList<String> signs = new ArrayList<String>();
        signs.addAll(Arrays.asList("1", "A", "2", "B", "3", "C", "4", "D", "5", "E"));

        ArrayList<PointF> circlePoints = new ArrayList<PointF>();
        circlePoints.addAll(Arrays.asList(point1, pointA, point2, pointB, point3, pointC, point4, pointD, point5, pointE));

        TMT tmt1 = new TMT(circlePoints, signs, diameter, factor);

        return tmt1;
    }

    ;

//Constructor TMT 2
    public static TMT createTMT2(float factor) {

        final PointF point2 = new PointF(12.5f, 45);
        final PointF point3 = new PointF(9.5f, 21);
        final PointF point4 = new PointF(43, 25.5f);
        final PointF point5 = new PointF(60, 26);
        final PointF point1 = new PointF(39, 45);
        final PointF pointB = new PointF(25, 32);
        final PointF pointC = new PointF(24.5f, 9.5f);
        final PointF pointD = new PointF(47.5f, 10);
        final PointF pointE = new PointF(59.5f, 48);
        final PointF pointA = new PointF(24, 58);

        final float diameter = 6.9f;



        final ArrayList<String> signs = new ArrayList<String>();
        signs.addAll(Arrays.asList("1", "A", "2", "B", "3", "C", "4", "D", "5", "E"));

        ArrayList<PointF> circlePoints = new ArrayList<PointF>();
        circlePoints.addAll(Arrays.asList(point1, pointA, point2, pointB, point3, pointC, point4, pointD, point5, pointE));

        TMT tmt2 = new TMT(circlePoints, signs, diameter, factor);

        return tmt2;
    }

    ;

    //Constructor TMT 3
    public static TMT createTMT3(float factor) {

        final PointF point1 = new PointF(12.5f, 45);
        final PointF point2 = new PointF(9.5f, 21);
        final PointF point3 = new PointF(43, 25.5f);
        final PointF point4 = new PointF(60, 26);
        final PointF point5 = new PointF(39, 45);
        final PointF pointA = new PointF(25, 32);
        final PointF pointB = new PointF(24.5f, 9.5f);
        final PointF pointC = new PointF(47.5f, 10);
        final PointF pointD = new PointF(59.5f, 48);
        final PointF pointE = new PointF(24, 58);

        final float diameter = 6.9f;


        final ArrayList<String> signs = new ArrayList<String>();
        signs.addAll(Arrays.asList("1", "A", "2", "B", "3", "C", "4", "D", "5", "E"));

        ArrayList<PointF> circlePoints = new ArrayList<PointF>();
        circlePoints.addAll(Arrays.asList(point1, pointA, point2, pointB, point3, pointC, point4, pointD, point5, pointE));

        TMT tmt3 = new TMT(circlePoints, signs, diameter, factor);

        return tmt3;
    }

    ;


    //Getter
    public ArrayList<PointF> getCirclePoints() {
        return circlePoints;
    }


    public float getDiameter() {
        return diameter;
    }


}
