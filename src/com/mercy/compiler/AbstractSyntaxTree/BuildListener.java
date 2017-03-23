package com.mercy.compiler.AbstractSyntaxTree;

import com.mercy.compiler.Entity.ClassEntity;
import com.mercy.compiler.Entity.FunctionEntity;
import com.mercy.compiler.Parser.MalicBaseListener;
import com.mercy.compiler.Parser.MalicParser;
import com.mercy.compiler.Type.*;
import com.mercy.compiler.Utility.InternalError;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by mercy on 17-3-17.
 */
public class BuildListener extends MalicBaseListener {
    private ParseTreeProperty<Object> map = new ParseTreeProperty<>();
    private AST ast;
    private boolean inClass;

    public AST getAST() {
        return ast;
    }

    public BuildListener() {
        super();
        inClass = false;
    }

    @Override
    public void exitCompilationUnit(MalicParser.CompilationUnitContext ctx) {
        List<FunctionEntity> functionEntities = new LinkedList<>();
        List<ClassEntity> classEntities = new LinkedList<>();

        for (MalicParser.FunctionDefinitionContext item : ctx.functionDefinition()) {
            functionEntities.add(((FunctionDefNode)map.get(item)).entity());
        }

        for (MalicParser.ClassDefinitionContext item : ctx.classDefinition()) {
            classEntities.add(((ClassDefNode)map.get(item)).entity());
        }

        ast = new AST(functionEntities, classEntities);
    }

    @Override
    public void exitClassDefinition(MalicParser.ClassDefinitionContext ctx) {
        List<VariableDefNode> vars = new LinkedList<>();
        List<FunctionEntity> funcs = new LinkedList<>();

        for (MalicParser.VariableDefinitionContext item : ctx.variableDefinition()) {
            vars.add((VariableDefNode)map.get(item));
        }

        for (MalicParser.FunctionDefinitionContext item : ctx.functionDefinition()) {
            funcs.add(((FunctionDefNode)map.get(item)).entity());
        }

        String name = ctx.name.getText();
        Type type = new ClassType(name);
        ClassEntity entity = new ClassEntity(type, name, vars, funcs);

        map.put(ctx, new ClassDefNode(new Location(ctx), name, entity));
    }

    @Override
    public void exitFunctionDefinition(MalicParser.FunctionDefinitionContext ctx) {
        List<ParameterDefNode> params = new LinkedList<>();

        for(MalicParser.ParameterContext item : ctx.parameter()) {
            params.add((ParameterDefNode)map.get(item));
        }

        FunctionEntity entity = new FunctionEntity((Type)map.get(ctx.ret), ctx.name.getText(),
                params, (BlockNode)map.get(ctx.block()));

        map.put(ctx, new FunctionDefNode(new Location(ctx), ctx.name.getText(), entity));
    }

    @Override
    public void exitVariableDefinition(MalicParser.VariableDefinitionContext ctx) {
        map.put(ctx, new VariableDefNode(new Location(ctx), (Type)map.get(ctx.typeType()),
                ctx.Identifier().getText(), getExpr(ctx.expression())));
    }

    @Override
    public void exitParameter(MalicParser.ParameterContext ctx) {
        map.put(ctx, new ParameterDefNode(new Location(ctx), (Type)map.get(ctx.typeType()),
                ctx.Identifier().getText()));
    }

    @Override
    public void exitPrimitiveType(MalicParser.PrimitiveTypeContext ctx) {
        Type type;
        switch (ctx.type.getText()) {
            case "bool"   : type = new BoolType();    break;
            case "int"    : type = new IntegerType(); break;
            case "void"   : type = new VoidType();    break;
            case "string" : type = new StringType();  break;
            default:
                throw new InternalError("Invalid type " + ctx.type.getText());
        }
        map.put(ctx, type);
    }

    @Override
    public void exitTypeType(MalicParser.TypeTypeContext ctx) {
        Type baseType;
        if (ctx.Identifier() != null) {
            baseType = new ClassType(ctx.Identifier().getText());
        } else {
            baseType = (Type)map.get(ctx.primitiveType());
        }

        int dimension = (ctx.getChildCount() - 1) / 2;
        if (dimension == 0) {
            map.put(ctx, baseType);
        } else {
            map.put(ctx, new ArrayType(baseType, dimension));
        }
    }

    @Override
    public void exitBlock(MalicParser.BlockContext ctx) {
        List<StmtNode> stmts = new LinkedList<>();
        for (MalicParser.StatementContext item : ctx.statement()) {
            StmtNode stmt = getStmt(item);
            if (item != null)
                stmts.add(stmt);
        }
        map.put(ctx, new BlockNode(new Location(ctx), stmts));
    }

    @Override
    public void exitBlockStmt(MalicParser.BlockStmtContext ctx) {
        map.put(ctx, map.get(ctx.block()));
    }
    
    @Override
    public void exitVarDefStmt(MalicParser.VarDefStmtContext ctx) {
        map.put(ctx, map.get(ctx.variableDefinition()));
    }
    
