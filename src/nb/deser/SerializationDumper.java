package nb.deser;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import nb.deser.support.ClassDataDesc;
import nb.deser.support.ClassDetails;
import nb.deser.support.ClassField;

/***********************************************************
 * Helper program to dump a hex-ascii encoded serialization
 * stream in a more readable form for debugging purposes.
 * 
 * Written by Nicky Bloor (@NickstaDB).
 **********************************************************/
public class SerializationDumper {
	/*******************
	 * Properties
	 ******************/
	private final LinkedList<Byte> _data;								//The data being parsed
	private String _indent;												//A string representing the current indentation level for output printing
	private int _handleValue;											//The current handle value
	private final ArrayList<ClassDataDesc> _classDataDescriptions;		//Array of all class data descriptions to use with TC_REFERENCE classDesc elements
	private boolean _enablePrinting;									//Flag to control console printing (used when rebuilding to test a rebuilt serialization stream)
	
	/*******************
	 * Converts the command line parameter to an ArrayList of bytes and sends
	 * them for parsing.
	 * 
	 * @param args Command line parameters.
	 * @throws Exception If an exception occurs.
	 ******************/
	public static void main(String[] args) throws Exception {
		File f;
		FileInputStream fis;
		SerializationDumper sd = new SerializationDumper();
		byte[] fileContents;
		
		//Validate number of command line args
		if(args.length == 0 || args.length > 3) {
			System.out.println("Usage:");
			System.out.println("\tSerializationDumper <hex-ascii-data>");
			System.out.println("\tSerializationDumper -f <file-containing-hex-ascii>");
			System.out.println("\tSerializationDumper -r <file-containing-raw-data>");
			System.out.println("");
			System.out.println("Rebuild a dumped stream:");
			System.out.println("\tSerializationDumper -b <input-file> <output-file>");
			return;
		}
		
		//A single argument must be a hex-ascii encoded byte string
		if(args.length == 1) {
			//Validation
			if(args[0].length() % 2 == 1) {
				System.out.println("Error: Data encoded as hex and passed on the command line must have a length that is a multiple of 2.");
				return;
			}
			
			//Load the data into the serialization dumper
			for(int i = 0; i < args[0].length() / 2; ++i) {
				//Validation
				if(Character.digit(args[0].charAt(i * 2), 16) == -1 || Character.digit(args[0].charAt(i * 2 + 1), 16) == -1) {
					System.out.println("Error: Data encoded as hex and passed on the command line must only contain hex digits.");
					return;
				}
				
				sd._data.add((byte)(
					(Character.digit(args[0].charAt(i * 2), 16) << 4) +
					(Character.digit(args[0].charAt(i * 2 + 1), 16))
				));
			}
		} else if(args.length == 3 && args[0].toLowerCase().equals("-b")) {
			//Three arguments starting with -r means we're rebuilding a stream from a dump
			sd.rebuildStream(args[1], args[2]);
			return;
		} else {
			//Two arguments means the data is in a file, read the file contents
			f = new File(args[1]);
			fis = new FileInputStream(f);
			fileContents = new byte[(int)f.length()];
			fis.read(fileContents);
			fis.close();
			
			//Load the file contents into the serialization dumper based on the expected format
			if(args[0].toLowerCase().equals("-f")) {
				//Hex-ascii encoded file contents
				for(int i = 0; i < fileContents.length / 2; ++i) {
					sd._data.add((byte)(
						(Character.digit(fileContents[i * 2], 16) << 4) +
						(Character.digit(fileContents[i * 2 + 1], 16))
					));
				}
			} else if(args[0].toLowerCase().equals("-r")) {
				//Raw bytes
				for(int i = 0; i < fileContents.length; ++i) {
					sd._data.add(fileContents[i]);
				}
			} else if(args[0].toLowerCase().equals("-b")) {
				System.out.println("Error: The -b option requires two parameters, the input file and the output file.");
				return;
			}
		}
		
		//Parse and dump the serialization stream
		System.out.println("");
		sd.parseStream();
	}
	
	/*******************
	 * Construct a SerializationDumper object.
	 ******************/
	public SerializationDumper() {
		this._data = new LinkedList<Byte>();
		this._indent = "";
		this._handleValue = 0x7e0000;
		this._classDataDescriptions = new ArrayList<ClassDataDesc>();
		this._enablePrinting = true;
	}
	
	/*******************
	 * Print the given string using the current indentation.
	 * 
	 * @param s The string to print out.
	 ******************/
	private void print(String s) {
		if(this._enablePrinting == true) {
			System.out.println(this._indent + s);
		}
	}
	
	/*******************
	 * Increase the indentation string.
	 ******************/
	private void increaseIndent() {
		this._indent = this._indent + "  ";
	}
	
	/*******************
	 * Decrease the indentation string or trigger an exception if the length is
	 * already below 2.
	 ******************/
	private void decreaseIndent() {
		if(this._indent.length() < 2) { throw new RuntimeException("Error: Illegal indentation decrease."); }
		this._indent = this._indent.substring(0, this._indent.length() - 2);
	}
	
	/*******************
	 * Convert a single byte to a hex-ascii string.
	 * 
	 * @param b The byte to convert to a string.
	 * @return The hex-ascii string representation of the byte.
	 ******************/
	private String byteToHex(byte b) {
		return String.format("%02x", b);
	}
	
	/*******************
	 * Convert a hex-ascii string to a byte array.
	 * 
	 * @param h The hex-ascii string to convert to bytes.
	 * @return A byte array containing the values from the hex-ascii string.
	 ******************/
	private byte[] hexStrToBytes(String h) {
		byte[] outBytes = new byte[h.length() / 2];
		for(int i = 0; i < outBytes.length; ++i) {
			outBytes[i] = (byte)((Character.digit(h.charAt(i * 2), 16) << 4) + Character.digit(h.charAt((i * 2) + 1), 16));
		}
		return outBytes;
	}
	
	/*******************
	 * Convert an int to a hex-ascii string.
	 * 
	 * @param i The int to convert to a string.
	 * @return The hex-ascii string representation of the int.
	 ******************/
	private String intToHex(int i) {
		return String.format("%02x", (i & 0xff000000) >> 24) +
			   String.format(" %02x", (i &   0xff0000) >> 16) +
			   String.format(" %02x", (i &     0xff00) >>  8) +
			   String.format(" %02x", (i &       0xff));
	}
	
