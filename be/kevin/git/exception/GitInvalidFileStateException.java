package be.kevin.git.exception;

public class GitInvalidFileStateException extends GitException {

	private static final long serialVersionUID = 9134012849275059896L;

	public GitInvalidFileStateException(String message) {
		super(message);
	}

}
