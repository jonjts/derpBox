package entity;

import java.util.Date;

public class Elemento {
	protected String caminho;
	protected String url;
	protected String name;
	protected Date dataModificacao;
	
	public String getUrl() {
		return this.url;
	}
	
	public String getNome() {
		return this.name;
	}
	
	public String getCaminho() {
		return this.caminho;
	}
	
	public Date getModificado() {
		return dataModificacao;
	}

	public void setModificado(Date modificado) {
		this.dataModificacao = modificado;
	}
	
}