	/*******************
	 * Print a handle value and increment the handle value for the next handle.
	 ******************/
	private int newHandle() {
		int handleValue = this._handleValue;
		
		//Print the handle value
		this.print("newHandle 0x" + this.intToHex(handleValue));
		
		//Increment the next handle value and return the one we just assigned
		this._handleValue++;
		return handleValue;
	}
	
	/*******************
	 * Parse output from parseStream() and turn it back into a binary
	 * serialization stream.
	 ******************/
	private void rebuildStream(String dumpFile, String outputFile) {
		String inputLine;
		BufferedReader reader;
		ByteArrayOutputStream byteStream;
		FileOutputStream outputFileStream;
		byte[] rebuiltStream;
		
		//Parse the input file (a serialization stream dumped by this program)
		System.out.println("Rebuilding serialization stream from dump: " + dumpFile);
		try {
			byteStream = new ByteArrayOutputStream();
			reader = new BufferedReader(new FileReader(dumpFile));
			while((inputLine = reader.readLine()) != null) {
				if(inputLine.trim().startsWith("newHandle ") == false) {
					if(inputLine.contains("0x")) {
						if(inputLine.trim().startsWith("Value - ")) {
							inputLine = inputLine.substring(inputLine.lastIndexOf("0x") + 2);
						} else {
							inputLine = inputLine.split("0x", 2)[1];
						}
						if(inputLine.contains(" - ")) {
							inputLine = inputLine.split("-", 2)[0];
						}
						inputLine = inputLine.replace(" ", "");
						byteStream.write(hexStrToBytes(inputLine));
					}
				}
			}
			reader.close();
		} catch(FileNotFoundException fnfe) {
			System.out.println("Error: input file not found (" + dumpFile + ").");
			System.out.println(fnfe.getMessage());
			return;
		} catch(IOException ioe) {
			System.out.println("Error: an exception occurred whilst reading the input file (" + dumpFile + ").");
			System.out.println(ioe.getMessage());
			return;
		}
		
		//Test the rebuilt serialization stream
		System.out.println("Stream rebuilt, attempting to parse...");
		this._enablePrinting = false;
		rebuiltStream = byteStream.toByteArray();
		for(byte b: rebuiltStream) {
			this._data.add(b);
		}
		try {
			this.parseStream();
		} catch(Exception e) {
			System.out.println("Warning: An exception was thrown whilst attempting to parse the rebuilt stream.");
			System.out.println(e.getMessage());
		}
		
		//Save the stream to disk
		try {
			outputFileStream = new FileOutputStream(outputFile);
			outputFileStream.write(rebuiltStream);
			outputFileStream.close();
		} catch(FileNotFoundException fnfe) {
			System.out.println("Error: exception opening output file (" + outputFile + ").");
			System.out.println(fnfe.getMessage());
			return;
		} catch(IOException ioe) {
			System.out.println("Error: an exception occurred whilst writing the output file (" + outputFile + ").");
			System.out.println(ioe.getMessage());
			return;
		}
		System.out.println("Done, rebuilt stream written to " + outputFile);
	}
	
	/*******************
	 * Parse the given serialization stream and dump the details out as text.
	 ******************/
	private void parseStream() throws Exception {
		byte b1, b2;
		
		//The stream may begin with an RMI packet type byte, print it if so
		if(this._data.peek() != (byte)0xac) {
			b1 = this._data.pop();
			switch(b1) {
				case (byte)0x50:
					this.print("RMI Call - 0x50");
					break;
				case (byte)0x51:
					this.print("RMI ReturnData - 0x51");
					break;
				case (byte)0x52:
					this.print("RMI Ping - 0x52");
					break;
				case (byte)0x53:
					this.print("RMI PingAck - 0x53");
					break;
				case (byte)0x54:
					this.print("RMI DgcAck - 0x54");
					break;
				default:
					this.print("Unknown RMI packet type - 0x" + this.byteToHex(b1));
					break;
			}
		}
		
		//Magic number, print and validate
		b1 = this._data.pop();
		b2 = this._data.pop();
		this.print("STREAM_MAGIC - 0x" + this.byteToHex(b1) + " " + this.byteToHex(b2));
		if(b1 != (byte)0xac || b2 != (byte)0xed) {
			this.print("Invalid STREAM_MAGIC, should be 0xac ed");
			return;
		}
		
		//Serialization version
		b1 = this._data.pop();
		b2 = this._data.pop();
		this.print("STREAM_VERSION - 0x" + this.byteToHex(b1) + " " + this.byteToHex(b2));
		if(b1 != (byte)0x00 || b2 != (byte)0x05) {
			this.print("Invalid STREAM_VERSION, should be 0x00 05");
		}
		
		//Remainder of the stream consists of one or more 'content' elements
		this.print("Contents");
		this.increaseIndent();
		while(this._data.size() > 0) {
			this.readContentElement();
		}
		this.decreaseIndent();
	}
	
	/*******************
	 * Read a content element from the data stream.
	 * 
	 * Could be any of:
	 *	TC_OBJECT			(0x73)
	 *	TC_CLASS			(0x76)
	 *	TC_ARRAY			(0x75)
	 *	TC_STRING			(0x74)
	 *	TC_LONGSTRING		(0x7c)
	 *	TC_ENUM				(0x7e)
	 *	TC_CLASSDESC		(0x72)
	 *	TC_PROXYCLASSDESC	(0x7d)
	 *	TC_REFERENCE		(0x71)
	 *	TC_NULL				(0x70)
	 *	TC_EXCEPTION		(0x7b)
	 *	TC_RESET			(0x79)
	 *	TC_BLOCKDATA		(0x77)
	 *	TC_BLOCKDATALONG	(0x7a)
	 ******************/
	private void readContentElement() {
		//Peek the next byte and delegate to the appropriate method
		switch(this._data.peek()) {
			case (byte)0x73:		//TC_OBJECT
				this.readNewObject();
				break;
				
			case (byte)0x76:		//TC_CLASS
				this.readNewClass();
				break;
				
			case (byte)0x75:		//TC_ARRAY
				this.readNewArray();
				break;
				
			case (byte)0x74:		//TC_STRING
			case (byte)0x7c:		//TC_LONGSTRING
				this.readNewString();
				break;
				
			case (byte)0x7e:		//TC_ENUM
				this.readNewEnum();
				break;
				
			case (byte)0x72:		//TC_CLASSDESC
			case (byte)0x7d:		//TC_PROXYCLASSDESC
				this.readNewClassDesc();
				break;
				
			case (byte)0x71:		//TC_REFERENCE
				this.readPrevObject();
				break;
				
			case (byte)0x70:		//TC_NULL
				this.readNullReference();
				break;
				
//			case (byte)0x7b:		//TC_EXCEPTION
//				this.readException();
//				break;
				
//			case (byte)0x79:		//TC_RESET
//				this.handleReset();
//				break;
				
			case (byte)0x77:		//TC_BLOCKDATA
				this.readBlockData();
				break;
				
			case (byte)0x7a:		//TC_BLOCKDATALONG
				this.readLongBlockData();
				break;
				
			default:
				this.print("Invalid content element type 0x" + this.byteToHex(this._data.peek()));
				throw new RuntimeException("Error: Illegal content element type.");
		}
	}
	
