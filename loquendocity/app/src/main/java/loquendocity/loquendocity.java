package loquendocity;

import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.io.*;
import java.util.*;
import java.util.List;

public class loquendocity extends JFrame {

    // Orden dentro de cada pétalo: Y (arriba), X (izquierda), B (derecha), A (abajo)
    static final String[][] LAYOUT = {
        {"b", "a", "c", "d"},
        {"f", "e", "g", "h"},
        {"j", "i", "k", "l"},
        {"n", "m", "o", "p"},
        {"r", "q", "s", "t"},
        {"v", "u", "w", "x"},
        {"z", "y", ",", " "},        // " " = espacio (rectángulo blanco)
        {"00", "01", "11", "10"}     // mensajes personalizables
    };
    static final Color[] BTN_COLORS = {
        new Color(0xC8960C), new Color(0x0A4C9A), new Color(0xCC2200), new Color(0x4A6A1E)
    };

    final JTextArea texto = new JTextArea(3, 30);
    final JLabel estado = new JLabel("Conecta un mando...");
    final JComboBox<String> voces = new JComboBox<>();
    final JSlider velocidad = new JSlider(-10, 10, 0);
    final JTextField[] mensajes = new JTextField[4];
    final Map<String, String> vozAPowerShell = new HashMap<>();
    final Rueda rueda = new Rueda();

    ControllerManager manager;
    int seleccion = -1;

    public loquendocity() {
        super("Teclado de rueda + voz");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        texto.setFont(new Font("SansSerif", Font.PLAIN, 20));
        add(new JScrollPane(texto), BorderLayout.NORTH);
        add(rueda, BorderLayout.CENTER);

        JPanel abajo = new JPanel(new GridLayout(0, 1, 4, 4));
        JPanel filaVoz = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaVoz.add(new JLabel("Voz:"));
        filaVoz.add(voces);
        filaVoz.add(new JLabel("Velocidad:"));
        filaVoz.add(velocidad);
        JButton hablar = new JButton("Hablar (L3)");
        hablar.addActionListener(e -> enviar());
        filaVoz.add(hablar);
        abajo.add(filaVoz);

        JPanel filaMsg = new JPanel(new FlowLayout(FlowLayout.LEFT));
        String[] etiquetas = LAYOUT[7];
        String[] porDefecto = {"Hola", "Gracias", "Sí", "No"};
        for (int i = 0; i < 4; i++) {
            mensajes[i] = new JTextField(porDefecto[i], 9);
            filaMsg.add(new JLabel(etiquetas[i] + ":"));
            filaMsg.add(mensajes[i]);
        }
        abajo.add(filaMsg);
        abajo.add(estado);
        add(abajo, BorderLayout.SOUTH);

        cargarVoces();

        try {
            manager = new ControllerManager();
            manager.initSDLGamepad();
        } catch (Throwable t) {
            estado.setText("No se pudo iniciar el mando: " + t);
        }
        new javax.swing.Timer(16, e -> leerMando()).start();

        setSize(640, 900);
        setLocationRelativeTo(null);
    }

    // ---------- Mando ----------
    void leerMando() {
        if (manager == null) return;
        ControllerState s = manager.getState(0);
        if (!s.isConnected) {
            estado.setText("Conecta un mando...");
            seleccion = -1;
            rueda.repaint();
            return;
        }
        estado.setText("Mando conectado");

        float x = s.leftStickX, y = s.leftStickY; // y positivo = arriba (si te sale al revés, invierte)
        if (Math.hypot(x, y) < 0.5) {
            seleccion = -1;
        } else {
            double ang = Math.toDegrees(Math.atan2(x, y));
            if (ang < 0) ang += 360;
            seleccion = ((int) Math.round(ang / 45.0)) % 8;
        }

        if (seleccion >= 0) {
            if (s.yJustPressed) escribir(seleccion, 0);
            if (s.xJustPressed) escribir(seleccion, 1);
            if (s.bJustPressed) escribir(seleccion, 2);
            if (s.aJustPressed) escribir(seleccion, 3);
        }
        if (s.lbJustPressed) {
            String t = texto.getText();
            if (!t.isEmpty()) texto.setText(t.substring(0, t.length() - 1));
        }
        if (s.rbJustPressed) texto.setText("");
        if (s.leftStickJustClicked) enviar();

        rueda.repaint();
    }

    void escribir(int petalo, int boton) {
        if (petalo == 7) {
            texto.append(mensajes[boton].getText() + " ");
        } else {
            texto.append(LAYOUT[petalo][boton]);
        }
    }

    // ---------- Voz (SAPI5 vía PowerShell) ----------
    static String ps64() { return "powershell"; }
    static String ps32() {
        File f = new File("C:\\Windows\\SysWOW64\\WindowsPowerShell\\v1.0\\powershell.exe");
        return f.exists() ? f.getAbsolutePath() : null;
    }

