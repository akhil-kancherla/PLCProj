package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.NameDef;

import java.util.HashMap;
import java.util.Stack;

public class LeBlancSymbolTable {
    private Stack<HashMap<String, NameDef>> scopes = new Stack<>();

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void leaveScope() {
        scopes.pop();
    }

    public void insert(String name, NameDef def) {
        if (!scopes.isEmpty()) {
            scopes.peek().put(name, def);
        }
    }

    public NameDef lookup(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                return scopes.get(i).get(name);
            }
        }
        return null;
    }
}
