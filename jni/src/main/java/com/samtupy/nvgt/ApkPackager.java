package com.samtupy.nvgt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.security.cert.X509Certificate;

public class ApkPackager {
    private static final String TAG = "ApkPackager";
    private static final String PREF_KEY_ALIAS = "apk_signing_alias";
    private static final String PREF_KEY_PASSWORD = "apk_signing_password";
    private static final String PREF_KEY_KEYSTORE_PATH = "apk_keystore_path";
    private static final int BUFFER_SIZE = 8192;
    
    private Activity activity;
    private Handler mainHandler;
    private ProgressDialog progressDialog;
    
    public ApkPackager(Activity activity) {
        this.activity = activity;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public interface PackCallback {
        void onProgress(String message, int percent);
        void onSuccess(File apkFile);
        void onError(String error);
    }
    
    public void packageApk(String scriptPath, String appName, String packageName,
                          int minSdk, int targetSdk, boolean arm64, boolean armv7,
                          boolean needsInternet, boolean needsStorage, 
                          boolean needsMicrophone, boolean needsVibrate,
                          boolean needsAudio, PackCallback callback) {
        
        new Thread(() -> {
            File tempDir = null;
            File outputApk = null;
            
            try {
                mainHandler.post(() -> callback.onProgress("正在准备打包环境...", 5));
                
                // 1. 创建临时目录
                tempDir = new File(activity.getCacheDir(), "apk_build_" + System.currentTimeMillis());
                tempDir.mkdirs();
                
                mainHandler.post(() -> callback.onProgress("正在解压APK模板...", 15));
                
                // 2. 解压APK模板
                File templateDir = new File(tempDir, "template");
                templateDir.mkdirs();
                extractAssetsToDir("base.apk", templateDir);
                
                mainHandler.post(() -> callback.onProgress("正在配置应用信息...", 30));
                
                // 3. 修改AndroidManifest.xml
                File manifestFile = new File(templateDir, "AndroidManifest.xml");
                if (manifestFile.exists()) {
                    String manifest = readFile(manifestFile);
                    manifest = manifest.replace("com.nvgt.template", packageName);
                    manifest = manifest.replace("NVGT Template", appName);
                    // 修改权限
                    manifest = modifyManifestPermissions(manifest, needsInternet, needsStorage, 
                            needsMicrophone, needsVibrate, needsAudio);
                    writeFile(manifestFile, manifest);
                }
                
                mainHandler.post(() -> callback.onProgress("正在复制脚本文件...", 50));
                
                // 4. 复制脚本到assets
                File assetsDir = new File(templateDir, "assets");
                assetsDir.mkdirs();
                File scriptFile = new File(scriptPath);
                File destScript = new File(assetsDir, scriptFile.getName());
                copyFile(scriptFile, destScript);
                
                mainHandler.post(() -> callback.onProgress("正在处理资源文件...", 65));
                
                // 5. 处理资源文件
                File resDir = new File(templateDir, "res");
                if (resDir.exists()) {
                    // 修改应用名称
                    File valuesDir = new File(resDir, "values");
                    if (valuesDir.exists()) {
                        File stringsFile = new File(valuesDir, "strings.xml");
                        if (stringsFile.exists()) {
                            String strings = readFile(stringsFile);
                            strings = strings.replace("NVGT Template", appName);
                            writeFile(stringsFile, strings);
                        }
                    }
                }
                
                mainHandler.post(() -> callback.onProgress("正在重新打包APK...", 80));
                
                // 6. 重新打包APK
                File unsignedApk = new File(tempDir, "unsigned.apk");
                zipDirectory(templateDir, unsignedApk);
                
                mainHandler.post(() -> callback.onProgress("正在签名APK...", 90));
                
                // 7. 签名APK
                File keystoreFile = getOrCreateKeystore();
                String alias = getOrCreateAlias();
                String password = getSigningPassword();
                
                outputApk = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), 
                        appName.replaceAll("[^a-zA-Z0-9]", "_") + ".apk");
                
                signApk(unsignedApk, outputApk, keystoreFile, password, alias, password);
                
                mainHandler.post(() -> callback.onProgress("打包完成！", 100));
                mainHandler.post(() -> callback.onSuccess(outputApk));
                
            } catch (Exception e) {
                Log.e(TAG, "打包失败", e);
                mainHandler.post(() -> callback.onError("打包失败: " + e.getMessage()));
            } finally {
                // 清理临时文件
                if (tempDir != null && tempDir.exists()) {
                    deleteRecursive(tempDir);
                }
            }
        }).start();
    }
    
    private void extractAssetsToDir(String assetName, File destDir) throws Exception {
        String[] assets = activity.getAssets().list("");
        boolean found = false;
        for (String asset : assets) {
            if (asset.equals(assetName)) {
                found = true;
                break;
            }
        }
        
        if (!found) {
            throw new Exception("未找到APK模板文件: " + assetName + "\n请确保在APK中添加base.apk模板文件。");
        }
        
        InputStream is = activity.getAssets().open(assetName);
        ZipInputStream zis = new ZipInputStream(is);
        ZipEntry entry;
        byte[] buffer = new byte[BUFFER_SIZE];
        
        while ((entry = zis.getNextEntry()) != null) {
            File file = new File(destDir, entry.getName());
            
            if (entry.isDirectory()) {
                file.mkdirs();
            } else {
                file.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(file);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zis.closeEntry();
        }
        zis.close();
        is.close();
    }
    
    private String modifyManifestPermissions(String manifest, boolean internet, boolean storage,
                                            boolean microphone, boolean vibrate, boolean audio) {
        StringBuilder sb = new StringBuilder();
        String[] lines = manifest.split("\n");
        boolean inUsesPermission = false;
        boolean addedPermissions = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            // 跳过原有的权限行
            if (line.contains("<uses-permission")) {
                continue;
            }
            
            // 在</manifest>前添加新权限
            if (line.contains("</manifest>") && !addedPermissions) {
                if (internet) sb.append("    <uses-permission android:name=\"android.permission.INTERNET\" />\n");
                if (storage) sb.append("    <uses-permission android:name=\"android.permission.READ_EXTERNAL_STORAGE\" />\n");
                if (microphone) sb.append("    <uses-permission android:name=\"android.permission.RECORD_AUDIO\" />\n");
                if (vibrate) sb.append("    <uses-permission android:name=\"android.permission.VIBRATE\" />\n");
                if (audio) sb.append("    <uses-permission android:name=\"android.permission.MODIFY_AUDIO_SETTINGS\" />\n");
                addedPermissions = true;
            }
            
            sb.append(line).append("\n");
        }
        
        return sb.toString();
    }
    
    private File getOrCreateKeystore() throws Exception {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        String keystorePath = prefs.getString(PREF_KEY_KEYSTORE_PATH, "");
        
        if (!keystorePath.isEmpty()) {
            File keystore = new File(keystorePath);
            if (keystore.exists()) {
                return keystore;
            }
        }
        
        // 创建新的keystore
        File keystoreFile = new File(activity.getFilesDir(), "signing.keystore");
        
        // 使用Java代码生成keystore
        // 这里需要调用Native方法来生成，因为Android没有keytool
        
        // 临时方案：使用内置的默认keystore
        InputStream is = activity.getAssets().open("signing.keystore");
        FileOutputStream fos = new FileOutputStream(keystoreFile);
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ((len = is.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
        }
        fos.close();
        is.close();
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_KEY_KEYSTORE_PATH, keystoreFile.getAbsolutePath());
        editor.apply();
        
        return keystoreFile;
    }
    
    private String getOrCreateAlias() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        String alias = prefs.getString(PREF_KEY_ALIAS, "");
        
        if (alias.isEmpty()) {
            alias = "nvgt_key";
            prefs.edit().putString(PREF_KEY_ALIAS, alias).apply();
        }
        
        return alias;
    }
    
    private String getSigningPassword() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        String password = prefs.getString(PREF_KEY_PASSWORD, "");
        
        if (password.isEmpty()) {
            password = "nvgt123456";
            prefs.edit().putString(PREF_KEY_PASSWORD, password).apply();
        }
        
        return password;
    }
    
    private void signApk(File unsignedApk, File signedApk, File keystore, 
                        String storePassword, String alias, String keyPassword) throws Exception {
        // 使用jarsigner签名APK
        String[] cmd = {
            "jarsigner",
            "-keystore", keystore.getAbsolutePath(),
            "-storepass", storePassword,
            "-keypass", keyPassword,
            "-signedjar", signedApk.getAbsolutePath(),
            unsignedApk.getAbsolutePath(),
            alias
        };
        
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            Log.d(TAG, "jarsigner: " + line);
        }
        
        int result = process.waitFor();
        if (result != 0) {
            throw new Exception("APK签名失败，错误码: " + result);
        }
    }
    
    private void zipDirectory(File sourceDir, File outputFile) throws Exception {
        FileOutputStream fos = new FileOutputStream(outputFile);
        ZipOutputStream zos = new ZipOutputStream(fos);
        
        addDirectoryToZip(sourceDir, sourceDir, zos);
        
        zos.close();
        fos.close();
    }
    
    private void addDirectoryToZip(File rootDir, File dir, ZipOutputStream zos) throws Exception {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        byte[] buffer = new byte[BUFFER_SIZE];
        
        for (File file : files) {
            if (file.isDirectory()) {
                addDirectoryToZip(rootDir, file, zos);
            } else {
                String relativePath = file.getAbsolutePath().substring(rootDir.getAbsolutePath().length() + 1);
                ZipEntry entry = new ZipEntry(relativePath);
                zos.putNextEntry(entry);
                
                FileInputStream fis = new FileInputStream(file);
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                fis.close();
                zos.closeEntry();
            }
        }
    }
    
    private String readFile(File file) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }
    
    private void writeFile(File file, String content) throws Exception {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(content.getBytes("UTF-8"));
        fos.close();
    }
    
    private void copyFile(File src, File dst) throws Exception {
        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(dst);
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ((len = fis.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
        }
        fis.close();
        fos.close();
    }
    
    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
    
    public void showPackageDialog() {
        activity.runOnUiThread(() -> {
            // 选择脚本文件
            String scriptPath = simple_file_open_dialog("NVGT scripts:nvgt");
            if (scriptPath == null || scriptPath.isEmpty()) {
                return;
            }
            
            showConfigDialog(scriptPath);
        });
    }
    
    private void showConfigDialog(String scriptPath) {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 10, 0, 20);
        
        // 应用名称
        EditText appNameInput = new EditText(activity);
        appNameInput.setHint("应用名称");
        appNameInput.setText("NVGT游戏");
        appNameInput.setLayoutParams(params);
        
        // 包名
        EditText packageInput = new EditText(activity);
        packageInput.setHint("包名 (com.example.game)");
        packageInput.setText("com.nvgt.game");
        packageInput.setLayoutParams(params);
        
        // 最低SDK版本
        Spinner minSdkSpinner = new Spinner(activity);
        List<String> sdkVersions = new ArrayList<>();
        sdkVersions.add("Android 8.0 (API 26)");
        sdkVersions.add("Android 9.0 (API 28)");
        sdkVersions.add("Android 10.0 (API 29)");
        sdkVersions.add("Android 11.0 (API 30)");
        sdkVersions.add("Android 12.0 (API 31)");
        sdkVersions.add("Android 13.0 (API 33)");
        sdkVersions.add("Android 14.0 (API 34)");
        sdkVersions.add("Android 15.0 (API 35)");
        ArrayAdapter<String> sdkAdapter = new ArrayAdapter<>(activity,
            android.R.layout.simple_spinner_item, sdkVersions);
        sdkAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        minSdkSpinner.setAdapter(sdkAdapter);
        minSdkSpinner.setSelection(3);
        minSdkSpinner.setLayoutParams(params);
        
        // CPU架构
        Spinner archSpinner = new Spinner(activity);
        List<String> archOptions = new ArrayList<>();
        archOptions.add("仅64位 (推荐)");
        archOptions.add("仅32位");
        archOptions.add("32位 + 64位");
        ArrayAdapter<String> archAdapter = new ArrayAdapter<>(activity,
            android.R.layout.simple_spinner_item, archOptions);
        archAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        archSpinner.setAdapter(archAdapter);
        archSpinner.setLayoutParams(params);
        
        // 权限
        CheckBox internetCheck = new CheckBox(activity);
        internetCheck.setText("互联网访问");
        internetCheck.setChecked(true);
        
        CheckBox storageCheck = new CheckBox(activity);
        storageCheck.setText("存储访问");
        storageCheck.setChecked(true);
        
        CheckBox microphoneCheck = new CheckBox(activity);
        microphoneCheck.setText("麦克风");
        
        CheckBox vibrateCheck = new CheckBox(activity);
        vibrateCheck.setText("震动");
        vibrateCheck.setChecked(true);
        
        CheckBox audioCheck = new CheckBox(activity);
        audioCheck.setText("音频录制");
        
        // 标签
        android.widget.TextView titleLabel = new android.widget.TextView(activity);
        titleLabel.setText("APK打包配置");
        titleLabel.setTextSize(20);
        titleLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        
        android.widget.TextView permLabel = new android.widget.TextView(activity);
        permLabel.setText("权限:");
        permLabel.setTextSize(16);
        permLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        permLabel.setPadding(0, 20, 0, 10);
        
        // 添加到布局
        layout.addView(titleLabel);
        layout.addView(new android.widget.TextView(activity));
        layout.addView(new android.widget.TextView(activity).setText("应用名称:"));
        layout.addView(appNameInput);
        layout.addView(new android.widget.TextView(activity).setText("包名:"));
        layout.addView(packageInput);
        layout.addView(new android.widget.TextView(activity).setText("最低Android版本:"));
        layout.addView(minSdkSpinner);
        layout.addView(new android.widget.TextView(activity).setText("CPU架构:"));
        layout.addView(archSpinner);
        layout.addView(permLabel);
        layout.addView(internetCheck);
        layout.addView(storageCheck);
        layout.addView(microphoneCheck);
        layout.addView(vibrateCheck);
        layout.addView(audioCheck);
        
        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle("APK打包配置")
            .setView(layout)
            .setPositiveButton("开始打包", null)
            .setNegativeButton("取消", null)
            .setCancelable(false)
            .create();
        
        dialog.setOnShowListener(dlg -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String appName = appNameInput.getText().toString().trim();
                String packageName = packageInput.getText().toString().trim();
                int minSdk = 26 + minSdkSpinner.getSelectedItemPosition();
                int arch = archSpinner.getSelectedItemPosition();
                
                if (appName.isEmpty()) {
                    Toast.makeText(activity, "请输入应用名称", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!packageName.matches("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")) {
                    Toast.makeText(activity, "包名格式不正确", Toast.LENGTH_LONG).show();
                    return;
                }
                
                dialog.dismiss();
                startPackaging(scriptPath, appName, packageName, minSdk, 
                              arch == 0 || arch == 2, arch == 1 || arch == 2,
                              internetCheck.isChecked(), storageCheck.isChecked(),
                              microphoneCheck.isChecked(), vibrateCheck.isChecked(),
                              audioCheck.isChecked());
            });
        });
        
        dialog.show();
    }
    
    private void startPackaging(String scriptPath, String appName, String packageName,
                               int minSdk, boolean arm64, boolean armv7,
                               boolean needsInternet, boolean needsStorage,
                               boolean needsMicrophone, boolean needsVibrate,
                               boolean needsAudio) {
        
        progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle("正在打包APK");
        progressDialog.setMessage("正在准备打包环境...");
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.show();
        
        packageApk(scriptPath, appName, packageName, minSdk, 35, arm64, armv7,
                  needsInternet, needsStorage, needsMicrophone, needsVibrate,
                  needsAudio, new PackCallback() {
            @Override
            public void onProgress(String message, int percent) {
                progressDialog.setMessage(message);
                progressDialog.setProgress(percent);
            }
            
            @Override
            public void onSuccess(File apkFile) {
                progressDialog.dismiss();
                showSuccessDialog(apkFile, appName);
            }
            
            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                new AlertDialog.Builder(activity)
                    .setTitle("打包失败")
                    .setMessage(error + "\n\n请确保APK模板文件存在于assets目录中。")
                    .setPositiveButton("确定", null)
                    .show();
            }
        });
    }
    
    private void showSuccessDialog(File apkFile, String appName) {
        new AlertDialog.Builder(activity)
            .setTitle("打包成功！")
            .setMessage("APK已成功生成！\n\n文件名: " + apkFile.getName() + "\n大小: " + (apkFile.length() / 1024) + " KB\n路径: " + apkFile.getAbsolutePath())
            .setPositiveButton("安装", (dlg, which) -> installApk(apkFile))
            .setNegativeButton("关闭", null)
            .show();
    }
    
    private void installApk(File apkFile) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            android.support.v4.content.FileProvider.getUriForFile(
                activity, 
                activity.getPackageName() + ".fileprovider",
                apkFile
            );
            
            intent.setDataAndType(
                androidx.core.content.FileProvider.getUriForFile(
                    activity,
                    activity.getPackageName() + ".fileprovider",
                    apkFile
                ),
                "application/vnd.android.package-archive"
            );
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(activity, "安装失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private native String simple_file_open_dialog(String filter);
}
