import java.util.ArrayList;
import java.util.List;

public class Graph {
    public int maxNodesInRow; // Maksymalna liczba węzłów w wierszu
    public List<Integer> nodeIndices; // Lista indeksów węzłów
    public List<Integer> rowPointers; // Wskaźniki wierszy (dla reprezentacji CSR)
    public List<Integer> groups; // Grupy węzłów
    public List<Integer> groupPointers; // Wskaźniki grup
    public List<Integer> groupAssignments; // Przypisania węzłów do grup
    public int[] degrees; // Tablica stopni węzłów

    // Konstruktor inicjalizujący strukturę grafu
    public Graph(int maxNodesInRow, int nodeCount) {
        this.maxNodesInRow = maxNodesInRow;
        this.nodeIndices = new ArrayList<>();
        this.rowPointers = new ArrayList<>();
        this.groups = new ArrayList<>();
        this.groupPointers = new ArrayList<>();
        this.groupAssignments = new ArrayList<>();
        this.degrees = new int[nodeCount]; // Inicjalizacja tablicy stopni
    }

    // Metoda obliczająca stopnie węzłów grafu
    public void calculateDegrees(int nodeCount, int groupCount, int totalGroupElements) {
        // Inicjalizacja wszystkich stopni na 0
        for (int i = 0; i < nodeCount; i++) {
            degrees[i] = 0;
        }

        // Przetwarzanie wszystkich grup węzłów
        for (int i = 0; i < groupCount; i++) {
            int start = groupPointers.get(i);
            int end = (i == groupCount - 1) ? totalGroupElements : groupPointers.get(i + 1);

            // Aktualizacja stopnia dla pierwszego węzła w grupie
            degrees[groups.get(start)] = end - start;

            // Zliczanie poprzednich wystąpień tego węzła w grupach
            for (int j = 0; j < start; j++) {
                if (groups.get(j) == groups.get(start)) {
                    degrees[groups.get(start)]++;
                }
            }
        }

        // Obsługa węzłów, które nie są pierwsze w żadnej grupie
        for (int i = 0; i < nodeCount; i++) {
            if (degrees[i] == 0) {
                for (int j = 0; j < totalGroupElements; j++) {
                    if (groups.get(j) == i) {
                        degrees[i]++;
                    }
                }
            }
        }
    }

    // Metoda przypisująca węzły do grup
    public void setGroupAssignments(int[] assignments) {
        groupAssignments.clear();
        for (int a : assignments) {
            groupAssignments.add(a);
        }
    }
}