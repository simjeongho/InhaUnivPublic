package kr.ac.inha.android.APP.FIDO.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.ssenstone.stonepass.libstonepass_sdk.SSUserManager;

import org.json.JSONException;
import org.json.JSONObject;

import kr.ac.inha.android.APP.FIDO.InhaAPI.ApiUtil;
import kr.ac.inha.android.APP.FIDO.model.Constants;
import kr.ac.inha.android.APP.FIDO.util.ErrorUtil;
import kr.ac.inha.android.APP.FIDO.util.FidoLibraryBuilder;
import kr.ac.inha.android.APP.FIDO.util.FidoResponseChecker;
import kr.ac.inha.android.APP.FIDO.util.FidoUtils;
import kr.ac.inha.android.APP.R;
import kr.ac.inha.android.APP.curl.RequestUtils;
import kr.ac.inha.android.APP.curl.VolleyUtil;

public class PinCode extends AppCompatActivity {

    private static final String TAG = PinCode.class.getSimpleName();

    TextView                                mTvTitle;
    EditText                                mInputNumber;
    EditText                                mNewInputNumber;
    Button                                  mAuthBtn;
    private ProgressBar pb;

    private String mOp;
    private String mUserName;
    private String mDuid;
    private String mSystemId = Constants.SYSTEMID.getValue();
    private final String mBioType = Constants.PINCODE.getValue();
    private final String mServerInfo = Constants.SERVERINFO.getValue();
    private int wrongTryCounter = 0;

    // StonePASS Library
    private SSUserManager mSSUserManager;
    private SSUserManager.SSFidoListener    mFidoListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayoutResource();
        initData();
        initSSUserManager();
        setConfirmInputVisibility();
        setListner();
        setPinCodeLengthInfoText((EditText)findViewById(R.id.pinCodeEdit));
        mInputNumber.performClick();
    }

    protected void setLayoutResource() {
        setContentView(R.layout.activity_pincode);
        pb = (ProgressBar) findViewById(R.id.progress);
        mTvTitle = (TextView) findViewById(R.id.tvTitle);
        mInputNumber = (EditText) findViewById(R.id.pinCodeEdit);
        mNewInputNumber = (EditText) findViewById(R.id.newPinCodeEdit);
        mAuthBtn = (Button) findViewById(R.id.pinAuthBtn);
    }

    /**
     * StonePASS PIN 인증 초기화 함수
     */
    protected void initData() {
        Bundle bundle = getIntent().getExtras();
        if(bundle == null) return;
        mOp = bundle.getString("operation");
        mUserName = bundle.getString("id");
        if(mOp.equalsIgnoreCase("Auth")) mSystemId = bundle.getString("systemId");
    }

    private void setConfirmInputVisibility() {
        if (mOp.equals("Auth")) {
            mNewInputNumber.setVisibility(View.GONE);
        }
    }

    protected void setListner() {
        mAuthBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAuthProcess();
            }
        });
        mInputNumber.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (i == KeyEvent.KEYCODE_ENTER
                        && keyEvent.getAction() == KeyEvent.ACTION_UP) startAuthProcess();
                return false;
            }
        });
        mNewInputNumber.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (i == KeyEvent.KEYCODE_ENTER
                        && keyEvent.getAction() == KeyEvent.ACTION_UP) startAuthProcess();
                return false;
            }
        });
    }

    protected void startAuthProcess() {
        if(!isTherePinInput()) return;
        if (!is6number()) return;
        pb.setVisibility(View.VISIBLE);
        switch (mOp) {
            case "Reg":
                if (checkConfirmPin()) ssenstoneFIDO(mOp, mUserName);
                break;
            case "Auth":
                ssenstoneFIDO(mOp, mUserName);
                break;
        }
    }

    protected void initSSUserManager() {
        FidoLibraryBuilder fidoLib = new FidoLibraryBuilder(this);
        mSSUserManager = fidoLib.getSSUserManager();
        mDuid = fidoLib.getDeviceID();
    }

    /**
     * SSenStone FIDO (해지 시 PIN 검중 필요)
     * @param op			Operation (Reg/Auth/Dereg(삭제는 미사용))
     * @param userName		user Name
     */
    protected void ssenstoneFIDO(String op, String userName) {
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
                String pin = mInputNumber.getText().toString();
                FidoProcess(op, userName, msg, pin);
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

    private boolean isTherePinInput() {
        String pin = mInputNumber.getText().toString();
        if(pin.isEmpty()) {
            Toast.makeText(this, "Enter Pin Code", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    // FIDO 처리
    void FidoProcess(final String op, final String userName, String message, String fcode) {
        Log.d(TAG, "[FidoProcess] " + op);
        Log.d(TAG, "[FidoProcess] " + userName);
        Log.d(TAG, "[FidoProcess] " + message);
        setFidoListener(op);
        mSSUserManager.setFidoListener(mFidoListener, this, userName, message, fcode, mBioType, false);
    }

    protected void setPinCodeLengthInfoText (EditText textInput) {
        textInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) return;
                if (textInput.getText().toString().trim().length() != 6)
                    textInput.setError("6자리로 넣으세요");
            }
        });
    }

    protected Boolean is6number () {
        String pinNumber = mInputNumber.getText().toString();
        if (pinNumber.trim().length() == 6) return true;
        FidoUtils.showToast(getApplicationContext(), "PIN 번호는 6자리 숫자로 입력해주세요");
        return false;
    }

    private boolean checkConfirmPin() {
        String pinNumber = mInputNumber.getText().toString();
        String confirmPinNumber = mNewInputNumber.getText().toString();

        if (confirmPinNumber.equals(pinNumber)) {
            return true;
        } else {
            pb.setVisibility(View.GONE);
            FidoUtils.showToast(getApplicationContext(), "PIN이 일치하지 않습니다");
            return false;
        }
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

    private void onAuthFailed(int errorCode, String errString) {
        pb.setVisibility(View.GONE);
        ErrorUtil.onAuthfailedAtSDK(errorCode);
        checkWrongTry(errorCode, errString);
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
                FidoUtils.showToast(getApplicationContext(), op + " Fail[" + error.toString() + "]");
            }
        };
    }
}
