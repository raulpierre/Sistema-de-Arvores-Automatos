package simuladores.avl;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.imgui.GuiButton;
import br.com.davidbuzatto.jsge.imgui.GuiLabel;
import br.com.davidbuzatto.jsge.imgui.GuiTextField;
import java.awt.Color;
import java.util.Random;

/**
 * Simulador Gráfico de Árvore AVL (Adelson-Velsky e Landis).
 * Exibe o Fator de Balanceamento (FB) de cada nó em tempo real.
 * Visualiza as rotações de auto-balanceamento (LL, RR, LR, RL) com animação.
 * Suporte a zoom (roda do mouse) e pan (arraste com botão do meio).
 *
 * @author Simulador de Árvores
 */
public class AVLSimulator extends EngineFrame {

    // =========================================================================
    // Nó AVL
    // =========================================================================
    private static class AVLNode {
        int     key;
        int     height;
        AVLNode left, right;

        // Posição visual
        double x, y;

        // Animação de destaque e rotação
        float  highlight;    // > 0 indica destaque/pulso
        String rotLabel;     // "LL", "RR", "LR", "RL"
        float  rotTimer;

        AVLNode(int key) {
            this.key       = key;
            this.height    = 1;
            this.highlight = 1.5f;
        }
    }

    // =========================================================================
    // Estado da Árvore
    // =========================================================================
    private AVLNode root;
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

    private static final int   NODE_R    = 22;
    private static final Color C_FILL    = new Color(20, 140, 80);
    private static final Color C_HIGH    = new Color(255, 215, 0);
    private static final Color C_BORDER  = new Color(10, 80, 40);
    private static final Color C_EDGE    = new Color(80, 80, 80);
    private static final Color C_FB_OK   = new Color(30, 180, 60);
    private static final Color C_FB_BAD  = new Color(220, 0, 0);
    private static final Color C_PANEL   = new Color(240, 250, 240);
    private static final Color C_HDR     = new Color(15, 75, 40);
    private static final Color C_OK      = new Color(0, 130, 0);
    private static final Color C_ERR     = new Color(200, 0, 0);
    private static final Color C_TXT_LEG = new Color(40, 40, 40);
    private static final Color C_ROT     = new Color(220, 110, 0);

    // =========================================================================
    // Pan / Zoom
    // =========================================================================
    private double panX = 0, panY = 0, zoom = 1.0;
    private double lastMX, lastMY;
    private boolean panning = false;

    // Layout auxiliar (array seguro para numeração in-order)
    private int[] counter = new int[]{0};

    // =========================================================================
    // Construtores
    // =========================================================================
    public AVLSimulator() {
        this(120, 100);
    }

