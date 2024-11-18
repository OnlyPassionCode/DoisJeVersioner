package be.kevin.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import be.kevin.git.exception.GitCloneException;
import be.kevin.git.exception.GitException;
import be.kevin.git.exception.GitExportFromHeadException;
import be.kevin.git.exception.GitNotFoundException;

public class Git {
	
	private ProcessBuilder processBuilder;
	private File directory;
	private static boolean gitInstalled = false;
	
	public Git(File directory) throws GitException {
		Git.checkGitInstalled();
		this.directory = directory;
		this.processBuilder = new ProcessBuilder();
		this.processBuilder.directory(this.directory);
	}
	
	private static void checkGitInstalled() throws GitException{
		if(!Git.gitInstalled) {
			Git.checkGitFound();
			Git.gitInstalled = true;
		}
    }
	
	private static void checkGitFound() throws GitException{
		try {
            // Exécute la commande "git --version"
            ProcessBuilder processBuilder = new ProcessBuilder("git", "--version");
            Process process = processBuilder.start();

            // Lire la sortie de la commande
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();

            if(line == null) {
            	throw new GitNotFoundException("La commande git n'a pas été trouvé sur la machine.");
            }
        } catch (Exception exception) {
        	throw new GitException(exception.getMessage());
        }
	}
	
	
	/**
	 * Exporte un fichier depuis une révision spécifique dans l'historique Git à partir de la branche HEAD.
	 * Cette méthode appelle la version plus générale en utilisant le répertoire de travail par défaut.
	 *
	 * @param fileName      Le nom du fichier à exporter depuis Git.
	 * @param newFileName   Le nom du fichier exporté dans le répertoire de destination.
	 * @param head          SOIT donner l'index de la révision ("<em>HEAD~n</em>") à partir de laquelle le fichier sera exporté SOIT l'identifiant du commit SOIT le tag du commit.
	 * @throws GitException Si une erreur se produit pendant l'exportation du fichier.
	 */
	public void exportFileFromHead(String fileName, String newFileName, String head) throws GitException {
	    // Appel de la méthode générale en utilisant le répertoire par défaut (this.directory).
	    this.exportFileFromHead(fileName, newFileName, head, this.directory);
	}

	/**
	 * Exporte un fichier depuis une révision spécifique dans l'historique Git à partir de la branche HEAD,
	 * et le place dans un répertoire de destination spécifié.
	 *
	 * @param fileName          	Le nom du fichier à exporter depuis Git.
	 * @param newFileName       	Le nom du fichier exporté dans le répertoire de destination.
	 * @param head          		SOIT donner l'index de la révision ("<em>HEAD~n</em>") à partir de laquelle le fichier sera exporté SOIT l'identifiant du commit SOIT le tag du commit.
	 * @param destinationDirectory	Le répertoire dans lequel le fichier sera exporté.
	 * @throws GitException     	Si une erreur se produit pendant l'exportation du fichier.
	 */
	public void exportFileFromHead(String fileName, String newFileName, String head, File destinationDirectory) throws GitException {
	    // Prépare la commande Git pour récupérer le fichier à partir de la révision (head).
	    this.processBuilder.command("git", "show", head + ":" + fileName);
	    
	    // Redirige la sortie du processus vers un fichier dans le répertoire de destination.
	    processBuilder.redirectOutput(new File(destinationDirectory.getAbsolutePath(), newFileName));
	    
	    try {
	        // Lancer la commande et obtenir le processus.
	        Process process = processBuilder.start();
	        
	        // Attend que la commande Git s'exécute et obtient le code de sortie.
	        int exitCode = process.waitFor();
	        
	        // Si le code de sortie n'est pas 0, cela signifie qu'une erreur est survenue.
	        if (exitCode != 0) {
	            throw new GitExportFromHeadException(String.format("Échec de l'exportation du fichier %s à partir de %s.", fileName, head));
	        }
	    } // Exception car il y a des erreurs de type Runtime qui peuvent arriver.
	    catch (Exception exception) {
	        // Gérer les exceptions en lançant une exception GitException.
	        throw new GitException(exception.getMessage());
	    }
	}

    
    public boolean isInit() throws GitException{
    	this.processBuilder.command("git", "status");
        try {
        	// Exécute la commande "git status"
            Process process = this.processBuilder.start();

            int exitCode = process.waitFor();
            
            if(exitCode != 0) {
            	return false;
            }
        } catch (Exception exception) {
            throw new GitException(exception.getMessage());
        }
        return true;
    }
    
