package net.cbaakman.occupy.network;

import java.io.Serializable;
import java.util.UUID;

import lombok.Data;

@Data
public class Update implements Serializable {
	
	public Update(Class<? extends Updatable> objectClass, UUID objectID, String fieldID, Object value) {
		this.objectClass = objectClass;
		this.objectID = objectID;
		this.fieldID = fieldID;
		this.value = value;
	}
	
	private Class<? extends Updatable> objectClass;
	private UUID objectID;
	private String fieldID;
	private Object value;
}
