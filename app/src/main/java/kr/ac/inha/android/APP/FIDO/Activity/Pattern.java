package kr.ac.inha.android.APP.FIDO.Activity;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.andrognito.patternlockview.PatternLockView;
import com.andrognito.patternlockview.listener.PatternLockViewListener;
import com.andrognito.patternlockview.utils.PatternLockUtils;
import com.andrognito.patternlockview.utils.ResourceUtils;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.ssenstone.stonepass.libstonepass_sdk.SSUserManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import kr.ac.inha.android.APP.FIDO.InhaAPI.ApiUtil;
import kr.ac.inha.android.APP.FIDO.model.Constants;
import kr.ac.inha.android.APP.FIDO.util.ErrorUtil;
import kr.ac.inha.android.APP.FIDO.util.FidoLibraryBuilder;
import kr.ac.inha.android.APP.FIDO.util.FidoResponseChecker;
import kr.ac.inha.android.APP.FIDO.util.FidoUtils;
import kr.ac.inha.android.APP.R;
import kr.ac.inha.android.APP.curl.RequestUtils;
import kr.ac.inha.android.APP.curl.VolleyUtil;

public class Pattern extends AppCompatActivity {

    public static final String TAG = Pattern.class.getSimpleName();

    // Pattern View Library
    private PatternLockView mPatternLockView;

    private ProgressBar pb;

    private Context mContext;

    // StonePASS Library
    private SSUserManager mSSUserManager;
    private SSUserManager.SSFidoListener    mFidoListener;

    private String mOp;
    private String mUserName;
    private String mDuid;
    private String mSystemId = Constants.SYSTEMID.getValue();
    private final String mBioType = Constants.PATTERN.getValue();
    private final String mServerInfo = Constants.SERVERINFO.getValue();

