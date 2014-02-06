package api;

import entity.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

public class FileTransferAPI {
	private InputStream isControle;
	private OutputStream osControle;
	private Socket socket;
	
	private String getControlAnswer(boolean print) throws IOException{
		InputStream is = this.socket.getInputStream();
		String resp;
		do{
			byte[] b = new byte[1000];
			is.read(b);
			resp = new String(b).trim();
			if (print)
				System.out.println(resp);
		} while (is.available() != 0);
		return resp;
	}
	
	/**
	 * Conecta a API a um determinado servidor
	 * @param server String do servidor FTP a ser conectado
	 * @param port	Porta do servidor FTP a ser conectado
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void connect(String server, int port) throws UnknownHostException, IOException{		
		this.socket = new Socket(server, port);	
		InputStream is = this.socket.getInputStream();
		String resp;
		do{
			byte[] b = new byte[1000];
			is.read(b);
			resp = new String(b).trim();
			System.out.println(resp);
		} while (is.available() != 0);
	}
	
	/**
	 * Disconecta a API do servidor atualmente conectado
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void disconnect() throws IOException, InterruptedException{
		query("QUIT r\n", this.isControle, this.osControle, true);
		getControlAnswer(true);
		this.isControle.close();
		this.osControle.close();
		this.socket.close();
	}
	
	/**
	 * Autentica no servidor atualmente conectado utilizando usuário e senha planos 
	 * @param user Nome de usuário plano
	 * @param password	Senha plana
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void login(String user, String password) throws UnknownHostException, IOException, InterruptedException{
		this.isControle = this.socket.getInputStream();
		this.osControle = this.socket.getOutputStream();
		query("USER " + user + "\r\n", this.isControle, this.osControle, true);
		query("PASS " + password + "\r\n", this.isControle, this.osControle, true);																				// Erros	
	}
	
	/**
	 * Executa uma determinada query no formato dos comandos FTP, podendo imprimir o resultado da query ou não
	 * @param s	Query a ser executada
	 * @param is Canal de entrada de dados a ser utilizado na execução da query
	 * @param os Canal de saída de dados a ser utilizado na execução da query
	 * @param print Flag que indica se o retorno da query deve ser impresso
	 * @return	Retorna a String com o retorno da query
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private String query(String s, InputStream is, OutputStream os, boolean print) throws IOException, InterruptedException{
		os.write(s.getBytes());
		String resp;
		do{
			byte[] b = new byte[10000];
			is.read(b);
			resp = new String(b).trim();
			if (print)
				System.out.println(resp);
		} while (is.available() != 0);
		return resp;
	}
	
	/**
	 * Baixa um arquivo remoto num diretório local especificado
	 * @param url URL do arquivo remoto a ser baixado
	 * @param destiny Caminho local onde o arquivo deve ser salvo
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected void downloadFile (String url, String destiny) throws IOException, InterruptedException{
		Socket socketDados = pasv();
		InputStream isDados = socketDados.getInputStream();
		query("RETR " + url + "\r\n", this.isControle, this.osControle, true);							
		FileOutputStream fout = new FileOutputStream(destiny + url);
		int ch;
		while ((ch = isDados.read()) != -1)		{
		   fout.write(ch);
		}
		isDados.close();
		fout.close();		
		socketDados.close();
		getControlAnswer(true);
		
		Date date = getRemoteFileDate(getDirectoryNameFromUrl(url), getFilenameFromUrl(url));
		File f = new File (destiny+url);
		f.setLastModified(date.getTime());
	}
	
	/**
	 * Retorna o nome de um arquivo dada a URL do mesmo
	 * @param url URL do arquivo 
	 * @return Nome do arquivo com extensão
	 */
	private String getFilenameFromUrl(String url){
		int index1 = url.indexOf("/");
		if (index1 != -1){
			int index2 = -1;
			do{
				index2 = url.indexOf("/");
			}
			while (index1 != index2);
			return url.substring(index1+1);
		}
		else
			return url;
	}
	