	/*******************
	 * Read an enum element from the data stream.
	 * 
	 * TC_ENUM		classDesc	newHandle	enumConstantName
	 ******************/
	private void readNewEnum() {
		byte b1;
		
		//TC_ENUM
		b1 = this._data.pop();
		this.print("TC_ENUM - 0x" + this.byteToHex(b1));
		if(b1 != (byte)0x7e) { throw new RuntimeException("Error: Illegal value for TC_ENUM (should be 0x7e)"); }
		
		//Indent
		this.increaseIndent();
		
		//classDesc
		this.readClassDesc();
		
		//newHandle
		this.newHandle();
		
		//enumConstantName
		this.readNewString();
		
		//Decrease indent
		this.decreaseIndent();
	}
	
	/*******************
	 * Read an object element from the data stream.
	 * 
	 * TC_OBJECT	classDesc	newHandle	classdata[]
	 ******************/
	private void readNewObject() {
		ClassDataDesc cdd;	//ClassDataDesc describing the format of the objects 'classdata' element
		byte b1;
		
		//TC_OBJECT
		b1 = this._data.pop();
		this.print("TC_OBJECT - 0x" + this.byteToHex(b1));
		if(b1 != (byte)0x73) { throw new RuntimeException("Error: Illegal value for TC_OBJECT (should be 0x73)"); }
		
		//Indent
		this.increaseIndent();
		
		//classDesc
		cdd = this.readClassDesc();	//Read the class data description
		
		//newHandle
		this.newHandle();
		
		//classdata
		this.readClassData(cdd);	//Read the class data based on the class data description - TODO This needs to check if cdd is null before reading anything
		
		//Decrease indent
		this.decreaseIndent();
	}
	
	/*******************
	 * Read a classDesc from the data stream.
	 * 
	 * Could be:
	 *	TC_CLASSDESC		(0x72)
	 *	TC_PROXYCLASSDESC	(0x7d)
	 *	TC_NULL				(0x70)
	 *	TC_REFERENCE		(0x71)
	 ******************/
	private ClassDataDesc readClassDesc() {
		int refHandle;
		
		//Peek the type and delegate to the appropriate method
		switch(this._data.peek()) {
			case (byte)0x72:		//TC_CLASSDESC
			case (byte)0x7d:		//TC_PROXYCLASSDESC
				return this.readNewClassDesc();
				
			case (byte)0x70:		//TC_NULL
				this.readNullReference();
				return null;
				
			case (byte)0x71:		//TC_REFERENCE
				refHandle = this.readPrevObject();		//Look up a referenced class data description object and return it
				for(ClassDataDesc cdd: this._classDataDescriptions) {	//Iterate over all class data descriptions
					for(int classIndex = 0; classIndex < cdd.getClassCount(); ++classIndex) {	//Iterate over all classes in this class data description
						if(cdd.getClassDetails(classIndex).getHandle() == refHandle) {	//Check if the reference handle matches
							return cdd.buildClassDataDescFromIndex(classIndex);			//Generate a ClassDataDesc starting from the given index and return it
						}
					}
				}
				
				//Invalid classDesc reference handle
				throw new RuntimeException("Error: Invalid classDesc reference (0x" + this.intToHex(refHandle) + ")");
				
			default:
				this.print("Invalid classDesc type 0x" + this.byteToHex(this._data.peek()));
				throw new RuntimeException("Error illegal classDesc type.");
		}
	}
	
	/*******************
	 * Read a newClassDesc from the stream.
	 * 
	 * Could be:
	 *	TC_CLASSDESC		(0x72)
	 *	TC_PROXYCLASSDESC	(0x7d)
	 ******************/
	private ClassDataDesc readNewClassDesc() {
		ClassDataDesc cdd;
		
		//Peek the type and delegate to the appropriate method
		switch(this._data.peek()) {
			case (byte)0x72:		//TC_CLASSDESC
				cdd = this.readTC_CLASSDESC();
				this._classDataDescriptions.add(cdd);
				return cdd;
				
			case (byte)0x7d:		//TC_PROXYCLASSDESC
				cdd = this.readTC_PROXYCLASSDESC();
				this._classDataDescriptions.add(cdd);
				return cdd;
				
			default:
				this.print("Invalid newClassDesc type 0x" + this.byteToHex(this._data.peek()));
				throw new RuntimeException("Error illegal newClassDesc type.");
		}
	}
	
	/*******************
	 * Read a TC_CLASSDESC from the stream.
	 * 
	 * TC_CLASSDESC		className	serialVersionUID	newHandle	classDescInfo
	 ******************/
	private ClassDataDesc readTC_CLASSDESC() {
		ClassDataDesc cdd = new ClassDataDesc();
		byte b1;
		
		//TC_CLASSDESC
		b1 = this._data.pop();
		this.print("TC_CLASSDESC - 0x" + this.byteToHex(b1));
		if(b1 != (byte)0x72) { throw new RuntimeException("Error: Illegal value for TC_CLASSDESC (should be 0x72)"); }
		this.increaseIndent();
		
		//className
		this.print("className");
		this.increaseIndent();
		cdd.addClass(this.readUtf());		//Add the class name to the class data description
		this.decreaseIndent();
		
		//serialVersionUID
		this.print("serialVersionUID - 0x" + this.byteToHex(this._data.pop()) + " " + this.byteToHex(this._data.pop()) + " " + this.byteToHex(this._data.pop()) + " " + this.byteToHex(this._data.pop()) +
				   " " + this.byteToHex(this._data.pop()) + " " + this.byteToHex(this._data.pop()) + " " + this.byteToHex(this._data.pop()) + " " + this.byteToHex(this._data.pop()));
		
		//newHandle
		cdd.setLastClassHandle(this.newHandle());	//Set the reference handle for the most recently added class
		
		//classDescInfo
		this.readClassDescInfo(cdd);	//Read class desc info, add the super class description to the ClassDataDesc if one is found
		
		//Decrease the indent
		this.decreaseIndent();
		
		//Return the ClassDataDesc
		return cdd;
	}
	
