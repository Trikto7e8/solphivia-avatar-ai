FROM python:3.13-slim
WORKDIR /service
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt && useradd --uid 10001 --create-home demo && mkdir /data && chown demo /data
COPY server.py .
USER demo
EXPOSE 8080
CMD ["uvicorn", "server:create_app", "--factory", "--host", "0.0.0.0", "--port", "8080", "--workers", "2", "--no-access-log", "--lifespan", "off", "--limit-concurrency", "32"]
