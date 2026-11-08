package simuladores;

import simuladores.avl.AVLSimulator;
import simuladores.bst.BSTSimulator;
import simuladores.rbt.RBTSimulator;

/**
 * Lançador unificado dos três simuladores de árvores binárias.
 * Abre as três janelas de forma independente e dispostas na tela:
 *   1. Árvore Binária de Busca (BST)
 *   2. Árvore AVL
 *   3. Árvore Rubro-Negra (Red-Black Tree)
 *
 * Cada janela executa em sua própria thread de engine e possui
 * seus próprios controles ImGUI para put (inserir) e delete (remover).
 *
 * @author Simulador de Árvores
 */
public class Launcher {

    /**
     * Ponto de entrada: inicia os três simuladores em janelas independentes.
     *
     * @param args argumentos da linha de comando (ignorados)
     */
    public static void main(String[] args) {

        // Inicia Árvore Binária de Busca (BST)
        new Thread(() -> {
            new BSTSimulator(30, 40);
        }, "Thread-BST").start();

        try {
            Thread.sleep(250);
        } catch (InterruptedException ignored) {}

        // Inicia Árvore AVL
        new Thread(() -> {
            new AVLSimulator(120, 90);
        }, "Thread-AVL").start();

        try {
            Thread.sleep(250);
        } catch (InterruptedException ignored) {}

        // Inicia Árvore Rubro-Negra (RBT)
        new Thread(() -> {
            new RBTSimulator(210, 140);
        }, "Thread-RBT").start();
    }
}
