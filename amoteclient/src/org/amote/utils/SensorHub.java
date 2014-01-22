/**
 * Copyright (C) 2009 Aisino Corporation Inc.
 *
 * No.18A, Xingshikou street, Haidian District,Beijing
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information of 
 * Aisino Corporation Inc. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in 
 * accordance with the terms of the license agreement you entered into
 * with Aisino.
 */

package org.amote.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.gmote.common.Protocol.UdpPacketTypes;
import org.gmote.common.utils.TypeUtils;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;
import org.amote.utils.DatagramUtils.SensorSendingThread;

/**
 * SensorUtil class: get sensor info of the client, send the info packet to
 * server with UDP. Modify by weidawei 2012-3-14
 *
 * @version 1.0 06 Mar 2012
 * @author gaoqi
 */
public final class SensorHub {
    private final static boolean DEBUG = true;

    /**
     * Constant Tag for debug.
     */
    private static final String TAG = "SensorUtil";

    /**
     * Constant to specify the length of datagram.
     */
    private static final int UDP_DATA_LEN = 22;

    /**
     * Constant to specify the long length / byte length.
     */
    private static final int LONG_D_BYTE = 8;

    /**
     * Constant to specify the start index of values in the bytes.
     */
    private static final int INDEX_IN_BYTES = LONG_D_BYTE + 2;

    /**
     * Constant to specify the float length / byte length.
     */
    private static final int FLOAT_D_BYTE = 4;

    /**
     *Constant to specify the length of sensor packet.
     */
    private static final int PACKET_LEN = 5;

    /**
     *
     */
    private static final int SENSOR_VAL_LEN = 3;

    /**
     * the singleton instance of the class.
     */
    private static SensorHub mInstance;

    /**
     * To manage to sensor.
     */
    private SensorManager mSensorManager;

    /**
     * value to save the the sensor.
     */
    private Sensor mSensor;

    /**
     * Util class to send datagram.
     */
    private DatagramUtils mUtils;

    /**
     * thread to send the udp data.
     */
    private Thread mThread;

    /**
     * the map to save the sensor enable/disable state.
     */
    private HashMap<Integer, Boolean> mSensorState;

    /**
     * value to specify is thread is working.
     */
    private boolean mThreadLoop = false;

    /**
     * value for test delays.
     */
    private float mTestCount = 0;

    /**
     * Constructs an instance from the specified context, then add support
     * sensor type to map.
     *
     * @param context the application context.
     */
    private SensorHub(final Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mUtils = DatagramUtils.instance();
        final SensorSendingThread sst = mUtils.new SensorSendingThread();
        mThread = new Thread(sst);
        final List<Sensor> list = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        mSensorState = new HashMap<Integer, Boolean>();
        for (Sensor s : list) {
            if (s.getType() == Sensor.TYPE_GRAVITY
                    || s.getType() == Sensor.TYPE_LINEAR_ACCELERATION
                    || s.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                continue;
            }
            mSensorState.put(s.getType(), false);
        }
    }

    /**
     * Constructs singleton instance.
     *
     * @param context the application context.
     * @return return the singleton instance.
     */
    public static synchronized  SensorHub getInstance(final Context context) {
        Log.d(TAG, "==========getInstance=" + (mInstance == null));
        if (null == mInstance) {
            mInstance = new SensorHub(context);
        }
        return mInstance;
    }

    /**
     * @return return the singleton instance.
     */
    public static synchronized SensorHub getInstance() {
        return mInstance;
    }

    /**
     * @return return the singleton instance.
     */
    private  void setInstance() {
         mInstance = null;
    }

    /**
     * stop sending dataThread.
     */
    public void stopSendingDataThread(){
        mUtils.stopThread();
        setInstance();
    }
    /**
     * Start or stop the thread to send data.
     *
     * @param choose true to start to send data , false to
     *            stop to send data and .
     */
    public void startOrStopSendData(final boolean choose) {
        mThreadLoop = choose;
        try {
            /**
             * first time start the thread.
             */
            if (mThread != null && !mThread.isAlive() && mThreadLoop) {
                Log.d(TAG, " ========> to start thread");
                mThread.start();
            }

        } catch (final IllegalThreadStateException e) {
            Log.d(TAG, "================>start mThread failed");
        }
    }

    /**
     * Start or stop the thread to send data.
     *
     * @param choose true to start to send data , false to
     *            stop to send data and .
     */
    public void enableOdisableSensor(boolean choose){

        Log.d(TAG, " ========> to enableOdisableSensor mThread != null="+ (mThread != null));
        Log.d(TAG, " ========> to enableOdisableSensor mThread.isAlive()="+mThread.isAlive());

        if(mThread != null && mThread.isAlive()){
            /* to enable and disable sensor */
            final Set<Integer> kset = mSensorState.keySet();
            for (Integer key : kset) {
                if (mSensorState.get(key)) {
                    sensorActivate(key, choose);
                }
            }
        }
    }

    /**
     * Used to remember our sensor en/dis state.
     * @param type the type of the sensor
     * @param state the state of the sensor
     */
    public void saveSensorState(final int type, final boolean state) {
        Integer local_type = new Integer(type);
        if (mSensorState.containsKey(local_type)) {
            if (state != mSensorState.get(local_type)) {
                mSensorState.put(local_type, new Boolean(state));
                if (mThreadLoop) {
                    sensorActivate(type, state);
                }
            }
        } else {
            Log.d(TAG, "!! server want to request the type client is not support!");
            return;
        }
    }