    public Map<String, FileStatus> statusPorcelain() throws GitException{
    	this.processBuilder.command("git", "status", "--porcelain");
        try {
        	// Exécute la commande
            Process process = this.processBuilder.start();
            final Map<String, FileStatus> filesStatus = new HashMap<String, FileStatus>();
            // Lire la sortie de la commande
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
	            final String[] lineSplit = line.trim().split(" ");
	            FileStatus state = FileStatus.fromState(lineSplit[0]);
	            final String fileName = lineSplit[1];
	            
	            filesStatus.put(fileName, state);
            }

            return filesStatus;
        } catch (Exception exception) {
        	// Sois une erreur de processBuilder
        	// Sois une erreur de FileStatus.fromState
            throw new GitException(exception.getMessage());
        }
    }
    
	/**
	 * Vérifie si le dossier Git a des changements.
	 * 
	 * @return
	 * @throws GitException
	 */
	public boolean hasChanges() throws GitException {
	    // On exécute git status --porcelain
	    this.processBuilder.command("git", "status", "--porcelain");
	    Process process;

	    try {
	        process = this.processBuilder.start();
	    } catch (Exception exception) {
	        throw new GitException(exception.getMessage());
	    }

	    // On lit la réponse reçue par la commande git
	    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
	        // Si une seule ligne est retournée, c'est qu'il y a des changements
	        if (reader.readLine() != null) {
	            return true;  // Il y a des changements
	        } else {
	            return false;  // Pas de changement
	        }
	    } catch (Exception exception) {
	    	throw new GitException(exception.getMessage());
	    }
	}
    
    /**
     * Clone un dépôt Git à partir d'une {@code URI} dans le répertoire par défaut de l'objet (this.directory).
     * 
     * @param uri L'URI du dépôt Git à cloner (peut être une URL HTTP(S), SSH, ou un chemin local).
     * @throws GitException Si une erreur survient lors de l'exécution de la commande git clone.
     */
    public void clone(URI uri) throws GitException {
        // Appelle la méthode clone en spécifiant le répertoire par défaut (this.directory).
        this.clone(uri, this.directory);
    }
    
    /**
     * Clone un dépôt Git à partir d'une {@code URI} dans un répertoire de destination spécifié.
     * 
     * @param uri L'URI du dépôt Git à cloner (peut être une URL HTTP(S), SSH, ou un chemin local).
     * @param destinationDirectory Le répertoire où le dépôt sera cloné.
     * @throws GitException Si une erreur survient lors de l'exécution de la commande git clone.
     */
    public void clone(URI uri, File destinationDirectory) throws GitException {
        try {
            // Prépare la commande git clone avec l'URI du dépôt et le chemin absolu du répertoire de destination.
            this.processBuilder.command("git", "clone", uri.toString(), destinationDirectory.getAbsolutePath());
            
            // Démarre le processus d'exécution de la commande.
            Process process = this.processBuilder.start();
            
            // Attend que le processus se termine et récupère le code de sortie.
            int exitCode = process.waitFor();
            
            // Si le code de sortie n'est pas 0, cela indique une erreur dans le clonage.
            if (exitCode != 0) {
                // Lance une exception personnalisée GitCloneException si le clonage échoue.
                throw new GitCloneException("Une erreur s'est produite lors du clone du dépôt : " 
                    + uri.toString() + " vers le dossier : " + destinationDirectory.getAbsolutePath());
            }
        } catch (Exception exception) {
            // En cas d'erreur lors de l'exécution de la commande, lance une GitException avec le message d'erreur.
            throw new GitException(exception.getMessage());
        }
    }

}
