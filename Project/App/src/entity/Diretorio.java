package entity;

public class Diretorio extends Elemento {
	
	public Diretorio (String caminho, String nome){
		this.caminho = caminho;
		this.name = nome;
		this.url = (caminho.substring(caminho.length()-1).equals("/"))? caminho + nome + "/" : caminho + "/" + nome + "/";
	}
	
	public String getUrl () {
		return this.url;
	}
	
	public String toString(){
		return ("Nome: " + this.name + "  " + " Caminho: " + this.caminho + " URL: " + this.url);
	}
}
