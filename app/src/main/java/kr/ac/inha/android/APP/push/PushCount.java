package kr.ac.inha.android.APP.push;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import kr.ac.inha.android.APP.curl.VolleyUtil;
import kr.ac.inha.android.APP.library.AppHelper;
import kr.ac.inha.android.APP.library.InhaUtility;

public class PushCount {
    private static final String TAG = PushCount.class.getSimpleName();
    private static final Context ctx = AppHelper.Companion.applicationContext();
    private static final SharedPreferences mPref = PreferenceManager.getDefaultSharedPreferences(ctx);

    public static void setPushCountBadge() {
        String devicetoken = mPref.getString("devicetoken", "");
        String url = InhaUtility.ROOTURL + "/push/api/push_badge_cnt.aspx";
        url = url + "?phone_regid=" + devicetoken;
        Response.Listener<JSONObject> callback = onSuccess();
        Response.ErrorListener fallback = onFail();
        VolleyUtil.get(url, null, callback, fallback);
    }

    private static Response.Listener<JSONObject> onSuccess() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, response.toString());

                int pushCnt = 0;
                try {
                    pushCnt = response.getInt("pushCnt");
                    updateBadgeCount(pushCnt);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        };
    }

    protected static Response.ErrorListener onFail() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, error.getMessage());
            }
        };
    }

    public static void updateBadgeCount(int count) {
        try {
            Intent intent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
            intent.putExtra("badge_count",count);
            intent.putExtra("badge_count_package_name", ctx.getPackageName());
            intent.putExtra("badge_count_class_name","kr.ac.inha.android.APP.MainActivity");
            ctx.sendBroadcast(intent);
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage());
        };
    }
}
