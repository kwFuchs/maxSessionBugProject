package mdbs;

/**
 * Indicating a problem in the processing of stack calls.
 * @author Angelo.Fuchs 
 */
public class StackException extends Exception {

	StackException(String message, Exception e) {
		super(message, e);
	}
	
}
