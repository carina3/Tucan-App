package moca.clockdraw;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class CubeRecognitionResult extends AppCompatActivity {

    File bildFile;

    int counter = 0;
    int anzahlBilder;
    ArrayList<File> bilder = new ArrayList<>();
    ArrayList<String> bilder_beschreibung = new ArrayList<>();
    ArrayList<Double> zahlen = new ArrayList<>();
    ArrayList<String> zahlen_beschreibung = new ArrayList<>();
    ArrayList<ArrayList> gezeichnet = new ArrayList<>();
    ProgressDialog spinner;
    Bitmap anzeigebild;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cube_recogniton_result);

        spinner = new ProgressDialog(this);
        spinner.setIndeterminate(true);
        spinner.setCancelable(true);
        spinner.setTitle("Würfelerkennung läuft");
        spinner.setMessage("Bitte warten...");
        spinner.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        final TextView tv_bild, tv_1, tv_2, tv_3, tv_4, tv_5, tv_6, tv_7, tv_8;
        final ImageView iv_bild;

        ArrayList data = MemoryHelper.task2data;
        File imgFile_task2 = (File) getIntent().getExtras().get("file");
        boolean form = (boolean) getIntent().getExtras().get("form");
        Bitmap myBitmap = BitmapFactory.decodeFile(imgFile_task2.getAbsolutePath());

        gezeichnet = Analyse.ki(myBitmap, data, form);

        bilder = gezeichnet.get(0);
        bilder_beschreibung = gezeichnet.get(1);
        zahlen = gezeichnet.get(2);
        zahlen_beschreibung = gezeichnet.get(3);

        tv_1 = (TextView) findViewById(R.id.eins_tv);
        tv_2 = (TextView) findViewById(R.id.zwei_tv);
        tv_3 = (TextView) findViewById(R.id.drei_tv);
        tv_4 = (TextView) findViewById(R.id.vier_tv);
        tv_5 = (TextView) findViewById(R.id.fuenf_tv);
        tv_6 = (TextView) findViewById(R.id.sechs_tv);
        tv_7 = (TextView) findViewById(R.id.sieben_tv);
        tv_8 = (TextView) findViewById(R.id.acht_tv);
        ArrayList<TextView> textfelder = new ArrayList<>();
        textfelder.add(tv_1);
        textfelder.add(tv_2);
        textfelder.add(tv_3);
        textfelder.add(tv_4);
        textfelder.add(tv_5);
        textfelder.add(tv_6);
        textfelder.add(tv_7);
        textfelder.add(tv_8);

        for (int i = 0; i<zahlen.size(); i++){
            textfelder.get(i).setText(zahlen_beschreibung.get(i) + String.valueOf(zahlen.get(i)));
        }

        anzahlBilder = bilder.size();
        iv_bild = (ImageView) findViewById(R.id.bild_iv);
        bildFile = bilder.get(counter);

        if (bildFile.exists()) {
            anzeigebild = BitmapFactory.decodeFile(bildFile.getAbsolutePath());
            iv_bild.setImageBitmap(anzeigebild);
        }
        tv_bild = (TextView) findViewById(R.id.bild_tv);
        tv_bild.setText(bilder_beschreibung.get(counter));

        Button links, rechts;
        links = (Button) findViewById(R.id.links_b);
        links.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter++;
                if (counter > anzahlBilder-1){
                    counter = 0;
                }

                bildFile = bilder.get(counter);
                if (bildFile.exists()) {
                    anzeigebild = BitmapFactory.decodeFile(bildFile.getAbsolutePath());
                    iv_bild.setImageBitmap(anzeigebild);
                }
                tv_bild.setText(bilder_beschreibung.get(counter));
            }
        });

        rechts= (Button) findViewById(R.id.rechts_b);
        rechts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter--;
                if (counter == -1){
                    counter = anzahlBilder-1;
                }

                bildFile = bilder.get(counter);
                if (bildFile.exists()) {
                    anzeigebild = BitmapFactory.decodeFile(bildFile.getAbsolutePath());
                    iv_bild.setImageBitmap(anzeigebild);
                }
                tv_bild.setText(bilder_beschreibung.get(counter));
            }
        });
    }

    @Override
    public void onBackPressed() {
        bilder.clear();
        bilder = new ArrayList<>();

        bilder_beschreibung.clear();
        bilder_beschreibung= new ArrayList<>();

        zahlen.clear();
        zahlen = new ArrayList<>();

        zahlen_beschreibung.clear();
        zahlen_beschreibung= new ArrayList<>();

        gezeichnet.clear();
        gezeichnet = new ArrayList<>();

        super.onBackPressed();
    }
}
