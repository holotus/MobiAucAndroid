package io.hengky.common;

/**
 * Created by yip on 21/5/16.
 */
public class BuildConfigHelper {

    private Boolean mIsSeller;

    private static BuildConfigHelper ourInstance = new BuildConfigHelper();

    public static BuildConfigHelper getInstance() {
        return ourInstance;
    }

    private BuildConfigHelper() {
    }

    public boolean getIsSeller(){
        if(mIsSeller == null){
            throw new Error("Must call BuildConfigHelper.setIsSeller()");
        }
        return mIsSeller;
    }

    public void setIsSeller(boolean isSeller){
        mIsSeller = isSeller;
    }
}
