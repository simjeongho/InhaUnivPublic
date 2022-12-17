package kr.ac.inha.android.APP.curl;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class VolleyUtil {
    protected static void sendRequest(JsonObjectRequest jsonObjectRequest) {
        VolleyQueue.getInstance().addToRequestQueue(jsonObjectRequest);
    }

    public static void get(String url
            , JSONObject jsonMsg
            , Response.Listener<JSONObject> callback
            , Response.ErrorListener fallback) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, jsonMsg, callback, fallback);

        sendRequest(jsonObjectRequest);
    }

    public static void post(String url
            , JSONObject jsonMsg
            , Response.Listener<JSONObject> callback
            , Response.ErrorListener fallback) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, jsonMsg, callback, fallback);

        sendRequest(jsonObjectRequest);
    }

    public static void postToAspDotNet2(String url
            , JSONObject jsonMsg
            , Response.Listener<JSONObject> callback
            , Response.ErrorListener fallback) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, jsonMsg, callback, fallback) {
            @Override
            public Map getHeaders() throws AuthFailureError {
                Map  params = new HashMap();
                params.put("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");

                return params;
            }
        };

        sendRequest(jsonObjectRequest);
    }
}
