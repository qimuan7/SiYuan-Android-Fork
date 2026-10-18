package org.b3log.siyuan;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.blankj.utilcode.util.BarUtils;
import java.io.File;

public class BootActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.logInfo("boot", "Create boot activity, process [" + android.os.Process.myPid()
                + "], instance [" + System.identityHashCode(this) + "], task [" + getTaskId()
                + "], saved state [" + (null != savedInstanceState) + "]");
        setContentView(R.layout.activity_boot);
        BarUtils.setNavBarVisibility(this, false);
        dispatchNext();
    }

    private void dispatchNext() {
        // 解析 siyuan:// 深层链接
        String blockURL = "";
        try {
            final Uri blockURLUri = getIntent().getData();
            if (null != blockURLUri
                    && blockURLUri.toString().toLowerCase().startsWith("siyuan://")) {
                Log.i("boot", "Block URL [" + blockURLUri + "]");
                blockURL = blockURLUri.toString();
            }
        } catch (final Exception e) {
            Utils.logError("boot", "gets block URL failed", e);
        }

        // 🔴 使用 SharedPreferences 判断工作区是否有效（替换原来的 WorkspaceStore）
        SharedPreferences prefs = getSharedPreferences("siyuan_prefs", MODE_PRIVATE);
        final String workspace = prefs.getString("workspace_path", "");
        final boolean hasValidWorkspace = workspace != null
                && !workspace.isEmpty()
                && new File(workspace, "conf/conf.json").exists();

        Utils.logInfo("boot", "workspace=[" + workspace + "], valid=" + hasValidWorkspace);

        if (hasValidWorkspace) {
            startMainActivity(workspace, blockURL);
        } else {
            startWelcomeActivity(blockURL);
        }
    }

    private void startMainActivity(String workspace, String blockURL) {
        Utils.logInfo("boot", "Start main activity, workspace=" + workspace);
        final Intent startMainIntent = new Intent(getApplicationContext(), MainActivity.class);
        startMainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startMainIntent.putExtra("workspace", workspace);
        if (!blockURL.isEmpty()) {
            startMainIntent.putExtra("blockURL", blockURL);
        }
        startActivity(startMainIntent);
        finish();
    }

    private void startWelcomeActivity(String blockURL) {
        Utils.logInfo("boot", "Start welcome activity, no valid workspace");
        final Intent intent = new Intent(getApplicationContext(), WelcomeActivity.class);
        if (!blockURL.isEmpty()) {
            intent.putExtra("blockURL", blockURL);
        }
        startActivity(intent);
        finish();
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Utils.logInfo("boot", "Change boot activity configuration, process [" + android.os.Process.myPid()
                + "], instance [" + System.identityHashCode(this) + "], task [" + getTaskId()
                + "], configuration [" + Utils.formatInputConfiguration(newConfig) + "]");
    }

    @Override
    protected void onDestroy() {
        Utils.logInfo("boot", "Destroy boot activity, process [" + android.os.Process.myPid()
                + "], instance [" + System.identityHashCode(this) + "], task [" + getTaskId()
                + "], finishing [" + isFinishing() + "], task root [" + isTaskRoot()
                + "], changing configurations [" + isChangingConfigurations() + "], changes [0x"
                + Integer.toHexString(getChangingConfigurations()) + "]");
        super.onDestroy();
    }
}