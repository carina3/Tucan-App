package moca.clockdraw;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class MyComparatorY implements Comparator<SPenData> {
    @Override
    public int compare(SPenData o1, SPenData o2) {
        if (o1.getyCoord() < o2.getyCoord()) {
            return -1;
        } else if (o1.getyCoord() > o2.getyCoord()) {
            return 1;
        }
        return 0;
    }}

class MyComparatorX implements Comparator<SPenData> {
    @Override
    public int compare(SPenData o1, SPenData o2) {
        if (o1.getxCoord() < o2.getxCoord()) {
            return -1;
        } else if (o1.getxCoord() > o2.getxCoord()) {
            return 1;
        }
        return 0;
    }}

class MyComparatorVelocitiy implements Comparator<SPenData> {
    @Override
    public int compare(SPenData o1, SPenData o2) {
        if (o1.getVelocitiy() < o2.getVelocitiy()) {
            return -1;
        } else if (o1.getVelocitiy() > o2.getVelocitiy()) {
            return 1;
        }
        return 0;
    }}

class MyComparatorAufsteigend implements Comparator<Float> {
    @Override
    public int compare(Float o1, Float o2) {
        if (o1 < o2) {
            return -1;
        } else if (o1 > o2) {
            return 1;
        }
        return 0;
    }}

class Analyse {
    static private float ebenenTolleranz = 30;
    static private float gruppenTolleranz = 25;
    static private ArrayList<SPenData> linie = new ArrayList();

    static ArrayList<File> rueckgabe_bilder = new ArrayList();
    static ArrayList rueckgabe_zahlen = new ArrayList();
    static ArrayList rueckgabe_beschreibung_bilder = new ArrayList();
    static ArrayList rueckgabe_beschreibung_zahlen = new ArrayList();

    static ArrayList rueckgabe = new ArrayList();

    // Ablaufplan des Algorithmus
    // Filtern der gesammelten Daten
    // 1) Start- und Endpunkte 2) Langsamme Punkte(VelocityCero)
    // 3) Löschen kurzer Striche und Punkte, um Falscheingaben zu vermeiden
    // 4) Gruppierung aller Start- und Endpunkte mit umliegenden langsamen Punkten
    // 5) Filterung der gemittelten gruppierten Punkte
    // 6) Prüfen aller Kombinationen sobald Punktzahl <= 9
    // 6) Bewertung und Vermessung

    static ArrayList<ArrayList> ki(Bitmap bmp, ArrayList<SPenData> task2, boolean form){
        double gezeichnet;

        //###########################################################################################
        // Beginn und Ende jeder Linie, da diese meist in den Ecken anfangen oder enden
        ArrayList<SPenData> StartundEndpunkte = kurzeStricheFiltern(StartUndEndpunkte(task2));
        Paint paint = new Paint();                          //define paint and paint color
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        zeichneArraylist(StartundEndpunkte, bmp, "1 StartundEndpunkte", paint, wertNormierung(4), false);
        rueckgabe_beschreibung_bilder.add("Alle Start- und Endpunkte");

        gruppenTolleranz = berechneGruppenTolleranz(StartundEndpunkte);
        //###########################################################################################
        // Zur Segmentierung einer Linie in mehrere Kanten des Würfels eignen sich die langsamsten Punkte
        ArrayList<SPenData> VelocitiCero = VelocityCero(task2);
        Paint paint2 = new Paint();                          //define paint and paint color
        paint2.setColor(Color.BLUE);
        paint2.setTextSize(wertNormierung(25));
        paint2.setStyle(Paint.Style.FILL_AND_STROKE);
        zeichneArraylist(VelocitiCero, bmp, "2 VelocitiCero", paint2, wertNormierung(4), false);
        rueckgabe_beschreibung_bilder.add("Punkte mit geringer Geschwindigkeit");
        Paint paint3 = new Paint();                          //define paint and paint color
        paint3.setColor(Color.BLACK);
        paint3.setStrokeWidth(2);
        paint3.setStyle(Paint.Style.STROKE);

        //###########################################################################################
        // Nun werden die Gruppen gemittelt, aber noch nicht gefiltert
        ArrayList<ArrayList<SPenData>> GruppiertePunkte = GruppiertePunkte(StartundEndpunkte, VelocitiCero);
        ArrayList<SPenData> gruppenMittelpunkt = new ArrayList<>();
        for (int i=0; i < GruppiertePunkte.size(); i++){
            gruppenMittelpunkt.add(GruppiertePunkte.get(i).get(0));
        }
        //In diesem Bereich werden die Start-und Endpunkte und die VelocityCero-Punkte zusammengefasst
        zeichneArraylist(gruppenMittelpunkt, bmp, "3 Suchfeld", paint3, (int)gruppenTolleranz, false);
        rueckgabe_beschreibung_bilder.add("Suchfelder zum gruppieren");

        ArrayList<SPenData> gemittelteEckpunkte = Eckpunkte(GruppiertePunkte);
        Paint paint4 = new Paint();                          //define paint and paint color
        paint4.setColor(Color.GREEN);
        paint4.setTextSize(wertNormierung(25));
        paint4.setStyle(Paint.Style.FILL_AND_STROKE);
        Paint paint5 = new Paint();                          //define paint and paint color
        paint5.setColor(Color.CYAN);
        paint5.setTextSize(wertNormierung(25));
        paint5.setStyle(Paint.Style.FILL_AND_STROKE);
        Paint paint6 = new Paint();
        paint6.setColor(Color.RED);
        paint6.setTextSize(wertNormierung(30));
        paint6.setStyle(Paint.Style.FILL_AND_STROKE);
        zeichneArraylist(gemittelteEckpunkte, bmp, "4 gemittelteEckpunkte", paint4, wertNormierung(6), false);
        rueckgabe_beschreibung_bilder.add("Gemittelte Eckpunkte");
        //###########################################################################################
        // Nun werden die gefundenen Gruppen gefiltert, um acht Eckpunkte zu identifizieren
        Log.d("Tobii", "Eckpunkte: "+ gemittelteEckpunkte.size());
        ebenenTolleranz = 10;
        ArrayList<SPenData>gemittelteSortierteEckpunkte = gemittelteEckpunkte;
        if (gemittelteSortierteEckpunkte.size() == 8){
            gemittelteSortierteEckpunkte = EbenenOrdnen(gemittelteSortierteEckpunkte);
            int vorhanden = EckpunkteVerbunden(gemittelteSortierteEckpunkte, task2);
            gezeichnet = bewerteWuerfel(gemittelteSortierteEckpunkte, vorhanden, form);
            if (gezeichnet != -1){
                zeichneArraylist(gemittelteSortierteEckpunkte, bmp, "4.2 gemittelteSortierteEckpunkte", paint4, wertNormierung(6), false);
                rueckgabe_beschreibung_bilder.add("Gemittelt und gefilterte Eckpunkte");
                zeichneArraylist(linie, bmp, "6 Linien zwischen Punkte", paint5, wertNormierung(2), false);
                zeichneArraylist(gemittelteSortierteEckpunkte, bmp, "Nummerierte Eckpunkte", paint6, wertNormierung(2), true);

                rueckgabe_beschreibung_bilder.add("Nummerierte Eckpunkte");
                rueckgabe_beschreibung_bilder.add("Linien zwischen Eckpunkte");
            }else {
                rueckgabe_zahlen.add(0);
                rueckgabe_beschreibung_zahlen.add("Erreichte Punktzahl: ");
                rueckgabe_zahlen.add(gemittelteSortierteEckpunkte.size());
                rueckgabe_beschreibung_zahlen.add("Anzahl identifizierter Eckpunkte: ");
                int gezeichneteLinien = EckpunkteVerbunden(gemittelteSortierteEckpunkte, task2);
                rueckgabe_zahlen.add(gezeichneteLinien);
                rueckgabe_beschreibung_zahlen.add("Anzahl gezeichnete Linien: ");
                 zeichneArraylist(linie, bmp, "6 Linien zwischen Punkte", paint5, wertNormierung(2), false);
                zeichneArraylist(gemittelteSortierteEckpunkte, bmp, "Nummerierte Eckpunkte", paint6, wertNormierung(2), true);
                rueckgabe_beschreibung_bilder.add("Nummerierte Eckpunkte");
                rueckgabe_beschreibung_bilder.add("Linien zwischen Eckpunkte");
            }
        }else {
            if (gemittelteSortierteEckpunkte.size() >= 8){
                int vorhanden;

                gezeichnet = -1;

                while (gezeichnet == -1 && ebenenTolleranz < 50){
                    vorhanden = 0;
                    if (gemittelteSortierteEckpunkte.size() == 8) { vorhanden = EckpunkteVerbunden(EbenenOrdnen(gemittelteSortierteEckpunkte), task2);}
                    if (gemittelteSortierteEckpunkte.size() == 9){gemittelteSortierteEckpunkte = kompinatiorik(gemittelteSortierteEckpunkte, task2);}
                    //kombinatorik ändert die größe auf die passende menge fals diese gefunden wird.
                    if (gemittelteSortierteEckpunkte.size() == 8){
                        gemittelteSortierteEckpunkte = EbenenOrdnen(gemittelteSortierteEckpunkte);
                        vorhanden = EckpunkteVerbunden(gemittelteSortierteEckpunkte, task2);
                        gezeichnet = bewerteWuerfel(gemittelteSortierteEckpunkte, vorhanden, form);}
                    if (gezeichnet == -1){gemittelteSortierteEckpunkte = sortiereEckpunkteX(gemittelteEckpunkte);}
                    ebenenTolleranz++;
                }
                if (gezeichnet != -1){
                    zeichneArraylist(gemittelteSortierteEckpunkte, bmp, "4.2 gemittelteSortierteEckpunkte", paint4, wertNormierung(6), false);
                    rueckgabe_beschreibung_bilder.add("Gemittelt und gefilterte Eckpunkte");
                    zeichneArraylist(linie, bmp, "6 Linien zwischen Punkte", paint5, wertNormierung(2), false);
                    zeichneArraylist(gemittelteSortierteEckpunkte, bmp, "Nummerierte Eckpunkte", paint6, wertNormierung(2), true);
                    rueckgabe_beschreibung_bilder.add("Nummerierte Eckpunkte");
                    rueckgabe_beschreibung_bilder.add("Linien zwischen Eckpunkte");
                }else {
                    rueckgabe_zahlen.add(0);
                    rueckgabe_beschreibung_zahlen.add("Erreichte Punktzahl: ");
                    rueckgabe_zahlen.add(gemittelteSortierteEckpunkte.size());
                    rueckgabe_beschreibung_zahlen.add("Anzahl identifizierter Eckpunkte: ");
                    if (gemittelteSortierteEckpunkte.size() == 8){
                        int gezeichneteLinien = EckpunkteVerbunden(gemittelteSortierteEckpunkte, task2);
                        rueckgabe_zahlen.add(gezeichneteLinien);
                        rueckgabe_beschreibung_zahlen.add("Anzahl gezeichnete Linien: ");
                        zeichneArraylist(linie, bmp, "6 Linien zwischen Punkte", paint5, wertNormierung(2), false);
                        zeichneArraylist(gemittelteSortierteEckpunkte, bmp, "Nummerierte Eckpunkte", paint6, wertNormierung(2), true);
                        rueckgabe_beschreibung_bilder.add("Nummerierte Eckpunkte");
                        rueckgabe_beschreibung_bilder.add("Linien zwischen Eckpunkte");
                    }
                }
            }else {
                rueckgabe_zahlen.add(0);
                rueckgabe_beschreibung_zahlen.add("Erreichte Punktzahl: ");
                rueckgabe_zahlen.add(gemittelteSortierteEckpunkte.size());
                rueckgabe_beschreibung_zahlen.add("Anzahl identifizierter Eckpunkte: ");
            }
        }

        rueckgabe.add(rueckgabe_bilder);
        rueckgabe.add(rueckgabe_beschreibung_bilder);
        rueckgabe.add(rueckgabe_zahlen);
        rueckgabe.add(rueckgabe_beschreibung_zahlen);
        return rueckgabe;
    }

