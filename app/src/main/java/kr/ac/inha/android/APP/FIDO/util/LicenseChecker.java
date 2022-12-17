package kr.ac.inha.android.APP.FIDO.util;

import android.content.Context;

public class LicenseChecker {
    public static boolean isLicenseAvailable(Context context, int initResult) {
        if(initResult != 0) {
            FidoUtils.showToast(context, "인증서버에 등록되지 않은 앱입니다");
            return false;
        }
        return true;
    }
}
