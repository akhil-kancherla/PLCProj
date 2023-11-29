package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import java.util.List;
import java.util.Stack;

import edu.ufl.cise.cop4020fa23.LeBlancSymbolTable;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;


public class TypeCheckVisitor implements ASTVisitor {

    private LeBlancSymbolTable symbolTable = new LeBlancSymbolTable();
    private Stack<Type> functionReturnTypes = new Stack<>();

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        symbolTable.enterScope();

        // Visit the right-hand side expression
        //Type exprType = (Type) assignmentStatement.getE().visit(this, arg);

        // Leave the scope after visiting the expression
        //symbolTable.leaveScope();
        // Get the type of the LHS of the assignment
        LValue lValue = assignmentStatement.getlValue();
        NameDef lValueNameDef = symbolTable.lookup(lValue.getName());
        if (lValueNameDef == null) {
            throw new TypeCheckException("Variable '" + lValue.getName() + "' used before declaration.");
        }
        Type lhsType = (Type) lValue.visit(this, arg);

        // Get the type of the RHS of the assignment
        Expr rhs = assignmentStatement.getE();
        Type rhsType = (Type) rhs.visit(this, arg);

        // Special handling for assignments to image types
        if (lhsType == Type.IMAGE) {
            if (rhsType != Type.IMAGE && !(rhs instanceof ExpandedPixelExpr)) {
                throw new TypeCheckException("Right-hand side of assignment must be an image or a pixel expression when assigning to an image type.");
            }
            if (!(rhs instanceof ExpandedPixelExpr)) {
                throw new TypeCheckException("Right-hand side of assignment to image must be a pixel expression.");
            }
            ExpandedPixelExpr expandedPixelExpr = (ExpandedPixelExpr) rhs;
            // Check each component of the pixel
            if (!((Type) expandedPixelExpr.getRed().visit(this, arg) == Type.INT &&
                    (Type) expandedPixelExpr.getGreen().visit(this, arg) == Type.INT &&
                    (Type) expandedPixelExpr.getBlue().visit(this, arg) == Type.INT)) {
                throw new TypeCheckException("Invalid pixel expression types: red, green, and blue components must be of type INT.");
            }
        } else if (lhsType != (rhsType)) {
            // Check if the types are compatible for assignment for other cases
            throw new TypeCheckException("Type mismatch in assignment: " + lhsType + " cannot be assigned to " + rhsType);
        }

        // If the variable is declared, set it as initialized
        lValueNameDef.setInitialized(true);

        symbolTable.leaveScope();