    //kompinatorik gibt alle 9 möglichen Kombinationen(9 mal je 8 Eckpunkte) der 9 übergebenen Eckpunkte zurück.
    private static ArrayList<SPenData> kompinatiorik(ArrayList<SPenData>gemittelteSortierteEckpunkte, ArrayList<SPenData> task2){

        int vorhanden;
        ArrayList<SPenData> auswahl;
        if (gemittelteSortierteEckpunkte.size() == 9){
            for (int i=0; i<gemittelteSortierteEckpunkte.size(); i++){
                auswahl = teilmenge(gemittelteSortierteEckpunkte, i);
                vorhanden = EckpunkteVerbunden(auswahl, task2);
                if (vorhanden == 12){
                    return gemittelteSortierteEckpunkte;
                }
            }
            return gemittelteSortierteEckpunkte;
        }
        return  null;

    }

    //teilmenge gibt die Menge der Elemente gemittelteSortierteEckpunkte ohne i zurück
    private static ArrayList<SPenData>teilmenge(ArrayList<SPenData> gemittelteSortierteEckpunkte, int i){
        ArrayList<SPenData> ergebniss = new ArrayList<>();
        for (int x = 0; x<gemittelteSortierteEckpunkte.size(); x++){
            if (x != i){ergebniss.add(gemittelteSortierteEckpunkte.get(x));}
        }
        return ergebniss;
    }

    //durchschnittlicheStrichlaenge berechnet die Durchschnittslänge aller erstellten strokes
    private static double durchschnittlicheStrichlaenge(ArrayList<SPenData> StartUndEndpunkte){
        SPenData anfangPunkt = StartUndEndpunkte.get(0);
        SPenData endPunkt = StartUndEndpunkte.get(0);
        double distanz;
        int counter = 0;
        double distanzSum = 0;
        for (int punkt = 0; punkt<StartUndEndpunkte.size(); punkt++){
                if (StartUndEndpunkte.get(punkt).getMotionEventType() == 0){
                    anfangPunkt = StartUndEndpunkte.get(punkt);
                }
                if (StartUndEndpunkte.get(punkt).getMotionEventType() == 1){
                    endPunkt = StartUndEndpunkte.get(punkt);
                    distanz = berechneDistanz(anfangPunkt, endPunkt);
                    distanzSum += distanz;
                    counter++;
                }
            }
        return distanzSum/counter;
    }

    //kurzeStricheFiltern löscht Start- und Endpunkte, wie kurze Striche und Punkte.
    private static ArrayList<SPenData> kurzeStricheFiltern(ArrayList<SPenData> StartUndEndpunkte){
        SPenData anfangPunkt = StartUndEndpunkte.get(0);
        SPenData endPunkt = StartUndEndpunkte.get(0);
        ArrayList<SPenData> ergebniss = new ArrayList<>();
        double distanz;
        for (int punkt = 0; punkt<StartUndEndpunkte.size(); punkt++){
            if (StartUndEndpunkte.get(punkt).getMotionEventType() == 0){
                anfangPunkt = StartUndEndpunkte.get(punkt);
            }
            if (StartUndEndpunkte.get(punkt).getMotionEventType() == 1){
                endPunkt = StartUndEndpunkte.get(punkt);
                distanz = berechneDistanz(anfangPunkt, endPunkt);
                if (distanz > durchschnittlicheStrichlaenge(StartUndEndpunkte)/2){
                        ergebniss.add(anfangPunkt);
                        ergebniss.add(endPunkt);
                }
            }
        }
        return ergebniss;
    }

