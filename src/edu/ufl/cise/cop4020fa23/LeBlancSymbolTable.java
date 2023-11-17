package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.NameDef;

import java.util.HashMap;
import java.util.Stack;

public class LeBlancSymbolTable {
    private Stack<HashMap<String, NameDef>> scopes = new Stack<>();

    public void enterScope() {
        System.out.println("Entering a new scope.");
        scopes.push(new HashMap<>());
    }

    public void leaveScope() {
        System.out.println("Leaving the current scope.");
        scopes.pop();
    }

    public void insert(String name, NameDef def) {
        if (!scopes.isEmpty()) {
            System.out.println("Inserting " + name + " into the current scope.");
            scopes.peek().put(name, def);
        }
    }

    public NameDef lookup(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                System.out.println(name + " found in the scope at level " + i);
                return scopes.get(i).get(name);
            }
        }
        System.out.println(name + " not found in any scope.");
        return null;
    }

    public NameDef lookupScope(String name) {
        NameDef def = scopes.peek().get(name);
        if (def!=null) {
            return def;
        }
        return null;
    }
}
