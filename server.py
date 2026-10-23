#!/usr/bin/env python3
"""Dial & Form storefront server with restricted static files and API proxy."""

from http.client import HTTPConnection
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from mimetypes import guess_type
from pathlib import Path
from urllib.parse import unquote, urlsplit
import argparse
import json
import os
import re


ROOT = Path(__file__).resolve().parent
PUBLIC_ROOT_FILES = {"index.html", "styles.css", "storefront.css", "app.js"}
PUBLIC_JS_FILES = {"api.js", "ui.js", "storefront.js"}
PUBLIC_CATALOG_FILES = {"placeholder-watch.svg"}
PUBLIC_FRAME = re.compile(r"frame_(?:00[0-9]{2}|01[0-9]{2}|02[0-3][0-9]|0240)\.webp\Z")


class StorefrontHandler(BaseHTTPRequestHandler):
    server_version = "DialFormStorefront/1.0"

    def do_GET(self):
        requested_path = unquote(urlsplit(self.path).path)
        if requested_path == "/healthz":
            self._send_text(200, "ok")
            return
        if self._is_api_request():
            self._proxy_api_request()
            return
        if requested_path in ("/", "/index.html") or (not Path(requested_path).suffix):
            self._serve_index()
            return
        public_file = self._public_file(requested_path)
        if public_file is None:
            self.send_error(404, "Not found.")
            return
        self._serve_file(public_file)

    def do_HEAD(self):
        requested_path = unquote(urlsplit(self.path).path)
        if requested_path == "/healthz":
            self.send_response(200)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", "2")
            self.end_headers()
            return
        if self._is_api_request():
            self._proxy_api_request()
            return
        if requested_path in ("/", "/index.html") or not Path(requested_path).suffix:
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Cache-Control", "no-cache")
            self.end_headers()
            return
        public_file = self._public_file(requested_path)
        if public_file is None:
            self.send_error(404, "Not found.")
            return
        self.send_response(200)
        self.send_header("Content-Type", guess_type(str(public_file))[0] or "application/octet-stream")
        self.send_header("Content-Length", str(public_file.stat().st_size))
        self.send_header("Cache-Control", "public, max-age=86400")
        self.end_headers()

    def do_POST(self):
        self._dispatch_api_request()

    def do_PUT(self):
        self._dispatch_api_request()

    def do_PATCH(self):
        self._dispatch_api_request()

    def do_DELETE(self):
        self._dispatch_api_request()

    def do_OPTIONS(self):
        self._dispatch_api_request()

    def _is_api_request(self):
        path = urlsplit(self.path).path
        return path == "/api/v1" or path.startswith("/api/v1/")

    def _dispatch_api_request(self):
        if not self._is_api_request():
            self.send_error(404, "Not found.")
            return
        self._proxy_api_request()

    def _proxy_api_request(self):
        parsed = urlsplit(self.path)
        forwarded_headers = {}
        for name in ("Accept", "Content-Type", "Authorization"):
            value = self.headers.get(name)
            if value:
                forwarded_headers[name] = value
        length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(length) if length else None
        connection = HTTPConnection(self.server.backend_host, self.server.backend_port, timeout=20)
        try:
            connection.request(self.command, parsed.path + (f"?{parsed.query}" if parsed.query else ""), body, forwarded_headers)
            response = connection.getresponse()
            payload = response.read()
            self.send_response(response.status, response.reason)
            hop_by_hop = {"connection", "keep-alive", "proxy-authenticate", "proxy-authorization", "te", "trailers", "transfer-encoding", "upgrade"}
            for name, value in response.getheaders():
                if name.lower() not in hop_by_hop:
                    self.send_header(name, value)
            self.end_headers()
            if payload:
                self.wfile.write(payload)
        except OSError:
            self.send_error(502, "The local development API is unavailable.")
        finally:
            connection.close()

    @staticmethod
    def _public_file(requested_path):
        relative = requested_path.lstrip("/")
        parts = Path(relative).parts
        if len(parts) == 1 and parts[0] in PUBLIC_ROOT_FILES:
            candidate = ROOT / parts[0]
        elif len(parts) == 2 and parts[0] == "js" and parts[1] in PUBLIC_JS_FILES:
            candidate = ROOT / "js" / parts[1]
        elif len(parts) == 3 and parts[0] == "assets" and parts[1] == "catalog" and parts[2] in PUBLIC_CATALOG_FILES:
            candidate = ROOT / "assets" / "catalog" / parts[2]
        elif len(parts) == 3 and parts[0] == "assets" and parts[1] == "frames" and PUBLIC_FRAME.fullmatch(parts[2]):
            candidate = ROOT / "assets" / "frames" / parts[2]
        else:
            return None
        try:
            candidate = candidate.resolve(strict=True)
            candidate.relative_to(ROOT)
        except (FileNotFoundError, ValueError):
            return None
        return candidate if candidate.is_file() else None

    def _serve_file(self, path):
        try:
            size = path.stat().st_size
            self.send_response(200)
            self.send_header("Content-Type", guess_type(str(path))[0] or "application/octet-stream")
            self.send_header("Content-Length", str(size))
            self.send_header("Cache-Control", "public, max-age=86400")
            self.end_headers()
            with path.open("rb") as content:
                while chunk := content.read(64 * 1024):
                    self.wfile.write(chunk)
        except OSError:
            self.send_error(404, "Not found.")

    def _send_text(self, status, content):
        payload = content.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(payload)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(payload)

    def _serve_index(self):
        payload = (ROOT / "index.html").read_bytes()
        public_config = json.dumps({
            "url": os.environ.get("SUPABASE_URL", ""),
            "publishableKey": os.environ.get("SUPABASE_PUBLISHABLE_KEY", ""),
        }).replace("<", "\\u003c")
        injection = (
            '<head>\n    <script>window.ATELIER_API_URL = "/api/v1"; '
            f'window.DIAL_FORM_SUPABASE_CONFIG = {public_config};</script>'
        ).encode("utf-8")
        payload = payload.replace(b"<head>", injection, 1)
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(payload)))
        self.send_header("Cache-Control", "no-cache")
        self.end_headers()
        self.wfile.write(payload)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Serve the Dial & Form storefront.")
    parser.add_argument("--host", default=os.environ.get("HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.environ.get("PORT", "8000")))
    parser.add_argument("--backend-hostport", default=os.environ.get("BACKEND_HOSTPORT", "127.0.0.1:8080"))
    parser.add_argument("--backend-port", type=int, default=int(os.environ.get("BACKEND_PORT", "8080")))
    args = parser.parse_args()
    server = ThreadingHTTPServer((args.host, args.port), StorefrontHandler)
    if args.backend_hostport == "127.0.0.1:8080" and args.backend_port != 8080:
        args.backend_hostport = f"127.0.0.1:{args.backend_port}"
    backend = urlsplit(f"//{args.backend_hostport}")
    if not backend.hostname:
        raise SystemExit("BACKEND_HOSTPORT must be in host:port form.")
    server.backend_host = backend.hostname
    server.backend_port = backend.port or 8080
    print(f"Dial & Form available at http://{args.host}:{args.port}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
