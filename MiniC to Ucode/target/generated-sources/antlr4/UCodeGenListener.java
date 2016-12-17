import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Hashtable;

public class UCodeGenListener extends MiniCBaseListener {

	private ParseTreeProperty<String> newTexts;
	private ArrayList<Hashtable<String, Integer>> variables; // hashtable<variable name, sequence>
	private Hashtable<String, Integer> functions; // hashtable<function name, function number>

	private int globalVariables = 0; // the number of global variables
	private int localVariables = 0; // the number of local variables of each function
	private int functionNumber = 0; // the number of functions

	private int var_decls = 0; // the number of 'var_decl'
	private int whiles = 0; // the number of 'while' keywords
	private int ifs = 0; // the number of 'if' keywords
	private boolean hasReturnStatement = false; // if a function has the return type void

	public UCodeGenListener() {
		super();
		newTexts = new ParseTreeProperty<String>();
		variables= new ArrayList<Hashtable<String, Integer>>();
		functions = new Hashtable<String, Integer>();
		
		variables.add(new Hashtable<String, Integer>()); // global variables
	}

	@Override
	public void exitProgram(MiniCParser.ProgramContext ctx) {
		StringBuilder line = new StringBuilder();

		// fun_decl
		for (int index = var_decls, declSize = ctx.getChildCount(); index < declSize; index++)
			line.append(newTexts.get(ctx.getChild(index)));

		// var_decl
		line.append(Keyword.BGN).append(globalVariables).append("\n");
		for (int index = 0; index < var_decls; index++)
			line.append(newTexts.get(ctx.getChild(index)));

		// exit, call main
		line.append(Keyword.LDP).append("\n");
		line.append(Keyword.CALL).append(Keyword.MAIN).append("\n");
		line.append(Keyword.END).append("\n");

		// Compilation Complete
		System.out.print(line.toString());
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("ucode.uco"));
			writer.write(line.toString());
			writer.close();
		} catch (IOException error) {
			System.out.println(error.toString());
		}
	}

	@Override
	public void exitDecl(MiniCParser.DeclContext ctx) {
		newTexts.put(ctx, newTexts.get(ctx.getChild(0)));
	}

	@Override
	public void enterVar_decl(MiniCParser.Var_declContext ctx) {
		var_decls++;
	}

	@Override
	public void exitVar_decl(MiniCParser.Var_declContext ctx) {
		StringBuilder line = new StringBuilder();

		// type_spec IDENT ';'
		if (ctx.getChildCount() == 3) {
			++globalVariables;
			line.append(Keyword.SYM).append("1 ").append(globalVariables).append(" 1\n");
			variables.get(0).put(ctx.IDENT().getText(), globalVariables);
		}

		// type_spec IDENT '=' LITERAL ';'
		if (ctx.getChildCount() == 5) {
			++globalVariables;
			line.append(Keyword.SYM).append("1 ").append(globalVariables).append(" 1\n");
			line.append(Keyword.LDC).append(numeration(ctx.LITERAL().getText())).append("\n");
			line.append(Keyword.STR).append("1 ").append(globalVariables).append("\n");
			variables.get(0).put(ctx.IDENT().getText(), globalVariables);
		}

		// type_spec IDENT '[' LITERAL ']' ';'
		if (ctx.getChildCount() == 6) {
			int arraySize = Integer.parseInt(numeration(ctx.LITERAL().getText()));
			String arrayName = ctx.IDENT().getText();
			
			++globalVariables;
			line.append(Keyword.SYM).append("1 ").append(globalVariables + " ").append(arraySize + 1).append("\n");
			line.append(Keyword.LDA).append("1 ").append(globalVariables).append("\n");
			line.append(Keyword.STR).append("1 ").append(globalVariables).append("\n");
			
			variables.get(0).put(arrayName, globalVariables);
			for (int index = 0; index < arraySize; index++)
				variables.get(0).put(arrayName + "[" + index + "]", globalVariables++);
		}

		newTexts.put(ctx, line.toString());
	}

	@Override
	public void enterFun_decl(MiniCParser.Fun_declContext ctx) {
		localVariables = 0;
		functions.put(ctx.IDENT().getText(), ++functionNumber);
		variables.add(new Hashtable<String, Integer>()); // local variables
	}

	@Override
	public void exitFun_decl(MiniCParser.Fun_declContext ctx) {
		StringBuilder line = new StringBuilder();
		String functionName = ctx.IDENT().getText();

		line.append(functionName).append(Keyword.SPACE[indentation(functionName)])
			.append(Keyword.PROC).append(localVariables).append(" 2").append(" 2\n");
		line.append(newTexts.get(ctx.params()));
		line.append(newTexts.get(ctx.compound_stmt()));
		line.append(hasReturnStatement ? "" : Keyword.RET + "\n");
		line.append(Keyword.END).append("\n");
		
		hasReturnStatement = false;
		newTexts.put(ctx, line.toString());
	}

	@Override
	public void exitParams(MiniCParser.ParamsContext ctx) {
		StringBuilder line = new StringBuilder();
		for (int index = ctx.param().size() - 1; index >= 0; index--)
			line.append(newTexts.get(ctx.param(index)));
		newTexts.put(ctx, line.toString());
	}

	@Override
	public void exitParam(MiniCParser.ParamContext ctx) {
		StringBuilder line = new StringBuilder();

		// type_spec IDENT
		if (ctx.getChildCount() == 2) {
			++localVariables;
			variables.get(functionNumber).put(ctx.IDENT().getText(), localVariables);
		}

		// type_spec IDENT '[' ']'
		if (ctx.getChildCount() == 4) {
			++localVariables;
			variables.get(functionNumber).put(ctx.IDENT().getText(), localVariables);
		}

		newTexts.put(ctx, line.toString());
	}

	@Override
	public void exitCompound_stmt(MiniCParser.Compound_stmtContext ctx) {
		StringBuilder line = new StringBuilder();

		// local_decl*
		for (int index = 0, localSize = ctx.local_decl().size(); index < localSize; index++)
			line.append(newTexts.get(ctx.local_decl(index)));

		// stmt*
		for (int index = 0, stmtSize = ctx.stmt().size(); index < stmtSize; index++)
			line.append(newTexts.get(ctx.stmt(index)));

		newTexts.put(ctx, line.toString());
	}

	@Override
	public void exitLocal_decl(MiniCParser.Local_declContext ctx) {
		StringBuilder line = new StringBuilder();

		// type_spec IDENT ';'
		if (ctx.getChildCount() == 3) {
			++localVariables;
			line.append(Keyword.SYM).append("2 ").append(localVariables).append(" 1\n");
			variables.get(functionNumber).put(ctx.IDENT().getText(), localVariables);
		}

		// type_spec IDENT '=' LITERAL ';'
		if (ctx.getChildCount() == 5) {
			++localVariables;
			line.append(Keyword.SYM).append("2 ").append(localVariables).append(" 1\n");
			line.append(Keyword.LDC).append(numeration(ctx.LITERAL().getText())).append("\n");
			line.append(Keyword.STR).append("2 ").append(localVariables).append("\n");
			variables.get(functionNumber).put(ctx.IDENT().getText(), localVariables);
		}

		// type_spec IDENT '[' LITERAL ']' ';'
		if (ctx.getChildCount() == 6) {
			int arraySize = Integer.parseInt(numeration(ctx.LITERAL().getText()));
			String arrayName = ctx.IDENT().getText();
			
			++localVariables;
			line.append(Keyword.SYM).append("2 ").append(localVariables + " ").append(arraySize + 1).append("\n");
			line.append(Keyword.LDA).append("2 ").append(localVariables).append("\n");
			line.append(Keyword.STR).append("2 ").append(localVariables).append("\n");
			
			variables.get(functionNumber).put(arrayName, localVariables);
			for (int index = 0; index < arraySize; index++)
				variables.get(functionNumber).put(arrayName + "[" + index + "]", localVariables++);
		}

		newTexts.put(ctx, line.toString());
	}

	@Override
	public void exitStmt(MiniCParser.StmtContext ctx) {
		newTexts.put(ctx, newTexts.get(ctx.getChild(0)));
	}

	@Override
	public void exitExpr_stmt(MiniCParser.Expr_stmtContext ctx) {
		newTexts.put(ctx, newTexts.get(ctx.getChild(0)));
	}

	@Override
	public void exitExpr(MiniCParser.ExprContext ctx) {
		StringBuilder line = new StringBuilder();
		
		if (isAssignmentOperation(ctx)) { // IDENT '=' expr
			String name = ctx.IDENT().getText();
			int location = findVariableLocation(name);
			int sequence = variables.get((location == 1) ? 0 : functionNumber).get(name);

			line.append(newTexts.get(ctx.expr(0)));
			line.append(Keyword.STR).append(location + " ").append(sequence).append("\n");
		} 
		else if (isArrayAssignmentOperation(ctx)) { // IDENT '[' expr ']' '=' expr
			String name = ctx.IDENT().getText();
			int location = findVariableLocation(name);
			int sequence = variables.get((location == 1) ? 0 : functionNumber).get(name);

			// calculate array index in stack
			line.append(newTexts.get(ctx.expr(0)));
			line.append(Keyword.LOD).append(location + " ").append(sequence).append("\n");
			line.append(Keyword.INC).append("\n");
			line.append(Keyword.ADD).append("\n");
			line.append(newTexts.get(ctx.expr(1))); // assignment
			line.append(Keyword.STI).append("\n");
		}
		else if (isArrayOperation(ctx)) { // IDENT '[' expr ']'
			String name = ctx.IDENT().getText();
			int location = findVariableLocation(name);
			int sequence = variables.get((location == 1) ? 0 : functionNumber).get(name);

			// calculate array index in stack
			line.append(newTexts.get(ctx.expr(0)));
			line.append(Keyword.LOD).append(location + " ").append(sequence).append("\n");
			line.append(Keyword.INC).append("\n");
			line.append(Keyword.ADD).append("\n");
			line.append(Keyword.LDI).append("\n"); // assignment
		} 
		else if (isFunctionOperation(ctx)) { // IDENT '(' args ')'
			line.append(Keyword.LDP).append("\n");
			line.append(newTexts.get(ctx.args()));
			line.append(Keyword.CALL).append(ctx.IDENT()).append("\n");
		} 
		else if (isBracketOperation(ctx)) { // '(' expr ')'
			line.append(newTexts.get(ctx.expr(0)));
		} 
		else if (isIncreaseOperation(ctx)) { // '++' expr
			String name = ctx.expr(0).getText();
			int location = findVariableLocation(name);
			int sequence = variables.get((location == 1) ? 0 : functionNumber).get(name);

			line.append(Keyword.LOD).append(location + " ").append(sequence).append("\n");
			line.append(Keyword.INC).append("\n");
			line.append(Keyword.STR).append(location + " ").append(sequence).append("\n");
		} 
		else if (isDecreaseOperation(ctx)) { // '--' expr
			String name = ctx.expr(0).getText();
			int location = findVariableLocation(name);
			int sequence = variables.get((location == 1) ? 0 : functionNumber).get(name);

			line.append(Keyword.LOD).append(location + " ").append(sequence).append("\n");
			line.append(Keyword.DEC).append("\n");
			line.append(Keyword.STR).append(location + " ").append(sequence).append("\n");
		} 
		else if (isMultiplyOperation(ctx)) { // expr '*' expr
			line.append(newTexts.get(ctx.expr(0)));
			line.append(newTexts.get(ctx.expr(1)));
			line.append(Keyword.MULT).append("\n");
		} 
		else if (isDivideOperation(ctx)) { // expr '/' expr
			line.append(newTexts.get(ctx.expr(0)));
			line.append(newTexts.get(ctx.expr(1)));
			line.append(Keyword.DIV).append("\n");
		} 
		else if (isModularOperation(ctx)) { // expr '%' expr
			line.append(newTexts.get(ctx.expr(0)));
			line.append(newTexts.get(ctx.expr(1)));
			line.append(Keyword.MOD).append("\n");
		} 
		else if (isAddOperation(ctx)) { // expr '+' expr
			line.append(newTexts.get(ctx.expr(0)));
			line.append(newTexts.get(ctx.expr(1)));
			line.append(Keyword.ADD).append("\n");
		} 
		else if (isSubtractOperation(ctx)) { // expr '-' expr
			line.append(newTexts.get(ctx.expr(0)));
			line.append(newTexts.get(ctx.expr(1)));
			line.append(Keyword.SUB).append("\n");
		} 
		else if (isEqualOperation(ctx)) { // expr EQ expr
			line.append(newTexts.get(ctx.expr(0)));
			line.append(newTexts.get(ctx.expr(1)));
			line.append(Keyword.EQ).append("\n");
		} 
		else if (isNotEqualOperation(ctx)) { // expr NE expr
			line.append(newTexts.get(ctx.expr(0)));
			line.append(newTexts.get(ctx.expr(1)));
			line.append(Keyword.NE).append("\n");
		} 
		else if (isLessThanOperation(ctx)) { // expr '<' expr
			line.append(newTexts.get(ctx.expr(0)));
			line.append(newTexts.get(ctx.expr(1)));
			line.append(Keyword.LT).append("\n");
		} 
		else if (isLessEqualOperation(ctx)) { // expr LE expr
			line.append(newTexts.get(ctx.expr(0)));
			line.append(newTexts.get(ctx.expr(1)));
			line.append(Keyword.LE).append("\n");
		} 
		else if (isGreaterThanOperation(ctx)) { // expr '>' expr
			line.append(newTexts.get(ctx.expr(0)));
			line.append(newTexts.get(ctx.expr(1)));
			line.append(Keyword.GT).append("\n");
		} 
		else if (isGreaterEqualOperation(ctx)) { // expr GE expr
			line.append(newTexts.get(ctx.expr(0)));
			line.append(newTexts.get(ctx.expr(1)));
			line.append(Keyword.GE).append("\n");
		} 
		else if (isNotOperation(ctx)) { // '!' expr
			line.append(newTexts.get(ctx.expr(0)));
			line.append(Keyword.NOTOP).append("\n");
		} 
		else if (isAndOperation(ctx)) { // expr AND expr
			line.append(newTexts.get(ctx.expr(0)));
			line.append(newTexts.get(ctx.expr(1)));
			line.append(Keyword.AND).append("\n");
		} 
		else if (isOrOperation(ctx)) { // expr OR expr
			line.append(newTexts.get(ctx.expr(0)));
			line.append(newTexts.get(ctx.expr(1)));
			line.append(Keyword.OR).append("\n");
		} 
		else if (isNegativeOperation(ctx)) { // '-' expr
			line.append(newTexts.get(ctx.expr(0)));
			line.append(Keyword.NEG).append("\n");
		} 
		else if (isPositiveOperation(ctx)) { // '+' expr
			line.append(newTexts.get(ctx.expr(0)));
		} 
		else if (isIdentOperation(ctx)) { // IDENT
			String name = ctx.IDENT().getText();
			int location = findVariableLocation(name);
			int sequence = variables.get((location == 1) ? 0 : functionNumber).get(name);
			line.append(Keyword.LOD).append(location + " ").append(sequence).append("\n");
		} 
		else if (isLiteralOperation(ctx)) { // LITERAL
			line.append(Keyword.LDC).append(numeration(ctx.getText())).append("\n");
		}

		newTexts.put(ctx, line.toString());
	}

	@Override
	public void exitArgs(MiniCParser.ArgsContext ctx) {
		StringBuilder line = new StringBuilder();
		for (int index = 0, exprSize = ctx.expr().size(); index < exprSize; index++)
			line.append(newTexts.get(ctx.expr(index)));
		newTexts.put(ctx, line.toString());
	}

	@Override
	public void enterWhile_stmt(MiniCParser.While_stmtContext ctx) {
		whiles++;
	}

	@Override
	public void exitWhile_stmt(MiniCParser.While_stmtContext ctx) {
		StringBuilder line = new StringBuilder();
		String whileName = Keyword.WHILE + whiles;
		String whileOutName = Keyword.WHILEOUT + whiles;

		line.append(whileName).append(Keyword.SPACE[indentation(whileName)]).append(Keyword.NOP).append("\n");
		line.append(newTexts.get(ctx.expr()));
		line.append(Keyword.FJP).append(whileOutName).append("\n");
		line.append(newTexts.get(ctx.stmt()));
		line.append(Keyword.UJP).append(whileName).append("\n");
		line.append(whileOutName).append(Keyword.SPACE[indentation(whileOutName)]).append(Keyword.NOP).append("\n");

		newTexts.put(ctx, line.toString());
	}

	@Override
	public void enterIf_stmt(MiniCParser.If_stmtContext ctx) {
		ifs++;
	}

	@Override
	public void exitIf_stmt(MiniCParser.If_stmtContext ctx) {
		StringBuilder line = new StringBuilder();
		String ifName = Keyword.IF + ifs;

		// IF '(' expr ')' stmt
		if (ctx.getChildCount() == 5) {
			line.append(newTexts.get(ctx.expr()));
			line.append(Keyword.FJP).append(ifName).append("\n");
			line.append(newTexts.get(ctx.stmt(0)));
			line.append(ifName).append(Keyword.SPACE[indentation(ifName)]).append(Keyword.NOP).append("\n");
		}

		// IF '(' expr ')' stmt ELSE stmt
		if (ctx.getChildCount() == 7) {
			line.append(newTexts.get(ctx.expr()));
			line.append(Keyword.FJP).append(ifName).append("\n");
			line.append(newTexts.get(ctx.stmt(0)));
			line.append(ifName).append(Keyword.SPACE[indentation(ifName)]).append(Keyword.NOP).append("\n");
			line.append(newTexts.get(ctx.stmt(1)));
		}

		newTexts.put(ctx, line.toString());
	}

	@Override
	public void exitReturn_stmt(MiniCParser.Return_stmtContext ctx) {
		StringBuilder line = new StringBuilder();

		// RETURN ';'
		if (ctx.getChildCount() == 2)
			line.append(Keyword.RET).append("\n");

		// RETURN expr ';'
		if (ctx.getChildCount() == 3) {
			line.append(newTexts.get(ctx.expr()));
			line.append(Keyword.RETV).append("\n");
		}

		hasReturnStatement = true;
		newTexts.put(ctx, line.toString());
	}
	
	private int indentation(String value) {
		return 11 - value.length();
	}
	
	private String numeration(String value) {
		if (isHexNumber(value)) // Hex Number
			return String.valueOf(Integer.parseInt(value.substring(2, value.length()), 16));
		
		if (isOctalNumber(value)) // Octal Number
			return String.valueOf(Integer.parseInt(value, 8));
		
		return value; // Decimal Number
	}

	private int findVariableLocation(String name) {
		if (variables.get(functionNumber).containsKey(name)) // find local variable of current function
			return 2;
		if (variables.get(0).containsKey(name)) // find global variable
			return 1;
		
		// compile error
		System.out.println("Compile error: " + name + " is undefined");
		return 0;
	}

	private boolean isAssignmentOperation(ParserRuleContext parserRuleContext) {
		return (parserRuleContext.getChildCount() == 3) && (parserRuleContext.getChild(1).getText().equals("="));
	}

	private boolean isArrayAssignmentOperation(ParserRuleContext parserRuleContext) {
		return (parserRuleContext.getChildCount() == 6) && (parserRuleContext.getChild(4).getText().equals("="));
	}

	private boolean isMultiplyOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 3) && (ctx.getChild(1).getText().equals("*"));
	}

	private boolean isDivideOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 3) && (ctx.getChild(1).getText().equals("/"));
	}

	private boolean isModularOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 3) && (ctx.getChild(1).getText().equals("%"));
	}

	private boolean isAddOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 3) && (ctx.getChild(1).getText().equals("+"));
	}

	private boolean isSubtractOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 3) && (ctx.getChild(1).getText().equals("-"));
	}

	private boolean isEqualOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 3) && (ctx.getChild(1).getText().equals("=="));
	}

	private boolean isNotEqualOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 3) && (ctx.getChild(1).getText().equals("!="));
	}

	private boolean isLessEqualOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 3) && (ctx.getChild(1).getText().equals("<="));
	}

	private boolean isGreaterEqualOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 3) && (ctx.getChild(1).getText().equals(">="));
	}

	private boolean isLessThanOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 3) && (ctx.getChild(1).getText().equals("<"));
	}

	private boolean isGreaterThanOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 3) && (ctx.getChild(1).getText().equals(">"));
	}

	private boolean isNotOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 2) && (ctx.getChild(0).getText().equals("!"));
	}

	private boolean isAndOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 3) && (ctx.getChild(1).getText().equals("and"));
	}

	private boolean isOrOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 3) && (ctx.getChild(1).getText().equals("or"));
	}

	private boolean isNegativeOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 2) && (ctx.getChild(0).getText().equals("-"));
	}

	private boolean isPositiveOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 2) && (ctx.getChild(0).getText().equals("+"));
	}

	private boolean isIncreaseOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 2) && (ctx.getChild(0).getText().equals("++"));
	}

	private boolean isDecreaseOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 2) && (ctx.getChild(0).getText().equals("--"));
	}
	
	private boolean isIdentOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 1) && (ctx.IDENT() != null);
	}

	private boolean isLiteralOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 1) && (ctx.LITERAL() != null);
	}

	private boolean isArrayOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 4) 
				&& (ctx.getChild(1).getText().equals("["))
				&& (ctx.getChild(3).getText().equals("]"));
	}

	private boolean isFunctionOperation(ParseTree parseTree) {
		return (parseTree.getChildCount() == 4)
				&& (parseTree.getChild(1).getText().equals("("))
				&& (parseTree.getChild(3).getText().equals(")"));
	}

	private boolean isBracketOperation(MiniCParser.ExprContext ctx) {
		return (ctx.getChildCount() == 3) 
				&& (ctx.getChild(0).getText().equals("("))
				&& (ctx.getChild(2).getText().equals(")"));
	}
	
	private boolean isHexNumber(String value) {
		return (value.length() >= 3) 
				&& (value.charAt(0) == '0') 
				&& (value.charAt(1) == 'x' || value.charAt(1) == 'X');
	}
	
	private boolean isOctalNumber(String value) {
		return (value.length() >= 2) && (value.charAt(0) == '0');
	}
}