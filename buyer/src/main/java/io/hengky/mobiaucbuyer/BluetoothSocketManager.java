package io.hengky.mobiaucbuyer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by yip on 16/5/16.
 */
public class BluetoothSocketManager {
    private static final String LOG_TAG = BluetoothSocketManager.class.getSimpleName();

    static final String SDP_NAME = "MobiAuc - Buyer";
    static final UUID SDP_UUID = UUID.fromString("53a4dd29-9eac-4556-9d24-c3e95b3c6f4b");

    BluetoothAdapter mBluetoothAdapter;

    private static BluetoothSocketManager ourInstance = new BluetoothSocketManager();

    public static BluetoothSocketManager getInstance() {
        return ourInstance;
    }

    //    static final int REQUEST_ENABLE_BT = 1;


    private BluetoothSocketManager() {
//        Log.i(LOG_TAG, UUID.randomUUID().toString());

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

//        if (mBluetoothAdapter == null) {
//            Log.i(LOG_TAG, "Device does not support Bluetooth");
//        } else if (!mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        } else
//        {
//
//        }
    }

    public void startAccept() {
        new AcceptThread();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mServerSocket;

        public AcceptThread() {
//             Use a temporary object that is later assigned to mmServerSocket,
//             because mServerSocket is final
            BluetoothServerSocket tmp = null;

            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(SDP_NAME, SDP_UUID);
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
                    socket = mServerSocket.accept();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)

                    new ConnectedThread(socket);

                    try {
                        mServerSocket.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "", e);
                    }

                    break;
                }
            }
        }

        /**
         * Will cancel the listening socket, and cause the thread to finish
         */
        public void cancel() {
            try {
                mServerSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(LOG_TAG, "", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {

                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI activity
//                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
//                            .sendToTarget();

                    Log.i(LOG_TAG, mmSocket.getRemoteDevice().getName() + ": " + new String(buffer));

                } catch (IOException e) {
                    Log.e(LOG_TAG, "", e);
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(LOG_TAG, "", e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "", e);
            }
        }
    }
}
