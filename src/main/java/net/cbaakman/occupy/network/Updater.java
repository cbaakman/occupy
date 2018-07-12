package net.cbaakman.occupy.network;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.cbaakman.occupy.network.enums.MessageType;
import lombok.AccessLevel;

@Data
public abstract class Updater {
	
	@Setter(AccessLevel.NONE)
	private UUID id;
	
	public Updater() {
		this.id = UUID.randomUUID();
	}

	@Setter(AccessLevel.NONE)
	@Getter(AccessLevel.NONE)
	protected Map<UUID, Updatable> updatables = new HashMap<UUID, Updatable>();
	
	public abstract void update(final float dt);
	
	protected List<Update> getUpdatesWith(Class<? extends Annotation> annotationClass) {
		
		List<Update> updates = new ArrayList<Update>();
		
		synchronized(updatables) {
			for (Entry<UUID, Updatable> entry : updatables.entrySet()) {
				UUID objectId = entry.getKey();
				Updatable updatable = entry.getValue();
				
				Class<? extends Updatable> objectClass = updatable.getClass();
				
				for (Field field : objectClass.getDeclaredFields()) {
					
					if (field.isAnnotationPresent(annotationClass)) {
						
						field.setAccessible(true);
				
						try {
							Update update = new Update(objectClass, objectId, field.getName(), field.get(updatable));
							
							updates.add(update);
							
						} catch (IllegalArgumentException | IllegalAccessException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		
		return updates;
	}
	
	protected void processUpdateWith(Update update, Class<? extends Annotation> annotationClass, UUID originId,
									 boolean createUpdatables) {
		
		Updatable updatable;
		synchronized(updatables) {
			updatable = updatables.get(update.getObjectID());
		}
			
		if (updatable == null) {
			try {
				updatable = update.getObjectClass().newInstance();
				
				synchronized(updatables) {
					updatables.put(update.getObjectID(), updatable);
				}
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				return;
			}
		}
		
		if (!updatable.getOwnerId().equals(originId))
			return;
		
		try {
			Field field = update.getObjectClass().getField(update.getFieldID());
			
			if (field.isAnnotationPresent(annotationClass)) {
			
				field.setAccessible(true);
			
				field.set(updatable, update.getValue());
			}
			
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
}
