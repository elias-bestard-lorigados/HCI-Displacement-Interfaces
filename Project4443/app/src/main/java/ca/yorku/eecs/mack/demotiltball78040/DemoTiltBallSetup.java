package ca.yorku.eecs.mack.demotiltball78040;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.util.Log;

public class DemoTiltBallSetup extends Activity implements AdapterView.OnItemSelectedListener {
//	final static String[] GAIN = { "Low", "Medium", "High" };


	final static String[] INPUT_TYPE = { "Tilt Sensor", "Joystick", "Button", "Dual Joystick" };

	final static String[] EXPERIMENT_MODE = { "Test", "Experiment" };

	final static String MYDEBUG = "MYDEBUG";

	// somewhat arbitrary mappings for gain by order of control
	final static int[] GAIN_ARG_VELOCITY_CONTROL = { 50, 100, 200 };
	final static String[] GROUP = { "1","2","3","4" };



	Spinner spinPathMode, spinGroup, spinMode;
//	Spinner spinGain;
	// called when the activity is first created
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		Log.i(MYDEBUG, "Got here! (DemoTiltBallSetup - onCreate");

		setContentView(R.layout.setup);


		spinPathMode = (Spinner) findViewById(R.id.paramPathType);
		ArrayAdapter<CharSequence> adapter1 = new ArrayAdapter<CharSequence>(this, R.layout.spinnerstyle, INPUT_TYPE);
		spinPathMode.setAdapter(adapter1);
		spinPathMode.setSelection(1); // free

		spinPathMode.setOnItemSelectedListener(this);

		spinMode = (Spinner) findViewById(R.id.parammode);
		ArrayAdapter<CharSequence> adapter6 = new ArrayAdapter<CharSequence>(this, R.layout.spinnerstyle, EXPERIMENT_MODE);
		spinMode.setAdapter(adapter6);
		spinMode.setSelection(1); // experimert


		spinGroup = (Spinner) findViewById(R.id.group_);
		ArrayAdapter<CharSequence> adapter7 = new ArrayAdapter<CharSequence>(this, R.layout.spinnerstyle, GROUP);
		spinGroup.setAdapter(adapter7);
		spinGroup.setSelection(0);
	}

	// called when the "OK" button is tapped
	public void clickOK(View view) 
	{

		// get user's choices... 

		// actual gain value depends on order of control
		int gain = GAIN_ARG_VELOCITY_CONTROL[1];

		String inputType = INPUT_TYPE[spinPathMode.getSelectedItemPosition()];

		int exp_mode = spinMode.getSelectedItemPosition();

		int group = spinGroup.getSelectedItemPosition()+1;

		// bundle up parameters to pass on to activity
		Bundle b = new Bundle();
		b.putInt("gain", gain);

		b.putString("inputType", inputType);

		b.putInt("experiment_mode",exp_mode);

		b.putInt("group",group);

		Intent i;
//"Tilt Sensor", "Joystic", "Button", "Dual Joysitck"
//		 start experiment activity


//		i = new Intent(getApplicationContext(), ResultsActivity.class);
//		startActivity(i);

		switch (inputType){
			case "Tilt Sensor":
				i = new Intent(getApplicationContext(), DemoTiltBall78040Activity.class);
				break;
			default:
				i = new Intent(getApplicationContext(), InputsActivity.class);
				break;
		}
		i.putExtras(b);
		startActivity(i);

//		 comment out (return to setup after clicking BACK in main activity
		finish();

	}

	/** Called when the "Exit" button is pressed. */
	public void clickExit(View view) 
	{
		super.onDestroy(); // cleanup
		this.finish(); // terminate
	}

	@Override // add this line
	public void onItemSelected(AdapterView<?>parent, View v, int position, long id) {

		if(position==0)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			builder.setMessage("Please mind the angle of your device before continue since you selected Tilt Sensor setting for motion control")
					.setTitle("Warning");

			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// User clicked OK button
				}
			});
			builder.setCancelable(false);
			Dialog dialog = builder.create();
			dialog.show();
		}

	}

	@Override // add this line
	public void onNothingSelected(AdapterView<?> arg0) {
	}
}
