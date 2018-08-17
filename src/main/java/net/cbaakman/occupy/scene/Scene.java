package net.cbaakman.occupy.scene;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashMap;
import java.util.Map;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import net.cbaakman.occupy.Update;

public abstract class Scene implements GLEventListener, MouseListener, MouseWheelListener, MouseMotionListener, KeyListener {

	@Override
	public void keyTyped(KeyEvent e) {
	}

	private Map<Integer, Boolean> keyDown = new HashMap<Integer, Boolean>();

	@Override
	public void keyPressed(KeyEvent e) {
		synchronized(keyDown) {
			keyDown.put(e.getKeyCode(), true);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		synchronized(keyDown) {
			keyDown.put(e.getKeyCode(), false);
		}
	}
	
	public boolean isKeyDown(int keyCode) {
		synchronized(keyDown) {
			return keyDown.containsKey(keyCode) && keyDown.get(keyCode);
		}
	}
	
	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent evt) {
    }

	@Override
	public void mousePressed(MouseEvent evt) {
    }

	@Override
	public void mouseReleased(MouseEvent evt) {
    }

	@Override
	public void mouseEntered(MouseEvent evt) {
    }

	@Override
    public void mouseExited(MouseEvent evt) {
    }

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
	}

	@Override
	public void init(GLAutoDrawable drawable) {
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
	}

	@Override
	public void display(GLAutoDrawable drawable) {
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
	}
	
	public void onUpdateFromServer(Update update) {
	}

	public void update(float dt) {
	}
}
