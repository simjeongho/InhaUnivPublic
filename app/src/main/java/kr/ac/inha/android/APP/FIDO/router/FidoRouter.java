package kr.ac.inha.android.APP.FIDO.router;

import android.app.Activity;
import android.content.Intent;

import kr.ac.inha.android.APP.FIDO.Activity.Fingerprint;
import kr.ac.inha.android.APP.FIDO.Activity.Pattern;
import kr.ac.inha.android.APP.FIDO.Activity.PinCode;
import kr.ac.inha.android.APP.FIDO.model.Constants;
import kr.ac.inha.android.APP.FIDO.model.RequestedAuth;

public class FidoRouter {
    public static Intent routeToReg(Activity mActivity, RequestedAuth item) {
        String type = item.type;

        Intent intent = setDestinationClass(mActivity, type);

        intent.putExtra("id", item.id);
        intent.putExtra("operation", "Reg");

        return intent;
    }

    public static Intent routeToAuth(Activity mActivity, RequestedAuth item) {
        String type = item.type;
        String systemId = item.sid;
        Intent intent = setDestinationClass(mActivity, type);

        intent.putExtra("id", item.id);
        intent.putExtra("operation", "Auth");
        intent.putExtra("systemId", systemId);

        return intent;
    }

    private static Intent setDestinationClass(Activity mActivity, String type) {
        Intent intent = null;

        if (type.equals(Constants.FINGERPRINT.getValue())) {
            intent = new Intent(mActivity, Fingerprint.class);
        } else if (type.equals(Constants.PATTERN.getValue())) {
            intent = new Intent(mActivity, Pattern.class);
        } else if (type.equals(Constants.PINCODE.getValue())) {
            intent = new Intent(mActivity, PinCode.class);
        }

        return intent;
    }
}
