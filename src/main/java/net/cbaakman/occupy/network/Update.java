package net.cbaakman.occupy.network;

import java.io.Serializable;
import java.util.UUID;

import lombok.Data;

@Data
public class Update implements Serializable {
	
	public Update(UUID objectID, String fieldID, Object value) {
		this.objectID = objectID;
		this.fieldID = fieldID;
		this.value = value;
	}
	
	private UUID objectID;
	private String fieldID;
	private Object value;
}
