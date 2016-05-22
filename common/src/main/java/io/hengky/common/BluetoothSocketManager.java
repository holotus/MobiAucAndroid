package io.hengky.common;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by yip on 16/5/16.
 * <p/>
 * Maintain a list of Thread
 */
public class BluetoothSocketManager {

    private static final String LOG_TAG = BluetoothSocketManager.class.getSimpleName();
    public static final String SDP_NAME = "MobiAucBluetooth";
    public static final UUID SDP_UUID = UUID.fromString("53a4dd29-9eac-4556-9d24-c3e95b3c6f4b");

    AcceptThread mAcceptThread;
    final ArrayList<ConnectThread> mConnectThreadList = new ArrayList<>();

    private static BluetoothSocketManager ourInstance = new BluetoothSocketManager();

    public static BluetoothSocketManager getInstance() {
        return ourInstance;
    }

    private BluetoothSocketManager() {
//        Log.i(LOG_TAG, UUID.randomUUID().toString());
    }


    public synchronized void stopAll() {

        if (mAcceptThread != null) {
            mAcceptThread.closeSocket();
            mAcceptThread = null;
        }

        for (int i = 0; i < mConnectThreadList.size(); i++) {
            mConnectThreadList.get(i).closeSocket();
            mConnectThreadList.clear();
        }


        for (int i = 0; i < DeviceManager.getInstance().mDeviceList.size(); i++) {
            DeviceManager.getInstance().mDeviceList.get(i).closeSocket();
        }
    }

    public synchronized void startAccept() {

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    public void startConnect(BluetoothDevice device) {

//        if(mConnectThread!=null) {
//            mConnectThread.cancel();
//        }

//        mConnectThread = new ConnectThread(device, callFromSeller);
//        mConnectThread.start();

//        // TODO: clean up unused ConnectThread
        ConnectThread ct = new ConnectThread(device);
        mConnectThreadList.add(ct);
        ct.start();

//        new ConnectThread(device).start();
    }

}
