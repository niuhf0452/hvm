FROM debian as builder

RUN apt update \
    && apt install -y curl tar \
    && curl -L https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B9/OpenJDK17U-jre_x64_linux_hotspot_17.0.9_9.tar.gz -o openjdk.tar.gz \
    && mkdir /app \
    && tar -xzf openjdk.tar.gz -C /app \
    && mv /app/jdk-17.0.9+9-jre /app/openjdk

FROM linuxserver/ffmpeg

ADD hvm.jar /app/hvm.jar
ADD run.sh /app/run.sh
COPY --from=builder /app/openjdk /app/openjdk

WORKDIR /app
ENTRYPOINT []
CMD ["/app/run.sh"]
EXPOSE 8080
