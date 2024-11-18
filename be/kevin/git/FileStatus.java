package be.kevin.git;

import be.kevin.git.exception.GitInvalidFileStateException;

/**
 * Représente les status possible d'un fichier git
 */
public enum FileStatus {
	/**
	 * Le fichier a été créé.
	 */
    CREATED("??"),
    /**
     * Le fichier a été modifié
     */
    MODIFIED("M"),
    /**
     * Le fichier n'a pas été touché.
     */
    UNCHANGED(""),
    /**
     * Le fichier a été supprimé.
     */
	DELETED("D");
	
	/**
	 * Le status de l'instance de l'enum
	 */
	public final String STATE;
	
	/**
	 * 
	 * @param state
	 */
	private FileStatus(String state) {
		this.STATE = state;
	}
	
    /**
     * Retourne un {@code FileStatus} selon le {@code state}.
     * 
     * @param state
     * @return
     * @throws GitInvalidFileStateException
     */
    public static FileStatus fromState(String state) throws GitInvalidFileStateException {
        for (FileStatus status : FileStatus.values()) {
            if (status.STATE.equals(state)) {
                return status;
            }
        }
        
        throw new GitInvalidFileStateException("État non valide: " + state);
    }
}
