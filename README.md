# SerializationDumper
A tool to dump Java serialization streams in a more human readable form.

The tool does not deserialize the stream (i.e. objects in the stream are not instantiated), so it does not require access to the classes that were used in the stream*.

This tool was developed to support research into Java deserialization vulnerabilities after spending many hours manually decoding raw serialization streams to debug code!

Download v1.01 built and ready to run from here: [https://github.com/NickstaDB/SerializationDumper/releases/download/v1.01/SerializationDumper-v1.01.jar](https://github.com/NickstaDB/SerializationDumper/releases/download/v1.01/SerializationDumper-v1.01.jar "SerializationDumper-v1.01.jar")

\* See the limitations section below for more details.

## Building
Run `build.sh` or `build.bat` to compile the JAR from the latest sources.

## Usage
SerializationDumper can take input in the form of hex-ascii encoded bytes on the command line, hex-ascii encoded bytes in a file, or a file containing raw serialized data. The following examples demonstrate its use:

    $ java -jar SerializationDumper-v1.0.jar ACED000574000441424344707100fe0000
    STREAM_MAGIC - 0xac ed
    STREAM_VERSION - 0x00 05
    Contents
      TC_STRING - 0x74
        newHandle 0x00 7e 00 00
        Length - 4 - 0x00 04
        Value - ABCD - 0x41424344
      TC_NULL - 0x70
      TC_REFERENCE - 0x71
        Handle - 16646144 - 0x00 fe 00 00
    
    $ java -jar SerializationDumper-v1.0.jar -f hex-ascii-input-file.txt
    STREAM_MAGIC - 0xac ed
    STREAM_VERSION - 0x00 05
    Contents
      TC_NULL - 0x70
    
    $ java -jar SerializationDumper-v1.0.jar -r raw-input-file.bin
    STREAM_MAGIC - 0xac ed
    STREAM_VERSION - 0x00 05
    Contents
      TC_CLASSDESC - 0x72
        className
          Length - 11 - 0x00 0b
          Value - com.foo.Bar - 0x636f6d2e666f6f2e426172
        serialVersionUID - 0x01 02 03 04 05 06 07 08
        newHandle 0x00 7e 00 00
        classDescFlags - 0x02 - SC_SERIALIZABLE
        fieldCount - 0 - 0x00 00
        classAnnotations
          TC_ENDBLOCKDATA - 0x78
        superClassDesc
          TC_NULL - 0x70

## Limitations
The tool currently doesn't support the full serialization specification. If you have something it can't dump please get in touch with some sample data, unless the stream contains an externalContents element.

'externalContents': If a class implements the interface java.io.Externalizable then it can use the *writeExternal* method  to write custom data to the serialization stream. This data can only be parsed by the corresponding *readExternal* method and it is not possible to read the data without access to the original class. Such classes will have the SC\_EXTERNALIZABLE flag set in the classDescFlags field but they will not have the SC\_BLOCKDATA flag set.

## Bug Reports/Improvements
This tool was hacked together on the fly to support my own research but if you find the tool useful and have any bug reports or suggestions please get in touch either here or on Twitter ([@NickstaDB](http://twitter.com/NickstaDB "@NickstaDB on Twitter")).
