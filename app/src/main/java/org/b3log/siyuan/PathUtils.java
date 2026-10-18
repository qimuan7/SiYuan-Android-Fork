package org.b3log.siyuan;

import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;

public class PathUtils {
    public static String getPathFromTreeUri(Uri treeUri) {
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
            Log.e("PathUtils", "getPathFromTreeUri fail", e);
            return null;
        }
    }
}