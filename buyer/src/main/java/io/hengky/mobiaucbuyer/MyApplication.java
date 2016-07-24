package io.hengky.mobiaucbuyer;

import android.app.Application;

import io.hengky.common.BuildConfigHelper;

/**
 * Created by yip on 21/5/16.
 */
public class MyApplication extends Application {

    public MyApplication(){
        super();
        BuildConfigHelper.getInstance().setIsSeller(false);

    }
}
