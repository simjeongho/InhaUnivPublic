package kr.ac.inha.android.APP.FIDO.InhaAPI;

import android.content.Context;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import kr.ac.inha.android.APP.FIDO.model.ResponseMessage;
import kr.ac.inha.android.APP.FIDO.util.ErrorUtil;
import kr.ac.inha.android.APP.FIDO.util.FidoUtils;
import kr.ac.inha.android.APP.WebViewActivity;

public class ApiUtil {
    private static final String TAG = ApiUtil.class.getSimpleName();
    private String mUserName;
    private String mSystemId;
    private Context context;

    public ApiUtil (String mUserName, String mSystemId, Context context){
        this.mUserName = mUserName;
        this.mSystemId = mSystemId;
        this.context = context;
    }

    public void sendSuccessToInhaAPI(String op) {
        JSONObject requestMsg = buildRequestBody();
        Response.Listener<JSONObject> callback = onSuccess(op);
        Response.ErrorListener fallback = onFail(op);

        if(op.equalsIgnoreCase("Reg")) {
            Register register = new Register();
            register.onAuthSuccessWhenReg(requestMsg, callback, fallback);
        }
        if(op.equalsIgnoreCase("Auth")) {
            Auth auth = new Auth();
            auth.onAuthSuccessWhenReg(requestMsg, callback, fallback);
        }
    }

    protected JSONObject buildRequestBody() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("id", mUserName);
        map.put("sid", mSystemId);
        return new JSONObject(map);
    }

    protected Response.Listener<JSONObject> onSuccess(String op) {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String status = response.getString("status");
                    String msg = response.getString("msg");
                    ResponseMessage resp = new ResponseMessage(status, msg);
                    if (!response.getString("status").equalsIgnoreCase("Success")) {
                        ErrorUtil.onErrorAtInhaAPI(context, op, resp);
                    }
                    FidoUtils.showToast(context, resp.getMsg());
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, "JSONException parseOnRequest" + e.getMessage());
                }
            }
        };
    }

    protected Response.ErrorListener onFail(String op) {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                FidoUtils.showToast(context, op + " Fail[" + error.toString() + "]");
            }
        };
    }

    public static void renewRegisterStatusOnWebview(){
        WebViewActivity.wv.post(new Runnable() {
            public void run() {
                WebViewActivity.wv.loadUrl("javascript:this.vueInstance.setAuthRegisterStatus();");
            }
        });
    }
}
