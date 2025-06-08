import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Get input file path
        System.out.print("Enter input file path: ");
        String inputFile = scanner.nextLine().trim();

        // Get number of parts
        System.out.print("Enter number of parts (default 2): ");
        String partsInput = scanner.nextLine().trim();
        int parts = partsInput.isEmpty() ? 2 : Integer.parseInt(partsInput);

        // Get margin
        System.out.print("Enter margin percentage (default 10): ");
        String marginInput = scanner.nextLine().trim();
        int margin = marginInput.isEmpty() ? 10 : Integer.parseInt(marginInput);

        // Get output format
        System.out.print("Enter output format (csrrg/bin, default csrrg): ");
        String outputFormat = scanner.nextLine().trim().toLowerCase();
        if (!outputFormat.equals("bin")) {
            outputFormat = "csrrg";
        }

        if (margin < 0 || margin > 100) {
            System.err.println("Margin must be between 0 and 100");
            System.exit(1);
        }

        try {
            // Read input file
            Graph graph = readInputFile(inputFile);

            // Calculate degrees
            int nodeCount = graph.nodeIndices.size();
            int groupCount = graph.groupPointers.size() - 1;
            int totalGroupElements = graph.groups.size();
            graph.calculateDegrees(nodeCount, groupCount, totalGroupElements);

            if (parts <= 1 || parts > nodeCount) {
                System.err.println("Cannot divide graph into " + parts + " parts");
                System.exit(1);
            }

            // Build Laplace matrix
            LaplaceMatrix L = LaplaceMatrix.buildLaplaceMatrix(graph, nodeCount);

            // Partition graph
            int[] assignments = new int[nodeCount];
            int[] indices = new int[nodeCount];
            for (int i = 0; i < nodeCount; i++) indices[i] = i;

            GraphPartitioner.recursivePartition(L, indices, nodeCount, assignments, 0, parts, margin);
            GraphPartitioner.refineBalancing(L, assignments, nodeCount, parts, margin);

            // Print results to console
            System.out.println("\n=== Graph divided into " + parts + " parts (margin " + margin + "%) ===");
            System.out.println("Number of groups: " + parts);
            for (int i = 0; i < nodeCount; i++) {
                System.out.println("Node " + i + " => group " + (assignments[i]));
            }

            // Save results
            graph.setGroupAssignments(assignments);
            saveAssignments("assignments.txt", graph.groupAssignments, parts);
            if (outputFormat.equals("csrrg")) {
                saveCSRRG("output.csrrg", graph, assignments, parts);
            } else {
                saveBinary("output.bin", graph, assignments, parts);
            }

            System.out.println("\nResults saved to:");
            System.out.println("- assignments.txt");
            System.out.println("- output." + outputFormat);

        } catch (IOException e) {
            System.err.println("Error processing file: " + e.getMessage());
            System.exit(1);
        } finally {
            scanner.close();
        }
    }

    private static Graph readInputFile(String filename) throws IOException {
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


    private static void saveAssignments(String filename, List<Integer> assignments, int parts) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Number of groups: " + parts);
            for (int i = 0; i < assignments.size(); i++) {
                writer.println("Node " + i + " => group " + assignments.get(i));
            }
        }
    }


    private static void saveCSRRG(String filename, Graph graph, int[] assignments, int parts) throws IOException {
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

    private static void saveBinary(String filename, Graph graph, int[] assignments, int parts) throws IOException {
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
}