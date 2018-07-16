package net.cbaakman.occupy;

public abstract class WhileThread extends Thread {

	private boolean running;
	
	public WhileThread(String name) {
		super(name);
	}

	@Override
	public void run() {
		running = true;
		while (running) {
			repeat();
		}
	}
	
	public void stopRunning() throws InterruptedException {
		running = false;
		join();
	}

	protected abstract void repeat();
}
