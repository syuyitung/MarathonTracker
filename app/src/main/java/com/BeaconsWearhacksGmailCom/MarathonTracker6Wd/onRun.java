package com.BeaconsWearhacksGmailCom.MarathonTracker6Wd;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.BeaconsWearhacksGmailCom.MarathonTracker6Wd.estimote.BeaconID;
import com.BeaconsWearhacksGmailCom.MarathonTracker6Wd.estimote.BeaconStats;
import com.BeaconsWearhacksGmailCom.MarathonTracker6Wd.estimote.Database;
import com.BeaconsWearhacksGmailCom.MarathonTracker6Wd.estimote.EstimoteCloudBeaconDetails;
import com.BeaconsWearhacksGmailCom.MarathonTracker6Wd.estimote.EstimoteCloudBeaconDetailsFactory;
import com.BeaconsWearhacksGmailCom.MarathonTracker6Wd.estimote.ProximityContentManager;
import com.estimote.sdk.SystemRequirementsChecker;
import com.estimote.sdk.cloud.model.Color;
import com.github.lzyzsd.circleprogress.DonutProgress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class onRun extends AppCompatActivity implements LocationListener, SensorEventListener {
    TextToSpeech t1;
    EditText ed1;
    private static final String TAG = "onRun";
    private TextView myTimer;
    private ImageButton stopButton;
    private ImageButton alert;
    LocationManager lm;
    LocationListener ll;
    public Location location;
    private float speed;
    private float maxSpeed;
    Location previousLocation = null;
    private SensorManager sensorManager;
    private int stepsTotal;
    private int stepsThisSection;
    private List<Integer> stepsAllSections = new ArrayList<Integer>();
    private int offset;
    boolean start = true;
    boolean activityRunning;
    private PopupWindow popup;
    private boolean check = true;
    private RelativeLayout layout;
    private RelativeLayout mainLayout;
    private LayoutParams params;
    private TextView txt;

    private static final Map<Color, Integer> BACKGROUND_COLORS = new HashMap<>();
    private long millisecs;

    private double lapTime;
    private double totalTime;

    Sensor countSensor;
    static {
        BACKGROUND_COLORS.put(Color.ICY_MARSHMALLOW, android.graphics.Color.rgb(109, 170, 199));
        BACKGROUND_COLORS.put(Color.BLUEBERRY_PIE, android.graphics.Color.rgb(98, 84, 158));
        BACKGROUND_COLORS.put(Color.MINT_COCKTAIL, android.graphics.Color.rgb(155, 186, 160));
    }

    private static final int BACKGROUND_COLOR_NEUTRAL = android.graphics.Color.rgb(160, 169, 172);

    private ProximityContentManager proximityContentManager;

    private Context mcontext;
    private Chronometer chronometer;
    private Thread thread;
    private DonutProgress donutProgress;

    private Database db;
    private TextView tv;
    private Button but;


    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.during_run);

        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        try {
            location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } catch (SecurityException e) {

        }

        this.onLocationChanged(null);

        mcontext = this;
        myTimer = (TextView) findViewById(R.id.textView3);
        stopButton = (ImageButton) findViewById(R.id.stopButton);
        alert = (ImageButton) findViewById(R.id.alertButton);
        popup = new PopupWindow(this);
        layout = new RelativeLayout(this);
        LayoutParams params;
        tv = new TextView(this);
        but = new Button(this);
        mainLayout = new RelativeLayout(this);
        db = new Database(this);
        database = db.getWritableDatabase();
        new inBack().execute(0);

        if (chronometer == null) {

            chronometer = new Chronometer(mcontext);
            thread = new Thread(chronometer);
            millisecs = SystemClock.currentThreadTimeMillis();
            thread.start();
            chronometer.start();
        }
    }




    @Override
    public void onLocationChanged(Location currentLocation) {
        txt = (TextView) this.findViewById(R.id.textView5);
        if (txt != null) {
            if (currentLocation == null) {
                speed = 0;
                txt.setText("-.- m/s");
            } else {
                speed = currentLocation.getSpeed();
                txt.setText(speed + "m/s");
            }

        }
    }

    private class inBack extends AsyncTask <Integer , Void, Boolean > {
        protected Boolean doInBackground(Integer ... params) {
            for (int i = 1; i < 11; i++) {
                double rand = Math.random();
                //db.insertDataHistorical(database, i + rand * 0.7 - 0.35, rand * 200, rand * 130, rand * 7.4, Chronometer.setFromSec((long) rand * 1600), i);
                if (isCancelled()) break;
            }
            return true;
        }
        protected void onPostExecute(Boolean result) {
            Log.d("Done","d");
        }

        protected void onPreExecute() {}

        protected void onProgressUpdate(Void... values) {}
    }

    @Override
    protected void onStart() {
        super.onStart();

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chronometer != null) {
                    chronometer.stop();
                    thread.interrupt();
                    thread = null;
                    chronometer = null;
                    startActivity(new Intent(onRun.this, historyPage.class));
                }
                proximityContentManager.destroy();
                // if (db.writeToHistoric())
                startActivity(new Intent(onRun.this, historyPage.class));
                //    else
                Log.e("ERROR", "could not write to db");
            }
        });

        alert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(check) {
                    popup.showAtLocation(layout, Gravity.BOTTOM, 10, 10);
                    popup.update(50,50,300,80);
                    check = false;
                } else {
                    popup.dismiss();
                    check = true;
                }
            }
        });