    // berechneDistanz berechnet die Euklidische Distanz zwischen zwei Punkten
    private static double berechneDistanz(SPenData anfang, SPenData ende){
        float x1, y1, x2, y2, deltaX, deltaY;
            x1 = anfang.getxCoord();
            x2 = ende.getxCoord();
            y1 = anfang.getyCoord();
            y2 = ende.getyCoord();

            deltaX = Math.abs(x1 - x2);
            deltaY = Math.abs(y1 - y2);
            double s = Math.sqrt( deltaX*deltaX + deltaY*deltaY );
            return s;

    }

    //passt displayabhängige Werte an unterschiedliche Displayauflösungen an, indem diese entsprechend skaliert werden.
    private static int wertNormierung(int wert){
        double originalCanvasWidth = MemoryHelper.originalCanvasWidth;
        double standardCanvasWidth = 744;
        double factor = originalCanvasWidth/standardCanvasWidth;

        int ergebniss = (int) Math.round(wert*factor);
        return ergebniss;
    }

    //EckpunkteVerbunden überprüft, ob alle Eckpunkte durch eine Kante verbunden sind.
    // Gibt die Anzahl der korrekt gezeichneten Kanten zurück.
    private static Integer EckpunkteVerbunden(ArrayList<SPenData> eckpunkte, ArrayList<SPenData> task2){
        linie = new ArrayList<>();
        ArrayList<SPenData> actionDownPunkte = new ArrayList<>();
        for (int i = 0; i< task2.size(); i++){
            if (task2.get(i).getMotionEventType() == 2){
                actionDownPunkte.add(task2.get(i));
            }
        }

        int ergebniss = 0;
        //prüfe ob horizontal 4 linien existieren
        for (int ecke = 0; ecke < eckpunkte.size()-1; ecke += 2){
            if (punkteVerbundenHorizontal(eckpunkte.get(ecke), eckpunkte.get(ecke+1), actionDownPunkte)){
                ergebniss++;
            }
        }
        //prüfe ob vertical 4 linien existieren
        for (int ecke = 0; ecke < eckpunkte.size()/2; ecke++){
            if (punkteVerbundenVertical(eckpunkte.get(ecke), eckpunkte.get(ecke+4), actionDownPunkte)){
                ergebniss++;
            }
        }
        //prüfe ob diagonal 4 linien existieren
        for (int ecke = 0; ecke < eckpunkte.size()/4; ecke++){
            Log.d("Tobiii", "von ecke =======: "+ecke );
            if (punkteVerbundenHorizontal(eckpunkte.get(ecke), eckpunkte.get(ecke+2), actionDownPunkte)){
                ergebniss++;
            }
            if (punkteVerbundenHorizontal(eckpunkte.get(ecke+4), eckpunkte.get(ecke+6), actionDownPunkte)){
                ergebniss++;
            }
        }
        return ergebniss;
    }

    //punkteVerbundenVertical überprüft, ob alle vertikale Kanten vorhanden sind. Gibt die Anzahl der vertikalen Kanten zurück.
    private static boolean punkteVerbundenVertical(SPenData punkt1, SPenData punkt2, ArrayList<SPenData> task2){
        SPenData punktUnten, punktOben;
        if (punkt1.getyCoord() < punkt2.getyCoord()){
            punktUnten = punkt1;
            punktOben = punkt2;
        }else {
            punktUnten = punkt2;
            punktOben = punkt1;
        }
        ArrayList<Float> gerade;
        gerade = berechneGeradengleichung(punktUnten, punktOben);
        float m = gerade.get(0);
        float b = gerade.get(1);

        //berechne punkte entlang geradenGleichung
        float xCoordinate, xAbweichung, yAbweichung;
        float tolleranz = wertNormierung(30);
        int punktGefunden = 0;
        int punktNichtGefunden = 0;
        float spruenge = ( punktOben.getyCoord() - punktUnten.getyCoord() ) / 100;

        float yCoordinate = punktUnten.getyCoord();
        SPenData punktBerechnte;
        while (yCoordinate < punktOben.getyCoord()){
            //y = mx + b // x = (y-b)/m

            xCoordinate = (yCoordinate-b)/m;

            //speicherung der Punkte zur Presentation als Bild
            punktBerechnte = new SPenData(0,xCoordinate,yCoordinate,0,2,10);
            linie.add(punktBerechnte);

            int punkt = 0;
            boolean gefunden = false;
            while (punkt < task2.size()){
                xAbweichung = Math.abs(xCoordinate-task2.get(punkt).getxCoord());
                yAbweichung = Math.abs(yCoordinate-task2.get(punkt).getyCoord());
                if (xAbweichung < tolleranz && yAbweichung < tolleranz){
                    gefunden = true;
                    break;
                }
                punkt++;
            }
            if (gefunden){
                punktGefunden++;
            }else {
                punktNichtGefunden++;
            }
            yCoordinate = yCoordinate+spruenge;
        }
        boolean ergebniss = false;
        if (punktNichtGefunden*20 < punktGefunden){
            ergebniss = true;
        }

        return ergebniss;
    }

    //punkteVerbundenHorizontal überprüft, ob alle horizontalen Kanten vorhanden sind. Gibt die Anzahl der horizontalen Kanten zurück.
    private static boolean punkteVerbundenHorizontal(SPenData punkt1, SPenData punkt2, ArrayList<SPenData> task2){
        SPenData punktLinks, punktRechts;
        if (punkt1.getxCoord() < punkt2.getxCoord()){
            punktLinks = punkt1;
            punktRechts = punkt2;
        }else {
            punktLinks = punkt2;
            punktRechts = punkt1;
        }
        ArrayList<Float> gerade;
        gerade = berechneGeradengleichung(punktLinks, punktRechts);
        float m = gerade.get(0);
        float b = gerade.get(1);

        //berechne punkte entlang geradenGleichung
        float yCoordinate, xAbweichung, yAbweichung;
        float tolleranz = wertNormierung(30);
        int punktGefunden = 0;
        int punktNichtGefunden = 0;
        float spruenge = ( punktRechts.getxCoord() - punktLinks.getxCoord() ) / 100;

        float xCoordinate = punktLinks.getxCoord();
        SPenData punktGerade;
        while (xCoordinate < punktRechts.getxCoord()){
            //y = mx + b
            yCoordinate = m*xCoordinate+b;
            punktGerade = new SPenData(0,xCoordinate,yCoordinate,0,2,10);
            linie.add(punktGerade);
            int punkt = 0;
            boolean gefunden = false;
            while (punkt < task2.size()){
                xAbweichung = Math.abs(xCoordinate-task2.get(punkt).getxCoord());
                yAbweichung = Math.abs(yCoordinate-task2.get(punkt).getyCoord());
                if (xAbweichung < tolleranz && yAbweichung < tolleranz){
                    gefunden = true;
                    break;
                }
                punkt++;
            }
            if (gefunden){
                punktGefunden++;
            }else {
                punktNichtGefunden++;
            }
            xCoordinate = xCoordinate+spruenge;
        }
        Log.d("Tobiii", "punkteGefunden: "+ punktGefunden);
        Log.d("Tobiii", "punkteNichtGefunden: "+ punktNichtGefunden);

        boolean ergebniss = false;
        if (punktNichtGefunden*20 < punktGefunden){
            ergebniss = true;
        }

        return ergebniss;
    }

    //berechneGeradengleichung berechnet aus zwei Punkten die Geradengleichung y = mx+b. Gibt m und b zurück.
    private static ArrayList<Float> berechneGeradengleichung(SPenData punkt1, SPenData punkt2) {
        // Geradengleichung ermitteln: f(x) = mx + b

        float x1 = punkt1.getxCoord();
        float x2 = punkt2.getxCoord();
        float y1 = punkt1.getyCoord();
        float y2 = punkt2.getyCoord();

        float m,b;

        m = (y2 - y1) / (x2 - x1);
        b = y2 - (m*x2);
        ArrayList<Float> ergebniss = new ArrayList();
        ergebniss.add(m);
        ergebniss.add(b);
        return ergebniss;
    }

