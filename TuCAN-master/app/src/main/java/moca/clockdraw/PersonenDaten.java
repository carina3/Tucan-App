package moca.clockdraw;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;


public class PersonenDaten extends AppCompatActivity {

    Button b_fertig;
    EditText tv_name, tv_nachname, tv_vpnummer, tv_alter, tv_kommentar, tv_untersucher, tv_manualString, tv_bildung;
    public RadioGroup g_geschlecht, g_haendigkeit, g_umgeschult, g_sehschwaeche, g_mci, g_uhr, g_wuerfel, g_pfad;
    public RadioButton b_geschlecht, b_haendigkeit, b_umgeschult, b_sehschwaeche, b_mci, b_uhr, b_wuerfel, b_pfad;
    public File ordner;
    Button readButton;
    int testnumber;

    Context context;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personen_daten);

        //change activity
        b_fertig = (Button) findViewById(R.id.fertig_b);
        b_fertig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tv_vpnummer.getText().toString().isEmpty()) {
                    Toast.makeText(context, "Bitte eine VP Nummer angeben!", Toast.LENGTH_SHORT).show();
                } else {
                    int selectedTimeString = g_uhr.getCheckedRadioButtonId();
                    b_uhr = (RadioButton) findViewById(selectedTimeString);
                    String uhr = b_uhr.getText().toString();

                    if (uhr.equals(getString(R.string.cdt_time_version_manual)) &&
                            (tv_manualString == null || tv_manualString.getText().toString().isEmpty())) {
                        Toast.makeText(context, "Bitte eine Uhrzeit im Textfeld eingeben!", Toast.LENGTH_LONG).show();
                    } else {
                        checkVPselection(tv_vpnummer.getText().toString());
                    }
                }
            }
        });

        context = this;
        tv_name = (EditText) findViewById(R.id.name_tv);
        tv_nachname = (EditText) findViewById(R.id.nachname_tv);
        tv_vpnummer = (EditText) findViewById(R.id.vpnummer_tv);
        tv_alter = (EditText) findViewById(R.id.alter_tv);
        tv_kommentar = (EditText) findViewById(R.id.kommentar_tv);
        tv_untersucher = (EditText) findViewById(R.id.betreuer_tv);
        tv_manualString = (EditText) findViewById(R.id.cdt_manual_string);
        tv_bildung = (EditText) findViewById(R.id.bildung_tv);

        readButton = (Button) findViewById(R.id.opendialog);

        g_geschlecht = (RadioGroup) findViewById(R.id.geschlecht_g);
        g_haendigkeit = (RadioGroup) findViewById(R.id.haendigkeit_g);
        g_umgeschult = (RadioGroup) findViewById(R.id.umgeschult_g);
        g_sehschwaeche = (RadioGroup) findViewById(R.id.sehschwaeche_g);
        g_mci = (RadioGroup) findViewById(R.id.mci_g);
        g_wuerfel = (RadioGroup) findViewById(R.id.wuerfel_g);
        g_uhr = (RadioGroup) findViewById(R.id.uhrzeit_g);
        g_pfad = (RadioGroup) findViewById(R.id.pfad_g);

        testnumber = 1;

        // Adding a little bit of automatic focus and cursor changes to enhance user experience:
        final RadioButton b_manualClock = (RadioButton) findViewById(R.id.uhrzeit4_radio);
        final TextView tv_manualString = (TextView) findViewById(R.id.cdt_manual_string);
        tv_manualString.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //automatically set the "manuell" radio button to state true if a time gets inserted
                b_manualClock.toggle();
            }
        });

        tv_manualString.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) b_manualClock.toggle();
            }
        });

        b_manualClock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    tv_manualString.requestFocus();
                    tv_manualString.setCursorVisible(true);
                }
            }
        });
    }

    public void speichern() {
        int selected_id = g_geschlecht.getCheckedRadioButtonId();
        b_geschlecht = (RadioButton) findViewById(selected_id);
        String geschlecht = b_geschlecht.getText().toString();

        selected_id = g_haendigkeit.getCheckedRadioButtonId();
        b_haendigkeit = (RadioButton) findViewById(selected_id);
        String haendigkeit = b_haendigkeit.getText().toString();

        selected_id = g_umgeschult.getCheckedRadioButtonId();
        b_umgeschult = (RadioButton) findViewById(selected_id);
        String umgeschult = b_umgeschult.getText().toString();

        selected_id = g_sehschwaeche.getCheckedRadioButtonId();
        b_sehschwaeche = (RadioButton) findViewById(selected_id);
        String sehschwaeche = b_sehschwaeche.getText().toString();

        selected_id = g_mci.getCheckedRadioButtonId();
        b_mci = (RadioButton) findViewById(selected_id);
        String mci = b_mci.getText().toString();

        selected_id = g_uhr.getCheckedRadioButtonId();
        b_uhr = (RadioButton) findViewById(selected_id);
        String uhr = b_uhr.getText().toString();

        selected_id = g_wuerfel.getCheckedRadioButtonId();
        b_wuerfel = (RadioButton) findViewById(selected_id);
        String wuerfel = b_wuerfel.getText().toString();

        selected_id = g_pfad.getCheckedRadioButtonId();
        b_pfad = (RadioButton) findViewById(selected_id);
        String pfad = b_pfad.getText().toString();

        SharedPreferences prefs = getSharedPreferences("com.moca.MODE_SETTINGS", Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putString("form", wuerfel);
        prefEditor.putString("pfad", pfad);
        if (haendigkeit.equals("Rechts")) {
            prefEditor.putBoolean("reightHander", true);
        } else {
            prefEditor.putBoolean("reightHander", false);
        }

        String name = tv_name.getText().toString();
        String nachname = tv_nachname.getText().toString();
        String vpnummer = tv_vpnummer.getText().toString();
        String alter = tv_alter.getText().toString();
        String kommentar = tv_kommentar.getText().toString();
        String untersucher = tv_untersucher.getText().toString();
        String manualTime = tv_manualString.getText().toString();
        String bildung = tv_bildung.getText().toString();

        uhr = uhr.equals("Manuell:") ? manualTime : uhr;
        prefEditor.putString("timeString", uhr);

        prefEditor.apply();

        String breite = String.valueOf(MemoryHelper.originalCanvasWidth);
        String hoehe = String.valueOf(MemoryHelper.originalCanvasHeight);

        MemoryHelper.currentVpNumber = vpnummer;

        Calendar kalender = Calendar.getInstance();
        SimpleDateFormat datumsformat = new SimpleDateFormat("dd.MM.yyyy HH.mm");
        String datum = datumsformat.format(kalender.getTime()) + " Uhr";

        ArrayList<String> fieldnames = new ArrayList<>();
        fieldnames.addAll(Arrays.asList("id", "name", "vorname", "alter", "bildung", "geschlecht", "haendigkeit", "umgeschult", "datum", "sehschwaeche", "mci",
                "kommentar", "untersucher", "pfad", "wuerfel", "uhr", "breite", "hoehe"));
        ArrayList<String> data = new ArrayList<>();
        data.addAll(Arrays.asList(vpnummer, name, nachname, alter, bildung, geschlecht, haendigkeit, umgeschult, datum, sehschwaeche, mci, kommentar, untersucher, pfad, wuerfel, uhr, breite, hoehe));

        saveCSV(fieldnames, data);
    }

    public void saveCSV(ArrayList<String> fieldnames, ArrayList<String> data) {

        try {

            String testNumberString = String.valueOf(testnumber);

            //Create Folder if it doesnt exist
            String id = data.get(0);
            String vpFolder = "MOCA/" + "vp " + id ;//+ "/test " + testNumberString;
            ordner = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), vpFolder);

            //save on external storage
            if (!ordner.exists()) {
                if (!ordner.mkdirs()) {
                    Toast.makeText(context, "Fehler im Dateipfad!", Toast.LENGTH_LONG).show();
                }
            }

            //Create a subdirectory if it doesn't exist
            String testFolder = "/test " + testNumberString;
            ordner = new File(ordner.getPath(), testFolder);


            if (!ordner.exists()) {
                if (!ordner.mkdirs()) {
                    Toast.makeText(context, "Fehler im Dateipfad ("+ordner.getPath()+")!", Toast.LENGTH_LONG).show();
                }
            }

            //add number of test with this VP to shares Preferences
            SharedPreferences prefs = getSharedPreferences("com.moca.MODE_SETTINGS", Context.MODE_PRIVATE);
            SharedPreferences.Editor prefEditor = prefs.edit();
            prefEditor.putString("testnumber", testNumberString);
            prefEditor.apply();

            BufferedWriter buffWriter;
            buffWriter = new BufferedWriter(new FileWriter(ordner + "/vpData" + ".csv"));
            for (int i = 0; i < fieldnames.size(); i++) {
                buffWriter.write(fieldnames.get(i));
                buffWriter.write(";");
            }
            buffWriter.write("\n");

            for (int i = 0; i < data.size(); i++) {
                buffWriter.write(data.get(i));
                if (i + 1 < data.size()) {
                    buffWriter.write(";");
                }
            }

            buffWriter.flush();
            buffWriter.close();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    /*
     * Add the options menu, in which there can be selected, whether to use the optimized drawing
     * version for tablet or the original version matching the one on paper.
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu with the according checkbox
        boolean superReturnVal = super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);

        // Load the last setting from app-wide SharedPrefs:
        SharedPreferences prefs = getSharedPreferences("com.moca.MODE_SETTINGS", Context.MODE_PRIVATE);
        boolean isTabletOptimized = prefs.getBoolean("isTabletOptimized", false);

        // Set the state of the checkbox to the last known state:
        menu.getItem(0).setChecked(isTabletOptimized);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        // Check whether the press occured on settings menu or on back button:
        if (!(item.getItemId() == R.id.tabletOptimizedSwitch)) {
            onBackPressed(); // "Press" the back button
        } else {
            SharedPreferences prefs = getSharedPreferences("com.moca.MODE_SETTINGS", Context.MODE_PRIVATE);
            SharedPreferences.Editor prefEditor = prefs.edit();

            // Negotiate the checkbox state and the boolean value in the shared prefs:
            boolean lastItemState = item.isChecked();
            item.setChecked(!lastItemState);
            prefEditor.putBoolean("isTabletOptimized", !lastItemState);
            prefEditor.apply();
        }

        return true;
    }

    private void checkVPselection(String vpNumberString) {
        ordner = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MOCA/vp " + vpNumberString);
        if (ordner.exists() && ordner.isDirectory() && (ordner.list().length > 0)) {

            // In this case there has been a person with this id already recorded.
            // => Ask the user if he likes to overwrite this person:
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.vpNumberAlertTitle);
            builder.setMessage(R.string.vpNumberAlertMessage);
            builder.setPositiveButton(R.string.vpNumberPosButton, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    testnumber = calcTestNumber();
                    //ordner = new File(ordner.getPath(), "/test " + testnumber);
                    speichern();

                    Intent intent = new Intent(PersonenDaten.this, TasksMenue.class);
                    intent.putExtra("ordner", ordner); //TODO: check if ordner has to be updatet to /test i/
                    startActivity(intent);
                }
            });
/*
Third button, overwrites VP if it already exists

            builder.setNegativeButton(R.string.vpNumberNegButton, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    testnumber = 1;
                    //ordner = new File(ordner.getPath(), "/test " + testnumber);
                    removeFile(ordner);
                    speichern();
                    Intent intent = new Intent(PersonenDaten.this, TasksMenue.class);
                    intent.putExtra("ordner", ordner);
                    startActivity(intent);
                }
            });*/

            builder.setNeutralButton(R.string.vpNumberNeutButton, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            speichern();
            Intent intent = new Intent(PersonenDaten.this, TasksMenue.class);
            intent.putExtra("ordner", ordner);
            startActivity(intent);
        }
    }

    private  int calcTestNumber(){
        //Get number of test with this VP
        int testnumber = 1;
        for (int ind = 2; new File(ordner, "/test " + String.valueOf(ind - 1) ).exists(); ind++) {
            testnumber = ind;
        }
        return  testnumber;

    }

    //Removes File/Directory with all of its subdirectories and files
    private void removeFile(File file) {
        if (file == null || !file.exists()) {
            Toast.makeText(this, "Remove of former data failed.", Toast.LENGTH_SHORT).show();
        }
        if (file.isDirectory()) {
            File[] list = file.listFiles();
            if (list != null) {

                for (File item : list) {
                    removeFile(item);
                }
            }
        }

        if (file.exists()) {
            file.delete();
        }
    }
}