    @Override
    public void exitIfStmt(MalicParser.IfStmtContext ctx) {
        map.put(ctx, new IfNode(new Location(ctx), getExpr(ctx.expression()), 
                getStmt(ctx.statement(0)), getStmt(ctx.statement(1))));
    }

    
    @Override
    public void exitForStmt(MalicParser.ForStmtContext ctx) {
        map.put(ctx, new ForNode(new Location(ctx), getExpr(ctx.init),
                getExpr(ctx.cond), getExpr(ctx.incr), getStmt(ctx.statement())));
    }
    
    @Override
    public void exitWhileStmt(MalicParser.WhileStmtContext ctx) {
        map.put(ctx, new WhileNode(new Location(ctx), getExpr(ctx.expression()),
                getStmt(ctx.statement())));
    }

    @Override
    public void exitReturnStmt(MalicParser.ReturnStmtContext ctx) {
        map.put(ctx, new ReturnNode(new Location(ctx), getExpr(ctx.expression())));
    }

    @Override
    public void exitBreakStmt(MalicParser.BreakStmtContext ctx) {
        map.put(ctx, new BreakNode(new Location(ctx)));
    }
    
    @Override
    public void exitContinueStmt(MalicParser.ContinueStmtContext ctx) {
        map.put(ctx, new ContinueNode(new Location(ctx)));
    }

    @Override
    public void exitExprStmt(MalicParser.ExprStmtContext ctx) {
        map.put(ctx, new ExprStmtNode(new Location(ctx), getExpr(ctx.expression())));
    }

    @Override
    public void exitBlankStmt(MalicParser.BlankStmtContext ctx) {
        map.put(ctx, null);
    }

    @Override
    public void exitPrimaryExpr(MalicParser.PrimaryExprContext ctx) {
        map.put(ctx, map.get(ctx.primary()));
    }

    @Override
    public void exitMemberExpr(MalicParser.MemberExprContext ctx) {
        map.put(ctx, new MemberNode(getExpr(ctx.expression()),
                ctx.expression().getText()));
    }

    @Override
    public void exitArefExpr(MalicParser.ArefExprContext ctx) {
        map.put(ctx, new ArefNode(getExpr(ctx.expression(0)),
                getExpr(ctx.expression(1))));
    }
    @Override
    public void exitExpressionList(MalicParser.ExpressionListContext ctx) {
        List<ExprNode> exprs = new LinkedList<>();

        for (MalicParser.ExpressionContext x : ctx.expression()) {
            exprs.add(getExpr(x));
        }

        map.put(ctx, exprs);
    }

    @Override
    public void exitFuncallExpr(MalicParser.FuncallExprContext ctx) {
        List<ExprNode> args;

        if (ctx.expressionList() == null)
            args = new LinkedList<>();
        else
            args = (List<ExprNode>)map.get(ctx.expressionList());

        map.put(ctx, new FuncallNode(getExpr(ctx.expression()), args));
    }

    @Override
    public void exitNewExpr(MalicParser.NewExprContext ctx) {
        map.put(ctx, map.get(ctx.creator()));
    }

    @Override
    public void exitSuffixExpr(MalicParser.SuffixExprContext ctx) {
        UnaryOpNode.UnaryOp op;
        switch (ctx.op.getText()) {
            case "++" : op = UnaryOpNode.UnaryOp.SUF_DEC; break;
            case "--" : op = UnaryOpNode.UnaryOp.SUF_INC; break;
            default:
                throw new InternalError("Invalid token " + ctx.op.getText());
        }

        map.put(ctx, new SuffixOpNode(op,
                getExpr(ctx.expression())));
    }

    @Override
    public void exitPrefixExpr(MalicParser.PrefixExprContext ctx) {
        UnaryOpNode.UnaryOp op;
        switch (ctx.op.getText()) {
            case "+"  : op = UnaryOpNode.UnaryOp.ADD;   break;
            case "-"  : op = UnaryOpNode.UnaryOp.MINUS; break;
            case "++" : op = UnaryOpNode.UnaryOp.PRE_DEC; break;
            case "--" : op = UnaryOpNode.UnaryOp.PRE_INC; break;
            case "~"  : op = UnaryOpNode.UnaryOp.BIT_NOT; break;
            case "!"  : op = UnaryOpNode.UnaryOp.LOGIC_NOT; break;
            default:
                throw new InternalError("Invalid token " + ctx.op.getText());
        }

        map.put(ctx, new PrefixOpNode(op,
                getExpr(ctx.expression())));
    }

