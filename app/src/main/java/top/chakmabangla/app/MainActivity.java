package top.chakmabangla.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String HOME = "https://chakma-bangla.top/";
    private static final String HOST = "chakma-bangla.top";
    private static final int REQ_PERMISSIONS = 100;
    private static final int REQ_FILE = 101;

    private WebView web;
    private PermissionRequest pendingRequest;
    private ValueCallback<Uri[]> filePathCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(isOnline() ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_CACHE_ELSE_NETWORK);

        web.setWebChromeClient(new WebChromeClient() {
            // Website asks for microphone / camera
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!HOST.equals(request.getOrigin().getHost())) {
                            request.deny();
                            return;
                        }
                        List<String> needed = new ArrayList<String>();
                        for (String r : request.getResources()) {
                            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r)
                                    && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                needed.add(Manifest.permission.RECORD_AUDIO);
                            }
                            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)
                                    && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                needed.add(Manifest.permission.CAMERA);
                            }
                        }
                        if (needed.isEmpty()) {
                            request.grant(request.getResources());
                        } else {
                            pendingRequest = request;
                            requestPermissions(needed.toArray(new String[0]), REQ_PERMISSIONS);
                        }
                    }
                });
            }

            // File upload and camera/audio pickers from <input type="file">
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), REQ_FILE);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if (HOST.equals(u.getHost())) return false;   // stay inside the app
                startActivity(new Intent(Intent.ACTION_VIEW, u)); // other sites open in browser
                return true;
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) {
                    v.loadData("<html><body style='font-family:sans-serif;text-align:center;padding:40px'>"
                        + "<h3>No internet connection</h3><p>Please check your connection and try again.</p>"
                        + "<button onclick=\"location.href='" + HOME + "'\" style='padding:10px 20px'>Retry</button>"
                        + "</body></html>", "text/html", "UTF-8");
                }
            }
        });

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl(HOME);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_PERMISSIONS || pendingRequest == null) return;
        List<String> allowed = new ArrayList<String>();
        for (String r : pendingRequest.getResources()) {
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r)
                    && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                allowed.add(r);
            }
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)
                    && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                allowed.add(r);
            }
        }
        if (allowed.isEmpty()) pendingRequest.deny();
        else pendingRequest.grant(allowed.toArray(new String[0]));
        pendingRequest = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE && filePathCallback != null) {
            filePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            filePathCallback = null;
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo n = cm.getActiveNetworkInfo();
        return n != null && n.isConnected();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && web.canGoBack()) { web.goBack(); return true; }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }
}
