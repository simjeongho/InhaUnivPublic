package kr.ac.inha.android.APP.library;

import android.app.Activity;
import android.content.Intent;
import android.webkit.JavascriptInterface;

import kr.ac.inha.android.APP.FIDO.model.RequestedAuth;
import kr.ac.inha.android.APP.FIDO.router.FidoRouter;
import kr.ac.inha.android.APP.FIDO.util.JsonUtil;
import kr.ac.inha.android.APP.WebViewActivity;
import kr.ac.inha.android.APP.s1.S1router;

public class WebViewInterface {

    private final static String TAG = WebViewInterface.class.getSimpleName();

    private Activity mActivity;

    public WebViewInterface(Activity activity) {
        this.mActivity = activity;
    }

    @JavascriptInterface
    public void reg(String message) {
        RequestedAuth requestedOne = JsonUtil.parseOnRequest(message);

        FidoRouter fidoRouter = new FidoRouter();
        Intent intent =  fidoRouter.routeToReg(mActivity, requestedOne);

        mActivity.startActivity(intent);
    }

    @JavascriptInterface
    public void auth(String message) {
        RequestedAuth requestedOne = JsonUtil.parseOnRequest(message);

        FidoRouter fidoRouter = new FidoRouter();
        Intent intent =  fidoRouter.routeToAuth(mActivity, requestedOne);

        mActivity.startActivity(intent);
    }

    @JavascriptInterface
    public void sOneBridge(String message) {
        S1router s1router = new S1router();
        s1router.routeToS1MobileBridge(mActivity.getApplicationContext(), message);
    }

    /**
     * 자바스크립트에 alert 메시지 띄우기
     * @param msg alert 메시지
     */
    public void javascriptAlert(final String msg) {
        WebViewActivity.wv.post(new Runnable() {
            @Override
            public void run() {
                WebViewActivity.wv.loadUrl("javascript:alert('"+msg+"');");
            }
        });
    }
}
