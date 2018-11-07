FROM maven:3.5.4-jdk-8 as builder
WORKDIR /build/
COPY src src
COPY pom.xml pom.xml
RUN mvn package

FROM openjdk:8-jdk
WORKDIR /novabot/data/
COPY --from=builder /build/target/novabot-1.2-jar-with-dependencies.jar /novabot/novabot.jar
COPY config.example.ini config.ini
COPY formatting.ini formatting.ini
COPY geofences.txt geofences.txt
COPY gkeys.txt gkeys.txt
COPY globalfilter.json globalfilter.json
COPY pokechannels.ini pokechannels.ini
COPY pokefilter.json pokefilter.json
COPY raidchannels.ini raidchannels.ini
COPY raidfilter.json raidfilter.json
COPY suburbs.txt suburbs.txt
RUN echo > supporterlevels.txt

ENTRYPOINT ["java", "-jar", "/novabot/novabot.jar"]
