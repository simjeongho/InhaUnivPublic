package kr.ac.inha.android.APP.loader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import kr.ac.inha.android.APP.library.InhaUtility;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class MainLoader extends Thread {
	Context context;
	private SharedPreferences mPref;
	private SharedPreferences.Editor editor;
	private Handler afterLoad;
	private String imgurl;
	public MainLoader(Context context, SharedPreferences mPref, Handler afterLoad) {
		this.context = context;
		this.mPref = mPref;
		editor = mPref.edit();
		this.afterLoad = afterLoad;
	}
	@Override
	public void run() {
		super.run();
		String url = InhaUtility.ROOTURL + "/app_xml/app_xml.aspx?p_xmlgubun=menu";
		String Push_YN = "";
		try {
			downloadFile(new URL(url).openStream(), "MENU.xml"); // 메뉴 XML의 저장
			Boolean isUpdated = ParseXML(); // XML에서 이미지 관련 정보 파싱
			

			
			if (isUpdated) // 업데이트 시 이미지 재 다운로드
				downloadFile(new URL(InhaUtility.ROOTURL + imgurl).openStream(), "MENUIMG");
			Message msg = new Message();
			msg.what = InhaUtility.SUCCESS;
			msg.obj = isUpdated;
			afterLoad.sendMessage(msg); // 업데이트 되었음을 알림
		} catch (Exception e) {
			e.printStackTrace();
			afterLoad.sendEmptyMessage(InhaUtility.FAILED);
		}
	}
	private boolean ParseXML() {
		try {

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document xmlDoc = builder.parse(new File(context.getFilesDir() + "/MENU.xml"));
			Element root = xmlDoc.getDocumentElement();

			int current_version = mPref.getInt("version", 0);
			int xml_version = Integer.valueOf(root
					.getElementsByTagName("VERSION").item(0).getFirstChild()
					.getNodeValue());

			// 버전이 동일하면 중지

			if (current_version == xml_version)
				return false;
			
			// 이미지 정보 업데이트
			NodeList mainimg = root.getElementsByTagName("MAIN_MENU_IMG").item(0).getChildNodes();
			HashMap<String, String> imginfo = new HashMap<String, String>();
			for (int i = 0; i < mainimg.getLength(); i++) {
//				Log.i(TAG, mainimg.item(i).getNodeName());
//				Log.i(TAG, mainimg.item(i).getFirstChild().getNodeValue());
				imginfo.put(mainimg.item(i).getNodeName(), mainimg.item(i).getFirstChild().getNodeValue());
			}
			
			// Preference 데이터 설정
			editor.putInt("version", xml_version);
			editor.putInt("WIDTH_SUB",
					Integer.valueOf(imginfo.get("WIDTH_SUB")));
			editor.putInt("HEIGHT_SUB",
					Integer.valueOf(imginfo.get("HEIGHT_SUB")));
			editor.putInt("COLUM_GAP",
					Integer.valueOf(imginfo.get("COLUM_GAP")));
			editor.putInt("LINE_GAP",
					Integer.valueOf(imginfo.get("LINE_GAP")));
			editor.putInt("OFFSET_X",
					Integer.valueOf(imginfo.get("OFFSET_X")));
			editor.putInt("OFFSET_Y",
					Integer.valueOf(imginfo.get("OFFSET_Y")));
			imgurl = imginfo.get("URL");
			editor.commit();
//			Log.i(InhaUtility.TAG, imginfo.get("URL"));
		} catch (Exception e) {
			Log.e(InhaUtility.TAG, e.toString());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private boolean downloadFile(InputStream is, String filename) {
//		Log.i(TAG, filename);
		try {
			FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
			BufferedInputStream bin = new BufferedInputStream(is);
			BufferedOutputStream bout = new BufferedOutputStream(fos);
			int byteRead = 0;
			byte[] buffer = new byte[10240];
			while ((byteRead = bin.read(buffer, 0, 10240)) != -1) {
				bout.write(buffer, 0, byteRead);
				bout.flush();
			}
			bin.close();
			bout.close();
			is.close();
			fos.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	
}
