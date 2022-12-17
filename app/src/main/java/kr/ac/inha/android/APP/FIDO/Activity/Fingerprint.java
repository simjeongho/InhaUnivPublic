package kr.ac.inha.android.APP.FIDO.Activity;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.ssenstone.stonepass.libstonepass_sdk.SSUserManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import kr.ac.inha.android.APP.FIDO.InhaAPI.ApiUtil;
import kr.ac.inha.android.APP.FIDO.model.Constants;
import kr.ac.inha.android.APP.FIDO.util.ErrorUtil;
import kr.ac.inha.android.APP.FIDO.util.FidoLibraryBuilder;
import kr.ac.inha.android.APP.FIDO.util.FidoUtils;
import kr.ac.inha.android.APP.R;
import kr.ac.inha.android.APP.curl.RequestUtils;
import kr.ac.inha.android.APP.curl.VolleyUtil;

public class Fingerprint extends AppCompatActivity {

    public static final String TAG = Fingerprint.class.getSimpleName();

    private Dialog mFingerprintDlg;
    private TextView mFingerPrintStatus;
    private Button mAuthBtn;
    private ProgressBar pb;

    private String mOp;
    private String mUserName;
    private String mDuid;
    private String mSystemId = Constants.SYSTEMID.getValue();
    private final String mBioType = Constants.FINGERPRINT.getValue();
    private final String mServerInfo = Constants.SERVERINFO.getValue();