        return null;
    }




    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        Type leftType = (Type) binaryExpr.getLeftExpr().visit(this, arg);
        Type rightType = (Type) binaryExpr.getRightExpr().visit(this, arg);
        Type resultType = null;

        if (leftType == null || rightType == null || leftType != rightType) {
            throw new TypeCheckException();
        }


            switch (binaryExpr.getOpKind()) {
                case BITAND, BITOR -> {
                    if(leftType == Type.PIXEL && rightType == Type.PIXEL) resultType = Type.PIXEL;
                    else throw new TypeCheckException(binaryExpr.firstToken.sourceLocation(), "BITAND or BITOR operation on invalid types of: " + leftType + ", " + rightType);
                }
                case AND, OR -> {
                    if(leftType == Type.BOOLEAN && rightType == Type.BOOLEAN) resultType = Type.BOOLEAN;
                    else throw new TypeCheckException(binaryExpr.firstToken.sourceLocation(), "AND or OR operation on invalid types of: " + leftType + ", " + rightType);

                }
                case LT, GT, LE, GE -> {
                    if(leftType == Type.INT && rightType == Type.INT) resultType = Type.BOOLEAN;
                    else throw new TypeCheckException(binaryExpr.firstToken.sourceLocation(), "comparison operation on invalid types of: " + leftType + ", " + rightType);

                }
                case EQ -> {
                    if(leftType == rightType) resultType = Type.BOOLEAN;
                    else throw new TypeCheckException(binaryExpr.firstToken.sourceLocation(), "comparison operation on invalid types of: " + leftType + ", " + rightType);

                }
                case EXP-> {
                    if(leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
                    else throw new TypeCheckException(binaryExpr.firstToken.sourceLocation(), "EXP operation on invalid types of: " + leftType + ", " + rightType);

                }
                case PLUS -> {
                    if(leftType == rightType) resultType = leftType;
                    else throw new TypeCheckException(binaryExpr.firstToken.sourceLocation(), "addition operation on invalid types of: " + leftType + ", " + rightType);

                }
                default -> {
                    if((leftType == Type.INT || leftType == Type.PIXEL || leftType == Type.IMAGE) && leftType == rightType) resultType = leftType;
                    else if((leftType == Type.PIXEL || leftType == Type.IMAGE) && rightType == Type.INT) resultType = leftType;
                    else throw new TypeCheckException(binaryExpr.firstToken.sourceLocation(), binaryExpr.getOpKind() + " operation on invalid types of: " + leftType + ", " + rightType);

                }
            }

        binaryExpr.setType(resultType);
        return resultType;
    }



    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        symbolTable.enterScope();
        for (AST node : block.getElems()) {
            node.visit(this, arg);
        }
        symbolTable.leaveScope();
        return null;
    }

    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        return visitBlock(statementBlock.getBlock(), arg);
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        // Assuming that the channel selector applies to an image or a pixel,
        // and that channel identifiers are strings like "red", "green", or "blue".
        String channel = channelSelector.color().name();

        // Verify that the channel is a valid identifier (e.g., "red", "green", "blue").
        if (!isValidChannel(channel)) {
            throw new TypeCheckException("Invalid channel identifier: " + channel);
        }

        // The type of a channel selector is INT since channels are individual color components.
        return Type.INT;
    }


    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        // Check the type of the guard expression
        Type guardType = (Type) conditionalExpr.getGuardExpr().visit(this, arg);
        if (guardType != Type.BOOLEAN) {
            throw new PLCCompilerException("Guard expression of a conditional should be of type BOOLEAN");
        }

        Type trueType = (Type) conditionalExpr.getTrueExpr().visit(this, arg);
        Type falseType = (Type) conditionalExpr.getFalseExpr().visit(this, arg);

        if (trueType != falseType) {
            throw new PLCCompilerException("Mismatched types in conditional expression");
        }

        conditionalExpr.setType(trueType);
        return trueType;
    }


    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        NameDef nD = symbolTable.lookup(declaration.getNameDef().getName());
        if (nD != null && (declaration.getNameDef().getType() == nD.getType()) && (symbolTable.lookupScope(declaration.getNameDef().getName()) != null)) {
            throw new TypeCheckException("Variable already declared: " + declaration.getNameDef().getName());
        }
        NameDef nameDef = declaration.getNameDef();
        Expr e = declaration.getInitializer();

        if (e != null) {
            e.visit(this, arg);
        }
        nameDef.visit(this, arg);
        if (!(e == null || e.getType() == nameDef.getType() || (e.getType() == Type.STRING && nameDef.getType() == Type.IMAGE))) {
            throw new TypeCheckException();
        }

        // Insert the variable into the symbol table
        symbolTable.insert(nameDef.getName(), nameDef);

        return nameDef.getType();

    }


    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        Type widthType = (Type) dimension.getWidth().visit(this, arg);
        Type heightType = (Type) dimension.getHeight().visit(this, arg);

        if (widthType != Type.INT || heightType != Type.INT) {
            throw new TypeCheckException("Dimension width and height should be of type INT");
        }

        return null;
    }


    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        // Enter a new scope for the do-statement block
        symbolTable.enterScope();
        for (GuardedBlock block : doStatement.getGuardedBlocks()) {
            block.visit(this, arg);
        }
        // Leave the scope after the do-statement block
        symbolTable.leaveScope();
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        Type redType = (Type) expandedPixelExpr.getRed().visit(this, arg);
        Type greenType = (Type) expandedPixelExpr.getGreen().visit(this, arg);
        Type blueType = (Type) expandedPixelExpr.getBlue().visit(this, arg);

        // Here we check if any of the types is not INT, since an image pixel should only consist of INT types
        if (redType != Type.INT || greenType != Type.INT || blueType != Type.INT) {
            throw new TypeCheckException("Invalid pixel expression types: red, green, and blue components must be of type INT.");
        }

        expandedPixelExpr.setType(Type.IMAGE);
        return Type.IMAGE;
    }


    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        // Type-check the guard expression. It should evaluate to a boolean.
        Type guardType = (Type) guardedBlock.getGuard().visit(this, arg);
        if (guardType != Type.BOOLEAN) {
            // For debugging purposes, remove in production code
            System.out.println("Guard expression evaluated to type: " + guardType);
            throw new TypeCheckException("Guard expression must evaluate to a boolean type");
        }

        // Enter a new scope for the block
        symbolTable.enterScope();

        // Visit all statements in the guarded block
        for (AST statement : guardedBlock.getBlock().getElems()) {
            statement.visit(this, arg);
        }

        // Leave the scope after visiting the block
        symbolTable.leaveScope();

        return null;
    }


    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        if (isSpecialIdentifier(identExpr.getName())) {
            identExpr.setType(Type.INT);
            return Type.INT;
        } else if (arg instanceof PixelSelector && ((PixelSelector) arg).isInLValueContext()) {
            // If the identifier is used in a pixel selector in an LValue context,
            // implicitly declare it by adding it to the symbol table
            NameDef nameDef = new SyntheticNameDef(identExpr.getName());
            symbolTable.enterScope();
            symbolTable.insert(identExpr.getName(), new SyntheticNameDef(nameDef.getName()));
            identExpr.setType(nameDef.getType());
            identExpr.setNameDef(nameDef);
            return nameDef.getType();
        } else {
            // Otherwise, proceed as normal
            NameDef nameDef = symbolTable.lookup(identExpr.getName());
            if (nameDef == null) {
                throw new TypeCheckException("Variable not declared: " + identExpr.getName());
            }
            identExpr.setType(nameDef.getType());
            identExpr.setNameDef(nameDef);
            return nameDef.getType();
        }
    }



    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {

        for (GuardedBlock block : ifStatement.getGuardedBlocks()) {
            block.visit(this, arg);
        }
        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        NameDef nameDef = symbolTable.lookup(lValue.getName());
        lValue.setNameDef(nameDef);
        if (nameDef == null) {
            throw new PLCCompilerException("Variable not declared (LValue): " + lValue.getName());
        }

        PixelSelector pixelSelector = lValue.getPixelSelector();
        if (pixelSelector != null) {
            if (nameDef.getType() != Type.IMAGE) {
                throw new PLCCompilerException("Pixel selectors can only be used with image types.");
            }
            Type selectorType = (Type) pixelSelector.visit(this, arg);
            if (selectorType != Type.PIXEL) {
                throw new PLCCompilerException("Invalid pixel selector usage for variable: " + lValue.getName());
            }
        }

        return nameDef.getType();
    }



