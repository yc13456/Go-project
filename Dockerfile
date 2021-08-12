FROM ccr.ccs.tencentyun.com/gizwits_platform/python_3.7:enterprise-api_20201231

RUN mkdir -p /app /app/log
WORKDIR /app
ADD . /app

ARG VERSION
RUN sed -i s/debug-version/${VERSION}/g /app/pkg/settings.py
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories && \
    apk --update add --no-cache libxml2-dev libxslt-dev libffi-dev gcc musl-dev libgcc && \
    apk add --no-cache openssl-dev curl jpeg-dev zlib-dev freetype-dev lcms2-dev openjpeg-dev tiff-dev tk-dev tcl-dev


RUN pip install -r build/requirements/requirements.txt

COPY build/nginx/nginx.conf /etc/nginx/nginx.conf
COPY build/nginx/server.conf /etc/nginx/sites-enabled/server.conf
RUN rm -f /etc/nginx/sites-enabled/default /etc/nginx/conf.d/default.conf

RUN cp build/supervisor/supervisord.conf /etc/supervisord.conf
RUN cp build/supervisor/supervisord_push.conf /etc/supervisord_push.conf

RUN mkdir -p /data/nginx
RUN mkdir -p /data/supervisor

VOLUME /data/nginx
VOLUME /data/supervisor
EXPOSE 80

RUN mkdir -p /data/nginx
RUN mkdir -p /data/supervisor

ENTRYPOINT ["/app/build/docker-entrypoint.sh"]
CMD ["-n"]