	/*******************
	 * Read a TC_PROXYCLASSDESC from the stream.
	 * 
	 * TC_PROXYCLASSDESC	newHandle	proxyClassDescInfo
	 ******************/
	private ClassDataDesc readTC_PROXYCLASSDESC() {
		ClassDataDesc cdd = new ClassDataDesc();
		byte b1;
		
		//TC_PROXYCLASSDESC
		b1 = this._data.pop();
		this.print("TC_PROXYCLASSDESC - 0x" + this.byteToHex(b1));
		if(b1 != (byte)0x7d) { throw new RuntimeException("Error: Illegal value for TC_PROXYCLASSDESC (should be 0x7d)"); }
		this.increaseIndent();
		
		//Create the new class descriptor
		cdd.addClass("<Dynamic Proxy Class>");
		
		//newHandle
		cdd.setLastClassHandle(this.newHandle());	//Set the reference handle for the most recently added class
		
		//proxyClassDescInfo
		this.readProxyClassDescInfo(cdd);	//Read proxy class desc info, add the super class description to the ClassDataDesc if one is found
		
		//Decrease the indent
		this.decreaseIndent();
		
		//Return the ClassDataDesc
		return cdd;
	}
	
	/*******************
	 * Read a classDescInfo from the stream.
	 * 
	 * classDescFlags	fields	classAnnotation	superClassDesc
	 ******************/
	private void readClassDescInfo(ClassDataDesc cdd) {
		String classDescFlags = "";
		byte b1;
		
		//classDescFlags
		b1 = this._data.pop();
		if((b1 & 0x01) == 0x01) { classDescFlags += "SC_WRITE_METHOD | "; }
		if((b1 & 0x02) == 0x02) { classDescFlags += "SC_SERIALIZABLE | "; }
		if((b1 & 0x04) == 0x04) { classDescFlags += "SC_EXTERNALIZABLE | "; }
		if((b1 & 0x08) == 0x08) { classDescFlags += "SC_BLOCK_DATA | "; }
		if(classDescFlags.length() > 0) { classDescFlags = classDescFlags.substring(0, classDescFlags.length() - 3); }
		this.print("classDescFlags - 0x" + this.byteToHex(b1) + " - " + classDescFlags);
		
		//Store the classDescFlags
		cdd.setLastClassDescFlags(b1);		//Set the classDescFlags for the most recently added class
		
		//Validate classDescFlags
		if((b1 & 0x02) == 0x02) {
			if((b1 & 0x04) == 0x04) { throw new RuntimeException("Error: Illegal classDescFlags, SC_SERIALIZABLE is not compatible with SC_EXTERNALIZABLE."); }
			if((b1 & 0x08) == 0x08) { throw new RuntimeException("Error: Illegal classDescFlags, SC_SERIALIZABLE is not compatible with SC_BLOCK_DATA."); }
		} else if((b1 & 0x04) == 0x04) {
			if((b1 & 0x01) == 0x01) { throw new RuntimeException("Error: Illegal classDescFlags, SC_EXTERNALIZABLE is not compatible with SC_WRITE_METHOD."); }
		} else if(b1 != 0x00) {
			throw new RuntimeException("Error: Illegal classDescFlags, must include either SC_SERIALIZABLE or SC_EXTERNALIZABLE.");
		}
		
		//fields
		this.readFields(cdd);		//Read field descriptions and add them to the ClassDataDesc
		
		//classAnnotation
		this.readClassAnnotation();
		
		//superClassDesc
		cdd.addSuperClassDesc(this.readSuperClassDesc());	//Read the super class description and add it to the ClassDataDesc
	}
	
	/*******************
	 * Read a proxyClassDescInfo from the stream.
	 * 
	 * (int)count	(utf)proxyInterfaceName[count]	classAnnotation		superClassDesc
	 ******************/
	private void readProxyClassDescInfo(ClassDataDesc cdd) {
		byte b1, b2, b3, b4;
		int count;
		
		//count
		b1 = this._data.pop();
		b2 = this._data.pop();
		b3 = this._data.pop();
		b4 = this._data.pop();
		count = (int)(
				((b1 << 24) & 0xff000000) +
				((b2 << 16) &   0xff0000) +
				((b3 <<  8) &     0xff00) +
				((b4      ) &       0xff)
		);
		this.print("Interface count - " + count + " - 0x" + this.byteToHex(b1) + " " + this.byteToHex(b2) + " " + this.byteToHex(b3) + " " + this.byteToHex(b4));
		
		//proxyInterfaceName[count]
		this.print("proxyInterfaceNames");
		this.increaseIndent();
		for(int i = 0; i < count; ++i) {
			this.print(i + ":");
			this.increaseIndent();
			this.readUtf();
			this.decreaseIndent();
		}
		this.decreaseIndent();
		
		//classAnnotation
		this.readClassAnnotation();
		
		//superClassDesc
		cdd.addSuperClassDesc(this.readSuperClassDesc());	//Read the super class description and add it to the ClassDataDesc
	}
	
	/*******************
	 * Read a classAnnotation from the stream.
	 * 
	 * Could be either:
	 *	contents
	 *	TC_ENDBLOCKDATA		(0x78)
	 ******************/
	private void readClassAnnotation() {
		//Print annotation section and indent
		this.print("classAnnotations");
		this.increaseIndent();
		
		//Loop until we have a TC_ENDBLOCKDATA
		while(this._data.peek() != (byte)0x78) {
			//Read a content element
			this.readContentElement();
		}
		
		//Pop and print the TC_ENDBLOCKDATA element
		this._data.pop();
		this.print("TC_ENDBLOCKDATA - 0x78");
		
		//Decrease indent
		this.decreaseIndent();
	}
	
	/*******************
	 * Read a superClassDesc from the stream.
	 * 
	 * classDesc
	 ******************/
	private ClassDataDesc readSuperClassDesc() {
		ClassDataDesc cdd;
		
		//Print header, indent, delegate, decrease indent
		this.print("superClassDesc");
		this.increaseIndent();
		cdd = this.readClassDesc();
		this.decreaseIndent();
		
		//Return the super class data description
		return cdd;
	}
	
