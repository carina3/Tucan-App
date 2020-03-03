package moca.clockdraw;

class Datenverarbeitung {

//    static ArrayList<Double> GeschwindigkeitTask(ArrayList<SPenData> data){
//        ArrayList<Double> velo = new ArrayList<Double>();
//        velo.add((double) 0);
//        int versuch;
//        for (int punkt = 0; punkt< data.size()-1; punkt++){
//            velo.add(berechneGewindigkeit(data.get(punkt), data.get(punkt +1)));
//        }
//        return velo;
//    }

    static double berechneGewindigkeit(SPenData erster, SPenData zweiter){
        float x1, x2, y1, y2, deltaX, deltaY;
        x1 = erster.getxCoord();
        y1 = erster.getyCoord();
        x2 = zweiter.getxCoord();
        y2 = zweiter.getyCoord();

        deltaX = Math.abs(x1 - x2);
        deltaY = Math.abs(y1 - y2);

        double s = Math.sqrt( deltaX*deltaX + deltaY*deltaY );
        int t1, t2;
        t1 = erster.getTimestamp();
        t2 = zweiter.getTimestamp();
        int t = t2-t1;
        if (t==0){return 0.0;}

        return s / t;
    }

}
