package ca.yorku.eecs.mack.demotiltball78040;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

/**
 * Demo_Android - with modifications by...
 *
 * Login ID - <elias39>
 * Student ID - <217978040>
 * Last name - <Bestard Lorigados>
 * First name(s) - <Elias>
 */
public class DemoTiltBall78040Activity extends Activity implements SensorEventListener
{
    final static String MYDEBUG = "MYDEBUG"; // for Log.i messages

    final static int REFRESH_INTERVAL = 20; // milliseconds (screen refreshes @ 50 Hz)

    // int constants to setup a mode (see DemoTiltMeter API for discussion)
    final static int ORIENTATION = 0;
    final static int ACCELEROMETER_ONLY = 1;
    final static int ACCELEROMETER_AND_MAGNETIC_FIELD = 2;
    final float RADIANS_TO_DEGREES = 57.2957795f;

    /*
     * Below are the alpha values for the low-pass filter. The four values are for the slowest
     * (NORMAL) to fastest (FASTEST) sampling rates, respectively. These values were determined by
     * trial and error. There is a trade-off. Generally, lower values produce smooth but sluggish
     * responses, while higher values produced jerky but fast responses.
     *
     * Furthermore, there is a difference by device, particularly for the FASTEST setting. For
     * example, the FASTEST sample rate is about 200 Hz on a Nexus 4 but only about 100 Hz on a
     * Samsung Galaxy Tab 10.1.
     *
     * Fiddle with these, as necessary.
     */
    final float[] ALPHA_VELOCITY = {0.99f, 0.80f, 0.40f, 0.15f};
    float alpha;

    RollingBallPanel rb;
    int sensorMode, group;
    float[] accValues = new float[3];
    float[] magValues = new float[3];
    float x, y, z, pitch, roll;

    // parameters from the Setup dialog
    String[] paths = new String[]{"Maze2","Square", "Maze","Circle", "Maze3"};
//    String[] paths = new String[]{"Maze"};

    int gain, path_index;
    int experiment_mode;
    int defaultOrientation;
    ScreenRefreshTimer refreshScreen;
    private SensorManager sm;
    private Sensor sA, sM, sO;

    AlertDialog dialog;

    // Statistics
    double[] trials_time,accuracy,missed_lap_time, best_times;
    int[] miss_path;

    String[] x_positions;
    String[] y_positions;
    String[] t_positions;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Log.i(MYDEBUG, "Got here! (DemoTiltBall78040Activity - onCreate");
        
        setContentView(R.layout.main);

        // get parameters selected by user from setup dialog
        Bundle b = getIntent().getExtras();
        gain = b.getInt("gain");
        group = b.getInt("group");
        experiment_mode = b.getInt("experiment_mode");
//        pathType = b.getString("pathType");

//        Initializing Statistics
        path_index =0;
        accuracy = new double[paths.length];
        trials_time = new double[paths.length];
        miss_path = new int[paths.length];
        best_times = new double[paths.length];


        x_positions= new String[paths.length];
        missed_lap_time= new double[paths.length];

        y_positions= new String[paths.length];
        t_positions= new String[paths.length];

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Please mind the angle of your device because this setting uses sensors for motion. Click OK to continue to the next step")
                .setTitle(R.string.dialog_title);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        builder.setCancelable(false);
        dialog = builder.create();


        // set alpha for low-pass filter (based on sampling rate and order of control)
        alpha = ALPHA_VELOCITY[2]; // for GAME sampling rate

        // get this device's default orientation
        defaultOrientation = getDefaultDeviceOrientation();

