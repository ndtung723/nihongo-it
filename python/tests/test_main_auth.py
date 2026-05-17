"""Tests for main.py — API route auth enforcement."""
import pytest
from unittest.mock import patch
from fastapi.testclient import TestClient


@pytest.fixture
def client():
    with patch.dict("os.environ", {
        "INTERNAL_API_KEY": "test-key-123",
        "INTERNAL_ONLY": "true",
        "OPENAI_API_KEY": "sk-test",
        "DEBUG_MODE": "false",
    }):
        from main import app
        return TestClient(app)


def test_speech_endpoint_rejects_missing_key(client):
    """Endpoint /analyze-speech rejects requests without X-Internal-Key."""
    response = client.post("/analyze-speech", files={"audio": ("test.wav", b"fake", "audio/wav")}, data={"text": "test"})
    assert response.status_code == 403


def test_speech_endpoint_rejects_wrong_key(client):
    """Endpoint /analyze-speech rejects requests with wrong X-Internal-Key."""
    response = client.post(
        "/analyze-speech",
        files={"audio": ("test.wav", b"fake", "audio/wav")},
        data={"text": "test"},
        headers={"X-Internal-Key": "wrong-key"},
    )
    assert response.status_code == 403


def test_health_endpoint_accessible(client):
    """Health/root endpoint is accessible without auth."""
    response = client.get("/")
    # Accepts 200 or 404 — just not 403
    assert response.status_code != 403
