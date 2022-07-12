package ca.yorku.eecs.mack.demotiltball78040;

import android.content.Intent;
import android.app.Activity;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class ResultsActivity extends Activity implements SeekBar.OnSeekBarChangeListener {

    private final static String MYDEBUG = "MYDEBUG"; // for Log.i messages

    private final static String DATA_DIRECTORY = "/DemoTiltBallData/";
    private final static String SD2_HEADER = "Participant,DispMethod,Group,Trial,"
            + "LapTime,OptimalLapTime,TotalTime,Accuracy(%),MissPath,InputRate\n";

    private final static String SD3_HEADER = "Participant,DispMethod,Group,Trial,"
            + "{x_y_t}\n";

    private BufferedWriter sd1;
    private File f1;

    private BufferedWriter sd2;
    private File f2;

    String input_type;
    int group;

    int number_laps ;
    double[] lap_time;
    double[] in_path_time;
    double[] missed_lap_time, best_times;
    double total_time;
    int[] wall_hints;

    String[] x_positions;
    String[] y_positions;
    String[] t_positions;

    TextView seekbar_value;
    SeekBar seekBar;

    float width,height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        seekbar_value = (TextView) findViewById(R.id.seekbar_value);
        seekBar = (SeekBar) findViewById(R.id.seekBar1);
        seekBar.setOnSeekBarChangeListener(this);

        Bundle b = getIntent().getExtras();
        wall_hints = b.getIntArray("wall_hints");
        in_path_time = b.getDoubleArray("in_path_time");
        missed_lap_time = b.getDoubleArray("missed_lap_time");
        best_times = b.getDoubleArray("best_times");
        lap_time = b.getDoubleArray("lap_time");
        number_laps = b.getInt("number_laps");
        input_type = b.getString("inputType");
        group = b.getInt("group");

        width = b.getFloat("rb_width");
        height = b.getFloat("rb_height");

        x_positions= b.getStringArray("x_pos");
        y_positions= b.getStringArray("y_pos");



//        t_positions= b.getStringArray("t_pos");


        total_time=0;
        for (double v : lap_time) total_time += v;

        seekbar_value.setText("5");

    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean arg2)
    {

        // update the value displayed in the UI below the slider
        seekbar_value.setText(Integer.toString(progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar arg0)
    {
    }

    @Override
    public void onStopTrackingTouch(SeekBar arg0)
    {
    }

    // called when the "setup_click" button is tapped
    public void setup_click(View view)
    {
        // start experiment activity
        Intent i = new Intent(getApplicationContext(), DemoTiltBallSetup.class);
        startActivity(i);
        super.onDestroy();
        finish();
    }

    /** Called when the "Exit" button is pressed. */
    public void clickExit(View view)
    {
        super.onDestroy(); // cleanup
        finish(); // terminate
    }

    public void stats_click(View view)
    {
        Bundle b = new Bundle();

        b.putIntArray("wall_hints", wall_hints);
        b.putDoubleArray("in_path_time",in_path_time);
        b.putDoubleArray("missed_lap_time",missed_lap_time);
        b.putDoubleArray("best_times", best_times);
        b.putDoubleArray("lap_time",lap_time);
        b.putInt("number_laps",number_laps);
        b.putString("inputType",input_type);
        b.putInt("group",group);

        b.putStringArray("x_pos",x_positions);
        b.putStringArray("y_pos",y_positions);

        b.putFloat("rb_width",width);
        b.putFloat("rb_height",height);


        Intent i = new Intent(getApplicationContext(), PathAnalysisActivity.class);
        i.putExtras(b);
        startActivity(i);
//        super.onPause();
//        super.onDestroy();
//        finish();
    }

    public void save_click(View v) {
        File dataDirectory = new File(Environment.getExternalStorageDirectory() +
                DATA_DIRECTORY);
        if (!dataDirectory.exists() && !dataDirectory.mkdirs())
        {
            Log.e(MYDEBUG, "ERROR --> FAILED TO CREATE DIRECTORY: " + DATA_DIRECTORY);
            return;
        }
        f1 = new File(dataDirectory, "db1.csv");

        String f1_content= "";
        int line_count=-1;
        try {
            Scanner myReader = new Scanner(f1);
            while (myReader.hasNextLine()) {
                f1_content += myReader.nextLine()+"/n";
                line_count++;
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        try {
            sd1 = new BufferedWriter(new FileWriter(f1,true));
            if(line_count==-1)
            {
                sd1.write(SD2_HEADER, 0, SD2_HEADER.length());
                line_count++;
            }
            String new_str ="";
            for( int i=0; i< lap_time.length; i++){
                new_str= Integer.toString(line_count/5)+","+input_type+",G0"+Integer.toString(group)+","+ Integer.toString(i+1)+","+ Double.toString(lap_time[i])+","+ Double.toString(best_times[i])+","+Double.toString(total_time)+","+Double.toString(in_path_time[i])+","+Double.toString(wall_hints[i])+","+Integer.toString(seekBar.getProgress());
                sd1.write(new_str, 0, new_str.length());
                sd1.newLine();
            }
            sd1.flush();


            sd1.close();
            MediaScannerConnection.scanFile(this, new String[] {f1.getAbsolutePath()}, null,
                    null);

            Toast.makeText(this, "Saved to " + dataDirectory + "db1.csv" ,
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }


        f2 = new File(dataDirectory, "db2.csv");

        String f2_content= "";
        line_count=-1;
        try {
            Scanner myReader = new Scanner(f2);
            while (myReader.hasNextLine()) {
                f2_content += myReader.nextLine()+"/n";
                line_count++;
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        try {
            sd2 = new BufferedWriter(new FileWriter(f2,true));
            if(line_count==-1)
            {
                sd2.write(SD3_HEADER, 0, SD3_HEADER.length());
                line_count++;
            }


            String new_str ="";
            for( int i=0; i< x_positions.length; i++){
                new_str= Integer.toString(line_count/10)+","+input_type+",G0"+Integer.toString(group)+","+ Integer.toString(i+1)+",x,"+x_positions[i];
                sd2.write(new_str, 0, new_str.length());
                sd2.newLine();

                new_str= Integer.toString(line_count/10)+","+input_type+",G0"+Integer.toString(group)+","+ Integer.toString(i+1)+",y,"+y_positions[i];
                sd2.write(new_str, 0, new_str.length());
                sd2.newLine();

//                new_str= Integer.toString(0)+","+input_type+",G0"+Integer.toString(group)+","+ Integer.toString(i+1)+",t,"+t_positions[i];
//                sd2.write(new_str, 0, new_str.length());
//                sd2.newLine();
            }
            sd2.flush();


            sd2.close();
            MediaScannerConnection.scanFile(this, new String[] {f2.getAbsolutePath()}, null,
                    null);

            Toast.makeText(this, "Saved to " + dataDirectory + "db2.csv" ,
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }




    }

}