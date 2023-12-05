package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.CodeGenException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;
import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
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
          code.append("import edu.ufl.cise.cop4020fa23.runtime.ImageOps;\n\n");
        code.append("import edu.ufl.cise.cop4020fa23.runtime.PixelOps;\n\n");

          code.append("import java.awt.image.BufferedImage;\n\n");


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
        PixelSelector pixelSelector = assignmentStatement.getlValue().getPixelSelector();
        ChannelSelector channelSelector = assignmentStatement.getlValue().getChannelSelector();
        Type exprType = assignmentStatement.getE().getType();
        if (assignmentStatement.getlValue().getVarType() == Type.IMAGE) {
            if (pixelSelector == null && channelSelector == null) {
                if (exprType == Type.IMAGE) {
                    return "ImageOps.copyInto(" + assignmentStatement.getE() + ", " + assignmentStatement.getlValue() + ")";
                }
                else if (exprType == Type.PIXEL) {
                    return "ImageOps.setAllPixels(" + assignmentStatement.getlValue().visit(this,arg) + ", " + assignmentStatement.getE().visit(this, arg) + ");\n";
                }
                else if (exprType == Type.STRING) {
                    return "ImageOps.copyInto((FileURLIO.readImage(" + assignmentStatement.getE() + "))," + assignmentStatement.getlValue() + ")\n";
                }
            }
            else if(pixelSelector != null && channelSelector == null){
                String xexpr = (String) assignmentStatement.getlValue().getPixelSelector().xExpr().visit(this,arg);
                String yexpr = (String) assignmentStatement.getlValue().getPixelSelector().yExpr().visit(this,arg);
                System.out.println(assignmentStatement.getlValue().getPixelSelector().xExpr().visit(this,arg));
                return "for (int " + xexpr + "=0; "+xexpr+"<" + assignmentStatement.getlValue().getNameDef().getJavaName()   + ".getWidth();"+ xexpr +"++){" + "for (int " + yexpr + "=0; "+ yexpr +"<" + assignmentStatement.getlValue().getNameDef().getJavaName() + ".getHeight();"+ yexpr + "++){\n" + "ImageOps.setRGB(" + assignmentStatement.getlValue().getNameDef().visit(this,arg) + "," + xexpr+","+ yexpr+"," + assignmentStatement.getE().visit(this,arg) + "); } };\n";
            }
            else if (channelSelector != null) {
                throw new UnsupportedOperationException("Null channelSelector in assignmentStatement");
            }

        }
        if (assignmentStatement.getlValue().getVarType() == Type.PIXEL && channelSelector != null) {
            String channelColor = "";
            if (channelSelector.color() == Kind.RES_red) {
                channelColor = "Red";
            }
            else if (channelSelector.color() == Kind.RES_green) {
                channelColor = "Green";
            }
            else if (channelSelector.color() == Kind.RES_blue) {
                channelColor = "Blue";
            }
            return assignmentStatement.getlValue().visit(this, arg) + "=PixelOps.set" + channelColor + "(" + varName + ", " + exprCode + ");";
        }
        String pixStatement = (String) assignmentStatement.getE().visit(this, arg);
        System.out.println(assignmentStatement.getlValue().getType());
        System.out.println(assignmentStatement.getE().getType());
        if (assignmentStatement.getlValue().getType() == Type.PIXEL && assignmentStatement.getE().getType() == Type.INT) {
            return assignmentStatement.getlValue().visit(this, arg) + " = " + "PixelOps.pack(" + pixStatement + ", " + pixStatement + ", " + pixStatement + ");";
        }

        return varName + " = " + exprCode + ";\n";
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
            StringBuilder binary = new StringBuilder();
            Type leftType = binaryExpr.getLeftExpr().getType();
            Type rightType = binaryExpr.getRightExpr().getType();

            //String imageOpsMethod = getImageOpsMethodName(leftType, rightType, binaryExpr.getOp());

            if(binaryExpr.getLeftExpr().getType() == Type.STRING && binaryExpr.getOp().kind() == Kind.EQ)
            {
                binary.append(binaryExpr.getLeftExpr().visit(this, arg));
                binary.append(".equals(");
                binary.append(binaryExpr.getRightExpr().visit(this, arg));
                binary.append(")");
            } else if(binaryExpr.getOp().kind() == Kind.EXP)
            {
                binary.append("((int)Math.round(Math.pow(");
                binary.append(binaryExpr.getLeftExpr().visit(this, arg));
                binary.append(", ");
                binary.append(binaryExpr.getRightExpr().visit(this, arg));
                binary.append(")))");
            }
            else if (leftType == Type.PIXEL && rightType == Type.PIXEL && binaryExpr.getOp().kind() == Kind.PLUS) {
                // Example for image-image operation
                binary.append("(")
                        .append("ImageOps.").append("binaryPackedPixelPixelOp").append("(")
                        .append("ImageOps.OP.")
                        .append(binaryExpr.getOp().kind())
                        .append(",")
                        .append(binaryExpr.getLeftExpr().visit(this, arg)).append(",")
                        .append(binaryExpr.getRightExpr().visit(this, arg)).append(")").append(")");

            } else if (leftType == Type.PIXEL && rightType == Type.INT){
                //im1$2=ImageOps.copyAndResize((ImageOps.binaryImageScalarOp(ImageOps.OP.TIMES,im0$2,factor$1)),w$1,h$1);
                binary.append(binaryExpr.getLeftExpr().visit(this,arg));
                binary.append(binaryExpr.getOp().text());
                binary.append("PixelOps.pack(")
                        .append(binaryExpr.getRightExpr().visit(this,arg)).append(", ")
                        .append(binaryExpr.getRightExpr().visit(this,arg)).append(", ")
                        .append(binaryExpr.getRightExpr().visit(this,arg)).append(")");

            }
            else if(leftType == Type.IMAGE && rightType == Type.INT){
                if(binaryExpr.getOp().kind() == Kind.TIMES){
                    binary.append("(ImageOps.binaryImageScalarOp(ImageOps.OP.TIMES, ").append(binaryExpr.getLeftExpr().visit(this,arg))
                            .append(" ,").append(binaryExpr.getRightExpr().visit(this,arg)).append("))");
                    //im1$2=ImageOps.cloneImage((ImageOps.binaryImageScalarOp(ImageOps.OP.DIV,im0$2,factor$1)));
                }else if(binaryExpr.getOp().kind() == Kind.DIV){ //passes but could be a problem when we have scopes
                    binary.append("(ImageOps.binaryImageScalarOp(ImageOps.OP.DIV,").append(binaryExpr.getLeftExpr().visit(this,arg))
                            .append(",").append(binaryExpr.getRightExpr().visit(this,arg)).append("))");
                }
            }
            else {
                binary.append("(");
                binary.append(binaryExpr.getLeftExpr().visit(this, arg));
                binary.append(binaryExpr.getOp().text());
                binary.append(binaryExpr.getRightExpr().visit(this, arg));
                binary.append(")");
            }

            return binary.toString();
        }

