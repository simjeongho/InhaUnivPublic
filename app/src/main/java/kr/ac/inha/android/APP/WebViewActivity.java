package kr.ac.inha.android.APP;


import android.Manifest;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.net.Uri;
import android.net.http.SslError;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.preference.PreferenceManager;
import android.provider.Browser;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.GeolocationPermissions;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.SslErrorHandler;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.squareup.seismic.ShakeDetector;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import kr.ac.inha.android.APP.FIDO.util.FidoLibraryBuilder;
import kr.ac.inha.android.APP.TimeTable.TimeTable;
import kr.ac.inha.android.APP.Webview.UrlUtil;
import kr.ac.inha.android.APP.library.GPSTracker;
import kr.ac.inha.android.APP.library.InhaUtility;
import kr.ac.inha.android.APP.library.WebViewInterface;
import kr.ac.inha.android.APP.push.PushCount;
import kr.ac.inha.android.APP.push.showMsg;
import kr.ac.inha.android.APP.vcard.VCard;


public class WebViewActivity extends AppCompatActivity implements ShakeDetector.Listener {
	public static Context WebViewContext;
	public static WebView wv;
	ProgressBar pb;
	AlertDialog dialog;
	View dialogView;
	AlertDialog.Builder builder;
	String loadingUrl;
	ImageView back, next, refresh, home;
	CookieManager cookieManager;
	String Cookies;
	SharedPreferences mPref;
	GPSTracker gps;
	Timer timer;
	NotificationManager notificationManager;//push ?????????(17.02.15)

	public String tempUrlForExternalStorage = "";

