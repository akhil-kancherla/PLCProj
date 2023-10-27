package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import java.util.List;
import edu.ufl.cise.cop4020fa23.LeBlancSymbolTable;


public class TypeCheckVisitor implements ASTVisitor {
    LeBlancSymbolTable symbolTable = new LeBlancSymbolTable();

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        symbolTable.enterScope();

        // Handle the program type.
        Type type = Type.kind2type(program.getTypeToken().kind());
        program.setType(type);

        // Check the children (NameDefs and Block).
        for (NameDef nameDef : program.getParams()) {
            nameDef.visit(this, arg);
        }
        program.getBlock().visit(this, arg);

        symbolTable.leaveScope();
        return type;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        symbolTable.enterScope();

        // Check children in the block.
        for (Block.BlockElem blockElem : block.getElems()) {
            blockElem.visit(this, arg);
        }

        symbolTable.leaveScope();

        return null;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        Type type = Type.kind2type(nameDef.getTypeToken().kind());

        // Check if Dimension is present.
        if (nameDef.getDimension() != null) {
            if (type != Type.IMAGE) {
                throw new PLCCompilerException("Type of NameDef should be IMAGE when Dimension is present.");
            }
        } else if (type != Type.INT && type != Type.BOOLEAN && type != Type.STRING && type != Type.PIXEL && type != Type.IMAGE) {
            throw new PLCCompilerException("Invalid Type for NameDef.");
        }

        //nameDef.setType(type);

        // Insert the NameDef into the symbol table.
        symbolTable.insert(nameDef.getName());

        return type;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        NameDef nameDef = declaration.getNameDef();
        Expr initializer = declaration.getInitializer();

        // Visit the initializer Expr node if it's present
        if (initializer != null) {
            Type nameDefType = nameDef.getType();
            Type exprType = (Type) initializer.visit(this, arg);

            // Check the conditions specified in the rule
            if (exprType == null || exprType == nameDefType) {
                // Type is valid, no need to do anything
            } else if (exprType == Type.STRING && nameDefType == Type.IMAGE) {
                // Type is valid, no need to do anything
            } else {
                throw new PLCCompilerException("Invalid type for Declaration.");
            }
        }
        return null;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        Expr guardExpr = conditionalExpr.getGuardExpr();
        Expr trueExpr = conditionalExpr.getTrueExpr();
        Expr falseExpr = conditionalExpr.getFalseExpr();

        // Visit the child expressions
        Type guardType = (Type) guardExpr.visit(this, arg);
        Type trueType = (Type) trueExpr.visit(this, arg);
        Type falseType = (Type) falseExpr.visit(this, arg);

        // Check the conditions
        if (guardType == Type.BOOLEAN && trueType != null && trueType == falseType) {
            conditionalExpr.setType(trueType);
            return trueType;
        } else {
            throw new PLCCompilerException("Invalid types in ConditionalExpr.");
        }
        //return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        Expr leftExpr = binaryExpr.getLeftExpr();
        Expr rightExpr = binaryExpr.getRightExpr();
        IToken operator = binaryExpr.getOp();

        // Visit the left and right expressions
        Type leftType = (Type) leftExpr.visit(this, arg);
        Type rightType = (Type) rightExpr.visit(this, arg);

        // Determine the resulting type using inferBinaryType
        Type resultType = inferBinaryType(leftType, operator, rightType);

        if (resultType != null) {
            binaryExpr.setType(resultType);
            return resultType;
        } else {
            throw new PLCCompilerException("Invalid types in BinaryExpr.");
        }
        //return null;
    }

    private Type inferBinaryType(Type leftType, IToken operator, Type rightType) {
        if (leftType == Type.PIXEL && (operator.kind() == Kind.BITAND || operator.kind() == Kind.BITAND) && rightType == Type.PIXEL) {
            return Type.PIXEL;
        }
        else if (leftType == Type.BOOLEAN && (operator.kind() == Kind.AND || operator.kind() == Kind.OR) && rightType == Type.BOOLEAN) {
            return Type.BOOLEAN;
        }
        else if (leftType == Type.INT && (operator.kind() == Kind.LT || operator.kind() == Kind.GT || operator.kind() == Kind.LE || operator.kind() == Kind.GE) && rightType == Type.INT) {
            return Type.BOOLEAN;
        }
        else if (leftType == Type.INT && operator.kind() == Kind.EXP && rightType == Type.INT) {
            return Type.INT;
        }
        else if (leftType == Type.PIXEL && operator.kind() == Kind.EXP && rightType == Type.INT) {
            return Type.PIXEL;
        }
        else if ((leftType == Type.INT || leftType == Type.PIXEL || leftType == Type.IMAGE) && (operator.kind() == Kind.MINUS || operator.kind() == Kind.TIMES || operator.kind() == Kind.DIV || operator.kind() == Kind.MOD) && rightType == leftType) {
            return leftType;
        }
        else if ((leftType == Type.PIXEL || leftType == Type.IMAGE) && (operator.kind() == Kind.TIMES || operator.kind() == Kind.DIV || operator.kind() == Kind.MOD) && rightType == Type.INT) {
            return leftType;
        }
        else if (operator.kind() == Kind.EQ && rightType == leftType) {
            return Type.BOOLEAN;
        }
        else if (operator.kind() == Kind.PLUS && rightType == leftType) {
            return leftType;
        }
        return null;
    }

    

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        return null;
    }



    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        return null;
    }



    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        return null;
    }



    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        Type type = Type.INT;
        numLitExpr.setType(type);
        return type;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        Type type = Type.STRING;
        stringLitExpr.setType(type);
        return type;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        writeStatement.getExpr().visit(this, arg);
        return writeStatement;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        return null;
    }
}