//        String leftExprCode = (String) binaryExpr.getLeftExpr().visit(this, arg);
//        String rightExprCode = (String) binaryExpr.getRightExpr().visit(this, arg);
//        if (binaryExpr.getOpKind() == Kind.EXP) {
//            return "((int)Math.round(Math.pow(" + leftExprCode + "," + rightExprCode + ")))";
//        } else if (binaryExpr.getOpKind() == Kind.PLUS) {
//            if (isImageType(binaryExpr.getLeftExpr().getType()) && isImageType(binaryExpr.getRightExpr().getType())) {
//                return "ImageOps.binaryImageImageOp(" + leftExprCode + ", " + rightExprCode + ")";
//            } else if (isPixelType(binaryExpr.getLeftExpr().getType()) && isPixelType(binaryExpr.getRightExpr().getType())) {
//                return "ImageOps.binaryPackedPixelPixelOp(ImageOps.OP.PLUS, " + leftExprCode + ", " + rightExprCode + ")";
//            } else if (isImageType(binaryExpr.getLeftExpr().getType()) && isPixelType(binaryExpr.getRightExpr().getType())) {
//                return "ImageOps.binaryImagePixelOp(ImageOps.OP.PLUS, " + leftExprCode + ", " + rightExprCode + ")";
//            } else if (isPixelType(binaryExpr.getLeftExpr().getType()) && isImageType(binaryExpr.getRightExpr().getType())) {
//                return "ImageOps.binaryImagePixelOp(ImageOps.OP.PLUS, " + rightExprCode + ", " + leftExprCode + ")";
//            }
//        }
//        else if (binaryExpr.getOpKind() == Kind.ASSIGN) {
//            if (isPixelType(binaryExpr.getLeftExpr().getType()) && binaryExpr.getRightExpr().getType() == Type.INT) {
//                return "ImageOps.binaryPackedPixelIntOp(" + binaryExpr.getOpKind() + ", " + leftExprCode + ", " + rightExprCode + ")";
//            }
//        }
//
//
//
//        return leftExprCode + " " + convert(binaryExpr.getOp().kind()) + " " + rightExprCode;


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
        if (channelSelector.color() == Kind.RES_red) {
            return 0;
        }
        if (channelSelector.color() == Kind.RES_green) {
            return 1;
        }
        if (channelSelector.color() == Kind.RES_blue) {
            return 2;
        }
        throw new CodeGenException("Channel selector not R/G/B");
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
        System.out.println(declaration.getNameDef().getName() + ", " + declaration.getNameDef().getType());
        Type varType = declaration.getNameDef().getType();
        String varInitialization = "";
        if (varType == Type.IMAGE) {
            if (declaration.getInitializer() == null){
                return "final BufferedImage " + declaration.getNameDef().getJavaName() + " = " + "ImageOps.makeImage(" + declaration.getNameDef().getDimension().visit(this, arg) + ");";
            }
            if (declaration.getInitializer() != null && declaration.getInitializer().getType() == Type.STRING) {
                if (declaration.getNameDef().getDimension() != null) {
                    return "BufferedImage " + declaration.getNameDef().getJavaName() + "=" + "FileURLIO.readImage(" + declaration.getInitializer().visit(this, arg) + ", " + declaration.getNameDef().getDimension().getWidth().visit(this, arg) + ", " + declaration.getNameDef().getDimension().getHeight().visit(this, arg) + ");";
                }
                return "BufferedImage " + declaration.getNameDef().getJavaName() + "=" +"FileURLIO.readImage(" + declaration.getInitializer().visit(this, arg) + ");";
            }
            if (declaration.getInitializer() != null && declaration.getInitializer().getType() == Type.IMAGE) {
                if (declaration.getNameDef().getDimension() != null) {
                    return "BufferedImage " + declaration.getNameDef().getJavaName() + "=" + "ImageOps.copyAndResize(" + declaration.getInitializer().visit(this, arg) + ", " + declaration.getNameDef().getDimension().getWidth().visit(this, arg) + ", " + declaration.getNameDef().getDimension().getHeight().visit(this, arg) + ");";
                }
                return "BufferedImage " + declaration.getNameDef().getJavaName() + "=" + "ImageOps.cloneImage(" + declaration.getInitializer().visit(this, arg) + ");";
            }

        }

        if (declaration.getInitializer() != null){
            varInitialization = " = " + declaration.getInitializer().visit(this, arg);
        }
        return convertType(varType) + " " + varName + varInitialization + ";\n";
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
       return dimension.getWidth().visit(this, arg) + ", " + dimension.getHeight().visit(this, arg);
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
        List<GuardedBlock> guardedBlocks = ifStatement.getGuardedBlocks();
        boolean firstGuard = true;

        for (GuardedBlock guardedBlock : guardedBlocks) {
            String guardExprCode = (String) guardedBlock.getGuard().visit(this, arg);
            String blockCode = (String) guardedBlock.getBlock().visit(this, arg);

            if (firstGuard) {
                ifStatementCode.append("if (").append(guardExprCode).append(") ").append(blockCode);
                firstGuard = false;
            } else {
                ifStatementCode.append("else if (").append(guardExprCode).append(") ").append(blockCode);
            }
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
        Expr expr = postfixExpr.primary();
        PixelSelector pixelSelector = postfixExpr.pixel();
        ChannelSelector channelSelector = postfixExpr.channel();
        StringBuilder sb = new StringBuilder();

        if (expr.getType() == Type.PIXEL) {
            int color = (int) channelSelector.visit(this, arg);
            switch (color) {
                case 0 -> sb.append("PixelOps.red(");
                case 1 -> sb.append("PixelOps.green(");
                case 2 -> sb.append("PixelOps.blue(");
            }
            String exprStr = (String) expr.visit(this, arg);
            sb.append(exprStr);
            sb.append(")");
            return sb.toString();
        }
        if (expr.getType() != Type.IMAGE) {
            throw new CodeGenException("Postfix type should be IMG");
        }

        if (pixelSelector != null && channelSelector == null) {
            sb.append("ImageOps.getRGB(");
            String exprStr = (String) expr.visit(this, arg);
            sb.append(exprStr);
            sb.append(", ");
            String xExpr = (String) pixelSelector.xExpr().visit(this,arg);
            String yExpr = (String) pixelSelector.yExpr().visit(this,arg);
            //System.out.println(pixStr);
            sb.append(xExpr);
            sb.append(", ");
            sb.append(yExpr);
            sb.append(")");
        }
        else if (pixelSelector != null && channelSelector != null) {
            int color = (int) channelSelector.visit(this, arg);

            switch (color) {
                case 0 -> sb.append("PixelOps.red(ImageOps.getRGB(" + expr.visit(this,arg) + "," + postfixExpr.pixel().xExpr().visit(this,arg)+","+ postfixExpr.pixel().yExpr().visit(this,arg));
                case 1 -> sb.append("PixelOps.green(ImageOps.getRGB(" + expr.visit(this,arg) + "," + postfixExpr.pixel().xExpr().visit(this,arg)+","+ postfixExpr.pixel().yExpr().visit(this,arg));
                case 2 -> sb.append("PixelOps.blue(ImageOps.getRGB(" + expr.visit(this,arg) + "," + postfixExpr.pixel().xExpr().visit(this,arg)+","+ postfixExpr.pixel().yExpr().visit(this,arg));
            }
            String exprStr = (String) expr.visit(this, arg);
            //sb.append(exprStr);
            sb.append("))");
        } else if (pixelSelector == null && channelSelector != null){
            int color = (int) channelSelector.visit(this, arg);

            switch (color) {
                case 0 -> sb.append("ImageOps.extractRed(");
                case 1 -> sb.append("ImageOps.extractGrn(");
                case 2 -> sb.append("ImageOps.extractBlu(");
            }
            String exprStr = (String) expr.visit(this, arg);
            sb.append(exprStr);
            sb.append(")");
        }
        return sb.toString();
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
            case "MAGENTA":
                return Color.MAGENTA;
            case "BLACK":
                return Color.BLACK;
            case "CYAN":
                return Color.CYAN;
            case "DARK_GRAY":
                return Color.DARK_GRAY;
            case "GRAY":
                return Color.GRAY;
            case "LIGHT_GRAY":
                return Color.LIGHT_GRAY;
            case "ORANGE":
                return Color.ORANGE;
            case "WHITE":
                return Color.WHITE;
            case "YELLOW":
                return Color.YELLOW;
            default:
                throw new PLCCompilerException("Unknown PLC Lang constant: " + plcLangConstant);
        }
    }


}
