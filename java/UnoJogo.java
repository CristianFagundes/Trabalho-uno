import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/** UNO (Swing): menu inicial, mesa com bots, histórico, placar e destaque de cartas jogáveis. */
class UnoJogo extends JFrame {

    static final Color[] COR = {
        new Color(0xE53935), new Color(0xF9A825), new Color(0x43A047), new Color(0x1E88E5), new Color(0x212121)
    };
    static final String[] NOME_COR = {"Vermelho", "Amarelo", "Verde", "Azul"};
    static final Color MESA = new Color(0x239B4B);
    static final Color PAINEL = new Color(0x14532B);
    static final String REGRAS =
        "OBJETIVO: ficar sem cartas primeiro.\n\n"
        + "• Jogue uma carta da mesma cor ou do mesmo número/símbolo da carta do topo.\n"
        + "• Sem carta jogável? Clique no monte para comprar. Se a carta comprada servir, jogue-a ou passe a vez.\n"
        + "• \u2298 Pular: o próximo jogador perde a vez.\n"
        + "• \u21C4 Inverter: muda o sentido (com 2 jogadores, você joga de novo).\n"
        + "• +2: o próximo compra 2 cartas e perde a vez.\n"
        + "• Coringa (W): você escolhe a cor.\n"
        + "• Coringa +4: você escolhe a cor e o próximo compra 4 e perde a vez.\n"
        + "• Quem ficar com 1 carta recebe o selo UNO!";

    static class Card {
        final int color;      // 0-3 = cores, 4 = coringa
        final String value;   // "0".."9", "SKIP", "REV", "+2", "WILD", "+4"
        Card(int c, String v) { color = c; value = v; }
        String label() {
            switch (value) {
                case "SKIP": return "\u2298";
                case "REV": return "\u21C4";
                case "WILD": return "W";
                default: return value;
            }
        }
    }

    // ---------- botão colorido ----------
    static class Flat extends JButton {
        final Color cor;
        Flat(String t, Color c) {
            super(t);
            cor = c;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFont(new Font("SansSerif", Font.BOLD, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color c = !isEnabled() ? new Color(0x777777) : (getModel().isPressed() ? cor.darker() : cor);
            g.setColor(c);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g.setColor(isEnabled() ? Color.WHITE : new Color(0xBBBBBB));
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
        }
    }

    // ---------- desenho de cartas ----------
    static void centro(Graphics2D g, String s, int cx, int base) {
        g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, base);
    }

