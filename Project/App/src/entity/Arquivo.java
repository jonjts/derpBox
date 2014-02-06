package entity;

import java.util.Date;

public class Arquivo extends Elemento {
	private long size;
	
	public Arquivo (String caminho, String nome, Date modificado, long tamanho){
		this.caminho = caminho;
		this.name = nome;
		this.url = (caminho.substring(caminho.length()-1).equals("/"))? caminho + nome + "/" : caminho + "/" + nome + "/";
		this.dataModificacao = modificado;
		this.size = tamanho;
	}
	
	public long getTamanho() {
		return this.size;
	}

	public void setTamanho(int tamanho) {
		this.size = tamanho;
	}

	public String toString(){
		return ("Nome: " + name + " Tamanho: " + size + " Ultima modificão: " + this.dataModificacao + " URL: " + url);
	}
}
