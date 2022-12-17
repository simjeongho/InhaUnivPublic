package kr.ac.inha.android.APP;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.StrictMode;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ProgressBar;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;

import javax.mail.internet.MimeUtility;

public class ContactsSendActivity extends Activity implements OnCheckedChangeListener {
	private NfcAdapter nfcAdapter;
	private static NdefMessage mMessage;
	private TextView mConfirmNFC;
	private TextView mDataNFC;
	private ToggleButton mToggleNFC;
	private CheckBox[] mCheckBox = new CheckBox [6];
	
	public String vCardData;
	private String[] vCardBuffer;
	int vCardBufferSize;
	private String[] vCardBufferCopy = new String[6];
	
	WebView wv;
	ProgressBar pb;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contacts_send);
        
        
        init();
		initWebView();
		wv.loadUrl(getIntent().getStringExtra("url"));


    }
    
    void init(){
    	pb = (ProgressBar) findViewById(R.id.progress);
		wv = (WebView) findViewById(R.id.webview);
		mConfirmNFC = (TextView) findViewById(R.id.confirmNFC);
        mDataNFC = (TextView) findViewById(R.id.dataNFC);
        mToggleNFC = (ToggleButton) findViewById(R.id.toggleNFC);
        mCheckBox[0] = (CheckBox) findViewById(R.id.checkBox1);
        mCheckBox[1] = (CheckBox) findViewById(R.id.checkBox2);
        mCheckBox[2] = (CheckBox) findViewById(R.id.checkBox3);
        mCheckBox[3] = (CheckBox) findViewById(R.id.checkBox4);
        mCheckBox[4] = (CheckBox) findViewById(R.id.checkBox5);
        mCheckBox[5] = (CheckBox) findViewById(R.id.checkBox6);
        for(int i = 0; i < 6; i++){
        	mCheckBox[i].setOnCheckedChangeListener(this);
        }
        
        //초기엔 NFC기능 안보이게 함 로그인 되면 보여주도록 처리
        mConfirmNFC.setVisibility(View.GONE);
        mDataNFC.setVisibility(View.GONE);
        mToggleNFC.setVisibility(View.GONE);
        for(int i = 0; i < 6; i++){
        	mCheckBox[i].setVisibility(View.GONE);
        }
        
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(nfcAdapter == null){
        	Toast.makeText(this,"스마트 명함을 사용하려면 NFC 기능을 사용할 수 있어야 합니다.",Toast.LENGTH_LONG).show();
        	finish();
        	return;
        }
        mToggleNFC.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Toast.makeText(getApplicationContext(), "NFC를 활성화한 후 뒤로가기를 통해 어플리케이션으로 돌아가세요!", Toast.LENGTH_LONG).show();
	        	startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
			}
		});
    }
	void initWebView(){
		wv.setVisibility(View.GONE);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.requestFocus(); 
		wv.setFocusable(true); 
		wv.setFocusableInTouchMode(true);
		wv.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
					case MotionEvent.ACTION_UP:
						if (!v.hasFocus()) {
							v.requestFocus();
						}
						break; 
				}
				return false;
			}
		});
		wv.setWebChromeClient(new WebChromeClient() {
		    public boolean onConsoleMessage(ConsoleMessage cmsg)
		    {
		        // check secret prefix
		        if (cmsg.message().startsWith("MAGIC"))
		        {
		            String msg = cmsg.message().substring(5); // strip off prefix
		            Log.i("Cielo3", msg);
		            vCardData = msg.replace("<head></head><body>", "").replace("</body>", "");
		            
		            if(vCardData != null){
			        	loadNFC();
			        }

		            return true;
		        }

		        return false;
		    }
		});
		wv.setWebViewClient(new WebViewClient() {	
			// 페이지 로딩 시작 전에
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				pb.setVisibility(View.VISIBLE); // Progress Bar 표시
				return super.shouldOverrideUrlLoading(view, url);
			}
			
			// 페이지 로딩이 끝났을 시
			@Override			
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				// 로그인 페이지가 로딩 되었으면
				if (url.contains("Login.aspx")) {
					pb.setVisibility(View.GONE);
					view.setVisibility(View.VISIBLE);
				} // Webview를 보여줘서 로그인을 하게 한다.
				// 아닐 경우
				else {
					view.setVisibility(View.GONE);
					pb.setVisibility(View.GONE);
			        mConfirmNFC.setVisibility(View.VISIBLE);
			        mDataNFC.setVisibility(View.VISIBLE);
			        mToggleNFC.setVisibility(View.VISIBLE);
			        //view.loadUrl("javascript:window.HTMLOUT.GetSource('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
			        view.loadUrl("javascript:console.log('MAGIC'+document.getElementsByTagName('html')[0].innerHTML);");
			        
					// Webview를 숨기고 실행
				}
			};
		});
	}
    void loadNFC(){
    	HashMap<String, String> Header = new HashMap<String, String>();
    	Header.put("N", "성    명: ");
    	Header.put("ORG", "직    장: ");
    	Header.put("ADR", "주    소: ");
    	Header.put("WORK", "사무실: ");
    	Header.put("CELL", "핸드폰: ");
    	Header.put("EMAIL", "이메일: ");
    	
        //android.os.NetworkOnMainThreadException를 위해 삽입 
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy); 
        
        try{
        	//vCardData = InhaUtility.downloadHtml("http://165.246.12.136:8880/foxler/vcard.aspx");
        	//vCardData = vCardData.substring(vCardData.indexOf("<body>") + 6, vCardData.indexOf("</body>"));
        	vCardBuffer = vCardData.split("\\|");
        	vCardBufferSize = vCardBuffer.length;
        	
        	if(nfcAdapter.isEnabled()){
        		mToggleNFC.setChecked(true);
            	mConfirmNFC.setText("NFC 읽기/쓰기 기능이 활성화된\n다른 스마트폰에 접촉해 주세요."); 	
            } else {
            	mToggleNFC.setChecked(false);
            	mConfirmNFC.setText("먼저 NFC를 활성화하세요.\n그 다음 다른 스마트폰 뒷면에 접촉해 주세요.");
            }
        	mDataNFC.setText("- 보낼 데이터 선택 -\n");
        	for(int i = 0; i < vCardBufferSize; i++){
        		String[] vCardBufferData = vCardBuffer[i].split(":");
        		mCheckBox[i].setText(Header.get(vCardBufferData[0]) + vCardBufferData[1]);
        		mCheckBox[i].setVisibility(View.VISIBLE);
        		if(vCardBufferData[0].equals("ADR")){
        			mCheckBox[i].setChecked(false);
        		}
        		if(vCardBufferData[0].equals("N") || vCardBufferData[0].equals("CELL")){
        			mCheckBox[i].setClickable(false);
        		}
        	}
        	mConfirmNFC.setTextColor(0xFF000000);
        	mDataNFC.setTextColor(0xFF000000);
        	for(int i = 0; i < vCardBufferSize; i++){
        		mCheckBox[i].setTextColor(0xFF000000);
        	}
        	
        	if(mMessage == null){
        		for(int i = 0; i < vCardBufferSize; i++){
       			 if(mCheckBox[i].isChecked()){
       				 vCardBufferCopy[i] = vCardBuffer[i];
       			 }else{
       				 vCardBufferCopy[i] = "";
       			 }
       		 }
	        	//vCardBufferCopy = vCardBuffer;
	        	String data = createVCardFormat(vCardBufferCopy);
	        	mMessage = createNdefVCard(data);
        	}
        	
        	if(nfcAdapter != null && mMessage != null) {
        		nfcAdapter.enableForegroundNdefPush(this, mMessage);
        	}
        	
        	findViewById(R.id.progress).setVisibility(View.GONE); // 로딩 종료 후 프로그래스 바 안보이게
        	
	         /*mMessage = new NdefMessage(
	        		new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
	        				"text/x-vcard".getBytes(),
	        				new byte[] {},
	        				sourceMsg.getBytes())
	        		);*/
	
	        //연락처관련
	        //phoneNum = getPhoneNum(); //777822
	        //mText.setText(phoneNum);
	        //saveToContact("010-6857-0428", "조성민, "Email@gmail.com");
        
        } catch(Exception e){
        	Log.i("Exception_C", e.getMessage());
        }
    }
    
    public void onResume(){
    	super.onResume();
    	this.onCreate(null);
    	if(nfcAdapter != null && mMessage != null) {
    		nfcAdapter.enableForegroundNdefPush(this, mMessage);
    	}
    }
	public void onPause(){
    	super.onPause();
    	mMessage = null;
    	if(nfcAdapter != null){
    		nfcAdapter.disableForegroundNdefPush(this);
    	}
    }
	public void onClick (View v) {
		switch(v.getId()) {
			case R.id.img_title:
				finish();
				break;
		}
	}
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
	  // TODO Auto-generated method stub
		 mMessage = null;
		 
		 for(int i = 0; i < vCardBufferSize; i++){
			 if(mCheckBox[i].isChecked()){
				 vCardBufferCopy[i] = vCardBuffer[i];
			 }else{
				 vCardBufferCopy[i] = "";
			 }
		 }
		 
		String data;
		try {
			data = createVCardFormat(vCardBufferCopy);
			mMessage = createNdefVCard(data);
			
	    	if(nfcAdapter != null) {
	    		nfcAdapter.enableForegroundNdefPush(this, mMessage);
	    	}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
	
	}
	public String createVCardFormat(String[] vCardBuffer) throws Exception{
        StringBuffer sourceMsg = new StringBuffer();
        String[] vCardBufferData;

        sourceMsg
        	.append("BEGIN:VCARD\n")
        	.append("VERSION:3.0\n");
        for(int i = 0; i < vCardBufferSize; i++){
        	vCardBufferData = vCardBuffer[i].split(":");
        	if(vCardBufferData[0].equals("N") || vCardBufferData[0].equals("ORG") || vCardBufferData[0].equals("ADR")){
        		sourceMsg
	        		.append(vCardBufferData[0] + ";CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:")
	        		.append(MimeUtility.encodeText(vCardBufferData[1], "UTF-8", "Q").replaceAll(" ", "").replaceAll("_", " ").replaceAll("=\\?UTF-8\\?Q\\?", "").replaceAll("\\?=", ""))
	        		.append("\n");
	        } else if ( vCardBufferData[0].equals("CELL") || vCardBufferData[0].equals("WORK")){
	        	sourceMsg
	        		.append("TEL;TYPE=" + vCardBufferData[0] + ",VOICE:")
	        		.append(vCardBufferData[1])
	        		.append("\n");
	        } else if( vCardBufferData[0].equals("EMAIL")){
	        	sourceMsg
	        		.append(vCardBufferData[0] +  ":")
	        		.append(vCardBufferData[1])
	        		.append("\n");
	        }
        }
        sourceMsg.append("END:VCARD\n");

		return sourceMsg.toString();
	}
    public static NdefMessage createNdefVCard(String vCard) {
    	byte[] vCardDataBytes = vCard.getBytes(Charset.forName("US-ASCII"));
    	byte[] vCardPayload = new byte[vCardDataBytes.length + 1];
    	System.arraycopy(vCardDataBytes, 0, vCardPayload, 1, vCardDataBytes.length);
    	//vCardDataBytes[0] = (byte)0x00;
    	
        NdefRecord record = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "text/x-vcard".getBytes(),
                new byte[0], vCardPayload);
        return new NdefMessage(new NdefRecord[] {
            record
        });
    }
    public NdefRecord createTextRecord(String payload, Locale locale, boolean encodeInUtf8) {
        byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII"));
        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        byte[] textBytes = payload.getBytes(utfEncoding);
        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + langBytes.length);
        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);
        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
        NdefRecord.RTD_TEXT, new byte[0], data);
        return record;
    }
    public void saveToContact(String sPhoneNumber, String sName, String sEmail){
    	// rawContact 삽입
    	ContentValues contentValues = new ContentValues();
    	contentValues.put(ContactsContract.RawContacts.CONTACT_ID, 0);
    	contentValues.put(ContactsContract.RawContacts.AGGREGATION_MODE, ContactsContract.RawContacts.AGGREGATION_MODE_DISABLED);
    	Uri rawContactUri = getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, contentValues);
    	long rawContactId = ContentUris.parseId(rawContactUri);
    	
    	// 전화번호
    	contentValues.clear();
    	contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
    	contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
    	contentValues.put(ContactsContract.CommonDataKinds.Phone.TYPE, Phone.TYPE_MOBILE);
    	contentValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, sPhoneNumber);
    	Uri dataUri = getContentResolver().insert(ContactsContract.Data.CONTENT_URI, contentValues);
    	
    	// 이름
    	contentValues.clear();
    	contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
    	contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
    	contentValues.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, sName);
    	dataUri = getContentResolver().insert(ContactsContract.Data.CONTENT_URI, contentValues);

    	// mail
    	contentValues.clear();
    	contentValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
    	contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
    	contentValues.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_MOBILE);
    	contentValues.put(ContactsContract.CommonDataKinds.Email.DATA1, sEmail);
    	dataUri = getContentResolver().insert(ContactsContract.Data.CONTENT_URI, contentValues);
    }
}
