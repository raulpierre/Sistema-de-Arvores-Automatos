package simuladores;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.imgui.GuiButton;
import java.awt.Color;
import java.awt.event.WindowEvent;
import javax.swing.SwingUtilities;
import simuladores.avl.AVLSimulator;
import simuladores.bst.BSTSimulator;
import simuladores.rbt.RBTSimulator;

/**
 * Menu Principal e gerenciador de abas dos simuladores de árvores binárias.
 * Permite selecionar qual das 3 abas será aberta e utilizada como principal:
 *   1. Árvore Binária de Busca (BST)
 *   2. Árvore AVL (Auto-balanceada)
 *   3. Árvore Rubro-Negra (Red-Black Tree)
 *
 * Também permite alternar diretamente entre as abas a partir do cabeçalho
 * de qualquer simulador ou retornar a este menu principal.
 *
 * @author Simulador de Árvores
 */
public class Launcher extends EngineFrame {

    // =========================================================================
    // Identificadores das Abas
    // =========================================================================
    public static final int TAB_MENU = 0;
    public static final int TAB_BST  = 1;
    public static final int TAB_AVL  = 2;
    public static final int TAB_RBT  = 3;

    // =========================================================================
    // Paleta de Cores do Menu
    // =========================================================================
    private static final Color C_BG         = new Color(235, 240, 248);
    private static final Color C_HDR        = new Color(22, 32, 58);
    private static final Color C_HDR_SUB    = new Color(180, 200, 240);
    private static final Color C_CARD_BG    = new Color(255, 255, 255);
    private static final Color C_CARD_HOVER = new Color(248, 251, 255);

    // Cores temáticas de cada aba
    private static final Color C_BST_MAIN   = new Color(25, 55, 140);
    private static final Color C_BST_NODE   = new Color(30, 120, 220);
    private static final Color C_BST_LIGHT  = new Color(230, 240, 255);

    private static final Color C_AVL_MAIN   = new Color(15, 75, 40);
    private static final Color C_AVL_NODE   = new Color(20, 140, 80);
    private static final Color C_AVL_LIGHT  = new Color(230, 248, 235);

    private static final Color C_RBT_MAIN   = new Color(95, 15, 15);
    private static final Color C_RBT_RED    = new Color(220, 40, 40);
    private static final Color C_RBT_BLACK  = new Color(35, 35, 35);
    private static final Color C_RBT_LIGHT  = new Color(252, 235, 235);

    private static final Color C_TAB_ACTIVE = new Color(255, 215, 0);

    // =========================================================================
    // Botões ImGUI
    // =========================================================================
    private GuiButton btnTopMenu;
    private GuiButton btnTopBST;
    private GuiButton btnTopAVL;
    private GuiButton btnTopRBT;

    private GuiButton btnSelectBST;
    private GuiButton btnSelectAVL;
    private GuiButton btnSelectRBT;
    private GuiButton btnOpenAll;

    private boolean switching;

    // Coordenadas dos 3 cartões de seleção
    private static final int CARD_Y = 130;
    private static final int CARD_W = 296;
    private static final int CARD_H = 450;
    private static final int CARD_1_X = 26;
    private static final int CARD_2_X = 352;
    private static final int CARD_3_X = 678;

    // =========================================================================
    // Construtores
    // =========================================================================
    public Launcher() {
        this(-1, -1);
    }

    public Launcher(int posX, int posY) {
        super(
            1000, 680,
            "Sistema de Árvores Binárias - Menu Principal",
            60, true, true, false, false, false, false
        );
        if (posX >= 0 && posY >= 0) {
            setLocation(posX, posY);
        }
    }

    // =========================================================================
    // Ciclo de Vida da Engine
    // =========================================================================
    @Override
    public void create() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        switching = false;

        // Barra de abas no topo direito (folga ampla para o texto monoespaçado)
        int tx = 490;
        int ty = 10;
        btnTopMenu = new GuiButton(tx, ty, 90, 26, "● Menu", this);
        btnTopBST  = new GuiButton(tx + 96, ty, 95, 26, "1. BST", this);
        btnTopAVL  = new GuiButton(tx + 197, ty, 95, 26, "2. AVL", this);
        btnTopRBT  = new GuiButton(tx + 298, ty, 110, 26, "3. RBT", this);

