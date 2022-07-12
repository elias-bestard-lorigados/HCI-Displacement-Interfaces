package ca.yorku.eecs.mack.demotiltball78040;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import io.github.controlwear.virtual.joystick.android.JoystickView;

/**
 * Demo_Android - with modifications by...
 *
 * Login ID - <elias39>
 * Student ID - <217978040>
 * Last name - <Bestard Lorigados>
 * First name(s) - <Elias>
 */
public class InputsActivity extends Activity implements View.OnTouchListener{
    final static String MYDEBUG = "MYDEBUG"; // for Log.i messages

    final static int REFRESH_INTERVAL = 20; // milliseconds (screen refreshes @ 50 Hz)

    RollingBallPanel rb;
    AlertDialog dialog;

    // parameters from the Setup dialog
    String[] paths = new String[]{"Maze2","Square", "Maze","Circle", "Maze3"};
//    String[] paths = new String[]{"Maze3"};

    //BundleSetup
    int gain, experiment_mode, group;
    String inputType;


    ScreenRefreshTimer refreshScreen;
    int strength_left, strength_right, angle_right, angle_left;
    int path_index;

    //Single Joystick
    int angle, strength, x_pos, y_pos;
    //  For the Buttom layout
    boolean action_up,action_down, action_left, action_right;
    ImageButton button_up,button_down,button_left,button_right;


    // Statistics
    double[] trials_time,accuracy , missed_lap_time, best_times;
    int[] miss_path;
    //path_stats_history
    String[] x_positions;
    String[] y_positions;
    String[] t_positions;



    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // get parameters selected by user from setup dialog
        Bundle b = getIntent().getExtras();
        gain = b.getInt("gain");
        experiment_mode = b.getInt("experiment_mode");
        group = b.getInt("group");
        inputType = b.getString("inputType");

        switch (inputType){
            case "Joystick":
                init_joystick_input();
                break;
            case "Button":
                init_button_input();
                break;
            default:
                init_dual_joystick_input();
                break;
        }

        path_index=0;
        accuracy = new double[paths.length];
        missed_lap_time= new double[paths.length];
        trials_time = new double[paths.length];
        miss_path = new int[paths.length];
        best_times = new double[paths.length];

        x_positions= new String[paths.length];
        y_positions= new String[paths.length];
        t_positions= new String[paths.length];

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Click OK to continue to the next steps")
                .setTitle(R.string.dialog_title);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        builder.setCancelable(false);
        dialog = builder.create();


