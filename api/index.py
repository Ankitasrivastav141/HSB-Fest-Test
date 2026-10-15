import sys
from pathlib import Path
from fastapi import Request

# Add project root to sys.path so 'backend' can be imported by Vercel
root_dir = Path(__file__).resolve().parent.parent
if str(root_dir) not in sys.path:
    sys.path.insert(0, str(root_dir))

from backend.main import app

# Middleware to handle Vercel's serverless path mapping
@app.middleware("http")
async def vercel_path_normalizer(request: Request, call_next):
    path = request.scope.get("path", "")
    
    # If Vercel rewrote /api/... to /api/index.py or /api/index
    if path in ("/api/index.py", "/api/index", "/api/index.py/", "/api/index/"):
        matched = (
            request.headers.get("x-matched-path")
            or request.headers.get("x-forwarded-uri")
            or request.headers.get("x-now-route-matches")
        )
        if matched and not matched.startswith("/api/index"):
            request.scope["path"] = matched

    # If the path has /api/index.py prefix attached
    p = request.scope.get("path", "")
    if p.startswith("/api/index.py"):
        rest = p[len("/api/index.py"):]
        if rest:
            request.scope["path"] = rest

    # If Vercel passed without /api prefix
    p = request.scope.get("path", "")
    if p.startswith("/events") or p.startswith("/registrations") or p.startswith("/stats"):
        # We also support both in main.py, but normalize to /api/...
        pass

    return await call_next(request)
