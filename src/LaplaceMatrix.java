import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LaplaceMatrix {
    public int nodeCount;          // Liczba węzłów w macierzy
    public int nonZeroCount;       // Liczba niezerowych elementów w macierzy
    public List<Integer> rowPointers; // Wskaźniki wierszy w formacie CSR (Compressed Sparse Row)
    public List<Integer> columnIndices; // Indeksy kolumn dla niezerowych elementów w formacie CSR
    public List<Double> values;    // Wartości niezerowych elementów w formacie CSR

    // Konstruktor inicjalizujący macierz Laplace'a
    public LaplaceMatrix(int nodeCount) {
        this.nodeCount = nodeCount;
        this.rowPointers = new ArrayList<>(nodeCount + 1);
        this.columnIndices = new ArrayList<>();
        this.values = new ArrayList<>();
        this.nonZeroCount = 0;
    }

    // Metoda budująca macierz Laplace'a na podstawie grafu
    public static LaplaceMatrix buildLaplaceMatrix(Graph graph, int nodeCount) {
        LaplaceMatrix L = new LaplaceMatrix(nodeCount);
        int nnz = 0; // Licznik niezerowych elementów

        for (int i = 0; i < nodeCount; i++) {
            L.rowPointers.add(nnz);

            // Element diagonalny: stopień węzła
            L.columnIndices.add(i);
            L.values.add((double) graph.degrees[i]);
            nnz++;

            // Zbiór sąsiadów do uniknięcia duplikatów
            Set<Integer> neighbors = new HashSet<>();

            // Sprawdzenie wszystkich grup, do których należy węzeł
            for (int g = 0; g < graph.groupPointers.size() - 1; g++) {
                int groupStart = graph.groupPointers.get(g);
                int groupEnd = graph.groupPointers.get(g + 1);

                // Jeśli węzeł i należy do tej grupy, dodaj pozostałe węzły jako sąsiadów
                for (int j = groupStart; j < groupEnd; j++) {
                    int member = graph.groups.get(j);
                    if (member == i) {
                        for (int k = groupStart; k < groupEnd; k++) {
                            int neighbor = graph.groups.get(k);
                            if (neighbor != i) {
                                neighbors.add(neighbor);
                            }
                        }
                        break; // Węzeł znaleziony w grupie, pomiń resztę
                    }
                }
            }

            // Dodanie sąsiadów do macierzy
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

    // Metoda tworząca podgraf na podstawie macierzy Laplace'a
    public static LaplaceMatrix createSubgraph(LaplaceMatrix L, int[] nodeIndices, int subgraphSize) {
        LaplaceMatrix subgraph = new LaplaceMatrix(subgraphSize);
        int[] oldToNewIndex = new int[L.nodeCount]; // Mapowanie starych indeksów na nowe

        // Inicjalizacja mapowania
        for (int i = 0; i < L.nodeCount; i++) {
            oldToNewIndex[i] = -1;
        }

        // Ustawienie nowych indeksów dla wybranych węzłów
        for (int i = 0; i < subgraphSize; i++) {
            oldToNewIndex[nodeIndices[i]] = i;
        }

        int nnz = 0;
        for (int i = 0; i < subgraphSize; i++) {
            int originalNode = nodeIndices[i];
            subgraph.rowPointers.add(nnz);

            // Kopiowanie niezerowych elementów dla wybranych węzłów
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

    // Metoda wykonująca mnożenie macierzy Laplace'a przez wektor
    public void multiply(double[] x, double[] result) {
        for (int i = 0; i < nodeCount; i++) {
            result[i] = 0.0;
            // Mnożenie wiersza macierzy przez wektor
            for (int j = rowPointers.get(i); j < rowPointers.get(i + 1); j++) {
                result[i] += values.get(j) * x[columnIndices.get(j)];
            }
        }
    }
}