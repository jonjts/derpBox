package application;

import api.*;
import java.io.File;
import java.util.Scanner;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class Main {

    public static void main(String[] args) throws Exception {
        //ler aquivo txt
        File file = new File("entradas.txt");
        Scanner scanner = new Scanner(file);
        String url = scanner.nextLine();
        Integer porta = scanner.nextInt();
        Integer timer = scanner.nextInt();
        scanner.nextLine();
        String user = scanner.nextLine();
        String senha = scanner.nextLine();
        String pathLocal = scanner.nextLine();
        String pathCloud = scanner.nextLine();

        //inicializa o objeto com os parametros do txt
        Derpbox db = new Derpbox(url, porta, user, senha, pathLocal, pathCloud);
        Sync sync = new Sync(db);
        int i = 0;
        //inicia o loop de sincronizacao
        while (true) {
            sync.sincronize("/", false);
            System.out.println("-------- Ciclo de Sincronização " + i + " Terminou --------");
            i++;
            Thread.sleep(timer * 1000);					// Sincroniza a cada 10 segundos
        }
            // Exclusoes de arquivos (ou pastas �nicas) na raiz do servidor nao s�o replicadas no cliente, pois nao temos como saber a modificacao da raiz do servidor a n�o ser concultando a data de modifica��o dos seus arquivos e diretporios

    }
}
