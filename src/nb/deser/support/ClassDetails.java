package nb.deser.support;

import java.util.ArrayList;

/***********************************************************
 * Support class for serialization data parsing that holds
 * details of a single class to enable class data for that
 * class to be read (classDescFlags, field descriptions).
 * 
 * Written by Nicky Bloor (@NickstaDB).
 **********************************************************/
public class ClassDetails {
	/*******************
	 * Properties
	 ******************/
	private final	String _className;							//The name of the class
	private int _refHandle;										//The reference handle for the class
	private byte _classDescFlags;								//The classDescFlags value for the class
	private final ArrayList<ClassField> _fieldDescriptions;		//The class field descriptions
	
	/*******************
	 * Construct the ClassDetails object.
	 * 
	 * @param className The name of the class.
	 ******************/
	public ClassDetails(String className) {
		this._className = className;
		this._refHandle = -1;
		this._classDescFlags = 0;
		this._fieldDescriptions = new ArrayList<>();
	}
	
	/*******************
	 * Get the class name.
	 * 
	 * @return The class name.
	 ******************/
	public String getClassName() {
		return this._className;
	}
	
	/*******************
	 * Set the reference handle of the class.
	 * 
	 * @param handle The reference handle value.
	 ******************/
	public void setHandle(int handle) {
		this._refHandle = handle;
	}
	
	/*******************
	 * Get the reference handle.
	 * 
	 * @return The reference handle value for this class.
	 ******************/
	public int getHandle() {
		return this._refHandle;
	}
	
	/*******************
	 * Set the classDescFlags property.
	 * 
	 * @param classDescFlags The classDescFlags value.
	 ******************/
	public void setClassDescFlags(byte classDescFlags) {
		this._classDescFlags = classDescFlags;
	}
	
	/*******************
	 * Check whether the class is SC_SERIALIZABLE.
	 * 
	 * @return True if the classDescFlags includes SC_SERIALIZABLE.
	 ******************/
	public boolean isSC_SERIALIZABLE() {
		return (this._classDescFlags & 0x02) == 0x02;
	}
	
	/*******************
	 * Check whether the class is SC_EXTERNALIZABLE.
	 * 
	 * @return True if the classDescFlags includes SC_EXTERNALIZABLE.
	 ******************/
	public boolean isSC_EXTERNALIZABLE() {
		return (this._classDescFlags & 0x04) == 0x04;
	}
	
	/*******************
	 * Check whether the class is SC_WRITE_METHOD.
	 * 
	 * @return True if the classDescFlags includes SC_WRITE_METHOD.
	 ******************/
	public boolean isSC_WRITE_METHOD() {
		return (this._classDescFlags & 0x01) == 0x01;
	}
	
	/*******************
	 * Check whether the class is SC_BLOCKDATA.
	 * 
	 * @return True if the classDescFlags includes SC_BLOCKDATA.
	 ******************/
	public boolean isSC_BLOCKDATA() {
		return (this._classDescFlags & 0x08) == 0x08;
	}
	
	/*******************
	 * Add a field description to the class details object.
	 * 
	 * @param cf The ClassField object describing the field.
	 ******************/
	public void addField(ClassField cf) {
		this._fieldDescriptions.add(cf);
	}
	
	/*******************
	 * Get the class field descriptions.
	 * 
	 * @return An array of field descriptions for the class.
	 ******************/
	public ArrayList<ClassField> getFields() {
		return this._fieldDescriptions;
	}
	
	/*******************
	 * Set the name of the last field to be added to the ClassDetails object.
	 * 
	 * @param name The field name.
	 ******************/
	public void setLastFieldName(String name) {
		this._fieldDescriptions.get(this._fieldDescriptions.size() - 1).setName(name);
	}
	
	/*******************
	 * Set the className1 of the last field to be added to the ClassDetails
	 * object.
	 * 
	 * @param cn1 The className1 value.
	 ******************/
	public void setLastFieldClassName1(String cn1) {
		this._fieldDescriptions.get(this._fieldDescriptions.size() - 1).setClassName1(cn1);
	}
}
