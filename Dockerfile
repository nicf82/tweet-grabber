FROM hseeberger/scala-sbt:graalvm-ce-21.0.0-java11_1.4.7_2.13.4

ADD src /root/src
ADD project /root/project
ADD build.sbt /root/

ENV LOGBACK_CONF "logback-prod.xml"
ENV MONGO_URL "mongodb://localhost:27017"
ENV MONGO_DB "racedata"
ENV MQTT_URI "tcp://localhost:1883"

RUN cd /root
RUN sbt compile assembly

#Metrics server
EXPOSE 9416

ENTRYPOINT java -Dlogback.configurationFile=$LOGBACK_CONF \
                -Dmqtt.uri=$MQTT_URI \
                -Ddatabase.url=$MONGO_URL \
                -Ddatabase.name=$MONGO_DB \
                -jar target/scala-2.13/tweet-grabber-assembly-0.1.jar