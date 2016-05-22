package io.hengky.common;

import android.bluetooth.BluetoothSocket;

/**
 * Created by yip on 21/5/16.
 */
public class BaseBluetoothThread extends Thread {

    protected void onSocketConnected(BluetoothSocket socket) {
        DeviceManager.getInstance().add(new Device(socket));
    }

}