    void cargarVoces() {
        String script = "Add-Type -AssemblyName System.Speech;"
            + "(New-Object System.Speech.Synthesis.SpeechSynthesizer).GetInstalledVoices()"
            + "| ForEach-Object { $_.VoiceInfo.Name }";
        List<String> hosts = new ArrayList<>();
        hosts.add(ps64());
        if (ps32() != null) hosts.add(ps32()); // las voces Loquendo suelen ser de 32 bits
        for (String host : hosts) {
            try {
                Process p = new ProcessBuilder(host, "-NoProfile", "-Command", script)
                        .redirectErrorStream(true).start();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String l;
                    while ((l = r.readLine()) != null) {
                        l = l.trim();
                        if (!l.isEmpty() && !vozAPowerShell.containsKey(l)) {
                            vozAPowerShell.put(l, host);
                            voces.addItem(l);
                        }
                    }
                }
            } catch (IOException ignored) { }
        }
        if (voces.getItemCount() == 0) estado.setText("No se encontraron voces (¿Windows?)");
    }

    void enviar() {
        String t = texto.getText().trim();
        if (t.isEmpty() || voces.getSelectedItem() == null) return;
        String voz = (String) voces.getSelectedItem();
        String host = vozAPowerShell.get(voz);
        int vel = velocidad.getValue();
        texto.setText(""); // listo para escribir otro texto

        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(host, "-NoProfile", "-Command",
                    "Add-Type -AssemblyName System.Speech;"
                    + "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer;"
                    + "$s.SelectVoice($env:TTS_VOICE);"
                    + "$s.Rate = [int]$env:TTS_RATE;"
                    + "$s.Speak($env:TTS_TEXT);");
                pb.environment().put("TTS_VOICE", voz);
                pb.environment().put("TTS_RATE", String.valueOf(vel));
                pb.environment().put("TTS_TEXT", t);
                pb.redirectErrorStream(true);
                pb.start().waitFor();
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> estado.setText("Error de voz: " + ex.getMessage()));
            }
        }).start();
    }

    // ---------- Rueda ----------
    class Rueda extends JPanel {
        Rueda() { setBackground(new Color(0x16466B)); setPreferredSize(new Dimension(600, 600)); }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int cx = getWidth() / 2, cy = getHeight() / 2;
            int R = Math.min(getWidth(), getHeight()) / 2 - 10;

            g.setColor(new Color(0x052B43));
            g.fill(new Ellipse2D.Double(cx - R, cy - R, 2 * R, 2 * R));

            for (int i = 0; i < 8; i++) {
                double start = 90 - 45 * i - 22.5;
                Arc2D arco = new Arc2D.Double(cx - R + 6, cy - R + 6, 2 * R - 12, 2 * R - 12, start, 45, Arc2D.PIE);
                g.setColor(i == seleccion ? new Color(0x2F6A8C) : new Color(0x0A3A56));
                g.fill(arco);
                g.setColor(new Color(0x052B43));
                g.setStroke(new BasicStroke(4));
                g.draw(arco);
            }

            // centro "L3"
            int rc = (int) (R * 0.2);
            g.setColor(new Color(0x555555));
            g.fillOval(cx - rc, cy - rc, 2 * rc, 2 * rc);
            g.setColor(Color.LIGHT_GRAY);
            g.setFont(new Font("SansSerif", Font.PLAIN, 20));
            centrado(g, "LS", cx, cy);

            double d = R * 0.12;
            double[][] off = {{0, -d}, {-d, 0}, {d, 0}, {0, d}};
            for (int i = 0; i < 8; i++) {
                double th = Math.toRadians(45 * i);
                double px = cx + R * 0.62 * Math.sin(th);
                double py = cy - R * 0.62 * Math.cos(th);
                for (int b = 0; b < 4; b++) {
                    int lx = (int) (px + off[b][0]), ly = (int) (py + off[b][1]);
                    String lab = LAYOUT[i][b];
                    boolean activo = (i == seleccion);
                    if (activo) {
                        g.setColor(BTN_COLORS[b]);
                        g.fillOval(lx - 17, ly - 17, 34, 34);
                    }
                    g.setColor(activo ? Color.WHITE : new Color(0xBBBBBB));
                    g.setFont(new Font("SansSerif", Font.BOLD, 20));
                    if (lab.equals(" ")) g.fillRect(lx - 14, ly - 7, 28, 14);
                    else centrado(g, lab, lx, ly);
                }
            }
        }

        void centrado(Graphics2D g, String s, int x, int y) {
            FontMetrics fm = g.getFontMetrics();
            g.drawString(s, x - fm.stringWidth(s) / 2, y + (fm.getAscent() - fm.getDescent()) / 2);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new loquendocity().setVisible(true));
    }
}