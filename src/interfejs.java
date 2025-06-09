import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.io.*;
import java.util.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.List;

public class interfejs extends JFrame {
    private GraphPanel graphPanel; // Panel do wyświetlania grafu
    private JPanel controlPanel;   // Panel kontrolny z przyciskami
    private JSpinner partitionSpinner; // Spinner do wyboru liczby części
    private JSpinner marginSpinner;    // Spinner do wyboru marginesu
    private Graph currentGraph;    // Aktualnie wczytany graf
    private int[] currentAssignments; // Aktualne przypisania węzłów do grup
    private int currentParts;      // Liczba aktualnych części

    public interfejs() {
        initUI(); // Inicjalizacja interfejsu użytkownika
    }

    private void initUI() {
        setTitle("Podział Grafu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        createMenuBar();    // Tworzenie paska menu
        createMainContent(); // Tworzenie głównej zawartości okna

        setVisible(true);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Menu "Wczytaj"
        JMenu loadMenu = new JMenu("Wczytaj");
        JMenuItem loadCsrrgItem = new JMenuItem("CSRRG");
        JMenuItem loadTxtItem = new JMenuItem("TXT z podziałem");
        loadCsrrgItem.addActionListener(e -> loadGraph("csrrg")); // Obsługa wczytywania pliku CSRRG
        loadTxtItem.addActionListener(e -> loadGraph("txt"));     // Obsługa wczytywania pliku TXT
        loadMenu.add(loadCsrrgItem);
        loadMenu.add(loadTxtItem);

        // Menu "Zapisz"
        JMenu saveMenu = new JMenu("Zapisz");
        JMenuItem saveTextItem = new JMenuItem("Jako tekstowy (.txt)");
        JMenuItem saveBinaryItem = new JMenuItem("Jako binarny (.bin)");
        JMenuItem saveCsrrgItem = new JMenuItem("Jako CSRRG (.csrrg)");

        saveTextItem.addActionListener(e -> saveAssignments("txt")); // Zapisywanie przypisań jako TXT
        saveBinaryItem.addActionListener(e -> saveGraph("bin"));    // Zapisywanie grafu jako BIN
        saveCsrrgItem.addActionListener(e -> saveGraph("csrrg"));  // Zapisywanie grafu jako CSRRG

        saveMenu.add(saveTextItem);
        saveMenu.add(saveBinaryItem);
        saveMenu.add(saveCsrrgItem);

        menuBar.add(loadMenu);
        menuBar.add(saveMenu);
        setJMenuBar(menuBar);
    }

    private void loadGraph(String extension) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Wybierz plik");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Pliki (*." + extension + ")", extension));

        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = fileChooser.getSelectedFile();
                currentGraph = readInputFile(selectedFile.getAbsolutePath()); // Wczytywanie grafu z pliku

                // Obliczanie stopni węzłów
                int nodeCount = currentGraph.nodeIndices.size();
                int groupCount = currentGraph.groupPointers.size() - 1;
                int totalGroupElements = currentGraph.groups.size();
                currentGraph.calculateDegrees(nodeCount, groupCount, totalGroupElements);

                String message = "Graf został wczytany pomyślnie!\nLiczba węzłów: " + nodeCount;

                if (extension.equals("txt") && currentAssignments != null) {
                    message += "\nWczytano podział na " + currentParts + " grup";
                }

                JOptionPane.showMessageDialog(this, message, "Sukces", JOptionPane.INFORMATION_MESSAGE);

                // Aktywacja przycisku podziału tylko jeśli nie wczytano gotowego podziału
                ((JButton)controlPanel.getComponent(2)).setEnabled(currentAssignments == null);

                graphPanel.repaint(); // Odświeżenie panelu grafu
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Błąd podczas wczytywania pliku: " + e.getMessage(),
                        "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveGraph(String extension) {
        if (currentGraph == null) {
            JOptionPane.showMessageDialog(this,
                    "Najpierw wczytaj graf!",
                    "Błąd", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Zapisz graf");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Pliki (*." + extension + ")", extension));

        int returnValue = fileChooser.showSaveDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            try {
                File fileToSave = fileChooser.getSelectedFile();
                String filePath = fileToSave.getAbsolutePath();
                if (!filePath.toLowerCase().endsWith("." + extension)) {
                    filePath += "." + extension;
                }

                if (extension.equals("csrrg")) {
                    saveCSRRG(filePath, currentGraph, currentAssignments, currentParts); // Zapisywanie jako CSRRG
                } else {
                    saveBinary(filePath, currentGraph, currentAssignments, currentParts); // Zapisywanie jako BIN
                }

                JOptionPane.showMessageDialog(this,
                        "Graf został zapisany pomyślnie!",
                        "Sukces", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Błąd podczas zapisywania pliku: " + e.getMessage(),
                        "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveAssignments(String extension) {
        if (currentAssignments == null) {
            JOptionPane.showMessageDialog(this,
                    "Najpierw podziel graf!",
                    "Błąd", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Zapisz przypisania");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Pliki (*." + extension + ")", extension));

        int returnValue = fileChooser.showSaveDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            try {
                File fileToSave = fileChooser.getSelectedFile();
                String filePath = fileToSave.getAbsolutePath();
                if (!filePath.toLowerCase().endsWith("." + extension)) {
                    filePath += "." + extension;
                }

                saveAssignmentsFile(filePath, currentGraph.groupAssignments, currentParts); // Zapisywanie przypisań
                JOptionPane.showMessageDialog(this,
                        "Przypisania zostały zapisane pomyślnie!",
                        "Sukces", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Błąd podczas zapisywania pliku: " + e.getMessage(),
                        "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void createMainContent() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Panel wyświetlania grafu
        graphPanel = new GraphPanel();
        graphPanel.setBackground(Color.WHITE);
        graphPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JScrollPane scrollPane = new JScrollPane(graphPanel);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel kontrolny
        controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Parametry podziału"));

        JPanel partitionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        partitionsPanel.add(new JLabel("Liczba części:"));
        partitionSpinner = new JSpinner(new SpinnerNumberModel(2, 2, 100, 1)); // Spinner do wyboru liczby części
        partitionsPanel.add(partitionSpinner);

        JPanel marginPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        marginPanel.add(new JLabel("Margines (w %):"));
        marginSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 100, 1)); // Spinner do wyboru marginesu
        marginPanel.add(marginSpinner);

        JButton partitionButton = new JButton("Podziel graf"); // Przycisk do podziału grafu
        partitionButton.setEnabled(false);
        partitionButton.addActionListener(e -> partitionGraph());

        controlPanel.add(partitionsPanel);
        controlPanel.add(marginPanel);
        controlPanel.add(partitionButton);

        mainPanel.add(graphPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.EAST);

        add(mainPanel);
    }

    private void partitionGraph() {
        if (currentGraph == null) {
            JOptionPane.showMessageDialog(this,
                    "Najpierw wczytaj graf!",
                    "Błąd", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int parts = (int) partitionSpinner.getValue(); // Pobranie liczby części
        int margin = (int) marginSpinner.getValue();   // Pobranie marginesu
        int nodeCount = currentGraph.nodeIndices.size();

        if (parts <= 1 || parts > nodeCount) {
            JOptionPane.showMessageDialog(this,
                    "Nieprawidłowa liczba części!",
                    "Błąd", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (margin < 0 || margin > 100) {
            JOptionPane.showMessageDialog(this,
                    "Margines musi być między 0 a 100!",
                    "Błąd", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Budowanie macierzy Laplace'a
            LaplaceMatrix L = LaplaceMatrix.buildLaplaceMatrix(currentGraph, nodeCount);

            // Podział grafu
            currentAssignments = new int[nodeCount];
            int[] indices = new int[nodeCount];
            for (int i = 0; i < nodeCount; i++) indices[i] = i;

            GraphPartitioner.recursivePartition(L, indices, nodeCount, currentAssignments, 0, parts, margin);
            GraphPartitioner.refineBalancing(L, currentAssignments, nodeCount, parts, margin);

            currentParts = parts;
            currentGraph.setGroupAssignments(currentAssignments);

            // Wyświetlenie wyników
            StringBuilder message = new StringBuilder("Graf podzielony na " + parts + " części (margines " + margin + "%)\n");
            for (int i = 0; i < Math.min(10, nodeCount); i++) {
                message.append("Węzeł ").append(i).append(" => część ").append(currentAssignments[i]).append("\n");
            }
            if (nodeCount > 10) {
                message.append("... i ").append(nodeCount - 10).append(" więcej");
            }

            JOptionPane.showMessageDialog(this,
                    message.toString(),
                    "Wynik podziału", JOptionPane.INFORMATION_MESSAGE);

            graphPanel.repaint(); // Odświeżenie panelu grafu
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Błąd podczas podziału grafu: " + e.getMessage(),
                    "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Metoda wczytująca graf z pliku
    private Graph readInputFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;

            // Wczytywanie maxNodesInRow
            line = reader.readLine();
            int maxNodesInRow = Integer.parseInt(line.trim());

            // Wczytywanie nodeIndices
            line = reader.readLine();
            String[] nodeIndicesStr = line.split(";");
            int nodeCount = nodeIndicesStr.length;
            Graph graph = new Graph(maxNodesInRow, nodeCount);
            for (String s : nodeIndicesStr) {
                graph.nodeIndices.add(Integer.parseInt(s.trim()));
            }

            // Wczytywanie rowPointers
            line = reader.readLine();
            String[] rowPointersStr = line.split(";");
            for (String s : rowPointersStr) {
                graph.rowPointers.add(Integer.parseInt(s.trim()));
            }

            // Wczytywanie groups
            line = reader.readLine();
            String[] groupsStr = line.split(";");
            for (String s : groupsStr) {
                graph.groups.add(Integer.parseInt(s.trim()));
            }

            // Wczytywanie groupPointers
            line = reader.readLine();
            String[] groupPointersStr = line.split(";");
            for (String s : groupPointersStr) {
                graph.groupPointers.add(Integer.parseInt(s.trim()));
            }

            // Dodatkowa obsługa plików TXT z przypisaniami grup
            if (filename.toLowerCase().endsWith(".txt")) {
                // Pomijanie pustej linii
                reader.readLine();

                // Wczytywanie przypisań grup
                line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    String[] assignments = line.trim().split("\\s+");
                    currentAssignments = new int[assignments.length];
                    for (int i = 0; i < assignments.length; i++) {
                        currentAssignments[i] = Integer.parseInt(assignments[i]);
                    }
                    currentParts = Arrays.stream(currentAssignments).max().getAsInt() + 1;
                    graph.setGroupAssignments(currentAssignments);
                }
            }

            return graph;
        }
    }

    // Metoda zapisująca przypisania do pliku tekstowego
    private void saveAssignmentsFile(String filename, List<Integer> assignments, int parts) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Number of groups: " + parts);
            for (int i = 0; i < assignments.size(); i++) {
                writer.println("Node " + i + " => group " + assignments.get(i));
            }
        }
    }

    // Metoda zapisująca graf w formacie CSRRG
    private void saveCSRRG(String filename, Graph graph, int[] assignments, int parts) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Sekcja 1: Opis grafu (identyczny jak format wejściowy)
            writer.println(graph.maxNodesInRow);
            writer.println(String.join(";",
                    graph.nodeIndices.stream().map(String::valueOf).toArray(String[]::new)));
            writer.println(String.join(";",
                    graph.rowPointers.stream().map(String::valueOf).toArray(String[]::new)));
            writer.println(String.join(";",
                    graph.groups.stream().map(String::valueOf).toArray(String[]::new)));
            writer.println(String.join(";",
                    graph.groupPointers.stream().map(String::valueOf).toArray(String[]::new)));

            // Sekcja 2: Opis podziału
            writer.println("# Number of groups: " + parts);
            StringBuilder partitionLine = new StringBuilder();
            for (int assignment : assignments) {
                partitionLine.append(assignment).append(" ");
            }
            writer.println(partitionLine.toString().trim());
        }
    }

    // Metoda zapisująca graf w formacie binarnym
    private void saveBinary(String filename, Graph graph, int[] assignments, int parts) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(filename))) {
            // Sekcja 1: Opis grafu
            out.writeInt(graph.maxNodesInRow);
            out.writeInt(graph.nodeIndices.size());
            out.writeInt(graph.groupPointers.size() - 1);

            for (int index : graph.nodeIndices) out.writeInt(index);
            for (int ptr : graph.rowPointers) out.writeInt(ptr);
            for (int group : graph.groups) out.writeInt(group);
            for (int ptr : graph.groupPointers) out.writeInt(ptr);

            // Sekcja 2: Opis podziału
            out.writeInt(parts);  // Zapisanie liczby grup
            for (int assignment : assignments) {
                out.writeInt(assignment);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new interfejs()); // Uruchomienie interfejsu
    }

    // Klasa wewnętrzna reprezentująca panel grafu
    private class GraphPanel extends JPanel {
        private double scale = 1.0;          // Skala wyświetlania
        private double translateX = 0;       // Przesunięcie w osi X
        private double translateY = 0;       // Przesunięcie w osi Y
        private Point dragStart;             // Punkt początkowy przeciągania
        private List<Point> nodePositions;   // Pozycje węzłów
        private List<Line2D> connections;    // Połączenia między węzłami

        public GraphPanel() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    dragStart = e.getPoint(); // Zapisanie punktu początkowego przeciągania
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    dragStart = null; // Zakończenie przeciągania
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragStart != null) {
                        Point dragEnd = e.getPoint();
                        translateX += dragEnd.x - dragStart.x; // Aktualizacja przesunięcia X
                        translateY += dragEnd.y - dragStart.y; // Aktualizacja przesunięcia Y
                        dragStart = dragEnd;
                        repaint(); // Odświeżenie panelu
                    }
                }
            });

            addMouseWheelListener(e -> {
                double scaleFactor = e.getWheelRotation() < 0 ? 1.1 : 0.9; // Zmiana skali
                scale *= scaleFactor;
                repaint(); // Odświeżenie panelu
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            AffineTransform oldTransform = g2d.getTransform();
            g2d.translate(translateX, translateY); // Zastosowanie przesunięcia
            g2d.scale(scale, scale);              // Zastosowanie skali

            if (currentGraph == null) {
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.drawString("Wczytaj graf, aby wyświetlić wizualizację",
                        getWidth()/2 - 100, getHeight()/2);
            } else {
                drawGraph(g2d); // Rysowanie grafu
            }

            g2d.setTransform(oldTransform); // Przywrócenie transformacji
        }

        private void drawGraph(Graphics2D g2d) {
            int nodeCount = currentGraph.nodeIndices.size();
            int gridSize = (int) Math.ceil(Math.sqrt(nodeCount));
            int nodeDiameter = 30; // Średnica węzła
            int margin = 20;       // Margines

            // Obliczanie pozycji węzłów
            nodePositions = new ArrayList<>(nodeCount);
            for (int i = 0; i < nodeCount; i++) {
                int row = i / gridSize;
                int col = i % gridSize;
                int x = margin + col * (nodeDiameter + margin);
                int y = margin + row * (nodeDiameter + margin);
                nodePositions.add(new Point(x + nodeDiameter/2, y + nodeDiameter/2));
            }

            // Rysowanie połączeń
            if (connections == null) {
                connections = new ArrayList<>();
                Set<String> drawnConnections = new HashSet<>(); // Unikanie duplikatów

                // Znajdowanie wszystkich połączeń między węzłami
                for (int i = 0; i < nodeCount; i++) {
                    for (int g = 0; g < currentGraph.groupPointers.size() - 1; g++) {
                        int groupStart = currentGraph.groupPointers.get(g);
                        int groupEnd = currentGraph.groupPointers.get(g + 1);

                        // Sprawdzenie, czy węzeł i należy do grupy
                        boolean nodeInGroup = false;
                        for (int j = groupStart; j < groupEnd; j++) {
                            if (currentGraph.groups.get(j) == i) {
                                nodeInGroup = true;
                                break;
                            }
                        }

                        if (nodeInGroup) {
                            // Dodanie połączeń do innych węzłów w grupie
                            for (int j = groupStart; j < groupEnd; j++) {
                                int neighbor = currentGraph.groups.get(j);
                                if (neighbor != i) {
                                    String connectionKey = Math.min(i, neighbor) + "-" + Math.max(i, neighbor);
                                    if (!drawnConnections.contains(connectionKey)) {
                                        Point p1 = nodePositions.get(i);
                                        Point p2 = nodePositions.get(neighbor);
                                        connections.add(new Line2D.Double(p1, p2));
                                        drawnConnections.add(connectionKey);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Rysowanie połączeń
            g2d.setColor(Color.BLACK);
            for (Line2D line : connections) {
                g2d.draw(line);
            }

            Color[] partColors = generateColors(currentParts); // Generowanie kolorów dla części

            // Rysowanie węzłów
            for (int i = 0; i < nodeCount; i++) {
                Point pos = nodePositions.get(i);
                int x = pos.x - nodeDiameter/2;
                int y = pos.y - nodeDiameter/2;

                int part = currentAssignments != null ? currentAssignments[i] : 0;
                g2d.setColor(partColors[part]);
                g2d.fill(new Ellipse2D.Double(x, y, nodeDiameter, nodeDiameter));

                g2d.setColor(Color.BLACK);
                g2d.draw(new Ellipse2D.Double(x, y, nodeDiameter, nodeDiameter));

                g2d.setColor(Color.WHITE);
                FontMetrics fm = g2d.getFontMetrics();
                String label = String.valueOf(i);
                int labelX = x + (nodeDiameter - fm.stringWidth(label)) / 2;
                int labelY = y + ((nodeDiameter - fm.getHeight()) / 2) + fm.getAscent();
                g2d.drawString(label, labelX, labelY);
            }

            if (currentAssignments != null) {
                drawLegend(g2d, partColors, margin, nodeDiameter); // Rysowanie legendy
            }
        }

        // Metoda rysująca legendę
        private void drawLegend(Graphics2D g2d, Color[] colors, int margin, int nodeDiameter) {
            int legendX = margin;
            int legendY = margin;
            int legendItemHeight = 20;

            g2d.setColor(Color.BLACK);
            g2d.drawString("Legenda:", legendX, legendY);

            for (int i = 0; i < colors.length; i++) {
                g2d.setColor(colors[i]);
                g2d.fillRect(legendX, legendY + (i+1)*legendItemHeight, 15, 15);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(legendX, legendY + (i+1)*legendItemHeight, 15, 15);
                g2d.drawString("Część " + i, legendX + 20, legendY + (i+1)*legendItemHeight + 12);
            }
        }

        // Metoda generująca kolory dla części
        private Color[] generateColors(int count) {
            Color[] colors = new Color[count];
            float hueStep = 1.0f / count;

            for (int i = 0; i < count; i++) {
                colors[i] = Color.getHSBColor(i * hueStep, 0.7f, 0.8f);
            }
            return colors;
        }
    }
}