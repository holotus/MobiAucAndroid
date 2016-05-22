package io.hengky.common;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;

/**
 * Created by yip on 21/5/16.
 */
public class AcceptThread extends BaseBluetoothThread {

    private static final String LOG_TAG = AcceptThread.class.getSimpleName();

    private final BluetoothServerSocket mServerSocket;

    public AcceptThread() {

        // Use a temporary object that is later assigned to mmServerSocket, because mServerSocket is final
        BluetoothServerSocket tmp = null;

        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = BluetoothAdapter.getDefaultAdapter().listenUsingInsecureRfcommWithServiceRecord(BluetoothSocketManager.SDP_NAME, BluetoothSocketManager.SDP_UUID);
        } catch (IOException e) {
            Log.e(LOG_TAG, "", e);
        }

        mServerSocket = tmp;

    }

    public void run() {

        BluetoothSocket socket = null;

        // Keep listening until exception occurs or a socket is returned
        while (true) {
            try {
//                if (mServerSocket != null) {
                    socket = mServerSocket.accept();
//                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "", e);
                break;
            }

            // If a connection was accepted
            if (socket != null) {
                onSocketConnected(socket);
            }
        }
    }

    /**
     * Will cancel the listening socket, and cause the thread to finish
     */
    public void closeSocket() {
        try {
            mServerSocket.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "", e);
        }
    }

}
