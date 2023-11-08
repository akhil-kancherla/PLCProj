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

        return null;
    }




    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        Type leftType = (Type) binaryExpr.getLeftExpr().visit(this, arg);
        Type rightType = (Type) binaryExpr.getRightExpr().visit(this, arg);
        Type resultType = null;

        switch (binaryExpr.getOpKind()) {
            case DIV:
                if ((leftType == (Type.INT) || leftType == (Type.PIXEL) || leftType == (Type.IMAGE)) && rightType == (leftType)) {
                    resultType = leftType;
                }
                else if ((leftType == (Type.PIXEL) || leftType == (Type.IMAGE)) && rightType == (Type.INT)) {
                    resultType = leftType;
                }
                break;
            case AND, OR:
                if (leftType == (Type.BOOLEAN) && rightType == (Type.BOOLEAN)) {
                    resultType = Type.BOOLEAN;
                }
                break;
            case BITAND, BITOR:
                if (leftType == (Type.PIXEL) && rightType == (Type.PIXEL)) {
                    resultType = Type.PIXEL;
                }
                break;
            case PLUS:
                if (rightType == (leftType)) {
                    resultType = leftType;
                }
                break;
            case MINUS:
                if ((leftType == (Type.INT) || leftType == (Type.PIXEL) || leftType == (Type.IMAGE)) && rightType == (leftType)) {
                    resultType = leftType;
                }
                break;
            case TIMES:
                if ((leftType == (Type.INT) || leftType == (Type.PIXEL) || leftType == (Type.IMAGE)) && rightType == (leftType)) {
                    resultType = leftType;
                }
                else if ((leftType == (Type.PIXEL) || leftType == (Type.IMAGE)) && rightType == (Type.INT)) {
                    resultType = leftType;
                }
                break;
            case EXP:
                if (leftType == (Type.INT) && rightType == (Type.INT)) {
                    resultType = Type.INT;
                }
                else if (leftType == (Type.PIXEL) && rightType == (Type.INT)) {
                    resultType = Type.PIXEL;
                }
                break;
            case EQ:
                if (rightType == (leftType)) {
                    resultType = leftType;
                }
                break;
            case LT:
                if (leftType == (Type.INT) && rightType == (Type.INT)) {
                    resultType = Type.BOOLEAN;
                }
                break;
            case GT:
                if (leftType == (Type.INT) && rightType == (Type.INT)) {
                    resultType = Type.BOOLEAN;
                }
                break;
            case LE:
                if (leftType == (Type.INT) && rightType == (Type.INT)) {
                    resultType = Type.BOOLEAN;
                }
                break;
            case GE:
                if (leftType == (Type.INT) && rightType == (Type.INT)) {
                    resultType = Type.BOOLEAN;
                }
                break;
            default:
                throw new TypeCheckException("Unknown binary operator");
        }

        if (resultType == null) {
            throw new TypeCheckException("Type mismatch or unsupported operation in binary expression.");
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
        declaration.getNameDef().visit(this, arg); // This should also handle type setting

        if (declaration.getInitializer() != null) {
            Type initializerType = (Type) declaration.getInitializer().visit(this, arg);
            declaration.getNameDef().setType(initializerType);
            declaration.getNameDef().setInitialized(true); // Mark the variable as initialized
        } else {
            declaration.getNameDef().setType(Type.VOID);
        }

        symbolTable.insert(declaration.getNameDef().getName(), declaration.getNameDef());

        return null;
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
        Type conditionType = (Type) ifStatement.visit(this, arg);
        if (conditionType != Type.BOOLEAN) {
            throw new TypeCheckException("If statement condition must be of boolean type");
        }
        ifStatement.visit(this, arg);
        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        NameDef nameDef = symbolTable.lookup(lValue.getName());
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



    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        symbolTable.insert(nameDef.getName(), nameDef);

        if(nameDef.getDimension() != null){
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
        Type xType = (Type) pixelSelector.xExpr().visit(this, arg);
        Type yType = (Type) pixelSelector.yExpr().visit(this, arg);
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