	ShakeDetector shakeDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			WebView.setWebContentsDebuggingEnabled(true);
		}

		WebViewContext = this;

		super.onCreate(savedInstanceState);
		setContentView(R.layout.webview);
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);//push ????????? ?????????(17.02.15)

		mPref = PreferenceManager.getDefaultSharedPreferences(this);

		builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		dialogView = inflater.inflate(R.layout.dialog, null);
		builder.setView(dialogView);


		back = (ImageView) findViewById(R.id.btn_back);
		next = (ImageView) findViewById(R.id.btn_next);
		refresh = (ImageView) findViewById(R.id.btn_refresh);
		home = (ImageView) findViewById(R.id.btn_home);
		pb = (ProgressBar) findViewById(R.id.progress);

		wv = (WebView) findViewById(R.id.webview);
		wv.getSettings().setJavaScriptEnabled(true);// ??? ????????? ?????????????????? ????????????
		wv.getSettings().setSupportMultipleWindows(false);//??? ??? ????????? ??????
		wv.getSettings().setDomStorageEnabled(true); // ?????? ????????? ?????? ??????
		wv.getSettings().setJavaScriptCanOpenWindowsAutomatically(true); // ?????????????????? ?????? ????????? ?????????
		//viewport setting
		WebSettings WVsettings = wv.getSettings();
		WVsettings.setUseWideViewPort(true);//?????? ????????? ????????? ?????? ??????
		WVsettings.setLoadWithOverviewMode(true); // ?????? ?????? ?????? ??????
		//webview zoom
		WVsettings.setBuiltInZoomControls(true); // ?????? ?????? ?????? ?????? ??????
		WVsettings.setSupportZoom(true); // ?????? ??? ??????
		wv.clearCache(true);

		wv.setWebChromeClient(wcclient);
		wv.setWebViewClient(wvclient);

		//final Context myApp = this;
		CookieSyncManager.createInstance(wv.getContext());
		cookieManager = CookieManager.getInstance();
		cookieManager.acceptCookie();
		CookieSyncManager.getInstance().startSync();

		loadingUrl =  getIntent().getStringExtra("url");
		
		back.setOnClickListener(mOnclickListener);
		next.setOnClickListener(mOnclickListener);
		refresh.setOnClickListener(mOnclickListener);
		home.setOnClickListener(mOnclickListener);

		initShakeSensor();

		wv.addJavascriptInterface(new WebViewInterface(WebViewActivity.this), "app");

		wv.loadUrl(loadingUrl);
	}

	View.OnClickListener mOnclickListener = new View.OnClickListener() {
		public void onClick(View v) {
			WebBackForwardList historyList = wv.copyBackForwardList();
			switch (v.getId()) {
				case R.id.btn_back:
					if (!wv.canGoBack() && timer == null) break;
					if (!wv.canGoBack() && timer != null) {
						timer.cancel();
						timer.purge();
						timer = null;
						break;
					}
					String backTargetUrl = historyList.getItemAtIndex(historyList.getCurrentIndex() - 1).getUrl();
					if (backTargetUrl.contains("device_authentication.aspx")) {
						wv.loadUrl(UrlUtil.getPortal());
					} else if (backTargetUrl.contains("mobile_id_stu.aspx")) {
						wv.loadUrl(UrlUtil.getPortal());
					} else if (wv.getUrl().contains("Login.aspx")) {
						wv.clearHistory();
						wv.loadUrl(UrlUtil.getMobileHome(getApplicationContext()));
					} else if (backTargetUrl.contains("index.aspx")) {
						pb.setVisibility(View.VISIBLE); // ProgressBar ??????
						wv.loadUrl(UrlUtil.getMobileHomeWithDeviceToken(getApplicationContext()));
					} else {
						wv.goBack();
					}
					break;
				case R.id.btn_next:
					if (!wv.canGoForward()) break;
					String forwardTargetUrl = historyList.getItemAtIndex(historyList.getCurrentIndex() + 1).getUrl();
					if (forwardTargetUrl.contains("device_authentication.aspx")) {
						wv.loadUrl(UrlUtil.getPortal());
					} else if (forwardTargetUrl.contains("mobile_id_stu.aspx")) {
						wv.loadUrl(UrlUtil.getPortal());
					} else {
						wv.goForward();
					}
					break;
				case R.id.btn_refresh:
					pb.setVisibility(View.VISIBLE);
					wv.reload();
					break;
				case R.id.btn_home:
					pb.setVisibility(View.VISIBLE); // ProgressBar ??????
					//devicetoken ????????????
					wv.loadUrl(UrlUtil.getMobileHomeWithDeviceToken(getApplicationContext()));
					break;
			}
		}
	};
	WebChromeClient wcclient = new WebChromeClient() {

		@Override
		public void onGeolocationPermissionsShowPrompt(String origin, Callback callback) {
			// Should implement this function.
			final String myOrigin = origin;
			final GeolocationPermissions.Callback myCallback = callback;
			AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);

			builder.setTitle("??????");
			builder.setMessage("???????????? ?????? ?????? ????????? ??????????????? ?????????.");
			builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					myCallback.invoke(myOrigin, true, true);
				}
			});

			builder.setNegativeButton("?????? ??? ???", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					myCallback.invoke(myOrigin, false, false);
				}
			});

			AlertDialog alert = builder.create();
			alert.show();
		}
	};

	WebViewClient wvclient = new WebViewClient() {
		@Override
		public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
			// TODO Auto-generated method stub
			super.doUpdateVisitedHistory(view, url, isReload);
		}

		// SSL ??????????????? ??????
		@Override
		public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {

			final AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);
			String message;

			switch (error.getPrimaryError()) {
				case SslError.SSL_EXPIRED:
					message = "?????? ???????????? ?????????????????????.";
					break;
				case SslError.SSL_IDMISMATCH:
					message = "?????? ????????? ID??? ???????????? ????????????.";
					break;
				case SslError.SSL_NOTYETVALID:
					message = "?????? ???????????? ?????? ???????????? ????????????.";
					break;
				case SslError.SSL_UNTRUSTED:
				default:
					message = "?????? ???????????? ????????? ??? ????????????.";
					break;
			}
			message += "\n?????? ?????????????????????????";
			builder.setTitle("SSL Certificate Error");
			builder.setMessage(message);

			builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					handler.proceed();
				}
			});
			builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					handler.cancel();
				}
			});
			final AlertDialog dialog = builder.create();
			dialog.show();
		}

		// ????????? ?????? ??? ?????? ??????
		@SuppressWarnings("deprecation")
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Log.i("test", url);
			view.clearFocus();
			pb.setVisibility(View.VISIBLE); // ProgressBar ??????

			if(timer != null){
				timer.cancel();
				timer.purge();
			}

			//????????? ????????? devicetoken ????????????
			if (url.contains("login/Login.aspx")) {
				if(mPref.getString("devicetoken", "").equals("")){
					//devicetoken??? ?????? ??? ?????? ??????
					view.loadUrl(url);
					return true;
				}
				else {
					//devicetoken??? ????????? ?????? ????????????
					url = url + "&phone_regid="+mPref.getString("devicetoken", "")+"&phone_type=Android";
					view.loadUrl(url);
					return true;
				}
			}

			//main ????????? devicetoken ????????????
			if (url.contains(InhaUtility.ROOTURL+"/index.aspx")) {
				if(mPref.getString("devicetoken", "").equals("")){
					view.loadUrl(url);
					return true;
				}
				else{
					url =  url + "?phone_regid="+mPref.getString("devicetoken", "")+"&phone_type=Android";
					view.loadUrl(url);
					return true;
				}
			}

			// Push??? ?????? ??????
			if (url.contains("Push_main.aspx")) { // ??? ??????
				url = InhaUtility.ROOTURL+ "/push/Push_main.aspx?phone_regid="+mPref.getString("devicetoken", "")+"&phone_type=Android";
				view.loadUrl(url);
				return true;
			}

			//???????????? ???????????? ??? ?????? ??????(17.02.15)
			if(url.contains("/push/push_receivedroom_main.aspx")){
				notificationManager.cancelAll();
				view.loadUrl(url);
				return true;
			}

			if(url.contains("/vcard.aspx")){
				Intent intent = new Intent(WebViewActivity.this, ContactsSendActivity.class);
				intent.putExtra("url", url);
				startActivity(intent);
				return true;
			}
			//qr reader ?????? url
			if(url.contains("/Dorm/QRreader.aspx")){
				Intent intent = new Intent(WebViewActivity.this, QrReader.class);
				intent.putExtra("url", url);
				startActivity(intent);
				return true;
			}
			if(url.contains("/Dorm/dormLaundryQRresult.aspx")){
				//notificationManager.cancelAll();
				view.loadUrl(url);
				return true;
			}
			//Push ???????????? ??? Url??? ????????? ??????????????? ??? ??????
			if(url.toUpperCase().startsWith("SCH:")){
				try {
					url = url.substring(4);
					String[] data = url.split("\\|"); //0: title, 1: ?????????, 2: ?????????
					data[0] = URLDecoder.decode(data[0], "UTF-8");

					showMsg tmp = new showMsg();
					tmp.saveSchedule(data[0], data[1], data[2], getApplicationContext());
				} catch (Exception e) {
					// TODO ?????? ????????? catch ??????
					// TODO ?????? ????????? catch ??????
					e.printStackTrace();
				}
				Toast.makeText(WebViewActivity.this, "????????? ?????? ???????????????.", Toast.LENGTH_SHORT).show();
				pb.setVisibility(View.GONE);
				return true;
			} else if(url.toUpperCase().startsWith("URL:")){
				url = url.substring(4); // ????????? ????????? url:??????
				if(!url.contains("http://") && !url.contains("https://")){ //http:// ????????????
					url = "https://" + url; //
				}
				Intent i = new Intent(Intent.ACTION_VIEW);
				Uri u = Uri.parse(url);
				i.setData(u);
				startActivity(i); // http://??? ???????????? ????????? ??????

				Toast.makeText(WebViewActivity.this, "?????????????????? ???????????????.", Toast.LENGTH_SHORT).show();
				pb.setVisibility(View.GONE);
				return true;
			}

			else if(url.toUpperCase().startsWith("APP:")){
				String package_name = url.substring(4); //app??? ?????? app:??? ?????????
				if(package_name.toUpperCase().startsWith("KR.COURSEMOS.ANDROID2"))
				{ Toast.makeText(WebViewActivity.this, "???????????? ????????? ???????????????.", Toast.LENGTH_SHORT).show();
				PackageManager packageManager = getPackageManager();
				Intent intent = packageManager.getLaunchIntentForPackage(package_name);
				startActivity(intent);
				return true;
				}
				else if(package_name.toUpperCase().startsWith("KR.CO.CRYPTOLAB.PANDEMICGUARDGG"))
				{
					try {
						Toast.makeText(WebViewActivity.this, "????????? ????????? ???????????????.", Toast.LENGTH_SHORT).show();
						PackageManager packageManager = getPackageManager();
						Intent intent = packageManager.getLaunchIntentForPackage(package_name);
						startActivity(intent);
						return true;
					} catch(Exception e)
					{
						Toast.makeText(WebViewActivity.this, "????????? ?????? ????????? ??????????????????", Toast.LENGTH_SHORT).show();
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse("market://details?id=" + package_name));
						startActivity(intent);
						return true;
					}
				}
				else{
					try {
						PackageManager packageManager = getPackageManager();
						Intent intent = packageManager.getLaunchIntentForPackage(package_name);
						Toast.makeText(WebViewActivity.this, "?????? ????????? ???????????????.", Toast.LENGTH_SHORT).show();
						startActivity(intent);

						return true;
					} catch (Exception e) {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse("market://details?id=" + package_name));
						startActivity(intent);
						return true;
					}
				}
			}

			else if(url.contains("pandemicGuardGG")){
				String package_name = "kr.co.cryptolab.pandemicGuardGG"; //app??? ?????? app:??? ?????????

				try {
					PackageManager packageManager = getPackageManager();
					Intent intent = packageManager.getLaunchIntentForPackage(package_name);
					startActivity(intent);
					Toast.makeText(WebViewActivity.this, "????????? ?????? ????????? ????????? ???????????????.", Toast.LENGTH_SHORT).show();
					return true;
				}
				catch(Exception e){
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse("market://details?id=" + package_name));
					startActivity(intent);
					return true;
				}
			}
			else if(url.toUpperCase().startsWith("VCARD:")){
				try {
					url = url.substring(6);
					String[] vCardDataArr = url.split("\\|"); //0: ??????, 1: ????????????, 2: email
					String name = URLDecoder.decode(vCardDataArr[0], "UTF-8");
					String phoneNumber = vCardDataArr[1];

					Intent intent = new Intent(Intent.ACTION_INSERT);
					intent.setType( ContactsContract.Contacts.CONTENT_TYPE );
					intent.putExtra( ContactsContract.Intents.Insert.NAME, name );
					intent.putExtra( ContactsContract.Intents.Insert.PHONE, phoneNumber );

					if(vCardDataArr.length == 3){
						String email = vCardDataArr[2];
						intent.putExtra( ContactsContract.Intents.Insert.EMAIL, email );
					}
					startActivity( intent );
				} catch (UnsupportedEncodingException e) {
					// TODO ?????? ????????? catch ??????
					e.printStackTrace();
				}
				Toast.makeText(WebViewActivity.this, "???????????? ?????? ???????????? ???????????????.", Toast.LENGTH_SHORT).show();
				pb.setVisibility(View.GONE);
				return true;
			}
			//????????? ??????/??????
			else if(url.toUpperCase().startsWith("TT:")){
				url = url.substring(3);
				if(url.toUpperCase().startsWith("INSERT")){
					TimeTable tt = new TimeTable(WebViewContext.getApplicationContext());
					tt.checkCalendarPermissionBeforeDownload(url);

					pb.setVisibility(View.GONE);
					return true;
				}
				else if(url.toUpperCase().startsWith("DELETE")){
					TimeTable tt = new TimeTable(WebViewContext.getApplicationContext());
					tt.checkCalendarPermissionBeforeDelete();

					pb.setVisibility(View.GONE);
					return true;
				}
			}

			// sms, tel, mailt???????????? ?????????
			if(url.startsWith("sms:") || url.startsWith("tel:") || url.startsWith("mailto:") )
			{
				Intent intent1 = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				intent1.addCategory(Intent.CATEGORY_BROWSABLE);
				intent1.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
				if (url.startsWith("sms:")) {
					Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
					startActivity(i);
					return true;
				} else if (url.startsWith("tel:")) {
					Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
					startActivity(i);
					return true;
				} else if (url.startsWith("mailto:")) {
					Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
					startActivity(i);
					return true;
				}
			} else if (url.startsWith("intent://")) {
				Intent intent = null;
				try {
					intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
					if (intent != null) startActivity(intent);
				} catch	(URISyntaxException e) {
					int errorCode = e.getIndex();
					String description = "Message: " + e.getMessage() + "/ Reason: " + e.getReason();
					view.loadUrl("file:///android_asset/www/error.html?errorCode=" + errorCode + "&errorDescription=" + description);
				} catch	(ActivityNotFoundException e) {
					String packageName = intent.getPackage();
					if (!packageName.equals("")) {
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
					}
				}
				return true;
			}

			if ((!url.startsWith("http:")) && (!url.startsWith("https:")))
			{
				view.loadUrl(url);
			}else if( url.contains("device_authentication.aspx")){
				if(checkAttendance() == false){
					Intent Intent = new Intent(WebViewActivity.this, showMsg.class);
					Bundle b = new Bundle();
					b.putString("title","???????????? ?????????????????????.");
					b.putString("msg", "????????? ?????? Wi-Fi(INHA-WLAN2)??? ?????????????????? ????????? ?????? GPS??? (???)??????????????????.");
					b.putString("dialogtype", "default");

					Intent.putExtras(b);

					startActivity(Intent);
					//Toast.makeText(WebViewActivity.this, "???????????? ?????????????????????. Wifi(INHA-WLAN2)??? ?????????????????? GPS??? ??? ??? ?????? ???????????????.", 0).show();
					pb.setVisibility(View.GONE);

					if(gps != null && gps.canGetLocation()){
						url = InhaUtility.ROOTURL + "/map/map3view.aspx";
						view.loadUrl(url);
					}
				} else {
					//Toast.makeText(WebViewActivity.this, "????????? ??????", 0).show();
					url = url + "?phone_regid=" + mPref.getString("devicetoken", "") + "&phone_type=Android";
					view.loadUrl(url);
				}

				TimerTask task = new TimerTask(){
                    @Override
					public void run() {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                wv.loadUrl(InhaUtility.ROOTURL + "/index.aspx");
                                onPageFinished(wv, InhaUtility.ROOTURL + "/index.aspx");
                            }
                        });

						try {
							Intent Intent = new Intent(WebViewActivity.this, showMsg.class);;
							Bundle b = new Bundle();
							b.putString("title","??????");
							b.putString("msg", "5??? ????????? ????????? ????????? ???????????????.");
							b.putString("dialogtype", "default");

							Intent.putExtras(b);

							startActivity(Intent);

							timer.cancel();
							timer.purge();

							timer = null;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				timer = new Timer(); //guntae.aspx ?????? ??????
				timer.schedule(task, 300 * 1000); // 5??? ?????? ??????
				return true;
			}
			else if( url.contains("mobile_id_stu.aspx")){//????????? ?????????
				url = url + "?phone_regid=" + mPref.getString("devicetoken", "") + "&phone_type=Android";
				view.loadUrl(url);
				return true;
			}
			else if (url.contains("otp/otp.aspx")) {
				FidoLibraryBuilder fidoLib = new FidoLibraryBuilder(WebViewContext);
				String fidoDeviceId = fidoLib.getDeviceID();
				url = InhaUtility.ROOTURL + "/otp/otp.aspx?fido_device_id=" + fidoDeviceId;
				view.loadUrl(url);
				return true;
			} else if( url.contains("device_auth.aspx")){
				//Toast.makeText(WebViewActivity.this, "device_auth.aspx", 0).show();
				url = url + "?phone_regid=" + mPref.getString("devicetoken", "") + "&phone_type=Android";
				view.loadUrl(url);
				return true;
			}
			else if(url.contains("inhanuri") || url.contains("user/inhauniversity")){
				Uri uriUrl = Uri.parse(url);
				Intent launchBrowser = new Intent(Intent.ACTION_VIEW,uriUrl);
				startActivity(launchBrowser);
				return true;
			}
			else if (url.contains("market.android.com")) { // ?????? ??????
				String pname = Uri.parse(url).getQueryParameter("id"); // ???????????? ??????

				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("market://details?id=" + pname));
				startActivity(intent); // ???????????? ?????? ???
			}

			else if (url.contains("www.youtube.com")) { // ????????? ????????? ??????
				try {
					getPackageManager().getApplicationInfo("com.google.android.youtube", PackageManager.GET_META_DATA); // ????????? ??? ?????? ?????? ??????
					String video_id = Uri.parse(url).getQueryParameter("v"); // url?????? ?????????ID ??????
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + video_id));
					startActivity(intent); // ????????? ?????? ????????? ????????? ??????
				}
				catch (Exception e) { // ????????? ?????? ?????????
					view.loadUrl(url); // ???????????? ??????
				}
			}
			else if (url.contains("FileGet") || url.contains("downloadHome") || url.contains("download") || url.contains("download_process.aspx") || url.contains("staff_vcard")) {
				tempUrlForExternalStorage = url;
				VCard vCard = new VCard(WebViewContext.getApplicationContext());
				vCard.checkExternalStoragePermission(tempUrlForExternalStorage);
				String finm = vCard.getFinm();

				if(dialog == null)
					dialog = builder.create();

				TextView name = (TextView) dialogView.findViewById(R.id.txt_name);
				name.setText(finm);
				TextView path = (TextView) dialogView.findViewById(R.id.txt_path);
				path.setText(InhaUtility.DOWNLOADPATH);
				dialog.show();

				new WebViewActivity.Download(url, finm).start();

				pb.setVisibility(View.GONE);

				tempUrlForExternalStorage = "";
			}
			else if(url.contains("campus_guide.aspx")) {
				getGpsPermission();
				url = url.replace("http://", "https://");
				view.loadUrl(url);
				return true;
			}
			else if(url.contains("mapview.aspx")) {
				getGpsPermission();
				view.loadUrl(url);
				return true;
			}
			else { // ?????? ????????? ????????? ?????? ???????????? ??????
				return super.shouldOverrideUrlLoading(view, url);
			}
			return true;
		}

		@Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl){
            Log.e(InhaUtility.TAG,"ReceivedError on WebView. ERROR CODE IS " + errorCode);
            Log.e(InhaUtility.TAG,"description IS " + description);
            Log.e(InhaUtility.TAG,"failingUrl IS " + failingUrl);
            try{
                view.loadUrl("file:///android_asset/www/error.html?errorCode=" + errorCode + "&errorDescription=" + description);
            }catch  (Exception e) {
                Log.e(InhaUtility.TAG, e.toString());
            }
        }

        // ????????? ????????? ????????? ???
		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			//cookie sync
			CookieSyncManager.getInstance().sync();

			// ????????? ???????????? ??????????????? ??????

			pb.setVisibility(View.GONE); // ProgressBar??? ??????
			// ????????? ????????? ??? (????????? ?????? ????????? ????????? ???????????? ??????)
			Log.i(InhaUtility.TAG, url);
			if (loadingUrl.contains(url)) {
				if (!view.canGoForward()) {
					view.clearHistory();
				}
			}

			PushCount.setPushCountBadge();

			if (view.canGoBack())
				back.setImageResource(R.drawable.web_back_enable);
			else
				back.setImageResource(R.drawable.web_back_disable);
			if (view.canGoForward())
				next.setImageResource(R.drawable.web_next_enable);
			else
				next.setImageResource(R.drawable.web_next_disable);
		};
	};
	Handler afterload = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == InhaUtility.SUCCESS) {
				Intent intent = new Intent(Intent.ACTION_VIEW);

				File file = new File(InhaUtility.DOWNLOADPATH + msg.obj);

				String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
				String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

				if(mimetype == null) return;
				Uri apkURI = FileProvider.getUriForFile(
						getApplicationContext(),
						getApplication().getPackageName() + ".provider", file);
				intent.setDataAndType(apkURI, mimetype);
				intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				startActivity(intent);
				dialog.dismiss();
			}
		};
	};
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {  // ???????????? ????????? ???
			if(wv.getUrl().contains(InhaUtility.ROOTURL + "/index.aspx")){
				finish();
			}
			else if (wv.canGoBack()) { // ??? ???????????? ?????? ??????
				if(wv.getUrl().contains("Login.aspx")){
					wv.clearHistory();
					wv.loadUrl(UrlUtil.getMobileHome(getApplicationContext()));
				}
				else{
					wv.goBack();
				}
				return true;
			}
			else finish(); // ????????? ???????????? ??????
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onStart() {
		super.onStart();
		SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		shakeDetector.start(sensorManager);
	}

	@Override
	public void onResume(){
		CookieSyncManager.getInstance().startSync();
		super.onResume();
	}
	@Override
	protected void onStop() {
		pb.setVisibility(View.GONE);
		if(timer != null){
			timer.cancel();
			timer.purge();
			timer = null;
		}
		shakeDetector.stop();
		super.onStop();
	}
	@Override
	protected void onPause() {
		overridePendingTransition(0, 0); // ???????????? ?????? ??????????????? ??????
		super.onPause();
	}

	@Override public void hearShake () {
		String tempCurrentURL = wv.getUrl();
		if(tempCurrentURL.contains(InhaUtility.ROOTURL + "/portal/mID/mobile_id.aspx") ||
				tempCurrentURL.contains(InhaUtility.ROOTURL + "/portal/mID/mobile_id_stu.aspx") ||
				tempCurrentURL.contains(InhaUtility.ROOTURL + "/portal/Staff/device_authentication.aspx")){
			wv.goBack();
			Toast.makeText(this, "?????? ???????????? ???????????????", Toast.LENGTH_SHORT).show();
			return;
		} else if (tempCurrentURL.contains(InhaUtility.ROOTURL + "/map/map3view.aspx")) {
			wv.loadUrl(InhaUtility.ROOTURL + "/index.aspx");
			Toast.makeText(this, "????????? ????????? ???????????? ??? ???????????? ???????????????", Toast.LENGTH_SHORT).show();
		} else {
			wv.loadUrl(InhaUtility.ROOTURL + "/shake/redirect.aspx");
			Toast.makeText(this, "????????? ????????? ???????????? ????????? ?????? ????????? ???????????? ???????????????", Toast.LENGTH_SHORT).show();
		}
	}

	class Download extends Thread {
		String url, fileName;

		public Download(String url, String fileName) {
			super();
			this.url = url;
			this.fileName = fileName;
		}

		@Override
		public void run() {
			boolean success = InhaUtility.downloadFile(url, fileName);

			Message msg = new Message();
			if (success) {
				msg.what = InhaUtility.SUCCESS;
				msg.obj = fileName;
			}
			else msg.what = InhaUtility.FAILED;
			afterload.sendMessage(msg);
		}
	}

	public boolean checkAttendance() {
		try{
			// /map/map3view.aspx??? HTML5 geolocation?????? ?????? ?????? ?????? ?????????
			if (wv.copyBackForwardList().getCurrentItem().getUrl().contains("map3view.aspx")) return true;

				//IP ??????
			WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
			if(wifiMgr.isWifiEnabled()){
				WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
				int ip = wifiInfo.getIpAddress();
				String ipAddress = Formatter.formatIpAddress(ip);
				String ipYN = null;//ip ?????? ?????? ??????(17.02.15)

				//ip ?????? ?????? ??????(17.02.15)
				ipYN = InhaUtility.retAuthYn(InhaUtility.ROOTURL + "/map/Ret_ipYN.aspx?ip="+ ipAddress.replace(".", "_"));
				ipYN = ipYN.replaceAll("[\n]", " ");

				if(ipYN.contains("YES")){
					return true;
				}
			}

			//GPS ??????
			getGpsPermission();
            if(ContextCompat.checkSelfPermission(WebViewContext , Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                gps = null;
                gps = new GPSTracker(WebViewActivity.this);
            }

			if(gps != null && gps.canGetLocation()){
				String gpsYN;
				double latitude = gps.getLatitude();
				double longitude = gps.getLongitude();

				gpsYN = InhaUtility.retAuthYn(InhaUtility.ROOTURL + "/map/Ret_gpsYN.aspx?a="+ latitude + "&b=" + longitude);
				gpsYN = gpsYN.replaceAll("[\n]", " ");

				gps.stopUsingGPS();

				if(gpsYN.contains("YES")){
					return true;
				} else {
					return false;
				}
			}

		} catch (Exception e) {
			Log.i("checkAtt", e.toString());
		}
		return false;
	}

    protected void initShakeSensor () {
        shakeDetector = new ShakeDetector(this);
    }

	public void goWebView(String url){
		wv.loadUrl(url);
	}

	public void getGpsPermission(){
		PermissionListener permissionlistener = new PermissionListener() {
			@Override
			public void onPermissionGranted() {
				gps = new GPSTracker(WebViewActivity.this);
				if(!gps.canGetLocation()){
					final AlertDialog.Builder builder =  new AlertDialog.Builder(WebViewActivity.this);
					final String action = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
					final String message = "GPS ????????? ???????????? ???????????????. GPS??? ????????? ????????? ???????????? ?????????.";

					builder.setMessage(message)
							.setPositiveButton("OK",
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface d, int id) {
											WebViewActivity.this.startActivity(new Intent(action));
											d.dismiss();
										}
									})
							.setNegativeButton("Cancel",
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface d, int id) {
											d.cancel();
										}
									});
					builder.create().show();
				}
			}

			@Override
			public void onPermissionDenied(ArrayList<String> deniedPermissions) {
				Log.i("permission", "permission denied on getGpsPermission");
			}
		};

		TedPermission.with(this)
				.setPermissionListener(permissionlistener)
				.setDeniedMessage("??? ????????? ?????????????????? ?????? ????????? ???????????????.\n\n[??????] > [??????????????????] > [???????????????] > [??????] ?????? ?????? ????????? ??????????????????.")
				.setPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION)
				.check();
	}
}