    // StonePASS Library
    private SSUserManager mSSUserManager;
    private SSUserManager.SSFidoListener    mFidoListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayoutResource();
        initData();
        setActionBar();
        if(!initSSUserManager()) return; // StonePASS 라이브러리 초기화
        setButtonListner();
        mAuthBtn.callOnClick();
    }

    protected void setLayoutResource() {
        setContentView(R.layout.activity_fingerprint);
        mFingerprintDlg = new Dialog(this , R.style.Dialog);
        mFingerprintDlg.setContentView(R.layout.fingerprint_dlg);
        mFingerPrintStatus = (TextView) mFingerprintDlg.findViewById(R.id.fingerprint_status);
        mAuthBtn = (Button) findViewById(R.id.fingerPrintAuthBtn);
        pb = (ProgressBar) findViewById(R.id.progress);
    }

    protected void initData() {
        // 리소스 초기화
        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            mOp = bundle.getString("operation");
            mUserName = bundle.getString("id");
            if(mOp.equalsIgnoreCase("Auth")) mSystemId = bundle.getString("systemId");
        }
    }

    protected boolean initSSUserManager() {
        FidoLibraryBuilder fidoLib = new FidoLibraryBuilder(this);
        if(!fidoLib.isAvailable()) {
            finishFingerAuth(false);
            return false;
        }
        mSSUserManager = fidoLib.getSSUserManager();
        mDuid = fidoLib.getDeviceID();
        return true;
    }

    protected void setActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            if(mOp.equalsIgnoreCase("Reg")) {
                actionBar.setTitle("등록");
            } else if(mOp.equalsIgnoreCase("Auth")) {
                actionBar.setTitle("인증");
            }
        }
    }

    protected void setButtonListner() {
        setAuthButtonListner();
        setCancelButtonListner();
    }

    protected void setAuthButtonListner() {
        // 인증 버튼 클릭 이벤트
        mAuthBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ssenstoneFIDO(mOp, mUserName);
            }
        });
    }

    protected void setCancelButtonListner() {
        // 취소 버튼 클릭 이벤트
        mFingerprintDlg.findViewById(R.id.cancelFingerBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSSUserManager.SSCancelFingerprint();
                finishFingerAuth(false);
            }
        });
    }

    /**
     * SSenStone FIDO
     * @param op			Operation (Reg/Auth/Dereg(는 삭제))
     * @param userName		user Name
     */
    protected void ssenstoneFIDO(String op, String userName) {
        // FIDO 지문인식 사용 가능 프로세스 진행
        if(!op.equalsIgnoreCase("Reg") && !op.equalsIgnoreCase("Auth")) return;

        JSONObject requestMsg = RequestUtils.buildRequestToSsenstone(op, userName, mSystemId, mBioType, mDuid);
        String url = mServerInfo + "/Get";
        Response.Listener<JSONObject> callback = onResponseFromFidoServer(op, userName);
        Response.ErrorListener fallback = onErrorResponseFromFidoServer(op);

        VolleyUtil.post(url, requestMsg, callback, fallback);
    }

    protected Response.Listener<JSONObject> onResponseFromFidoServer(String op, String userName) {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                String msg = FidoUtils.getUafRequest(response);
                mFingerprintDlg.show();
                FidoProcess(op, userName, msg, mBioType);
            }
        };
    }

    protected Response.ErrorListener onErrorResponseFromFidoServer(String op) {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                FidoUtils.showToast(getApplicationContext(), op + " Fail[" + error.toString() + "]");
            }
        };
    }

    // FIDO 처리
    void FidoProcess(final String op, final String userName, String message, String fcode) {
        Log.d(TAG, "[FidoProcess] " + op);
        Log.d(TAG, "[FidoProcess] " + userName);
        Log.d(TAG, "[FidoProcess] " + message);

        // 정상
        pb.setVisibility(View.VISIBLE);
        setFidoListener(op);

        mSSUserManager.setFidoListener(mFidoListener, this, userName, message, fcode, mBioType, true);
    }

    protected void setFidoListener(String op) {
        mFidoListener = new SSUserManager.SSFidoListener() {
            @Override
            public void authenticationFailed(int errorCode, String errString) {
                onAuthFailed(errorCode, errString);
            }

            @Override
            public void authenticationSucceeded(String responseMsg) {
                onAuthSucceded(op, responseMsg);
            }
        };
    }

    private void onAuthFailed(int errorCode, String errString) {
        pb.setVisibility(View.GONE);
        Log.d(TAG, "[authenticationFailed] [" + String.valueOf(errorCode) + "] " + errString);
        updateFingerPrintStatus(errString);
        onAuthfailedAtSDK(errorCode);
    }

    private void onAuthSucceded(String op, String msg) {
        Log.d(TAG, "[authenticationSucceeded] " + msg);
        try {
            JSONObject jsonMsg = new JSONObject(msg);
            String url = mServerInfo + "/Send/" + op;
            Response.Listener<JSONObject> callback = onAuthFromFidoServer(op);
            Response.ErrorListener fallback = onAuthFailFromFidoServer(op);

            VolleyUtil.post(url, jsonMsg, callback, fallback);
        } catch (JSONException e) {
            String errorMsg = op + " Fail[" + e.toString() + "]";
            FidoUtils.showToast(getApplicationContext(), errorMsg);
        }
    }

    protected Response.Listener<JSONObject> onAuthFromFidoServer(String op) {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "[authenticationSucceeded 2차 response] " + response);

                int statusCode = 0;
                try {
                    statusCode = response.getInt("statusCode");
                    if (statusCode != 1200) {
                        onAuthFailedAtFidoServer(op, statusCode);
                        return;
                    }
                    ApiUtil apiUtil = new ApiUtil(mUserName, mSystemId, getApplicationContext());
                    apiUtil.sendSuccessToInhaAPI(op);
                    finishFingerAuth(true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    protected Response.ErrorListener onAuthFailFromFidoServer(String op) {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                FidoUtils.showToast(getApplicationContext(), op + " Fail[" + error.toString() + "]");
            }
        };
    }

    protected void updateFingerPrintStatus(String errString) {
        mFingerPrintStatus.setText(errString);
        new Timer().schedule(new TimerTask() {
            public void run() {
                mFingerPrintStatus.post(new Runnable() {
                    @Override
                    public void run() {
                        mFingerPrintStatus.setText(getApplicationContext().getString(R.string.fingerprint_hint));
                    }
                });
            } }, 1000);
    }

    protected void onAuthfailedAtSDK(int errorCode) {
        ErrorUtil.onAuthfailedAtSDK(errorCode);
        if(errorCode == 5 || errorCode == 6 || errorCode == 7 || errorCode == 255) {
            // 5	지문 스캐너 오류: 지원되지 않는 Authenticator
            // 6	메시지 오류: 정상 메시지 아님
            // 7	FacetID 오류: 서버에 미등록된 APP
            // 255	기타 오류
            mFingerprintDlg.dismiss();
        } else if(errorCode == 10 || errorCode == 200) {
            // 10	지문 스캐너 오류: 지문 잠금 상태 (5회 실패)
            // 200	키 인증 관련 오류: 재등록 프로세스로 이동 필요(지문정보 변경 사용자)
            finishFingerAuth(false);
        }
    }

    protected void onAuthFailedAtFidoServer(String op, int statusCode) {
        ErrorUtil.onAuthFailedAtFidoServer(statusCode);
        pb.setVisibility(View.GONE);
        FidoUtils.showToast(getApplicationContext(), op + " Fail[" + String.valueOf(statusCode) + "]");
        mFingerprintDlg.dismiss();
    }

    protected void finishFingerAuth(Boolean isOK) {
        mFingerprintDlg.dismiss();
        mFingerprintDlg = null;
        ApiUtil.renewRegisterStatusOnWebview();
        if (isOK) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }
}
