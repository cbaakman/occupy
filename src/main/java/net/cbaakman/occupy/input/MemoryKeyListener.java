package net.cbaakman.occupy.input;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class MemoryKeyListener extends KeyAdapter {
	
	static Logger logger = Logger.getLogger(MemoryKeyListener.class);

	private Map<Integer, Boolean> keyDown = new HashMap<Integer, Boolean>();

	@Override
	public void keyPressed(KeyEvent e) {
		keyDown.put(e.getKeyCode(), true);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		keyDown.put(e.getKeyCode(), false);
	}
	
	public boolean isKeyDown(int keyCode) {
		return keyDown.containsKey(keyCode) && keyDown.get(keyCode);
	}
}
