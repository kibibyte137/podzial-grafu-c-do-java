import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.*;
import java.util.List;
import java.awt.event.*;
import java.awt.geom.AffineTransform;

public class interfejs extends JFrame {
    private GraphPanel graphPanel;
    private JPanel controlPanel;
    private JSpinner partitionSpinner;
    private JSpinner marginSpinner;
    private Graph currentGraph;
    private int[] currentAssignments;
    private int currentParts;

    public interfejs() {
        initUI();
    }

    private void initUI() {
        setTitle("Podział Grafu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        createMenuBar();
        createMainContent();

        setVisible(true);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Menu "Wczytaj"
        JMenu loadMenu = new JMenu("Wczytaj");
        JMenuItem loadCsrrgItem = new JMenuItem("CSRRG");
        loadCsrrgItem.addActionListener(e -> loadGraph("csrrg"));
        loadMenu.add(loadCsrrgItem);

        // Menu "Zapisz"
        JMenu saveMenu = new JMenu("Zapisz");
        JMenuItem saveTextItem = new JMenuItem("Jako tekstowy (.txt)");
        JMenuItem saveBinaryItem = new JMenuItem("Jako binarny (.bin)");
        JMenuItem saveCsrrgItem = new JMenuItem("Jako CSRRG (.csrrg)");

        saveTextItem.addActionListener(e -> saveAssignments("txt"));
        saveBinaryItem.addActionListener(e -> saveGraph("bin"));
        saveCsrrgItem.addActionListener(e -> saveGraph("csrrg"));

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
                currentGraph = readInputFile(selectedFile.getAbsolutePath());

                // Calculate degrees
                int nodeCount = currentGraph.nodeIndices.size();
                int groupCount = currentGraph.groupPointers.size() - 1;
                int totalGroupElements = currentGraph.groups.size();
                currentGraph.calculateDegrees(nodeCount, groupCount, totalGroupElements);

                JOptionPane.showMessageDialog(this,
                        "Graf został wczytany pomyślnie!\nLiczba węzłów: " + nodeCount,
                        "Sukces", JOptionPane.INFORMATION_MESSAGE);

                // Enable partition button
                ((JButton)controlPanel.getComponent(2)).setEnabled(true);
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
                    saveCSRRG(filePath, currentGraph, currentAssignments, currentParts);
                } else {
                    saveBinary(filePath, currentGraph, currentAssignments, currentParts);
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

                saveAssignmentsFile(filePath, currentGraph.groupAssignments, currentParts);
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
        partitionSpinner = new JSpinner(new SpinnerNumberModel(2, 2, 100, 1));
        partitionsPanel.add(partitionSpinner);

        JPanel marginPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        marginPanel.add(new JLabel("Margines (w %):"));
        marginSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 100, 1));
        marginPanel.add(marginSpinner);

        JButton partitionButton = new JButton("Podziel graf");
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

        int parts = (int) partitionSpinner.getValue();
        int margin = (int) marginSpinner.getValue();
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
            // Build Laplace matrix
            LaplaceMatrix L = LaplaceMatrix.buildLaplaceMatrix(currentGraph, nodeCount);

            // Partition graph
            currentAssignments = new int[nodeCount];
            int[] indices = new int[nodeCount];
            for (int i = 0; i < nodeCount; i++) indices[i] = i;

            GraphPartitioner.recursivePartition(L, indices, nodeCount, currentAssignments, 0, parts, margin);
            GraphPartitioner.refineBalancing(L, currentAssignments, nodeCount, parts, margin);

            currentParts = parts;
            currentGraph.setGroupAssignments(currentAssignments);

            // Show results
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

            graphPanel.repaint();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Błąd podczas podziału grafu: " + e.getMessage(),
                    "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Graph readInputFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;

            // Read maxNodesInRow
            line = reader.readLine();
            int maxNodesInRow = Integer.parseInt(line.trim());

            // Read nodeIndices
            line = reader.readLine();
            String[] nodeIndicesStr = line.split(";");
            int nodeCount = nodeIndicesStr.length;
            Graph graph = new Graph(maxNodesInRow, nodeCount);
            for (String s : nodeIndicesStr) {
                graph.nodeIndices.add(Integer.parseInt(s.trim()));
            }

            // Read rowPointers
            line = reader.readLine();
            String[] rowPointersStr = line.split(";");
            for (String s : rowPointersStr) {
                graph.rowPointers.add(Integer.parseInt(s.trim()));
            }

            // Read groups
            line = reader.readLine();
            String[] groupsStr = line.split(";");
            for (String s : groupsStr) {
                graph.groups.add(Integer.parseInt(s.trim()));
            }

            // Read groupPointers
            line = reader.readLine();
            String[] groupPointersStr = line.split(";");
            for (String s : groupPointersStr) {
                graph.groupPointers.add(Integer.parseInt(s.trim()));
            }