    //berechneGruppenTolleranz berechnet die Toleranz zur Gruppierung der Start- und Endpunke
    private static float berechneGruppenTolleranz(ArrayList<SPenData> startUndEndpunkte){
        ArrayList<Float> deltaX = new ArrayList<>();
        ArrayList<Float> deltaY = new ArrayList<>();
        for (int punkt = 0; punkt < startUndEndpunkte.size(); punkt++){
            SPenData ausgangsPunkt = startUndEndpunkte.get(punkt);
            float ausgangsPunktX = ausgangsPunkt.getxCoord();
            float ausgangsPunktY = ausgangsPunkt.getyCoord();

            for (int i = 0; i < startUndEndpunkte.size(); i++){
                deltaX.add(Math.abs(ausgangsPunktX - startUndEndpunkte.get(i).getxCoord()));
                deltaY.add(Math.abs(ausgangsPunktY - startUndEndpunkte.get(i).getyCoord()));
            }
        }

        Collections.sort(deltaX, new MyComparatorAufsteigend());
        Collections.sort(deltaY, new MyComparatorAufsteigend());

        float teiler = 3;
        if (deltaX.get(Math.round(deltaX.size()/teiler)) > deltaY.get(Math.round(deltaY.size()/teiler))){
            return deltaY.get(Math.round(deltaY.size()/teiler));
        }else{
            return deltaX.get(Math.round(deltaX.size()/teiler));
        }
    }

    //bewerteWuerfel berechnet aus der Längen- und Winkelabweichung sowie die Anzahl der gezeichneten Linien eine Bewertung zwischen 0 - 100
    private static double bewerteWuerfel(ArrayList<SPenData> sortierteEckpunkte, int gezeichneteLinien, boolean form){
        if (gezeichneteLinien != 12){
            return -1;
        }



        ArrayList<Double> hoeheAbweichung = berechneHoeheAbweichung(sortierteEckpunkte);
        ArrayList<Double> breiteAbweichung = berechneLaengeHorizontal(sortierteEckpunkte);
        double prozenthoehe = 0;
        double prozentbreite = 0;
        double diagonalenAbweichung = bereichneDiagonalenAbweichung(sortierteEckpunkte);
        double paralellHorizontal = berechneParalellHorizontal(sortierteEckpunkte);
        double paralellVertikal = berechneParalellVertikal(sortierteEckpunkte);
        double paralellDiagonal = berechneParalellDiagonal(sortierteEckpunkte);

        rueckgabe_zahlen.add(gezeichneteLinien);
        rueckgabe_beschreibung_zahlen.add("Anzahl gezeichnete Linien: ");
        if (!form){
            prozenthoehe = berechneProzentAbweichung(hoeheAbweichung);
            rueckgabe_zahlen.add(prozenthoehe);
            rueckgabe_beschreibung_zahlen.add("Rel. Längenabweichung Höhe in %: ");
            prozentbreite = berechneProzentAbweichung(breiteAbweichung);
            rueckgabe_zahlen.add(prozentbreite);
            rueckgabe_beschreibung_zahlen.add("Rel. Längenabweichung Breite in %: ");
        }else {
            for (int i=0; i<hoeheAbweichung.size(); i++){
                breiteAbweichung.add(hoeheAbweichung.get(i));
            }
            prozentbreite = berechneProzentAbweichung(breiteAbweichung);
            rueckgabe_zahlen.add(prozentbreite);
            rueckgabe_beschreibung_zahlen.add("Rel. Längenabweichung Höhe und Breite in %: ");
        }
        rueckgabe_zahlen.add(diagonalenAbweichung);
        rueckgabe_beschreibung_zahlen.add("Rel. Längenabweichung Diagonal in %: ");
        rueckgabe_zahlen.add(paralellHorizontal);
        rueckgabe_beschreibung_zahlen.add("Paralellität horizontal in Grad: ");
        rueckgabe_zahlen.add(paralellVertikal);
        rueckgabe_beschreibung_zahlen.add("Paralellität vertikal in Grad: ");
        rueckgabe_zahlen.add(paralellDiagonal);
        rueckgabe_beschreibung_zahlen.add("Paralellität diagonal in Grad: ");

        int gradAbweichungGesammt = (int)Math.round(paralellHorizontal+paralellDiagonal+paralellVertikal);
        int prozentAbweichungGesammt = (int)Math.round(prozenthoehe+prozentbreite+diagonalenAbweichung);
        int ergebniss = 100- gradAbweichungGesammt/10 - prozentAbweichungGesammt;
        rueckgabe_zahlen.add(ergebniss);
        rueckgabe_beschreibung_zahlen.add("Erreichte Punktzahl von 100 Punkten: ");

        return ergebniss;
    }

    //berechneParalellDiagonal berechnet die Abweichung jeder diagonalen Linie (in Grad) zum
    // durchschnittlichen Winkel der Diagonallinien
    private static double berechneParalellDiagonal(ArrayList<SPenData> sortierteEckpunkte){
        double winkel;
        ArrayList<Double> winkelAlle = new ArrayList<>();
        double winkelDurchschnitt = 0;
        double abweichung = 0;
        for (int Linie = 0; Linie < 2; Linie++){
            winkel = berechneWinkel(sortierteEckpunkte.get(Linie), sortierteEckpunkte.get(Linie+2));
            winkelDurchschnitt += winkel;
            winkelAlle.add(winkel);
        }

        for (int Linie = 4; Linie < 6; Linie++){
            winkel = berechneWinkel(sortierteEckpunkte.get(Linie), sortierteEckpunkte.get(Linie+2));
            winkelDurchschnitt += winkel;
            winkelAlle.add(winkel);
        }
        //winkelDruchschnitt ist die durchschnittliche Abweichung der diagonal Linien von der perfekten diagonalen.
        winkelDurchschnitt = winkelDurchschnitt/4;

        for (int Linie = 0; Linie < 4; Linie++){
            abweichung += Math.abs(winkelAlle.get(Linie)-winkelDurchschnitt);
        }
        //alle 4 diagonal linien weichen insgesammt um einen winkel von x vom durchschnittlich gezeichnetet winkel ab.
        return abweichung;
    }

    //berechneParalellVertikal berechnet die Abweichung jeder Vertikallinien Linie (in Grad) zum
    // durchschnittlichen Winkel der Vertikallinien
    private static double berechneParalellVertikal(ArrayList<SPenData> sortierteEckpunkte){
        double winkel;
        ArrayList<Double> winkelAlle = new ArrayList<>();
        double winkelDurchschnitt = 0;
        double abweichung = 0;
        for (int Linie = 0; Linie < 4; Linie++){
            winkel = berechneWinkel(sortierteEckpunkte.get(Linie), sortierteEckpunkte.get(Linie+4));
            winkelDurchschnitt += winkel;
            winkelAlle.add(winkel);
        }
        //winkelDruchschnitt ist die durchschnittliche Abweichung der vertikalen Linie von der perfekte vertikalen.
        winkelDurchschnitt = winkelDurchschnitt/4;

        for (int Linie = 0; Linie < 4; Linie++){
            abweichung += Math.abs(winkelAlle.get(Linie)-winkelDurchschnitt);
        }
        //alle 4 vertikalen linien weichen insgesammt um einen winkel von x vom durchschnittlich gezeichnetet winkel ab.
        return abweichung;
    }

