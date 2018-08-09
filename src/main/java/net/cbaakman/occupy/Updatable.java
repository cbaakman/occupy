package net.cbaakman.occupy;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public abstract class Updatable {
	
	private Identifier ownerId;
	
	protected Updatable() {
		this.ownerId = null;
	}
	
	public void updateOnClient(final float dt) {
	}
	public void updateOnServer(final float dt) {
	}
	
	public List<Field> getDeclaredFieldsSinceUpdatable() {
		
		Class<?> clazz = this.getClass();
		
		List<Field> list = new ArrayList<Field>();		
		
		while (clazz != null && !clazz.equals(Updatable.class)) {
			for (Field field : clazz.getDeclaredFields()) {
				list.add(field);
			}
			clazz = clazz.getSuperclass();
		}
		return list;
	}
	
	public Field getDeclaredFieldSinceUpdatable(String fieldId) throws NoSuchFieldException {
		for (Field field : getDeclaredFieldsSinceUpdatable()) {
			if (field.getName().equals(fieldId))
				return field;
		}
		throw new NoSuchFieldException(fieldId);
	}
}
