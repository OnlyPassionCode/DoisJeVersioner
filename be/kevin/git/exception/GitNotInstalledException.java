package be.kevin.git.exception;

/**
 * TODO : Erreur qui permet de dire que git n'est pas installé sur la machine.
 */
public class GitNotInstalledException extends GitException{

	private static final long serialVersionUID = 4027913694832091745L;

	public GitNotInstalledException(String message) {
		super(message);
	}

}
