import java.util.ArrayList;
import java.util.List;

public class Graph {
    public int maxNodesInRow;
    public List<Integer> nodeIndices;
    public List<Integer> rowPointers;
    public List<Integer> groups;
    public List<Integer> groupPointers;
    public List<Integer> groupAssignments; /*Przypisania grup do węzłów*/
    public int[] degrees;

    public Graph(int maxNodesInRow, int nodeCount) {
        this.maxNodesInRow = maxNodesInRow;
        this.nodeIndices = new ArrayList<>();
        this.rowPointers = new ArrayList<>();
        this.groups = new ArrayList<>();
        this.groupPointers = new ArrayList<>();
        this.groupAssignments = new ArrayList<>(); /*Przypisania grup do węzłów*/
        this.degrees = new int[nodeCount];
    }

    public void calculateDegrees(int nodeCount, int groupCount, int totalGroupElements) {
        // Initialize all degrees to 0
        for (int i = 0; i < nodeCount; i++) {
            degrees[i] = 0;
        }

        // Process all node groups
        for (int i = 0; i < groupCount; i++) {
            int start = groupPointers.get(i);
            int end = (i == groupCount - 1) ? totalGroupElements : groupPointers.get(i + 1);

            // Update degree for the group's first node
            degrees[groups.get(start)] = end - start;

            // Count previous occurrences of this node in groups
            for (int j = 0; j < start; j++) {
                if (groups.get(j) == groups.get(start)) {
                    degrees[groups.get(start)]++;
                }
            }
        }

        // Handle nodes that aren't first in any group
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

    public void setGroupAssignments(int[] assignments) {
        groupAssignments.clear();
        for (int a : assignments) {
            groupAssignments.add(a);
        }
    }

}