    @Override
    public void exitBinaryExpr(MalicParser.BinaryExprContext ctx) {
        BinaryOpNode.BinaryOp op;
        switch (ctx.op.getText()) {
            case "*"  : op = BinaryOpNode.BinaryOp.MUL; break;
            case "/"  : op = BinaryOpNode.BinaryOp.DIV; break;
            case "%"  : op = BinaryOpNode.BinaryOp.MOD; break;
            case "+"  : op = BinaryOpNode.BinaryOp.ADD; break;
            case "-"  : op = BinaryOpNode.BinaryOp.MINUS;  break;
            case "<<" : op = BinaryOpNode.BinaryOp.LSHIFT; break;
            case ">>" : op = BinaryOpNode.BinaryOp.RSHIFT; break;
            case ">"  : op = BinaryOpNode.BinaryOp.LT; break;
            case "<"  : op = BinaryOpNode.BinaryOp.GT; break;
            case ">=" : op = BinaryOpNode.BinaryOp.GE; break;
            case "<=" : op = BinaryOpNode.BinaryOp.LE; break;
            case "==" : op = BinaryOpNode.BinaryOp.EQ; break;
            case "!=" : op = BinaryOpNode.BinaryOp.NE; break;
            case "&"  : op = BinaryOpNode.BinaryOp.BIT_AND; break;
            case "^"  : op = BinaryOpNode.BinaryOp.BIT_XOR; break;
            case "|"  : op = BinaryOpNode.BinaryOp.BIT_OR; break;
            default   :
                throw new InternalError("Invalid token " + ctx.op.getText());
        }

        map.put(ctx, new BinaryOpNode(
                getExpr(ctx.expression(0)),
                op,
                getExpr(ctx.expression(1)))
        );
    }

    @Override
    public void exitLogicalAndExpr(MalicParser.LogicalAndExprContext ctx) {
        map.put(ctx, new LogicalAndNode(
                getExpr(ctx.expression(0)),
                getExpr(ctx.expression(1)))
        );
    }

    @Override
    public void exitLogicalOrExpr(MalicParser.LogicalOrExprContext ctx) {
        map.put(ctx, new LogicalOrNode(
                getExpr(ctx.expression(0)),
                getExpr(ctx.expression(1)))
        );
    }

    @Override
    public void exitAssignExpr(MalicParser.AssignExprContext ctx) {
        map.put(ctx, new AssignNode(
                getExpr(ctx.expression(0)),
                getExpr(ctx.expression(1))));
    }

    @Override
    public void exitSubExpr(MalicParser.SubExprContext ctx) {
        map.put(ctx, map.get(ctx.expression()));
    }

    @Override
    public void exitThisExpr(MalicParser.ThisExprContext ctx) {
        map.put(ctx, new VariableNode(new Location(ctx), "this"));
    }
    
    @Override
    public void exitVariableExpr(MalicParser.VariableExprContext ctx) {
        map.put(ctx, new VariableNode(new Location(ctx.Identifier()),
                ctx.Identifier().getText()));
    }

    @Override
    public void exitLiteralExpr(MalicParser.LiteralExprContext ctx) {
        map.put(ctx, map.get(ctx.literal()));
    }

    @Override
    public void exitDecIntegerConst(MalicParser.DecIntegerConstContext ctx) {
        map.put(ctx, new IntegerLiteralNode(new Location(ctx), Long.parseLong(ctx.DecimalInteger().getText())));
    }

    @Override
    public void exitStringConst(MalicParser.StringConstContext ctx) {
        map.put(ctx, new StringLiteralNode(new Location(ctx), ctx.StringLiteral().getText()));
    }

    @Override
    public void exitBoolConst(MalicParser.BoolConstContext ctx) {
        map.put(ctx, new BoolLiteralNode(new Location(ctx),
                ctx.value.getText().equals("true")));
    }

    @Override
    public void exitNullConst(MalicParser.NullConstContext ctx) {
        map.put(ctx, new VariableNode(new Location(ctx), "null"));
    }

    @Override
    public void exitArrayCreator(MalicParser.ArrayCreatorContext ctx) {
        Type baseType;
        if (ctx.Identifier() != null) {
            baseType = new ClassType(ctx.Identifier().getText());
        } else {
            baseType = (Type)map.get(ctx.primitiveType());
        }

        List<MalicParser.ExpressionContext> exprs = ctx.expression();
        int dimension = (ctx.getChildCount() - 1 - exprs.size()) / 2;
        Type type = new ArrayType(baseType, dimension);

        List<ExprNode> exprNodes = new LinkedList<>();
        for (MalicParser.ExpressionContext item : exprs) {
            exprNodes.add(getExpr(item));
        }

        map.put(ctx, new CreatorNode(new Location(ctx),
                type, exprNodes, dimension));
    }

    @Override
    public void exitNonarrayCreator(MalicParser.NonarrayCreatorContext ctx) {
        Type type;
        type = new ClassType(ctx.Identifier().getText());
        map.put(ctx, new CreatorNode(new Location(ctx), type, null, 0));
    }


    private StmtNode getStmt(MalicParser.StatementContext ctx) {
        if (ctx == null)
            return null;
        else
            return (StmtNode)map.get(ctx);
    }

    private ExprNode getExpr(MalicParser.ExpressionContext ctx) {
        if (ctx == null)
            return null;
        else
            return (ExprNode)map.get(ctx);
    }
}
