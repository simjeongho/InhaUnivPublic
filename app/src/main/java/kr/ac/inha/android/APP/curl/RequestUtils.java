package kr.ac.inha.android.APP.curl;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.util.HashMap;

import kr.ac.inha.android.APP.FIDO.util.FidoUtils;

public class RequestUtils {
    public static StringRequest buildGetStringRequest(String url, HashMap<String, String> params) {
        String urlWithParams = url + buildGetParameter(params);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlWithParams,
                new Response.Listener<String>() {
                    public void onResponse(String response) {
                        System.out.println(response);
                    }
                },
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                }) {
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };

        return stringRequest;
    }

    private static String buildGetParameter(HashMap<String, String> params) {
        if (params == null) return "";
        if (params.size() == 0) return "";

        StringBuilder builder = new StringBuilder();
        builder.append("?");
        for ( String key : params.keySet() ) {
            builder.append(key);
            builder.append("=");
            builder.append(params.get(key));
            builder.append("&");
        }
        builder.deleteCharAt(builder.length() - 1);

        return builder.toString();
    }

    public static JSONObject buildRequestToSsenstone(String op, String userName, String systemId, String mBioType, String mDuid) {
        JSONObject requestMsg = FidoUtils.generateFidoRequestMsg(op, userName, "", systemId, mBioType, mDuid);

        return requestMsg;
    }
}
