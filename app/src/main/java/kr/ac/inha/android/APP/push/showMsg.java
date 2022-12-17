package kr.ac.inha.android.APP.push;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import kr.ac.inha.android.APP.FIDO.util.FidoLibraryBuilder;
import kr.ac.inha.android.APP.MainActivity;
import kr.ac.inha.android.APP.R;
import kr.ac.inha.android.APP.WebViewActivity;
import kr.ac.inha.android.APP.library.InhaUtility;
import androidx.appcompat.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

public class showMsg extends AppCompatActivity {
    String title, msg, dialogtype, msg_seq, msg_extra, go_url, currentActivity, msgInput;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //android.os.NetworkOnMainThreadException를 위해 삽입 
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
         
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Bundle bun = getIntent().getExtras();
        title = bun.getString("title");
        msg = bun.getString("msg");
        msgInput = bun.getString("msg");
        msg_seq = bun.getString("msg_seq");
        dialogtype = bun.getString("dialogtype");
        msg_extra = bun.getString("msg_extra");
        currentActivity = bun.getString("currentActivity");
		go_url = bun.getString("go_url");

		if (go_url != null && !go_url.equals("")) {
			requestMsgReceiveOk();
			showWebView();
		} else {
			showAlertDialog();
		}
    }

    private void showAlertDialog() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(showMsg.this);
		alertDialog.setIcon(R.drawable.icon);

		//타입 별로 분기
		if(dialogtype.contains("close")){
			msg = "제목 : " + msg;
			requestMsgReceiveOk();
		} else if(dialogtype.contains("confirm")){
			msg = "제목 : " + msg + "\n" +
					"내용 : " + msg_extra;
			showDetailButton(alertDialog);
		}

		showCloseButton(alertDialog);
		alertDialog.setTitle(title);
		alertDialog.setMessage(msg);

		alertDialog.show();
	}

    private void showWebView() {
    	if (go_url.contains("otp.aspx")) {
			FidoLibraryBuilder fidoLib = new FidoLibraryBuilder(this);
			String fidoDeviceId = fidoLib.getDeviceID();
    		go_url = go_url + "?fido_device_id=" + fidoDeviceId;
		}

		if(currentActivity.equals("kr.ac.inha.android.APP.WebViewActivity") && WebViewActivity.WebViewContext != null) {
			((WebViewActivity) WebViewActivity.WebViewContext).goWebView(go_url);
			showMsg.this.finish();
		} else {
			// push보기를 누를경우 mainActivity로 보내서 처리하도록 함 단, 기존에 떠있는 창들은 정리함
			Intent intent = new Intent();
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.setClass(showMsg.this, MainActivity.class);
			intent.putExtra("pushurl", go_url);
			startActivity(intent);
			showMsg.this.finish();
		}
	}

	private void showDetailButton(AlertDialog.Builder alertDialog) {
		//보기 버튼
		alertDialog.setPositiveButton("보기", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if(currentActivity.equals("kr.ac.inha.android.APP.WebViewActivity") && WebViewActivity.WebViewContext != null) {
					((WebViewActivity) WebViewActivity.WebViewContext).goWebView(InhaUtility.ROOTURL+ "/push/push_received_msg_detail.aspx?p_msgseq=" + msg_seq);
					showMsg.this.finish();
				} else {
					// push보기를 누를경우 mainActivity로 보내서 처리하도록 함 단, 기존에 떠있는 창들은 정리함
					Intent intent = new Intent();
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					intent.setClass(showMsg.this, MainActivity.class);
					intent.putExtra("pushurl", InhaUtility.ROOTURL+ "/push/push_received_msg_detail.aspx?p_msgseq=" + msg_seq);
					startActivity(intent);
					showMsg.this.finish();
				}
			}
		});
	}

	void showCloseButton(AlertDialog.Builder alertDialog){
    	//닫기 버튼
        alertDialog.setNegativeButton("닫기", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                showMsg.this.finish();
            }
    	});
    }
    void requestMsgReceiveOk(){
    	try {
	        HttpGet request = new HttpGet(InhaUtility.ROOTURL + "/push/push_msg_receive_ok.aspx?p_msgseq=" + msg_seq + "&p_regid=" + InhaUtility.regid + "&p_msg_status=RD");
	        HttpClient client = new DefaultHttpClient();
			client.execute(request);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    //일정 저장하는 메소드
    public void saveSchedule(String title, String startDate, String endDate, Context Contexts){
    	try{
    		String url = Build.VERSION.SDK_INT >= 8 ? "com.android.calendar" : "calendar";
    		Uri events = Uri.parse(String.format("content://%s/events", url));
    		Uri reminders = Uri.parse(String.format("content://%s/reminders", url));
    		Uri when = Uri.parse(String.format("content://%s/instances/when", url));
    		
    		String calId="INHA_C";
    		Cursor c = Contexts.getContentResolver().query(
    				Uri.parse(String.format("content://%s/calendars", url)), 
    				new String[] { "_id" }, null, null, null);
    		
    		if (c.moveToFirst()) {
    			calId = c.getString(c.getColumnIndex("_id"));
    			do {
//    				Log.i(InhaUtility.TAG, calId + " : " + c.getString(c.getColumnIndex("name")));				
    			} while (c.moveToNext());
    		} // 첫번째 캘린더 (기본 캘린더) 아이디를 가져온다.
    		c.close();
    		
        	SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
			Date st = format.parse(startDate);
			Date edt = format.parse(endDate);
			long duration = format.parse(endDate).getTime() - st.getTime();
			int hour = (int) (duration / ( 3600 * 1000 )) ;
			duration = duration % (3600 * 1000);
			int min = (int) (duration / ( 60 * 1000 )) ;
        	
        	ContentValues cv = new ContentValues();
        	cv.put("calendar_id", calId);
        	cv.put("title", title);
        	cv.put("dtstart", st.getTime());
        	cv.put("dtend", edt.getTime());
        	//cv.put("duration", "PT" + hour + "H" + min + "M");
        	//cv.put("eventTimezone", TimeZone.getTimeZone("Asia/Seoul").toString());
        	cv.put("eventTimezone", "Asia/Seoul");
        	cv.put("hasAlarm", 1);
        	Uri inserturi = Contexts.getContentResolver().insert(events, cv);
        	
        	// 알림 입력 부분
			long id = Long.parseLong(inserturi.getLastPathSegment()); // 입력된 일정의 아이디 추출
			cv = new ContentValues();
			cv.put("event_id", id);
			cv.put("method", 1);
			cv.put("minutes", 30);

			Contexts.getContentResolver().insert(reminders, cv);
    	} catch  (Exception e) {
			Log.i("showMsg", e.toString());
		}
    }
}