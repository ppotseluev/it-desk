FROM sbtscala/scala-sbt:eclipse-temurin-17.0.2_1.6.2_2.13.8 AS builder

COPY . /it-desk
WORKDIR /it-desk
RUN sbt "project api" assembly

FROM amazoncorretto:17.0.5

ENV MYSQL_PWD UNDEFINED
ENV TELEGRAM_CHAT_ID UNDEFINED
ENV GREETING_BOT_TOKEN UNDEFINED
ENV GREETING_BOT_SECRET UNDEFINED
ENV JAVA_OPTS ""

COPY --from=builder /it-desk/api/target/scala-2.13/api-assembly-1.0-SNAPSHOT.jar ./
EXPOSE 8080
CMD java $JAVA_OPTS -jar api-assembly-1.0-SNAPSHOT.jar
