package io.hengky.common;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * Created by yip on 21/5/16.
 */
public class BaseBluetoothThread extends Thread {

    private static final String LOG_TAG = BaseBluetoothThread.class.getSimpleName();


    long time;
    public BaseBluetoothThread(){
        time = System.currentTimeMillis();
    }

    protected void onSocketConnected(BluetoothSocket socket) {
        Device newDevice = new Device(socket);
        Log.i(LOG_TAG, "onSocketConnected: " + (System.currentTimeMillis() - time) +" " + newDevice.deviceName.get());
        DeviceManager.getInstance().add(newDevice);
    }

}