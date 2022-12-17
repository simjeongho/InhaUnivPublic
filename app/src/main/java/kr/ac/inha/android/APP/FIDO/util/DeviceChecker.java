package kr.ac.inha.android.APP.FIDO.util;

import android.content.Context;

import com.ssenstone.stonepass.libstonepass_sdk.SSUserManager;

public class DeviceChecker {
    public static boolean isFidoAvailable(Context context, SSUserManager mSSUserManager) {
        int ret = mSSUserManager.SSCheckDevice();
        if ( ret == - 3 ) { // 디바이스 지문 미등록
            FidoUtils.showToastLong(context, "기기에 지문이 등록되어 있지 않습니다");
            return false;
        } else if ( ret != 0 ) {  // FIDO 미지원
            FidoUtils.showToastLong(context, "생체인증을 지원하지 않는 기기입니다");
            return false;
        }
        return true;
    }
}
