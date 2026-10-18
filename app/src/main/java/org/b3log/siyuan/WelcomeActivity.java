package org.b3log.siyuan;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class WelcomeActivity extends AppCompatActivity {

    private static final String TAG = "WELCOME";
    private static final int REQ_PERM_SETTING = 1001;
    private static final int REQ_NEW_WS = 1002;
    private static final int REQ_OPEN_WS = 1003;

    // 和 MainActivity 保持一致的 SP 名字和 Key
    private static final String PREFS_NAME = "siyuan_prefs";
    private static final String KEY_WORKSPACE_PATH = "workspace_path";

    private String pendingBlockURL = "";
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        pendingBlockURL = getIntent().getStringExtra("blockURL");
        if (pendingBlockURL == null) pendingBlockURL = "";

        Button btnPerm = findViewById(R.id.btn_permission);
        Button btnNew  = findViewById(R.id.btn_new);
        Button btnOpen = findViewById(R.id.btn_open);
        tvStatus = findViewById(R.id.tv_status);

        btnPerm.setOnClickListener(v -> requestAllFilesAccess());
        btnNew.setOnClickListener(v -> pickFolder(REQ_NEW_WS));
        btnOpen.setOnClickListener(v -> pickFolder(REQ_OPEN_WS));

        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        if (tvStatus == null) return;
        tvStatus.setText(hasAllFilesAccess()
                ? getString(R.string.status_permission_granted)
                : getString(R.string.status_permission_denied));
    }

    private void requestAllFilesAccess() {
        if (hasAllFilesAccess()) {
            Toast.makeText(this, getString(R.string.toast_permission_granted), Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            i.setData(Uri.parse("package:" + getPackageName()));
            try {
                startActivityForResult(i, REQ_PERM_SETTING);
            } catch (Exception e) {
                startActivityForResult(
                        new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
                        REQ_PERM_SETTING);
            }
        } else {
            requestPermissions(
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQ_PERM_SETTING);
        }
    }

    private void pickFolder(int req) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, req);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_PERM_SETTING) {
            refreshStatus();
            return;
        }
        if (resultCode != Activity.RESULT_OK || data == null) return;

        Uri treeUri = data.getData();
        if (treeUri == null) return;

        int flags = data.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(treeUri, flags);
        } catch (SecurityException e) {
            Log.w(TAG, "takePersistableUriPermission fail", e);
        }

        String path = getPathFromTreeUri(treeUri); // 直接调下面的方法
        Log.d(TAG, "treeUri=" + treeUri + " path=" + path);
        if (path == null) {
            Toast.makeText(this, getString(R.string.toast_cannot_parse_path), Toast.LENGTH_LONG).show();
            return;
        }

        File dir = new File(path);
        if (!dir.canRead() || !dir.canWrite()) {
            Toast.makeText(this, getString(R.string.toast_cannot_write), Toast.LENGTH_LONG).show();
            return;
        }

        if (requestCode == REQ_NEW_WS) {
            if (!dir.exists() && !dir.mkdirs()) {
                Toast.makeText(this, getString(R.string.toast_create_dir_failed, path), Toast.LENGTH_LONG).show();
                return;
            }
            if (new File(dir, "conf/conf.json").exists()) {
                Toast.makeText(this, getString(R.string.toast_existing_workspace), Toast.LENGTH_LONG).show();
            }
        } else {
            if (!new File(dir, "conf/conf.json").exists()) {
                Toast.makeText(this, getString(R.string.toast_not_valid_workspace), Toast.LENGTH_LONG).show();
                return;
            }
        }

        // 保存路径到 SharedPreferences 并跳转
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_WORKSPACE_PATH, path).apply();

        startMain(path);
    }

    private void startMain(String workspace) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("workspace", workspace);
        if (!pendingBlockURL.isEmpty()) {
            i.putExtra("blockURL", pendingBlockURL);
        }
        startActivity(i);
        finish();
    }

    private boolean hasAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    // 直接在这个类里实现路径解析，不用管 PathUtils
    private String getPathFromTreeUri(Uri treeUri) {
        if (treeUri == null) return null;
        try {
            String docId = DocumentsContract.getTreeDocumentId(treeUri);
            docId = Uri.decode(docId);
            String[] split = docId.split(":");
            String type = split[0];
            String relativePath = split.length > 1 ? split[1] : "";

            String basePath;
            switch (type.toLowerCase()) {
                case "primary":
                    basePath = Environment.getExternalStorageDirectory().getAbsolutePath();
                    break;
                case "home":
                    basePath = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
                    break;
                default:
                    basePath = "/storage/" + type;
                    break;
            }
            return relativePath.isEmpty() ? basePath : basePath + "/" + relativePath;
        } catch (Exception e) {
            Log.e(TAG, "getPathFromTreeUri fail", e);
            return null;
        }
    }
}