FROM python:3.10-slim

WORKDIR /app

# Cài đặt curl, ffmpeg và các dependencies khác
RUN apt-get update &&     apt-get install -y curl ffmpeg libsndfile1 gcc g++ &&     apt-get clean &&     rm -rf /var/lib/apt/lists/*

# Cài đặt các dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy source code
COPY . .

# Biến môi trường
ENV PORT=8000
ENV DEBUG_MODE=False

# Expose port
EXPOSE 8000

# Simple health check (không sử dụng HEALTHCHECK vì Cloud Run không hỗ trợ)
# Thay vào đó, sử dụng endpoint "/" để kiểm tra

# Chạy ứng dụng
CMD ["python", "main.py"]
