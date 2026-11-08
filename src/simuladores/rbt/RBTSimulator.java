package simuladores.rbt;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.imgui.GuiButton;
import br.com.davidbuzatto.jsge.imgui.GuiLabel;
import br.com.davidbuzatto.jsge.imgui.GuiTextField;
import java.awt.Color;
import java.util.Random;

/**
 * Simulador Gráfico de Árvore Rubro-Negra (Red-Black Tree).
 * Nós vermelhos e pretos de acordo com as propriedades da árvore balanceada.
 * Animação e destaque em tempo real de rotações e recolorações.
 * Suporte a zoom (roda do mouse) e pan (arraste com botão do meio).
 *
 * @author Simulador de Árvores
 */
public class RBTSimulator extends EngineFrame {

    // =========================================================================
    // Definição de Cores da Árvore Rubro-Negra (int para evitar colisões de nome)
    // =========================================================================
    private static final int RBT_RED   = 1;
    private static final int RBT_BLACK = 0;

    private static class RBNode {
        int    key;
        int    color;      // RBT_RED ou RBT_BLACK
        RBNode left, right, parent;

        // Posição visual
        double x, y;

        // Animação de destaque
        float highlight;   // > 0 indica destaque/pulso

        RBNode(int key, int color) {
            this.key       = key;
            this.color     = color;
            this.highlight = 1.5f;
        }
    }

    // =========================================================================
    // Estado da Árvore
    // =========================================================================
    private RBNode NIL;
    private RBNode root;
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
    private static final Color C_RED_N   = new Color(220, 40, 40);
    private static final Color C_BLACK_N = new Color(30, 30, 30);
    private static final Color C_HIGH    = new Color(255, 215, 0);
    private static final Color C_BRD_R   = new Color(140, 0, 0);
    private static final Color C_BRD_B   = new Color(60, 60, 60);
    private static final Color C_EDGE    = new Color(90, 90, 90);
    private static final Color C_PANEL   = new Color(248, 240, 240);
    private static final Color C_HDR     = new Color(95, 15, 15);
    private static final Color C_OK      = new Color(0, 130, 0);
    private static final Color C_ERR     = new Color(200, 0, 0);
    private static final Color C_TXT_LEG = new Color(40, 40, 40);

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
    public RBTSimulator() {
        this(190, 150);
    }