	/**
	 * Dada a URL de um arquivo, retorna o caminho do mesmo
	 * @param url URL do arquivo em questão
	 * @return Caminho do arquivoem questão
	 */
	private String getDirectoryNameFromUrl(String url){
		int index1 = url.indexOf("/");
		if (index1 != -1){
			int index2 = -1;
			do{
				index2 = url.indexOf("/");
			}
			while (index1 != index2);
			return url.substring(0,index1);
		}
		else
			return url;
	}
	
	/**
	 * Deleta um arquivo remoto 
	 * @param url URL do arquivo remoto em questão
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected void delRemoteFile (String url) throws IOException, InterruptedException{
		query("DELE " + url + "\r\n", this.isControle, this.osControle, true);
	}
	
	/**
	 * Envia ao servidor remoto um arquivo local
	 * @param urlLocal URL do arquivo local
	 * @param urlRemota URL onde o arquivo vai ser salvo remotamente
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected void uploadFile (String urlLocal, String urlRemota) throws IOException, InterruptedException{
		File f = new File(urlLocal);																		// Cria um arquivo
		long size = f.length();																		
		byte[] data = new byte[(int) size];															// Inicializa um array com o tamanho do arquivo
		FileInputStream fin = new FileInputStream(urlLocal);
		fin.read(data);	
		fin.close();
		
		Socket socketDados = pasv();
		socketDados.setTcpNoDelay(true);
		OutputStream osDados = socketDados.getOutputStream();
		query("STOR " + urlRemota + "\r\n", this.isControle, osControle, true);									// Comando STORE para enviar um arquivo para o servidor FTP		
		osDados.write(data);																		// Envia o array de bytes que representa o arquivo	       

		osDados.close();
		socketDados.close();																		// Fecha o Stream
		getControlAnswer(true);
		
		long lastModified = f.lastModified();
		Date date = new Date(lastModified);
		changeRemoteElementDate(urlRemota, date);
	}
	
	/**
	 * Executa o comando FTP PASV(Passive Mode) do servidor atualmente conectado à API
	 * @return Um Socket TCP para troca de dados
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private Socket pasv() throws IOException, InterruptedException{
		String resp = query("PASV\r\n", this.isControle, this.osControle, true); 							// Passive mode    		-Djava.net.preferIPv4Stack=true				
		int port = getPort(resp);																	// Pega a porta do PASV
		String IP = getIP(resp);																	// Pega o IP do PASV
		query("TYPE I\r\n", this.isControle, this.osControle, true);										// Muda o tipo da transferência para binário
		return new Socket(IP,port);
	}
	
	/**
	 * Trata a String que é retornada do comando PASV e retorna a porta do Modo Passivo
	 * @param msg Mensagem retorno do comando PASV que contém IP e porta
	 * @return Porta do modo passivo
	 */
	private int getPort(String msg){
		String s1[] = msg.split(","); 
		int p1 = Integer.parseInt(s1[4]);
		int p2 = Integer.parseInt(s1[5].replaceAll("\\)\\.",""));
		int port = p1*256 + p2;
		return port;
	}
	
	/**
	 * Trata a String que é retornada do comando PASV e retorna o IP do Modo Passivo
	 * @param msg Mensagem retorno do comando PASV que contém IP e porta
	 * @return
	 */
	private String getIP(String msg){		
		String s1[] = msg.split("\\(");  
		String s2[] = s1[1].split(",");
		String ip = s2[0] + "." + s2[1] + "." + s2[2] + "." + s2[3];
		return ip;
	}
	
