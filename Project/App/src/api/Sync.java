package api;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import entity.Arquivo;
import entity.Diretorio;
import entity.Elemento;

public class Sync {
	public Derpbox db;
	
	public Sync(Derpbox db){
		this.db = db;
	}	
	
	/**
	 * Indica se dois objetos do tipo Date s�o iguais (para uso dessa aplica��o), com precis�o de segundos
	 * @param a
	 * @param b
	 * @return Boolean que indica se os objetos do tipo Date s�o iguais
	 */
	@SuppressWarnings("deprecation")
	private boolean equals (Date a, Date b){
		boolean anoIgual = a.getYear() == b.getYear();
		boolean mesIgual = a.getMonth() == b.getMonth();
		boolean diaIgual = a.getDate() == b.getDate();
		boolean horaIgual = a.getHours() == b.getHours();
		boolean minutoIgual = a.getMinutes() == b.getMinutes();
		boolean segundoiGual = a.getSeconds() == b.getSeconds();
		boolean equals = (anoIgual && mesIgual && diaIgual && horaIgual && minutoIgual && segundoiGual);
		return equals;
	}
	
	/**
	 * Indica se dois objetos do tipo Arquivo s�o iguais
	 * @param a
	 * @param b
	 * @return Boolean que indica se os objetos do tipo Arquivo s�o iguais
	 */
	private boolean equals(Arquivo a, Arquivo b) {
		boolean resp = false;
		
		resp = (a.getNome().equals(b.getNome())) &&
				(a.getTamanho() == b.getTamanho()) &&
				(equals(a.getModificado(),b.getModificado()));
		return resp;
	}
	
	/**
	 * M�todo que sincroniza uma determinada pasta.
	 * @param dir Diret�rio a ser sincronizado.
	 * @param servidorRecente Flag que informa se a pasta � mais recente no servidor.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void sincronize(String dir, boolean servidorRecente) throws IOException, InterruptedException{
		//Caso a url passada n�o seja uma URL do servidor, ele ajusta para o caminho relativo
		if (!dir.substring(0, 1).equals("/")) {
			dir = this.db.getLocalRelativeDir(new File(dir));
		}
		
		Arquivo[] remoteFiles = this.db.getRemoteFiles(dir);
		Arquivo[] localFiles = this.db.getLocalFiles(dir);
		Diretorio[] remoteDirectories = this.db.getRemoteDirectories(dir);
		Diretorio[] localDirectories = this.db.getLocalDirectories(dir);
		
		/**
		 * Essas linhas s�o para resolver o problema da raiz que, quando solicitada a data, ela retorna a hora atual
		 */
		if (dir.equals("/")) { /** IN�CIO DO TRATAMENTO DA RAIZ*/
			Date mostRecentRemoteDir = mostRecent(remoteDirectories, remoteFiles);
			Date mostRecentLocalDir =  new Date(new File(db.getUrl() + dir).lastModified());
			if (mostRecentRemoteDir.getTime() > mostRecentLocalDir.getTime()) 
				servidorRecente = true;
			else
				servidorRecente = false;
		} /**FIM DO TRATAMENTO PARA RAIZ*/
		
		//caso o diret�rio mais recente seja o remoto
		if (servidorRecente) {
			boolean[] localFilesFlag = new boolean[localFiles.length];
			boolean[] localDirFlag = new boolean[localDirectories.length];
			for (int i=0; i<localFilesFlag.length; i++)
				localFilesFlag[i] = false;
			for (int i=0; i<localDirFlag.length; i++)
				localDirFlag[i] = false;
			//resolvendo para os arquivos
			for (int j=0; j<remoteFiles.length; j++){
				boolean baixar = true;
				//verifica se h� necessidade de atualizar
				for (int i=0; i<localFiles.length; i++) {
					Arquivo localFile = localFiles[i];
					if (remoteFiles[j].getNome().equals(localFile.getNome())) {
						localFilesFlag[i] = true;
						if (equals(remoteFiles[j], localFile)) {
							baixar = false;
						}
					}
				}
				if (baixar) {
					this.db.downloadFile(remoteFiles[j].getUrl());
				}
			}
			//apaga os arquivos locais que n�o existem mais no servidor
			for (int i=0; i<localFilesFlag.length; i++) {
				boolean b = localFilesFlag[i];
				if (!b) {
					this.db.deleteLocalFile(localFiles[i].getUrl());					
				}
			}
			
			//resolvendo para os diret�rios
			for (int j=0; j<remoteDirectories.length; j++) {
				Diretorio remoteDir = remoteDirectories[j];
				boolean existe = false;
				int index = -1;
				for (int i=0; i<localDirectories.length; i++) {
					Diretorio localDir = localDirectories[i];
					if (localDir.getNome().equals(remoteDir.getNome())) {
						localDirFlag[i] = true;
						existe = true;
						index = i;
					}
				}
				if (!existe) {
					this.db.downloadAll(remoteDir.getUrl());
				} else {
					boolean remoteIsRecent = serverElemenIsRecent(remoteDir, localDirectories[index]);
					this.sincronize(remoteDir.getUrl(), remoteIsRecent);
				}
			}
			for (int i=0; i<localDirFlag.length; i++) {
				boolean b = localDirFlag[i];
				if (!b){
					this.db.deleteLocalFile(localDirectories[i].getUrl());					
				}
			}
		} //if (servidorRecente)
		
