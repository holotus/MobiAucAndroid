package io.hengky.common;

import android.databinding.ObservableField;
import android.databinding.ObservableFloat;
import android.databinding.ObservableInt;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by yip on 17/5/16.
 */
public class DeviceManager extends Observable<List<Device>> {

    private static final String LOG_TAG = Device.class.getSimpleName();

    /////
    // States of current device
    /////
    public final ObservableInt price = new ObservableInt();
    public final ObservableField<Device> tradedWithDevice = new ObservableField<>();
    public final ObservableFloat tradedAtPrice = new ObservableFloat();
    public static final ArrayList<Device> mDeviceList = new ArrayList<>();


    private static Subscriber<? super List<Device>> mSubscriber; // never error, never complete.

    private static DeviceManager ourInstance = new DeviceManager(new OnSubscribe<List<Device>>() {
        @Override
        public void call(Subscriber<? super List<Device>> subscriber) {

            if (mSubscriber != null) {
                Log.w(LOG_TAG, "mSubscriber assigning again?");
            }

            mSubscriber = subscriber;
        }
    });

    protected DeviceManager(OnSubscribe<List<Device>> f) {
        super(f);

        price.addOnPropertyChangedCallback(new android.databinding.Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(android.databinding.Observable observable, int i) {
                broadcasePrice(price.get());
            }
        });

        tradedWithDevice.addOnPropertyChangedCallback(new android.databinding.Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(android.databinding.Observable observable, int i) {
                broadcaseIsTraded(tradedWithDevice.get() != null);
            }
        });
    }

    public static DeviceManager getInstance() {
        return ourInstance;
    }

    public synchronized void add(Device device) {

        // Don't add device with the same mac address twice.
        for (int i = 0; i < mDeviceList.size(); i++) {
            if (mDeviceList.get(i).getMacAddress().equals(device.getMacAddress())) {
                device.closeSocket();
                return;
            }
        }

        mDeviceList.add(device);


        device.writeIsSeller(BuildConfigHelper.getInstance().getIsSeller());
        device.writePrice(price.get());
        device.writeIsTraded(tradedWithDevice.get() != null);

        if (mSubscriber != null) {
            mSubscriber.onNext(mDeviceList);
        }

    }

    public synchronized void remove(Device device) {
        if (mDeviceList.contains(device)) {

            mDeviceList.remove(device);

            if (mSubscriber != null) {
                mSubscriber.onNext(mDeviceList);
            }

        }
    }

    private void broadcasePrice(int price) {
        for (int i = 0; i < mDeviceList.size(); i++) {
            mDeviceList.get(i).writePrice(price);
        }
    }

    private void broadcaseIsTraded(boolean isTraded) {
        for (int i = 0; i < mDeviceList.size(); i++) {
            mDeviceList.get(i).writeIsTraded(isTraded);
        }
    }

    public void letsTrade(float price, Device tradeWithDevice){
        tradeWithDevice.writeLetsTrade(price);
        tradeWithDevice.isTraded.set(true);
        this.tradedWithDevice.set(tradeWithDevice);
        this.tradedAtPrice.set(price);
    }

    public void resetTrade(){
        Device lastTradedDevice = this.tradedWithDevice.get();

        this.tradedWithDevice.set(null);
        this.tradedAtPrice.set(0);
        this.price.set(0);

        if(lastTradedDevice!=null) {
            lastTradedDevice.writeResetTrade();
        }
    }

}
