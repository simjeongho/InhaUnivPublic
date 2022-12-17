package kr.ac.inha.android.APP.push;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import androidx.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.android.volley.toolbox.StringRequest;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kr.ac.inha.android.APP.R;
import kr.ac.inha.android.APP.curl.RequestUtils;
import kr.ac.inha.android.APP.curl.VolleyQueue;
import kr.ac.inha.android.APP.library.InhaUtility;

public class CustomFirebaseMessagingService extends FirebaseMessagingService {

    String sound_yn = null;
    String vibrate_yn = null;

    SharedPreferences mPref;
    SharedPreferences.Editor editor;
    private static final String TAG = "MyFirebaseMsgService";


    private void sendRegistrationToServer(String token){

        mPref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = mPref.edit();
        Log.i("send from onNewToken" , token);
        InhaUtility.regid = token;
        Log.d("fcmtoken", token);

        //editor.putString("devicetoken", InhaUtility.regid);
        editor.putString("devicetoken", token);
        editor.apply();
        Log.d("customFireBase", "deviceToken " + token);

        Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
        registrationIntent.putExtra("app", PendingIntent.getBroadcast(this, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE));
        registrationIntent.putExtra("sender", InhaUtility.SENDER_ID);
        registrationIntent.setPackage("com.google.android.c2dm.intent.REGISTER");
        startService(registrationIntent);
    }
    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);

        sendRegistrationToServer(s);
        Log.d("onNewToken" , "devicetoken is!!! :" + s);

    }
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Map<String, String> data = remoteMessage.getData();
        //RemoteMessage.Notification RN = remoteMessage.getNotification();
        setNotiSoundVibration(data);
        sendDidReceiveToDB(data.get("msgseq"));
        PushCount.updateBadgeCount(Integer.parseInt(data.get("badge")));
        Bundle b = setMsgBundle(data);
        sendSimpleNoti(getApplicationContext(), b);
        if(data.size() > 0)
        {
            Log.d(TAG, "onMessageReceived: " + data);//수정
        }
    }

    private void sendSimpleNoti(Context arg0, Bundle b){

        //sound, vibration on/off 기능 추가(17.02.15)
        genSoundAndVibrate(arg0);

        // 정보 설정
        long when = System.currentTimeMillis();
        int notiId = (int)(System.currentTimeMillis()/1000);

        // Noti 클릭 시 이벤트 설정
        Intent notificationIntent = new Intent(arg0, showMsg.class);;
        notificationIntent.putExtras(b);
        PendingIntent contentIntent = PendingIntent.getActivity(this, notiId, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // 푸쉬 알림 생성
        NotificationManager notificationManager = (NotificationManager)arg0.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mCompatBuilder;
        if(Build.VERSION.SDK_INT >= 26) {
            NotificationChannel mChannel = new NotificationChannel("푸시알림","푸시알림",NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(mChannel);
            mCompatBuilder = new NotificationCompat.Builder(arg0,mChannel.getId());
        } else {
            mCompatBuilder = new NotificationCompat.Builder(arg0);
        }
        mCompatBuilder.setSmallIcon(R.drawable.icon_notification_small);
        mCompatBuilder.setLargeIcon(BitmapFactory.decodeResource(arg0.getResources(), R.drawable.icon_notification_large));
        mCompatBuilder.setWhen(when);
        mCompatBuilder.setNumber(10);
        mCompatBuilder.setContentTitle(b.getString("msg"));
        mCompatBuilder.setContentText(b.getString("msg_extra"));
        mCompatBuilder.setStyle((new NotificationCompat.BigTextStyle().bigText(b.getString("msg_extra"))));
        mCompatBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        //mCompatBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText("동해물과 백두산이 마르고 닳도록 하느님이 보우하사 우리나라 만세 무궁화 삼천리 화려강산 대한 사람 대한으로 길이 보전하세 남산위에 저 소나무 철갑을 두른 듯 "));//2021.08.16 확장형 푸쉬 알림 수정
       // mCompatBuilder.setStyle((new NotificationCompat.BigTextStyle().bigText(b.getString("msg_seq"))));

        mCompatBuilder.setContentIntent(contentIntent);
        mCompatBuilder.setAutoCancel(true);
        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.notify(notiId, mCompatBuilder.build());
    }

    void genSoundAndVibrate(Context arg0){

        //sound, vibration on/off 기능 추가(17.02.15)
        AudioManager mAudioManager = (AudioManager) arg0.getSystemService(Context.AUDIO_SERVICE);

        Vibrator vibrator = (Vibrator) arg0.getSystemService(Context.VIBRATOR_SERVICE);
        Uri ringNotification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone ring = RingtoneManager.getRingtone(arg0, ringNotification);

        if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
            if(vibrate_yn.equals("Y") && sound_yn.equals("Y")){
                Log.e("vibe and sound", "OKAY");
                vibrator.vibrate(1000);
                ring.play();
            } else if(vibrate_yn.equals("Y") && sound_yn.equals("N")){
                vibrator.vibrate(1000);
            } else if(vibrate_yn.equals("N") && sound_yn.equals("Y")){
                ring.play();
            } else {
                //vibrate_yn도 sound_yn도 N인 경우 아무 것도 안함
            }
        } else{
            //무음 모드이면 아무것도 안함
        }
    }

    private void setNotiSoundVibration(Map<String, String> data) {
        sound_yn = data.get("sound_yn").toUpperCase(Locale.getDefault());
        vibrate_yn = data.get("vibrate_yn").toUpperCase(Locale.getDefault());
    }

    private void sendDidReceiveToDB(String msgseq) {
        mPref = PreferenceManager.getDefaultSharedPreferences(this);

        HashMap<String, String> getParams = new HashMap<String, String>();
        getParams.put("p_msg_status", "RS");
        getParams.put("p_regid", mPref.getString("devicetoken", ""));
        getParams.put("p_msgseq", msgseq);

        String url = InhaUtility.ROOTURL + "/push/push_msg_receive_ok.aspx";

        StringRequest stringRequest = RequestUtils.buildGetStringRequest(url, getParams);

        VolleyQueue.getInstance().addToRequestQueue(stringRequest);
    }

    private Bundle setMsgBundle(Map<String, String> data) {
        List<ActivityManager.RunningTaskInfo> Info = null;
    //parameter data는 remoteMessage.getData()로 얻어진 Map
        ActivityManager am = (ActivityManager)getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) { Info = am.getRunningTasks(1); }
        String currentActivity = Info.get(0).topActivity.getClassName();

        Bundle b = new Bundle();
        b.putString("title","인하대학교");
        b.putString("msg", data.get("msg"));
        b.putString("msg_seq", data.get("msgseq"));
        b.putString("badge", data.get("badge"));
        b.putString("msg_extra", data.get("msg_extra"));
        //b.putString("body" , RN.getBody());
        //b.putString("msg_body", data.get("msg_body"));
        b.putString("go_url", data.get("go_url"));
        b.putString("currentActivity", currentActivity);

        String msgType = "";
        msgType = data.get("msg_type");
        if ("TITLE".equals(msgType)) {
            b.putString("title", "알림");
            b.putString("dialogtype", "close");
        } else {
            b.putString("title", "상세 알림");
            b.putString("dialogtype", "confirm");
        }

        return b;
    }
}
