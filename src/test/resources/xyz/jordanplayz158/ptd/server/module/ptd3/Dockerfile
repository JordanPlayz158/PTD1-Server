FROM python:3-alpine

WORKDIR /usr/src/app

COPY python/requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY python/ .

EXPOSE 8080

CMD [ "gunicorn", "--bind", "0.0.0.0:8080", "src.main:app" ]