package edu.ufl.cise.cop4020fa23;

import java.util.HashMap;
import java.util.Stack;

public class LeBlancSymbolTable {
    private int currentNum; // serial number of the current scope
    private int nextNum; // next serial number to assign
    private HashMap<String, Integer> symbolTable; // Symbol table to store entries
    private Stack<Integer> scopeStack; // Stack to manage scope levels

    public void enterScope() {
        currentNum = nextNum++;
        scopeStack.push(currentNum);
    }

    public void closeScope() {
        currentNum = scopeStack.pop();
    }

    public void insert(String name) {
        // Insert a new entry into the symbol table
        symbolTable.put(name, currentNum);
    }

    public int lookup(String name) {
        if (symbolTable.containsKey(name)) {
            int closestSerialNum = -1;
            int closestDistance = Integer.MAX_VALUE;

            for (String key : symbolTable.keySet()) {
                int serialNum = symbolTable.get(key);
                if (scopeStack.contains(serialNum)) {
                    int distance = scopeStack.indexOf(serialNum);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestSerialNum = serialNum;
                    }
                }
            }

            if (closestSerialNum != -1) {
                return closestSerialNum;
            }
        }

        // If not found in the current scope or any enclosing scope, it's an error
        throw new IllegalArgumentException("Symbol '" + name + "' is not bound in the current scope.");
    }
}
