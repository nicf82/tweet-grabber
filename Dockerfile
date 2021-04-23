FROM hseeberger/scala-sbt:graalvm-ce-21.0.0-java11_1.4.7_2.13.4

ADD src /root/src
ADD project /root/project
ADD build.sbt /root/

ENV T_API_KEY ""
ENV T_API_SECRET ""
ENV T_TOKEN ""
ENV T_TOKEN_SECRET ""
ENV APP_NAME "tweet-grabber"
ENV ROOT_LOG_LEVEL "INFO"
ENV APP_LOG_LEVEL "INFO"
ENV LOGBACK_CONF "logback-prod.xml"
ENV MQTT_URI "tcp://localhost:1883"
ENV LOGSTASH_DESTINATION "localhost:4560"

RUN cd /root
RUN sbt "set test in assembly := {}" compile assembly

#Metrics server
EXPOSE 9416

ENTRYPOINT java -Dlogback.configurationFile=$LOGBACK_CONF \
                -Dmqtt.uri=$MQTT_URI \
                -Dtwitter.apiKey=$T_API_KEY \
                -Dtwitter.apiSecret=$T_API_SECRET \
                -Dtwitter.token=$T_TOKEN \
                -Dtwitter.tokenSecret=$T_TOKEN_SECRET \
                -jar target/scala-2.13/tweet-grabber-assembly-0.1.jar