/*
        donutProgress = (DonutProgress) findViewById(R.id.donut_progress);
        //donutProgress.setM*//*
        params = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        //layout.setOrientation(LinearLayout.VERTICAL);
        tv.setText("Are you sure?");
        layout.addView(tv, params);
        popup.setContentView(layout);
        // popUp.showAtLocation(layout, Gravity.BOTTOM, 10, 10);
        mainLayout.addView(but, params);
        setContentView(mainLayout);
        *//**//*
        lapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });ax(42);*/


                t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });

        if(this.activityRunning) {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            this.onResume();
        }
        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        try {
            location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } catch (SecurityException e) {
        }
        this.onLocationChanged(null);

        proximityContentManager = new ProximityContentManager(this,
                Arrays.asList(
                        new BeaconID("B9407F30-F5F8-466E-AFF9-25556B57FE6D", 23105, 37595)),
                new EstimoteCloudBeaconDetailsFactory());
        proximityContentManager.setListener(new ProximityContentManager.Listener() {
            @Override
            public void onContentChanged(Object content) {

                if (content != null) {
                    EstimoteCloudBeaconDetails beaconDetails = (EstimoteCloudBeaconDetails) content;

                    double lapTime = (SystemClock.currentThreadTimeMillis() - millisecs) / 6000.0;


                    BeaconStats bs = new BeaconStats();
                    //Read beacon info
                    bs = bs.grabById(beaconDetails.getId());
                    speakOut("You have ran" +  bs.getMileMarker() + " miles, Your average speed is this split was " + (int)getAverageSpeed(bs.getMileMarker() - 1, (float)lapTime ) + "kilometers per hour");

                    //Write to d String distanceTravelled, String caloriesBurned, String stepCount, String maxSpeed, String timeTaken, String section
                    //  if(db.contains

                    if(!db.insertData(database, bs.getMileMarker(), 10, getSteps(bs.getMileMarker()),maxSpeed, Double.toString(lapTime), bs.getMileMarker()))
                        Log.e("ERROR", "COULD NOT POST");

                    //lapTimes.add(lap);

                }
            }
        });

    }

    public void updateTime(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                myTimer.setText("Time: " + text);
            }
        });
    }

    // TODO move to util class?
    private float getAverageSpeed(float distance, float timeTaken) {
        //float minutes = timeTaken/60;
        //float hours = minutes/60;
        float speed = 0;
        if(distance > 0) {
            float distancePerSecond = timeTaken > 0 ? distance/timeTaken : 0;
            float distancePerMinute = distancePerSecond*60;
            float distancePerHour = distancePerMinute*60;
            speed = distancePerHour > 0 ? (distancePerHour/1000) : 0;
        }
        return speed;
    }

    @Override
    protected void onResume() {
        super.onResume();


        if (!SystemRequirementsChecker.checkWithDefaultDialogs(this)) {
            Log.e(TAG, "Can't scan for beacons, some pre-conditions were not met");
            Log.e(TAG, "Read more about what's required at: http://estimote.github.io/Android-SDK/JavaDocs/com/estimote/sdk/SystemRequirementsChecker.html");
            Log.e(TAG, "If this is fixable, you should see a popup on the app's screen right now, asking to enable what's necessary");
        } else {
            Log.d(TAG, "Starting ProximityContentManager content updates");
            proximityContentManager.startContentUpdates();
        }
        if(this.activityRunning) {
            countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

            chronometer.start();

        }        if (countSensor != null) {
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Stopping ProximityContentManager content updates");
        proximityContentManager.stopContentUpdates();
        activityRunning = false;

    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        stepsThisSection = (int)event.values[0];

        if (start){
            offset = stepsThisSection;
            start = false;
        }
    }
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // AFTER THE WHOLE TRIP IS DONE
    public int getTotalSteps(){
        return stepsTotal;
    }

    // DURING THE TRIP
    public int getCurrentSteps(){
        int currentSteps = 0;
        for (Integer stepsPerSection: stepsAllSections){
            currentSteps += stepsPerSection;
        }
        currentSteps += stepsThisSection;
        return currentSteps;
    }

    // GET SPECIFIC SECTION
    public int getSteps(int section){
        if(stepsAllSections.size() >= section)
            return stepsAllSections.get(section);
        else
            return 100;
    }

    public void passCheckPoint(){
        stepsAllSections.add(stepsThisSection);
        stepsTotal += stepsThisSection;
        stepsThisSection = 0;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        proximityContentManager.destroy();
        database.close();
        db.close();
        //  lm.clearTestProviderLocation();
    }


    private void speakOut(String text) {

        final AudioManager audioManager =
                (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                //    audioManager.abandonAudioFocus(afChangeListener);
            }
        };

        int result = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            t1.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        if(!t1.isSpeaking())
            audioManager.abandonAudioFocus(afChangeListener);
    }

    @Override
    public void onProviderDisabled(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        // TODO Auto-generated method stub

    }
}
