package kr.ac.inha.android.APP;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;

import kr.ac.inha.android.APP.FIDO.model.RootChecker;
import kr.ac.inha.android.APP.library.InhaUtility;
import kr.ac.inha.android.APP.push.showMsg;


public class MainActivity extends Activity {
	int currentVer;
	SharedPreferences mPref;
	SharedPreferences.Editor editor;
	ProgressDialog progDailog;
	HashMap<String, String> toApp;
	String AppVersion;
	String PlaystoreVersion;
	ImageView pushIconImage;
	static int countArr[];

	private void init() {
		Log.d(this.getPackageName(), "mainActivity init");
		//기본 공유 환경설정 파일
		mPref = PreferenceManager.getDefaultSharedPreferences(this);
		//mPref = getPreferences(Context.MODE_PRIVATE);
		editor = mPref.edit();
		editor.apply();
		Log.d("MainDeviceToken" , mPref.getString("devicetoken", "where is devicetoken"));
		//currentVer = mPref.getInt("version", 0);
		progDailog = null;

		toApp = new HashMap<String, String>();
		toApp.put("LIBRARY_CONNECT", "mlib.inha.ac.kr");
		toApp.put("admi01", "com.kr.mncapro.inha");
		AppVersion = getVersionName(this);

		PlaystoreVersion = getPlaystoreVersion();
		Log.i("appversion", AppVersion);
		Log.i("playstoreversion", PlaystoreVersion);
		RootChecker Rc = new RootChecker(this);
		Log.i("Rootingcheck", "Rootingcheck");
		Rc.isDeviceRooted();
		
		PackageManager packageManager = getPackageManager();
		Log.i("SignChecking", "signChecking");

		try {
			Signature[] signs = packageManager.getPackageInfo(getPackageName(),
					PackageManager.GET_SIGNATURES).signatures;
			for(Signature signature : signs) {
				//Log.d("sign = " + signature.toCharsString());
				Log.d("signCheck", "sign = " + signature.toCharsString());
			}
		} catch(NameNotFoundException e) {
			e.printStackTrace();
		}
		Log.i("SignCheckFinish", "SignCheckFinish");
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 메인에서는 네트워크 체크와 모바일 인증

		init();
		RootChecker Rc = new RootChecker(this);
		WebView wv = new WebView(this);
		//버전 값이 넘어가기 전
		Log.i(InhaUtility.TAG, InhaUtility.MOBILEURL);
		wv.loadUrl(InhaUtility.MOBILEURL);
		
		//update 확인
		checkUpdate();
		//Rooting check
		//Log.i("Rootingcheck", "Rootingcheck");
		//Rc.isDeviceRooted();

			// push보기를 클릭한것인지 확인후에 페이지 이동시킴
		if(getIntent().getStringExtra("pushurl") != null)
		{
			Intent intent = new Intent();			
			intent.setClass(MainActivity.this, WebViewActivity.class);
			intent.putExtra("url", getIntent().getStringExtra("pushurl"));
			intent.putExtra("TopUrl", InhaUtility.ROOTURL + "/push/Push_main.aspx");
			//intent.putExtra("title", "스마트 알림");
			startActivity(intent);
			finish();
		} else {
			Intent intent = new Intent(this, WebViewActivity.class);
			//버전 값, devicetoken 추가
			intent.putExtra("url", InhaUtility.MOBILEURL+AppVersion+"&phone_regid="+InhaUtility.regid+"&phone_type=Android");
			//intent.putExtra("url", InhaUtility.MOBILEURL+AppVersion);
			startActivity(intent);
			finish();
		}
	}

	@Override
	protected void onPause() {
		overridePendingTransition(0, 0); // 액티비티 이동 애니메이션 제거
		super.onPause();
	}
	@Override
	protected void onResume() {
		// TODO 자동 생성된 메소드 스텁
		super.onResume();
	}

	 public static String getVersionName(Context context)
	 {
	     try {
	         PackageInfo pi= context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
	         return pi.versionName;
	     } catch (NameNotFoundException e) {
	         return null;
	     }
	 }
	 
	 public static String getPlaystoreVersion()
	 {
		 String rtPlaystoreVersion = InhaUtility.downloadHtml("https://play.google.com/store/apps/details?id=kr.ac.inha.android.APP");
		 if(rtPlaystoreVersion.indexOf("softwareVersion") == -1){
			 return "error";
		 }
		 rtPlaystoreVersion = rtPlaystoreVersion.substring(rtPlaystoreVersion.indexOf("softwareVersion"), rtPlaystoreVersion.indexOf("softwareVersion") + 100);
		 String[] arrVersion = rtPlaystoreVersion.split(" ");
		 rtPlaystoreVersion = arrVersion[1];
		 return rtPlaystoreVersion;
	 }




		public void checkUpdate(){
		 if(PlaystoreVersion.equals("error")){
			 return;
		 }
		 if(!AppVersion.equals(PlaystoreVersion)){
			
			Intent updateIntent = new Intent(MainActivity.this, showMsg.class);;
		    Bundle b = new Bundle();
		    b.putString("title","인하대학교");
		    b.putString("msg", "새로운 버전이 있습니다. 플레이 스토어로 연결하시겠습니까?");
		    b.putString("dialogtype", "update");
		    
		    updateIntent.putExtras(b);
		    
		    startActivity(updateIntent);
		 }
	 }

	 @Override
    public void onDestroy()
	 {
		this.finish();
        super.onDestroy();
    }

    //private boolean isRooted() {
		//boolean runtimeFlag = false;
		//try {
			//Runtime.getRuntime().exec("su");
			//runtimeFlag = true;
		//} catch(Exception e){
			//runtimeFlag = false;
		//}
	//}
}