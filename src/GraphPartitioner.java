import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class GraphPartitioner {
    // Metoda rekurencyjnie dzieląca graf na części
    public static void recursivePartition(LaplaceMatrix L, int[] nodeIndices, int nodeCount,
                                          int[] assignments, int startPart, int partCount, int margin) {
        if (partCount == 1) {
            // Przypisanie wszystkich węzłów do jednej części
            for (int i = 0; i < nodeCount; i++) {
                assignments[nodeIndices[i]] = startPart;
            }
            return;
        }

        List<Integer> left = new ArrayList<>();  // Węzły po lewej stronie podziału
        List<Integer> right = new ArrayList<>(); // Węzły po prawej stronie podziału
        double[] fiedlerVector = new double[nodeCount]; // Wektor Fiedlera

        // Tworzenie podgrafu i obliczanie wektora Fiedlera
        LaplaceMatrix subgraph = LaplaceMatrix.createSubgraph(L, nodeIndices, nodeCount);
        powerMethod(subgraph, fiedlerVector, 100);

        // Sortowanie wektora Fiedlera
        double[] sorted = Arrays.copyOf(fiedlerVector, nodeCount);
        Arrays.sort(sorted);

        int leftParts = partCount / 2;          // Liczba części po lewej stronie
        int rightParts = partCount - leftParts; // Liczba części po prawej stronie
        int leftNodeCount = (int)((double)leftParts / (leftParts + rightParts) * nodeCount);
        if (leftNodeCount == 0) leftNodeCount = 1;

        // Obliczenie progu podziału
        double threshold = (sorted[leftNodeCount] + sorted[leftNodeCount - 1]) / 2.0;

        // Podział węzłów na podstawie progu
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

        // Rekurencyjne dzielenie lewej i prawej części
        recursivePartition(L, leftArr, left.size(), assignments, startPart, leftParts, margin);
        recursivePartition(L, rightArr, right.size(), assignments, startPart + leftParts, rightParts, margin);
    }

    // Metoda potęgowa obliczająca wektor Fiedlera
    public static void powerMethod(LaplaceMatrix L, double[] fiedlerVector, int iterations) {
        double[] x = new double[L.nodeCount];
        double[] y = new double[L.nodeCount];
        Arrays.fill(x, 1.0); // Inicjalizacja wektora startowego

        for (int k = 0; k < iterations; k++) {
            L.multiply(x, y); // Mnożenie macierzy Laplace'a przez wektor

            // Normalizacja wektora
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

    // Metoda poprawiająca balans podziału
    public static void refineBalancing(LaplaceMatrix L, int[] assignments, int nodeCount,
                                       int partCount, int margin) {
        int[] counts = new int[partCount]; // Liczba węzłów w każdej części
        for (int i = 0; i < nodeCount; i++) {
            counts[assignments[i]]++;
        }

        // Obliczenie idealnego rozmiaru części z uwzględnieniem marginesu
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

                    // Znajdowanie najlepszego węzła do przeniesienia
                    for (int node = 0; node < nodeCount; node++) {
                        if (assignments[node] != i) continue;

                        int inside = 0;
                        int outside = 0;
                        calculateEdges(L, assignments, node, inside, outside);

                        // Sprawdzenie, czy przeniesienie poprawia balans
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

    // Metoda obliczająca liczbę połączeń wewnętrznych i zewnętrznych dla węzła
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