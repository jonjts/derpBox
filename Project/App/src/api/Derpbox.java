package api;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Date;

import entity.*;

/**
 * Classe Derpbox
 * @author Avner
 * @extend FileTransferAPI
 */
public class Derpbox extends FileTransferAPI {
	private String server;
	private int port;
	private String user;
	private String password;
	private String caminho;
	private String nome;
	private String url;
	
	public Derpbox(String server, int port, String user, String password, String caminho, String nome) throws UnknownHostException, IOException, InterruptedException {
		this.server = server;
		this.port = port;
		this.user = user;
		this.password = password;
		this.nome = nome;
		this.setCaminho(caminho);
		
		this.connect(server, port);
		this.login(user, password);
		
		if (!this.createLocalFolder("/")) this.downloadAll("/");
	}
	
	/**
	 * Seta o caminho da pasta do Derpbox.
	 * Se o caminho passado não terminar com uma '/', adiciona.
	 * Após a execução, atualiza a URL.
	 * @param diretorio a ser utilizado.
	 */
	private void setCaminho(String diretorio) {
		this.caminho = (diretorio.substring(diretorio.length() - 1).equals("/"))? diretorio : diretorio + "/";
		this.updateUrl();
	}
	
	/**
	 * Atualiza a URL da pasta utilizada.
	 */
	private void updateUrl() {
		if (this.caminho != null && this.nome != null) {
			String newurl = "";
			newurl = this.caminho + this.nome;
			this.url = (newurl.substring(newurl.length() - 1).equals("/"))? newurl : newurl + "/";
		}
	}
	
	public String getUrl() {
		return this.url;
	}
		
	/**
	 * Reconecta
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void reconect() throws UnknownHostException, IOException, InterruptedException {
		this.connect(this.server, this.port);
		this.login(this.user, this.password);
	}
	
	/**
	 * Retorna as URL's de todos os arquivos num determinado diretório do servidor.
	 * @param diretorio que terá os arquivos listados.
	 * @return String[] com os nomes dos arquivos.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private String[] getRemoteArquivoUrls (String diretorio) throws IOException, InterruptedException{
		Arquivo[] files = this.getRemoteFiles(diretorio);
		if (files != null){
			int tamanho = files.length;
			String[] urls = new String[tamanho];
			for (int i=0; i<tamanho; i++) {
				urls[i] = files[i].getUrl();
			}		
			return urls;
		}
		return null;
	}
	
	/**
	 * Retorna as URL's das subpastas de um diretório no servidor.
	 * @param diretorio
	 * @return String[] com as URL's das pastas no diretório especificado.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private String[] getRemoteDiretorioUrls (String diretorio) throws IOException, InterruptedException{
		Diretorio[] dir = this.getRemoteDirectories(diretorio);
		if (dir != null){
			int tamanho = dir.length;
			String[] urls = new String[tamanho];
			for (int i=0; i<tamanho; i++) {
				urls[i] = dir[i].getUrl();
			}
			return urls;
		}
		else
			return null;
	}
	
	/**
	 * Cria a pasta local utilizando a Url do Derpbox.
	 * Caso a pasta local ainda não exista, baixa tudo.
	 * @return True se a pasta já existia antes, False caso contrário.
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private boolean createLocalFolder (String diretorio) throws IOException, InterruptedException {
		String caminho = (diretorio == "/")? this.url : this.url + diretorio;
		File f = new File(caminho);
		if (!f.exists()){
			f.mkdir();
			return false;
		}
		return true;
	}
	
	/**
	 * Chama o método downloadFile da FileTransferAPI com o caminho de destino.
	 * @param urlArquivo URL do arquivo a ser baixado.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void downloadFile (String urlArquivo) throws IOException, InterruptedException {
		this.downloadFile(urlArquivo, this.url);
	}
	
	/**
	 * Envia um arquivo local para o servidor remoto no diretório equivalente ao local
	 * @param urlLocal URL do arquivo local
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void uploadFile (String urlLocal) throws IOException, InterruptedException{
		int index = url.indexOf(this.nome) + this.nome.length();
		String urlRemota = urlLocal.substring(index).replace("\\","/");
		this.uploadFile(urlLocal, urlRemota);
	}
	
	
	
	/**
	 * Baixa todos os arquivos de um determinado diretório.
	 * @param diretorio que terá os arquivos baixados.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void downloadAll(String diretorio) throws IOException, InterruptedException {
		this.reconect();
		this.createLocalFolder(diretorio);
		String[] fileUrls = this.getRemoteArquivoUrls(diretorio);
		
		// Baixa os arquivos do diretório escolhido
		if (fileUrls != null){
			for (int i=0; i<fileUrls.length; i++){
				System.out.println("\n... Fazendo download de " + fileUrls[i] + " ...");
				this.downloadFile(fileUrls[i]);
			}
		}
		
		// Para cada pasta dentro do diretório escolhido, baixa tudo
		String[] dirUrls = this.getRemoteDiretorioUrls(diretorio);
		if (dirUrls != null){
			for (int i=0; i<dirUrls.length; i++){
				this.downloadAll(dirUrls[i]);
			}	
		}
	}
	
	/**
	 * Upa todos os arquivos presentes na pasta local.
	 * @param diretorio
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void uploadAll(String diretorio) throws IOException, InterruptedException {
		File raiz = new File(diretorio);
		System.out.println("Nome: " + raiz.getName());
		File[] filhos = raiz.listFiles();
		
		int contadorPastas = 0;
		int[] indexPastas = (filhos != null) ? new int[filhos.length] : new int[0];
		
		for (int i=0; i<indexPastas.length; i++) {
			if (filhos[i].isDirectory()) {
				indexPastas[contadorPastas] = i;
				contadorPastas++;
			} else {
				System.out.println("... Fazendo upload de ... " + filhos[i]);
				this.uploadFile(filhos[i].toString(), this.getLocalRelativeDir(filhos[i]));
			}
		}
		//aplica recursivamente para os diretórios encontrados
		for (int i=0; i<contadorPastas; i++) {
			String relativeDir = getLocalRelativeDir(filhos[indexPastas[i]]);
			this.createRemoteFolder(relativeDir);
			this.uploadAll(filhos[indexPastas[i]].getPath());
		}
	}
	
	
	public String getLocalRelativeDir (File f) {
		String resp = f.getPath().replace("\\", "/");
		resp = resp.substring(this.url.length()-1); 
		return resp;
	}
	
	/**
	 * Lista os Arquivos locais de um determinado diretório
	 * @param diretorio URL do diretório em questão
	 * @return Array de Arquivos do diretório em questão
	 */
	public Arquivo[] getLocalFiles (String diretorio) {
		File dir = new File(this.url + diretorio);
		if (dir.exists() && dir.isDirectory()) {
			File[] filhos = dir.listFiles();
			int contadorArquivos = 0;
			File[] temp = new File[filhos.length];
			for (File f : filhos) {
				if (f.isFile()) {
					temp[contadorArquivos] = f;
					contadorArquivos++;
				}
			}
			Arquivo[] arquivos = new Arquivo[contadorArquivos];
			for (int i=0; i<contadorArquivos; i++) {
				Date c = new Date(temp[i].lastModified());
				arquivos[i] = new Arquivo(temp[i].getParent(),temp[i].getName(),c,temp[i].length());
			}
			return arquivos;
		} else return new Arquivo[0];
	}
	
