package be.kevin.kernel;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import be.kevin.exception.DoisJeVersionerException;
import be.kevin.exception.DoisJeVersionerNoRootElementException;
import be.kevin.exception.DoisJeVersionerParseFileException;
import be.kevin.git.FileStatus;
import be.kevin.git.Git;
import be.kevin.git.exception.GitException;

public class DoisJeVersioner {
	
	private Git git;
	private File directory;
	private String actualVersion;
	private String lastVersion;
	
	public DoisJeVersioner(File directory) {
		this.directory = directory;
		
		try {
			this.git = new Git(directory);
		} catch (GitException e) {
			e.printStackTrace();
			return;
		}
    
	    // On parcours le dossier pour le vérifier.
		List<String> messages = this.analyzeDirectory(directory);
		for (String message : messages) {
			System.out.println(message);
		}
	}
	
	/**
	 * Crée le fichier temporaire qui permet de comparer les versions du pom.xml.
	 * 
	 * @throws GitException
	 */
	private void createTempFile() throws GitException {
		// "HEAD~0" c'est pour le commit actuel.
		this.git.exportFileFromHead(Config.pomFile, Config.tempFile, "HEAD~0");
	}
	
	/**
	 * Permet de supprimer le fichier temporaire qui avait été crée pour comparer les versions
	 * @throws DoisJeVersionerException
	 */
	private void removeTempFile() throws DoisJeVersionerException {
		Path path = Paths.get(this.directory.getAbsolutePath(), Config.tempFile);
		// Si le fichier n'éxiste pas on arrête ici
		if(!Files.exists(path)) {
			return;
		}
		try {
            Files.delete(path);
        } catch (Exception exception) {
            throw new DoisJeVersionerException(exception.getMessage());
        }
	}
	
	/**
	 * Permet de récupérer le status du fichier pom.xml
	 * Si il n'éxiste pas on considére qu'il n'a pas eu de changement
	 * 
	 * @return
	 * @throws GitException
	 */
	private FileStatus hasPomFileChanged() throws GitException {
	    final Map<String, FileStatus> filesStatus = this.git.statusPorcelain();
	    FileStatus status = filesStatus.get(Config.pomFile);
	    // Si le fichier pom.xml ne figure pas c'est que il est forcément pas à jour.
	    
	    if(status == null) {
	    	return FileStatus.UNCHANGED;
	    }
	    
	    return status;
	}
	
	/**
	 * Crée un fichier temporaire afin de comparer les versions du fichier pom actuel avec celui du commit actuel.
	 * Si les version ne concorde pas, on return true sinon false
	 * 
	 * Si la balise version n'est pas dans le fichier pom.xml, on ignore
	 * 
	 * @return
	 * @throws GitException
	 * @throws DoisJeVersionerException
	 */
	private boolean doisJeVersioner() throws GitException, DoisJeVersionerException {
		this.createTempFile();
		
        this.actualVersion = this.getVersion(new File(this.directory.getAbsolutePath(), Config.pomFile));
        if(this.actualVersion == null) {
        	return false;
        }
        
        this.lastVersion = this.getVersion(new File(this.directory.getAbsolutePath(), Config.tempFile));
        if(this.lastVersion == null) {
        	return false;
        }
        
		return !actualVersion.equals(lastVersion);
	}
	
	/**
	 * Retourne la version du fichier xml.
	 * 
	 * @param file Un fichier xml.
	 * @return La version du xml sinon {@code null}.
	 * 
	 * @throws DoisJeVersionerException
	 */
	private String getVersion(File file) throws DoisJeVersionerException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;
        Document document;
		try {
			documentBuilder = factory.newDocumentBuilder();
			document = documentBuilder.parse(file);
		} catch (Exception exception) {
			throw new DoisJeVersionerParseFileException(exception.getMessage());
		}
		
		// Normaliser le document (supprimer les espaces inutiles).
        document.getDocumentElement().normalize();
        Element root = document.getDocumentElement();
        if(root == null) {
        	throw new DoisJeVersionerNoRootElementException(String.format("Le fichier %s n'existe pas dans le dossier : %s", file.getName(), file.getParent()));
        }
        
        NodeList children = root.getChildNodes();
        Node version = null;
        for(int i = 0; i < children.getLength(); ++i) {
        	Node child = children.item(i);
        	if (child.getNodeType() == Node.ELEMENT_NODE && "version".equals(child.getNodeName())) {
                version = child;
                break;
            }
        }
        
        if(version == null) {
        	return null;
        }
        
        return version.getTextContent();
	}
	
	private List<String> analyzeDirectory(File directory) {
		List<String> messageList = new ArrayList<String> ();
		try {
			if(!this.git.isInit()) {
				messageList.add(String.format("Le dossier %s n'a pas été git init.", directory.getAbsolutePath()));
				return messageList;
			}
			
			// Si aucun changement n'est fait on ignore :
			if(!this.git.hasChanges()) {
				messageList.add(String.format("Le dossier %s n'a aucun changement.", directory.getAbsolutePath()));
				return messageList;
			}
			
			final FileStatus pomStatus = this.hasPomFileChanged();
			
			switch(pomStatus) {
				case UNCHANGED -> {
					messageList.add(String.format("La version n'est pas à jour : %s", directory.getAbsolutePath()));
					return messageList;
				}
				
				// On ignore car le projet est nouveau.
				case CREATED -> {
					messageList.add(String.format("La projet est nouveau : %s", directory.getAbsolutePath()));
					return messageList;
				}
			}
			
			// On vérifie si la version a bien été changée.
			if(this.doisJeVersioner()) {
				messageList.add(String.format("La version n'est pas à jour : %s", directory.getAbsolutePath()));
				return messageList;
			}
			
			messageList.add(String.format("La projet est à jour : %s", directory.getAbsolutePath()));

			return messageList;
		}catch(GitException gitException) {
			gitException.printStackTrace();
		}catch(DoisJeVersionerException doisJeVersionerException) {
			doisJeVersionerException.printStackTrace();
		}
		// Même si il y a un return , le finally sera appelé
		finally {
			try {
				this.removeTempFile();
			} catch (DoisJeVersionerException e) {
				e.printStackTrace();
			}
		}
		
		return messageList;
	}
}
