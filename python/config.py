import os
from dotenv import load_dotenv

load_dotenv()

DEBUG_MODE = os.getenv("DEBUG_MODE", "False").lower() in ("true", "1", "t")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")
SAMPLE_AUDIO_BASE_PATH = os.getenv("SAMPLE_AUDIO_BASE_PATH", "services/ai-service/src/main/resources")
PROJECT_ROOT_PATH = os.getenv("PROJECT_ROOT_PATH", "")


def get_project_root() -> str:
    if PROJECT_ROOT_PATH:
        return PROJECT_ROOT_PATH

    current_dir = os.path.dirname(os.path.abspath(__file__))
    while current_dir != os.path.dirname(current_dir):
        if any(os.path.exists(os.path.join(current_dir, marker))
               for marker in ["services", "pom.xml", ".git", "package.json", "README.md"]):
            return current_dir
        current_dir = os.path.dirname(current_dir)

    return os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def get_sample_audio_path(audio_type: str, reference_text: str) -> str:
    return os.path.join(get_project_root(), SAMPLE_AUDIO_BASE_PATH, audio_type, f"{reference_text}.mp3")