	/**
	 * Lista os arquivos remotos de um determinado diretório (arquivos de subpastas não são listados) por meio do comando FTP LIST
	 * @param diretorio Diretório qual queremos listas arquivos
	 * @return Array de Arquivos do diretório especificado
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected Arquivo[] getRemoteFiles(String diretorio) throws IOException, InterruptedException {
		Socket socketDados = pasv();	
		InputStream isDados = socketDados.getInputStream();
		String listagem = query("LIST " + diretorio + "\r\n", isDados, this.osControle, true);
		getControlAnswer(true);
		socketDados.close();
		getControlAnswer(true);
		isDados.close();
		
		if (listagem.equals("")){
			return new Arquivo[0];
		} 
		else {
			String[] lines = listagem.split(System.getProperty("line.separator"));			
			Arquivo[] elementos = new Arquivo[lines.length];
			int contadorArquivos = 0;
			for (int i = 0; i < lines.length; i++){
				String nome = getName(lines[i]);
				if (!lines[i].substring(0, 1).equals("d")){
					int tamanho = getSize(lines[i]);
					Date modificado = getRemoteFileDate(diretorio, nome);
					Arquivo ele = new Arquivo(diretorio,nome,modificado,tamanho);
					elementos[contadorArquivos] = ele;
					System.out.println(ele.toString());
					contadorArquivos++;
				}
			}
			if (contadorArquivos > 0){
				Arquivo[] files = new Arquivo[contadorArquivos];
				for (int i=0; i < contadorArquivos; i++) {
					files[i] = elementos[i];
				}
				return files;
			}
			return new Arquivo[0];
		}
	}
	
	/**
	 * Lista os diretórios remotos de um determinado diretório por meio do comando FTP LIST
	 * @param diretorio Diretório qual queremos listas subdiretórios
	 * @return Array de Diretórios do diretório especificado
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected Diretorio[] getRemoteDirectories(String diretorio) throws IOException, InterruptedException {
		Socket socketDados = pasv();
		InputStream isDados = socketDados.getInputStream();
		String listagem = query("LIST " + diretorio + "\r\n", isDados, this.osControle, true);
		getControlAnswer(true);
		socketDados.close();
		getControlAnswer(true);
		isDados.close();
		
		if (!listagem.equals("")){
			String[] lines = listagem.split(System.getProperty("line.separator"));			
			Diretorio[] elementos = new Diretorio[lines.length];
			int contadorPastas = 0;
			for (int i = 0; i < lines.length; i++){
				String nome = getName(lines[i]);
				if (lines[i].substring(0, 1).equals("d")){
					Diretorio ele = new Diretorio(diretorio,nome);
					ele.setModificado(getRemoteDirectoryDate(nome));
					elementos[contadorPastas] = ele;
					System.out.println(ele.toString());
					contadorPastas ++;
				}
			}		
			if (contadorPastas > 0){
				Diretorio[] pastas = new Diretorio[contadorPastas];
				for (int i=0; i < contadorPastas; i++) {
					pastas[i] = elementos[i];
				}
				return pastas;
			}
		}
		return new Diretorio[0];
	}
	
	/**
	 * Encontra o tamanho de um arquivo dado uma linha de resposta do comando FTP LIST
	 * @param linha Linha do comando FTP LIST
	 * @return Tamanho do arquivo
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private int getSize(String linha) throws IOException, InterruptedException {
		int xpgindex = linha.indexOf(" xpg ");
		return Integer.parseInt(linha.substring(xpgindex+4,xpgindex+19).trim());
	}

	/**
	 * Encontra o nome de um arquivo dado uma linha de resposta do comando FTP LIST
	 * @param linha Linha de resposta do comando FTP LIST
	 * @return Nome do arquivo
	 */
	private String getName (String linha){
		int xpgindex = linha.indexOf(" xpg ");
		return linha.substring(xpgindex+32).replace("\n", "");
	}
	
