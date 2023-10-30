package edu.ufl.cise.cop4020fa23;

import java.util.HashMap;
import java.util.Stack;

public class LeBlancSymbolTable {
    int currentNum; // Serial number of the current scope
    int nextNum; // Next serial number to assign
    HashMap<String, Integer> symbolTable; // Symbol table to store entries
    Stack<Integer> scopeStack; // Stack to manage scope levels

    public LeBlancSymbolTable() {
        currentNum = 0;
        nextNum = 0;
        symbolTable = new HashMap<>();
        scopeStack = new Stack<>();
    }

    public void enterScope() {
        currentNum = nextNum++;
        scopeStack.push(currentNum);
    }

    public void leaveScope() {
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
