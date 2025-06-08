import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class GraphPartitioner {
    public static void recursivePartition(LaplaceMatrix L, int[] nodeIndices, int nodeCount,
                                          int[] assignments, int startPart, int partCount, int margin) {
        if (partCount == 1) {
            for (int i = 0; i < nodeCount; i++) {
                assignments[nodeIndices[i]] = startPart;
            }
            return;
        }

        List<Integer> left = new ArrayList<>();
        List<Integer> right = new ArrayList<>();
        double[] fiedlerVector = new double[nodeCount];

        LaplaceMatrix subgraph = LaplaceMatrix.createSubgraph(L, nodeIndices, nodeCount);
        powerMethod(subgraph, fiedlerVector, 100);

        double[] sorted = Arrays.copyOf(fiedlerVector, nodeCount);
        Arrays.sort(sorted);

        int leftParts = partCount / 2;
        int rightParts = partCount - leftParts;
        int leftNodeCount = (int)((double)leftParts / (leftParts + rightParts) * nodeCount);
        if (leftNodeCount == 0) leftNodeCount = 1;

        double threshold = (sorted[leftNodeCount] + sorted[leftNodeCount - 1]) / 2.0;

        for (int i = 0; i < nodeCount; i++) {
            if (fiedlerVector[i] < threshold) {
                left.add(nodeIndices[i]);
            } else if (fiedlerVector[i] == threshold) {
                if (left.size() < leftNodeCount) {
                    left.add(nodeIndices[i]);
                } else {
                    right.add(nodeIndices[i]);
                }
            } else {
                right.add(nodeIndices[i]);
            }
        }

        int[] leftArr = left.stream().mapToInt(i -> i).toArray();
        int[] rightArr = right.stream().mapToInt(i -> i).toArray();

        recursivePartition(L, leftArr, left.size(), assignments, startPart, leftParts, margin);
        recursivePartition(L, rightArr, right.size(), assignments, startPart + leftParts, rightParts, margin);
    }

    public static void powerMethod(LaplaceMatrix L, double[] fiedlerVector, int iterations) {
        double[] x = new double[L.nodeCount];
        double[] y = new double[L.nodeCount];
        Arrays.fill(x, 1.0);

        for (int k = 0; k < iterations; k++) {
            L.multiply(x, y);

            double norm = 0.0;
            for (double value : y) {
                norm += value * value;
            }
            norm = Math.sqrt(norm);

            for (int i = 0; i < L.nodeCount; i++) {
                x[i] = y[i] / norm;
            }
        }

        System.arraycopy(x, 0, fiedlerVector, 0, L.nodeCount);
    }

    public static void refineBalancing(LaplaceMatrix L, int[] assignments, int nodeCount,
                                       int partCount, int margin) {
        int[] counts = new int[partCount];
        for (int i = 0; i < nodeCount; i++) {
            counts[assignments[i]]++;
        }

        int idealSize = nodeCount / partCount;
        int lower = (int)(idealSize * (1.0 - margin / 100.0));
        int upper = (int)Math.ceil(idealSize * (1.0 + margin / 100.0));

        boolean improved;
        do {
            improved = false;
            for (int i = 0; i < partCount; i++) {
                if (counts[i] > upper) {
                    int bestNode = -1;
                    int bestInside = Integer.MAX_VALUE;
                    int bestOutside = -1;
                    int bestNewPart = -1;

                    for (int node = 0; node < nodeCount; node++) {
                        if (assignments[node] != i) continue;

                        int inside = 0;
                        int outside = 0;
                        calculateEdges(L, assignments, node, inside, outside);

                        for (int j = 0; j < partCount; j++) {
                            if (counts[j] < lower) {
                                if (outside >= inside || counts[i] > 2 * upper) {
                                    if (inside < bestInside ||
                                            (inside == bestInside && outside > bestOutside)) {
                                        bestNode = node;
                                        bestInside = inside;
                                        bestOutside = outside;
                                        bestNewPart = j;
                                    }
                                }
                            }
                        }
                    }

                    if (bestNode != -1) {
                        assignments[bestNode] = bestNewPart;
                        counts[i]--;
                        counts[bestNewPart]++;
                        improved = true;
                    }
                }
            }
        } while (improved);
    }

    private static void calculateEdges(LaplaceMatrix L, int[] assignments, int node,
                                       int inside, int outside) {
        inside = 0;
        outside = 0;
        int nodePart = assignments[node];

        for (int j = L.rowPointers.get(node); j < L.rowPointers.get(node + 1); j++) {
            int neighbor = L.columnIndices.get(j);
            if (neighbor == node) continue;

            if (assignments[neighbor] == nodePart) {
                inside++;
            } else {
                outside++;
            }
        }
    }
}
