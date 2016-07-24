package io.hengky.common;

import android.bluetooth.BluetoothSocket;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.util.Log;

import com.google.common.primitives.Bytes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Created by yip on 17/5/16.
 * <p>
 * Device is Thread.
 */
public class Device extends Thread {

    private static final String LOG_TAG = Device.class.getSimpleName();

    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    private final Charset CHARSET = Charset.forName("US-ASCII");

    //    /**
//     * 0 = not_init, 1 = inited_isseller, 2 = inited_price
//     */
//    public final ObservableInt status = new ObservableInt();
    public final ObservableBoolean isSeller = new ObservableBoolean();
    public final ObservableInt price = new ObservableInt();
    public final ObservableBoolean isTraded = new ObservableBoolean();
    public final ObservableField<String> deviceName = new ObservableField<>();

    public final ObservableInt data = new ObservableInt();

    protected Device() {
        // used by special version of Device (ThisDevice, TestDevice) which is not a threat
        mmSocket = null;
        mmInStream = null;
        mmOutStream = null;
    }

    public Device(BluetoothSocket socket) {


        deviceName.set(socket.getRemoteDevice().getName());

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
            OnDropped();
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;

        // start reading
        start();

    }

    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytesRead; // bytes returned from read()
        byte[] unconsumedBytes = new byte[]{};

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {

                // Read from the InputStream
                bytesRead = mmInStream.read(buffer);
                unconsumedBytes = Bytes.concat(unconsumedBytes, subBytes(buffer, 0, bytesRead));
                unconsumedBytes = consume(unconsumedBytes);
//                String message = new String(buffer, CHARSET);
//                Log.i(LOG_TAG, mmSocket.getRemoteDevice().getName() + ": " + new String(buffer));

            } catch (IOException e) {
                Log.e(LOG_TAG, "", e);
                OnDropped();
                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    private void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
            Log.e(LOG_TAG, "", e);
            OnDropped();
        }
    }

    public String getMacAddress() {
        return this.mmSocket.getRemoteDevice().getAddress();
    }

    public void closeSocket() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "", e);
        }
    }

    void OnDropped() {
        DeviceManager.getInstance().remove(this);
    }


//    public void write(String message) {
//        write(message.getBytes(CHARSET));
//    }

    static final int BYTE_BEGIN = 9999;
    static final int BYTE_COMMAND_IS_SELLER = 1;
    static final int BYTE_COMMAND_PRICE = 2;
    static final int BYTE_COMMAND_IS_TRADED = 3;
    static final int BYTE_COMMAND_LETS_TRADE = 4;
    static final int BYTE_COMMAND_RESET_TRADE = 5;
    static final int BYTE_TRUE = 1;
    static final int BYTE_FALSE = 0;

    public void writeIsSeller(boolean isSeller) {
        writeCommand(BYTE_COMMAND_IS_SELLER, isSeller ? BYTE_TRUE : BYTE_FALSE);
    }

    public void writePrice(int price) {
        writeCommand(BYTE_COMMAND_PRICE, price);
    }

    public void writeIsTraded(boolean isTraded) {
        writeCommand(BYTE_COMMAND_IS_TRADED, isTraded ? BYTE_TRUE : BYTE_FALSE);
    }

    public void writeLetsTrade(float price) {
        // TODO: send float, don't drop.
        writeCommand(BYTE_COMMAND_LETS_TRADE, (int)price);
    }

    public void writeResetTrade() {
        writeCommand(BYTE_COMMAND_RESET_TRADE, 0);
    }

    /**
     * each commaind contatin 3 int
     */
    void writeCommand(int command, int data) {
        write(Bytes.concat(
                intToByteArray(BYTE_BEGIN),
                intToByteArray(command),
                intToByteArray(data))
        );
    }

    /**
     * each commaind contatin 3 int
     */
    byte[] consume(byte[] unconsumedData) {
        int intSize = 4;

        int i = 0;
        while (i < unconsumedData.length) {
            boolean isFoundCompleteCommand = false;
            if (unconsumedData.length >= i + intSize * 3) {

                int begin = byteArrayToInt(subBytes(unconsumedData, i, 4));
                int command = byteArrayToInt(subBytes(unconsumedData, i + 4, 4));
                int data = byteArrayToInt(subBytes(unconsumedData, i + 8, 4));

                if (begin == BYTE_BEGIN) {
                    if (command == BYTE_COMMAND_IS_SELLER) {
                        if (data == BYTE_TRUE) {

                            isSeller.set(true);
                            isFoundCompleteCommand = true;

                        } else if (data == BYTE_FALSE) {

                            isSeller.set(false);
                            isFoundCompleteCommand = true;

                        }
                    } else if (command == BYTE_COMMAND_IS_TRADED) {

                        if (data == BYTE_TRUE) {

                            isTraded.set(true);
                            isFoundCompleteCommand = true;

                        } else if (data == BYTE_FALSE) {

                            isTraded.set(false);
                            isFoundCompleteCommand = true;

                        }

                    } else if (command == BYTE_COMMAND_PRICE) {

                        price.set(data);
                        isFoundCompleteCommand = true;

                    } else if (command == BYTE_COMMAND_LETS_TRADE) {

                        if (DeviceManager.getInstance().tradedWithDevice.get() != null) {

                            Log.w(LOG_TAG, "Already traded, LETS_TRADE command from " + deviceName.get() + " is ignored.");

                        } else {

                            DeviceManager.getInstance().letsTrade(data, this);

                            this.isTraded.set(true); // Also, mark the incoming device (may not needed)
                        }

                        isFoundCompleteCommand = true;

                    } else if (command == BYTE_COMMAND_RESET_TRADE) {

                        if (DeviceManager.getInstance().tradedWithDevice.get() == null) {

                            Log.w(LOG_TAG, "I am not trading, RESET_TRADE command from " + deviceName.get() + " is ignored.");

                        } else {

                            if (!DeviceManager.getInstance().tradedWithDevice.get().getMacAddress().equals(this.getMacAddress())) {

                                Log.w(LOG_TAG, "I am  trading, but not with your, RESET_TRADE command from " + deviceName.get() + " is ignored.");

                            } else {

                                DeviceManager.getInstance().resetTrade();

                                this.isTraded.set(false); // Also, mark the incoming device (may not needed)

                            }
                        }

                        isFoundCompleteCommand = true;
                    }
                }
            }

            if (isFoundCompleteCommand) {
                i += intSize * 3;
            } else {
                i += intSize;
            }
        }

        return subBytes(unconsumedData, i, unconsumedData.length - i);
    }

    public static int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[]{
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }


    static byte[] subBytes(byte[] source, int fromIndex, int length) {
        byte[] data = new byte[source.length - fromIndex];
        System.arraycopy(source, fromIndex, data, 0, length);
        return data;
    }
}