	/**
	 * Lista os Diretórios locais de um determinado diretório
	 * @param diretorio URL do diretório em questão
	 * @return Array de Diretórios do diretório em questão
	 */
	public Diretorio[] getLocalDirectories (String diretorio) {
		File dir = new File (this.url + diretorio);
		if (dir.exists() && dir.isDirectory()) {
			File[] filhos = dir.listFiles();
			int contadorPastas = 0;
			File[] temp = new File[filhos.length];
			for (File f : filhos) {
				if (f.isDirectory()) {
					temp[contadorPastas] = f;
					contadorPastas++;
				}
			}
			Diretorio[] pastas = new Diretorio[contadorPastas];
			for (int i=0; i<contadorPastas; i++) {
				Date date = new Date(temp[i].lastModified());
				pastas[i] = new Diretorio(diretorio, temp[i].getName());
				pastas[i].setModificado(date);
			}
			return pastas;
		} else return new Diretorio[0];
	}
	
	
	
	/**
	 * Método que deleta arquivos e pastas locais recursivamente.
	 * Este método foi criado para resolver o problema do File.delete() que não funciona com pastas que contém arquivos.
	 * @param url do arquivo a ser deletado.
	 * @return True caso a deleção ocorra com sucesso, False caso contrário.
	 */
	public boolean deleteLocalFile (String url) {
		url = (url.substring(0,1).equals("/"))? this.getUrl() + url : url;
		File f = new File(url);
		if (f.exists()){
			if (f.isDirectory()) {
				if (f.delete()) return true;
				else {
					File[] filhos = f.listFiles();
					for (File filho : filhos)
						deleteLocalFile(filho.getPath());
					return f.delete();
				}
			} else {
				f.delete();
				return true;
			}
		} else return true;
	}	
	
}