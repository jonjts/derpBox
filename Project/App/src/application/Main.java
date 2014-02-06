package application;

import api.*;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class Main {

    public static void main(String[] args) throws Exception {

        String path;
        JFileChooser pathChosser = new JFileChooser();
        pathChosser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (pathChosser.showOpenDialog(pathChosser) == JFileChooser.APPROVE_OPTION) {
            path = pathChosser.getSelectedFile().getPath();
            Derpbox db = new Derpbox("ftp.xpg.com.br", 21, "vinigerhardt", "vinicius210789", path, "Derpbox");

            Sync sync = new Sync(db);
            int i = 0;
            while (true) {
                sync.sincronize("/", false);
                System.out.println("-------- Ciclo de Sincronização " + i + " Terminou --------");
                i++;
                Thread.sleep(20000);					// Sincroniza a cada 10 segundos
            }
            // Exclusoes de arquivos (ou pastas �nicas) na raiz do servidor nao s�o replicadas no cliente, pois nao temos como saber a modificacao da raiz do servidor a n�o ser concultando a data de modifica��o dos seus arquivos e diretporios
        } else {
            JOptionPane.showConfirmDialog(null, "Aplicação Encerrada!");
        }
    }
}
