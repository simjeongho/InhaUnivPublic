package kr.ac.inha.android.APP.Webview;

import android.content.Context;
import kr.ac.inha.android.APP.library.InhaUtility;

import static kr.ac.inha.android.APP.library.InhaUtility.getDeviceToken;

public class UrlUtil {
    public static String getMobileHome (Context ctx) {
        return InhaUtility.MOBILEURL + InhaUtility.getVersionName(ctx);
    }

    public static String getMobileHomeWithDeviceToken (Context ctx) {
        StringBuilder builder = new StringBuilder();
        builder.append(InhaUtility.MOBILEURL);
        builder.append(InhaUtility.getVersionName(ctx));
        builder.append("&phone_regid=");
        builder.append(getDeviceToken(ctx));
        builder.append("&phone_type=Android");
        return builder.toString();
    }

    public static String getPortal() {
        return InhaUtility.ROOTURL + "/portal/portal_main.aspx?";
    }
}