            return graph;
        }
    }

    private void saveAssignmentsFile(String filename, List<Integer> assignments, int parts) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Number of groups: " + parts);
            for (int i = 0; i < assignments.size(); i++) {
                writer.println("Node " + i + " => group " + assignments.get(i));
            }
        }
    }

    private void saveCSRRG(String filename, Graph graph, int[] assignments, int parts) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Section 1: Graph description (identical to input format)
            writer.println(graph.maxNodesInRow);
            writer.println(String.join(";",
                    graph.nodeIndices.stream().map(String::valueOf).toArray(String[]::new)));
            writer.println(String.join(";",
                    graph.rowPointers.stream().map(String::valueOf).toArray(String[]::new)));
            writer.println(String.join(";",
                    graph.groups.stream().map(String::valueOf).toArray(String[]::new)));
            writer.println(String.join(";",
                    graph.groupPointers.stream().map(String::valueOf).toArray(String[]::new)));

            // Section 2: Partition description
            writer.println("# Number of groups: " + parts);
            StringBuilder partitionLine = new StringBuilder();
            for (int assignment : assignments) {
                partitionLine.append(assignment).append(" ");
            }
            writer.println(partitionLine.toString().trim());
        }
    }

    private void saveBinary(String filename, Graph graph, int[] assignments, int parts) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(filename))) {
            // Section 1: Graph description
            out.writeInt(graph.maxNodesInRow);
            out.writeInt(graph.nodeIndices.size());
            out.writeInt(graph.groupPointers.size() - 1);

            for (int index : graph.nodeIndices) out.writeInt(index);
            for (int ptr : graph.rowPointers) out.writeInt(ptr);
            for (int group : graph.groups) out.writeInt(group);
            for (int ptr : graph.groupPointers) out.writeInt(ptr);

            // Section 2: Partition description
            out.writeInt(parts);  // Write number of groups first
            for (int assignment : assignments) {
                out.writeInt(assignment);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new interfejs());
    }

    private class GraphPanel extends JPanel {
        private double scale = 1.0;
        private double translateX = 0;
        private double translateY = 0;
        private Point dragStart;

        public GraphPanel() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    dragStart = e.getPoint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    dragStart = null;
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragStart != null) {
                        Point dragEnd = e.getPoint();
                        translateX += dragEnd.x - dragStart.x;
                        translateY += dragEnd.y - dragStart.y;
                        dragStart = dragEnd;
                        repaint();
                    }
                }
            });

            addMouseWheelListener(e -> {
                double scaleFactor = e.getWheelRotation() < 0 ? 1.1 : 0.9;
                scale *= scaleFactor;
                repaint();
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            AffineTransform oldTransform = g2d.getTransform();
            g2d.translate(translateX, translateY);
            g2d.scale(scale, scale);

            if (currentGraph == null) {
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.drawString("Wczytaj graf, aby wyświetlić wizualizację",
                        getWidth()/2 - 100, getHeight()/2);
            } else {
                drawGraph(g2d);
            }

            g2d.setTransform(oldTransform);
        }

        private boolean areNodesConnected(int node1, int node2) {
            // Sprawdź czy wierzchołki są w tej samej grupie
            for (int g = 0; g < currentGraph.groupPointers.size() - 1; g++) {
                int groupStart = currentGraph.groupPointers.get(g);
                int groupEnd = currentGraph.groupPointers.get(g + 1);

                boolean found1 = false;
                boolean found2 = false;

                for (int j = groupStart; j < groupEnd; j++) {
                    int member = currentGraph.groups.get(j);
                    if (member == node1) found1 = true;
                    if (member == node2) found2 = true;

                    if (found1 && found2) return true;
                }
            }
            return false;
        }

        private void drawGraph(Graphics2D g2d) {
            int nodeCount = currentGraph.nodeIndices.size();
            int gridSize = (int) Math.ceil(Math.sqrt(nodeCount));
            int nodeDiameter = 30;
            int margin = 20;
            int nodeRadius = nodeDiameter / 2;

            int totalWidth = gridSize * (nodeDiameter + margin) + margin;
            int totalHeight = gridSize * (nodeDiameter + margin) + margin;

            setPreferredSize(new Dimension(
                    (int)(totalWidth * scale),
                    (int)(totalHeight * scale)));

            // Najpierw rysuj połączenia
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.0f)); // Cienkie linie

            for (int i = 0; i < nodeCount; i++) {
                for (int j = i + 1; j < nodeCount; j++) {
                    if (areNodesConnected(i, j)) {
                        int row1 = i / gridSize;
                        int col1 = i % gridSize;
                        int x1 = margin + col1 * (nodeDiameter + margin) + nodeRadius;
                        int y1 = margin + row1 * (nodeDiameter + margin) + nodeRadius;

                        int row2 = j / gridSize;
                        int col2 = j % gridSize;
                        int x2 = margin + col2 * (nodeDiameter + margin) + nodeRadius;
                        int y2 = margin + row2 * (nodeDiameter + margin) + nodeRadius;

                        g2d.drawLine(x1, y1, x2, y2);
                    }
                }
            }

            // Potem rysuj wierzchołki (żeby były na wierzchu linii)
            Color[] partColors = generateColors(currentParts);
            for (int i = 0; i < nodeCount; i++) {
                int row = i / gridSize;
                int col = i % gridSize;

                int x = margin + col * (nodeDiameter + margin);
                int y = margin + row * (nodeDiameter + margin);

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
                drawLegend(g2d, partColors, margin, nodeDiameter);
            }
        }

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