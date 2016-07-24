package io.hengky.mobiaucseller.view;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ListViewCompat;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;

import io.hengky.common.BaseMainActivity;
import io.hengky.common.BluetoothSocketManager;
import io.hengky.common.BuildConfigHelper;
import io.hengky.common.Device;
import io.hengky.common.DeviceManager;
import io.hengky.mobiaucseller.MyApplication;
import io.hengky.mobiaucseller.R;
import io.hengky.mobiaucseller.databinding.ActivityMainBinding;
import io.hengky.mobiaucseller.databinding.ViewDeviceListItemBinding;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;


/**
 * Created by yip on 16/5/16.
 */
public class MainActivity extends BaseMainActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int MIN_PRICE = 50;
    private static final int MAX_PRICE = 100;
    private static final int SELLING_COUNTDOWN_SECONDS = 30;

    // possible value of "sellingState"
    public static final int SELLING_STATE_INIT = 0;
    public static final int SELLING_STATE_SELLING = 1;
    public static final int SELLING_STATE_SOLD = 2;

    public final ObservableInt sellingState = new ObservableInt();
    public final ObservableInt secondsLeft = new ObservableInt();

    private ArrayAdapter<Device> arrayAdapter;
    private final CountDownTimer timer = new InternalCountDownTimer(SELLING_COUNTDOWN_SECONDS);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        ActivityMainBinding dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        dataBinding.setState(this);
        dataBinding.setDeviceManager(DeviceManager.getInstance());

        {
            dataBinding.powerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    setDiscoverable(isChecked);
                }
            });
        }

        /////
        // Binding device list
        /////
        {
            arrayAdapter = new ArrayAdapter<Device>(this, R.layout.view_device_list_item, R.id.title_txt) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    ViewDeviceListItemBinding binding = ViewDeviceListItemBinding.inflate(getLayoutInflater(), parent, false);
                    binding.setDevice(arrayAdapter.getItem(position));
                    return binding.getRoot();
                }
            };

            dataBinding.deviceList.setAdapter(arrayAdapter);
            DeviceManager.getInstance().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<List<Device>>() {
                @Override
                public void call(List<Device> devices) {
                    arrayAdapter.clear();
                    arrayAdapter.addAll(devices);
                }
            });
        }

        /////
        // sellingToDevice and isDiscoverable will effect sellingState
        /////
        {
            DeviceManager.getInstance().tradedWithDevice.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable observable, int i) {
                    updateSellingState();
                }
            });
            secondsLeft.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable observable, int i) {
                    updateSellingState();
                }
            });

        }

