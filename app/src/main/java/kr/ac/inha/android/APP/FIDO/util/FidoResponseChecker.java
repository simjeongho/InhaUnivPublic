package kr.ac.inha.android.APP.FIDO.util;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

public class FidoResponseChecker extends AppCompatActivity {

    private Context context;
    private int errorCode;
    private String errString;

    public FidoResponseChecker(Context context, int errorCode, String errString) {
        this.context = context;
        this.errorCode = errorCode;
        this.errString = errString;
    }

    public void showWrongTryToast(int wrongTry) {
        if (this.errorCode != 100) {
            FidoUtils.showToast(context, "오류 발생: [" + String.valueOf(errorCode) + "]" + errString);
            return;
        }
        if (wrongTry >= 4) {
            FidoUtils.showToast(context, "5회 잘못 입력하셨습니다");
            return;
        }
        FidoUtils.showToast(context, "잘못 입력하셨습니다");
    }
}
