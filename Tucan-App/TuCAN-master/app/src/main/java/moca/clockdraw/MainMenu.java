package moca.clockdraw;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainMenu extends AppCompatActivity {

//    Button b_startTest, b_startDataAnalyse;

    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        context = this;

        // get the absolute size of the current screen and set the according variable in MemoryHelper class.
        // Also calculate the desired pen size from this value.
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        MemoryHelper.actualScreenResolution = displayMetrics.widthPixels * displayMetrics.heightPixels;
        float screenScalingFactor = (float) MemoryHelper.actualScreenResolution / MemoryHelper.referenceScreenResolution * 1.0f;
        MemoryHelper.actualPenSize = screenScalingFactor * MemoryHelper.referencePenSize;

        /*  Check if all necessary permissions are granted and otherwise ask the user to grant them
            via an explaining Dialog: */
        if(ContextCompat.checkSelfPermission(context, Manifest.permission_group.STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainMenu.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

//        b_startTest = (Button)findViewById(R.id.startTest_b);
//        b_startDataAnalyse = (Button)findViewById(R.id.startDataAnalyse_b);

        LinearLayout test_starten = (LinearLayout)findViewById(R.id.startTest_b);
        test_starten.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainMenu.this, PersonenDaten.class);
                startActivity(intent);
            }
        });

        LinearLayout daten_analysieren = (LinearLayout)findViewById(R.id.startDataAnalyse_b);
        daten_analysieren.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenu.this, DatenAnalyse.class);
                startActivity(intent);
            }
        });

//        b_startTest.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                Intent intent = new Intent(MainMenu.this, PersonenDaten.class);
//                startActivity(intent);
//            }
//        });

//        b_startDataAnalyse.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                Intent intent = new Intent(MainMenu.this, DatenAnalyse.class);
//                startActivity(intent);
//            }
//        });

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //super.onConfigurationChanged(newConfig);
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

        if(item.getItemId() == R.id.startAbout){
            // start the webview activity to display the info/faq/copyright:
            Intent startInfo =  new Intent(MainMenu.this, InfoWebView.class);
            startActivity(startInfo);
        } else {
            SharedPreferences prefs = getSharedPreferences("com.moca.MODE_SETTINGS", Context.MODE_PRIVATE);
            SharedPreferences.Editor prefEditor = prefs.edit();

            // Negotiate the checkbox state and the boolean value in the shared prefs:
            boolean lastItemState = item.isChecked();
            item.setChecked(!lastItemState);
            prefEditor.putBoolean("isTabletOptimized", !lastItemState);
            prefEditor.commit();
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 1) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.i("info", "STORAGE permission has been granted by user!");
            } else {
                Log.e("info", "STORAGE permision has been denied by user!");

                Toast.makeText(context, "Speicherzugriff verweigert. App funktioniert nicht richtig. " +
                        "App neu starten und Speicherzugriff zulassen!", Toast.LENGTH_LONG).show();
            }
        }
    }
}
