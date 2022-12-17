package kr.ac.inha.android.APP

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.*
import kotlinx.android.synthetic.main.qrreader.*

private const val CAMERA_REQUEST_CODE = 101

class QrReader : AppCompatActivity() {

    private lateinit var codeScanner: CodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.qrreader)
        setupPermission()
        codeScanner()

        resetBtn.setOnClickListener(){
            if(codeScanner.camera == 0) {
                codeScanner.camera = 1
            }
            else {
                codeScanner.camera = 0
            }
        }
    }

    private fun codeScanner() {
        codeScanner = CodeScanner(this, scanner_view)

        codeScanner.apply {
            camera = CodeScanner.CAMERA_BACK
            formats = CodeScanner.ALL_FORMATS

            autoFocusMode = AutoFocusMode.SAFE
            scanMode = ScanMode.CONTINUOUS
            isAutoFocusEnabled = true
            isFlashEnabled = false

            decodeCallback = DecodeCallback {
                runOnUiThread {
                    //Toast.makeText(this@QrReader, "Scan result: ${it.text}", Toast.LENGTH_LONG).show()
                    //webview 시작
                    //var intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.text))
                    //startActivity(intent)
                    val intent = Intent(this@QrReader, WebViewActivity::class.java)
                    intent.putExtra("url", it.text);
                    startActivity(intent)
                }
            }

            errorCallback = ErrorCallback {
                runOnUiThread {
                    Log.e("Main", "camera initialization error : ${it.message}")
                }
            }
        }

        scanner_view.setOnClickListener {
            codeScanner.startPreview()
        }
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    private fun setupPermission()
    {
        val permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) // contextcompat은 sdk버전을 신경쓰지 않기 위해 설정
        //앱에 특정 권한을 부여했는지 확인
        if (permission != PackageManager.PERMISSION_GRANTED) // PERMISSION_GRANTED = 권한 부여 성공
        {
            makeRequest()
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE) // 만약 권한 부여가 되지 않았다면 다시 요청
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this@QrReader, "이 앱을 사용하려면 카메라 접근 권한을 허용해주세요", Toast.LENGTH_SHORT).show()
                } else {

                }
            }
        }
    }
}