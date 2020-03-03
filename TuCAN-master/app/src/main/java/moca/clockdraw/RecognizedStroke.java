package moca.clockdraw;

import android.graphics.PointF;
import android.graphics.RectF;

import com.samsung.android.sdk.pen.document.SpenObjectBase;
import com.samsung.android.sdk.pen.document.SpenObjectStroke;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a stroke that has been recognized. It can consist of multiple strokes and alwas
 * holds one recognized value.
 */
public class RecognizedStroke {
    private String value;
    private ArrayList<SpenObjectBase> strokes;

    public RecognizedStroke(String recognizedValue, List<SpenObjectBase> strokes) {
        this.value = recognizedValue;
        this.strokes = new ArrayList<>();
        this.strokes.addAll(strokes);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ArrayList<SpenObjectBase> getStrokes() {
        return strokes;
    }

    public SpenObjectStroke getStrokeAt(int position) {
        if(position >= strokes.size() || position < 0) {
            throw new IndexOutOfBoundsException("Position does not correspond to a valid Stroke Object");
        } else {
            return (SpenObjectStroke) strokes.get(position);
        }
    }

    public RectF getRect() {
        if(strokes.size() == 1) {
            return strokes.get(0).getRect();
        } else {
            RectF firstRect = strokes.get(0).getRect();
            float minX = firstRect.left;
            float minY = firstRect.top;
            float maxX = firstRect.right;
            float maxY = firstRect.bottom;

            for(int i = 1; i < strokes.size(); i++) {
                RectF currRect = strokes.get(i).getRect();
                if(currRect.left < minX) minX = currRect.left;
                if(currRect.top < minY) minY = currRect.top;
                if(currRect.right > maxX) maxX = currRect.right;
                if(currRect.bottom > maxY) maxY = currRect.bottom;
            }

            RectF newRect = new RectF(minX, minY, maxX, maxY);

            return newRect;
        }
    }

    public PointF getRectCenter() {
        RectF bBox = this.getRect();
        PointF center = new PointF(bBox.centerX(), bBox.centerY());

        return center;
    }

}
