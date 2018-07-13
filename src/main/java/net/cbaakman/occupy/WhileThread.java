package net.cbaakman.occupy;

public abstract class WhileThread extends Thread {

	private boolean running;
	
	public WhileThread(String name) {
		super(name);
	}

	@Override
	public void run() {
		running = false;
		while (running) {
			repeat();
		}
	}
	
	public void stopRunning() {
		running = false;
	}

	protected abstract void repeat();
}
