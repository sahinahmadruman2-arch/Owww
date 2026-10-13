#!/usr/bin/env python3
"""Karllo AI - Resilient Multi-Threaded Web Server & API Bridge
==============================================================
Serves the Karllo Voice Interaction Module on port 3000
Proxied automatically by Nginx to the AI Studio preview URL.
"""

import os
import sys
import json
import mimetypes
from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler

PORT = 3000
WEB_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT_DIR = os.path.dirname(WEB_DIR)

mimetypes.init()
mimetypes.add_type("application/javascript", ".js")
mimetypes.add_type("text/css", ".css")
mimetypes.add_type("text/html", ".html")
mimetypes.add_type("application/json", ".json")

class KarlloHTTPHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=WEB_DIR, **kwargs)

    def log_message(self, format, *args):
        # Quiet logging to prevent pipe buffer saturation
        sys.stderr.write(f"[Karllo Web] {self.address_string()} - {format % args}\n")

    def do_GET(self):
        try:
            # Serve root files (training scripts and notebooks)
            if self.path in ["/train_urbots7_pytorch.py", "/UrBots7_Free_GPU_Training_Colab.ipynb", "/URBOTS7_TRAINING_GUIDE.md"]:
                target_path = os.path.join(ROOT_DIR, self.path.lstrip("/"))
                if os.path.exists(target_path):
                    self.send_response(200)
                    self.send_header("Content-Type", "application/octet-stream")
                    self.send_header("Content-Disposition", f'attachment; filename="{os.path.basename(target_path)}"')
                    self.send_header("Content-Length", str(os.path.getsize(target_path)))
                    self.end_headers()
                    with open(target_path, "rb") as f:
                        self.wfile.write(f.read())
                    return

            # API Endpoints
            if self.path == "/api/status":
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Access-Control-Allow-Origin", "*")
                self.end_headers()
                status_data = {
                    "name": "Karllo AI",
                    "model": "UrBots7",
                    "architecture": "6-Layer Causal Transformer Decoder with Multi-Head Self-Attention",
                    "parameters": 2428928,
                    "vocab_size": 4015,
                    "dataset_scale": "Trillion-word streaming ingestion ready",
                    "infinity_analysis": "Active",
                    "external_llm": "Removed (100% on-device native reasoning)",
                    "voice_module": "Web Speech API (STT & TTS)"
                }
                self.wfile.write(json.dumps(status_data).encode("utf-8"))
                return

            # Default static file handler for /app/applet/web
            return super().do_GET()
        except (BrokenPipeError, ConnectionResetError):
            pass
        except Exception as e:
            sys.stderr.write(f"[Karllo Web Error] {e}\n")

class ResilientServer(ThreadingHTTPServer):
    daemon_threads = True
    allow_reuse_address = True

    def handle_error(self, request, client_address):
        # Gracefully absorb disconnected clients without exiting
        pass

def run_server():
    server_address = ("0.0.0.0", PORT)
    httpd = ResilientServer(server_address, KarlloHTTPHandler)
    print(f"[✓] Karllo AI Voice Web Server active on port {PORT}...")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping server.")
        httpd.server_close()

if __name__ == "__main__":
    run_server()
