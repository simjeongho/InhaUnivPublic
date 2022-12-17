package kr.ac.inha.android.APP.vcard;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.util.ArrayList;

import kr.ac.inha.android.APP.library.InhaUtility;

public class VCard {
    private Context ctx;
    private String finm;
    private String filename;
    private String fileNM;

    public VCard(Context ctx) {
        this.ctx = ctx;
        this.filename = "";
    }


    public String getFinm() {
        return this.finm;
    }

    public void checkExternalStoragePermission(String url){
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                vcardFileControler(url);
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                Log.i("permission", "permission denied on checkExternalStoragePermission");
            }
        };

        TedPermission.with(this.ctx)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("이 기능을 사용하시려면 권한 부여가 필요합니다.\n\n[설정] > [애플리케이션] > [인하대학교] > [권한] 에서 저장 권한을 부여해주세요.")
                .setPermissions(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check();
    }

    private void vcardFileControler(String url) {
        new File(InhaUtility.DOWNLOADPATH).mkdirs();

        this.finm = Uri.parse(url).getQueryParameter("p_finm");
        this.fileNM = Uri.parse(url).getQueryParameter("pFileNm");

        if(this.fileNM != null && this.fileNM.length() > 0)
            this.finm = Uri.parse(url).getQueryParameter("pFileNm");

        if(url.contains("download") && !url.contains("Home"))
            this.finm = Uri.decode(url.substring(url.lastIndexOf("/")+1,url.length()));

        if(this.filename != null && this.filename.length() > 0)
            this.finm = Uri.parse(url).getQueryParameter("filename");

        if(url.contains("download_process.aspx")){
            this.filename = Uri.parse(url).getQueryParameter("filename");
        } else if(url.contains("staff_vcard")){
            this.finm = "staff.vcf";
        }

        // 중복 파일명 처리
        if (new File(InhaUtility.DOWNLOADPATH + "/" + this.finm).exists()) {
            String fn = this.finm.substring(0, this.finm.lastIndexOf('.'));
            String ext = this.finm.substring(this.finm.lastIndexOf('.'));

            for (int i=1;;i++) {
                String tmp = fn + "-" + i + ext;
                if (!new File(InhaUtility.DOWNLOADPATH + "/" + tmp).exists()) {
                    this.finm = tmp;
                    break;
                }
            }
        }
    }
}
