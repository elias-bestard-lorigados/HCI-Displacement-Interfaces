package ca.yorku.eecs.mack.demotiltball78040;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
public class PathAnalysisActivity extends Activity {
    final static String MYDEBUG = "MYDEBUG"; // for Log.i messages
    PathAnalysis pa;

    //Stats
    double total_time;
    int[] wall_hints;
    int number_laps,group;
    double[] in_path_time, lap_time, missed_lap_time, best_times;
    String input_type;
    String[] x_positions, y_positions,t_positions;

    int trial_no;
    String[] paths=new String[]{"Maze2","Square", "Maze","Circle", "Maze3"};;

    float widht, height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_analysis);

        Bundle b = getIntent().getExtras();
        wall_hints = b.getIntArray("wall_hints");
        best_times = b.getDoubleArray("best_times");
        in_path_time = b.getDoubleArray("in_path_time");
        lap_time = b.getDoubleArray("lap_time");
        missed_lap_time = b.getDoubleArray("missed_lap_time");
        number_laps = b.getInt("number_laps");
        input_type = b.getString("inputType");
        group = b.getInt("group");

        x_positions= b.getStringArray("x_pos");
        y_positions= b.getStringArray("y_pos");
        t_positions= b.getStringArray("t_pos");

        widht = b.getFloat("rb_width");
        height = b.getFloat("rb_height");


        total_time=0;
        for (double v : lap_time) total_time += v;
        trial_no=0;
        pa = (PathAnalysis)findViewById(R.id.pathAnalysis);
        pa.configure(paths[trial_no],trial_no,x_positions[trial_no].split(","),y_positions[trial_no].split(","),widht,
                height,
                wall_hints[trial_no],
                in_path_time[trial_no],
                lap_time[trial_no],
                missed_lap_time[trial_no],
                best_times[trial_no]
        );
        pa.config_path();

    }

    public void next_click(View v) {
        trial_no+=1;
        if (trial_no==5){
            back_click(v);
            return;
        }
        pa = (PathAnalysis)findViewById(R.id.pathAnalysis);
        pa.configure(paths[trial_no],trial_no,x_positions[trial_no].split(","),y_positions[trial_no].split(","),
                widht,
                height,
                wall_hints[trial_no],
                in_path_time[trial_no],
                lap_time[trial_no],
                missed_lap_time[trial_no],
                best_times[trial_no]
                );
        pa.config_path();
        pa.invalidate();

    }

    public void back_click(View v) {
        super.onDestroy();
        finish();
    }
}