//    @Override
//    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
//        symbolTable.insert(nameDef.getName(), nameDef);
//
//        if(nameDef.getDimension() != null){
//            nameDef.getDimension().visit(this, arg);
//        }
//
//        return nameDef.getType();
//    }
    private int variableCounter = 1;

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        String uniqueName = nameDef.getName() + "$" + symbolTable.getCurrentScopeIndex();
        nameDef.setJavaName(uniqueName);

        symbolTable.insert(uniqueName, nameDef);

        if (nameDef.getDimension() != null) {
            nameDef.getDimension().visit(this, arg);
        }

        return nameDef.getType();
    }



    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        numLitExpr.setType(Type.INT);
        return Type.INT;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        Expr xExpr = pixelSelector.xExpr();
        Expr yExpr = pixelSelector.yExpr();

        if (xExpr instanceof IdentExpr && symbolTable.lookup(((IdentExpr) xExpr).getName()) == null) {
            symbolTable.insert(((IdentExpr) xExpr).getName(), new SyntheticNameDef(((IdentExpr) xExpr).getName()));
        }

        if (yExpr instanceof IdentExpr && symbolTable.lookup(((IdentExpr) yExpr).getName()) == null) {
            symbolTable.insert(((IdentExpr) yExpr).getName(), new SyntheticNameDef(((IdentExpr) yExpr).getName()));
        }

        Type xType = (Type) xExpr.visit(this, arg);
        Type yType = (Type) yExpr.visit(this, arg);

        if (xType != Type.INT || yType != Type.INT) {
            throw new TypeCheckException("Pixel selector coordinates must be of integer type");
        }

        return Type.PIXEL;
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        Type primaryType = (Type) postfixExpr.primary().visit(this, arg);
        if (postfixExpr.pixel() != null && postfixExpr.channel() != null) {
            postfixExpr.pixel().visit(this, arg);
            postfixExpr.setType(Type.INT);
        } else if (postfixExpr.pixel() != null) {
            postfixExpr.pixel().visit(this, arg);
            postfixExpr.setType(Type.PIXEL);
        } else {
            postfixExpr.setType(primaryType);
        }

        return postfixExpr.getType();
    }



    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        symbolTable.enterScope();

        // Validate function parameters
        for (NameDef nameDef : program.getParams()) {
            if (nameDef.getType() == Type.VOID) {
                throw new TypeCheckException("Function parameters cannot have 'void' type.");
            }
            // Check if parameters already exist in the symbol table
            if (symbolTable.lookup(nameDef.getName()) != null) {
                throw new TypeCheckException("Function parameter redefinition: " + nameDef.getName());
            }
            // Insert function parameters into the symbol table
            nameDef.visit(this, arg);
            symbolTable.insert(nameDef.getName(), nameDef);
        }

        functionReturnTypes.push(program.getType());

        // Visit the block
        program.getBlock().visit(this, arg);

        functionReturnTypes.pop();

        symbolTable.leaveScope();
        return null;
    }



    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        Type returnType = (Type) returnStatement.getE().visit(this, arg);

        Type currentFunctionReturnType = functionReturnTypes.peek();

        if (currentFunctionReturnType == Type.VOID && returnType != Type.VOID) {
            throw new TypeCheckException("Function with return type void cannot return a value");
        } else if (currentFunctionReturnType != returnType) {
            throw new TypeCheckException("Return type mismatch: expected " + currentFunctionReturnType + " but found " + returnType);
        }

        return returnType;
    }



    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        stringLitExpr.setType(Type.STRING);
        return Type.STRING;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        Type operandType = (Type) unaryExpr.getExpr().visit(this, arg);

        switch (unaryExpr.getOp()) {
            case RETURN:
                if (operandType != Type.INT) {
                    throw new TypeCheckException("Invalid type for '^' operation");
                }
                unaryExpr.setType(Type.INT);
                return Type.INT;

            case BANG:
                if (operandType != Type.BOOLEAN) {
                    throw new TypeCheckException("Invalid type for '!' operation");
                }
                unaryExpr.setType(Type.BOOLEAN);
                return Type.BOOLEAN;

            case MINUS:
                if (operandType != Type.INT) {
                    throw new TypeCheckException("Invalid type for '-' operation");
                }
                unaryExpr.setType(Type.INT);
                return Type.INT;

            default:
                throw new TypeCheckException("Unknown unary operator: " + unaryExpr.getOp());
        }
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        writeStatement.getExpr().visit(this, arg);
        return null;
    }


    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        booleanLitExpr.setType(Type.BOOLEAN);
        return Type.BOOLEAN;
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        String constName = constExpr.getName();
        if ("RED".equals(constName) || "GREEN".equals(constName) || "BLUE".equals(constName)) {
            constExpr.setType(Type.PIXEL);
            return Type.PIXEL; // Return PIXEL type for pixel constants
        }
        constExpr.setType(Type.INT);
        return Type.INT;
    }




    private boolean isSpecialIdentifier(String name) {
        // Add the logic to identify if 'name' is a special identifier like 'z'
        // This might be a predefined list or some pattern that these identifiers follow
        // For example, if 'z' is the only special identifier, you can directly check for it
        return "z" == (name) || "b" == (name) || "y" == (name); // Adjust this condition based on your language's semantics
    }

    private boolean isValidChannel(String channel) {
        // Example implementation
        return "red".equalsIgnoreCase(channel) || "green".equalsIgnoreCase(channel) || "blue".equalsIgnoreCase(channel);
    }

}
