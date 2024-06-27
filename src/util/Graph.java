package util;

import util.set.FunSet;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

// a graph is parameterized by its containing data type "X"
@SuppressWarnings("unused")
public class Graph<X> {

    // graph node
    public class Node {
        X data;
        public LinkedList<Edge> edges;
        public Plist plist;

        public Node() {
            this.data = null;
            this.edges = null;
            this.plist = null;
        }

        public Node(X data) {
            this.data = data;
            this.edges = new LinkedList<>();
            this.plist = new Plist();
        }

        public Plist getPlist() {
            return this.plist;
        }
    }

    // graph edge
    public class Edge {
        Node from;
        Node to;
        public Plist plist;

        public Edge(Node from, Node to) {
            this.from = from;
            this.to = to;
            this.plist = new Plist();
        }

        public Plist getPlist() {
            return plist;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (!(o instanceof Graph.Edge)) return false;

            return (this == o);
        }
    }

    // graph data structures:
    String name;
    LinkedList<Node> allNodes;

    public Graph(String name) {
        this.name = name;
        this.allNodes = new LinkedList<>();
    }

    private void addNode(Node node) {
        this.allNodes.addLast(node);
    }

    public void addNode(X data) {
        // sanity checking to make the data unique:
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

    public <Y> void dfsDoit(Node n,
                            BiFunction<X, Y, Y> doit,
                            Y value,
                            HashSet<Node> visited) {
        visited.add(n);
        // System.out.println("now visiting: "+n);
        Y result = doit.apply(n.data, value);

        for (Edge edge : n.edges)
            if (!visited.contains(edge.to))
                dfsDoit(edge.to,
                        doit,
                        result,
                        visited);
    }

    public <Y> void dfs(X start,
                        BiFunction<X, Y, Y> doit,
                        Y value) {
        Node startNode = this.lookupNode(start);
        if (startNode == null)
            throw new util.Error();

        HashSet<Node> visited = new HashSet<>();

        dfsDoit(startNode,
                doit,
                value,
                visited);

//        // For control-flow allNodes, we do not need this, as
//        // the "startNode" will reach all other nodes.
//        for (Node n : this.allNodes) {
//            if (!visited.contains(n))
//                dfsDoit(n, doit, value, visited);
//        }
    }

    public void dot(Function<X, String> converter) {
        Dot dot = new Dot(this.name);
        for (Node node : this.allNodes) {
            for (Edge edge : node.edges)
                dot.insert(converter.apply(edge.from.data),
                        converter.apply(edge.to.data));
        }
        dot.visualize();
    }

    // topological-sort the nodes
    public List<Node> topologicalSort() {
        throw new util.Todo();
    }

    // TODO: lab7, exercise 3.
    // calculate the dominators for each node "n".
    public FunSet<Node> dominators(Node n) {
        throw new util.Todo();
    }

    // TODO: lab7, exercise 4.
    // calculate the dominator tree
    public Tree<Node> dominatorTree() {
        throw new util.Todo();
    }

    // TODO: lab7, exercise 5.
    // calculate the immediate dominator for a node n
    public Node idom(Tree<Node> tree, Node n) {
        throw new util.Todo();
    }

    // TODO: lab7, exercise 6.
    // calculate the dominance frontiers for a node n
    public FunSet<Node> dominanceFrontier(Node n) {
        throw new util.Todo();
    }

    // TODO: lab7, exercise 9.
    // split critical edges in a graph。
    // when inserting a fresh node n between two node "from"
    // and "to":
    //   1. invoke "doitFrom()" to process the "from" node (to modify
    //     its transfers); and
    //   2. invoke "doitTo()" to process the "to" node (to generate
    //     the data for the freshly generated node).
    public void splitCriticalEdges(Consumer<X> doitFrom,
                                   Function<X, X> doitTo) {
        throw new util.Todo();
    }


}