    //berechneParalellHorizontal berechnet die Abweichung jeder horizontalen Linie (in Grad) zum
    // durchschnittlichen Winkel der Horizontallinien
    private static double berechneParalellHorizontal(ArrayList<SPenData> sortierteEckpunkte){
        double winkel;
        ArrayList<Double> winkelAlle = new ArrayList<>();
        double winkelDurchschnitt = 0;
        double abweichung = 0;
        for (int Linie = 0; Linie < 7; Linie+= 2){
            winkel = berechneWinkel(sortierteEckpunkte.get(Linie), sortierteEckpunkte.get(Linie+1));
            winkelDurchschnitt += winkel;
            winkelAlle.add(winkel);
            Log.d("Tobi2", "Horizontal: "+ winkel);

        }
        //winkelDruchschnitt ist die durchschnittliche Abweichung der horizontal Linie von der perfekten horizontalen.
        winkelDurchschnitt = winkelDurchschnitt/4;
        for (int Linie = 0; Linie < 4; Linie++){
            abweichung += Math.abs(winkelAlle.get(Linie)-winkelDurchschnitt);
        }

        //alle 4 horizontalen linien weichen insgesammt um einen winkel von x vom durchschnittlich gezeichnetet winkel ab.
        return abweichung;
    }

    //berechneWinkel berechnet aus zwei gegebenen Punkten den Winkel dieser Linie zur x-Achse
    private static double berechneWinkel(SPenData punkt1, SPenData punkt2){
        float xLinks, xRechts, yLinks, yRechts, xDiff, yDiff;
        double winkel;

        xLinks = punkt1.getxCoord();
        xRechts = punkt2.getxCoord();
        yLinks = punkt1.getyCoord();
        yRechts = punkt2.getyCoord();

        xDiff = Math.abs(xLinks-xRechts);
        yDiff = Math.abs(yLinks-yRechts);

        double hypotenuse = Math.sqrt(((xDiff*xDiff) + (yDiff*yDiff)));
        winkel = Math.toDegrees(Math.asin(yDiff /hypotenuse));
        return winkel;
    }

    //bereichneDiagonalenAbweichung berechnet die Länge jeder diagonalen Linie, daraus die
    // Durchschnittslänge und davon die prozentuale Abweichung jeder Diagonalen zu dieser druchschnittlichen Länge
    private static double bereichneDiagonalenAbweichung(ArrayList<SPenData> sortierteEckpunkte){
        ArrayList<Double> distanzen = new ArrayList<>();
        float sumDistanz = 0;
        float xLinks, xRechts, yLinks, yRechts, deltaX, deltaY;

        //Kanten von Punkt 0-2, 1-3, 4-6, 5-7
        for (int ecke=0; ecke< 2; ecke++){
            xLinks = sortierteEckpunkte.get(ecke).getxCoord();
            xRechts = sortierteEckpunkte.get(ecke+2).getxCoord();
            yLinks = sortierteEckpunkte.get(ecke).getyCoord();
            yRechts = sortierteEckpunkte.get(ecke+2).getyCoord();
            deltaX = Math.abs(xRechts - xLinks);
            deltaY = Math.abs(yRechts - yLinks);
            double s = Math.sqrt( deltaX*deltaX + deltaY*deltaY );
            distanzen.add(s);
            sumDistanz +=  s;
        }

        for (int ecke=4; ecke< 6; ecke++){
            xLinks = sortierteEckpunkte.get(ecke).getxCoord();
            xRechts = sortierteEckpunkte.get(ecke+2).getxCoord();
            yLinks = sortierteEckpunkte.get(ecke).getyCoord();
            yRechts = sortierteEckpunkte.get(ecke+2).getyCoord();
            deltaX = Math.abs(xRechts - xLinks);
            deltaY = Math.abs(yRechts - yLinks);
            double s = Math.sqrt( deltaX*deltaX + deltaY*deltaY );
            distanzen.add(s);
            sumDistanz +=  s;
        }

        float durchschnittslaenge = sumDistanz/distanzen.size();
        double prozentAbweichung;
        double sumProzentAbweichung = 0;
        double durchschAbweichung = 0;
        ArrayList<Double> prozentAbweichungen = new ArrayList<>();
        for (int kante=0; kante< distanzen.size(); kante++){
            prozentAbweichung = Math.abs(1 - Math.abs(distanzen.get(kante) / durchschnittslaenge));
            sumProzentAbweichung += prozentAbweichung;
            prozentAbweichungen.add(prozentAbweichung);
        }

        Log.d("Tobiii", "prozent Abweichung summe Diagonale länge: "+ sumProzentAbweichung);
        durchschAbweichung = sumProzentAbweichung / prozentAbweichungen.size();
        double durchschAbweichungGerundet = Math.round(durchschAbweichung * 100);
        Log.d("Tobiii", "prozent Abweichung durchschnitt Diagonale länge: "+ durchschAbweichungGerundet + "%");
        return durchschAbweichungGerundet;
    }

    //berechneProzentAbweichung berechnet aus einer gegebenen Menge (ArrayList<Double> distanzen)
    // die prozentuale Abweichung jeder Linie zur Durchscnittslänge der Gruppe
    private static double berechneProzentAbweichung(ArrayList<Double> distanzen){
        double sumDistanz = 0;
        for (int i=0; i<distanzen.size(); i++){
            sumDistanz += distanzen.get(i);
        }

        double durchschnittslaenge = sumDistanz/distanzen.size();
        double prozentAbweichung;
        double sumProzentAbweichung = 0;
        ArrayList<Double> prozentAbweichungen = new ArrayList<>();
        for (int kante=0; kante< distanzen.size(); kante++){
            prozentAbweichung = Math.abs(1 - Math.abs(distanzen.get(kante) / durchschnittslaenge));
            sumProzentAbweichung += prozentAbweichung;
            prozentAbweichungen.add(prozentAbweichung);
        }

        double durchschAbweichung = sumProzentAbweichung / prozentAbweichungen.size();
        double durchschAbweichungGerundet = Math.round(durchschAbweichung * 100);
        return durchschAbweichungGerundet;
    }

    // berechneHoeheAbweichung berechnet die Länge aller vertikalen Linien aus den sortierten Eckpunkten des Würfels/Quaders
    private static ArrayList<Double> berechneHoeheAbweichung(ArrayList<SPenData> sortierteEckpunkte){
        ArrayList<Double> distanzen = new ArrayList<>();
        float xLinks, xRechts, yLinks, yRechts, deltaX, deltaY;
        //Kanten von Punkt 0-4, 1-5, 2-3, 3-7
        for (int ecke=0; ecke< 4; ecke++){
            xLinks = sortierteEckpunkte.get(ecke).getxCoord();
            xRechts = sortierteEckpunkte.get(ecke+4).getxCoord();
            yLinks = sortierteEckpunkte.get(ecke).getyCoord();
            yRechts = sortierteEckpunkte.get(ecke+4).getyCoord();
            deltaX = Math.abs(xRechts - xLinks);
            deltaY = Math.abs(yRechts - yLinks);
            double s = Math.sqrt( deltaX*deltaX + deltaY*deltaY );
            distanzen.add(s);
        }
        return distanzen;
    }

    // berechneLaengeHorizontal berechnet die Länge aller horizontalen linien aus den sortierten Eckpunkten des Würfels/Quaders
    private static ArrayList<Double> berechneLaengeHorizontal(ArrayList<SPenData> sortierteEckpunkte){
        ArrayList<Double> distanzen = new ArrayList<>();
        float xLinks, xRechts, yLinks, yRechts, deltaX, deltaY;
        //Kanten von Punkt 0-1, 2-3, 4-5, 6-7
        for (int ecke=0; ecke< sortierteEckpunkte.size()-1; ecke = ecke+2){
            xLinks = sortierteEckpunkte.get(ecke).getxCoord();
            xRechts = sortierteEckpunkte.get(ecke+1).getxCoord();
            yLinks = sortierteEckpunkte.get(ecke).getyCoord();
            yRechts = sortierteEckpunkte.get(ecke+1).getyCoord();
            deltaX = Math.abs(xRechts - xLinks);
            deltaY = Math.abs(yRechts - yLinks);
            double s = Math.sqrt( deltaX*deltaX + deltaY*deltaY );
            distanzen.add(s);
        }

        return distanzen;
    }

