
package dev.ceymikey.exceptions;

public class InjectionFailureException extends RuntimeException {

	public InjectionFailureException() {
		super("An issue occurred while injecting : content of the embed was empty");
	}
}