	/**
	 * Cria um diretporio remoto
	 * @param url URL do diretporio remoto que deve ser criado
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected void createRemoteFolder (String url) throws IOException, InterruptedException{
		query("MKD " + url + "\r\n", isControle, osControle, true);
	}
	
	/**
	 * Deleta um diretporio remoto
	 * @param url URL do diretório remoto que deve ser deletado
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected void deleteRemoteFolder (String url) throws IOException, InterruptedException {
		Diretorio[] diretorios = getRemoteDirectories(url);
		if (diretorios != null){
			for (Diretorio d: diretorios)
				deleteRemoteFolder(d.getUrl());
		}
		Arquivo[] arquivos = getRemoteFiles(url);
		if (arquivos != null){
			for (Arquivo a : arquivos){
				delRemoteFile(a.getUrl());
			}
		}
		query("RMD " + url + "\r\n", this.isControle, this.osControle, true);
	}
	
	/**
	 * Muda a data de modificação de um Elemento remoto através do comando FTP MFMT
	 * @param url URL do Elemento
	 * @param date Data que deve ser salva
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("deprecation")
	private void changeRemoteElementDate (String url, Date date) throws IOException, InterruptedException{
		String ano = String.valueOf(date.getYear() + 1900);
		String mes = fixDate(date.getMonth() + 1);
		String dia = fixDate(date.getDate());
		String hora = fixDate(date.getHours() + 3);
		String minuto = fixDate(date.getMinutes());
		String segundo = fixDate(date.getSeconds());
		query ("MFMT " + ano + mes + dia + hora + minuto + segundo + " " + url + "\r\n", this.isControle, this.osControle, false);
	}
	
	/**
	 * Encontra a data de modificação de um arquivo remoto através do comando MDTM
	 * @param diretorio Diretório do arquivo em questão
	 * @param nome Nome do arquivo em questão
	 * @return Data de modificação do arquivo em questão
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("deprecation")
	private Date getRemoteFileDate(String diretorio, String nome) throws IOException, InterruptedException{
		String url;
		if (!diretorio.equals(""))
			url = diretorio + "/" + nome;
		else
			url = nome;
		String data = query("MDTM " + url + "\r\n", this.isControle, this.osControle, false);
		int ano = Integer.parseInt(data.substring(4, 8))-1900;
		int mes = Integer.parseInt(data.substring(8, 10))-1;
		int dia = Integer.parseInt(data.substring(10, 12));
		int hora = Integer.parseInt(data.substring(12, 14))-3;
		int minuto = Integer.parseInt(data.substring(14, 16));
		int segundo = Integer.parseInt(data.substring(16, 18));
		Date date = new Date(ano, mes, dia, hora, minuto, segundo);
		return date;
	}
	
	/**
	 * Faz tratamento de complementação de dias e meses para sempre terem 2 dígitos
	 * @param i Dia/Mês
	 * @return Dia/Mês com 2 dígitos
	 */
	private String fixDate(int i){
		String s =  String.valueOf(i);
		if (s.length() == 1)
			s = "0" + s;
		return s;		
	}
	
	/**
	 * Encontra a data de modificação de um diretório remoto através do comando MDTM
	 * @param url URL do diretório em questão
	 * @return Data de modificação do diretório em questão
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("deprecation")
	private Date getRemoteDirectoryDate(String url) throws IOException, InterruptedException{
		Socket socketDados = pasv();
		InputStream isDados = socketDados.getInputStream();
		query("CWD " + url + "\r\n", this.isControle, this.osControle, false);
		query("CDUP\r\n", this.isControle, this.osControle, true);	
		String upDirec = getCurrentDirectory();
		String listagem = query("MLSD " + upDirec + "\r\n", isDados, this.osControle, false);
		getControlAnswer(false);
		socketDados.close();
		getControlAnswer(false);
		
		Date date = new Date();
		String[] linhas = listagem.split("\n");
		String nome = url.replace(upDirec, "");
		nome = nome.replace("/","");
		for (String linha : linhas){
			int index = linha.indexOf(nome);		
			if (index != -1){
				String p = linha.substring(index).replace("\r","");
				if (p.equals(nome)){
					int ano = Integer.parseInt(linha.substring(7, 11));
					int mes = Integer.parseInt(linha.substring(11, 13));
					int dia = Integer.parseInt(linha.substring(13, 15));
					int hora = Integer.parseInt(linha.substring(15, 17));
					int minuto = Integer.parseInt(linha.substring(17, 19));
					int segundo = Integer.parseInt(linha.substring(19, 21));
					date.setYear(ano-1900);
					date.setMonth(mes-1);
					date.setDate(dia);
					date.setHours(hora-3);
					date.setMinutes(minuto);
					date.setSeconds(segundo);	
					return date;
				}
			}
		}
		return date;
	}
	
	/**
	 * Encontra o diretório atual da API através do comando FTP PWD
	 * @return String com a URL do diretório atual
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private String getCurrentDirectory() throws IOException, InterruptedException{
		String up = query("PWD\r\n", this.isControle, this.osControle, true);
		int i = up.indexOf("\"");
		int j = up.indexOf("\"", i+1);
		return  up.substring(i+1, j);
	}

}
