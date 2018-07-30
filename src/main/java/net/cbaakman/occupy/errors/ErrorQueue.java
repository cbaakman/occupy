package net.cbaakman.occupy.errors;

import java.util.ArrayList;
import java.util.List;

/**
 * Transfers errors to the main thread, where they can be handled
 *
 */
public class ErrorQueue {

	private List<Exception> exceptionsEncountered = new ArrayList<Exception>();
	
	public void pushError(Exception e) {
		synchronized(exceptionsEncountered) {
			exceptionsEncountered.add(e);
		}
	}
	
	public void throwAnyFirstEncounteredError()
			throws RenderError, InitError, CommunicationError {

		synchronized(exceptionsEncountered) {
			if (exceptionsEncountered.size() > 0) {
				Exception e = exceptionsEncountered.get(0);
				exceptionsEncountered.remove(e);
				
				if (e instanceof RenderError)
					throw (RenderError)e;
				else if (e instanceof InitError)
					throw (InitError)e;
				else if (e instanceof CommunicationError)
					throw (CommunicationError)e;
				else if (e instanceof SeriousError)
					throw (SeriousError)e;
				else
					throw new SeriousError(e);
			}
		}
	}
}