        // force the UI to appear in the device's default orientation (and stay that way)
        if (defaultOrientation == Configuration.ORIENTATION_LANDSCAPE)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // configure rolling ball panel, as per setup parameters
        rb = (RollingBallPanel)findViewById(R.id.rollingballpanel);
        rb.configure(paths[path_index], gain,experiment_mode);
        rb.config_path();
        // get sensors
        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        sO = sm.getDefaultSensor(Sensor.TYPE_ORIENTATION); // supported on many devices
        sA = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // supported on most devices
        sM = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD); // null on many devices

        // setup the sensor mode (see API for discussion)
        if (sO != null)
        {
            sensorMode = ORIENTATION;
            sA = null;
            sM = null;
            Log.i(MYDEBUG, "Sensor mode: ORIENTATION");
        } else if (sA != null && sM != null)
        {
            sensorMode = ACCELEROMETER_AND_MAGNETIC_FIELD;
            Log.i(MYDEBUG, "Sensor mode: ACCELEROMETER_AND_MAGNETIC_FIELD");
        } else if (sA != null)
        {
            sensorMode = ACCELEROMETER_ONLY;
            Log.i(MYDEBUG, "Sensor mode: ACCELEROMETER_ONLY");
        } else
        {
            Log.i(MYDEBUG, "Can't run demo.  Requires Orientation sensor or Accelerometer");
            this.finish();
        }

        // on my umidigi device, orientation doesn't seem to work very well, switch mode
