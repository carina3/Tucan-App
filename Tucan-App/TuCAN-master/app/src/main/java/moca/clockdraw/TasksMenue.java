package moca.clockdraw;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

public class TasksMenue extends AppCompatActivity {

    ImageView iv_form;
    TextView tv_aufgabe;
    CardView b_tremoranalyse1, b_tremoranalyse2, b_taskClockDraw, b_taskPathDraw, b_taskCubeDraw, b_zeichnenUeben;
    File ordner;

    Context context;

    private int latestMotionEventType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks_menue);

        context = this;
        latestMotionEventType = -1;

        ordner = (File) getIntent().getExtras().get("ordner");

        iv_form = (ImageView) findViewById(R.id.form_iv);
        tv_aufgabe = (TextView) findViewById(R.id.aufgabe_tv);
        SharedPreferences prefs = getSharedPreferences("com.moca.MODE_SETTINGS", Context.MODE_PRIVATE);
        final String form = prefs.getString("form", "fehler");
        switch (form){
            case "Würfel":
                iv_form.setImageResource(R.drawable.wuerfel);
                tv_aufgabe.setText("Würfel zeichnen");
                break;
            case "Quader":
                iv_form.setImageResource(R.drawable.cuboidsimple);
                tv_aufgabe.setText("Quader zeichnen");
                break;
            case "Zylinder":
                iv_form.setImageResource(R.drawable.zylinder_quer);
                tv_aufgabe.setText("Zylinder zeichnen");
                break;
        }

        b_tremoranalyse1 = (CardView) findViewById(R.id.task_tremor1);
        b_tremoranalyse2 = (CardView) findViewById(R.id.task_tremor2);
        b_taskClockDraw = (CardView) findViewById(R.id.task_drawClock);
        b_taskPathDraw = (CardView) findViewById(R.id.task_drawPath);
        b_taskCubeDraw = (CardView) findViewById(R.id.task_drawCube);
        b_zeichnenUeben = (CardView) findViewById(R.id.task_zeichnenUeben);

        b_zeichnenUeben.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TasksMenue.this, zeichnenUeben.class);
                intent.putExtra("ordner", ordner);
                startActivity(intent);
            }
        });

        b_tremoranalyse1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TasksMenue.this, tremoranalyse1.class);
                intent.putExtra("ordner", ordner);
                startActivity(intent);
            }
        });

        b_tremoranalyse2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TasksMenue.this, tremoranalyse2.class);
                intent.putExtra("ordner", ordner);
                startActivity(intent);
            }
        });

        b_taskClockDraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TasksMenue.this, task3.class);
                intent.putExtra("ordner", ordner);
                startActivity(intent);
            }
        });

        b_taskPathDraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TasksMenue.this, task1.class);
                intent.putExtra("ordner", ordner);
                startActivity(intent);
            }
        });

        b_taskCubeDraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TasksMenue.this, task2.class);
                intent.putExtra("ordner", ordner);
                startActivity(intent);
            }
        });

        View.OnHoverListener hoverListener = new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                CardView view = (CardView) v;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_HOVER_ENTER:
                        if(latestMotionEventType == MotionEvent.ACTION_UP) break;
                        view.setCardBackgroundColor(ContextCompat.getColor(context, R.color.hoverGray));
                        break;

                    case MotionEvent.ACTION_HOVER_EXIT:
                        view.setCardBackgroundColor(Color.WHITE);
                        break;
                }

                latestMotionEventType = event.getAction();

                return true;
            }
        };

        b_zeichnenUeben.setOnHoverListener(hoverListener);
        b_taskClockDraw.setOnHoverListener(hoverListener);
        b_taskCubeDraw.setOnHoverListener(hoverListener);
        b_taskPathDraw.setOnHoverListener(hoverListener);
        b_tremoranalyse1.setOnHoverListener(hoverListener);
        b_tremoranalyse2.setOnHoverListener(hoverListener);

        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                CardView view = (CardView) v;

                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.setCardBackgroundColor(ContextCompat.getColor(context, R.color.hoverGray));
                        view.setCardElevation(3.0f * view.getCardElevation());
                        break;

                    case MotionEvent.ACTION_UP:
                        view.setCardElevation(0.5f * view.getCardElevation());
                        view.setCardBackgroundColor(Color.WHITE);
                        break;
                }

                latestMotionEventType = event.getAction();

                return false;
            }
        };

        b_zeichnenUeben.setOnTouchListener(touchListener);
        b_taskClockDraw.setOnTouchListener(touchListener);
        b_taskCubeDraw.setOnTouchListener(touchListener);
        b_taskPathDraw.setOnTouchListener(touchListener);
        b_tremoranalyse1.setOnTouchListener(touchListener);
        b_tremoranalyse2.setOnTouchListener(touchListener);
    }

    @Override
    /*
     * Add the options menu, in which there can be selected whether to use the optimized drawing
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
        if(!(item.getItemId() == R.id.tabletOptimizedSwitch)) {
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

}