    // EbenenOrdnen ordnet die gegebenen acht Eckpunkte von links oben 0 nach rechts unten 7
    private static ArrayList<SPenData> EbenenOrdnen(ArrayList<SPenData> sortierteEckpunkte){
        // aufruf immer mit 8 gemitteltenEckpunkten. Gegebenenfalls kominationen durchgehen. (n über k)
        Collections.sort(sortierteEckpunkte, new MyComparatorY());
        ArrayList<SPenData> punkteY = new ArrayList<>();
        punkteY.addAll(sortierteEckpunkte);

        Collections.sort(sortierteEckpunkte, new MyComparatorX());
        ArrayList<SPenData> punkteX = new ArrayList<>();
        Log.d("Tobiii", "anzahl sortierteEckpunkte: "+ sortierteEckpunkte.size());

        punkteX.addAll(sortierteEckpunkte);

        Log.d("Tobiii", "anzahl punkteX: "+ punkteX.size());

        ArrayList<SPenData> ergebniss = new ArrayList<>();

        ergebniss.add(gleich(punkteX.get(2), punkteX.get(3), punkteY.get(0), punkteY.get(1)));
        ergebniss.add(gleich(punkteX.get(6), punkteX.get(7), punkteY.get(0), punkteY.get(1)));

        ergebniss.add(gleich(punkteX.get(0), punkteX.get(1), punkteY.get(2), punkteY.get(3)));
        ergebniss.add(gleich(punkteX.get(4), punkteX.get(5), punkteY.get(2), punkteY.get(3)));

        ergebniss.add(gleich(punkteX.get(2), punkteX.get(3), punkteY.get(4), punkteY.get(5)));
        ergebniss.add(gleich(punkteX.get(6), punkteX.get(7), punkteY.get(4), punkteY.get(5)));

        ergebniss.add(gleich(punkteX.get(0), punkteX.get(1), punkteY.get(6), punkteY.get(7)));
        ergebniss.add(gleich(punkteX.get(4), punkteX.get(5), punkteY.get(6), punkteY.get(7)));

        boolean linkerWuerfel = false;
        for (int i = 0; i<ergebniss.size(); i++){
            if (ergebniss.get(i) == null){
                linkerWuerfel = true;
            }
        }

        if (linkerWuerfel){
            ergebniss = new ArrayList<>();

            ergebniss.add(gleich(punkteX.get(0), punkteX.get(1), punkteY.get(0), punkteY.get(1)));
            ergebniss.add(gleich(punkteX.get(4), punkteX.get(5), punkteY.get(0), punkteY.get(1)));

            ergebniss.add(gleich(punkteX.get(2), punkteX.get(3), punkteY.get(2), punkteY.get(3)));
            ergebniss.add(gleich(punkteX.get(6), punkteX.get(7), punkteY.get(2), punkteY.get(3)));

            ergebniss.add(gleich(punkteX.get(0), punkteX.get(1), punkteY.get(4), punkteY.get(5)));
            ergebniss.add(gleich(punkteX.get(4), punkteX.get(5), punkteY.get(4), punkteY.get(5)));

            ergebniss.add(gleich(punkteX.get(2), punkteX.get(3), punkteY.get(6), punkteY.get(7)));
            ergebniss.add(gleich(punkteX.get(6), punkteX.get(7), punkteY.get(6), punkteY.get(7)));
        }

        for (int x = 0; x<ergebniss.size(); x++){
            if (ergebniss.get(x) == null){
                //falls die erkennung mit den ebenen nicht funktioneiert werd erinfach nach x sortiert zurückgegeben.
                for (int i = 0; i < ergebniss.size(); i++){
                    for (int j = i+1; j < ergebniss.size(); j++){
                        if (ergebniss.get(i) == ergebniss.get(j)){
                            return sortierteEckpunkte;
                        }
                    }
                }
            }
        }

        return ergebniss;
    }

    // gibt aus zwei zweielementigen Mengen menge1= a,b menge2= x,y die Schmittmenge zurück
    private static SPenData gleich(SPenData a, SPenData b, SPenData x, SPenData y){
        if (a == x){
            return a;
        }
        if (a == y){
            return a;
        }
        if (b == x){
            return b;
        }
        if (b == y){
            return b;
        }
        return null;
    }

    // sortiereEckpunkteY teilt den Würfel in vertikale Ebenen. Sofern mehr als 2 Punkte auf einer Ebene sind, werden die beiden Punkte mit y min und max behalten.
    private static ArrayList<SPenData> sortiereEckpunkteY(ArrayList<SPenData> gemittelteEckpunkte){
        //y Wert aufsteigend sortiert
        Collections.sort(gemittelteEckpunkte, new MyComparatorY());

        //in Ebenen aufteilen
        ArrayList<ArrayList<SPenData>> unterschiedlicheEbenen = new ArrayList<>();
        ArrayList<SPenData> eineEbene = new ArrayList<>();

        float vorgaengerY = gemittelteEckpunkte.get(0).getyCoord();
        eineEbene.add(gemittelteEckpunkte.get(0));
        for (int ecke = 1; ecke < gemittelteEckpunkte.size(); ecke++){
            if (Math.abs(gemittelteEckpunkte.get(ecke).getyCoord() - vorgaengerY) < ebenenTolleranz){
                eineEbene.add(gemittelteEckpunkte.get(ecke));
            }else {
                vorgaengerY = gemittelteEckpunkte.get(ecke).getyCoord();
                unterschiedlicheEbenen.add(eineEbene);
                eineEbene = new ArrayList<>();
                eineEbene.add(gemittelteEckpunkte.get(ecke));
            }
        }
        unterschiedlicheEbenen.add(eineEbene);

        // einzelne Ebenen nun nach x ordnen
        for (int ebene = 0; ebene < unterschiedlicheEbenen.size(); ebene++){
            Collections.sort(unterschiedlicheEbenen.get(ebene), new MyComparatorX());
        }

        //Filtere alle Ebenen mit mehr als 2 Punkte und überführe in eine eindimensionale Arraylist.
        int gesammtGroesse = 0;
        for (int ebene = 0; ebene< unterschiedlicheEbenen.size(); ebene++){
            gesammtGroesse += unterschiedlicheEbenen.get(ebene).size();
        }
        ArrayList<SPenData> ergebniss = new ArrayList<>();
        for (int i = 0; i< unterschiedlicheEbenen.size(); i++){
            if (gesammtGroesse > 8){
                if (unterschiedlicheEbenen.get(i).size() >= 3 && gesammtGroesse-(unterschiedlicheEbenen.get(i).size()-2) >= 8){
                    ergebniss.add(unterschiedlicheEbenen.get(i).get(0)); //linkeste Element
                    ergebniss.add(unterschiedlicheEbenen.get(i).get(unterschiedlicheEbenen.get(i).size()-1)); //rechteste Element der ebene
                    gesammtGroesse = gesammtGroesse - (unterschiedlicheEbenen.get(i).size()-2);
                }else {
                    if (unterschiedlicheEbenen.get(i).size() == 2){
                        ergebniss.add(unterschiedlicheEbenen.get(i).get(0));
                        ergebniss.add(unterschiedlicheEbenen.get(i).get(1));
                    }else {
                        ergebniss.add(unterschiedlicheEbenen.get(i).get(0));
                    }

                }
            }else {
                ergebniss.addAll(unterschiedlicheEbenen.get(i));
            }

        }

        //sofern immer noch zu viele ecken vorhanden lösche punkte die alleine auf einer ebene
        // zb auf diagonale zwischen zwei punkten.
       if (ergebniss.size() > 8){

           //Filtere alle Ebenen mit mehr als 2 Punkte und überführe in eine eindimensionale Arraylist.
           for (int ebene = 0; ebene< unterschiedlicheEbenen.size(); ebene++){
               gesammtGroesse += unterschiedlicheEbenen.get(ebene).size();
           }
           ergebniss = new ArrayList<>();
           for (int i = 0; i< unterschiedlicheEbenen.size(); i++){
               if (gesammtGroesse > 8){
                   if (unterschiedlicheEbenen.get(i).size() >= 3 && gesammtGroesse-(unterschiedlicheEbenen.get(i).size()-2) >= 8){
                       ergebniss.add(unterschiedlicheEbenen.get(i).get(0)); //linkeste Element
                       ergebniss.add(unterschiedlicheEbenen.get(i).get(unterschiedlicheEbenen.get(i).size()-1)); //rechteste Element der ebene
                       gesammtGroesse = gesammtGroesse - (unterschiedlicheEbenen.get(i).size()-2);
                   }else {
                       if (unterschiedlicheEbenen.get(i).size() == 2){
                           ergebniss.add(unterschiedlicheEbenen.get(i).get(0));
                           ergebniss.add(unterschiedlicheEbenen.get(i).get(1));
                       }
                       //else {
                         //  ergebniss.add(unterschiedlicheEbenen.get(i).get(0));
                       //}

                   }
               }else {
                   ergebniss.addAll(unterschiedlicheEbenen.get(i));
               }

           }

       }
        return ergebniss;
    }

