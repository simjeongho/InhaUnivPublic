package kr.ac.inha.android.APP.FIDO.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by WY.SEO on 2017-06-22.
 */

public class FidoUtils {

    public static final String TAG = FidoUtils.class.getSimpleName();

    // FIDO 메시지 요청 생성
    public static JSONObject generateFidoRequestMsg(String op, String userName, String transaction, String systemID, String bioType, String deviceID) {
        String ret = "";
        JSONObject jsonRequestMsg = new JSONObject();

        try {
            JSONObject jsonContextMsg = new JSONObject();
            jsonContextMsg.put("userName", userName);
            jsonContextMsg.put("SYSTEMID", systemID);
            jsonContextMsg.put("BIOTYPE", bioType);
            jsonContextMsg.put("MODEL", Build.MODEL);
            jsonContextMsg.put("token", "");

            if(op.equalsIgnoreCase("Reg"))
                jsonContextMsg.put("DEVICE", deviceID);
            else {
                String deviceHash = getSHA256(deviceID);
                jsonContextMsg.put("DEVICEHASH", deviceHash.toUpperCase());
            }

            // 전자서명(transaction) 할 경우
            if(!transaction.isEmpty())
                jsonContextMsg.put("transaction", transaction);

            jsonRequestMsg.put("op", op);
            jsonRequestMsg.put("context", jsonContextMsg.toString());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return jsonRequestMsg;
        }
    }

    public static String getUafRequest(JSONObject response) {
        String msg = "";
        try {
            if (response.getInt("statusCode") == 1200) {
                msg = response.getString("uafRequest");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            return msg;
        }
    }

    public static String getSHA256(String str){
        String SHA = "";
        try{
            MessageDigest sh = MessageDigest.getInstance("SHA-256");
            sh.update(str.getBytes());
            byte byteData[] = sh.digest();
            StringBuffer sb = new StringBuffer();
            for(int i = 0 ; i < byteData.length ; i++){
                sb.append(Integer.toString((byteData[i]&0xff) + 0x100, 16).substring(1));
            }
            SHA = sb.toString();

        }catch(NoSuchAlgorithmException e){
            e.printStackTrace();
            SHA = null;
        }

        return SHA;
    }

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void showToastLong(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Set Preference
     */
    public static void setPreferences(Context context, String key, String value) {
        SharedPreferences p = context.getSharedPreferences("pref", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = p.edit();
        editor.putString(key, value);
        editor.commit();
    }

    /**
     * Get Preference
     */
    public static String getPreferences(Context context, String key) {
        SharedPreferences p = context.getSharedPreferences("pref", context.MODE_PRIVATE);
        p = context.getSharedPreferences("pref", context.MODE_PRIVATE);
        return p.getString(key, "");
    }

    /**
     * Delete Preference
     */
    public static void removePreferences(Context context, String key) {
        SharedPreferences p = context.getSharedPreferences("pref", context.MODE_PRIVATE);
        p = context.getSharedPreferences("pref", context.MODE_PRIVATE);

        if(p.getString(key, "").isEmpty()) {
            //Log.d(TAG, "EMPTY");
        } else {
            SharedPreferences.Editor editor = p.edit();
            editor.remove(key);
            editor.commit();
            //Log.d(TAG, "REMOVE");
        }
    }

    /**
     * Clear Preference
     */
    public static void clearPreferences(Context context, String key) {
        SharedPreferences p = context.getSharedPreferences("pref", context.MODE_PRIVATE);
        p = context.getSharedPreferences("pref", context.MODE_PRIVATE);

        if(p.getString(key, "").isEmpty()) {
            //Log.d(TAG, "EMPTY");
        } else {
            SharedPreferences.Editor editor = p.edit();
            editor.clear();
            editor.commit();
            //Log.d(TAG, "CLEAR");
        }
    }
}