	/*******************
	 * Read a fields element from the stream.
	 * 
	 * (short)count		fieldDesc[count]
	 ******************/
	private void readFields(ClassDataDesc cdd) {
		byte b1, b2;
		short count;
		
		//count
		b1 = this._data.pop();
		b2 = this._data.pop();
		count = (short)(
				((b1 << 8) & 0xff00) +
				(b2 & 0xff)
		);
		this.print("fieldCount - " + count + " - 0x" + this.byteToHex(b1) + " " + this.byteToHex(b2));
		
		//fieldDesc
		if(count > 0) {
			this.print("Fields");
			this.increaseIndent();
			for(int i = 0; i < count; ++i) {
				this.print(i + ":");
				this.increaseIndent();
				this.readFieldDesc(cdd);
				this.decreaseIndent();
			}
			this.decreaseIndent();
		}
	}
	
	/*******************
	 * Read a fieldDesc from the stream.
	 * 
	 * Could be either:
	 *	prim_typecode	fieldName
	 *	obj_typecode	fieldName	className1
	 ******************/
	private void readFieldDesc(ClassDataDesc cdd) {
		byte b1;
		
		//prim_typecode/obj_typecode
		b1 = this._data.pop();
		cdd.addFieldToLastClass(b1);		//Add a field of the type in b1 to the most recently added class
		switch((char)b1) {
			case 'B':		//byte
				this.print("Byte - B - 0x" + this.byteToHex(b1));
				break;
				
			case 'C':		//char
				this.print("Char - C - 0x" + this.byteToHex(b1));
				break;
				
			case 'D':		//double
				this.print("Double - D - 0x" + this.byteToHex(b1));
				break;
				
			case 'F':		//float
				this.print("Float - F - 0x" + this.byteToHex(b1));
				break;
				
			case 'I':		//int
				this.print("Int - I - 0x" + this.byteToHex(b1));
				break;
				
			case 'J':		//long
				this.print("Long - L - 0x" + this.byteToHex(b1));
				break;
				
			case 'S':		//Short
				this.print("Short - S - 0x" + this.byteToHex(b1));
				break;
				
			case 'Z':		//boolean
				this.print("Boolean - Z - 0x" + this.byteToHex(b1));
				break;
				
			case '[':		//array
				this.print("Array - [ - 0x" + this.byteToHex(b1));
				break;
				
			case 'L':		//object
				this.print("Object - L - 0x" + this.byteToHex(b1));
				break;
				
			default:
				//Unknown field type code
				throw new RuntimeException("Error: Illegal field type code ('" + (char)b1 + "', 0x" + this.byteToHex(b1) + ")");
		}
		
		//fieldName
		this.print("fieldName");
		this.increaseIndent();
		cdd.setLastFieldName(this.readUtf());		//Set the name of the most recently added field
		this.decreaseIndent();
		
		//className1 (if non-primitive type)
		if((char)b1 == '[' || (char)b1 == 'L') {
			this.print("className1");
			this.increaseIndent();
			cdd.setLastFieldClassName1(this.readNewString());		//Set the className1 of the most recently added field
			this.decreaseIndent();
		}
	}
	
	/*******************
	 * Read classdata from the stream.
	 * 
	 * Consists of data for each class making up the object starting with the
	 * most super class first. The length and type of data depends on the
	 * classDescFlags and field descriptions.
	 ******************/
	private void readClassData(ClassDataDesc cdd) {
		ClassDetails cd;
		int classIndex;
		
		//Print header and indent
		this.print("classdata");
		this.increaseIndent();
		
		//Print class data if there is any
		if(cdd != null) {
			//Iterate backwards through the classes as we need to deal with the most super (last added) class first
			for(classIndex = cdd.getClassCount() - 1; classIndex >= 0; --classIndex) {
				//Get the class details
				cd = cdd.getClassDetails(classIndex);
				
				//Print the class name and indent
				this.print(cd.getClassName());
				this.increaseIndent();
				
				//Read the field values if the class is SC_SERIALIZABLE
				if(cd.isSC_SERIALIZABLE()) {
					//Start the field values section and indent
					this.print("values");
					this.increaseIndent();
					
					//Iterate over the field and read/print the contents
					for(ClassField cf: cd.getFields()) {
						this.readClassDataField(cf);
					}
					
					//Revert indent
					this.decreaseIndent();
				}
				
				boolean hasBlockData = cd.isSC_SERIALIZABLE() && cd.isSC_WRITE_METHOD();
				
				if(cd.isSC_EXTERNALIZABLE()) {
					if(cd.isSC_BLOCK_DATA()) {
						hasBlockData = true;
					}
					//Protocol version 1 does not use block data; cannot parse it
					else {
						this.increaseIndent();
						this.print("Unable to parse externalContents for protocol version 1.");
						throw new RuntimeException("Error: Unable to parse externalContents element.");
					}
				}
				
				//Read object annotations
				if(hasBlockData) {
					//Start the object annotations section and indent
					this.print("objectAnnotation");
					this.increaseIndent();
					
					//Loop until we have a TC_ENDBLOCKDATA
					while(this._data.peek() != (byte)0x78) {
						//Read a content element
						this.readContentElement();
					}
					
					//Pop and print the TC_ENDBLOCKDATA element
					this._data.pop();
					this.print("TC_ENDBLOCKDATA - 0x78");
					
					//Revert indent
					this.decreaseIndent();
				}
				
				//Revert indent for this class
				this.decreaseIndent();
			}
		} else {
			this.print("N/A");
		}
		
		//Revert indent
		this.decreaseIndent();
	}
	
	/*******************
	 * Read a classdata field from the stream.
	 * 
	 * The data type depends on the given field description.
	 * 
	 * @param f A description of the field data to read.
	 ******************/
	private void readClassDataField(ClassField cf) {
		byte b1, b2, b3, b4, b5, b6, b7, b8;
		
		//Print the field name and indent
		this.print(cf.getName());
		this.increaseIndent();
		
		//Read the field data
		this.readFieldValue(cf.getTypeCode());
		
		//Decrease the indent
		this.decreaseIndent();
	}
	
