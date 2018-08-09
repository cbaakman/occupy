package net.cbaakman.occupy.load;

import lombok.Data;

@Data
public class LoadStats {

	private int waiting = 0,
				running = 0,
				done = 0,
				error = 0;
	
	public int getTotal() {
		return waiting + running + done + error;
	}
}