    public RBTSimulator(int posX, int posY) {
        super(
            1000, 680,
            "Árvore Rubro-Negra (Red-Black Tree) - Simulador",
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

        // Sentinela NIL (folha externa padrão, sempre preta)
        NIL = new RBNode(0, RBT_BLACK);
        NIL.left = NIL.right = NIL.parent = NIL;
        root = NIL;

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

        // Popula com nós de teste
        for (int v : new int[]{20, 15, 25, 10, 18, 22, 30, 5, 12}) {
            rbInsert(v);
        }
        statusMsg = "Árvore Rubro-Negra criada com 9 elementos.";
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
            root = NIL;
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
        clearBackground(new Color(252, 240, 240));

        int panelW = getScreenWidth() - 20;
        int panelH = getScreenHeight() - PANEL_Y - 15;

        // --- Cabeçalho e Título ---
        fillRectangle(0, 0, getScreenWidth(), 100, C_HDR);
        drawText("Árvore Rubro-Negra (Red-Black Tree)", 14, 10, 22, WHITE);
        drawText("Propriedades RBT: Raiz=Preta | NIL=Preto | Pai Vermelho tem Filhos Pretos | Altura Negra Igual",
                 14, 34, 12, new Color(255, 190, 190));

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
        drawRectangle(PANEL_X, PANEL_Y, panelW, panelH, new Color(200, 150, 150));

        if (root == NIL || root == null) {
            drawText("Árvore vazia. Digite um valor ou clique em 'Aleatório' para inserir.",
                     PANEL_X + panelW / 2.0 - 200, PANEL_Y + panelH / 2.0, 16, new Color(180, 140, 140));
        } else {
            recalcLayout(panelW);
            drawEdges(root);
            drawNodes(root);
        }

        // --- Estatísticas da Árvore ---
        int totalNodes = countNodes(root);
        int bh = blackHeight(root);
        String stats = String.format("Nós: %d | Altura Negra (Black Height): %d | Zoom: %.1fx", totalNodes, bh, zoom);
        drawText(stats, PANEL_X + 12, PANEL_Y + 12, 13, new Color(100, 20, 20));

        // --- Legenda ---
        int lx = PANEL_X + panelW - 235;
        int ly = PANEL_Y + 10;
        fillRoundRectangle(lx - 8, ly - 5, 230, 95, 8, new Color(255, 255, 255, 215));
        drawRoundRectangle(lx - 8, ly - 5, 230, 95, 8, new Color(200, 150, 150));
        fillCircle(lx + 10, ly + 10, 10, C_RED_N);
        drawText("Nó Vermelho (V)", lx + 25, ly + 5, 12, C_TXT_LEG);
        fillCircle(lx + 10, ly + 32, 10, C_BLACK_N);
        drawText("Nó Preto (P)", lx + 25, ly + 27, 12, C_TXT_LEG);
        fillCircle(lx + 10, ly + 54, 10, C_HIGH);
        drawText("Recém-operado / Rotacionado", lx + 25, ly + 49, 12, C_TXT_LEG);
        fillRectangle(lx + 4, ly + 72, 12, 10, C_BLACK_N);
        drawText("NIL (sentinela preta)", lx + 25, ly + 70, 11, new Color(80, 80, 80));

        drawFPS(getScreenWidth() - 80, 12);
    }

    // =========================================================================
    // Algoritmo de Posicionamento e Layout
    // =========================================================================
    private void recalcLayout(int panelW) {
        if (root == NIL || root == null) return;
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

    private void assignX(RBNode n) {
        if (n == NIL || n == null) return;
        assignX(n.left);
        n.x = PANEL_X + 50 + counter[0] * (NODE_R * 2 + 10) * zoom;
        counter[0]++;
        assignX(n.right);
    }

    private double findMinX(RBNode n, double m) {
        if (n == NIL || n == null) return m;
        m = Math.min(m, n.x);
        m = findMinX(n.left, m);
        m = findMinX(n.right, m);
        return m;
    }

    private double findMaxX(RBNode n, double m) {
        if (n == NIL || n == null) return m;
        m = Math.max(m, n.x);
        m = findMaxX(n.left, m);
        m = findMaxX(n.right, m);
        return m;
    }

    private void shiftX(RBNode n, double off) {
        if (n == NIL || n == null) return;
        n.x += off;
        shiftX(n.left, off);
        shiftX(n.right, off);
    }

    private void assignY(RBNode n, int d, double rootY, double gap) {
        if (n == NIL || n == null) return;
        n.y = rootY + d * gap;
        assignY(n.left, d + 1, rootY, gap);
        assignY(n.right, d + 1, rootY, gap);
    }

    // =========================================================================
    // Desenho dos Elementos
    // =========================================================================
    private void drawEdges(RBNode n) {
        if (n == NIL || n == null) return;
        setStrokeLineWidth(2);
        if (n.left != NIL) {
            drawLine(n.x, n.y, n.left.x, n.left.y, C_EDGE);
            drawEdges(n.left);
        }
        if (n.right != NIL) {
            drawLine(n.x, n.y, n.right.x, n.right.y, C_EDGE);
            drawEdges(n.right);
        }
    }

    private void drawNodes(RBNode n) {
        if (n == NIL || n == null) return;
        drawNodes(n.left);
        drawNodes(n.right);

        double r = NODE_R * zoom;
        boolean isRed = (n.color == RBT_RED);

        Color baseFill = isRed ? C_RED_N : C_BLACK_N;
        Color border   = isRed ? C_BRD_R : C_BRD_B;
        Color fill     = baseFill;
        if (n.highlight > 1f) {
            fill = blendColor(baseFill, C_HIGH, Math.min(1f, (n.highlight - 1f) * 2f));
        }

        setStrokeLineWidth(2);
        fillCircle(n.x, n.y, r, fill);
        drawCircle(n.x, n.y, r, border);

        // Chave do nó
        String txt = String.valueOf(n.key);
        int fs = Math.max(8, (int) (13 * zoom));
        double tw = measureText(txt, fs);
        drawText(txt, n.x - tw / 2.0, n.y - fs / 2.0, fs, WHITE);

        // Indicador de cor acima do nó (V ou P)
        String colorLabel = isRed ? "V" : "P";
        Color clc = isRed ? new Color(255, 100, 100) : new Color(160, 160, 160);
        int cfs = Math.max(7, (int) (9 * zoom));
        double ctw = measureText(colorLabel, cfs);
        drawText(colorLabel, n.x - ctw / 2.0, n.y - r - cfs - 2, cfs, clc);
    }

    // =========================================================================
    // Algoritmo da Árvore Rubro-Negra (Cormen et al. / CLRS)
    // =========================================================================
    private boolean isNodeRed(RBNode n) {
        return n != NIL && n != null && n.color == RBT_RED;
    }

    private void rbInsert(int key) {
        RBNode z = new RBNode(key, RBT_RED);
        z.left = z.right = z.parent = NIL;

        RBNode y = NIL;
        RBNode x = root;
        while (x != NIL) {
            y = x;
            if (z.key < x.key) {
                x = x.left;
            } else if (z.key > x.key) {
                x = x.right;
            } else {
                return; // duplicata ignorada
            }
        }
        z.parent = y;
        if (y == NIL) {
            root = z;
        } else if (z.key < y.key) {
            y.left = z;
        } else {
            y.right = z;
        }

        insertFixup(z);
        ensureSentinelIntegrity();
    }

    private void insertFixup(RBNode z) {
        while (isNodeRed(z.parent)) {
            if (z.parent == z.parent.parent.left) {
                RBNode y = z.parent.parent.right;
                if (isNodeRed(y)) {
                    // Caso 1: Tio vermelho -> apenas recoloração
                    z.parent.color        = RBT_BLACK;
                    z.parent.highlight    = 1.4f;
                    y.color               = RBT_BLACK;
                    y.highlight           = 1.4f;
                    z.parent.parent.color = RBT_RED;
                    z.parent.parent.highlight = 1.4f;
                    z = z.parent.parent;
                } else {
                    if (z == z.parent.right) {
                        // Caso 2: Tio preto, z é filho direito -> rotação à esquerda
                        z = z.parent;
                        rotateLeft(z);
                    }
                    // Caso 3: Tio preto, z é filho esquerdo -> rotação à direita + recoloração
                    z.parent.color        = RBT_BLACK;
                    z.parent.highlight    = 1.4f;
                    z.parent.parent.color = RBT_RED;
                    z.parent.parent.highlight = 1.4f;
                    rotateRight(z.parent.parent);
                }
            } else {
                RBNode y = z.parent.parent.left;
                if (isNodeRed(y)) {
                    // Caso 1 espelhado: Tio vermelho
                    z.parent.color        = RBT_BLACK;
                    z.parent.highlight    = 1.4f;
                    y.color               = RBT_BLACK;
                    y.highlight           = 1.4f;
                    z.parent.parent.color = RBT_RED;
                    z.parent.parent.highlight = 1.4f;
                    z = z.parent.parent;
                } else {
                    if (z == z.parent.left) {
                        // Caso 2 espelhado
                        z = z.parent;
                        rotateRight(z);
                    }
                    // Caso 3 espelhado
                    z.parent.color        = RBT_BLACK;
                    z.parent.highlight    = 1.4f;
                    z.parent.parent.color = RBT_RED;
                    z.parent.parent.highlight = 1.4f;
                    rotateLeft(z.parent.parent);
                }
            }
        }
        root.color = RBT_BLACK;
    }

    private void rbDelete(int key) {
        RBNode z = search(root, key);
        if (z == NIL) return;
        rbDeleteNode(z);
        ensureSentinelIntegrity();
    }

    private void rbDeleteNode(RBNode z) {
        RBNode y = z;
        RBNode x;
        int yOrigColor = y.color;

        if (z.left == NIL) {
            x = z.right;
            transplant(z, z.right);
        } else if (z.right == NIL) {
            x = z.left;
            transplant(z, z.left);
        } else {
            y = minimum(z.right);
            yOrigColor = y.color;
            x = y.right;
            if (y.parent == z) {
                x.parent = y;
            } else {
                transplant(y, y.right);
                y.right = z.right;
                y.right.parent = y;
            }
            transplant(z, y);
            y.left = z.left;
            y.left.parent = y;
            y.color = z.color;
            y.highlight = 1.4f;
        }

        if (yOrigColor == RBT_BLACK) {
            deleteFixup(x);
        }
    }

    private void deleteFixup(RBNode x) {
        while (x != root && x.color == RBT_BLACK) {
            if (x == x.parent.left) {
                RBNode w = x.parent.right;
                if (isNodeRed(w)) {
                    w.color        = RBT_BLACK;
                    w.highlight    = 1.4f;
                    x.parent.color = RBT_RED;
                    x.parent.highlight = 1.4f;
                    rotateLeft(x.parent);
                    w = x.parent.right;
                }
                if (w.left.color == RBT_BLACK && w.right.color == RBT_BLACK) {
                    w.color     = RBT_RED;
                    w.highlight = 1.4f;
                    x = x.parent;
                } else {
                    if (w.right.color == RBT_BLACK) {
                        w.left.color = RBT_BLACK;
                        w.left.highlight = 1.4f;
                        w.color      = RBT_RED;
                        w.highlight  = 1.4f;
                        rotateRight(w);
                        w = x.parent.right;
                    }
                    w.color        = x.parent.color;
                    w.highlight    = 1.4f;
                    x.parent.color = RBT_BLACK;
                    x.parent.highlight = 1.4f;
                    w.right.color  = RBT_BLACK;
                    w.right.highlight = 1.4f;
                    rotateLeft(x.parent);
                    x = root;
                }
            } else {
                RBNode w = x.parent.left;
                if (isNodeRed(w)) {
                    w.color        = RBT_BLACK;
                    w.highlight    = 1.4f;
                    x.parent.color = RBT_RED;
                    x.parent.highlight = 1.4f;
                    rotateRight(x.parent);
                    w = x.parent.left;
                }
                if (w.right.color == RBT_BLACK && w.left.color == RBT_BLACK) {
                    w.color     = RBT_RED;
                    w.highlight = 1.4f;
                    x = x.parent;
                } else {
                    if (w.left.color == RBT_BLACK) {
                        w.right.color = RBT_BLACK;
                        w.right.highlight = 1.4f;
                        w.color       = RBT_RED;
                        w.highlight   = 1.4f;
                        rotateLeft(w);
                        w = x.parent.left;
                    }
                    w.color        = x.parent.color;
                    w.highlight    = 1.4f;
                    x.parent.color = RBT_BLACK;
                    x.parent.highlight = 1.4f;
                    w.left.color   = RBT_BLACK;
                    w.left.highlight = 1.4f;
                    rotateRight(x.parent);
                    x = root;
                }
            }
        }
        x.color = RBT_BLACK;
    }

    private void rotateLeft(RBNode x) {
        RBNode y = x.right;
        x.right = y.left;
        if (y.left != NIL) {
            y.left.parent = x;
        }
        y.parent = x.parent;
        if (x.parent == NIL) {
            root = y;
        } else if (x == x.parent.left) {
            x.parent.left = y;
        } else {
            x.parent.right = y;
        }
        y.left   = x;
        x.parent = y;

        x.highlight = 1.3f;
        y.highlight = 1.3f;
    }

    private void rotateRight(RBNode x) {
        RBNode y = x.left;
        x.left = y.right;
        if (y.right != NIL) {
            y.right.parent = x;
        }
        y.parent = x.parent;
        if (x.parent == NIL) {
            root = y;
        } else if (x == x.parent.right) {
            x.parent.right = y;
        } else {
            x.parent.left = y;
        }
        y.right  = x;
        x.parent = y;

        x.highlight = 1.3f;
        y.highlight = 1.3f;
    }

    private void transplant(RBNode u, RBNode v) {
        if (u.parent == NIL) {
            root = v;
        } else if (u == u.parent.left) {
            u.parent.left = v;
        } else {
            u.parent.right = v;
        }
        v.parent = u.parent;
    }

    private RBNode minimum(RBNode n) {
        while (n.left != NIL) n = n.left;
        return n;
    }

    private RBNode search(RBNode n, int key) {
        while (n != NIL) {
            if (key < n.key) {
                n = n.left;
            } else if (key > n.key) {
                n = n.right;
            } else {
                return n;
            }
        }
        return NIL;
    }

    private boolean find(RBNode n, int key) {
        return search(n, key) != NIL;
    }

    private int countNodes(RBNode n) {
        if (n == NIL || n == null) return 0;
        return 1 + countNodes(n.left) + countNodes(n.right);
    }

    private int blackHeight(RBNode n) {
        if (n == NIL || n == null) return 0;
        int bh = blackHeight(n.left);
        return (n.color == RBT_BLACK) ? bh + 1 : bh;
    }

    private void ensureSentinelIntegrity() {
        if (NIL != null) {
            NIL.color = RBT_BLACK;
            NIL.left = NIL;
            NIL.right = NIL;
            NIL.parent = NIL;
        }
    }

    private void hlNode(RBNode n, int key) {
        RBNode found = search(n, key);
        if (found != NIL) {
            found.highlight = 1.5f;
        }
    }

    private void fadeAll(RBNode n, float dt) {
        if (n == NIL || n == null) return;
        if (n.highlight > 0) n.highlight = Math.max(0f, n.highlight - dt * 0.8f);
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
                setStatus("Chave " + v + " já existe na árvore Rubro-Negra.", true);
            } else {
                rbInsert(v);
                hlNode(root, v);
                setStatus("✔ Inserido com sucesso: " + v + " (propriedades RBT mantidas)", false);
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
                rbDelete(v);
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
            rbInsert(v);
            hlNode(root, v);
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
        new RBTSimulator();
    }
}
