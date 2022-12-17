package kr.ac.inha.android.APP.FIDO.InhaAPI;

import com.android.volley.Response;

import org.json.JSONObject;

import kr.ac.inha.android.APP.curl.VolleyUtil;
import kr.ac.inha.android.APP.library.InhaUtility;

public class Register {
    public static void onAuthSuccessWhenReg(JSONObject requestMsg,
                                            Response.Listener<JSONObject> callback,
                                            Response.ErrorListener fallback) {
        String url = InhaUtility.ROOTURL + "/otp/api/v1/register/register_up_fin.aspx";
        VolleyUtil.postToAspDotNet2(url, requestMsg, callback, fallback);
    }
}
