mkdir out
javac -cp ./src ./src/nb/deser/SerializationDumper.java ./src/nb/deser/support/*.java -d ./out
jar cvfm SerializationDumper.jar MANIFEST.MF -C ./out/ .
