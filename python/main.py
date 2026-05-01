import os
import logging
import traceback

from fastapi import FastAPI, File, UploadFile, Form, Request
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exceptions import RequestValidationError

from config import DEBUG_MODE, OPENAI_API_KEY, SAMPLE_AUDIO_BASE_PATH
from speech import analyze_audio
from openai_client import summarize_feedback_with_llm

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

INTERNAL_API_KEY = os.getenv("INTERNAL_API_KEY")
INTERNAL_ONLY = os.getenv("INTERNAL_ONLY", "true").lower() == "true"

app = FastAPI()

if not INTERNAL_ONLY:
    allowed_origins = [o.strip() for o in os.getenv("CORS_ALLOWED_ORIGINS", "").split(",") if o.strip()]
    app.add_middleware(
        CORSMiddleware,
        allow_origins=allowed_origins,
        allow_credentials=True,
        allow_methods=["GET", "POST"],
        allow_headers=["*"],
        max_age=86400,
    )


@app.middleware("http")
async def verify_internal_call(request: Request, call_next):
    if request.url.path in ("/", "/health"):
        return await call_next(request)
    if INTERNAL_API_KEY and request.headers.get("X-Internal-Key") != INTERNAL_API_KEY:
        return JSONResponse(status_code=403, content={"detail": "Forbidden"})
    return await call_next(request)


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    return JSONResponse(status_code=422, content={"detail": "Validation error", "errors": exc.errors()})


@app.exception_handler(Exception)
async def general_exception_handler(request: Request, exc: Exception):
    if DEBUG_MODE:
        return JSONResponse(
            status_code=500,
            content={"detail": str(exc), "traceback": traceback.format_exc()},
        )
    return JSONResponse(status_code=500, content={"detail": "Internal server error"})


@app.get("/")
async def health_check():
    return {"status": "ok", "service": "python-service"}


@app.post("/analyze-audio-enhanced", response_class=JSONResponse)
async def analyze_enhanced_endpoint(
    file: UploadFile = File(...),
    reference_text: str = Form(...),
    type: str = Form(None),
):
    try:
        data = await file.read()
        await file.seek(0)
        if len(data) < 1000:
            return JSONResponse(status_code=400, content={"detail": "File âm thanh quá nhỏ hoặc rỗng"})

        result = await analyze_audio(file, reference_text, type)
        return JSONResponse(content=result)
    except Exception as e:
        if DEBUG_MODE:
            return JSONResponse(
                status_code=500,
                content={"detail": str(e), "traceback": traceback.format_exc()},
            )
        return JSONResponse(status_code=500, content={"detail": str(e)})


@app.post("/summarize-feedback", response_class=JSONResponse)
async def summarize_feedback_endpoint(request: Request):
    try:
        body = await request.json()
        result = await summarize_feedback_with_llm(
            body.get("feedback_list", []),
            body.get("conversation_text", ""),
        )
        return JSONResponse(content=result)
    except Exception as e:
        if DEBUG_MODE:
            return JSONResponse(
                status_code=500,
                content={"detail": str(e), "traceback": traceback.format_exc()},
            )
        return JSONResponse(status_code=500, content={"detail": str(e)})


if __name__ == "__main__":
    import uvicorn

    port = int(os.getenv("PORT", "8000"))
    logger.info(f"Starting on port {port}, debug={DEBUG_MODE}")
    logger.info(f"OpenAI configured: {'yes' if OPENAI_API_KEY else 'no'}")
    logger.info(f"SAMPLE_AUDIO_BASE_PATH: {SAMPLE_AUDIO_BASE_PATH}")
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=False)
