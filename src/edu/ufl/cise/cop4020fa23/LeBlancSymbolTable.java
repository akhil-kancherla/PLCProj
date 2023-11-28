package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.NameDef;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class LeBlancSymbolTable {
    private Stack<Map<String, NameDef>> scopes = new Stack<>();

    public void enterScope() {
        System.out.println("Entering a new scope.");
        scopes.push(new HashMap<>());
    }

    public void leaveScope() {
        System.out.println("Leaving the current scope.");
        scopes.pop();
    }

    public boolean insert(String name, NameDef def) throws TypeCheckException {
        if (scopes.isEmpty()) {
            enterScope(); // ensure there is always at least one scope
        }
        Map<String, NameDef> currentScope = scopes.peek();
//        if (currentScope.containsKey(name)) {
//            throw new TypeCheckException(); // duplicate name definition
//        }
        currentScope.put(name, def);
        return true;
    }

    public NameDef lookup(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            NameDef def = scopes.get(i).get(name);
            //System.out.println("Looking for " + name + scopes.get(i).get(name) + "<--scope name");
            if (def != null) {
                return def;
            }
        }
        return null; // not found
    }

    public NameDef lookupScope(String name) {
        NameDef def = scopes.peek().get(name);
        if (def!=null) {
            return def;
        }
        return null;
    }

    public int getCurrentScopeIndex() {
        return scopes.size() - 1;
    }

    public Map<String, NameDef> getScope(int index) {
        if (index >= 0 && index < scopes.size()) {
            return scopes.get(index);
        }
        return null;
    }



}