	/*******************
	 * Read a field value based on the type code.
	 * 
	 * @param typeCode The field type code.
	 ******************/
	private void readFieldValue(byte typeCode) {
		switch((char)typeCode) {
			case 'B':		//byte
				this.readByteField();
				break;
				
			case 'C':		//char
				this.readCharField();
				break;
				
			case 'D':		//double
				this.readDoubleField();
				break;
				
			case 'F':		//float
				this.readFloatField();
				break;
				
			case 'I':		//int
				this.readIntField();
				break;
				
			case 'J':		//long
				this.readLongField();
				break;
				
			case 'S':		//short
				this.readShortField();
				break;
				
			case 'Z':		//boolean
				this.readBooleanField();
				break;
				
			case '[':		//array
				this.readArrayField();
				break;
				
			case 'L':		//object
				this.readObjectField();
				break;
				
			default:		//Unknown field type
				throw new RuntimeException("Error: Illegal field type code ('" + typeCode + "', 0x" + this.byteToHex((byte)typeCode) + ")");
		}
	}
	
	/*******************
	 * Read a TC_ARRAY from the stream.
	 * 
	 * TC_ARRAY		classDesc	newHandle	(int)size	values[size]
	 ******************/
	private void readNewArray() {
		ClassDataDesc cdd;
		ClassDetails cd;
		byte b1, b2, b3, b4;
		int size;
		
		//TC_ARRAY
		b1 = this._data.pop();
		this.print("TC_ARRAY - 0x" + this.byteToHex(b1));
		if(b1 != (byte)0x75) { throw new RuntimeException("Error: Illegal value for TC_ARRAY (should be 0x75)"); }
		this.increaseIndent();
		
		//classDesc
		cdd = this.readClassDesc();	//Read the class data description to enable array elements to be read
		if(cdd.getClassCount() != 1) {
			throw new RuntimeException("Error: Array class description made up of more than one class.");
		}
		cd = cdd.getClassDetails(0);
		if(cd.getClassName().charAt(0) != '[') {
			throw new RuntimeException("Error: Array class name does not begin with '['.");
		}
		
		//newHandle
		this.newHandle();
		
		//Array size
		b1 = this._data.pop();
		b2 = this._data.pop();
		b3 = this._data.pop();
		b4 = this._data.pop();
		size = (int)(
				((b1 << 24) & 0xff000000) +
				((b2 << 16) &   0xff0000) +
				((b3 <<  8) &     0xff00) +
				((b4      ) &       0xff)
		);
		this.print("Array size - " + size + " - 0x" + this.byteToHex(b1) + " " + this.byteToHex(b2) + " " + this.byteToHex(b3) + " " + this.byteToHex(b4));
		
		//Array data
		this.print("Values");
		this.increaseIndent();
		for(int i = 0; i < size; ++i) {
			//Print element index
			this.print("Index " + i + ":");
			this.increaseIndent();
			
			//Read the field values based on the classDesc read above
			this.readFieldValue((byte)cd.getClassName().charAt(1));
			
			//Revert indent
			this.decreaseIndent();
		}
		this.decreaseIndent();
		
		//Revert indent
		this.decreaseIndent();
	}
	
	/*******************
	 * Read a TC_CLASS element from the stream.
	 * 
	 * TC_CLASS		classDesc		newHandle
	 ******************/
	private void readNewClass() {
		byte b1;
		
		//TC_CLASS
		b1 = this._data.pop();
		this.print("TC_CLASS - 0x" + this.byteToHex(b1));
		if(b1 != (byte)0x76) { throw new RuntimeException("Error: Illegal value for TC_CLASS (should be 0x76)"); }
		this.increaseIndent();
		
		//classDesc
		this.readClassDesc();
		
		//Revert indent
		this.decreaseIndent();
		
		//newHandle
		this.newHandle();
	}
	
	/*******************
	 * Read a TC_REFERENCE from the stream.
	 * 
	 * TC_REFERENCE		(int)handle
	 ******************/
	private int readPrevObject() {
		byte b1, b2, b3, b4;
		int handle;
		
		//TC_REFERENCE
		b1 = this._data.pop();
		this.print("TC_REFERENCE - 0x" + this.byteToHex(b1));
		if(b1 != (byte)0x71) { throw new RuntimeException("Error: Illegal value for TC_REFERENCE (should be 0x71)"); }
		this.increaseIndent();
		
		//Reference handle
		b1 = this._data.pop();
		b2 = this._data.pop();
		b3 = this._data.pop();
		b4 = this._data.pop();
		handle = (int)(
				((b1 << 24) & 0xff000000) +
				((b2 << 16) &   0xff0000) +
				((b3 <<  8) &     0xff00) +
				((b4      ) &       0xff)
		);
		this.print("Handle - " + handle + " - 0x" + this.byteToHex(b1) + " " + this.byteToHex(b2) + " " + this.byteToHex(b3) + " " + this.byteToHex(b4));
		
		//Revert indent
		this.decreaseIndent();
		
		//Return the handle
		return handle;
	}
	
	/*******************
	 * Read a TC_NULL from the stream.
	 * 
	 * TC_NULL
	 ******************/
	private void readNullReference() {
		byte b1;
		
		//TC_NULL
		b1 = this._data.pop();
		this.print("TC_NULL - 0x" + this.byteToHex(b1));
		if(b1 != (byte)0x70) { throw new RuntimeException("Error: Illegal value for TC_NULL (should be 0x70)"); }
	}
	
	/*******************
	 * Read a blockdatashort element from the stream.
	 * 
	 * TC_BLOCKDATA		(unsigned byte)size		contents
	 ******************/
	private void readBlockData() {
		String contents = "";
		int len;
		byte b1;
		
		//TC_BLOCKDATA
		b1 = this._data.pop();
		this.print("TC_BLOCKDATA - 0x" + this.byteToHex(b1));
		if(b1 != (byte)0x77) { throw new RuntimeException("Error: Illegal value for TC_BLOCKDATA (should be 0x77)"); }
		this.increaseIndent();
		
		//size
		len = this._data.pop() & 0xFF;
		this.print("Length - " + len + " - 0x" + this.byteToHex((byte)(len & 0xff)));
		
		//contents
		for(int i = 0; i < len; ++i) {
			contents += this.byteToHex(this._data.pop());
		}
		this.print("Contents - 0x" + contents);
		
		//Drop indent back
		this.decreaseIndent();
	}
	
