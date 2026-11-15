"""Infraestructura manual: sirve el frontend generado y reenvia /api al backend local."""
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import argparse
import json
import urllib.request
import urllib.error

root = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument('--port', type=int, default=8137)
parser.add_argument('--backend', default='http://127.0.0.1:8080')
args = parser.parse_args()

class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *positional, **keywords):
        super().__init__(*positional, directory=str(root / 'generated/frontend'), **keywords)

    def proxy(self):
        size = int(self.headers.get('Content-Length', '0'))
        if size > 1024 * 1024:
            self.send_error(413)
            return
        payload = self.rfile.read(size) if size else None
        request = urllib.request.Request(args.backend.rstrip('/') + self.path, data=payload,
            headers={'Content-Type': self.headers.get('Content-Type', 'application/json')}, method=self.command)
        try:
            response = urllib.request.urlopen(request, timeout=20)
        except urllib.error.HTTPError as error:
            response = error
        except (urllib.error.URLError, TimeoutError, OSError):
            content = json.dumps({'detail': 'El backend no esta disponible. Puedes usar Demostracion local.'}).encode()
            self.send_response(502)
            self.send_header('Content-Type', 'application/json; charset=utf-8')
            self.send_header('Content-Length', str(len(content)))
            self.end_headers()
            self.wfile.write(content)
            return
        with response:
            content = response.read()
            self.send_response(response.code)
            self.send_header('Content-Type', response.headers.get('Content-Type', 'application/json'))
            self.send_header('Content-Length', str(len(content)))
            self.end_headers()
            self.wfile.write(content)

    def do_GET(self):
        if self.path.startswith('/api/'):
            self.proxy()
        else:
            super().do_GET()

    def do_POST(self):
        if self.path.startswith('/api/'):
            self.proxy()
        else:
            self.send_error(405)

print(f'Aero: http://127.0.0.1:{args.port}', flush=True)
print('Modo demostracion disponible sin Java ni PostgreSQL. Ctrl+C para detener.', flush=True)
try:
    with ThreadingHTTPServer(('127.0.0.1', args.port), Handler) as server:
        server.serve_forever()
except KeyboardInterrupt:
    pass
