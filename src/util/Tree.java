package util;

import java.util.LinkedList;
import java.util.List;


// a tree is parameterized by its containing datatype "X"
public class Tree<X> {

    // tree node
    public class Node {
        X data;
        public List<Node> children;

        public Node(X data) {
            this.data = data;
            this.children = new LinkedList<>();
        }

        @Override
        public String toString() {
            return data.toString();
        }
    }

    // the tree
    String treeName;
    Node root;
    List<Node> allNodes;

    public Tree(String name) {
        this.treeName = name;
        this.root = null;
        this.allNodes = new LinkedList<>();
    }

    private void addNode(Node node) {
        this.allNodes.add(node);
    }

    public void addNode(X data) {
        for (Node n : this.allNodes)
            if (n.data.equals(data))
                throw new Error();

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
        from.children.add(to);
    }

    public void addEdge(X from, X to) {
        Node f = this.lookupNode(from);
        Node t = this.lookupNode(to);

        if (f == null || t == null)
            throw new Error();

        this.addEdge(f, t);
    }

    public void visualize() {
        Dot dot = new Dot();
        for (Node node : this.allNodes) {
            for (Node child : node.children)
                dot.insert(node.toString(), child.toString());
        }
        dot.visualize(this.treeName);
    }
}
