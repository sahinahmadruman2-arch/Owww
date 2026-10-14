import subprocess
import os
import glob

os.makedirs('/tmp/android_src/com/example', exist_ok=True)
os.makedirs('/tmp/android_src/bin', exist_ok=True)
os.makedirs('/tmp/android_src/dex_out', exist_ok=True)

java_code = """package com.example;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private static final int REQ_SPEECH = 101;
    private WebView webView;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#F8FAFC"));
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        tts = new TextToSpeech(this, this);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor("#F8FAFC"));
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        request.grant(request.getResources());
                    }
                });
            }
        });

        webView.setWebViewClient(new WebViewClient());

        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void startNativeSpeech() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Karllo...");
                            startActivityForResult(intent, REQ_SPEECH);
                        } catch (Exception e) {
                            webView.evaluateJavascript("console.warn('Native speech not available')", null);
                        }
                    }
                });
            }

            @JavascriptInterface
            public void speakNative(final String text) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (ttsReady && tts != null && text != null) {
                            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "karllo_voice");
                        }
                    }
                });
            }

            @JavascriptInterface
            public void stopSpeakingNative() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (tts != null) {
                            tts.stop();
                        }
                    }
                });
            }
        }, "AndroidVoiceBridge");

        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                String safeText = spokenText.replace("'", "\\\\'");
                webView.evaluateJavascript("if (window.onNativeSpeechResult) window.onNativeSpeechResult('" + safeText + "');", null);
            }
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true;
            tts.setLanguage(Locale.US);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
"""

with open('/tmp/android_src/com/example/MainActivity.java', 'w') as f:
    f.write(java_code)

res = subprocess.run([
    'javac',
    '-cp', '/opt/android/sdk/platforms/android-36/android.jar',
    '-d', '/tmp/android_src/bin',
    '/tmp/android_src/com/example/MainActivity.java'
], capture_output=True, text=True)
print('javac exit:', res.returncode, res.stderr)

class_files = glob.glob('/tmp/android_src/bin/com/example/*.class')
print('Found classes:', len(class_files))

d8_res = subprocess.run([
    '/opt/android/sdk/build-tools/36.0.0/d8',
    '--lib', '/opt/android/sdk/platforms/android-36/android.jar',
    '--output', '/tmp/android_src/dex_out',
    *class_files
], capture_output=True, text=True)
print('d8 exit:', d8_res.returncode, d8_res.stderr)
print('dex created:', os.path.exists('/tmp/android_src/dex_out/classes.dex'))