    private Boolean firstTry = true;
    private String firstPattern = "";
    private int wrongTryCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayoutResource();
        initPatternResource();
        initData();
        initSSUserManager(); // StonePASS 라이브러리 초기화
    }

    protected void setLayoutResource() {
        setContentView(R.layout.activity_pattern);
        mContext = this;
        pb = (ProgressBar) findViewById(R.id.progress);
    }

    private void initPatternResource() {
        mPatternLockView = (PatternLockView) findViewById(R.id.pattern_lock_view);
        mPatternLockView.setDotCount(3);
        mPatternLockView.setDotNormalSize((int) ResourceUtils.getDimensionInPx(this, R.dimen.pattern_lock_dot_size));
        mPatternLockView.setDotSelectedSize((int) ResourceUtils.getDimensionInPx(this, R.dimen.pattern_lock_dot_selected_size));
        mPatternLockView.setPathWidth((int) ResourceUtils.getDimensionInPx(this, R.dimen.pattern_lock_path_width));
        mPatternLockView.setAspectRatioEnabled(true);
        mPatternLockView.setAspectRatio(PatternLockView.AspectRatio.ASPECT_RATIO_HEIGHT_BIAS);
        mPatternLockView.setViewMode(PatternLockView.PatternViewMode.CORRECT);
        mPatternLockView.setDotAnimationDuration(150);
        mPatternLockView.setPathEndAnimationDuration(100);
        mPatternLockView.setBackgroundColor(Color.WHITE);
        mPatternLockView.setNormalStateColor(Color.BLACK);
        mPatternLockView.setCorrectStateColor(Color.GREEN);
        mPatternLockView.setWrongStateColor(Color.RED);
        mPatternLockView.setInStealthMode(false);
        mPatternLockView.setTactileFeedbackEnabled(true);
        mPatternLockView.setInputEnabled(true);
        mPatternLockView.addPatternLockListener(mPatternLockViewListener);
    }

    private void initData() {
        // 리소스 초기화
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) return;
        mOp = bundle.getString("operation");
        mUserName = bundle.getString("id");
        if(mOp.equalsIgnoreCase("Auth")) mSystemId = bundle.getString("systemId");
    }

    protected void initSSUserManager() {
        FidoLibraryBuilder fidoLib = new FidoLibraryBuilder(this);
        mSSUserManager = fidoLib.getSSUserManager();
        mDuid = fidoLib.getDeviceID();
    }

    private PatternLockViewListener mPatternLockViewListener = new PatternLockViewListener() {
        @Override
        public void onStarted() {
            Log.d(TAG, "Pattern drawing started");
        }

        @Override
        public void onProgress(List<PatternLockView.Dot> progressPattern) {
            Log.d(TAG, "Pattern progress: " + PatternLockUtils.patternToString(mPatternLockView, progressPattern));
        }

        @Override
        public void onComplete(List<PatternLockView.Dot> pattern) {
            Log.d(TAG, "Pattern complete: " + PatternLockUtils.patternToString(mPatternLockView, pattern));

            // *** 저장된 패턴 값 검증 (등록 시 저장 / 인증 및 해지 시 검증 필요) ***
            String drawnPattern = PatternLockUtils.patternToString(mPatternLockView, pattern);

            // 등록시 패턴 2번 검증
            if (mOp.equalsIgnoreCase("Reg")) {
                checkPatternWhenReg(drawnPattern);
            } else if (mOp.equalsIgnoreCase("Auth")) {
                pb.setVisibility(View.VISIBLE);
                ssenstoneFIDO(mOp, mUserName, drawnPattern);
                mPatternLockView.clearPattern();
            }
        }

        @Override
        public void onCleared() {
            Log.d(TAG, "Pattern has been cleared");
        }
    };

    /**
     * SSenStone FIDO
     * @param op			Operation (Reg/Auth/Dereg(해지는 삭제))
     * @param userName		user Name
     */
    protected void ssenstoneFIDO(String op, String userName, String pattern) {
        if(!op.equalsIgnoreCase("Reg") && !op.equalsIgnoreCase("Auth")) return;

        JSONObject requestMsg = RequestUtils.buildRequestToSsenstone(op, userName, mSystemId, mBioType, mDuid);
        String url = mServerInfo + "/Get";
        Response.Listener<JSONObject> callback = onResponseFromFidoServer(op, userName, pattern);
        Response.ErrorListener fallback = onErrorResponseFromFidoServer(op);

        VolleyUtil.post(url, requestMsg, callback, fallback);
    }

    protected Response.Listener<JSONObject> onResponseFromFidoServer(String op, String userName, String pattern) {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                String msg = FidoUtils.getUafRequest(response);
                FidoProcess(op, userName, msg, pattern);
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
    void FidoProcess(final String op, final String userName, final String message, final String fcode) {
        Log.d(TAG, "[FidoProcess] " + fcode);
        setFidoListener(op);
        mSSUserManager.setFidoListener(mFidoListener, this, userName, message, fcode, mBioType, false);
    }

    private void checkPatternWhenReg(String drawnPattern) {
        if (firstTry) {
            firstTry = false;
            firstPattern = drawnPattern;
            FidoUtils.showToast(getApplicationContext(), "동일한 패턴을 한 번 더 입력해 주세요");
            mPatternLockView.clearPattern();
            return;
        }
        if (!firstPattern.equalsIgnoreCase(drawnPattern)) {
            FidoUtils.showToast(getApplicationContext(), "패턴이 일치하지 않습니다");
            mPatternLockView.clearPattern();
            return;
        }
        pb.setVisibility(View.VISIBLE);
        ssenstoneFIDO(mOp, mUserName, firstPattern);
        mPatternLockView.clearPattern();
    }

    private void checkWrongTry(int errorCode, String errString) {
        FidoResponseChecker fidoResponseChecker = new FidoResponseChecker(getApplicationContext(), errorCode, errString);
        fidoResponseChecker.showWrongTryToast(wrongTryCounter);
        wrongTryCounter++;
        if (wrongTryCounter >= 5) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void onAuthFailedAtFidoServer(String op, int statusCode) {
        ErrorUtil.onAuthFailedAtFidoServer(statusCode);
        pb.setVisibility(View.GONE);
        FidoUtils.showToast(getApplicationContext(), op + " Fail[" + String.valueOf(statusCode) + "]");
    }

    private void setFidoListener(String op) {
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
                Log.d(TAG, "[authenticationSucceeded 2번째 response] " + response);

                int statusCode = 0;
                try {
                    statusCode = response.getInt("statusCode");
                    if (statusCode != 1200) {
                        onAuthFailedAtFidoServer(op, statusCode);
                        return;
                    }
                    ApiUtil apiUtil = new ApiUtil(mUserName, mSystemId, getApplicationContext());
                    apiUtil.sendSuccessToInhaAPI(op);
                    ApiUtil.renewRegisterStatusOnWebview();
                    setResult(RESULT_OK);
                    finish();
                } catch (JSONException e) {
                    String errorMsg = op + " Fail[" + e.toString() + "]";
                    FidoUtils.showToast(getApplicationContext(), errorMsg);
                }
            }
        };
    }

    protected Response.ErrorListener onAuthFailFromFidoServer(String op) {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String errorMsg = op + " Fail[" + error.toString() + "]";
                FidoUtils.showToast(getApplicationContext(), errorMsg);
            }
        };
    }

    private void onAuthFailed(int errorCode, String errString) {
        pb.setVisibility(View.GONE);
        ErrorUtil.onAuthfailedAtSDK(errorCode);
        checkWrongTry(errorCode, errString);
    }
}