    /**
     * Used to remember our sensor en/dis state.
     * @param map the state map of
     */
    public void saveSensorState(final HashMap<Integer, Boolean> map) {
        for (Integer key : map.keySet()) {
            saveSensorState(key, map.get(key));
        }
    }

    /**
     * Active or deactive the sensor of specified type.
     *
     * @param sensorType supported sensor type.
     * @param enable is to register or unregister the sensor of this type.
     * @return the error type.
     */
    private ErrorType sensorActivate(final int sensorType, final boolean enable) {
        mSensor = mSensorManager.getDefaultSensor(sensorType);
        if (null == mSensor) {
            return ErrorType.NO_THIS_SENSOR;
        }

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_MAGNETIC_FIELD:
            case Sensor.TYPE_ORIENTATION:
            case Sensor.TYPE_GYROSCOPE:
            case Sensor.TYPE_LIGHT:
            case Sensor.TYPE_PRESSURE:
            case Sensor.TYPE_TEMPERATURE:
            case Sensor.TYPE_PROXIMITY:
                if (enable) {
                    mSensorManager.registerListener(mSensorListener, mSensor,
                            SensorManager.SENSOR_DELAY_FASTEST);
                } else {
                    mSensorManager.unregisterListener(mSensorListener);
                }
                break;
            default:
                return ErrorType.NO_THIS_SENSOR;
        }
        return ErrorType.NO_ERROR;
    }
    private long pretime =0;
    /**
     * the listener to listen the change of sensor.
     */
    private final SensorEventListener mSensorListener = new SensorEventListener() {

        public void onSensorChanged(final SensorEvent event) {

            final int type = event.sensor.getType();
            if(!mSensorState.get(new Integer(type))){
                return;
            }
            long current = SystemClock.elapsedRealtime();
            if (DEBUG) Log.d(TAG, "========>"+(current -pretime)  +" event-time"+event.timestamp);

            if(current - pretime <=30){
                return;
            }
            pretime = current;

            // TODO: we have to distinguish the values length
            /* make a event packet */
//            mTestCount++;
//
            if (DEBUG)
                Log.d(TAG, "-----type=" + ((byte) type - 1) + "-----values = " + mTestCount
                        + "-------" + event.values[1] + " " + event.values[2]);
//            event.values[0] = mTestCount;

            if (PACKET_LEN == addElement((byte) type, event.values, event.timestamp)) {
                resetIndex();
                final byte[] data = convertDataToBytes(types, timestamps, values, PACKET_LEN);
                mUtils.makeDatagramPacket(data);
            }
        };

        @Override
        public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
        }
    };

    /**
     * Convert sensor data to bytes.
     *
     * @param types the value to specify the udp packet type.
     * @param timestamps the timestamp of sensor event.
     * @param vals the values of sensor event.
     * @param len len of packet to send.
     * @return the byte array to be send now.
     */
    private synchronized byte[] convertDataToBytes(final byte[] types, final long[] timestamps,
            final float[] vals, final int len) {

        final byte[] arrbytes = new byte[UDP_DATA_LEN * len];
        for (int l = 0; l < len; l++) {
            final int offset = UDP_DATA_LEN * l;
            arrbytes[offset + 0] = UdpPacketTypes.SENSOR_EVENT.getId();
            arrbytes[offset + 1] = types[l];

            final byte[] ts = TypeUtils.longToByte(timestamps[l]);
            for (int i = 0; i < LONG_D_BYTE; i++) {
                arrbytes[offset + 2 + i] = ts[i];
            }
            byte[] fbs;
            for (int j = 0; j < SENSOR_VAL_LEN; j++) {
                fbs = TypeUtils.float2Byte(vals[SENSOR_VAL_LEN * l + j]);
                for (int i = 0; i < FLOAT_D_BYTE; i++) {
                    arrbytes[offset + INDEX_IN_BYTES + i + FLOAT_D_BYTE * j] = fbs[i];
                }
            }
        }
        return arrbytes;
    }

    /**
     * @author weidawei this class is used the response the error of active
     *         sensor.
     */
    public enum ErrorType {
        /**
         * this sensor can use.
         */
        NO_ERROR,
        /**
         * there is no this type sensor.
         */
        NO_THIS_SENSOR,
    }

    /**
     * array used to save sensor event time.
     */
    private long[] timestamps = new long[PACKET_LEN];

    /**
     * array used to save sensor values.
     */
    private float[] values = new float[SENSOR_VAL_LEN * PACKET_LEN];

    /**
     * array used to save sensor types.
     */
    private byte[] types = new byte[PACKET_LEN];

    /**
     * record the array save index.
     */
    private int index = 0;

    /**
     * @param type specify the type of sensor.
     * @param val values of sensor event.
     * @param time time of the event.
     * @return the offset of array.
     */
    private synchronized int addElement(final byte type , final float[] val , final long time) {
        types[index] = (byte) (type - 1);
        timestamps[index] = time;
        for (int i = 0; i < val.length; i++) {
            values[SENSOR_VAL_LEN * index + i] = val[i];
        }
        index++;
        return index;
    }

    /**
     * reset the offset of array to zero.
     */
    private synchronized void resetIndex() {
        index = 0;
    }

}
