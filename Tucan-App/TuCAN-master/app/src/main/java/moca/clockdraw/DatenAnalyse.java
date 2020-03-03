package moca.clockdraw;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class DatenAnalyse extends AppCompatActivity implements View.OnClickListener {

    Context context = this;

    TextView tv_vorname, tv_nachname, tv_vpnummer, tv_alter, tv_bildung, tv_geschlecht, tv_haendigkeit, tv_umgeschult, tv_datum, tv_sehschwaeche, tv_mci, tv_kommentar, tv_untersucher;
    TextView tv_task1, tv_task2, tv_task3;
    TextView tv_task1_globalStart, tv_task1_timeInAir, tv_task1_timeOnSurface, tv_task1_globalDuration, tv_task1_druck;
    TextView tv_task2_globalStart, tv_task2_timeInAir, tv_task2_timeOnSurface, tv_task2_globalDuration, tv_task2_druck;
    TextView tv_task3_globalStart, tv_task3_timeInAir, tv_task3_timeOnSurface, tv_task3_globalDuration, tv_task3_druck;
    ImageView iv_task1, iv_task2, iv_task3, iv_tremor1, iv_tremor2;

    ArrayList<TextView> tvId = new ArrayList<>();

    String timeString = "Zehn nach elf";

    Button buttonOpenDialog;
    Button btn_tremor1, btn_tremor2, btn_replay_task1, btn_replay_task2, btn_replay_task3, btn_ki_task2, btn_clock_rec;
    TextView textFolder;

    static final int CUSTOM_DIALOG_ID = 0;
    ListView dialog_ListView;

    File root;
    File curFolder;
    File ordner = null;
    File tempFolder = null;

    String oldComment;


    private boolean newInput;
    private boolean inVpOrdner;

    private List<String> fileList = new ArrayList<>();
    private List<String> fileList2 = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daten_analyse2);

        buttonOpenDialog = (Button) findViewById(R.id.opendialog);
        buttonOpenDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(CUSTOM_DIALOG_ID);
                if (ordner != root){
                    if (newInput){
                        saveComment();
                    }
                }
            }
        });

        //Initialize Buttons
        btn_ki_task2 = (Button) findViewById(R.id.ki_task2_b);
        btn_ki_task2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ordner == null) {
                    Toast.makeText(DatenAnalyse.this, "Zuerst Versuchsperson auswählen",
                            Toast.LENGTH_LONG).show();
                } else {
                    File imgFile_task2 = new File(curFolder + "/task2/task2.png");
                    if (imgFile_task2.exists()) {
                        String form =  tv_task2.getText().toString();
                        boolean wuerfel = true;
                        Log.d("Tobi1", "form: " + form);
                        if (form.equals("Form: Würfel") || form.equals("Form: Quader")) {
                            if (form.equals("Form: Würfel")){wuerfel = true;}
                            if (form.equals("Form: Quader")){wuerfel = false;}
                            Intent intent = new Intent(DatenAnalyse.this, CubeRecognitionResult.class);
                            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile_task2.getAbsolutePath());
                            intent.putExtra("file", imgFile_task2);
                            intent.putExtra("form", wuerfel);
                            startActivity(intent);
                        }else {
                            Toast.makeText(DatenAnalyse.this, "KI Auswertung nur beim Würfel oder Quader möglich",
                                    Toast.LENGTH_LONG).show();
                        }
                    }else {
                        Toast.makeText(DatenAnalyse.this, "Keine Daten vorhanden",
                                Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        btn_clock_rec = (Button) findViewById(R.id.cdt_start_recognition);
        btn_clock_rec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ordner == null) {
                    Toast.makeText(DatenAnalyse.this, "Zuerst Versuchsperson auswählen",
                            Toast.LENGTH_LONG).show();
                } else {

                    String SpdFileString = curFolder + "/task3/task3.spd";
                    Intent intent = new Intent(DatenAnalyse.this, ClockRecognitionResult.class);
                    intent.putExtra("ordner", SpdFileString);

                    if(timeString.charAt(0) == ' ') {
                        timeString = timeString.substring(1);
                    }
                    if(timeString.charAt(timeString.length() - 1) == ' ') {
                        timeString = timeString.substring(0, timeString.length());
                    }

                    intent.putExtra("timeString", timeString);
                    startActivity(intent);
                }
            }
        });

        btn_tremor1 = (Button) findViewById(R.id.tremoranalyse1_b);
        btn_tremor1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ordner == null) {
                    Toast.makeText(DatenAnalyse.this, "Zuerst Versuchsperson auswählen",
                            Toast.LENGTH_LONG).show();
                } else {
                    String SpdFileString = curFolder + "/tremoranalyse1/tremoranalyse1.spd";
                    Intent intent = new Intent(DatenAnalyse.this, showTask.class);
                    intent.putExtra("ordner", SpdFileString);
                    intent.putExtra("task", "tremoranalyse1");
                    startActivity(intent);
                }
            }
        });

        btn_tremor2 = (Button) findViewById(R.id.tremoranalyse2_b);
        btn_tremor2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ordner == null) {
                    Toast.makeText(DatenAnalyse.this, "Zuerst Versuchsperson auswählen",
                            Toast.LENGTH_LONG).show();
                } else {
                    String SpdFileString = curFolder + "/tremoranalyse2/tremoranalyse2.spd";
                    Intent intent = new Intent(DatenAnalyse.this, showTask.class);
                    intent.putExtra("ordner", SpdFileString);
                    intent.putExtra("task", "tremoranalyse2");
                    startActivity(intent);
                }
            }
        });

        btn_replay_task1 = (Button) findViewById(R.id.replay_task1_b);
        btn_replay_task1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ordner == null) {
                    Toast.makeText(DatenAnalyse.this, "Zuerst Versuchsperson auswählen",
                            Toast.LENGTH_LONG).show();
                } else {
                    String SpdFileString = curFolder + "/task1/task1.spd";
                    String tmtPngPath = curFolder + "/task1/task1.png";
                    Intent intent = new Intent(DatenAnalyse.this, showTask.class);
                    intent.putExtra("ordner", SpdFileString);
                    intent.putExtra("task", "task1");

                    intent.putExtra("tmtPngPath", tmtPngPath);
                    startActivity(intent);
                }
            }
        });

        btn_replay_task2 = (Button) findViewById(R.id.replay_task2_b);
        btn_replay_task2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ordner == null) {
                    Toast.makeText(DatenAnalyse.this, "Zuerst Versuchsperson auswählen",
                            Toast.LENGTH_LONG).show();
                } else {
                    String SpdFileString = curFolder + "/task2/task2.spd";
                    Intent intent = new Intent(DatenAnalyse.this, showTask.class);
                    intent.putExtra("ordner", SpdFileString);
                    intent.putExtra("task", "task2");
                    startActivity(intent);
                }
            }
        });

        btn_replay_task3 = (Button) findViewById(R.id.replay_task3_b);
        btn_replay_task3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ordner == null) {
                    Toast.makeText(DatenAnalyse.this, "Zuerst Versuchsperson auswählen",
                            Toast.LENGTH_LONG).show();
                } else {
                    String SpdFileString = curFolder + "/task3/task3.spd";
                    Intent intent = new Intent(DatenAnalyse.this, showTask.class);
                    intent.putExtra("ordner", SpdFileString);
                    intent.putExtra("task", "task3");
                    startActivity(intent);
                }
            }
        });

        root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MOCA/");
        curFolder = root;


        //initialize TextViews
        tv_vorname = (TextView) findViewById(R.id.vorname_tv);
        tv_nachname = (TextView) findViewById(R.id.nachname_tv);
        tv_vpnummer = (TextView) findViewById(R.id.vpnummer_tv);
        tv_alter = (TextView) findViewById(R.id.alter_tv);
        tv_bildung = (TextView) findViewById(R.id.bildung_tv);
        tv_geschlecht = (TextView) findViewById(R.id.geschlecht_tv);
        tv_haendigkeit = (TextView) findViewById(R.id.händigkeit_tv);
        tv_umgeschult = (TextView) findViewById(R.id.umgeschult_tv);
        tv_datum = (TextView) findViewById(R.id.datum_tv);
        tv_sehschwaeche = (TextView) findViewById(R.id.sehschwaeche_tv);
        tv_mci = (TextView) findViewById(R.id.mci_tv);
        tv_kommentar = (TextView) findViewById(R.id.kommentar_tv);
        tv_untersucher = (TextView) findViewById(R.id.betreuer_tv);


        //Tv data of the different tasks
        tv_task1 = (TextView) findViewById(R.id.task1_tv);
        tv_task1_globalStart = (TextView) findViewById(R.id.task1_globalStart_tv);
        tv_task1_globalDuration = (TextView) findViewById(R.id.task1_globalDuration_tv);
        tv_task1_timeInAir = (TextView) findViewById(R.id.task1_timeInAir_tv);
        tv_task1_timeOnSurface = (TextView) findViewById(R.id.task1_timeOnSurface_tv);
        tv_task1_druck = (TextView) findViewById(R.id.task1_druckstaerke_tv);

        tv_task2 = (TextView) findViewById(R.id.task2_tv);
        tv_task2_globalStart = (TextView) findViewById(R.id.task2_globalStart_tv);
        tv_task2_globalDuration = (TextView) findViewById(R.id.task2_globalDuration_tv);
        tv_task2_timeInAir = (TextView) findViewById(R.id.task2_timeInAir_tv);
        tv_task2_timeOnSurface = (TextView) findViewById(R.id.task2_timeOnSurface_tv);
        tv_task2_druck = (TextView) findViewById(R.id.task2_druckstaerke_tv);

        tv_task3 = (TextView) findViewById(R.id.task3_tv);
        tv_task3_globalStart = (TextView) findViewById(R.id.task3_globalStart_tv);
        tv_task3_globalDuration = (TextView) findViewById(R.id.task3_globalDuration_tv);
        tv_task3_timeInAir = (TextView) findViewById(R.id.task3_timeInAir_tv);
        tv_task3_timeOnSurface = (TextView) findViewById(R.id.task3_timeOnSurface_tv);
        tv_task3_druck = (TextView) findViewById(R.id.task3_druckstaerke_tv);

        iv_task1 = (ImageView) findViewById(R.id.task1_iv);
        iv_task2 = (ImageView) findViewById(R.id.task2_iv);
        iv_task3 = (ImageView) findViewById(R.id.task3_iv);
        iv_tremor1 = (ImageView) findViewById(R.id.tremor1_iv);
        iv_tremor2 = (ImageView) findViewById(R.id.tremor2_iv);


        tvId.add(tv_vpnummer);
        tvId.add(tv_vorname);
        tvId.add(tv_nachname);
        tvId.add(tv_alter);
        tvId.add(tv_bildung);
        tvId.add(tv_geschlecht);
        tvId.add(tv_haendigkeit);
        tvId.add(tv_umgeschult);
        tvId.add(tv_datum);
        tvId.add(tv_sehschwaeche);
        tvId.add(tv_mci);
        tvId.add(tv_kommentar);
        tvId.add(tv_untersucher);
        tvId.add(tv_task1);
        tvId.add(tv_task2);
        tvId.add(tv_task3);

        btn_tremor1 = (Button) findViewById(R.id.tremoranalyse1_b);


        //Comments
        oldComment = "";

        newInput = false;
        inVpOrdner = true;


        //Check if typing is done

        //If user clicks on Screen after typing Comment is saved, Keyboard is hidden and comment will be saved
        tv_kommentar.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                    saveComment();
                    newInput = false;
                }
            }
        });

        tv_kommentar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //Now we know we will recieve new Input
                newInput = true;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        //Make Comment Textview scrollable
        tv_kommentar.setMovementMethod(new ScrollingMovementMethod());

    }


    //Dialog for selecting the VP-Data to be shown
    @Override
    protected Dialog onCreateDialog(int id) {

        Dialog dialog = null;

        switch (id) {
            case CUSTOM_DIALOG_ID:
                dialog = new Dialog(DatenAnalyse.this);
                dialog.setContentView(R.layout.activity_dialog_layout);
                dialog.setTitle("Custom Dialog");
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);

                textFolder = (TextView) dialog.findViewById(R.id.folder);

                dialog_ListView = (ListView) dialog.findViewById(R.id.dialoglist);
                dialog_ListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        File selected = new File(fileList.get(position));

                        if (inVpOrdner) {
                            selected = new File(tempFolder, selected.getAbsolutePath());
                            //Subfolders of selected Vp-Folder will be listed
                            ListDir(selected);
                            inVpOrdner = false;
                        } else {
                            //Final folder of the test data is selected
                            Toast.makeText(DatenAnalyse.this, selected.toString() + " ausgewählt",
                                    Toast.LENGTH_LONG).show();
                            dismissDialog(CUSTOM_DIALOG_ID);

                            //Update folders
                            ordner = selected;
                            File vp_folder = tempFolder;
                            curFolder = new File(tempFolder, ordner.toString());


                            readUserData(selected.toString());
                            MemoryHelper.loadCSV(selected.toString(), vp_folder, context);
                            readTaskData();
                        }
                    }
                });
                break;
        }
        return dialog;
    }

    private void readTaskData() {

        File file = new File(curFolder + "/task1/task1.spd");

        if (file.exists()) {
            tv_task1_timeOnSurface.setText(String.valueOf(MemoryHelper.task1TimeAndScreenData.getTimeOnSurface()) + " ms");
            tv_task1_timeInAir.setText(String.valueOf(MemoryHelper.task1TimeAndScreenData.getTimeInAir()) + " ms");
            tv_task1_globalDuration.setText(String.valueOf(MemoryHelper.task1TimeAndScreenData.getTimeOffScreen()) + " ms");
            tv_task1_druck.setText(String.valueOf(Math.round(berechneDruckDuchschnitt(MemoryHelper.task1data) * 100)) + " %");

            File imgFile_task1 = new File(curFolder + "/task1/task1.png");
            if (imgFile_task1.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile_task1.getAbsolutePath());
                iv_task1.setImageBitmap(myBitmap);
            }


            long globalStart_task1 = MemoryHelper.task1TimeAndScreenData.getTimeInAir() + MemoryHelper.task1TimeAndScreenData.getTimeOffScreen() + MemoryHelper.task1TimeAndScreenData.getTimeOnSurface();
            tv_task1_globalStart.setText(String.valueOf(globalStart_task1) + " ms");
        } else {
            tv_task1_timeOnSurface.setText("n.a.");
            tv_task1_timeInAir.setText("n.a.");
            tv_task1_globalDuration.setText("n.a.");
            tv_task1_globalStart.setText("n.a.");
            tv_task1_druck.setText("n.a.");


            iv_task1.setImageDrawable(getDrawable(R.drawable.ic_empty_set_mathematical_symbol));
        }

        File file2 = new File(curFolder + "/task2/task2.spd");
        if (file2.exists()) {
            tv_task2_timeOnSurface.setText(String.valueOf(MemoryHelper.task2TimeAndScreenData.getTimeOnSurface()) + " ms");
            tv_task2_timeInAir.setText(String.valueOf(MemoryHelper.task2TimeAndScreenData.getTimeInAir()) + " ms");
            tv_task2_globalDuration.setText(String.valueOf(MemoryHelper.task2TimeAndScreenData.getTimeOffScreen()) + " ms");
            tv_task2_druck.setText(String.valueOf(Math.round(berechneDruckDuchschnitt(MemoryHelper.task2data) * 100)) + " %");




            File imgFile_task2 = new File(curFolder + "/task2/task2.png");
            if (imgFile_task2.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile_task2.getAbsolutePath());
                iv_task2.setImageBitmap(myBitmap);
            }

            long globalStart_task2 = MemoryHelper.task2TimeAndScreenData.getTimeInAir() + MemoryHelper.task2TimeAndScreenData.getTimeOffScreen() + MemoryHelper.task2TimeAndScreenData.getTimeOnSurface();
            tv_task2_globalStart.setText(String.valueOf(globalStart_task2) + " ms");
        } else {
            tv_task2_timeOnSurface.setText("n.a.");
            tv_task2_timeInAir.setText("n.a.");
            tv_task2_globalDuration.setText("n.a.");
            tv_task2_globalStart.setText("n.a.");
            tv_task2_druck.setText("n.a.");

            iv_task2.setImageDrawable(getDrawable(R.drawable.ic_empty_set_mathematical_symbol));
        }

        File file3 = new File(curFolder + "/task3/task3.spd");
        if (file3.exists()) {
            tv_task3_timeOnSurface.setText(String.valueOf(MemoryHelper.task3TimeAndScreenData.getTimeOnSurface()) + " ms");
            tv_task3_timeInAir.setText(String.valueOf(MemoryHelper.task3TimeAndScreenData.getTimeInAir()) + " ms");
            tv_task3_globalDuration.setText(String.valueOf(MemoryHelper.task3TimeAndScreenData.getTimeOffScreen()) + " ms");
            tv_task3_druck.setText(String.valueOf(Math.round(berechneDruckDuchschnitt(MemoryHelper.task3data) * 100)) + " %");

            File imgFile_task3 = new File(curFolder + "/task3/task3.png");
            if (imgFile_task3.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile_task3.getAbsolutePath());
                iv_task3.setImageBitmap(myBitmap);
            }

            long globalStart_task3 = MemoryHelper.task3TimeAndScreenData.getTimeInAir() + MemoryHelper.task3TimeAndScreenData.getTimeOffScreen() + MemoryHelper.task3TimeAndScreenData.getTimeOnSurface();
            tv_task3_globalStart.setText(String.valueOf(globalStart_task3) + " ms");
        } else {
            tv_task3_timeOnSurface.setText("n.a.");
            tv_task3_timeInAir.setText("n.a.");
            tv_task3_globalDuration.setText("n.a.");
            tv_task3_globalStart.setText("n.a.");
            tv_task3_druck.setText("n.a.");

            iv_task3.setImageDrawable(getDrawable(R.drawable.ic_empty_set_mathematical_symbol));
        }

        File imgFile_task4 = new File(curFolder + "/tremoranalyse1/tremoranalyse1.png");
        if (imgFile_task4.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile_task4.getAbsolutePath());
            iv_tremor1.setImageBitmap(myBitmap);
        } else {
            iv_tremor1.setImageDrawable(getDrawable(R.drawable.ic_empty_set_mathematical_symbol));
        }

        File imgFile_task5 = new File(curFolder + "/tremoranalyse2/tremoranalyse2.png");
        if (imgFile_task5.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile_task5.getAbsolutePath());
            iv_tremor2.setImageBitmap(myBitmap);
        } else {
            iv_tremor2.setImageDrawable(getDrawable(R.drawable.ic_empty_set_mathematical_symbol));
        }

    }

    private void readUserData(String selection) {
        BufferedReader reader;
        try {

            reader = new BufferedReader(new FileReader(curFolder + "/vpData.csv"));
            reader.readLine(); // discard the header fields
            String[] data = reader.readLine().split(";");
            String[] beschreibung = new String[]{"VP-Nummer:", "Vorname:", "Nachname:", "Alter:", "Bildungsjahre:",
                    "Geschlecht:", "Händigkeit:", "Umgeschult:", "Datum u. Zeit:", "Sehschwäche:", "MCI:", "Kommentar des Untersuchers:", "Untersucher:", "Pfad: ", "Form: ", "Uhr: "};
            String anhang;

            for (int tv = 0; tv < tvId.size(); tv++) {
                if (data[tv].isEmpty()) {
                    anhang = "n.a.";
                } else {
                    //Data string for the current Text View
                    anhang = data[tv];
                }

                String currText = "";


                if (!beschreibung[tv].equalsIgnoreCase("Kommentar des Untersuchers:")) {
                    //String with additional description
                    currText = "<b>" + beschreibung[tv] + "</b>";}

                    //For the three Task-Textviews no description necessary
                    if (beschreibung[tv].equals("Form: ") ||
                            beschreibung[tv].equals("Pfad: ") ||
                            beschreibung[tv].equals("Uhr: ")) {
                        currText +=  anhang;

                        if (beschreibung[tv].equals("Uhr: ")) { // needed for cdt AI
                            this.timeString = anhang;
                        }
                    }
                    else {
                    //Whole string that will be displayed: Description(in bold) + data String
                        currText += "<br>" + anhang;
                    }


                    //Read text in HTML format
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    tvId.get(tv).setText(Html.fromHtml(currText, Html.FROM_HTML_MODE_LEGACY));
                } else {
                    tvId.get(tv).setText(Html.fromHtml(currText));
                }
            }

            //Set the static fields in MemoryHelper representing the original screen resolution:
            MemoryHelper.originalCanvasWidth = Integer.parseInt(data[data.length - 2]);
            MemoryHelper.originalCanvasHeight = Integer.parseInt(data[data.length - 1]);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Fehler beim Öffnen!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private float berechneDruckDuchschnitt(ArrayList<SPenData> task) {
        int counter = 0;
        float pressureCounter = 0;
        float currentPressure = 0;
        for (int i = 0; i < task.size(); i++) {
            currentPressure = task.get(i).getPressure();
            if (currentPressure != 0) {
                counter++;
                pressureCounter += currentPressure;
            }
        }
        return pressureCounter / counter;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        switch (id) {
            case CUSTOM_DIALOG_ID:
                inVpOrdner = true;
                ListDir(root);
                break;
        }
    }

    void ListDir(File f) {

        tempFolder = f;
        textFolder.setText("Ordner: " + f.getName());

        File[] files = f.listFiles();
        fileList.clear();

        if (files != null) {
            for (File file : files) {
                fileList.add(file.getName());
            }

            ArrayAdapter<String> directoryList = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, fileList);
            dialog_ListView.setAdapter(directoryList);
        } else {
            Toast.makeText(DatenAnalyse.this, "Bisher keine Versuchspersonen vorhanden!", Toast.LENGTH_LONG).show();
        }

    }

    public void saveComment() {

        String HTMLkommentar = htmlToString(tv_kommentar);

        //Some aspects have to be concerned when saving HTML in a csv:

        //Replace HTML breaks
        String masked = HTMLkommentar.replaceAll("\n", "");

        //mask semicola(for saving "umlaute" properly)
        masked = masked.replaceAll(";", "_");
        //Delete underlines
        masked = masked.replaceAll("<u>|</u>", "");


        //Replace comment in csv File
        File file = new File(curFolder + "/vpData.csv");

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String header = reader.readLine();
            String[] data = reader.readLine().split(";");
            reader.close();

            BufferedWriter writer = new BufferedWriter(new FileWriter(curFolder + "/vpData.csv"));

            //Header
            writer.write(header);
            //new Line
            writer.write("\n");

            for (int i = 0; i < data.length; i++) {
                //Change comment
                //Comment is on 12th position
                if (i == 11) {
                    writer.write(masked + ";");
                } else {
                    writer.write(data[i] + ";");
                }
            }
            writer.close();


        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }


    @Override
    public void onBackPressed() {
        // If back is pressed, Comment will be saved
        if (newInput) {
            saveComment();
            newInput = false;
        }

        super.onBackPressed();
    }


    public static String htmlToString(TextView textview) {
        SpannableString contentText = new SpannableString(textview.getText());
        return Html.toHtml(contentText).toString();
    }


    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    @Override
    public void onClick(View v) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (ordner != null) {
            editor.putString("lastUserNumber", ordner.toString());
        }
        editor.apply();

        if (newInput) {
            saveComment();
            newInput = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        String lastSelectedUser = prefs.getString("lastUserNumber", "NA");
        if (!Objects.equals(lastSelectedUser, "NA")) {
            readUserData(lastSelectedUser);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); // Delete all Pref entries for a fresh the start of the app
        editor.apply();
    }
}

