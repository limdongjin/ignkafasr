# SPBRAIN_VERSION=5.6
# docker build -t spbrain:$SPBRAIN_VERSION . && docker tag spbrain:$SPBRAIN_VERSION gcr.io/limdongjin-kube/spbrain:$SPBRAIN_VERSION && docker push gcr.io/limdongjin-kube/spbrain:$SPBRAIN_VERSION
FROM python:3.8-buster
RUN apt-get update && \
    apt-get install --no-install-suggests --no-install-recommends --yes python3-venv gcc libpython3-dev
COPY requirements.txt /requirements.txt
RUN pip install -r /requirements.txt
COPY . /app
WORKDIR /app

ENTRYPOINT ["python", "main.py"]
