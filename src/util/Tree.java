package util;

import java.util.List;
import java.util.Vector;


// a tree is parameterized by its containing data "X"
public class Tree<X> {

    // tree node
    public class Node {
        public X data;
        public List<Node> children;

        public Node(X data) {
            this.data = data;
            this.children = new Vector<>();
        }

        @Override
        public String toString() {
            return data.toString();
        }
    }
    // end of tree node

    // the tree name, for debugging
    public final String name;
    public Node root;
    private Vector<Node> allNodes;

    public Tree(String name) {
        this.name = name;
        this.root = null;
        this.allNodes = new Vector<>();
    }

    public void addRoot(X data) {
        Node n = new Node(data);
        this.allNodes.add(n);
        this.root = n;
    }

    public void addNode(X data) {
        // data must not already be in the tree
        // we should check that for correctness
        this.allNodes.add(new Node(data));
    }

    public Node lookupNode(X data) {
        for (Node node : this.allNodes) {
            if (node.data.equals(data))
                return node;
        }
        return null;
    }

    public void addEdge(Node from, Node to) {
        from.children.add(to);
    }

    public void addEdge(X from, X to) {
        Node f = this.lookupNode(from);
        Node t = this.lookupNode(to);

        if (f == null || t == null)
            throw new Error();

        this.addEdge(f, t);
    }

//    public void visualize() {
//        Dot dot = new Dot();
//        for (Node node : this.allNodes) {
//            for (Node child : node.children)
//                dot.insert(node.toString(), child.toString());
//        }
//        dot.visualize(this.treeName);
//    }
}