package io.hengky.common;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
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

    static final String SDP_NAME = "MobiAucBluetooth";
    static final UUID SDP_UUID = UUID.fromString("53a4dd29-9eac-4556-9d24-c3e95b3c6f4b");

    BluetoothAdapter mBluetoothAdapter;
    AcceptThread mAcceptThread;
    ConnectThread mConnectThread;
    ConnectedThread mConnectedThread;

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
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
    }

    public void startConnect(BluetoothDevice device) {
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mServerSocket;

        public AcceptThread() {
//             Use a temporary object that is later assigned to mmServerSocket,
//             because mServerSocket is final
            BluetoothServerSocket tmp = null;

            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(SDP_NAME, SDP_UUID);
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

                    mConnectedThread = new ConnectedThread(socket);
                    mConnectedThread.start();

//                    try {
//                        mServerSocket.close();
//                    } catch (IOException e) {
//                        Log.e(LOG_TAG, "", e);
//                    }

//                    break;
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
                Log.e(LOG_TAG, "", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createInsecureRfcommSocketToServiceRecord(SDP_UUID);
            } catch (IOException e) {
                Log.e(LOG_TAG, "", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                Log.e(LOG_TAG, "", connectException);

                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(LOG_TAG, "", closeException);
                }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "", e);
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

    public void write(byte[] bytes) {
        if (mConnectedThread != null) {
            mConnectedThread.write(bytes);
        }
    }
}
