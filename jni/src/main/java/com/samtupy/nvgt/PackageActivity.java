package com.samtupy.nvgt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class PackageActivity extends Activity {
    private static final String TAG = "PackageActivity";
    private static final int REQUEST_SCRIPT = 1;
    
    private TextView scriptPathText;
    private EditText appNameInput;
    private EditText packageInput;
    private Spinner minSdkSpinner;
    private Spinner archSpinner;
    private CheckBox internetCheck;
    private CheckBox storageCheck;
    private CheckBox microphoneCheck;
    private CheckBox vibrateCheck;
    private CheckBox audioCheck;
    private Button selectScriptBtn;
    private Button buildBtn;
    
    private String selectedScriptPath = "";
    private ProgressDialog progressDialog;
    private Handler mainHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 设置布局
        ScrollView scrollView = new ScrollView(this);
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(40, 40, 40, 40);
        scrollView.addView(mainLayout);
        
        // 标题
        TextView title = new TextView(this);
        title.setText("APK打包配置");
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        mainLayout.addView(title);
        
        // 脚本选择
        TextView scriptLabel = new TextView(this);
        scriptLabel.setText("已选脚本: 未选择");
        scriptLabel.setPadding(0, 30, 0, 10);
        scriptPathText = scriptLabel;
        mainLayout.addView(scriptLabel);
        
        selectScriptBtn = new Button(this);
        selectScriptBtn.setText("选择脚本文件");
        selectScriptBtn.setOnClickListener(v -> selectScript());
        mainLayout.addView(selectScriptBtn);
        
        // 应用名称
        TextView appNameLabel = new TextView(this);
        appNameLabel.setText("应用名称:");
        appNameLabel.setPadding(0, 20, 0, 10);
        mainLayout.addView(appNameLabel);
        
        appNameInput = new EditText(this);
        appNameInput.setText("NVGT游戏");
        mainLayout.addView(appNameInput);
        
        // 包名
        TextView packageLabel = new TextView(this);
        packageLabel.setText("包名 (com.example.game):");
        packageLabel.setPadding(0, 20, 0, 10);
        mainLayout.addView(packageLabel);
        
        packageInput = new EditText(this);
        packageInput.setText("com.nvgt.game");
        mainLayout.addView(packageInput);
        
        // 最低SDK版本
        TextView sdkLabel = new TextView(this);
        sdkLabel.setText("最低Android版本:");
        sdkLabel.setPadding(0, 20, 0, 10);
        mainLayout.addView(sdkLabel);
        
        minSdkSpinner = new Spinner(this);
        List<String> sdkVersions = new ArrayList<>();
        sdkVersions.add("Android 8.0 (API 26)");
        sdkVersions.add("Android 9.0 (API 28)");
        sdkVersions.add("Android 10.0 (API 29)");
        sdkVersions.add("Android 11.0 (API 30)");
        sdkVersions.add("Android 12.0 (API 31)");
        sdkVersions.add("Android 13.0 (API 33)");
        sdkVersions.add("Android 14.0 (API 34)");
        sdkVersions.add("Android 15.0 (API 35)");
        ArrayAdapter<String> sdkAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, sdkVersions);
        sdkAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        minSdkSpinner.setAdapter(sdkAdapter);
        minSdkSpinner.setSelection(3);
        mainLayout.addView(minSdkSpinner);
        
        // CPU架构
        TextView archLabel = new TextView(this);
        archLabel.setText("CPU架构:");
        archLabel.setPadding(0, 20, 0, 10);
        mainLayout.addView(archLabel);
        
        archSpinner = new Spinner(this);
        List<String> archOptions = new ArrayList<>();
        archOptions.add("仅64位 (推荐)");
        archOptions.add("仅32位");
        archOptions.add("32位 + 64位");
        ArrayAdapter<String> archAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, archOptions);
        archAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        archSpinner.setAdapter(archAdapter);
        mainLayout.addView(archSpinner);
        
        // 权限
        TextView permLabel = new TextView(this);
        permLabel.setText("权限:");
        permLabel.setTextSize(18);
        permLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        permLabel.setPadding(0, 30, 0, 10);
        mainLayout.addView(permLabel);
        
        internetCheck = new CheckBox(this);
        internetCheck.setText("互联网访问");
        internetCheck.setChecked(true);
        mainLayout.addView(internetCheck);
        
        storageCheck = new CheckBox(this);
        storageCheck.setText("存储访问");
        storageCheck.setChecked(true);
        mainLayout.addView(storageCheck);
        
        microphoneCheck = new CheckBox(this);
        microphoneCheck.setText("麦克风");
        mainLayout.addView(microphoneCheck);
        
        vibrateCheck = new CheckBox(this);
        vibrateCheck.setText("震动");
        vibrateCheck.setChecked(true);
        mainLayout.addView(vibrateCheck);
        
        audioCheck = new CheckBox(this);
        audioCheck.setText("音频录制");
        mainLayout.addView(audioCheck);
        
        // 打包按钮
        buildBtn = new Button(this);
        buildBtn.setText("开始打包");
        buildBtn.setTextSize(20);
        buildBtn.setPadding(0, 40, 0, 20);
        buildBtn.setOnClickListener(v -> startBuild());
        mainLayout.addView(buildBtn);
        
        // 返回按钮
        Button backBtn = new Button(this);
        backBtn.setText("返回主菜单");
        backBtn.setOnClickListener(v -> finish());
        mainLayout.addView(backBtn);
        
        setContentView(scrollView);
    }
    
    private void selectScript() {
        // 调用Native方法打开文件选择器
        try {
            String path = native_simple_file_open_dialog("NVGT scripts:nvgt");
            if (path != null && !path.isEmpty()) {
                selectedScriptPath = path;
                scriptPathText.setText("已选脚本: " + new File(path).getName());
                // 自动填充应用名称
                String fileName = new File(path).getName();
                appNameInput.setText(fileName.replace(".nvgt", "").replace("_", " "));
            }
        } catch (Exception e) {
            Log.e(TAG, "选择脚本失败", e);
            Toast.makeText(this, "选择脚本失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void startBuild() {
        // 验证输入
        if (selectedScriptPath.isEmpty()) {
            Toast.makeText(this, "请先选择脚本文件", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String appName = appNameInput.getText().toString().trim();
        String packageName = packageInput.getText().toString().trim();
        
        if (appName.isEmpty()) {
            Toast.makeText(this, "请输入应用名称", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!packageName.matches("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")) {
            Toast.makeText(this, "包名格式不正确，示例: com.example.game", Toast.LENGTH_LONG).show();
            return;
        }
        
        // 显示确认对话框
        new AlertDialog.Builder(this)
            .setTitle("确认打包")
            .setMessage("应用名称: " + appName + "\n" +
                       "包名: " + packageName + "\n" +
                       "脚本: " + new File(selectedScriptPath).getName() + "\n\n" +
                       "注意：APK打包功能需要预先配置APK模板。\n" +
                       "完整打包功能将在后续版本提供。")
            .setPositiveButton("确定", (dlg, which) -> {
                showInfoDialog();
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void showInfoDialog() {
        new AlertDialog.Builder(this)
            .setTitle("功能说明")
            .setMessage("APK打包功能正在开发中。\n\n" +
                       "当前版本提供配置界面，完整打包需要：\n" +
                       "1. APK模板文件\n" +
                       "2. 签名配置\n" +
                       "3. 构建工具链\n\n" +
                       "完整功能将在下一个版本提供。\n\n" +
                       "感谢您的耐心等待！")
            .setPositiveButton("确定", null)
            .show();
    }
    
    private native String native_simple_file_open_dialog(String filter);
    
    static {
        System.loadLibrary("nvgt");
    }
}