	/*******************
	 * Read a blockdatalong element from the stream.
	 * 
	 * TC_BLOCKDATALONG		(int)size	contents
	 ******************/
	private void readLongBlockData() {
		String contents = "";
		long len;
		byte b1, b2, b3, b4;
		
		//TC_BLOCKDATALONG
		b1 = this._data.pop();
		this.print("TC_BLOCKDATALONG - 0x" + this.byteToHex(b1));
		if(b1 != (byte)0x7a) { throw new RuntimeException("Error: Illegal value for TC_BLOCKDATA (should be 0x77)"); }
		this.increaseIndent();
		
		//size
		b1 = this._data.pop();
		b2 = this._data.pop();
		b3 = this._data.pop();
		b4 = this._data.pop();
		len = (int)(
				((b1 << 24) & 0xff000000) +
				((b2 << 16) &   0xff0000) +
				((b3 <<  8) &     0xff00) +
				((b4      ) &       0xff)
		);
		this.print("Length - " + len + " - 0x" + this.byteToHex(b1) + " " + this.byteToHex(b2) + " " + this.byteToHex(b3) + " " + this.byteToHex(b4));
		
		//contents
		for(long l = 0; l < len; ++l) {
			contents += this.byteToHex(this._data.pop());
		}
		this.print("Contents - 0x" + contents);
		
		//Drop indent back
		this.decreaseIndent();
	}
	
	/*******************
	 * Read a newString element from the stream.
	 * 
	 * Could be:
	 *	TC_STRING		(0x74)
	 *	TC_LONGSTRING	(0x7c)
	 ******************/
	private String readNewString() {
		int handle;
		
		//Peek the type and delegate to the appropriate method
		switch(this._data.peek()) {
			case (byte)0x74:		//TC_STRING
				return this.readTC_STRING();
				
			case (byte)0x7c:		//TC_LONGSTRING
				return this.readTC_LONGSTRING();
				
			case (byte)0x71:		//TC_REFERENCE
				this.readPrevObject();
				return "[TC_REF]";
				
			default:
				this.print("Invalid newString type 0x" + this.byteToHex(this._data.peek()));
				throw new RuntimeException("Error illegal newString type.");
		}
	}
	
	/*******************
	 * Read a TC_STRING element from the stream.
	 * 
	 * TC_STRING	newHandle	utf
	 ******************/
	private String readTC_STRING() {
		String val;
		byte b1;
		
		//TC_STRING
		b1 = this._data.pop();
		this.print("TC_STRING - 0x" + this.byteToHex(b1));
		if(b1 != (byte)0x74) { throw new RuntimeException("Error: Illegal value for TC_STRING (should be 0x74)"); }
		
		//Indent
		this.increaseIndent();
		
		//newHandle
		this.newHandle();
		
		//UTF
		val = this.readUtf();
		
		//Decrease indent
		this.decreaseIndent();
		
		//Return the string value
		return val;
	}
	
	/*******************
	 * Read a TC_LONGSTRING element from the stream.
	 * 
	 * TC_LONGSTRING	newHandle	long-utf
	 ******************/
	private String readTC_LONGSTRING() {
		String val;
		byte b1;
		
		//TC_LONGSTRING
		b1 = this._data.pop();
		this.print("TC_LONGSTRING - 0x" + this.byteToHex(b1));
		if(b1 != (byte)0x7c) { throw new RuntimeException("Error: Illegal value for TC_LONGSTRING (should be 0x7c)"); }
		
		//Indent
		this.increaseIndent();
		
		//newHandle
		this.newHandle();
		
		//long-utf
		val = this.readLongUtf();
		
		//Decrease indent
		this.decreaseIndent();
		
		//Return the string value
		return val;
	}
	
	/*******************
	 * Read a UTF string from the stream.
	 * 
	 * (short)length	contents
	 ******************/
	private String readUtf() {
		String content = "", hex = "";
		byte b1, b2;
		int len;
		
		//length
		b1 = this._data.pop();
		b2 = this._data.pop();
		len = (int)(
				((b1 << 8) & 0xff00) +
				(b2 & 0xff)
		);
		this.print("Length - " + len + " - 0x" + this.byteToHex(b1) + " " + this.byteToHex(b2));
		
		//Contents
		for(int i = 0; i < len; ++i) {
			b1 = this._data.pop();
			content += (char)b1;
			hex += this.byteToHex(b1);
		}
		this.print("Value - " + content + " - 0x" + hex);
		
		//Return the string
		return content;
	}
	
	/*******************
	 * Read a long-UTF string from the stream.
	 * 
	 * (long)length		contents
	 ******************/
	private String readLongUtf() {
		String content = "", hex = "";
		byte b1, b2, b3, b4, b5, b6, b7, b8;
		long len;
		
		//Length
		b1 = this._data.pop();
		b2 = this._data.pop();
		b3 = this._data.pop();
		b4 = this._data.pop();
		b5 = this._data.pop();
		b6 = this._data.pop();
		b7 = this._data.pop();
		b8 = this._data.pop();
		len = (long)(
				((b1 << 56) & 0xff00000000000000L) +
				((b2 << 48) &   0xff000000000000L) +
				((b3 << 40) &     0xff0000000000L) +
				((b4 << 32) &       0xff00000000L) +
				((b5 << 24) &         0xff000000 ) +
				((b6 << 16) &           0xff0000 ) +
				((b7 <<  8) &             0xff00 ) +
				( b8        &               0xff )
		);
		this.print("Length - " + len + " - 0x" + this.byteToHex(b1) + " " + this.byteToHex(b2) + " " + this.byteToHex(b3) + " " + this.byteToHex(b4) + " " +
				   this.byteToHex(b5) + " " + this.byteToHex(b6) + " " + this.byteToHex(b7) + " " + this.byteToHex(b8));
		
		//Contents
		for(long l = 0; l < len; ++l) {
			b1 = this._data.pop();
			content += (char)b1;
			hex += this.byteToHex(b1);
		}
		this.print("Value - " + content + " - 0x" + hex);
		
		//Return the string
		return content;
	}
	
	/*******************
	 * Read a byte field.
	 ******************/
	private void readByteField() {
		byte b1 = this._data.pop();
		if(((int)b1) >= 0x20 && ((int)b1) <= 0x7e) {
			//Print with ASCII
			this.print("(byte)" + b1 + " (ASCII: " + ((char)b1) + ") - 0x" + this.byteToHex(b1));
		} else {
			//Just print byte value
			this.print("(byte)" + b1 + " - 0x" + this.byteToHex(b1));
		}
	}
	
