package io.hengky.common;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by yip on 17/5/16.
 */
public class DeviceManager extends Observable<List<Device>> {


    public final ArrayList<Device> mDeviceList = new ArrayList<>();
    private static Subscriber<? super List<Device>> mSubscriber; // never error, never complete.

    private static DeviceManager ourInstance = new DeviceManager(new OnSubscribe<List<Device>>() {
        @Override
        public void call(Subscriber<? super List<Device>> subscriber) {
            mSubscriber = subscriber;
        }
    });

    protected DeviceManager(OnSubscribe<List<Device>> f) {
        super(f);
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

    public void broadcasePrice(int price){
        for (int i = 0; i < mDeviceList.size(); i++) {
            mDeviceList.get(i).writePrice(price);
        }
    }

}
