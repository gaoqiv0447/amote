package com.sensor;

import java.util.List;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class SensorsTest extends Activity {
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private boolean mRegisteredSensor;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        for(Sensor s:sensors){
            Log.d("SensorList", "--------->type="+s.getName() + " "+s.getType());
        }
        if (sensors.size() > 0) {
            mSensor = sensors.get(0);

            mRegisteredSensor = mSensorManager.registerListener(
                            mLightListener,
                            mSensor,
                            SensorManager.SENSOR_DELAY_FASTEST);
        }

    }
    SensorEventListener mLightListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(final SensorEvent event) {
            Log.d("SensorList", "------>event.type="+event.sensor.getType() +" values="+event.values[0] + " " +event.values[1] + " " +event.values[2] );
        }

        @Override
        public void onAccuracyChanged(final Sensor arg0, final int arg1) {

        }
    };

}