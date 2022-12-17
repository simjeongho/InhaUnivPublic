package kr.ac.inha.android.APP.curl;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import kr.ac.inha.android.APP.library.AppHelper;

public class VolleyQueue {
    private static VolleyQueue instance;
    private RequestQueue requestQueue;
    private static Context ctx;

    private VolleyQueue() {
        ctx = AppHelper.Companion.applicationContext();
        requestQueue = getRequestQueue();
    }

    public static synchronized VolleyQueue getInstance() {
        if (instance == null) instance = new VolleyQueue();
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}
