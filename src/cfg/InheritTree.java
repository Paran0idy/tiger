package cfg;

import ast.Ast;
import util.Id;
import util.Tree;
import util.Tuple;

import java.util.LinkedList;
import java.util.List;

// we have an empty "Object" class as the root.
public class InheritTree {

    // build an inherit tree
    public Tree<Ast.Class.T> buildTree(Ast.Program.T ast) {
        // we create a fake and empty "Object" class
        // this class servers as the root node
        Ast.Class.T objCls = new Ast.Class.Singleton(Id.newName("Object"),
                null, // null for non-existing "extends"
                new LinkedList<>(),
                new LinkedList<>(),
                new Tuple.One<>());// parent

        Tree<Ast.Class.T> tree = new Tree<>("inheritTree");
        // make "Object" the root
        tree.addRoot(objCls);


        List<Ast.Class.T> classes = null;
        switch (ast) {
            case Ast.Program.Singleton(Ast.MainClass.T _, List<Ast.Class.T> classes1) -> {
                // round #1: scan all class
                // add all classes (excluding "Main" class) into the tree,
                classes1.forEach(tree::addNode);
                // round #2: scan all class
                // to establish the parent-child relationship
                classes1.forEach((c) -> {
                    Ast.Class.Singleton cls = (Ast.Class.Singleton) c;
                    var parentNode = (cls.extends_() == null) ?
                            tree.root :
                            tree.lookupNode(cls.parent().get());
                    var childNode = tree.lookupNode(c);
                    // add the child into parent
                    tree.addEdge(parentNode, childNode);
                });
            }
        }
        return tree;
    }
}

