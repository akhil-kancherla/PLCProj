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
        Type rhsType = (Type) assignmentStatement.getE().visit(this, arg);

        Type lhsType = (Type) assignmentStatement.getlValue().visit(this, arg);

        if (rhsType != lhsType) {
            throw new PLCCompilerException("Type mismatch in assignment");
        }

        return lhsType;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        Type leftType = (Type) binaryExpr.getLeftExpr().visit(this, arg);
        Type rightType = (Type) binaryExpr.getRightExpr().visit(this, arg);
        Type resultType = null;

        switch (binaryExpr.getOpKind()) {
            case DIV:
                if (leftType == Type.INT && rightType == Type.INT) {
                    resultType = Type.INT;
                }
                break;
            case AND:
            case BITAND:
                if (leftType == Type.BOOLEAN && rightType == Type.BOOLEAN) {
                    resultType = Type.BOOLEAN;
                }
                break;
            case OR:
            case BITOR:
                if (leftType == Type.BOOLEAN && rightType == Type.BOOLEAN) {
                    resultType = Type.BOOLEAN;
                }
                break;
            case PLUS:
                if (leftType == Type.INT && rightType == Type.INT) {
                    resultType = Type.INT;
                } else if (leftType == Type.STRING && rightType == Type.STRING) {
                    resultType = Type.STRING;
                }
                break;
            case MINUS:
            case TIMES:
            case EXP:
                if (leftType == Type.INT && rightType == Type.INT) {
                    resultType = Type.INT;
                }
                break;
            case EQ:
            case LT:
            case GT:
            case LE:
            case GE:
                if (leftType == rightType) {
                    resultType = Type.BOOLEAN;
                }
                break;
            case RARROW:
                if (leftType == rightType) {
                    resultType = leftType;
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
        for (AST node : statementBlock.getBlock().getElems()) {
            node.visit(this, arg);
        }
        return null;
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        return null;
    }


    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
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
        if (declaration.getInitializer() != null) {
            Type initializerType = (Type) declaration.getInitializer().visit(this, arg);
            declaration.getNameDef().setType(initializerType);
        } else {
            declaration.getNameDef().setType(Type.VOID);
        }

        symbolTable.insert(declaration.getNameDef().getName(), declaration.getNameDef());
        return null;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        for (GuardedBlock block : doStatement.getGuardedBlocks()) {
            block.visit(this, arg);
        }
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        Type redType = (Type) expandedPixelExpr.getRed().visit(this, arg);
        Type greenType = (Type) expandedPixelExpr.getGreen().visit(this, arg);
        Type blueType = (Type) expandedPixelExpr.getBlue().visit(this, arg);

        if (redType == Type.INT && greenType == Type.INT && blueType == Type.INT) {
            expandedPixelExpr.setType(Type.IMAGE);
            return Type.IMAGE;
        } else {
            throw new PLCCompilerException("Invalid pixel expression types");
        }
    }


    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        NameDef nameDef = symbolTable.lookup(identExpr.getName());
        if (nameDef == null) {
            throw new TypeCheckException("Variable not declared: " + identExpr.getName());
        }
        identExpr.setType(nameDef.getType());
        return nameDef.getType();
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
        PixelSelector pixelSelector = lValue.getPixelSelector();
        if (pixelSelector != null) {
            pixelSelector.visit(this, arg);
        }
        NameDef nameDef = symbolTable.lookup(lValue.getName());
        if (nameDef == null) {
            throw new PLCCompilerException("Variable not declared: " + lValue.getName());
        }
        return nameDef.getType();
    }


    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        symbolTable.insert(nameDef.getName(), nameDef);

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

        if (postfixExpr.pixel() != null) {
            Type pixelType = (Type) postfixExpr.pixel().visit(this, arg);
        }

        if (postfixExpr.channel() != null) {
            Type channelType = (Type) postfixExpr.channel().visit(this, arg);
        }

        postfixExpr.setType(primaryType);
        return primaryType;
    }


    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        symbolTable.enterScope();

        for (NameDef nameDef : program.getParams()) {
            if (nameDef.getType() == Type.VOID) {
                throw new TypeCheckException("Function parameters cannot have 'void' type.");
            }
            nameDef.visit(this, arg);
        }

        functionReturnTypes.push(program.getType());

        for (NameDef nameDef : program.getParams()) {
            nameDef.visit(this, arg);
        }

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
        Type valueType = (Type) writeStatement.getExpr().visit(this, arg);

        if (valueType != Type.STRING && valueType != Type.INT) {
            throw new TypeCheckException("Can only write string or integer values");
        }

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

}
