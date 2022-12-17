package kr.ac.inha.android.APP.FIDO.util;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.ssenstone.stonepass.libstonepass_sdk.SSUserManager;

public class FidoLibraryBuilder extends AppCompatActivity {
    private SSUserManager mSSUserManager;
    private String mDuid;
    private boolean isAvailable = false;

    public FidoLibraryBuilder (Context context) {
        mSSUserManager =  new SSUserManager();
        int initResult = mSSUserManager.SSInit(context, null, "FIDO");
        if(checkLibraryAvailable(context, initResult)) isAvailable = true;
        mDuid = mSSUserManager.SSGetDUID();
    }

    public SSUserManager getSSUserManager() {
        return mSSUserManager;
    }

    public String getDeviceID () {
        return mDuid;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    private boolean checkLibraryAvailable(Context context, int initResult) {
        boolean isLicenseAvailable = LicenseChecker.isLicenseAvailable(context, initResult);
        if (!isLicenseAvailable) return false;

        Boolean isFidoAvailable = DeviceChecker.isFidoAvailable(context, mSSUserManager);
        if (!isFidoAvailable) return false;

        return true;
    }
}
