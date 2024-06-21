# SerializationDumper
A tool to dump and rebuild Java serialization streams and Java RMI packet contents in a more human readable form.

The tool does not deserialize the stream (i.e. objects in the stream are not instantiated), so it does not require access to the classes that were used in the stream*.

This tool was developed to support research into Java deserialization vulnerabilities after spending many hours manually decoding raw serialization streams to debug code!

Download v1.11 built and ready to run from here: [https://github.com/NickstaDB/SerializationDumper/releases/download/1.13/SerializationDumper-v1.13.jar](https://github.com/NickstaDB/SerializationDumper/releases/download/1.13/SerializationDumper-v1.13.jar "SerializationDumper-v1.13.jar")

\* See the limitations section below for more details.

**Update 21/06/2024:** Fixed bugs in `readFloatField()` and `readDoubleField()`.

**Update 19/12/2018:** SerializationDumper now supports rebuilding serialization streams so you can dump a Java serialization stream to a text file, modify the hex or string values, then convert the text file back into a binary serialization stream. See the section below on [Rebuilding Serialization Streams](#rebuilding-serialization-streams) for an example of this.

## Building
Run `build.sh` or `build.bat` to compile the JAR from the latest sources.

## Usage
SerializationDumper can take input in the form of hex-ascii encoded bytes on the command line, hex-ascii encoded bytes in a file, or a file containing raw serialized data. The following examples demonstrate its use:

    $ java -jar SerializationDumper-v1.1.jar aced0005740004414243447071007e0000
    STREAM_MAGIC - 0xac ed
    STREAM_VERSION - 0x00 05
    Contents
      TC_STRING - 0x74
        newHandle 0x00 7e 00 00
        Length - 4 - 0x00 04
        Value - ABCD - 0x41424344
      TC_NULL - 0x70
      TC_REFERENCE - 0x71
        Handle - 8257536 - 0x00 7e 00 00
    
    $ java -jar SerializationDumper-v1.1.jar -f hex-ascii-input-file.txt
    STREAM_MAGIC - 0xac ed
    STREAM_VERSION - 0x00 05
    Contents
      TC_NULL - 0x70
    
    $ java -jar SerializationDumper-v1.1.jar -r raw-input-file.bin
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

## Rebuilding Serialization Streams
As of 19/12/2018, SerializationDumper can do the reverse operation and convert a dumped serialization stream back into a raw byte stream. This can be useful for working with raw serialized streams because modifications can be made to the dumped text and be "recompiled" back into a byte stream.

### Example Usage
To demonstrate the use of the stream rebuilding functionality, let's start with a simple serialization stream.

    aced0005740004414243447071007e0000

We can dump this using SerializationDumper, as shown above, to get the following:

    STREAM_MAGIC - 0xac ed
    STREAM_VERSION - 0x00 05
    Contents
      TC_STRING - 0x74
        newHandle 0x00 7e 00 00
        Length - 4 - 0x00 04
        Value - ABCD - 0x41424344
      TC_NULL - 0x70
      TC_REFERENCE - 0x71
        Handle - 8257536 - 0x00 7e 00 00

To modify the string value from `ABCD` to `AAAABBBB` we must update the hex-ascii values for both the string length and the string value as follows:

    STREAM_MAGIC - 0xac ed
    STREAM_VERSION - 0x00 05
    Contents
      TC_STRING - 0x74
        newHandle 0x00 7e 00 00
        Length - 4 - 0x00 08
        Value - ABCD - 0x4141414142424242
      TC_NULL - 0x70
      TC_REFERENCE - 0x71
        Handle - 8257536 - 0x00 7e 00 00

If we save this to the file `dumped.txt` then we can rebuild the stream as follows:

    $ java -jar SerializationDumper-v1.1.jar -b dumped.txt rebuilt.bin

The file `rebuilt.bin` will now contain the raw bytes of the modified serialization stream. If we encode that file as hex-ascii we get the following:

    aced000574000841414141424242427071007e0000

See the limitations section below for stream rebuilding limitations.

## Limitations

### Deserialization/Dump Mode
The tool cannot deserialize all Java serialized data streams and may not be fully compliant with the Java serialization specification. In particular, if the stream contains an *externalContents* element written with serialization protocol version 1 then it cannot be deserialized without using the original class. If you have something that cannot be dumped which does not include an `externalContents` element then please get in touch with some sample data so I can look at producing a fix!

***externalContents:*** If a class implements the interface `java.io.Externalizable` then it can use the `writeExternal` method to write custom data to the serialization stream. This data can only be parsed by the corresponding `readExternal` method so it is often not possible to fully interpret the binary data data without access to the original class. Such classes will have the `SC_EXTERNALIZABLE` flag set in the `classDescFlags` field. For serialization protocol version 1 they will not have the `SC_BLOCK_DATA` flag set and this tool cannot parse the data at all. However, version 1 is only used by old JDK versions (JDK 1.1 and older), or when explicitly enabled through `java.io.ObjectOutputStream.useProtocolVersion(int)`. Therefore in most cases this tool can read the external data (or at least display the hex representation of the binary data).

### Serialization/Rebuild Mode
The stream rebuild mode currently only operates on the hex-ascii encoded bytes from the dumped data. For that reason, changing the string "ABCD" to "AAAABBBB" won't have the desired effect of producing the bytes 0x4141414142424242 in the output file. A future update may improve this but for now you'll have to do your hex-ascii encoding of strings manually!

Length fields aren't updated automatically during stream rebuilding. This may be desirable or not, but if you modify a string value in a way that changes the length just be aware that you may also need to modify the length (hex-ascii) field accordingly. The same applies to arrays.

If the stream contains any `TC_REFERENCE` elements and you modify it to remove an element that includes a `newHandle` field then you may break the references in the stream unless you manually update them. Reference handles/IDs are incremental and start at `0x7e0000` so the first `newHandle` field is reference by `0x7e0000`, the second by `0x7e0001`, and so on. If the first element with a `newHandle` field is removed from the stream then any `TC_REFERENCE` elements in the stream must be modified to refer to a handle value one less than what they originally referred to.

## Bug Reports/Improvements
This tool was hacked together on the fly to support my own research but if you find the tool useful and have any bug reports or suggestions please get in touch either here or on Twitter ([@NickstaDB](https://twitter.com/NickstaDB "@NickstaDB on Twitter")).

**Please** include a sample of the data you were trying to dump when submitting bug reports, this makes it far easier for me to debug and work out what the problem is, cheers!