    // sortiereEckpunkteX teilt den Würfel in horizontale Ebenen. Sofern mehr als 2 Punkte auf einer Ebene sind, werden die beiden Punkte mit x min und max behalten.
    private static ArrayList<SPenData> sortiereEckpunkteX(ArrayList<SPenData> gemittelteEckpunkte){
        //x Wert aufsteigend sortiert
        Collections.sort(gemittelteEckpunkte, new MyComparatorX());

        //in Ebenen aufteilen
        ArrayList<ArrayList<SPenData>> unterschiedlicheEbenen = new ArrayList<>();
        ArrayList<SPenData> eineEbene = new ArrayList<>();

        float vorgaengerX = gemittelteEckpunkte.get(0).getxCoord();
        eineEbene.add(gemittelteEckpunkte.get(0));
        for (int ecke = 1; ecke < gemittelteEckpunkte.size(); ecke++){
            if (Math.abs(gemittelteEckpunkte.get(ecke).getxCoord() - vorgaengerX) < ebenenTolleranz){
                eineEbene.add(gemittelteEckpunkte.get(ecke));
            }else {
                vorgaengerX = gemittelteEckpunkte.get(ecke).getxCoord();
                unterschiedlicheEbenen.add(eineEbene);
                eineEbene = new ArrayList<>();
                eineEbene.add(gemittelteEckpunkte.get(ecke));
            }
        }
        unterschiedlicheEbenen.add(eineEbene);

        // einzelne Ebenen nun nach y ordnen
        for (int ebene = 0; ebene < unterschiedlicheEbenen.size(); ebene++){
            Collections.sort(unterschiedlicheEbenen.get(ebene), new MyComparatorY());
        }

        //Filtere alle Ebenen mit mehr als 2 Punkte und überführe in eine eindimensionale Arraylist.
        int gesammtGroesse = 0;
        for (int ebene = 0; ebene< unterschiedlicheEbenen.size(); ebene++){
            gesammtGroesse += unterschiedlicheEbenen.get(ebene).size();
        }
        ArrayList<SPenData> ergebniss = new ArrayList<>();
        for (int i = 0; i< unterschiedlicheEbenen.size(); i++){
            if (gesammtGroesse > 8){
                if (unterschiedlicheEbenen.get(i).size() >= 3 && gesammtGroesse-(unterschiedlicheEbenen.get(i).size()-2) >= 8){
                    ergebniss.add(unterschiedlicheEbenen.get(i).get(0)); //linkeste Element
                    ergebniss.add(unterschiedlicheEbenen.get(i).get(unterschiedlicheEbenen.get(i).size()-1)); //rechteste Element der ebene
                    gesammtGroesse = gesammtGroesse - (unterschiedlicheEbenen.get(i).size()-2);
                }else {
                    if (unterschiedlicheEbenen.get(i).size() == 2){
                        ergebniss.add(unterschiedlicheEbenen.get(i).get(0));
                        ergebniss.add(unterschiedlicheEbenen.get(i).get(1));
                    }else {
                        ergebniss.add(unterschiedlicheEbenen.get(i).get(0));
                    }

                }
            }else {
                ergebniss.addAll(unterschiedlicheEbenen.get(i));
            }

        }
        return sortiereEckpunkteY(ergebniss);
    }