	/*******************
	 * Read a char field.
	 ******************/
	private void readCharField() {
		byte b1 = this._data.pop();
		byte b2 = this._data.pop();
		char c1 = (char)(((b1 << 8) & 0xff00) + (b2 & 0xff));
		this.print("(char)" + (char)c1 + " - 0x" + this.byteToHex(b1) + " " + this.byteToHex(b2));
	}
	
	/*******************
	 * Read a double field.
	 ******************/
	private void readDoubleField() {
		byte b1, b2, b3, b4, b5, b6, b7, b8;
		b1 = this._data.pop(); b2 = this._data.pop(); b3 = this._data.pop(); b4 = this._data.pop();
		b5 = this._data.pop(); b6 = this._data.pop(); b7 = this._data.pop(); b8 = this._data.pop();
		this.print("(double)" + Double.longBitsToDouble((((long)b1 << 56) & 0xff00000000000000L) +
														(((long)b2 << 48) &   0xff000000000000L) +
														(((long)b3 << 40) &     0xff0000000000L) +
														(((long)b4 << 32) &       0xff00000000L) +
														((      b5 << 24) &         0xff000000 ) +
														((      b6 << 16) &           0xff0000 ) +
														((      b7 <<  8) &             0xff00 ) +
														((      b8      ) &               0xff )) +
				   " - 0x" + this.byteToHex(b1) + " " + this.byteToHex(b2) + " " + this.byteToHex(b3) +
				   " " + this.byteToHex(b4) + " " + this.byteToHex(b5) + " " + this.byteToHex(b6) + " " +
				   this.byteToHex(b7) + " " + this.byteToHex(b8));
	}
	
	/*******************
	 * Read a float field.
	 ******************/
	private void readFloatField() {
		byte b1, b2, b3, b4;
		b1 = this._data.pop(); b2 = this._data.pop(); b3 = this._data.pop(); b4 = this._data.pop();
		this.print("(float)" + Float.intBitsToFloat(((b1 << 24) & 0xff000000) +
													((b2 << 16) &   0xff0000) +
													((b3 <<  8) &     0xff00) +
													( b4        &       0xff)) +
				   " - 0x" + this.byteToHex(b1) + " " + this.byteToHex(b2) + " " + this.byteToHex(b3) +
				   " " + this.byteToHex(b4));
	}
	
	/*******************
	 * Read an int field.
	 ******************/
	private void readIntField() {
		byte b1, b2, b3, b4;
		b1 = this._data.pop(); b2 = this._data.pop(); b3 = this._data.pop(); b4 = this._data.pop();
		this.print("(int)" + (int)(((b1 << 24) & 0xff000000) +
								   ((b2 << 16) &   0xff0000) +
								   ((b3 <<  8) &     0xff00) +
								   ( b4        &       0xff)) + " - 0x" + this.byteToHex(b1) + " " + this.byteToHex(b2) + " " + this.byteToHex(b3) +
				   " " + this.byteToHex(b4));
	}
	
	/*******************
	 * Read a long field.
	 ******************/
	private void readLongField() {
		byte b1, b2, b3, b4, b5, b6, b7, b8;
		b1 = this._data.pop(); b2 = this._data.pop(); b3 = this._data.pop(); b4 = this._data.pop();
		b5 = this._data.pop(); b6 = this._data.pop(); b7 = this._data.pop(); b8 = this._data.pop();
		this.print("(long)" + (long)(((b1 << 56) & 0xff00000000000000L) +
									 ((b2 << 48) &   0xff000000000000L) +
									 ((b3 << 40) &     0xff0000000000L) +
									 ((b4 << 32) &       0xff00000000L) +
									 ((b5 << 24) &         0xff000000 ) +
									 ((b6 << 16) &           0xff0000 ) +
									 ((b7 <<  8) &             0xff00 ) +
									 ( b8        &               0xff)) + " - 0x" + this.byteToHex(b1) +
				   " " + this.byteToHex(b2) + " " + this.byteToHex(b3) + " " + this.byteToHex(b4) + " " + this.byteToHex(b5) + " " + this.byteToHex(b6) + " " +
				   this.byteToHex(b7) + " " + this.byteToHex(b8));
	}
	
	/*******************
	 * Read a short field.
	 ******************/
	private void readShortField() {
		byte b1, b2;
		b1 = this._data.pop(); b2 = this._data.pop();
		this.print("(short)" + (short)(((b1 << 8) & 0xff00) + (b2 & 0xff)) + " - 0x" + this.byteToHex(b1) + " " + this.byteToHex(b2));
	}
	
	/*******************
	 * Read a boolean field.
	 ******************/
	private void readBooleanField() {
		byte b1 = this._data.pop();
		this.print("(boolean)" + (b1 == 0 ? "false" : "true") + " - 0x" + this.byteToHex(b1));
	}
	
	/*******************
	 * Read an array field.
	 ******************/
	private void readArrayField() {
		//Print field type and increase indent
		this.print("(array)");
		this.increaseIndent();
		
		//Array could be null
		switch(this._data.peek()) {
			case (byte)0x70:		//TC_NULL
				this.readNullReference();
				break;
				
			case (byte)0x75:		//TC_ARRAY
				this.readNewArray();
				break;
				
			case (byte)0x71:		//TC_REFERENCE
				this.readPrevObject();
				break;
				
			default:				//Unknown
				throw new RuntimeException("Error: Unexpected array field value type (0x" + this.byteToHex(this._data.peek()));
		}
		
		//Revert indent
		this.decreaseIndent();
	}
	
	/*******************
	 * Read an object field.
	 ******************/
	private void readObjectField() {
		this.print("(object)");
		this.increaseIndent();
		
		//Object fields can have various types of values...
		switch(this._data.peek()) {
			case (byte)0x73:		//New object
				this.readNewObject();
				break;
				
			case (byte)0x71:		//Reference
				this.readPrevObject();
				break;
				
			case (byte)0x70:		//Null
				this.readNullReference();
				break;
				
			case (byte)0x74:		//TC_STRING
				this.readTC_STRING();
				break;
				
			case (byte)0x76:		//TC_CLASS
				this.readNewClass();
				break;
				
			case (byte)0x75:		//TC_ARRAY
				this.readNewArray();
				break;
				
			case (byte)0x7e:		//TC_ENUM
				this.readNewEnum();
				break;
				
			default:				//Unknown/unsupported
				throw new RuntimeException("Error: Unexpected identifier for object field value 0x" + this.byteToHex(this._data.peek()));
		}
		this.decreaseIndent();
	}
}
