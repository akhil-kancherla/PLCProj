package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.CodeGenException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;
import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;
import java.awt.Color;
import edu.ufl.cise.cop4020fa23.runtime.FileURLIO;
import edu.ufl.cise.cop4020fa23.runtime.*;



import java.sql.SQLOutput;
import java.util.List;

//import edu.ufl.cise.cop4020fa23.ast.ASTVisitor;

public class CodeGenVisitor implements ASTVisitor {

    private int uniqueId = 0;

    private String getUniqueName(String name) {
        return name + "_" + uniqueId++;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {

          StringBuilder code = new StringBuilder();

          code.append("package edu.ufl.cise.cop4020fa23;\n\n");

          code.append("import edu.ufl.cise.cop4020fa23.runtime.*;\n\n");


          String className = program.getName();
          code.append("public class ").append(className).append(" {\n");

          String returnType = convertType(program.getType());
          System.out.println(returnType);
          code.append("    public static ").append(returnType).append(" apply(");

          List<NameDef> params = program.getParams();
          for (int i = 0; i < params.size(); i++) {
              NameDef param = params.get(i);

              if (i>0) {
                  code.append(", ");
              }

              String paramName = param.getJavaName();
              if (isReservedWord(paramName) || paramName.equals("true") || paramName.equals("false")) {
                  code.append(convertType(param.getType())).append(" ").append(paramName).append("$");
                  param.visit(this,arg);
              } else {
                  code.append(convertType(param.getType())).append(" ").append(paramName);
              }

          }

          code.append(") {\n");

          Block block = program.getBlock();
          code.append(block.visit(this,arg));

          code.append("    }\n");
          code.append("}\n");

          return code.toString();

    }

    private String convertType(Type type) {
        switch (type) {
            case STRING:
                return "String";
            case PIXEL:
                return "int";
            case INT:
                return "int";
            case BOOLEAN:
                return "Boolean";
            case IMAGE:
                return "BufferedImage";
            case VOID:
                return "void";
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }



    public boolean isReservedWord(String word) {
        String[] reservedWords = {"image", "pixel", "int", "string", "void", "boolean", "write", "height", "width", "if", "fi", "do", "od", "red", "green", "blue"};
        for (String reservedWord : reservedWords) {
            if (reservedWord.equals(word)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        String varName = (String) assignmentStatement.getlValue().visit(this, arg);
        String exprCode = (String) assignmentStatement.getE().visit(this, arg);
        return varName + " = " + exprCode + ";\n";
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        String leftExprCode = (String) binaryExpr.getLeftExpr().visit(this, arg);
        String rightExprCode = (String) binaryExpr.getRightExpr().visit(this, arg);
        if (binaryExpr.getOpKind() == Kind.EXP) {
            return "((int)Math.round(Math.pow(" + leftExprCode + "," + rightExprCode + ")))";
        } else if (binaryExpr.getOpKind() == Kind.PLUS) {
            if (isImageType(binaryExpr.getLeftExpr().getType()) && isImageType(binaryExpr.getRightExpr().getType())) {
                return "ImageOps.binaryImageImageOp(" + leftExprCode + ", " + rightExprCode + ")";
            } else if (isPixelType(binaryExpr.getLeftExpr().getType()) && isPixelType(binaryExpr.getRightExpr().getType())) {
                return "ImageOps.binaryPackedPixelPixelOp(ImageOps.OP.PLUS, " + leftExprCode + ", " + rightExprCode + ")";
            } else if (isImageType(binaryExpr.getLeftExpr().getType()) && isPixelType(binaryExpr.getRightExpr().getType())) {
                return "ImageOps.binaryImagePixelOp(ImageOps.OP.PLUS, " + leftExprCode + ", " + rightExprCode + ")";
            } else if (isPixelType(binaryExpr.getLeftExpr().getType()) && isImageType(binaryExpr.getRightExpr().getType())) {
                return "ImageOps.binaryImagePixelOp(ImageOps.OP.PLUS, " + rightExprCode + ", " + leftExprCode + ")";
            }
        }
        return leftExprCode + " " + convert(binaryExpr.getOp().kind()) + " " + rightExprCode;
    }

    private boolean isImageType(Type type) {
        return type == Type.IMAGE;
    }

    private boolean isPixelType(Type type) {
        return type == Type.PIXEL;
    }


    String convert(Kind kind) throws CodeGenException{
        switch(kind){
            case BANG:
                return "!";
            case EQ:
                return "==";
            case LPAREN:
                return "(";
            case RPAREN:
                return ")";
            case ASSIGN:
                return"=";
            case LT:
                return "<";
            case GT:
                return ">";
            case PLUS:
                return "+";
            case MINUS:
                return "-";
            case OR:
                return "||";
            case LSQUARE:
                return "[";
            case RSQUARE:
                return "]";
            case TIMES:
                return "*";
            case QUESTION:
                return "?";
            case LE:
                return "<=";
        }
         throw new CodeGenException("Could not match kind" + kind);
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        StringBuilder javaCode = new StringBuilder();
        javaCode.append("{\n");
        for (Block.BlockElem blockElem : block.getElems()) {
            javaCode.append((String) blockElem.visit(this, arg));
        }
        javaCode.append("}\n");
        return javaCode.toString();
    }

    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        return (String) statementBlock.getBlock().visit(this, arg);
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        return channelSelector.color();
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        String conditionCode = (String) conditionalExpr.getGuardExpr().visit(this, arg);
        String trueExprCode = (String) conditionalExpr.getTrueExpr().visit(this, arg);
        String falseExprCode = (String) conditionalExpr.getFalseExpr().visit(this, arg);
        return "(" + conditionCode + " ? " + trueExprCode + " : " + falseExprCode + ")";
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        String varName = declaration.getNameDef().getJavaName();
        Type varType = declaration.getNameDef().getType();
        String varInitialization = "";
        if (declaration.getInitializer() != null){
            varInitialization = " = " + declaration.getInitializer().visit(this, arg);
        }
        return convertType(varType) + " " + varName + varInitialization + ";\n";
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
//        return dimension.getWidth() + ", " + dimension.getHeight();
        return null;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        StringBuilder doStatementCode = new StringBuilder();

        doStatementCode.append("do {\n");

        for (GuardedBlock guardedBlock : doStatement.getGuardedBlocks()) {
            String guardExprCode = (String) guardedBlock.getGuard().visit(this, arg);
            String blockCode = (String) guardedBlock.getBlock().visit(this, arg);
            doStatementCode.append("if (").append(guardExprCode).append(") ").append(blockCode).append("\n");
        }

        doStatementCode.append("} while (false);\n");

        return doStatementCode.toString();
    }


    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        String redCode = (String) expandedPixelExpr.getRed().visit(this, arg);
        String greenCode = (String) expandedPixelExpr.getGreen().visit(this, arg);
        String blueCode = (String) expandedPixelExpr.getBlue().visit(this, arg);
        return "PixelOps.pack(" + redCode + ", " + greenCode + ", " + blueCode + ")";
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        String conditionCode = (String) guardedBlock.getGuard().visit(this, arg);
        String blockCode = (String) guardedBlock.getBlock().visit(this, arg);
        return "if (" + conditionCode + ") " + blockCode;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        return identExpr.getNameDef().getJavaName();
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        StringBuilder ifStatementCode = new StringBuilder();
        for (GuardedBlock guardedBlock : ifStatement.getGuardedBlocks()) {
            String guardExprCode = (String) guardedBlock.getGuard().visit(this, arg);
            String blockCode = (String) guardedBlock.getBlock().visit(this, arg);
            ifStatementCode.append("if (").append(guardExprCode).append(") ").append(blockCode).append("\n");
        }
        return ifStatementCode.toString();
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        return lValue.getNameDef().getJavaName();
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        return nameDef.getJavaName();
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        return String.valueOf(numLitExpr.getText());
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        String xExprCode = (String) pixelSelector.xExpr().visit(this, arg);
        String yExprCode = (String) pixelSelector.yExpr().visit(this, arg);
        return "[" + xExprCode + ", " + yExprCode + "]";
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        String exprCode = (String) postfixExpr.primary().visit(this, arg);
        String pixelSelectorCode = (String) postfixExpr.pixel().visit(this, arg);
        return exprCode + pixelSelectorCode;
    }


    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        String exprCode = (String) returnStatement.getE().visit(this, arg);
        return "return " + exprCode + ";\n";
    }


    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        return stringLitExpr.getText();
    }


    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        String exprCode = (String) unaryExpr.getExpr().visit(this, arg);
        if (unaryExpr.getOp() == Kind.MINUS) {
            return "-" + "(" + exprCode + ")";
        }
        else if (unaryExpr.getOp() == Kind.BANG) {
            return "!" + exprCode;
        } else if (unaryExpr.getOp() == Kind.QUESTION){

        }
        return unaryExpr.getOp() + exprCode;
    }


    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        String exprCode = (String) writeStatement.getExpr().visit(this, arg);
        return "ConsoleIO.write(" + exprCode + ");\n";
    }


    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        return String.valueOf(booleanLitExpr.getText().toLowerCase());
    }


    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        if ("Z".equals(constExpr.getName())) {
            return "255";
        } else {
            Color colorConstant = getColorConstant(constExpr.getName());
            int rgb = colorConstant.getRGB();
            String hexColor = "0x" + Integer.toHexString(rgb);
            return hexColor;
        }
    }

    private Color getColorConstant(String plcLangConstant) throws PLCCompilerException {
        switch (plcLangConstant) {
            case "BLUE":
                return Color.BLUE;
            case "RED":
                return Color.RED;
            case "PINK":
                return Color.PINK;
            case "GREEN":
                return Color.GREEN;
            default:
                throw new PLCCompilerException("Unknown PLC Lang constant: " + plcLangConstant);
        }
    }


}
