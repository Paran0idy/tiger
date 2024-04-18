package cfg;

import ast.Ast;
import util.Id;
import util.Tuple1;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

// a tree to record the inherit relationship between class.
// we have an empty "Object" class as the root.
public class InheritTree {

    public static class Node {
        public Ast.Class.T theClass;
        public Vector<Node> children;

        public Node(Ast.Class.T theClass, Vector<Node> children) {
            this.theClass = theClass;
            this.children = children;
        }
    }
    // end of Node

    public Ast.Program.T ast;
    public Vector<Node> nodes;
    public Node root;

    // constructor
    public InheritTree(Ast.Program.T ast) {
        this.ast = ast;
        this.nodes = new Vector<>();
        this.root = null;
    }

    public void addClass(Ast.Class.T cls) {
        //Ast.Class.T cls = Ast.Program.searchClass(this.ast, className);
        Node n = new Node(cls, new Vector<>());
        this.nodes.add(n);
    }

    public Node searchClass(Ast.Class.T cls) {
        for (Node n : this.nodes) {
            if (n.theClass.equals(cls)) {
                return n;
            }
        }
        return null;
    }

    // build an inherit tree
    public Node buildTree() {
        // we first create a fake "Object" class
        // this is also the root node
        Ast.Class.T objCls = new Ast.Class.Singleton(Id.newName("Object"),
                null, // null for non-existing "extends"
                new LinkedList<>(),
                new LinkedList<>(),
                new Tuple1<>());
        Node n = new Node(objCls, new Vector<>());
        this.nodes.add(n);
        this.root = n;

        // round #1: scan all class
        // to add all classes into the "nodes", excluding "Main" class
        List<Ast.Class.T> classes = null;
        if (ast instanceof Ast.Program.Singleton(Ast.MainClass.T mainClass, List<Ast.Class.T> classes1)) {
            classes = classes1;
        }
        assert classes != null;
        for (Ast.Class.T cls : classes) {
            this.addClass(cls);
        }
        // scan all class for the second time,
        // to establish the parent-child relationship
        for (Ast.Class.T c : classes) {
            Ast.Class.Singleton cls = (Ast.Class.Singleton) c;
            Node parentNode;
            // this is special, as its root is "Object"
            if (cls.extends_() == null) {
                parentNode = this.root;
            } else {
                parentNode = searchClass(cls.parent().getData());
            }
            Node childNode = searchClass(c);
            // add the child into parent
            parentNode.children.add(childNode);
        }
        return this.root;
    }
}

