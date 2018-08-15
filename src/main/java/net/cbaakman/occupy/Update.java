package net.cbaakman.occupy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.Data;

@Data
public class Update implements Serializable {
	
	public Update(Class<? extends Updatable> objectClass, UUID objectID) {
		this.objectClass = objectClass;
		this.objectID = objectID;
	}
	
	private Class<? extends Updatable> objectClass;
	private UUID objectID;
	
	private Map<String, Object> fieldValues = new HashMap<String, Object>();
	
	public void setValue(String fieldId, Object value) {
		fieldValues.put(fieldId, value);
	}
}
