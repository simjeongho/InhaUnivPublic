package kr.ac.inha.android.APP.FIDO.util;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import kr.ac.inha.android.APP.FIDO.model.Constants;
import kr.ac.inha.android.APP.FIDO.model.RequestedAuth;
import kr.ac.inha.android.APP.FIDO.model.ResponseMessage;

import static kr.ac.inha.android.APP.library.InhaUtility.TAG;

public class JsonUtil {
    public static RequestedAuth parseOnRequest(String message) {
        String id = "";
        String type = "";
        String sid = Constants.SYSTEMID.getValue();

        JSONObject jsonObject = getJsonObject(message);

        try {
            id = jsonObject.getString("id");
            type = jsonObject.getString("type");
            if (jsonObject.has("sid") == true)  sid = jsonObject.getString("sid");
        } catch (JSONException e) {
            Log.e(TAG, "JSONException parseOnRequest" + e.getMessage());
            id = "Error";
            type = e.getMessage();
        }
        return new RequestedAuth(id, type, sid);
    }

    public static ResponseMessage parseResponseMessage(String response) {
        JSONObject jsonObject = getJsonObject(response);
        ResponseMessage responseMessage = null;
        try {
            String status = jsonObject.getString("status");
            String msg = jsonObject.getString("msg");
            responseMessage = new ResponseMessage(status, msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return responseMessage;
    }

    public static JSONObject getJsonObject(String message) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(message);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException getJsonObject" + e.getMessage());
            e.getStackTrace();
        } finally {
            return jsonObject;
        }
    }

    public static int getStatusCode(JSONObject jsonObject) {
        int statusCode = 0;
        try {
            statusCode =jsonObject.getInt("statusCode");
        } catch (JSONException e) {
            statusCode = -1;
            Log.e(TAG, "JSONException getStatusCode" + e.getMessage());
            e.getStackTrace();
        }
        return statusCode;
    }

    public static String makeRequestBody(JsonObject jsonObject) {
        Gson gson = new Gson();
        String body = gson.toJson(jsonObject);
        try {
            body = URLEncoder.encode(body, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return body;
    }

    public static JsonObject makeJsonObject(Map<String, String> map) {
        JsonObject object = new JsonObject();
        for (String key : map.keySet()) {
            object.addProperty(key, map.get(key));
        }
        return object;
    }
}