        styleTopButton(btnTopMenu, true, C_HDR);
        styleTopButton(btnTopBST, false, C_BST_MAIN);
        styleTopButton(btnTopAVL, false, C_AVL_MAIN);
        styleTopButton(btnTopRBT, false, C_RBT_MAIN);

        // Botões principais de seleção em cada cartão
        int btnY = CARD_Y + CARD_H - 46;
        btnSelectBST = new GuiButton(CARD_1_X + 20, btnY, CARD_W - 40, 32, "Selecionar Aba BST", this);
        btnSelectAVL = new GuiButton(CARD_2_X + 20, btnY, CARD_W - 40, 32, "Selecionar Aba AVL", this);
        btnSelectRBT = new GuiButton(CARD_3_X + 20, btnY, CARD_W - 40, 32, "Selecionar Aba RBT", this);

        styleActionButton(btnSelectBST, C_BST_MAIN);
        styleActionButton(btnSelectAVL, C_AVL_MAIN);
        styleActionButton(btnSelectRBT, C_RBT_MAIN);

        // Botão secundário no rodapé para abrir todas caso o usuário deseje comparar
        btnOpenAll = new GuiButton(705, 615, 255, 32, "Abrir as 3 Simultaneamente", this);
    }

    private void styleTopButton(GuiButton btn, boolean active, Color themeColor) {
        if (active) {
            btn.setBackgroundColor(C_TAB_ACTIVE);
            btn.setBorderColor(new Color(180, 140, 0));
            btn.setTextColor(new Color(20, 20, 20));
        } else {
            btn.setBackgroundColor(themeColor);
            btn.setBorderColor(new Color(200, 215, 240));
            btn.setTextColor(WHITE);
        }
    }

    private void styleActionButton(GuiButton btn, Color mainColor) {
        btn.setBackgroundColor(mainColor);
        btn.setBorderColor(mainColor.darker());
        btn.setTextColor(WHITE);
    }

    @Override
    public void update(double delta) {
        if (switching) return;

        btnTopMenu.update(delta);
        btnTopBST.update(delta);
        btnTopAVL.update(delta);
        btnTopRBT.update(delta);

        btnSelectBST.update(delta);
        btnSelectAVL.update(delta);
        btnSelectRBT.update(delta);
        btnOpenAll.update(delta);

        // Seleção por botão ou tecla de atalho (1, 2, 3)
        if (btnSelectBST.isMousePressed() || btnTopBST.isMousePressed()
                || isKeyPressed(KEY_ONE) || isKeyPressed(KEY_KP_1)) {
            switchTab(this, TAB_BST);
            return;
        }
        if (btnSelectAVL.isMousePressed() || btnTopAVL.isMousePressed()
                || isKeyPressed(KEY_TWO) || isKeyPressed(KEY_KP_2)) {
            switchTab(this, TAB_AVL);
            return;
        }
        if (btnSelectRBT.isMousePressed() || btnTopRBT.isMousePressed()
                || isKeyPressed(KEY_THREE) || isKeyPressed(KEY_KP_3)) {
            switchTab(this, TAB_RBT);
            return;
        }

        // Clique direto nos cartões também seleciona a aba
        if (isMouseButtonPressed(MOUSE_BUTTON_LEFT) && !btnOpenAll.isMouseOver()) {
            int mx = getMouseX();
            int my = getMouseY();
            if (isInside(mx, my, CARD_1_X, CARD_Y, CARD_W, CARD_H)) {
                switchTab(this, TAB_BST);
                return;
            }
            if (isInside(mx, my, CARD_2_X, CARD_Y, CARD_W, CARD_H)) {
                switchTab(this, TAB_AVL);
                return;
            }
            if (isInside(mx, my, CARD_3_X, CARD_Y, CARD_W, CARD_H)) {
                switchTab(this, TAB_RBT);
                return;
            }
        }

        if (btnOpenAll.isMousePressed()) {
            openAllSimultaneously(this);
        }
    }

    @Override
    public void draw() {
        clearBackground(C_BG);

        // --- Cabeçalho ---
        fillRectangle(0, 0, getScreenWidth(), 100, C_HDR);
        drawText("Sistema de Árvores Binárias", 24, 14, 22, WHITE);
        drawText("Menu Principal — Escolha qual das 3 abas deseja abrir como principal",
                 24, 48, 12, C_HDR_SUB);
        drawText("Você pode alternar entre as abas a qualquer momento pelo menu superior.",
                 24, 68, 11, new Color(140, 170, 220));

        btnTopMenu.draw();
        btnTopBST.draw();
        btnTopAVL.draw();
        btnTopRBT.draw();

        int mx = getMouseX();
        int my = getMouseY();

        // --- Cartão 1: BST ---
        drawCardBST(CARD_1_X, CARD_Y, CARD_W, CARD_H, isInside(mx, my, CARD_1_X, CARD_Y, CARD_W, CARD_H));

        // --- Cartão 2: AVL ---
        drawCardAVL(CARD_2_X, CARD_Y, CARD_W, CARD_H, isInside(mx, my, CARD_2_X, CARD_Y, CARD_W, CARD_H));

        // --- Cartão 3: Rubro-Negra ---
        drawCardRBT(CARD_3_X, CARD_Y, CARD_W, CARD_H, isInside(mx, my, CARD_3_X, CARD_Y, CARD_W, CARD_H));

        // --- Botões dos Cartões ---
        btnSelectBST.draw();
        btnSelectAVL.draw();
        btnSelectRBT.draw();

        // --- Rodapé ---
        fillRoundRectangle(26, 605, 948, 52, 10, new Color(220, 228, 242));
        drawRoundRectangle(26, 605, 948, 52, 10, new Color(180, 195, 220));
        drawText("Atalhos: Pressione [1] BST, [2] AVL ou [3] Rubro-Negra.",
                 40, 616, 12, new Color(30, 45, 80));
        drawText("Clique em um cartão para definir aquela árvore como aba principal.",
                 40, 635, 11, new Color(70, 85, 115));

        btnOpenAll.draw();

        drawFPS(getScreenWidth() - 80, 12);
    }

    // =========================================================================
    // Desenho dos 3 Cartões de Seleção
    // =========================================================================
    private void drawCardBST(int x, int y, int w, int h, boolean hover) {
        fillRoundRectangle(x, y, w, h, 14, hover ? C_CARD_HOVER : C_CARD_BG);
        setStrokeLineWidth(hover ? 3 : 2);
        drawRoundRectangle(x, y, w, h, 14, hover ? C_BST_NODE : new Color(180, 195, 225));
        setStrokeLineWidth(1);

        // Faixa de título do cartão
        fillRoundRectangle(x, y, w, 54, 14, C_BST_MAIN);
        fillRectangle(x, y + 40, w, 14, C_BST_MAIN);
        drawText("ABA 1 — Árvore BST", x + 16, y + 12, 16, WHITE);
        drawText("Árvore Binária de Busca", x + 16, y + 34, 11, new Color(190, 215, 255));

        // Área de pré-visualização gráfica
        int px = x + 16;
        int py = y + 66;
        int pw = w - 32;
        int ph = 165;
        fillRoundRectangle(px, py, pw, ph, 10, C_BST_LIGHT);
        drawRoundRectangle(px, py, pw, ph, 10, new Color(180, 205, 245));

        // Mini-árvore BST ilustrativa
        double cx = px + pw / 2.0;
        double r1Y = py + 34;
        double r2Y = py + 84;
        double r3Y = py + 134;
        setStrokeLineWidth(2);
        drawLine(cx, r1Y, cx - 55, r2Y, new Color(90, 90, 90));
        drawLine(cx, r1Y, cx + 55, r2Y, new Color(90, 90, 90));
        drawLine(cx - 55, r2Y, cx - 85, r3Y, new Color(90, 90, 90));
        drawLine(cx - 55, r2Y, cx - 25, r3Y, new Color(90, 90, 90));
        drawLine(cx + 55, r2Y, cx + 85, r3Y, new Color(90, 90, 90));

        drawMiniNode(cx, r1Y, "50", C_BST_NODE, new Color(10, 60, 150));
        drawMiniNode(cx - 55, r2Y, "30", C_BST_NODE, new Color(10, 60, 150));
        drawMiniNode(cx + 55, r2Y, "70", C_BST_NODE, new Color(10, 60, 150));
        drawMiniNode(cx - 85, r3Y, "20", C_BST_NODE, new Color(10, 60, 150));
        drawMiniNode(cx - 25, r3Y, "40", new Color(240, 190, 0), new Color(160, 110, 0));
        drawMiniNode(cx + 85, r3Y, "80", C_BST_NODE, new Color(10, 60, 150));
        setStrokeLineWidth(1);

        // Descrição das funcionalidades (textos compactos que cabem com folga no card)
        int ty = py + ph + 16;
        drawText("Características:", x + 16, ty, 13, C_BST_MAIN);
        drawText("• Inserção (put) e remoção", x + 16, ty + 24, 11, new Color(50, 55, 65));
        drawText("• Menores à esq., maiores à dir.", x + 16, ty + 44, 11, new Color(50, 55, 65));
        drawText("• Estrutura sem auto-rotação", x + 16, ty + 64, 11, new Color(50, 55, 65));
        drawText("• Busca binária visual animada", x + 16, ty + 84, 11, new Color(50, 55, 65));
        drawText("Atalho rápido: Tecla [1]", x + 16, ty + 110, 11, new Color(100, 120, 160));
    }

    private void drawCardAVL(int x, int y, int w, int h, boolean hover) {
        fillRoundRectangle(x, y, w, h, 14, hover ? C_CARD_HOVER : C_CARD_BG);
        setStrokeLineWidth(hover ? 3 : 2);
        drawRoundRectangle(x, y, w, h, 14, hover ? C_AVL_NODE : new Color(175, 215, 185));
        setStrokeLineWidth(1);

        // Faixa de título do cartão
        fillRoundRectangle(x, y, w, 54, 14, C_AVL_MAIN);
        fillRectangle(x, y + 40, w, 14, C_AVL_MAIN);
        drawText("ABA 2 — Árvore AVL", x + 16, y + 12, 16, WHITE);
        drawText("Árvore Auto-balanceada", x + 16, y + 34, 11, new Color(185, 235, 195));

        // Área de pré-visualização gráfica
        int px = x + 16;
        int py = y + 66;
        int pw = w - 32;
        int ph = 165;
        fillRoundRectangle(px, py, pw, ph, 10, C_AVL_LIGHT);
        drawRoundRectangle(px, py, pw, ph, 10, new Color(165, 215, 175));

        // Mini-árvore AVL ilustrativa
        double cx = px + pw / 2.0;
        double r1Y = py + 38;
        double r2Y = py + 88;
        double r3Y = py + 136;
        setStrokeLineWidth(2);
        drawLine(cx, r1Y, cx - 55, r2Y, new Color(90, 90, 90));
        drawLine(cx, r1Y, cx + 55, r2Y, new Color(90, 90, 90));
        drawLine(cx - 55, r2Y, cx - 85, r3Y, new Color(90, 90, 90));
        drawLine(cx - 55, r2Y, cx - 25, r3Y, new Color(90, 90, 90));
        drawLine(cx + 55, r2Y, cx + 25, r3Y, new Color(90, 90, 90));
        drawLine(cx + 55, r2Y, cx + 85, r3Y, new Color(90, 90, 90));

        drawMiniNode(cx, r1Y, "50", C_AVL_NODE, new Color(10, 80, 40));
        drawText("FB:0", cx - 12, r1Y - 26, 10, new Color(20, 140, 50));
        drawText("⟳ LL", cx + 20, r1Y - 6, 11, new Color(220, 110, 0));

        drawMiniNode(cx - 55, r2Y, "30", C_AVL_NODE, new Color(10, 80, 40));
        drawText("FB:0", cx - 67, r2Y - 26, 10, new Color(20, 140, 50));

        drawMiniNode(cx + 55, r2Y, "70", C_AVL_NODE, new Color(10, 80, 40));
        drawText("FB:0", cx + 43, r2Y - 26, 10, new Color(20, 140, 50));

        drawMiniNode(cx - 85, r3Y, "20", C_AVL_NODE, new Color(10, 80, 40));
        drawMiniNode(cx - 25, r3Y, "40", C_AVL_NODE, new Color(10, 80, 40));
        drawMiniNode(cx + 25, r3Y, "60", C_AVL_NODE, new Color(10, 80, 40));
        drawMiniNode(cx + 85, r3Y, "80", C_AVL_NODE, new Color(10, 80, 40));
        setStrokeLineWidth(1);

        // Descrição das funcionalidades (textos compactos que cabem com folga no card)
        int ty = py + ph + 16;
        drawText("Características:", x + 16, ty, 13, C_AVL_MAIN);
        drawText("• Balanceamento FB ∈ {-1, 0, 1}", x + 16, ty + 24, 11, new Color(50, 55, 65));
        drawText("• Rotações LL, RR, LR e RL", x + 16, ty + 44, 11, new Color(50, 55, 65));
        drawText("• Exibe FB e rotações na tela", x + 16, ty + 64, 11, new Color(50, 55, 65));
        drawText("• Altura balanceada O(log n)", x + 16, ty + 84, 11, new Color(50, 55, 65));
        drawText("Atalho rápido: Tecla [2]", x + 16, ty + 110, 11, new Color(60, 130, 80));
    }

    private void drawCardRBT(int x, int y, int w, int h, boolean hover) {
        fillRoundRectangle(x, y, w, h, 14, hover ? C_CARD_HOVER : C_CARD_BG);
        setStrokeLineWidth(hover ? 3 : 2);
        drawRoundRectangle(x, y, w, h, 14, hover ? C_RBT_RED : new Color(220, 180, 180));
        setStrokeLineWidth(1);

        // Faixa de título do cartão
        fillRoundRectangle(x, y, w, 54, 14, C_RBT_MAIN);
        fillRectangle(x, y + 40, w, 14, C_RBT_MAIN);
        drawText("ABA 3 — Rubro-Negra", x + 16, y + 12, 16, WHITE);
        drawText("Red-Black Tree (CLRS)", x + 16, y + 34, 11, new Color(255, 195, 195));

        // Área de pré-visualização gráfica
        int px = x + 16;
        int py = y + 66;
        int pw = w - 32;
        int ph = 165;
        fillRoundRectangle(px, py, pw, ph, 10, C_RBT_LIGHT);
        drawRoundRectangle(px, py, pw, ph, 10, new Color(225, 175, 175));

        // Mini-árvore Rubro-Negra ilustrativa
        double cx = px + pw / 2.0;
        double r1Y = py + 38;
        double r2Y = py + 88;
        double r3Y = py + 136;
        setStrokeLineWidth(2);
        drawLine(cx, r1Y, cx - 55, r2Y, new Color(90, 90, 90));
        drawLine(cx, r1Y, cx + 55, r2Y, new Color(90, 90, 90));
        drawLine(cx - 55, r2Y, cx - 85, r3Y, new Color(90, 90, 90));
        drawLine(cx - 55, r2Y, cx - 25, r3Y, new Color(90, 90, 90));
        drawLine(cx + 55, r2Y, cx + 25, r3Y, new Color(90, 90, 90));
        drawLine(cx + 55, r2Y, cx + 85, r3Y, new Color(90, 90, 90));

        drawMiniNode(cx, r1Y, "20", C_RBT_BLACK, new Color(70, 70, 70));
        drawText("P", cx - 4, r1Y - 26, 10, new Color(90, 90, 90));

        drawMiniNode(cx - 55, r2Y, "15", C_RBT_RED, new Color(140, 0, 0));
        drawText("V", cx - 59, r2Y - 26, 10, C_RBT_RED);

        drawMiniNode(cx + 55, r2Y, "25", C_RBT_BLACK, new Color(70, 70, 70));
        drawText("P", cx + 51, r2Y - 26, 10, new Color(90, 90, 90));

        drawMiniNode(cx - 85, r3Y, "10", C_RBT_BLACK, new Color(70, 70, 70));
        drawMiniNode(cx - 25, r3Y, "18", C_RBT_BLACK, new Color(70, 70, 70));
        drawMiniNode(cx + 25, r3Y, "22", C_RBT_RED, new Color(140, 0, 0));
        drawMiniNode(cx + 85, r3Y, "30", C_RBT_RED, new Color(140, 0, 0));
        setStrokeLineWidth(1);

        // Descrição das funcionalidades (textos compactos que cabem com folga no card)
        int ty = py + ph + 16;
        drawText("Características:", x + 16, ty, 13, C_RBT_MAIN);
        drawText("• Nós Vermelhos (V) e Pretos (P)", x + 16, ty + 24, 11, new Color(50, 55, 65));
        drawText("• Raiz e sentinelas NIL pretas", x + 16, ty + 44, 11, new Color(50, 55, 65));
        drawText("• Recoloração e rotações auto", x + 16, ty + 64, 11, new Color(50, 55, 65));
        drawText("• Altura Negra sempre uniforme", x + 16, ty + 84, 11, new Color(50, 55, 65));
        drawText("Atalho rápido: Tecla [3]", x + 16, ty + 110, 11, new Color(150, 60, 60));
    }

    private void drawMiniNode(double x, double y, String label, Color fill, Color border) {
        double r = 15;
        fillCircle(x, y, r, fill);
        drawCircle(x, y, r, border);
        int fs = 11;
        double tw = measureText(label, fs);
        drawText(label, x - tw / 2.0, y - fs / 2.0, fs, WHITE);
    }

    private boolean isInside(int mx, int my, int rx, int ry, int rw, int rh) {
        return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
    }

    // =========================================================================
    // Gerenciamento de Transição de Abas / Janelas
    // =========================================================================

    /**
     * Alterna da janela atual para a aba selecionada como janela principal,
     * mantendo a mesma posição na tela e encerrando a janela anterior.
     *
     * @param currentWindow janela EngineFrame que está solicitando a troca
     * @param targetTab     constante da aba destino (TAB_MENU, TAB_BST, TAB_AVL, TAB_RBT)
     */
    public static void switchTab(EngineFrame currentWindow, int targetTab) {
        if (currentWindow instanceof Launcher) {
            ((Launcher) currentWindow).switching = true;
        }
        final int posX = currentWindow.getX();
        final int posY = currentWindow.getY();

        new Thread(() -> {
            openTabWindow(targetTab, posX, posY);
            closeWindowCleanly(currentWindow);
        }, "Thread-Switch-Tab").start();
    }

    /**
     * Abre a janela correspondente à aba informada na posição especificada.
     */
    public static void openTabWindow(int targetTab, int posX, int posY) {
        switch (targetTab) {
            case TAB_BST:
                new BSTSimulator(posX, posY);
                break;
            case TAB_AVL:
                new AVLSimulator(posX, posY);
                break;
            case TAB_RBT:
                new RBTSimulator(posX, posY);
                break;
            case TAB_MENU:
            default:
                new Launcher(posX, posY);
                break;
        }
    }

    /**
     * Opção auxiliar caso o usuário queira abrir as 3 janelas simultaneamente.
     */
    public static void openAllSimultaneously(EngineFrame currentWindow) {
        if (currentWindow instanceof Launcher) {
            ((Launcher) currentWindow).switching = true;
        }
        new Thread(() -> {
            new Thread(() -> {
                BSTSimulator bst = new BSTSimulator(30, 40);
                bst.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            }, "Thread-BST").start();

            try { Thread.sleep(200); } catch (InterruptedException ignored) {}

            new Thread(() -> {
                AVLSimulator avl = new AVLSimulator(120, 90);
                avl.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            }, "Thread-AVL").start();

            try { Thread.sleep(200); } catch (InterruptedException ignored) {}

            new Thread(() -> {
                RBTSimulator rbt = new RBTSimulator(210, 140);
                rbt.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            }, "Thread-RBT").start();

            closeWindowCleanly(currentWindow);
        }, "Thread-Open-All").start();
    }

    /**
     * Fecha uma janela EngineFrame de forma limpa sem encerrar a JVM.
     */
    private static void closeWindowCleanly(EngineFrame window) {
        if (window == null) return;
        SwingUtilities.invokeLater(() -> {
            window.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
        });
    }

    // =========================================================================
    // Ponto de Entrada
    // =========================================================================
    public static void main(String[] args) {
        new Launcher();
    }
}
