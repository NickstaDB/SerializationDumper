FROM openjdk:13-alpine
COPY . /usr/src/SerializationDumper
WORKDIR /usr/src/SerializationDumper
RUN mkdir out && \
	javac -cp ./src ./src/nb/deser/SerializationDumper.java ./src/nb/deser/support/*.java -d ./out && \
	jar cvfm SerializationDumper.jar MANIFEST.MF -C ./out/ .
#	javac Main.java
#CMD ["java", "Main"]
ENTRYPOINT ["java", "-jar", "./SerializationDumper.jar"]
#mkdir out
#javac -cp ./src ./src/nb/deser/SerializationDumper.java ./src/nb/deser/support/*.java -d ./out
#jar cvfm SerializationDumper.jar MANIFEST.MF -C ./out/ .
