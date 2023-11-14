package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;

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

          code.append("import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;\n\n");

          String className = program.getName();
          code.append("public class ").append(className).append(" {\n");

          String returnType = program.getType().toString().toLowerCase();
          System.out.println(returnType);
          code.append("    public static ").append(returnType).append(" apply(");

          List<NameDef> params = program.getParams();
          for (int i = 0; i < params.size(); i++) {
              NameDef param = params.get(i);

              if (i>0) {
                  code.append(", ");
              }

              if (isReservedWord(param.getName())) {
                  code.append(program.getType().toString().toLowerCase()).append(" ").append(param.getName()).append("$" + i);
              }
              else {
                  code.append(program.getType().toString().toLowerCase()).append(" ").append(param.getName());
              }
          }

          code.append(") {\n");

          Block block = program.getBlock();
          code.append(visitBlock(block, null));

          code.append("    }\n");
          code.append("}\n");

          return code.toString();

    }

    public boolean isReservedWord(String word) {
        String[] reservedWords = {"true", "false", "image", "pixel", "int", "string", "void", "boolean", "write", "height", "width", "if", "fi", "do", "od", "red", "green", "blue"};
        for (String reservedWord : reservedWords) {
            if (reservedWord.equals(word)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        String varName = assignmentStatement.getlValue().getName();
        String exprCode = (String) assignmentStatement.getE().visit(this, arg);
        return varName + " = " + exprCode + ";\n";
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        String leftExprCode = (String) binaryExpr.getLeftExpr().visit(this, arg);
        String rightExprCode = (String) binaryExpr.getRightExpr().visit(this, arg);
        return leftExprCode + " " + binaryExpr.getOp() + " " + rightExprCode;
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
//        return channelSelector.color();
        return null;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        String conditionCode = (String) conditionalExpr.getGuardExpr().visit(this, arg);
        String trueExprCode = (String) conditionalExpr.getTrueExpr().visit(this, arg);
        String falseExprCode = (String) conditionalExpr.getFalseExpr().visit(this, arg);
        return conditionCode + " ? " + trueExprCode + " : " + falseExprCode;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        String varName = declaration.getNameDef().getName();
        return declaration.getNameDef().getType() + " " + varName + ";\n";
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
//        return dimension.getWidth() + ", " + dimension.getHeight();
        return null;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
//        javaCode.append("do {\n");
//        for (GuardedBlock guardedBlock : doStatement.getGuardedBlocks()) {
//            String guardExprCode = (String) guardedBlock.getGuard().visit(this, arg);
//            String blockCode = (String) guardedBlock.getBlock().visit(this, arg);
//            javaCode.append("if (").append(guardExprCode).append(") ").append(blockCode).append("\n");
//        }
//        javaCode.append("} while (true);\n");
//        return javaCode.toString();
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
//        String redCode = (String) expandedPixelExpr.getRed().visit(this, arg);
//        String greenCode = (String) expandedPixelExpr.getGreen().visit(this, arg);
//        String blueCode = (String) expandedPixelExpr.getBlue().visit(this, arg);
//        return "new Color(" + redCode + ", " + greenCode + ", " + blueCode + ")";
        return null;
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
//        String conditionCode = (String) guardedBlock.getGuard().visit(this, arg);
//        String blockCode = (String) guardedBlock.getBlock().visit(this, arg);
//        return "if (" + conditionCode + ") " + blockCode;
        return null;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        return identExpr.getName();
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
//        StringBuilder ifStatementCode = new StringBuilder();
//        for (GuardedBlock guardedBlock : ifStatement.getGuardedBlocks()) {
//            String guardExprCode = (String) guardedBlock.getGuard().visit(this, arg);
//            String blockCode = (String) guardedBlock.getBlock().visit(this, arg);
//            ifStatementCode.append("if (").append(guardExprCode).append(") ").append(blockCode).append("\n");
//        }
//        return ifStatementCode.toString();
        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        return null;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        return nameDef.getName();
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        return String.valueOf(numLitExpr.getText());
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
//        String xExprCode = (String) pixelSelector.xExpr().visit(this, arg);
//        String yExprCode = (String) pixelSelector.yExpr().visit(this, arg);
//        return "[" + xExprCode + ", " + yExprCode + "]";
        return null;
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
//        String exprCode = (String) postfixExpr.primary().visit(this, arg);
//        String pixelSelectorCode = (String) postfixExpr.pixel().visit(this, arg);
//        return exprCode + pixelSelectorCode;
        return null;
    }



    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        String exprCode = (String) returnStatement.getE().visit(this, arg);
        return "return " + exprCode + ";\n";
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        return "\"" + stringLitExpr.getText() + "\"";
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        String exprCode = (String) unaryExpr.getExpr().visit(this, arg);
        return unaryExpr.getOp() + exprCode;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        String exprCode = (String) writeStatement.getExpr().visit(this, arg);
        return "System.out.println(" + exprCode + ");\n";
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        return String.valueOf(booleanLitExpr.getText());
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
//        return (String) constExpr.visit(this, arg);
        return null;
    }
}