//        sensorMode = ACCELEROMETER_AND_MAGNETIC_FIELD;
//        sA = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // supported on most devices
//        sM = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD); // null on many devices

        // NOTE: sensor listeners are registered in onResume

        // setup the screen refresh timer (updates every REFRESH_INTERVAL milliseconds)
        refreshScreen = new ScreenRefreshTimer(REFRESH_INTERVAL, REFRESH_INTERVAL);
        refreshScreen.start();
    }

    /*
     * Get the default orientation of the device. This is needed to correctly map the sensor data
     * for pitch and roll (see onSensorChanged). See...
     *
     * http://stackoverflow.com/questions/4553650/how-to-check-device-natural-default-orientation-on-
     * android-i-e-get-landscape
     */
    public int getDefaultDeviceOrientation()
    {
        WindowManager windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        Configuration config = getResources().getConfiguration();
        int rotation = windowManager.getDefaultDisplay().getRotation();

        if (((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && config.orientation ==
                Configuration.ORIENTATION_LANDSCAPE)
                || ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && config.orientation ==
                Configuration.ORIENTATION_PORTRAIT))
            return Configuration.ORIENTATION_LANDSCAPE;
        else
            return Configuration.ORIENTATION_PORTRAIT;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        sm.registerListener(this, sO, SensorManager.SENSOR_DELAY_GAME); // good enough!
        sm.registerListener(this, sA, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(this, sM, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        sm.unregisterListener(this);
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        // not needed, but we need to provide an implementation anyway
    }

    @Override
    public void onSensorChanged(SensorEvent se)
    {
        // =======================================================
        // DETERMINE DEVICE PITCH AND ROLL (VARIES BY SENSOR MODE)
        // =======================================================

        switch (sensorMode)
        {
            // ---------------------------------------------------------------------------------------------
            case ORIENTATION:
                pitch = se.values[1];
                roll = se.values[2];
                break;

            // ---------------------------------------------------------------------------------------------
            case ACCELEROMETER_AND_MAGNETIC_FIELD:
                // smooth the sensor values using a low-pass filter
                if (se.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                    accValues = lowPass(se.values.clone(), accValues, alpha); // filtered
                if (se.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                    magValues = lowPass(se.values.clone(), magValues, alpha); // filtered

                if (accValues != null && magValues != null)
                {
                    // compute pitch and roll
                    float R[] = new float[9];
                    float I[] = new float[9];
                    boolean success = SensorManager.getRotationMatrix(R, I, accValues, magValues);
                    if (success) // see SensorManager API
                    {
                        float[] orientation = new float[3];
                        SensorManager.getOrientation(R, orientation); // see getOrientation API
                        pitch = orientation[1] * RADIANS_TO_DEGREES;
                        roll = -orientation[2] * RADIANS_TO_DEGREES;
                    }
                }
                break;

            // ---------------------------------------------------------------------------------------------
            case ACCELEROMETER_ONLY:

				/*
                 * Use this mode if the device has an accelerometer but no magnetic field sensor and
				 * no orientation sensor (e.g., HTC Desire C, Asus MeMOPad). This algorithm doesn't
				 * work quite as well, unfortunately. See...
				 * 
				 * http://www.hobbytronics.co.uk/accelerometer-info
				 */

                // smooth the sensor values using a low-pass filter
                if (se.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                    accValues = lowPass(se.values.clone(), accValues, alpha);

                x = accValues[0];
                y = accValues[1];
                z = accValues[2];
                pitch = -(float)Math.atan(y / Math.sqrt(x * x + z * z)) * RADIANS_TO_DEGREES;
                roll = (float)Math.atan(x / Math.sqrt(y * y + z * z)) * RADIANS_TO_DEGREES;
                break;
        }
    }

    /*
     * Low pass filter. The algorithm requires tracking only two numbers - the prior number and the
     * new number. There is a time constant "alpha" which determines the amount of smoothing. Alpha
     * is like a "weight" or "momentum". It determines the effect of the new value on the current
     * smoothed value. A lower alpha means more smoothing.
     *
     * NOTE: 0 <= alpha <= 1.
     *
     * See...
     *
     * http://blog.thomnichols.org/2011/08/smoothing-sensor-data-with-a-low-pass-filter
     */
    protected float[] lowPass(float[] input, float[] output, float alpha)
    {
        for (int i = 0; i < input.length; i++)
            output[i] = output[i] + alpha * (input[i] - output[i]);
        return output;
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
            float tiltMagnitude = (float)Math.sqrt(pitch * pitch + roll * roll);
            float tiltAngle = tiltMagnitude == 0f ? 0f : (float)Math.asin(roll / tiltMagnitude) * RADIANS_TO_DEGREES;

            if (pitch > 0 && roll > 0)
                tiltAngle = 360f - tiltAngle;
            else if (pitch > 0 && roll < 0)
                tiltAngle = -tiltAngle;
            else if (pitch < 0 && roll > 0)
                tiltAngle = tiltAngle + 180f;
            else if (pitch < 0 && roll < 0)
                tiltAngle = tiltAngle + 180f;

            if (rb.updateBallPosition(pitch, roll, tiltAngle, tiltMagnitude))
            {
                //If it is a trial
                if (experiment_mode==0){
                    Intent i = new Intent(getApplicationContext(), DemoTiltBallSetup.class);
                    startActivity(i);
                    onDestroy();
                    finish();
                    return;
                }

                accuracy[path_index]=rb.accuracy;
                missed_lap_time[path_index] = rb.missed_time;
                trials_time[path_index]=rb.time_per_trial;
                miss_path[path_index]=rb.wallHits;

                x_positions[path_index]=rb.x_positions;
                y_positions[path_index]=rb.y_positions;
                t_positions[path_index]=rb.t_positions;

                best_times[path_index] = rb.get_best_time();

                path_index++;


                if (path_index<paths.length){
                    dialog.show();
                    rb.reset();
                    rb.configure(paths[path_index],gain, experiment_mode);
                    rb.config_path();
                }else{
                    Bundle b = new Bundle();
                    b.putDoubleArray("in_path_time", accuracy);
                    b.putDoubleArray("missed_lap_time", missed_lap_time);
                    b.putDoubleArray("lap_time", trials_time);
                    b.putDoubleArray("best_times", best_times);
                    b.putIntArray("wall_hints", miss_path);
                    b.putInt("number_laps", 1);
                    b.putInt("group", group);
                    b.putString("inputType","Tilt Sensor");

                    b.putStringArray("x_pos",x_positions);
                    b.putStringArray("y_pos",y_positions);
                    b.putStringArray("t_pos",t_positions);

                    b.putFloat("rb_width",rb.width);
                    b.putFloat("rb_height",rb.height);

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