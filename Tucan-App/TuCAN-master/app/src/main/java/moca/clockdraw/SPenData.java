package moca.clockdraw;

/**
 * Created by Tobias on 02.11.2017.
 */

public class SPenData {
    private int timestamp;
    private float xCoord;
    private float yCoord;
    private float pressure;
    private float velocitiy;
    private int motionEventType;

    public SPenData(int timestamp, float xCoord, float yCoord, float pressure, int motionEventType, float velocity) {
        this.timestamp = timestamp;
        this.xCoord = xCoord;
        this.yCoord = yCoord;
        this.pressure = pressure;
        this.motionEventType = motionEventType;
        this.velocitiy = velocity;
    }


    public int getTimestamp() {
        return timestamp;
    }

    public float getxCoord() { return xCoord; }

    public float getyCoord() {
        return yCoord;
    }

    public float getPressure() {
        return pressure;
    }

    public int getMotionEventType() {
        return motionEventType;
    }

    public float getVelocitiy() { return velocitiy; }
}
