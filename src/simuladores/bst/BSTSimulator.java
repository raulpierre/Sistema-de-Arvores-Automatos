package simuladores.bst;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.imgui.GuiButton;
import br.com.davidbuzatto.jsge.imgui.GuiLabel;
import br.com.davidbuzatto.jsge.imgui.GuiTextField;
import java.awt.Color;
import java.util.Random;

/**
 * Simulador Gráfico de Árvore Binária de Busca (BST).
 * Permite inserir (put) e remover (delete) nós com visualização gráfica animada.
 * Suporte a zoom (roda do mouse) e pan (arraste com botão do meio).
 *
 * @author Simulador de Árvores
 */
public class BSTSimulator extends EngineFrame {

    // =========================================================================
    // Nó da BST
    // =========================================================================
    private static class BSTNode {
        int key;
        BSTNode left, right;

        // Posição visual (calculada no layout)
        double x, y;

        // Animação de destaque
        float highlight;   // > 0 indica destaque/pulso

        BSTNode(int key) {
            this.key = key;
            this.highlight = 1.5f;
        }
    }

    // =========================================================================
    // Estado da Árvore
    // =========================================================================
    private BSTNode root;
    private final Random random = new Random();

    // =========================================================================
    // Componentes ImGUI (cada um recebe 'this' como EngineFrame dependente)
    // =========================================================================
    private GuiLabel     lblValor;
    private GuiTextField tfValue;
    private GuiButton    btnInsert;
    private GuiButton    btnDelete;
    private GuiButton    btnRandom;
    private GuiButton    btnClear;
    private GuiButton    btnResetView;

    private String  statusMsg   = "Pronto para interagir.";
    private boolean statusError = false;

    // =========================================================================
    // Constantes de Layout e Cores
    // =========================================================================
    private static final int PANEL_X = 10;
    private static final int PANEL_Y = 110;

    private static final int   NODE_R         = 22;
    private static final Color C_NODE_FILL    = new Color(30, 120, 220);
    private static final Color C_NODE_HIGH    = new Color(255, 215, 0);
    private static final Color C_NODE_BORDER  = new Color(10, 60, 150);
    private static final Color C_EDGE         = new Color(80, 80, 80);
    private static final Color C_PANEL_BG     = new Color(245, 248, 255);
    private static final Color C_PANEL_BORDER = new Color(180, 190, 220);
    private static final Color C_HEADER_BG    = new Color(25, 55, 140);
    private static final Color C_OK           = new Color(0, 130, 0);
    private static final Color C_ERR          = new Color(200, 0, 0);
    private static final Color C_TXT_LEG      = new Color(40, 40, 40);

    // =========================================================================
    // Pan / Zoom
    // =========================================================================
    private double panX = 0, panY = 0, zoom = 1.0;
    private double lastMX, lastMY;
    private boolean panning = false;

    // Layout auxiliar (array seguro para numeração in-order)
    private int[] inOrderIdx = new int[]{0};

    // =========================================================================
    // Construtores
    // =========================================================================
    public BSTSimulator() {
        this(50, 50);
    }

    public BSTSimulator(int posX, int posY) {
        super(
            1000, 680,
            "Árvore Binária de Busca (BST) - Simulador",
            60, true, true, false, false, false, false
        );
        setLocation(posX, posY);
    }

    // =========================================================================
    // Ciclo de Vida da Engine
    // =========================================================================
    @Override
    public void create() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        inOrderIdx = new int[]{0};

        int px = 10;
        int py = 10;

        // Instancia os componentes ImGUI SEMPRE passando 'this' para evitar conflito entre janelas
        lblValor     = new GuiLabel(px, py + 42, 60, 26, "Valor:", this);
        tfValue      = new GuiTextField(px + 65, py + 40, 90, 28, "", this);
        btnInsert    = new GuiButton(px + 165, py + 40, 85, 28, "Inserir", this);
        btnDelete    = new GuiButton(px + 258, py + 40, 85, 28, "Remover", this);
        btnRandom    = new GuiButton(px + 351, py + 40, 85, 28, "Aleatório", this);
        btnClear     = new GuiButton(px + 444, py + 40, 80, 28, "Limpar", this);
        btnResetView = new GuiButton(px + 532, py + 40, 95, 28, "Reset Zoom", this);

