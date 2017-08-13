package nb.deser.support;

/***********************************************************
 * Support class for serialization data parsing that holds
 * details of a class field to enable the field value to
 * be read from the stream.
 * 
 * Written by Nicky Bloor (@NickstaDB).
 **********************************************************/
public class ClassField {
	/*******************
	 * Properties
	 ******************/
	private final byte _typeCode;		//The field type code
	private String _name;				//The field name
	private String _className1;			//The className1 property (object and array type fields)
	
	/*******************
	 * Construct the ClassField object.
	 * 
	 * @param typeCode The field type code.
	 ******************/
	public ClassField(byte typeCode) {
		this._typeCode = typeCode;
		this._name = "";
		this._className1 = "";
	}
	
	/*******************
	 * Get the field type code.
	 * 
	 * @return The field type code.
	 ******************/
	public byte getTypeCode() {
		return this._typeCode;
	}
	
	/*******************
	 * Set the field name.
	 * 
	 * @param name The field name.
	 ******************/
	public void setName(String name) {
		this._name = name;
	}
	
	/*******************
	 * Get the field name.
	 * 
	 * @return The field name.
	 ******************/
	public String getName() {
		return this._name;
	}
	
	/*******************
	 * Set the className1 property of the field.
	 * 
	 * @param cn1 The className1 value.
	 ******************/
	public void setClassName1(String cn1) {
		this._className1 = cn1;
	}
}
