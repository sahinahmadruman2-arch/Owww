#!/usr/bin/env python3
import os
import sys
import glob
import shutil
import zipfile
import subprocess
import hashlib
import zlib
import struct

print("[1] Compiling new White Professional MainActivity...")
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

        // Load bundled White Professional Voice Assistant
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
if res.returncode != 0:
    print("javac failed:", res.stderr)
    sys.exit(1)

class_files = glob.glob('/tmp/android_src/bin/com/example/*.class')
d8_res = subprocess.run([
    '/opt/android/sdk/build-tools/36.0.0/d8',
    '--lib', '/opt/android/sdk/platforms/android-36/android.jar',
    '--output', '/tmp/android_src/dex_out',
    *class_files
], capture_output=True, text=True)
if d8_res.returncode != 0:
    print("d8 failed:", d8_res.stderr)
    sys.exit(1)

print("[2] Patching classes9.dex to rename old MainActivity to PastActivity...")
with zipfile.ZipFile('UrBots7.apk') as z:
    c9_bytes = bytearray(z.read('classes9.dex'))

# Replace Lcom/example/MainActivity; (26 chars) with Lcom/example/PastActivity; (26 chars)
sid752_off = 132189
assert c9_bytes[sid752_off:sid752_off+28] == b'\x1aLcom/example/MainActivity;\x00'
c9_bytes[sid752_off:sid752_off+28] = b'\x1aLcom/example/PastActivity;\x00'

sha = hashlib.sha1(c9_bytes[32:]).digest()
c9_bytes[12:32] = sha
chk = zlib.adler32(c9_bytes[12:]) & 0xffffffff
c9_bytes[8:12] = struct.pack('<I', chk)

with open('/tmp/c9_patched.dex', 'wb') as f:
    f.write(c9_bytes)

print("[3] Repackaging APK with classes14.dex and bundled assets...")
web_files = [
    'index.html', 'style.css', 'app.js', 'speech-module.js',
    'audio-visualizer.js', 'karllo-core.js'
]

with zipfile.ZipFile('UrBots7.apk', 'r') as zin:
    with zipfile.ZipFile('/tmp/urbots7_prof.apk', 'w', compression=zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            if item.filename == 'classes9.dex':
                zout.writestr(item, c9_bytes)
            elif not item.filename.startswith('META-INF/'):
                zout.writestr(item, zin.read(item.filename))
        
        # Write new classes14.dex
        with open('/tmp/android_src/dex_out/classes.dex', 'rb') as f:
            zout.writestr('classes14.dex', f.read())

        # Write bundled assets
        for wf in web_files:
            local_path = os.path.join('/app/applet/web', wf)
            if not os.path.exists(local_path):
                local_path = os.path.join('web', wf)
            with open(local_path, 'rb') as f:
                content = f.read()
                zout.writestr(f'assets/{wf}', content)

print("[4] Aligning and signing APK...")
subprocess.run([
    '/opt/android/sdk/build-tools/36.0.0/zipalign', '-f', '-p', '4',
    '/tmp/urbots7_prof.apk', '/tmp/urbots7_prof_aligned.apk'
], check=True)

subprocess.run([
    '/opt/android/sdk/build-tools/36.0.0/apksigner', 'sign',
    '--ks', 'debug.keystore', '--ks-pass', 'pass:android',
    '--ks-key-alias', 'androiddebugkey',
    '/tmp/urbots7_prof_aligned.apk'
], check=True)

print("[5] Verifying signature and DEX bytecode...")
v_res = subprocess.run([
    '/opt/android/sdk/build-tools/36.0.0/apksigner', 'verify', '-v',
    '/tmp/urbots7_prof_aligned.apk'
], capture_output=True, text=True)
print("apksigner:", v_res.stdout.strip())

d_res = subprocess.run([
    '/opt/android/sdk/build-tools/36.0.0/dexdump', '-c',
    '/tmp/urbots7_prof_aligned.apk'
], capture_output=True, text=True)
print("dexdump returncode:", d_res.returncode, d_res.stderr.strip())

if v_res.returncode == 0 and d_res.returncode == 0:
    shutil.copy('/tmp/urbots7_prof_aligned.apk', 'UrBots7.apk')
    shutil.copy('/tmp/urbots7_prof_aligned.apk', '.build-outputs/app-debug.apk')
    print("SUCCESS: Both UrBots7.apk and .build-outputs/app-debug.apk updated!")
else:
    print("Verification failed!")
    sys.exit(1)