        // Popula com nós iniciais de teste
        for (int v : new int[]{50, 30, 70, 20, 40, 60, 80, 10, 35, 65}) {
            root = insertNode(root, v);
        }
        statusMsg = "Árvore inicial criada com 10 elementos.";
    }

    @Override
    public void update(double delta) {
        // Atualiza componentes ImGUI vinculados a esta janela
        tfValue.update(delta);
        btnInsert.update(delta);
        btnDelete.update(delta);
        btnRandom.update(delta);
        btnClear.update(delta);
        btnResetView.update(delta);

        // Desvanece destaques de animação
        fadeHighlight(root, (float) delta);

        // Disparo por clique ou tecla ENTER
        if (btnInsert.isMousePressed() || isKeyPressed(KEY_ENTER)) {
            doInsert();
        }
        if (btnDelete.isMousePressed()) {
            doDelete();
        }
        if (btnRandom.isMousePressed()) {
            doRandom();
        }
        if (btnClear.isMousePressed()) {
            root = null;
            setStatus("Árvore limpa com sucesso.", false);
        }
        if (btnResetView.isMousePressed()) {
            panX = 0;
            panY = 0;
            zoom = 1.0;
            setStatus("Visualização restaurada ao padrão.", false);
        }

        // Navegação: pan com o botão do meio do mouse
        double mx = getMouseX();
        double my = getMouseY();
        if (isMouseButtonDown(MOUSE_BUTTON_MIDDLE)) {
            if (!panning) {
                lastMX = mx;
                lastMY = my;
                panning = true;
            } else {
                panX += (mx - lastMX);
                panY += (my - lastMY);
                lastMX = mx;
                lastMY = my;
            }
        } else {
            panning = false;
        }

        // Zoom com a roda do mouse
        double wh = getMouseWheelMove();
        if (wh != 0) {
            zoom = Math.max(0.3, Math.min(3.0, zoom + wh * 0.08));
        }
    }

    @Override
    public void draw() {
        clearBackground(new Color(235, 240, 255));

        int panelW = getScreenWidth() - 20;
        int panelH = getScreenHeight() - PANEL_Y - 15;

        // --- Cabeçalho e Título ---
        fillRectangle(0, 0, getScreenWidth(), 100, C_HEADER_BG);
        drawText("Árvore Binária de Busca (BST)", 14, 10, 22, WHITE);
        drawText("Operações de Inserção (put) e Remoção (delete) | Roda do mouse: zoom | Botão do meio: pan",
                 14, 34, 12, new Color(200, 220, 255));

        // --- Desenho dos Componentes ImGUI ---
        lblValor.draw();
        tfValue.draw();
        btnInsert.draw();
        btnDelete.draw();
        btnRandom.draw();
        btnClear.draw();
        btnResetView.draw();

        // Linha de status
        Color sc = statusError ? C_ERR : C_OK;
        if (statusMsg != null && !statusMsg.isEmpty()) {
            drawText(statusMsg, 640, 48, 12, sc);
        }

        // --- Painel Gráfico da Árvore ---
        fillRectangle(PANEL_X, PANEL_Y, panelW, panelH, C_PANEL_BG);
        drawRectangle(PANEL_X, PANEL_Y, panelW, panelH, C_PANEL_BORDER);

        if (root == null) {
            drawText("Árvore vazia. Digite um valor ou clique em 'Aleatório' para inserir.",
                     PANEL_X + panelW / 2.0 - 200, PANEL_Y + panelH / 2.0, 16, new Color(150, 160, 190));
        } else {
            recalcLayout(panelW);
            drawEdges(root);
            drawNodes(root);
        }

        // --- Estatísticas da Árvore ---
        int totalNodes = countNodes(root);
        int treeHeight = treeHeight(root);
        String stats = String.format("Nós: %d | Altura: %d | Zoom: %.1fx", totalNodes, treeHeight, zoom);
        drawText(stats, PANEL_X + 12, PANEL_Y + 12, 13, new Color(40, 60, 120));

        // --- Legenda ---
        int lx = PANEL_X + panelW - 220;
        int ly = PANEL_Y + 10;
        fillRoundRectangle(lx - 8, ly - 5, 215, 75, 8, new Color(255, 255, 255, 210));
        drawRoundRectangle(lx - 8, ly - 5, 215, 75, 8, C_PANEL_BORDER);
        fillCircle(lx + 10, ly + 10, 10, C_NODE_FILL);
        drawText("Nó comum", lx + 25, ly + 5, 12, C_TXT_LEG);
        fillCircle(lx + 10, ly + 32, 10, C_NODE_HIGH);
        drawText("Recém-operado / Destaque", lx + 25, ly + 27, 12, C_TXT_LEG);
        drawText("Menor à esq. / Maior à dir.", lx + 4, ly + 52, 11, new Color(100, 100, 100));

        drawFPS(getScreenWidth() - 80, 12);
    }

    // =========================================================================
    // Algoritmo de Posicionamento e Layout
    // =========================================================================
    private void recalcLayout(int panelW) {
        if (root == null) return;
        if (inOrderIdx == null) inOrderIdx = new int[]{0};
        inOrderIdx[0] = 0;

        assignX(root);

        double minX = findMinX(root, Double.MAX_VALUE);
        double maxX = findMaxX(root, -Double.MAX_VALUE);
        double treeW = maxX - minX;
        double cx = PANEL_X + panelW / 2.0 + panX;
        shiftX(root, cx - (minX + treeW / 2.0));
        assignY(root, 0, PANEL_Y + 55 + panY, 68 * zoom);
    }

    private void assignX(BSTNode n) {
        if (n == null) return;
        assignX(n.left);
        n.x = PANEL_X + 50 + inOrderIdx[0] * (NODE_R * 2 + 10) * zoom;
        inOrderIdx[0]++;
        assignX(n.right);
    }

    private double findMinX(BSTNode n, double m) {
        if (n == null) return m;
        m = Math.min(m, n.x);
        m = findMinX(n.left, m);
        m = findMinX(n.right, m);
        return m;
    }

    private double findMaxX(BSTNode n, double m) {
        if (n == null) return m;
        m = Math.max(m, n.x);
        m = findMaxX(n.left, m);
        m = findMaxX(n.right, m);
        return m;
    }

    private void shiftX(BSTNode n, double off) {
        if (n == null) return;
        n.x += off;
        shiftX(n.left, off);
        shiftX(n.right, off);
    }

    private void assignY(BSTNode n, int depth, double rootY, double gap) {
        if (n == null) return;
        n.y = rootY + depth * gap;
        assignY(n.left, depth + 1, rootY, gap);
        assignY(n.right, depth + 1, rootY, gap);
    }

    // =========================================================================
    // Desenho dos Elementos
    // =========================================================================
    private void drawEdges(BSTNode n) {
        if (n == null) return;
        setStrokeLineWidth(2);
        if (n.left != null) {
            drawLine(n.x, n.y, n.left.x, n.left.y, C_EDGE);
            drawEdges(n.left);
        }
        if (n.right != null) {
            drawLine(n.x, n.y, n.right.x, n.right.y, C_EDGE);
            drawEdges(n.right);
        }
    }

    private void drawNodes(BSTNode n) {
        if (n == null) return;
        drawNodes(n.left);
        drawNodes(n.right);

        double r = NODE_R * zoom;
        Color fill = C_NODE_FILL;
        if (n.highlight > 1f) {
            fill = blendColor(C_NODE_FILL, C_NODE_HIGH, Math.min(1f, (n.highlight - 1f) * 2f));
        }

        setStrokeLineWidth(2);
        fillCircle(n.x, n.y, r, fill);
        drawCircle(n.x, n.y, r, C_NODE_BORDER);

        String txt = String.valueOf(n.key);
        int fs = Math.max(8, (int) (13 * zoom));
        double tw = measureText(txt, fs);
        drawText(txt, n.x - tw / 2.0, n.y - fs / 2.0, fs, WHITE);
    }

    // =========================================================================
    // Operações da Árvore Binária de Busca (BST)
    // =========================================================================
    private BSTNode insertNode(BSTNode n, int key) {
        if (n == null) return new BSTNode(key);
        if (key < n.key) {
            n.left = insertNode(n.left, key);
        } else if (key > n.key) {
            n.right = insertNode(n.right, key);
        }
        return n;
    }

    private BSTNode deleteNode(BSTNode n, int key) {
        if (n == null) return null;
        if (key < n.key) {
            n.left = deleteNode(n.left, key);
        } else if (key > n.key) {
            n.right = deleteNode(n.right, key);
        } else {
            if (n.left == null)  return n.right;
            if (n.right == null) return n.left;
            BSTNode succ = minNode(n.right);
            n.key = succ.key;
            n.right = deleteNode(n.right, succ.key);
        }
        return n;
    }

    private BSTNode minNode(BSTNode n) {
        while (n.left != null) n = n.left;
        return n;
    }

    private boolean contains(BSTNode n, int key) {
        if (n == null) return false;
        if (key == n.key) return true;
        return key < n.key ? contains(n.left, key) : contains(n.right, key);
    }

    private int countNodes(BSTNode n) {
        if (n == null) return 0;
        return 1 + countNodes(n.left) + countNodes(n.right);
    }

    private int treeHeight(BSTNode n) {
        if (n == null) return 0;
        return 1 + Math.max(treeHeight(n.left), treeHeight(n.right));
    }

    private void highlightNode(BSTNode n, int key) {
        if (n == null) return;
        if (n.key == key) {
            n.highlight = 1.5f;
            return;
        }
        if (key < n.key) highlightNode(n.left, key);
        else highlightNode(n.right, key);
    }

    private void fadeHighlight(BSTNode n, float dt) {
        if (n == null) return;
        if (n.highlight > 0) n.highlight = Math.max(0f, n.highlight - dt * 0.8f);
        fadeHighlight(n.left, dt);
        fadeHighlight(n.right, dt);
    }

    // =========================================================================
    // Interação e Handlers
    // =========================================================================
    private void doInsert() {
        String s = tfValue.getValue().trim();
        if (s.isEmpty()) {
            setStatus("Digite um valor inteiro para inserir!", true);
            return;
        }
        try {
            int v = Integer.parseInt(s);
            if (contains(root, v)) {
                setStatus("Chave " + v + " já existe na árvore BST.", true);
            } else {
                root = insertNode(root, v);
                highlightNode(root, v);
                setStatus("✔ Inserido com sucesso: " + v, false);
            }
            tfValue.setValue("");
        } catch (NumberFormatException e) {
            setStatus("Valor inválido! Digite apenas números inteiros.", true);
        }
    }

    private void doDelete() {
        String s = tfValue.getValue().trim();
        if (s.isEmpty()) {
            setStatus("Digite um valor inteiro para remover!", true);
            return;
        }
        try {
            int v = Integer.parseInt(s);
            if (!contains(root, v)) {
                setStatus("Chave " + v + " não encontrada na árvore.", true);
            } else {
                root = deleteNode(root, v);
                setStatus("✔ Removido com sucesso: " + v, false);
            }
            tfValue.setValue("");
        } catch (NumberFormatException e) {
            setStatus("Valor inválido! Digite apenas números inteiros.", true);
        }
    }

    private void doRandom() {
        int v;
        int attempts = 0;
        do {
            v = random.nextInt(99) + 1;
            attempts++;
        } while (contains(root, v) && attempts < 200);

        if (!contains(root, v)) {
            root = insertNode(root, v);
            highlightNode(root, v);
            setStatus("✔ Inserido aleatório: " + v, false);
        } else {
            setStatus("Muitas chaves ocupadas.", true);
        }
    }

    private void setStatus(String msg, boolean err) {
        this.statusMsg   = msg;
        this.statusError = err;
    }

    private Color blendColor(Color a, Color b, float t) {
        int r  = Math.min(255, Math.max(0, (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t)));
        int g  = Math.min(255, Math.max(0, (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t)));
        int bl = Math.min(255, Math.max(0, (int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * t)));
        return new Color(r, g, bl);
    }

    // =========================================================================
    public static void main(String[] args) {
        new BSTSimulator();
    }
}
