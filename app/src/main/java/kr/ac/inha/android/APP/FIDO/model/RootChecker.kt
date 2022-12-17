package kr.ac.inha.android.APP.FIDO.model

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.RuntimeException
import java.nio.charset.Charset

class RootChecker (private val context : Context){
    private val rootFiles = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "system/usr/we-need-root/",
            "/data/local/xbin/su",
            "data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/su/bin",
            "/system/xbin/daemonsu"
    )

    private val rootPackage = arrayOf(
            "com.devadvance.rootcloak",
            "com.devadvance.rootcloakplus",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "de.robv.android.xposed.installer",
            "com.saurik.substrate",
            "com.zachspong.temprootremovejb",
            "com.amphoras.hidemyroot",
            "com.amphoras.hiderootPremium",
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "com.yellowes.su",
            "com.topjohnwu.magisk",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global",
            "com.alephzain.framaroot",
    )

    private val runtime by lazy {
        Runtime.getRuntime()
    }

    fun isDeviceRooted() : Boolean {
        Log.d("rootchecking", "rootchecking");
        return checkRootFiles() || checkSUExist() || checkRootPackages()
    }

    private fun checkRootFiles() : Boolean
    {
        for (path in rootFiles) {
            try {
                if(File(path).exists()){
                    return true
                }
            } catch (e : RuntimeException)
            {

            }
        }
        return false
    }

    private fun checkSUExist() : Boolean
    {
        var process : Process? = null
        val su = arrayOf("/system/xbin/which", "su")
        try {
            process = runtime.exec(su)
            BufferedReader(
                    InputStreamReader(process.inputStream,
                    Charset.forName("UTF-8")
                    )
            ).use {reader -> return reader.readLine() != null}
        } catch(e: Exception) {

        }catch (e : Exception) {

        } finally {
            process?.destroy()
        }
        return false
    }

    private fun checkRootPackages() : Boolean {
        val pm = context.packageManager
        if(pm != null) {
            for(pkg in rootPackage) {
                try{
                    pm.getPackageInfo(pkg,0)
                    return true
                } catch (ignored:PackageManager.NameNotFoundException) {
                    //fine, package doesn't exist
                }
            }
        }
        return false
    }
}