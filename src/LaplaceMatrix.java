import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LaplaceMatrix {
    public int nodeCount;
    public int nonZeroCount;
    public List<Integer> rowPointers;
    public List<Integer> columnIndices;
    public List<Double> values;

    public LaplaceMatrix(int nodeCount) {
        this.nodeCount = nodeCount;
        this.rowPointers = new ArrayList<>(nodeCount + 1);
        this.columnIndices = new ArrayList<>();
        this.values = new ArrayList<>();
        this.nonZeroCount = 0;
    }

    public static LaplaceMatrix buildLaplaceMatrix(Graph graph, int nodeCount) {
        LaplaceMatrix L = new LaplaceMatrix(nodeCount);
        int nnz = 0;

        for (int i = 0; i < nodeCount; i++) {
            L.rowPointers.add(nnz);

            // Diagonal element: degree
            L.columnIndices.add(i);
            L.values.add((double) graph.degrees[i]);
            nnz++;

            // Use a set to avoid duplicate neighbors
            Set<Integer> neighbors = new HashSet<>();

            // Find all groups this node belongs to
            for (int g = 0; g < graph.groupPointers.size() - 1; g++) {
                int groupStart = graph.groupPointers.get(g);
                int groupEnd = graph.groupPointers.get(g + 1);

                // If node i is in this group, add all other nodes as its neighbors
                for (int j = groupStart; j < groupEnd; j++) {
                    int member = graph.groups.get(j);
                    if (member == i) {
                        for (int k = groupStart; k < groupEnd; k++) {
                            int neighbor = graph.groups.get(k);
                            if (neighbor != i) {
                                neighbors.add(neighbor);
                            }
                        }
                        break; // Node found in this group, skip remaining
                    }
                }
            }

            for (int neighbor : neighbors) {
                L.columnIndices.add(neighbor);
                L.values.add(-1.0);
                nnz++;
            }
        }

        L.rowPointers.add(nnz);
        L.nonZeroCount = nnz;
        return L;
    }

    public static LaplaceMatrix createSubgraph(LaplaceMatrix L, int[] nodeIndices, int subgraphSize) {
        LaplaceMatrix subgraph = new LaplaceMatrix(subgraphSize);
        int[] oldToNewIndex = new int[L.nodeCount];

        for (int i = 0; i < L.nodeCount; i++) {
            oldToNewIndex[i] = -1;
        }

        for (int i = 0; i < subgraphSize; i++) {
            oldToNewIndex[nodeIndices[i]] = i;
        }

        int nnz = 0;
        for (int i = 0; i < subgraphSize; i++) {
            int originalNode = nodeIndices[i];
            subgraph.rowPointers.add(nnz);

            for (int j = L.rowPointers.get(originalNode); j < L.rowPointers.get(originalNode + 1); j++) {
                int originalCol = L.columnIndices.get(j);
                if (oldToNewIndex[originalCol] != -1) {
                    subgraph.columnIndices.add(oldToNewIndex[originalCol]);
                    subgraph.values.add(L.values.get(j));
                    nnz++;
                }
            }
        }

        subgraph.rowPointers.add(nnz);
        subgraph.nonZeroCount = nnz;
        return subgraph;
    }

    public void multiply(double[] x, double[] result) {
        for (int i = 0; i < nodeCount; i++) {
            result[i] = 0.0;
            for (int j = rowPointers.get(i); j < rowPointers.get(i + 1); j++) {
                result[i] += values.get(j) * x[columnIndices.get(j)];
            }
        }
    }
}