        // configure rolling ball panel, as per setup parameters
        rb = (RollingBallPanel)findViewById(R.id.rollingballpanel);
        rb.configure(paths[path_index], gain,experiment_mode);
        rb.config_path();
        // setup the screen refresh timer (updates every REFRESH_INTERVAL milliseconds)
        refreshScreen = new ScreenRefreshTimer(REFRESH_INTERVAL, REFRESH_INTERVAL);
        refreshScreen.start();
    }

    private void init_dual_joystick_input()
    {
        setContentView(R.layout.main_dual_joystick_input);
        final JoystickView joystickRight = (JoystickView) findViewById(R.id.joystickView_right);
        joystickRight.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle_arg, int strength_arg) {
                angle_right=angle_arg;
                strength_right=strength_arg;

            }
        });
        final JoystickView joystickLeft = (JoystickView) findViewById(R.id.joystickView_left);
        joystickLeft.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle_arg, int strength_arg) {
                angle_left=angle_arg;
                strength_left=strength_arg;

            }
        });
    }

    private void init_button_input()
    {
        setContentView(R.layout.main_button_input);

        button_up= (ImageButton)findViewById(R.id.upBtn);
        button_up.setOnTouchListener(this);

        button_down = (ImageButton)findViewById(R.id.downBtn);
        button_down.setOnTouchListener(this);

        button_left = (ImageButton)findViewById(R.id.leftBtn);
        button_left.setOnTouchListener(this);

        button_right = (ImageButton)findViewById(R.id.rightBtn);
        button_right.setOnTouchListener(this);
    }
    private void init_joystick_input()
    {
        setContentView(R.layout.main_joystick_input);
        final JoystickView joystickLeft = (JoystickView) findViewById(R.id.joystickView_right);
        joystickLeft.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle_arg, int strength_arg) {
                angle=angle_arg;
                strength=strength_arg;
                y_pos = joystickLeft.getNormalizedY()-50;
                x_pos = joystickLeft.getNormalizedX()-50;

            }
        });
    }

    /*
     * Cancel the timer when the activity is stopped. If we don't do this, the timer continues after
     * the activity finishes. See...
     *
     * http://stackoverflow.com/questions/15144232/countdowntimer-continues-to-tick-in-background-how
     * -do-i-retrieve-that-count-in
     */
    @Override
    public void onStop()
    {
        refreshScreen.cancel();
        super.onStop();
    }

    //For the Button Input Method!!!
    @Override
    public boolean onTouch(View v, MotionEvent me) {
        if( v != button_up && v != button_down && v != button_left && v != button_right)
            return false;

        switch (me.getActionMasked())
        {
            case MotionEvent.ACTION_DOWN: // this is the first touch point in a gesture
            case MotionEvent.ACTION_POINTER_DOWN: // this is a subsequent touch point
                switch (v.getId()) {
                    case R.id.upBtn:
                        action_up = true;
                        break;
                    case R.id.downBtn:
                        action_down = true;
                        break;
                    case R.id.leftBtn:
                        action_left = true;
                        break;
                    case R.id.rightBtn:
                        action_right = true;
                        break;
                }
                v.setPressed(true);
                break;

            case MotionEvent.ACTION_POINTER_UP: // a finger goes up (but it's not the last finger)
            case MotionEvent.ACTION_UP: // last touch point (end of gesture)
                switch (v.getId()) {
                    case R.id.upBtn:
                        action_up = false;
                        break;
                    case R.id.downBtn:
                        action_down = false;
                        break;
                    case R.id.leftBtn:
                        action_left = false;
                        break;
                    case R.id.rightBtn:
                        action_right = false;
                        break;
                }
                v.setPressed(false);
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }

    /*
     * Screen updates are initiated in onFinish which executes every REFRESH_INTERVAL milliseconds
     */
    private class ScreenRefreshTimer extends CountDownTimer
    {
        ScreenRefreshTimer(long millisInFuture, long countDownInterval)
        {
            super(millisInFuture, countDownInterval);
        }
        @Override
        public void onTick(long millisUntilFinished)
        {
        }

        @Override
        public void onFinish()
        {
            boolean flag_end;

            //Update the ball differently for each Input Method
            switch (inputType){
                case "Joystick":
                    flag_end =rb.updateBallPosition(angle,strength);
                    break;
                case "Button":
                    flag_end = rb.updateBallPosition(action_right, action_left,action_up,action_down);
                    break;
                default:
                    flag_end = rb.updateBallPosition(angle_left, strength_left, angle_right, strength_right);
                    break;
            }

            if (flag_end)
            {
                //If it is a trial
                if (experiment_mode==0){
                    Intent i = new Intent(getApplicationContext(), DemoTiltBallSetup.class);
                    startActivity(i);
                    onDestroy();
                    finish();
                    return;
                }

                //THe Experiment
                missed_lap_time[path_index] = rb.missed_time;
                accuracy[path_index] = rb.accuracy;
                trials_time[path_index] = rb.time_per_trial;
                miss_path[path_index] = rb.wallHits;

                x_positions[path_index] = rb.x_positions;
                y_positions[path_index] = rb.y_positions;
                t_positions[path_index] = rb.t_positions;

                best_times[path_index] = rb.get_best_time();

                path_index++;

                //If it was not the last Trial
                if (path_index<paths.length){
                    dialog.show();
                    rb.reset();
                    rb.configure(paths[path_index],gain, experiment_mode);
                    rb.config_path();
                }else{
                    //Last trial


                    //Bundle the stats
                    Bundle b = new Bundle();
                    b.putDoubleArray("in_path_time", accuracy);
                    b.putDoubleArray("lap_time", trials_time);
                    b.putDoubleArray("best_times", best_times);
                    b.putDoubleArray("missed_lap_time", missed_lap_time);
                    b.putIntArray("wall_hints", miss_path);
                    b.putInt("number_laps", 1);
                    b.putInt("group", group);
                    b.putString("inputType",inputType);
                    
                    b.putStringArray("x_pos",x_positions);
                    b.putStringArray("y_pos",y_positions);
                    b.putStringArray("t_pos",t_positions);

                    b.putFloat("rb_width",rb.width);
                    b.putFloat("rb_height",rb.height);



                    //Result Activity
                    Intent i = new Intent(getApplicationContext(), ResultsActivity.class);
                    i.putExtras(b);
                    startActivity(i);
                    finish();
                }

            }
            //will invalidate ball panel
            this.start();
        }

    }

}