		//caso o diret�rio mais recente seja o local
		else {
			boolean[] remoteFilesFlag = new boolean[remoteFiles.length];
			boolean[] remoteDirFlag = new boolean[remoteDirectories.length];
			for (int i=0; i<remoteFilesFlag.length; i++)
				remoteFilesFlag[i] = false;
			for (int i=0; i<remoteDirFlag.length; i++)
				remoteDirFlag[i] = false;
			//resolvendo para os arquivos
			for (int j=0; j<localFiles.length; j++){
				boolean baixar = true;
				//verifica se h� necessidade de atualizar
				for (int i=0; i<remoteFiles.length; i++) {
					Arquivo remoteFile = remoteFiles[i];
					if (localFiles[j].getNome().equals(remoteFile.getNome())) {
						remoteFilesFlag[i] = true;
						if (equals(localFiles[j], remoteFile)) {
							baixar = false;
						}
					}
				}
				if (baixar) {
					this.db.uploadFile(localFiles[j].getUrl());
				}
			}
			//apaga os arquivos remotos que n�o existem mais no cliente
			for (int i=0; i<remoteFilesFlag.length; i++) {
				boolean b = remoteFilesFlag[i];
				if (!b) {
					this.db.delRemoteFile(remoteFiles[i].getUrl());
				}
			}
			
			//resolvendo para os diret�rios
			for (int j=0; j<localDirectories.length; j++) {
				Diretorio localDir = localDirectories[j];
				boolean existe = false;
				int index = -1;
				for (int i=0; i<remoteDirectories.length; i++) {
					Diretorio remoteDir = remoteDirectories[i];
					if (remoteDir.getNome().equals(localDir.getNome())) {
						remoteDirFlag[i] = true;
						existe = true;
						index = i;
					}
				}
				if (!existe) {
					this.db.createRemoteFolder(localDir.getUrl());
					this.db.uploadAll(this.db.getUrl() + localDir.getUrl());
				} else {
					boolean remoteIsRecent = serverElemenIsRecent(remoteDirectories[index], localDir);
					this.sincronize(localDir.getUrl(), remoteIsRecent);
				}
			}
			for (int i=0; i<remoteDirFlag.length; i++) {
				boolean b = remoteDirFlag[i];
				if (!b){
					this.db.deleteRemoteFolder(remoteDirectories[i].getUrl()); 					
				}
			}
		}
	}
	
	/**
	 * Indica se um Elemento do servidor � mais recente do que um Elemento local
	 * @param remoteFile Elemento remoto em quest�o
	 * @param localFile Elemento local em quest�o
	 * @return Boolean que indica se o elemento remoto � o mais recente
	 */
	private boolean serverElemenIsRecent (Elemento remoteFile, Elemento localFile) {
		Date remoteCal = remoteFile.getModificado();
		Date localCal = localFile.getModificado();
		return (remoteCal.compareTo(localCal) <= 0)? false : true; 
	}
	
	private boolean serverElemenIsRecent (Date remoteCal, Date localCal) {
		return (remoteCal.compareTo(localCal) <= 0)? false : true;
	}
	
	/**
	 * Indica qual a Data de Modifica��o de um Elemento mais recente de duas listas de Elementos
	 * @param list1
	 * @param list2
	 * @return Data de Modifica��o mais recente
	 */
	private Date mostRecent (Elemento[] list1, Elemento[] list2) {
			Date resp = new Date();
			resp.setTime(0);
			if (!(list1 == null)){
				for (Elemento ele : list1){
					if (serverElemenIsRecent(ele.getModificado(), resp)) 
						resp = ele.getModificado();  
				}
			}
			if (!(list2 == null)){
				for (Elemento ele : list2){
					if (serverElemenIsRecent(ele.getModificado(), resp)) 
						resp = ele.getModificado();  
					}
			}
			return resp;

	}
}