//        test_1();
    }

    void updateSellingState()  {
        if (secondsLeft.get() > 0) {
            sellingState.set(SELLING_STATE_SELLING);
        } else {
            if (DeviceManager.getInstance().tradedWithDevice.get() != null) {
                sellingState.set(SELLING_STATE_SOLD);
            } else {
                sellingState.set(SELLING_STATE_INIT);
            }
        }
    }

    public void onStartSellingClicked() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog);

        final SeekBar input = new SeekBar(MainActivity.this);

        {
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    DeviceManager.getInstance().price.set(input.getProgress() + MIN_PRICE);
                    timer.cancel();
                    timer.start();
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
        }

        final AlertDialog dialog = builder.create();
        {
            dialog.setTitle("Set price: $" + MIN_PRICE);
            input.setMax(MAX_PRICE - MIN_PRICE);
            input.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    dialog.setTitle("Set price: $" + (progress + MIN_PRICE));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // do nothing
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // do nothing
                }
            });
            dialog.setView(input);
        }

        dialog.show();
    }

    public void onResetSellingClicked() {
        DeviceManager.getInstance().resetTrade();
        timer.cancel();
        secondsLeft.set(0);
    }

    void onSellingOK(float sellingAtPrice, Device sellingToDevice) {
        DeviceManager.getInstance().letsTrade(sellingAtPrice, sellingToDevice);
        timer.cancel();
        secondsLeft.set(0);
    }

    void onSellingKO() {
        DeviceManager.getInstance().resetTrade();
        timer.cancel();
        secondsLeft.set(0);
    }

    class InternalCountDownTimer extends CountDownTimer {

        public InternalCountDownTimer(long seconds) {
            super(seconds * 1000, 1000);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            secondsLeft.set((int) millisUntilFinished / 1000);
        }

        @Override
        public void onFinish() {

            ArrayList<Device> B = new ArrayList<>();
            ArrayList<Device> S = new ArrayList<>();

            // add this device
            Device thisDevice = new ThisDevice(DeviceManager.getInstance().price.get(), DeviceManager.getInstance().tradedWithDevice.get() != null);
            if (!thisDevice.isTraded.get()) {
                S.add(thisDevice);
            }

            // add other devices
            for (int i = 0; i < arrayAdapter.getCount(); i++) {
                Device d = arrayAdapter.getItem(i);

                if (d.isTraded.get()) {
                    continue;
                }

                if (d.isSeller.get()) {
                    S.add(d);
                } else {
                    B.add(d);
                }
            }


            Pair<Device, Float> result = findBuyerAndPrice(B, S);

            if (result != null) {

                Device sellingToDevice = result.first;
                float sellingPrice = result.second;

                onSellingOK(sellingPrice, sellingToDevice);

                new android.app.AlertDialog.Builder(MainActivity.this)
                        .setMessage("Good!! Sold to: " + sellingToDevice.deviceName.get())
                        .setPositiveButton("OK", null)
                        .setCancelable(true)
                        .show();
            } else {

                onSellingKO();

                new android.app.AlertDialog.Builder(MainActivity.this)
                        .setMessage("No matching deal, please try again.")
                        .setPositiveButton("OK", null)
                        .setCancelable(true)
                        .show();
            }

        }
    }

    // TODO: handle concurrent situation where device will be updated any moment.
    public static Pair<Device, Float> findBuyerAndPrice(ArrayList<Device> B, ArrayList<Device> S) {

        /////
        // Algorithm 1: Determining Qualified Buyers and Sellers
        /////

        /////
        // No trade will take place.
        /////
        if (B.size() < 1 || S.size() < 1) return null;


        // 1. Descending buyer list
        Collections.sort(B, new Comparator<Device>() {
            @Override
            public int compare(Device lhs, Device rhs) {
                return rhs.price.get() - lhs.price.get();
            }
        });

        // 2. Ascending seller list
        Collections.sort(S, new Comparator<Device>() {
            @Override
            public int compare(Device lhs, Device rhs) {
                return lhs.price.get() - rhs.price.get();
            }
        });

        // 3. compute p_auc
        float p_auc = 0;
        {
            for (int k = 0; k < Math.min(B.size(), S.size()); k++) {
                int p_bid_k = B.get(k).price.get();
                int p_ask_k = S.get(k).price.get();
                int p_bid_k_plus_1 = B.size() > k + 1 ? B.get(k + 1).price.get() : 0;
                int p_ask_k_plus_1 = S.size() > k + 1 ? S.get(k + 1).price.get() : 0;

                if (p_bid_k >= p_ask_k) {
                    if (p_bid_k_plus_1 == 0 || p_ask_k_plus_1 == 0) {
                        // 3b
                        p_auc = (float) 0.5 * (p_bid_k + p_ask_k);
                        Log.i(LOG_TAG, "p_auc using 3b");
                    } else {
                        // 3a
                        p_auc = (float) 0.5 * (p_bid_k_plus_1 + p_ask_k_plus_1);
                        Log.i(LOG_TAG, "p_auc using 3a");
                    }
                } else {
                    // done
                }
            }
        }

        /////
        // No trade will take place.
        /////
        if (p_auc == 0) return null;


        // 2. Descending buyer list by data
        Collections.sort(B, new Comparator<Device>() {
            @Override
            public int compare(Device lhs, Device rhs) {
                return rhs.data.get() - lhs.data.get();
            }
        });

        // 2. Descending seller list by data
        Collections.sort(S, new Comparator<Device>() {
            @Override
            public int compare(Device lhs, Device rhs) {
                return rhs.data.get() - lhs.data.get();
            }
        });

        Device sellingToDevice = null;

        Log.i(LOG_TAG, "p_auc settled at: " + p_auc);
        int b_i = 0, s_i = 0;
        while (b_i < B.size() && s_i < S.size()) {

            Device b = B.get(b_i);
            Device s = S.get(s_i);

            if (b.price.get() < p_auc) {
                b_i++;
                continue;
            }

            if (s.price.get() < p_auc) {
                s_i++;
                continue;
            }

            if (b.data.get() <= s.data.get()) {
                Log.i(LOG_TAG, ": buyer_" + b_i + " (" + b.deviceName.get() + ", " + b.price.get() + ", " + b.data.get() + ") <-o-> seller_" + s_i + " (" + s.deviceName.get() + ", " + s.price.get() + ", " + s.data.get() + ")");

                if (s instanceof ThisDevice) {
                    sellingToDevice = b;
                }

                b_i++;
                s_i++;
            } else {
                b_i++;
            }

        }

        if (sellingToDevice != null) {
            return new Pair<>(sellingToDevice, p_auc);
        } else {
            return null;
        }
    }

    /**
     * Special purpose class to represent this current device. (always seller, with device name "__this_device__"
     */
    class ThisDevice extends Device {

        public ThisDevice(int price, boolean isTraded) {
            this.price.set(price);
            this.isTraded.set(isTraded);
            isSeller.set(true); // always seller
            deviceName.set("__this_device__");
        }
    }

    class TestDevice extends Device {

        public TestDevice(int price, boolean isTraded, boolean isSeller, String deviceName, int data) {
            this.price.set(price);
            this.isTraded.set(isTraded);
            this.isSeller.set(isSeller); //
            this.deviceName.set(deviceName);
            this.data.set(data);
        }
    }
