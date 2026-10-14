package com.example;

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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

        // Keep input bar visible when virtual keyboard opens
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#F8FAFC"));
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

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

            @JavascriptInterface
            public String fetchLiveUrl(final String urlString) {
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; KarlloAI) Chrome/120.0.0.0 Mobile Safari/537.36");
                    conn.setConnectTimeout(7000);
                    conn.setReadTimeout(7000);
                    int code = conn.getResponseCode();
                    if (code >= 200 && code < 300) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null && sb.length() < 60000) {
                            sb.append(line).append((char) 10);
                        }
                        reader.close();
                        return sb.toString();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Live URL fetch exception: " + e.getMessage());
                }
                return null;
            }
        }, "AndroidVoiceBridge");

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
                    char bs = 92;
                    String safeText = spokenText.replace(String.valueOf((char)39), " ");
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
