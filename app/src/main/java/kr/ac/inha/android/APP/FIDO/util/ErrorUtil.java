package kr.ac.inha.android.APP.FIDO.util;

import android.content.Context;

import kr.ac.inha.android.APP.FIDO.model.ResponseMessage;

public class ErrorUtil {
    public static void onAuthfailedAtSDK(int errorCode) {
        if(errorCode == 6 || errorCode == 7 || errorCode == 255) {
            // 6	메시지 오류: 정상 메시지 아님
            // 7	FacetID 오류: 서버에 미등록된 APP
            // 255	기타 오류
        } else if(errorCode == 200) {
            // 200	키 인증 관련 오류: 재등록 프로세스로 이동 필요(지문정보 변경 사용자)
        }
    }

    // 인증서버에서 response된 오류 처리
    public static void onAuthFailedAtFidoServer(int statusCode) {
        return;
    }

    public static void onErrorAtInhaAPI(Context context, String op, ResponseMessage respObj) {
        String operation = "";
        if (op.equalsIgnoreCase("Reg")) {
            operation = "등록";
        } else if (op.equalsIgnoreCase("Auth")) {
            operation = "인증";
        }
        String msg = operation + " 실패: " + respObj.getStatus() + "/" + respObj.getMsg();
        FidoUtils.showToast(context, msg);
    }
}