    //zeichneArraylist zeichnet in das gegebene Bitmap bmp die in ArrayList<SPenData> Daten genannten Punkte in der Größe und Farbe von Paint paint und int radius
    private static Bitmap zeichneArraylist(ArrayList<SPenData> daten, Bitmap bmp, String speicherName, Paint paint, int radius, boolean nummerieren){
        Bitmap bmOverlay = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp, new Matrix(), null);
        for (int i=0; i<daten.size(); i++){
            canvas.drawCircle(daten.get(i).getxCoord(), daten.get(i).getyCoord(), radius, paint);
            if (nummerieren){
                canvas.drawText(String.valueOf(i), daten.get(i).getxCoord(), daten.get(i).getyCoord(), paint);
            }
        }
        saveImage(bmOverlay, speicherName + ".png");
        return  bmOverlay;
    }

    //Eckpunkte berechnet aus allen übergebenen Gruppen ArrayList<ArrayList<SPenData>> den Mittelpunkt und gibt diese als ArrayList zurück
    private static ArrayList Eckpunkte(ArrayList<ArrayList<SPenData>> GruppiertePunkte){
        ArrayList<SPenData> ergibniss = new ArrayList<>();

        for (int gruppe=0; gruppe<GruppiertePunkte.size(); gruppe++){
            SPenData mittelpunkt = berechneMittelpunkt(GruppiertePunkte.get(gruppe));
            ergibniss.add(mittelpunkt);
        }
        return ergibniss;
    }

    // berechneMittelpunkt berechnet den Mittelpunkt einer ArrayList<SPenData> und gibt diesen als SPenData Objekt zurück
    private static SPenData berechneMittelpunkt(ArrayList<SPenData> gruppe){
        float xSum = 0;
        float ySum = 0;
        for (int gruppenelement=0; gruppenelement<gruppe.size(); gruppenelement++){
            xSum += gruppe.get(gruppenelement).getxCoord();
            ySum += gruppe.get(gruppenelement).getyCoord();
        }
        SPenData mittelpunkt = new SPenData(gruppe.get(0).getTimestamp(), xSum/gruppe.size(), ySum/gruppe.size(), 1, 1, 0);
        return mittelpunkt;
    }

    //GruppiertePunkte ordnet die "Start- und Endpunkte" sowie die "VelocitiCero" Punkte in Gruppen anhand der Gruppentoleranz. Die Gruppen werden als ArrayList<ArrayList<SPenData>> zurückgegeben.
    private static ArrayList GruppiertePunkte(ArrayList<SPenData> StartundEndPunkte, ArrayList<SPenData> VelocitiCero){
        SPenData ersterPunkt;
        ArrayList<ArrayList<SPenData>> ergebniss = new ArrayList<>();
        float xAusgang, yAusgang;
        Log.d("Tobiiii", "Größe vor: " + VelocitiCero.size());
        while (!StartundEndPunkte.isEmpty()){
            ArrayList<SPenData>gruppe = new ArrayList<>();
            ersterPunkt = StartundEndPunkte.get(StartundEndPunkte.size()-1);
            StartundEndPunkte.remove(StartundEndPunkte.size()-1);
            gruppe.add(ersterPunkt);
            xAusgang = ersterPunkt.getxCoord();
            yAusgang = ersterPunkt.getyCoord();

            //StartundEndpunkte durchsuchen
            float x, y, diffx, diffy;
            for (int rest=StartundEndPunkte.size()-1; rest>=0;){
                x = StartundEndPunkte.get(rest).getxCoord();
                y = StartundEndPunkte.get(rest).getyCoord();
                diffx = Math.abs(x-xAusgang);
                diffy = Math.abs(y-yAusgang);
                    if (diffx < gruppenTolleranz && diffy < gruppenTolleranz){
                        gruppe.add(StartundEndPunkte.get(rest));
                        StartundEndPunkte.remove(rest);
                    }
                    rest--;
            }

            //Velocitiy Punkte durchsuchen
            float xVelo, yVelo, diffxVelo, diffyVelo;
            for (int rest=VelocitiCero.size()-1; rest>=0;){
                xVelo = VelocitiCero.get(rest).getxCoord();
                yVelo = VelocitiCero.get(rest).getyCoord();
                diffxVelo = Math.abs(xVelo-xAusgang);
                diffyVelo = Math.abs(yVelo-yAusgang);
                if (diffxVelo < gruppenTolleranz && diffyVelo < gruppenTolleranz){
                    gruppe.add(VelocitiCero.get(rest));
                    VelocitiCero.remove(rest);
                }
                rest--;
            }
            ergebniss.add(gruppe);
        }

        // sofern zuwenig Eckpunkte erkannt werden, wird versucht dies mit den restlichen velocity punkten zu finden
        while (ergebniss.size() < 8 && !VelocitiCero.isEmpty()){
            ArrayList<SPenData>gruppe = new ArrayList<>();
            ersterPunkt = VelocitiCero.get(VelocitiCero.size()-1);
            VelocitiCero.remove(VelocitiCero.size()-1);
            gruppe.add(ersterPunkt);
            xAusgang = ersterPunkt.getxCoord();
            yAusgang = ersterPunkt.getyCoord();

            //Übrige Velocitiy Punkte durchsuchen
            float xVelo, yVelo, diffxVelo, diffyVelo;
            for (int rest=VelocitiCero.size()-1; rest>=0;){
                xVelo = VelocitiCero.get(rest).getxCoord();
                yVelo = VelocitiCero.get(rest).getyCoord();
                diffxVelo = Math.abs(xVelo-xAusgang);
                diffyVelo = Math.abs(yVelo-yAusgang);
                if (diffxVelo < gruppenTolleranz && diffyVelo < gruppenTolleranz){
                    gruppe.add(VelocitiCero.get(rest));
                    //fehler löscht iwie die daten aus VelocitiCero die ich oben behalten möchte
                    VelocitiCero.remove(rest);
                }
                rest--;
            }
            ergebniss.add(gruppe);
        }
        Log.d("Tobiiii", "Größe nach: " + VelocitiCero.size());
        return ergebniss;
    }

    //VelocityCero filtert aus der übergebenen ArrayList<SPenData> die 200 langsamsten SPenData- Einträge
    private static ArrayList VelocityCero(ArrayList<SPenData> task2){
        ArrayList<SPenData> gefiltert = new ArrayList<>();
        //filtern
        for (int punkt = 0; punkt< task2.size(); punkt++){
            if (task2.get(punkt).getMotionEventType() == 2 && task2.get(punkt).getVelocitiy() > 0){
                gefiltert.add(task2.get(punkt));
            }
        }
        //sortieren
        Collections.sort(gefiltert, new MyComparatorVelocitiy());

        //die kleinsten "anzahlPunkte" als ecken annemhmen
        int anzahlPunkte = 200;
        ArrayList<SPenData> ergebniss = new ArrayList<>();
        if (gefiltert.size() > anzahlPunkte){
            for (int punkt = 0; punkt<anzahlPunkte; punkt++){
                ergebniss.add(gefiltert.get(punkt));
            }
        }else {
            return gefiltert;
        }
        return ergebniss;
    }

    //StartUndEndpunkte filtert aus der übergebenen ArrayList<SPenData> alle SPenData- Einträge mit
    //MotionEventType = 1 oder 2 und gibt diese als ArrayList zurück.
    private static ArrayList StartUndEndpunkte(ArrayList<SPenData> task2){
        ArrayList<SPenData> ergebniss = new ArrayList<>();
        int motionTyp;
        for (int DatenPunkt=0; DatenPunkt<task2.size(); DatenPunkt++){
            motionTyp = task2.get(DatenPunkt).getMotionEventType();
            if (motionTyp == 1 | motionTyp == 0){
                ergebniss.add(task2.get(DatenPunkt));
            }
        }
        return ergebniss;
    }

    //Die Methode speichert die übergebene Bitmap bitmap unter dem String name in den Ordner Documents MOCA_CubeRecognition
    private static void saveImage(Bitmap bitmap, String name) {
        File ordner = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        String path = ordner.getPath();
        File myDir = new File(path + "/MOCA_CubeRecognition");
        myDir.mkdirs();

        File file = new File(myDir, name);
        rueckgabe_bilder.add(file);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //    static ArrayList VelocityCero(ArrayList<SPenData> task2){
//        ArrayList<SPenData> ergebniss = new ArrayList<>();
//        int motionTyp;
//        float velocitiy;
//        for (int DatenPunkt=0; DatenPunkt<task2.size(); DatenPunkt++){
//            motionTyp = task2.get(DatenPunkt).getMotionEventType();
//            velocitiy = task2.get(DatenPunkt).getVelocitiy();
//            if (motionTyp == 2 && velocitiy < 0.1 && velocitiy > 0){
//                ergebniss.add(task2.get(DatenPunkt));
//            }
//        }
//        return ergebniss;
//    }

    //    static float berechneTollerant(ArrayList<SPenData> task2){
//        Collections.sort(task2, new MyComparatorY());
//        int i = 0;
//        while (task2.get(i).getMotionEventType() != 2){
//            i++;
//        }
//        float yMin = task2.get(i).getyCoord();
//        i = task2.size()-1;
//        while (task2.get(i).getMotionEventType() != 2){
//            i--;
//        }
//        float yMax = task2.get(i).getyCoord();
//        float diff = yMax-yMin;
//
//        int anzahlEbenen = 4;
//
//        float tolleranz = diff/(anzahlEbenen*2+1);
//        return tolleranz;
//    }

    //    private static ArrayList<SPenData> sortiereEckpunkte(ArrayList<SPenData> gemittelteEckpunkte){
//        //y Wert aufsteigend sortiert
//        Collections.sort(gemittelteEckpunkte, new MyComparatorY());
//        ArrayList<SPenData> eineEbene = new ArrayList<>();
//        ArrayList<SPenData> sortierteEbenen = new ArrayList<>();
//
//        for (int i=0; i<gemittelteEckpunkte.size()-1; i+=2){
//            SPenData current = gemittelteEckpunkte.get(i);
//            SPenData nachfolger = gemittelteEckpunkte.get(i+1);
//
//            if (current.getxCoord() > nachfolger.getxCoord()){
//                gemittelteEckpunkte.set(i, nachfolger);
//                gemittelteEckpunkte.set(i+1, current);
//            }
//        }
//        return gemittelteEckpunkte;
//    }

    //zeichneErgebniss erstellt eine Bitmap des Würfels mit nummerierten Eckpunkten.
//    private static Bitmap zeichneErgebniss(ArrayList<SPenData> Eckpunkte, Bitmap bmp){
//        Bitmap bmOverlay = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());
//        Canvas canvas = new Canvas(bmOverlay);
//        canvas.drawBitmap(bmp, new Matrix(), null);
//
//        Paint paint = new Paint();
//        paint.setColor(Color.RED);
//        paint.setTextSize(wertNormierung(30));
//        paint.setStyle(Paint.Style.FILL_AND_STROKE);
//
//        for (int i=0; i<Eckpunkte.size(); i++){
//            canvas.drawCircle(Eckpunkte.get(i).getxCoord(), Eckpunkte.get(i).getyCoord(), wertNormierung(2), paint);
//            canvas.drawText(String.valueOf(i), Eckpunkte.get(i).getxCoord(), Eckpunkte.get(i).getyCoord(), paint);
//        }
//        return bmOverlay;
//    }

}


