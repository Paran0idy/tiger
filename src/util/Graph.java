package util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


// a graph is parameterized by its containing datatype "X"
public class Graph<X> {

    // graph node
    public class Node {
        X data;
        public LinkedList<Edge> edges;

        public Node() {
            this.data = null;
            this.edges = null;
        }

        public Node(X data) {
            this.data = data;
            this.edges = new LinkedList<>();
        }

        @Override
        public String toString() {
            return data.toString();
        }
    }

    // graph edge
    public class Edge {
        Node from;
        Node to;

        public Edge(Node from, Node to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (!(o instanceof Graph.Edge)) return false;

            return (this == o);
        }

        @Override
        public String toString() {
            return STR."\{this.from.toString()}->\{this.to.toString()}";
        }
    }

    // the allNodes
    LinkedList<Node> allNodes;
    String graphName;

    public Graph(String name) {
        this.graphName = name;
        this.allNodes = new LinkedList<>();
    }

    private void addNode(Node node) {
        this.allNodes.addLast(node);
    }

    public void addNode(X data) {
        for (Node n : this.allNodes)
            if (n.data.equals(data))
                throw new util.Error();

        Node node = new Node(data);
        this.addNode(node);
    }

    public Node lookupNode(X data) {
        for (Node node : this.allNodes) {
            if (node.data.equals(data))
                return node;
        }
        return null;
    }

    private void addEdge(Node from, Node to) {
        from.edges.addLast(new Edge(from, to));
    }

    public void addEdge(X from, X to) {
        Node f = this.lookupNode(from);
        Node t = this.lookupNode(to);

        if (f == null || t == null)
            throw new util.Error();

        this.addEdge(f, t);
    }

    public void dfsDoit(Node n, java.util.HashSet<Node> visited) {
        visited.add(n);
        // System.out.println("now visiting: "+n);

        for (Edge edge : n.edges)
            if (!visited.contains(edge.to))
                dfsDoit(edge.to, visited);
    }

    public void dfs(X start) {
        Node startNode = this.lookupNode(start);
        if (startNode == null)
            throw new util.Error();

        java.util.HashSet<Node> visited = new java.util.HashSet<>();

        dfsDoit(startNode, visited);

//        // For control-flow allNodes, we do not need this, as
        // // the "startNode" will reach all other nodes.
//        for (Node n : this.allNodes) {
//            if (!visited.contains(n))
//                dfsDoit(n, visited);
//        }
    }


    // topological-sort the nodes
    public List<Node> topologicalSort() {
        throw new util.Todo();
    }

    // calculate the dominator tree
    // the tree Node is the graph Node.
    public Tree<Node> dominatorTree() {
        throw new util.Todo();
    }

    //
    public HashMap<Node, Node> dominanceFrontier() {
        throw new util.Todo();
    }


    public void visualize() {
        Dot dot = new Dot();
        for (Node node : this.allNodes) {
            for (Edge edge : node.edges)
                dot.insert(edge.from.toString(), edge.to.toString());
        }
        dot.visualize(this.graphName);
    }
}