    public AVLSimulator(int posX, int posY) {
        super(
            1000, 680,
            "Árvore AVL (Auto-balanceada) - Simulador",
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

        counter = new int[]{0};

        int px = 10;
        int py = 10;

        // Instancia os componentes ImGUI SEMPRE passando 'this'
        lblValor     = new GuiLabel(px, py + 42, 60, 26, "Valor:", this);
        tfValue      = new GuiTextField(px + 65, py + 40, 90, 28, "", this);
        btnInsert    = new GuiButton(px + 165, py + 40, 85, 28, "Inserir", this);
        btnDelete    = new GuiButton(px + 258, py + 40, 85, 28, "Remover", this);
        btnRandom    = new GuiButton(px + 351, py + 40, 85, 28, "Aleatório", this);
        btnClear     = new GuiButton(px + 444, py + 40, 80, 28, "Limpar", this);
        btnResetView = new GuiButton(px + 532, py + 40, 95, 28, "Reset Zoom", this);

        // Popula com nós de teste para demonstrar balanceamento
        for (int v : new int[]{50, 30, 70, 20, 40, 60, 80, 10, 25, 35}) {
            root = insert(root, v);
        }
        statusMsg = "Árvore AVL inicial criada e auto-balanceada.";
    }

    @Override
    public void update(double delta) {
        tfValue.update(delta);
        btnInsert.update(delta);
        btnDelete.update(delta);
        btnRandom.update(delta);
        btnClear.update(delta);
        btnResetView.update(delta);

        fadeAll(root, (float) delta);

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

        // Pan com botão do meio
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

        // Zoom com roda do mouse
        double wh = getMouseWheelMove();
        if (wh != 0) {
            zoom = Math.max(0.3, Math.min(3.0, zoom + wh * 0.08));
        }
    }

    @Override
    public void draw() {
        clearBackground(new Color(230, 245, 235));

        int panelW = getScreenWidth() - 20;
        int panelH = getScreenHeight() - PANEL_Y - 15;

        // --- Cabeçalho e Título ---
        fillRectangle(0, 0, getScreenWidth(), 100, C_HDR);
        drawText("Árvore AVL (Adelson-Velsky e Landis)", 14, 10, 22, WHITE);
        drawText("Auto-balanceamento por rotações | FB = alt(dir) - alt(esq) ∈ {-1, 0, 1} | Enter para inserir",
                 14, 34, 12, new Color(180, 230, 180));

        // --- Desenho dos Componentes ImGUI ---
        lblValor.draw();
        tfValue.draw();
        btnInsert.draw();
        btnDelete.draw();
        btnRandom.draw();
        btnClear.draw();
        btnResetView.draw();

        Color sc = statusError ? C_ERR : C_OK;
        if (statusMsg != null && !statusMsg.isEmpty()) {
            drawText(statusMsg, 640, 48, 12, sc);
        }

        // --- Painel Gráfico da Árvore ---
        fillRectangle(PANEL_X, PANEL_Y, panelW, panelH, C_PANEL);
        drawRectangle(PANEL_X, PANEL_Y, panelW, panelH, new Color(160, 200, 160));

        if (root == null) {
            drawText("Árvore vazia. Digite um valor ou clique em 'Aleatório' para inserir.",
                     PANEL_X + panelW / 2.0 - 200, PANEL_Y + panelH / 2.0, 16, new Color(140, 170, 140));
        } else {
            recalcLayout(panelW);
            drawEdges(root);
            drawNodes(root);
        }

        // --- Estatísticas da Árvore ---
        int totalNodes = countNodes(root);
        int treeH = height(root);
        String stats = String.format("Nós: %d | Altura AVL: %d | Zoom: %.1fx", totalNodes, treeH, zoom);
        drawText(stats, PANEL_X + 12, PANEL_Y + 12, 13, new Color(20, 80, 40));

        // --- Legenda ---
        int lx = PANEL_X + panelW - 240;
        int ly = PANEL_Y + 10;
        fillRoundRectangle(lx - 8, ly - 5, 235, 95, 8, new Color(255, 255, 255, 215));
        drawRoundRectangle(lx - 8, ly - 5, 235, 95, 8, new Color(160, 200, 160));
        fillCircle(lx + 10, ly + 10, 10, C_FILL);
        drawText("Nó balanceado (FB ∈ {-1,0,1})", lx + 25, ly + 5, 12, C_TXT_LEG);
        fillCircle(lx + 10, ly + 32, 10, C_HIGH);
        drawText("Recém-operado / Rotacionado", lx + 25, ly + 27, 12, C_TXT_LEG);
        drawText("FB = alt(dir) - alt(esq)", lx + 4, ly + 54, 11, new Color(40, 100, 40));
        drawText("LL / RR / LR / RL = tipo de rotação", lx + 4, ly + 72, 11, C_ROT);

        drawFPS(getScreenWidth() - 80, 12);
    }

    // =========================================================================
    // Algoritmo de Posicionamento e Layout
    // =========================================================================
    private void recalcLayout(int panelW) {
        if (root == null) return;
        if (counter == null) counter = new int[]{0};
        counter[0] = 0;

        assignX(root);

        double minX = findMinX(root, Double.MAX_VALUE);
        double maxX = findMaxX(root, -Double.MAX_VALUE);
        double treeW = maxX - minX;
        double cx = PANEL_X + panelW / 2.0 + panX;
        shiftX(root, cx - (minX + treeW / 2.0));
        assignY(root, 0, PANEL_Y + 55 + panY, 68 * zoom);
    }

    private void assignX(AVLNode n) {
        if (n == null) return;
        assignX(n.left);
        n.x = PANEL_X + 50 + counter[0] * (NODE_R * 2 + 10) * zoom;
        counter[0]++;
        assignX(n.right);
    }

    private double findMinX(AVLNode n, double m) {
        if (n == null) return m;
        m = Math.min(m, n.x);
        m = findMinX(n.left, m);
        m = findMinX(n.right, m);
        return m;
    }

    private double findMaxX(AVLNode n, double m) {
        if (n == null) return m;
        m = Math.max(m, n.x);
        m = findMaxX(n.left, m);
        m = findMaxX(n.right, m);
        return m;
    }

    private void shiftX(AVLNode n, double off) {
        if (n == null) return;
        n.x += off;
        shiftX(n.left, off);
        shiftX(n.right, off);
    }

    private void assignY(AVLNode n, int d, double rootY, double gap) {
        if (n == null) return;
        n.y = rootY + d * gap;
        assignY(n.left, d + 1, rootY, gap);
        assignY(n.right, d + 1, rootY, gap);
    }

    // =========================================================================
    // Desenho dos Elementos
    // =========================================================================
    private void drawEdges(AVLNode n) {
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

    private void drawNodes(AVLNode n) {
        if (n == null) return;
        drawNodes(n.left);
        drawNodes(n.right);

        double r = NODE_R * zoom;
        Color fill = C_FILL;
        if (n.highlight > 1f) {
            fill = blendColor(C_FILL, C_HIGH, Math.min(1f, (n.highlight - 1f) * 2f));
        }

        setStrokeLineWidth(2);
        fillCircle(n.x, n.y, r, fill);
        drawCircle(n.x, n.y, r, C_BORDER);

        // Chave do nó
        String txt = String.valueOf(n.key);
        int fs = Math.max(8, (int) (13 * zoom));
        double tw = measureText(txt, fs);
        drawText(txt, n.x - tw / 2.0, n.y - fs / 2.0, fs, WHITE);

        // Fator de balanceamento acima do nó
        int fb = balanceFactor(n);
        Color fbColor = (Math.abs(fb) <= 1) ? C_FB_OK : C_FB_BAD;
        String fbTxt = "FB:" + fb;
        int ffs = Math.max(7, (int) (10 * zoom));
        double ftw = measureText(fbTxt, ffs);
        drawText(fbTxt, n.x - ftw / 2.0, n.y - r - ffs - 2, ffs, fbColor);

        // Badge de rotação
        if (n.rotLabel != null && n.rotTimer > 0) {
            String rl = "⟳ " + n.rotLabel;
            int rfs = Math.max(8, (int) (11 * zoom));
            drawText(rl, n.x + r + 3, n.y - 8, rfs, C_ROT);
        }
    }

    // =========================================================================
    // Algoritmo da Árvore AVL
    // =========================================================================
    private int height(AVLNode n) {
        return n == null ? 0 : n.height;
    }

    private int balanceFactor(AVLNode n) {
        return n == null ? 0 : height(n.right) - height(n.left);
    }

    private void updateHeight(AVLNode n) {
        if (n != null) {
            n.height = 1 + Math.max(height(n.left), height(n.right));
        }
    }

    private AVLNode rotateRight(AVLNode y) {
        AVLNode x  = y.left;
        AVLNode T2 = x.right;

        x.right = y;
        y.left  = T2;

        updateHeight(y);
        updateHeight(x);

        x.highlight = 1.5f;
        y.highlight = 1.5f;
        x.rotLabel = "LL";
        x.rotTimer = 1.8f;

        return x;
    }

    private AVLNode rotateLeft(AVLNode x) {
        AVLNode y  = x.right;
        AVLNode T2 = y.left;

        y.left  = x;
        x.right = T2;

        updateHeight(x);
        updateHeight(y);

        x.highlight = 1.5f;
        y.highlight = 1.5f;
        y.rotLabel = "RR";
        y.rotTimer = 1.8f;

        return y;
    }

    private AVLNode balance(AVLNode n) {
        if (n == null) return null;
        updateHeight(n);
        int bf = balanceFactor(n);

        // Desbalanceado para a esquerda (FB < -1)
        if (bf < -1) {
            if (balanceFactor(n.left) > 0) {
                // Caso Esquerda-Direita (LR): rotação dupla
                n.left.rotLabel = "LR";
                n.left.rotTimer = 1.8f;
                n.left = rotateLeft(n.left);
            }
            return rotateRight(n);
        }

        // Desbalanceado para a direita (FB > 1)
        if (bf > 1) {
            if (balanceFactor(n.right) < 0) {
                // Caso Direita-Esquerda (RL): rotação dupla
                n.right.rotLabel = "RL";
                n.right.rotTimer = 1.8f;
                n.right = rotateRight(n.right);
            }
            return rotateLeft(n);
        }

        return n;
    }

    private AVLNode insert(AVLNode n, int key) {
        if (n == null) return new AVLNode(key);
        if (key < n.key) {
            n.left = insert(n.left, key);
        } else if (key > n.key) {
            n.right = insert(n.right, key);
        }
        return balance(n);
    }

    private AVLNode minNode(AVLNode n) {
        while (n.left != null) n = n.left;
        return n;
    }

    private AVLNode delete(AVLNode n, int key) {
        if (n == null) return null;
        if (key < n.key) {
            n.left = delete(n.left, key);
        } else if (key > n.key) {
            n.right = delete(n.right, key);
        } else {
            if (n.left == null)  return n.right;
            if (n.right == null) return n.left;
            AVLNode succ = minNode(n.right);
            n.key   = succ.key;
            n.right = delete(n.right, succ.key);
        }
        return balance(n);
    }

    private boolean find(AVLNode n, int key) {
        if (n == null) return false;
        if (key == n.key) return true;
        return key < n.key ? find(n.left, key) : find(n.right, key);
    }

    private int countNodes(AVLNode n) {
        if (n == null) return 0;
        return 1 + countNodes(n.left) + countNodes(n.right);
    }

    private void highlight(AVLNode n, int key) {
        if (n == null) return;
        if (n.key == key) {
            n.highlight = 1.5f;
            return;
        }
        if (key < n.key) highlight(n.left, key);
        else highlight(n.right, key);
    }

    private void fadeAll(AVLNode n, float dt) {
        if (n == null) return;
        if (n.highlight > 0) n.highlight = Math.max(0f, n.highlight - dt * 0.8f);
        if (n.rotTimer > 0)  n.rotTimer  = Math.max(0f, n.rotTimer - dt * 0.8f);
        fadeAll(n.left, dt);
        fadeAll(n.right, dt);
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
            if (find(root, v)) {
                setStatus("Chave " + v + " já existe na árvore AVL.", true);
            } else {
                root = insert(root, v);
                highlight(root, v);
                setStatus("✔ Inserido com sucesso: " + v + " (balanceado)", false);
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
            if (!find(root, v)) {
                setStatus("Chave " + v + " não encontrada na árvore.", true);
            } else {
                root = delete(root, v);
                setStatus("✔ Removido com sucesso: " + v + " (rebalanceado)", false);
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
        } while (find(root, v) && attempts < 200);

        if (!find(root, v)) {
            root = insert(root, v);
            highlight(root, v);
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
        new AVLSimulator();
    }
}
