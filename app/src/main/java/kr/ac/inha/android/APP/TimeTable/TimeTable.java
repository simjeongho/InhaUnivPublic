package kr.ac.inha.android.APP.TimeTable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import kr.ac.inha.android.APP.library.InhaUtility;

public class TimeTable {
    private String calId;
    private Uri events, reminders, when;
    private Context ctx;

    public TimeTable (Context ctx) {
        this.ctx = ctx;
        this.calId = "INHA_C";
        String url = "com.android.calendar";
        this.events = Uri.parse(String.format("content://%s/events", url));
        this.reminders = Uri.parse(String.format("content://%s/reminders", url));
        this.when = Uri.parse(String.format("content://%s/instances/when", url));
    }

    public void initTT() {
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                // 버전에 따라 구글 내부 uri가 다르므로 그를 고려
                String url = "com.android.calendar";
                Cursor c = ctx.getContentResolver().query(
                        Uri.parse(String.format("content://%s/calendars", url)),
                        new String[] { "_id" }, null, null, null);

                if (c.moveToFirst()) {
                    calId = c.getString(c.getColumnIndex("_id"));
                    do {
//    					Log.i(InhaUtility.TAG, calId + " : " + c.getString(c.getColumnIndex("name")));
                    } while (c.moveToNext());
                } // 첫번째 캘린더 (기본 캘린더) 아이디를 가져온다.
                c.close();
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                Log.i("Permission", "permission denied on initTT");
            }
        };

        TedPermission.with(ctx)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("캘린더 권한을 승인하지 않으시면 이 기능을 사용할 수 없습니다.\n\n[설정] > [애플리케이션] > [인하대학교] > [권한] 에서 캘린더 권한을 부여해주세요.")
                .setPermissions(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR)
                .check();
    }

    public void ParseXML(String xml, String alarmTime) {
        try {
            Log.i(InhaUtility.TAG, xml);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document xmlDoc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
            Element root = xmlDoc.getDocumentElement();
            NodeList items = root.getElementsByTagName("items");

            for (int i = 0; i < items.getLength(); i++) {
                HashMap<String, String> iteminfo = new HashMap<String, String>();
                NodeList menu = items.item(i).getChildNodes();
                for (int j = 0 ; j < menu.getLength(); j++) {
                    if (menu.item(j).getFirstChild() != null) {
                        iteminfo.put(menu.item(j).getNodeName(), menu.item(j).getFirstChild().getNodeValue());
                    }
                }
                // 일정 입력을 위한 정보 재구성
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date d = format.parse(iteminfo.get("startDate"));
                long duration = format.parse(iteminfo.get("endDate")).getTime() - d.getTime();
                int hour = (int) (duration / ( 3600 * 1000 )) ;
                duration = duration % (3600 * 1000);
                int min = (int) (duration / ( 60 * 1000 )) ;
                // 일정 입력 부분
                ContentValues cv = new ContentValues();
                cv.put("calendar_id", calId);
                cv.put("title", iteminfo.get("ttitle"));
                cv.put("description", "inha_" + iteminfo.get("notes"));
                cv.put("eventLocation", iteminfo.get("location"));
                //cv.put("account_name", "inha");
                cv.put("dtstart", d.getTime());
                //cv.put("dtend", format.parse(iteminfo.get("enddate")).getTime());
                cv.put("duration", "PT" + hour + "H" + min + "M"); // rfc2445 dur-value 형식에 따름
                cv.put("rrule", String.format("FREQ=%s;BYDAY=%s;UNTIL=%sZ",
                        getFREQ(iteminfo.get("frequency")),
                        getWKST(iteminfo.get("recurrence")),
                        iteminfo.get("endRecurrence").replaceAll("\\-|:", "").replace(" ", "T")
                ));
                //cv.put("eventTimezone", TimeZone.getTimeZone("Asia/Seoul").toString());
                cv.put("eventTimezone", "Asia/Seoul");
                //cv.put("commentsUri", "inha"); // 일정 구분을 위한 부분
                cv.put("hasAlarm", 1);
                if (iteminfo.get("allDay").equals("YES"))
                    cv.put("allDay", 1);
                Uri inserturi = ctx.getContentResolver().insert(events, cv);

                // 알림 입력 부분
                long id = Long.parseLong(inserturi.getLastPathSegment()); // 입력된 일정의 아이디 추출
                cv = new ContentValues();
                cv.put("event_id", id);
                cv.put("method", 1);
                //몇분 전 알림 제어하는 부분
                cv.put("minutes", Integer.parseInt(alarmTime));
                ctx.getContentResolver().insert(reminders, cv);
            }
        }
        catch (Exception e) {
            Log.d("test", e.toString());
        }
    }

    public void deleteTT()
    {
        ctx.getContentResolver().delete(events, "description like ?", new String[] { "inha_%" });
    }

    public void checkCalendarPermissionBeforeDownload(String tempUrlForCalendar){
        System.out.println("on checkCalendarPermissionBeforeDownload");
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                try {
                    initTT();
                    String[] data = tempUrlForCalendar.split("\\|"); //알림시간
                    data[1] = URLDecoder.decode(data[1], "UTF-8");

                    TTDownloadThread ttThread = new TTDownloadThread(data[1], ctx);
                    ttThread.start();
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (Exception ex) {
                    Log.e("Error", ex.toString());
                    ex.printStackTrace();
                }
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                Log.i("permission", "permission denied on checkCalendarPermissionBeforeDownload");
            }
        };

        TedPermission.with(ctx)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("캘린더 권한을 승인하지 않으시면 이 기능을 사용할 수 없습니다.\n\n[설정] > [애플리케이션] > [인하대학교] > [권한] 에서 캘린더 권한을 부여해주세요.")
                .setPermissions(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR)
                .check();
    }

    public void checkCalendarPermissionBeforeDelete(){
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                try {
                    initTT();
                    deleteTT();
                } catch (Exception ex) {
                    Log.e("Error", ex.toString());
                }
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                Log.i("permission", "permission denied on checkCalendarPermissionBeforeDelete");
            }
        };

        TedPermission.with(ctx)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("캘린더 권한을 승인하지 않으시면 이 기능을 사용할 수 없습니다.\n\n[설정] > [애플리케이션] > [인하대학교] > [권한] 에서 캘린더 권한을 부여해주세요.")
                .setPermissions(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR)
                .check();
    }

    // 캘린더 입력을 위한 함수
    protected String getFREQ(String frequency) {
        if (frequency.equals("매주"))
            return "WEEKLY";
        return null;
    }

    protected String getWKST(String recurrence) {
        switch(recurrence.charAt(0)) {
            case '월':
                return "MO";
            case '화':
                return "TU";
            case '수':
                return "WE";
            case '목':
                return "TH";
            case '금':
                return "FR";
            case '토':
                return "SA";
            default:
                return "SU";
        }
    }

    public class TTDownloadThread extends Thread {
        private String alarmTime;

        public TTDownloadThread(String alarmTime, Context ctx){
            this.alarmTime = alarmTime;
        }
        public void run() {
            deleteTT();
            String rsult = InhaUtility.downloadHtmlWithSession(InhaUtility.ROOTURL + "/app_xml/app_xml.aspx?p_xmlgubun=sch");
            ParseXML(rsult, alarmTime);
        }
    }

}