/*
    void test_1() {

        Log.i(LOG_TAG, "test_1: begin");



        // Add this device
//        S.add(new ThisDevice(9, false));

        // Add other devices
        Random randomPrice = new Random();
        int minPrice = 1;
        int maxPrice = 30;

        Random randomData = new Random();
        int minData = 1;
        int maxData = 200;

        int numberOfTrial = 1000;
        int numberOfDevice = 100 / 2;
//        int numberOfDevice = 500 / 2;
//        int numberOfDevice = 1000 / 2;
//        int numberOfDevice = 5000 / 2;
//        int numberOfDevice = 10000 / 2;

        long totalUsedTime = 0;
        for (int j = 0; j < numberOfTrial; j++) {

            ArrayList<Device> B = new ArrayList<>();
            ArrayList<Device> S = new ArrayList<>();
            for (int i = 0; i < numberOfDevice; i++) {
                int nextPrice = randomPrice.nextInt(maxPrice - minPrice + 1) + minPrice;
                int nextData = randomData.nextInt(maxData - minData + 1) + minData;
                B.add(new TestDevice(nextPrice, false, false, "__device_" + i + "__", nextData));
            }
            for (int i = 0; i < numberOfDevice; i++) {
                int nextPrice = randomPrice.nextInt(maxPrice - minPrice + 1) + minPrice;
                int nextData = randomData.nextInt(maxData - minData + 1) + minData;
                S.add(new TestDevice(nextPrice, false, true, "__device_" + i + "__", nextData));
            }

//        Log.i(LOG_TAG, "findBuyerAndPrice: " + (System.currentTimeMillis() - time));

            long time = System.currentTimeMillis();
            Pair<Device, Float> result = findBuyerAndPrice(B, S);
            long usedTime = System.currentTimeMillis() - time;
//            Log.i(LOG_TAG, "findBuyerAndPrice(" + j + ") usedTime: " + usedTime);
            totalUsedTime += usedTime;

//            Log.i(LOG_TAG, "test result.sellingToDevice: " + (result == null ? "null" : result.first.deviceName.get()));
//            Log.i(LOG_TAG, "test result.p_auc: " + (result == null ? 0 : result.second));
        }
        Log.i(LOG_TAG, "findBuyerAndPrice totalUsedTime: " + totalUsedTime);
        Log.i(LOG_TAG, "findBuyerAndPrice averageUsedTime: " + totalUsedTime / numberOfTrial);

//
//        Log.i(LOG_TAG, "test_1: finish");
    }
    */
}