    static void desenhaCarta(Graphics2D g, int x, int y, int w, int h, Card c, boolean up) {
        int arc = Math.max(8, w / 6), b = Math.max(3, w / 14);
        g.setColor(new Color(0, 0, 0, 70));
        g.fillRoundRect(x + 2, y + 3, w, h, arc, arc);
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, w, h, arc, arc);
        AffineTransform t = g.getTransform();
        if (!up) {
            g.setColor(new Color(0x111111));
            g.fillRoundRect(x + b, y + b, w - 2 * b, h - 2 * b, arc - 2, arc - 2);
            g.setColor(COR[0]);
            g.rotate(Math.toRadians(-35), x + w / 2.0, y + h / 2.0);
            g.fillOval(x + w / 8, y + h / 5, w * 3 / 4, h * 3 / 5);
            g.setTransform(t);
            g.setColor(new Color(0xFFD600));
            g.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, Math.max(9, w / 3)));
            centro(g, "UNO", x + w / 2, y + h / 2 + w / 9);
        } else {
            Shape old = g.getClip();
            g.clip(new RoundRectangle2D.Float(x + b, y + b, w - 2 * b, h - 2 * b, arc - 2, arc - 2));
            if (c.color == 4) {
                int ix = x + b, iy = y + b, iw = w - 2 * b, ih = h - 2 * b;
                g.setColor(COR[0]); g.fillRect(ix, iy, iw / 2, ih / 2);
                g.setColor(COR[3]); g.fillRect(ix + iw / 2, iy, iw, ih / 2);
                g.setColor(COR[2]); g.fillRect(ix, iy + ih / 2, iw / 2, ih);
                g.setColor(COR[1]); g.fillRect(ix + iw / 2, iy + ih / 2, iw, ih);
            } else {
                g.setColor(COR[c.color]);
                g.fillRect(x, y, w, h);
            }
            g.setClip(old);
            g.rotate(Math.toRadians(-30), x + w / 2.0, y + h / 2.0);
            g.setColor(Color.WHITE);
            g.fillOval(x + w / 8, y + h / 5, w * 3 / 4, h * 3 / 5);
            g.setTransform(t);
            String s = c.label();
            g.setColor(c.color == 4 ? Color.BLACK : COR[c.color]);
            g.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, s.length() > 1 ? w * 2 / 5 : w / 2));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + h / 2 + fm.getAscent() / 3);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, w / 5)));
            fm = g.getFontMetrics();
            g.drawString(s, x + b + 3, y + b + fm.getAscent());
            g.drawString(s, x + w - b - 3 - fm.stringWidth(s), y + h - b - 4);
        }
        g.setColor(new Color(0, 0, 0, 120));
        g.drawRoundRect(x, y, w, h, arc, arc);
    }

    // ---------- estado ----------
    final List<Card> deck = new ArrayList<>();
    final List<Card> discard = new ArrayList<>();
    final List<List<Card>> hands = new ArrayList<>();
    int n = 4, cur, dir, curColor, bots = 3;
    String[] names = new String[0];
    int[] wins = new int[0];
    boolean over, drewFlag;
    String msg = " ";

    final CardLayout layout = new CardLayout();
    final JPanel root = new JPanel(layout);
    final Board board = new Board();
    final JTextArea historico = new JTextArea();
    final JTextArea placar = new JTextArea();
    final JCheckBox destacar = new JCheckBox("Destacar cartas jogáveis", true);
    final Flat passBtn = new Flat("Passar a vez", new Color(0x666666));
    final Flat unoBtn = new Flat("UNO!", new Color(0xE53935));
    boolean unoDito, unoPendente;
    final JTextField nomeField = new JTextField("Você");
    final javax.swing.Timer botTimer = new javax.swing.Timer(1000, e -> botTurn());

    // ---------- menu inicial ----------
    class Header extends JPanel {
        Header() { setOpaque(false); setPreferredSize(new Dimension(460, 310)); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            g.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, 96));
            g.setColor(new Color(0x7A5C00));
            centro(g, "UNO", w / 2 + 4, 99);
            g.setColor(new Color(0xFFD600));
            centro(g, "UNO", w / 2, 95);
            Card[] leque = {new Card(3, "5"), new Card(2, "SKIP"), new Card(4, "+4"), new Card(1, "+2"), new Card(0, "7")};
            int[] ang = {-32, -16, 0, 16, 32};
            for (int i = 0; i < 5; i++) {
                AffineTransform t = g.getTransform();
                g.rotate(Math.toRadians(ang[i]), w / 2.0, 295);
                desenhaCarta(g, w / 2 - 47, 295 - 136, 94, 136, leque[i], true);
                g.setTransform(t);
            }
        }
    }

    class MenuPanel extends JPanel {
        MenuPanel() {
            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0; c.insets = new Insets(3, 0, 3, 0); c.fill = GridBagConstraints.NONE;
            add(new Header(), c);
            add(rotulo("Seu nome"), c);
            nomeField.setHorizontalAlignment(JTextField.CENTER);
            nomeField.setPreferredSize(new Dimension(300, 26));
            add(nomeField, c);
            c.insets = new Insets(10, 0, 3, 0);
            add(rotulo("Adversários"), c);
            c.insets = new Insets(3, 0, 12, 0);
            JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
            row.setOpaque(false);
            ButtonGroup bg = new ButtonGroup();
            for (int k = 1; k <= 3; k++) {
                JRadioButton r = new JRadioButton(k + (k == 1 ? " bot" : " bots"), k == bots);
                r.setOpaque(false);
                r.setForeground(Color.WHITE);
                final int kk = k;
                r.addActionListener(e -> bots = kk);
                bg.add(r);
                row.add(r);
            }
            add(row, c);
            c.insets = new Insets(4, 0, 4, 0);
            Flat jogar = new Flat("JOGAR", new Color(0xE53935));
            jogar.addActionListener(e -> iniciar());
            Flat regras = new Flat("Regras", new Color(0x1E88E5));
            regras.addActionListener(e -> mostrarRegras());
            Flat sair = new Flat("Sair", new Color(0x333333));
            sair.addActionListener(e -> System.exit(0));
            for (Flat b : new Flat[]{jogar, regras, sair}) {
                b.setPreferredSize(new Dimension(300, 36));
                add(b, c);
            }
        }
        JLabel rotulo(String s) {
            JLabel l = new JLabel(s);
            l.setForeground(new Color(0xFFD600));
            l.setFont(new Font("SansSerif", Font.BOLD, 13));
            return l;
        }
        @Override protected void paintComponent(Graphics g) {
            g.setColor(MESA);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // ---------- mesa ----------
    class Board extends JPanel {
        final List<Rectangle> rects = new ArrayList<>();
        final Rectangle deckRect = new Rectangle();
        int hover = -1;

        Board() {
            setPreferredSize(new Dimension(760, 660));
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { click(e.getPoint()); }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int i = idxAt(e.getPoint());
                    boolean mine = cur == 0 && !over && !hands.isEmpty();
                    boolean hand = mine && i >= 0 && i < hands.get(0).size() && canPlay(hands.get(0).get(i));
                    boolean dk = mine && deckRect.contains(e.getPoint());
                    setCursor(hand || dk ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                    if (i != hover) { hover = i; repaint(); }
                }
            });
        }

        int idxAt(Point p) {
            for (int i = rects.size() - 1; i >= 0; i--) if (rects.get(i).contains(p)) return i;
            return -1;
        }

        int seatOf(int p) { return n == 2 ? 2 : (n == 3 ? (p == 1 ? 1 : 3) : p); }

        void drawBot(Graphics2D g, int p, int W, int H) {
            int cnt = hands.get(p).size(), cw = 44, ch = 60, s = seatOf(p);
            String nm = names[p] + " (" + cnt + ")";
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(cur == p && !over ? new Color(0xFFEB3B) : Color.WHITE);
            if (s == 2) {
                int step = cnt <= 1 ? 0 : Math.min(26, (int) (W * 0.5 - cw) / (cnt - 1));
                int x0 = (W - (cw + step * (cnt - 1))) / 2;
                for (int i = 0; i < cnt; i++) desenhaCarta(g, x0 + i * step, 22, cw, ch, null, false);
                centro(g, nm, W / 2, 22 + ch + 18);
            } else {
                int step = cnt <= 1 ? 0 : Math.min(24, (int) (H * 0.45 - ch) / (cnt - 1));
                int y0 = (H - (ch + step * (cnt - 1))) / 2 - 40;
                int x = s == 1 ? 24 : W - 24 - cw;
                for (int i = 0; i < cnt; i++) desenhaCarta(g, x, y0 + i * step, cw, ch, null, false);
                centro(g, nm, x + cw / 2, y0 - 10);
            }
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int W = getWidth(), H = getHeight();
            g.setColor(MESA);
            g.fillRect(0, 0, W, H);
            if (hands.isEmpty()) return;
            for (int p = 1; p < n; p++) drawBot(g, p, W, H);

            // centro: info, monte, descarte, mensagem
            int cw = 76, ch = 108, cy = H / 2 - 70;
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(Color.WHITE);
            centro(g, "Cor: " + NOME_COR[curColor] + "  |  sentido " + (dir == 1 ? "horário" : "anti-horário"), W / 2, cy - 14);
            deckRect.setBounds(W / 2 - 100, cy, cw, ch);
            desenhaCarta(g, deckRect.x, cy, cw, ch, null, false);
            g.setColor(new Color(0xFFD600));
            centro(g, "Comprar (" + deck.size() + ")", deckRect.x + cw / 2, cy + ch + 18);
            int dx = W / 2 + 24;
            desenhaCarta(g, dx, cy, cw, ch, top(), true);
            g.setColor(COR[curColor]);
            g.setStroke(new BasicStroke(4));
            g.drawRoundRect(dx - 2, cy - 2, cw + 4, ch + 4, 14, 14);
            g.setStroke(new BasicStroke(1));
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            int mw = g.getFontMetrics().stringWidth(msg) + 40;
            int my = cy + ch + 34;
            g.setColor(new Color(0x0F3D20));
            g.fillRoundRect(W / 2 - mw / 2, my, mw, 28, 10, 10);
            g.setColor(Color.WHITE);
            centro(g, msg, W / 2, my + 19);

            // sua mão
            List<Card> h = hands.get(0);
            rects.clear();
            int hw = 72, hh = 104, cnt = h.size();
            int step = cnt <= 1 ? 0 : Math.min(hw + 6, (W - 40 - hw) / (cnt - 1));
            int x0 = (W - (cnt == 0 ? 0 : hw + step * (cnt - 1))) / 2, y0 = H - hh - 14;
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(cur == 0 && !over ? new Color(0xFFEB3B) : Color.WHITE);
            g.drawString(names[0] + " (" + cnt + ")", 14, y0 - 8);
            boolean minha = cur == 0 && !over;
            for (int i = 0; i < cnt; i++) {
                Card c = h.get(i);
                boolean ok = minha && canPlay(c);
                int lift = ok ? (i == hover ? 28 : (destacar.isSelected() ? 14 : 0)) : 0;
                int x = x0 + i * step;
                desenhaCarta(g, x, y0 - lift, hw, hh, c, true);
                if (destacar.isSelected() && minha) {
                    if (ok) {
                        g.setColor(new Color(0xFFEB3B));
                        g.setStroke(new BasicStroke(3));
                        g.drawRoundRect(x, y0 - lift, hw, hh, 12, 12);
                        g.setStroke(new BasicStroke(1));
                    } else {
                        g.setColor(new Color(0, 0, 0, 110));
                        g.fillRoundRect(x, y0, hw, hh, 12, 12);
                    }
                }
                rects.add(new Rectangle(x, y0 - 30, hw, hh + 30));
            }
        }

        void click(Point p) {
            if (over || cur != 0 || hands.isEmpty()) return;
            int i = idxAt(p);
            if (i >= 0 && i < hands.get(0).size()) {
                Card c = hands.get(0).get(i);
                if (!canPlay(c)) { setMsg("Essa carta não pode ser jogada agora."); return; }
                int color = c.color == 4 ? escolherCor() : c.color;
                drewFlag = false;
                playCard(0, c, color);
            } else if (deckRect.contains(p)) {
                humanDraw();
            }
        }
    }

    // ---------- janela ----------
    UnoJogo() {
        super("UNO");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JMenuBar mb = new JMenuBar();
        JMenu jogo = new JMenu("Jogo");
        JMenuItem mNova = new JMenuItem("Nova partida");
        mNova.addActionListener(e -> novaPartida());
        JMenuItem mMenu = new JMenuItem("Menu inicial");
        mMenu.addActionListener(e -> voltarMenu());
        JMenuItem mSair = new JMenuItem("Sair");
        mSair.addActionListener(e -> System.exit(0));
        jogo.add(mNova); jogo.add(mMenu); jogo.addSeparator(); jogo.add(mSair);
        JMenu ajuda = new JMenu("Ajuda");
        JMenuItem mReg = new JMenuItem("Regras");
        mReg.addActionListener(e -> mostrarRegras());
        ajuda.add(mReg);
        mb.add(jogo); mb.add(ajuda);
        setJMenuBar(mb);

        root.add(new MenuPanel(), "menu");
        root.add(montaJogo(), "jogo");
        add(root);
        botTimer.setRepeats(false);
        setMinimumSize(new Dimension(900, 640));
        pack();
        setLocationRelativeTo(null);
        layout.show(root, "menu");
    }

    JPanel montaJogo() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(PAINEL);
        side.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        side.setPreferredSize(new Dimension(200, 600));

        JLabel hl = new JLabel("Histórico");
        hl.setForeground(new Color(0xFFD600));
        hl.setFont(new Font("SansSerif", Font.BOLD, 14));
        for (JTextArea a : new JTextArea[]{historico, placar}) {
            a.setEditable(false);
            a.setLineWrap(true);
            a.setWrapStyleWord(true);
            a.setForeground(Color.WHITE);
            a.setFont(new Font("SansSerif", Font.PLAIN, 12));
        }
        historico.setBackground(new Color(0x0D3B20));
        placar.setOpaque(false);
        JScrollPane sp = new JScrollPane(historico);
        sp.setPreferredSize(new Dimension(180, 220));
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        sp.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        JLabel pl = new JLabel("Placar");
        pl.setForeground(new Color(0xFFD600));
        pl.setFont(new Font("SansSerif", Font.BOLD, 14));
        destacar.setOpaque(false);
        destacar.setForeground(Color.WHITE);
        destacar.addActionListener(e -> board.repaint());

        Flat nova = new Flat("Nova partida", new Color(0x2ECC40));
        nova.addActionListener(e -> novaPartida());
        Flat menu = new Flat("Menu inicial", new Color(0x1E88E5));
        menu.addActionListener(e -> voltarMenu());
        passBtn.addActionListener(e -> {
            if (!over && cur == 0 && drewFlag) {
                drewFlag = false;
                passBtn.setEnabled(false);
                log("Você passou a vez.");
                endTurn(nxt(0));
            }
        });

        unoBtn.setEnabled(false);
        unoBtn.addActionListener(e -> {
            if (over || hands.isEmpty()) return;
            if (hands.get(0).size() > 2) { setMsg("Você só pode dizer UNO com 1 ou 2 cartas na mão."); return; }
            if (unoDito) return;
            unoDito = true;
            unoPendente = false;
            log("Você disse UNO!");
            setMsg("UNO! Você avisou a mesa.");
        });
        Component[] cs = {hl, sp, pl, placar, destacar, unoBtn, nova, menu, passBtn};
        for (Component c : cs) {
            ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
            if (c instanceof Flat) c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            side.add(c);
            side.add(Box.createVerticalStrut(c instanceof Flat ? 8 : 6));
        }
        JPanel game = new JPanel(new BorderLayout());
        game.add(board, BorderLayout.CENTER);
        game.add(side, BorderLayout.EAST);
        return game;
    }

    void mostrarRegras() {
        JTextArea t = new JTextArea(REGRAS);
        t.setEditable(false);
        t.setOpaque(false);
        JOptionPane.showMessageDialog(this, t, "Regras do UNO", JOptionPane.PLAIN_MESSAGE);
    }

    void voltarMenu() {
        botTimer.stop();
        layout.show(root, "menu");
    }

    void iniciar() {
        n = bots + 1;
        names = new String[n];
        names[0] = nomeField.getText().trim().isEmpty() ? "Você" : nomeField.getText().trim();
        for (int i = 1; i < n; i++) names[i] = "Bot " + i;
        wins = new int[n];
        historico.setText("");
        layout.show(root, "jogo");
        novaPartida();
    }

    void setMsg(String s) {
        msg = s;
        if (!hands.isEmpty() && hands.get(0).size() > 2) unoDito = false;
        unoBtn.setEnabled(!over && !hands.isEmpty() && hands.get(0).size() <= 2 && !unoDito);
        board.repaint();
    }

    void log(String s) {
        historico.append(s + "\n");
        historico.setCaretPosition(historico.getDocument().getLength());
    }

    void atualizaPlacar() {
        StringBuilder sb = new StringBuilder();
        for (int p = 0; p < n; p++) sb.append(names[p]).append(": ").append(wins[p]).append(p < n - 1 ? "\n" : "");
        placar.setText(sb.toString());
    }

    // ---------- regras do jogo ----------
    void novaPartida() {
        botTimer.stop();
        deck.clear(); discard.clear(); hands.clear();
        for (int c = 0; c < 4; c++) {
            deck.add(new Card(c, "0"));
            for (int i = 0; i < 2; i++) {
                for (int v = 1; v <= 9; v++) deck.add(new Card(c, String.valueOf(v)));
                deck.add(new Card(c, "SKIP"));
                deck.add(new Card(c, "REV"));
                deck.add(new Card(c, "+2"));
            }
        }
        for (int i = 0; i < 4; i++) { deck.add(new Card(4, "WILD")); deck.add(new Card(4, "+4")); }
        Collections.shuffle(deck);
        for (int p = 0; p < n; p++) {
            List<Card> h = new ArrayList<>();
            for (int i = 0; i < 7; i++) h.add(deck.remove(deck.size() - 1));
            hands.add(h);
        }
        Card first = null;
        while (first == null) {
            Card c = deck.remove(deck.size() - 1);
            if (c.color == 4 || !Character.isDigit(c.value.charAt(0))) deck.add(0, c); else first = c;
        }
        discard.add(first);
        curColor = first.color;
        cur = 0; dir = 1; over = false; drewFlag = false;
        unoDito = false; unoPendente = false;
        board.hover = -1;
        sortHand();
        passBtn.setEnabled(false);
        log("--- Nova partida ---");
        atualizaPlacar();
        setMsg("Sua vez! Jogue uma carta ou compre do baralho.");
    }

    void sortHand() {
        hands.get(0).sort(Comparator.comparingInt((Card c) -> c.color).thenComparing((Card c) -> c.value));
    }

    int nxt(int p) { return ((p + dir) % n + n) % n; }

    Card top() { return discard.get(discard.size() - 1); }

    boolean canPlay(Card c) {
        return c.color == 4 || c.color == curColor || c.value.equals(top().value);
    }

    Card draw() {
        if (deck.isEmpty()) {
            Card t = discard.remove(discard.size() - 1);
            deck.addAll(discard);
            discard.clear();
            discard.add(t);
            Collections.shuffle(deck);
        }
        return deck.isEmpty() ? null : deck.remove(deck.size() - 1);
    }

    void give(int p, int count) {
        for (int i = 0; i < count; i++) { Card c = draw(); if (c != null) hands.get(p).add(c); }
    }

    int escolherCor() {
        int r = JOptionPane.showOptionDialog(this, "Escolha a nova cor:", "Coringa",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, NOME_COR, NOME_COR[0]);
        return r < 0 ? 0 : r;
    }

    void humanDraw() {
        if (drewFlag) { setMsg("Você já comprou. Jogue a carta ou passe a vez."); return; }
        Card c = draw();
        if (c == null) { endTurn(nxt(0)); return; }
        hands.get(0).add(c);
        sortHand();
        if (canPlay(c)) {
            drewFlag = true;
            passBtn.setEnabled(true);
            log("Você comprou uma carta.");
            setMsg("Você comprou uma carta jogável. Jogue-a ou passe a vez.");
        } else {
            log("Você comprou e passou a vez.");
            setMsg("Você comprou e não pôde jogar.");
            endTurn(nxt(0));
        }
    }

    void playCard(int p, Card c, int color) {
        hands.get(p).remove(c);
        discard.add(c);
        curColor = color;
        passBtn.setEnabled(false);
        String info = names[p] + " jogou " + (c.color == 4 ? "coringa" : NOME_COR[c.color]) + " " + c.label();
        if (hands.get(p).isEmpty()) { log(info); fim(p); return; }
        if (p == 0) {
            if (hands.get(0).size() == 1) unoPendente = !unoDito;
            else { unoPendente = false; unoDito = false; }
        }
        int next = nxt(p), after;
        switch (c.value) {
            case "SKIP":
                after = nxt(next); info += " (" + names[next] + " perdeu a vez)"; break;
            case "REV":
                dir = -dir; after = n == 2 ? p : nxt(p); info += " (sentido invertido)"; break;
            case "+2":
                give(next, 2); after = nxt(next); info += " (" + names[next] + " comprou 2)"; break;
            case "+4":
                give(next, 4); after = nxt(next); info += " (" + names[next] + " comprou 4)"; break;
            default:
                after = next;
        }
        if (c.color == 4) info += " -> " + NOME_COR[color];
        if (hands.get(p).size() == 1) info += " | " + names[p] + ": UNO!";
        sortHand();
        log(info);
        endTurn(after);
    }

    void fim(int p) {
        over = true;
        wins[p]++;
        atualizaPlacar();
        setMsg(names[p] + " venceu a partida!");
        log(names[p] + " venceu!");
        String txt = p == 0 ? "Parabéns, você venceu!" : names[p] + " venceu a partida.";
        int r = JOptionPane.showOptionDialog(this, txt, "Fim da partida", JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, new String[]{"Nova partida", "Menu inicial"}, "Nova partida");
        if (r == 0) novaPartida(); else voltarMenu();
    }

    void endTurn(int next) {
        cur = next;
        if (unoPendente) setMsg("Rápido! Clique em UNO! antes que o próximo jogue.");
        else if (cur == 0) setMsg("Sua vez! Jogue uma carta ou compre do baralho.");
        else setMsg("Vez de " + names[cur] + "...");
        if (!over && cur != 0) {
            int d = unoPendente ? 2500 : 1000;
            botTimer.setInitialDelay(d);
            botTimer.setDelay(d);
            botTimer.restart();
        }
    }

    void botTurn() {
        if (over || cur == 0) return;
        if (unoPendente) {
            unoPendente = false;
            unoDito = false;
            give(0, 2);
            sortHand();
            log("Você esqueceu de dizer UNO! e comprou 2 cartas.");
        }
        int p = cur;
        List<Card> h = hands.get(p);
        Card pick = choose(h);
        if (pick == null) {
            Card d = draw();
            if (d != null) {
                h.add(d);
                if (canPlay(d)) pick = d;
            }
        }
        if (pick == null) {
            log(names[p] + " comprou e passou a vez.");
            endTurn(nxt(p));
            return;
        }
        playCard(p, pick, pick.color == 4 ? melhorCor(h) : pick.color);
    }

    Card choose(List<Card> h) {
        Card wild = null;
        for (Card c : h) {
            if (!canPlay(c)) continue;
            if (c.color != 4) return c;
            if (wild == null) wild = c;
        }
        return wild;
    }

    int melhorCor(List<Card> h) {
        int[] cnt = new int[4];
        for (Card c : h) if (c.color < 4) cnt[c.color]++;
        int best = 0;
        for (int i = 1; i < 4; i++) if (cnt[i] > cnt[best]) best = i;
        return best;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UnoJogo().setVisible(true));
    }
}
