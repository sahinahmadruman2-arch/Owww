import os
import sys
import glob
import zlib
import hashlib
import struct
import shutil
import zipfile
import subprocess

print("=== [1] Compiling Enhanced MainActivity.java ===")
os.makedirs('/tmp/android_src/com/example', exist_ok=True)
os.makedirs('/tmp/android_src/bin', exist_ok=True)
os.makedirs('/tmp/android_src/dex_out', exist_ok=True)

java_code = """package com.example;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private static final String TAG = "KarlloNative";
    private static final int REQ_SPEECH = 101;
    private WebView webView;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Professional White Theme Status Bar
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
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
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

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("KarlloWebView", consoleMessage.message() + " -- Line "
                    + consoleMessage.lineNumber() + " of "
                    + consoleMessage.sourceId());
                return true;
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
                            if (!SpeechRecognizer.isRecognitionAvailable(MainActivity.this)) {
                                Log.w(TAG, "Speech recognition not available on system");
                                webView.evaluateJavascript("if (window.onNativeSpeechError) window.onNativeSpeechError('ActivityNotFoundException');", null);
                                return;
                            }
                            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Karllo AI...");
                            startActivityForResult(intent, REQ_SPEECH);
                        } catch (Exception e) {
                            Log.w(TAG, "Native speech trigger exception: " + e.getMessage());
                            webView.evaluateJavascript("if (window.onNativeSpeechError) window.onNativeSpeechError('" + e.getClass().getSimpleName() + "');", null);
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

            @JavascriptInterface
            public boolean isVoiceAvailable() {
                return ttsReady;
            }
        }, "AndroidVoiceBridge");

        // Load bundled White Professional Voice Assistant
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SPEECH) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (results != null && !results.isEmpty()) {
                    String spokenText = results.get(0);
                    String safeText = spokenText.replace("\\\\", "\\\\\\\\").replace("'", "\\\\'");
                    webView.evaluateJavascript("if (window.onNativeSpeechResult) window.onNativeSpeechResult('" + safeText + "');", null);
                }
            } else {
                webView.evaluateJavascript("if (window.onNativeSpeechResult) window.onNativeSpeechResult('');", null);
            }
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true;
            tts.setLanguage(Locale.US);
            Log.d(TAG, "Native TextToSpeech initialized successfully");
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
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
    '/tmp/android_src/com/example/MainActivity.java'], capture_output=True, text=True)

if res.returncode != 0:
    print("javac error:", res.stderr)
    sys.exit(1)

class_files = glob.glob('/tmp/android_src/bin/com/example/*.class')
d8_res = subprocess.run([
    '/opt/android/sdk/build-tools/36.0.0/d8',
    '--lib', '/opt/android/sdk/platforms/android-36/android.jar',
    '--output', '/tmp/android_src/dex_out',
    *class_files], capture_output=True, text=True)

if d8_res.returncode != 0:
    print("d8 error:", d8_res.stderr)
    sys.exit(1)

print("MainActivity classes compiled to classes.dex successfully.")

print("=== [2] Patching classes9.dex ===")
with zipfile.ZipFile('UrBots7.apk') as z:
    c9_bytes = bytearray(z.read('classes9.dex'))

sid752_off = 132189
if c9_bytes[sid752_off:sid752_off+28] == b'\x1aLcom/example/MainActivity;\x00':
    c9_bytes[sid752_off:sid752_off+28] = b'\x1aLcom/example/PastActivity;\x00'
    sha = hashlib.sha1(c9_bytes[32:]).digest()
    c9_bytes[12:32] = sha
    chk = zlib.adler32(c9_bytes[12:]) & 0xffffffff
    c9_bytes[8:12] = struct.pack('<I', chk)
    print('Patched classes9.dex.')
else:
    print('classes9.dex already PastActivity.')

print("=== [3] Packaging Complete APK with Web Assets and New Logo Icons ===")
web_files = [
    'index.html', 'style.css', 'bundle.js', 'logo.png', 'logo.jpg',
    'speech-module.js', 'audio-visualizer.js', 'karllo-core.js', 'app.js'
]

# Mapping for launcher icons
icon_replacements = {
    'res/mipmap-mdpi-v4/ic_launcher.png': '/tmp/icons/ic_launcher_mdpi.png',
    'res/mipmap-hdpi-v4/ic_launcher.png': '/tmp/icons/ic_launcher_hdpi.png',
    'res/mipmap-xhdpi-v4/ic_launcher.png': '/tmp/icons/ic_launcher_xhdpi.png',
    'res/mipmap-xxhdpi-v4/ic_launcher.png': '/tmp/icons/ic_launcher_xxhdpi.png',
    'res/mipmap-xxxhdpi-v4/ic_launcher.png': '/tmp/icons/ic_launcher_xxxhdpi.png',
    'res/mipmap-mdpi-v4/ic_launcher_round.png': '/tmp/icons/ic_launcher_mdpi.png',
    'res/mipmap-hdpi-v4/ic_launcher_round.png': '/tmp/icons/ic_launcher_hdpi.png',
    'res/mipmap-xhdpi-v4/ic_launcher_round.png': '/tmp/icons/ic_launcher_xhdpi.png',
    'res/mipmap-xxhdpi-v4/ic_launcher_round.png': '/tmp/icons/ic_launcher_xxhdpi.png',
    'res/mipmap-xxxhdpi-v4/ic_launcher_round.png': '/tmp/icons/ic_launcher_xxxhdpi.png',
}

with zipfile.ZipFile('UrBots7.apk', 'r') as zin:
    with zipfile.ZipFile('/tmp/karllo_full.apk', 'w', compression=zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            if item.filename == 'classes9.dex':
                zout.writestr(item, c9_bytes)
            elif item.filename in icon_replacements and os.path.exists(icon_replacements[item.filename]):
                with open(icon_replacements[item.filename], 'rb') as f:
                    zout.writestr(item, f.read())
            elif item.filename == 'classes14.dex' or item.filename.startswith('assets/'):
                continue # will write fresh
            elif not item.filename.startswith('META-INF/'):
                zout.writestr(item, zin.read(item.filename))
        
        # Write fresh classes14.dex
        with open('/tmp/android_src/dex_out/classes.dex', 'rb') as f:
            zout.writestr('classes14.dex', f.read())

        # Write fresh assets
        for wf in web_files:
            local_path = os.path.join('web', wf)
            if os.path.exists(local_path):
                with open(local_path, 'rb') as f:
                    zout.writestr(f'assets/{wf}', f.read())
                    print(f"Bundled assets/{wf}")

print("=== [4] Aligning and Signing APK ===")
subprocess.run([
    '/opt/android/sdk/build-tools/36.0.0/zipalign', '-f', '-p', '4',
    '/tmp/karllo_full.apk', '/tmp/karllo_full_aligned.apk'], check=True)

subprocess.run([
    '/opt/android/sdk/build-tools/36.0.0/apksigner', 'sign',
    '--ks', 'debug.keystore', '--ks-pass', 'pass:android',
    '--ks-key-alias', 'androiddebugkey',
    '/tmp/karllo_full_aligned.apk'], check=True)

print("=== [5] Verifying Signature & DEX Bytecode ===")
v_res = subprocess.run([
    '/opt/android/sdk/build-tools/36.0.0/apksigner', 'verify', '-v',
    '/tmp/karllo_full_aligned.apk'], capture_output=True, text=True)
print("apksigner:", v_res.stdout.strip())

d_res = subprocess.run([
    '/opt/android/sdk/build-tools/36.0.0/dexdump', '-c',
    '/tmp/karllo_full_aligned.apk'], capture_output=True, text=True)
print("dexdump returncode:", d_res.returncode)

if v_res.returncode == 0 and d_res.returncode == 0:
    os.makedirs('.build-outputs', exist_ok=True)
    os.makedirs('app/build/outputs/apk/debug', exist_ok=True)
    shutil.copy('/tmp/karllo_full_aligned.apk', 'UrBots7.apk')
    shutil.copy('/tmp/karllo_full_aligned.apk', '.build-outputs/app-debug.apk')
    shutil.copy('/tmp/karllo_full_aligned.apk', 'app/build/outputs/apk/debug/app-debug.apk')
    print("SUCCESS: All APK targets updated and verified successfully!")
else:
    print("APK verification failed!")
    sys.exit(1)
