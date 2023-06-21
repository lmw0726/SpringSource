/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression.spel.standard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateAwareExpressionParser;
import org.springframework.expression.spel.InternalParseException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.ast.Assign;
import org.springframework.expression.spel.ast.BeanReference;
import org.springframework.expression.spel.ast.BooleanLiteral;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.ConstructorReference;
import org.springframework.expression.spel.ast.Elvis;
import org.springframework.expression.spel.ast.FunctionReference;
import org.springframework.expression.spel.ast.Identifier;
import org.springframework.expression.spel.ast.Indexer;
import org.springframework.expression.spel.ast.InlineList;
import org.springframework.expression.spel.ast.InlineMap;
import org.springframework.expression.spel.ast.Literal;
import org.springframework.expression.spel.ast.MethodReference;
import org.springframework.expression.spel.ast.NullLiteral;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.expression.spel.ast.OpDec;
import org.springframework.expression.spel.ast.OpDivide;
import org.springframework.expression.spel.ast.OpEQ;
import org.springframework.expression.spel.ast.OpGE;
import org.springframework.expression.spel.ast.OpGT;
import org.springframework.expression.spel.ast.OpInc;
import org.springframework.expression.spel.ast.OpLE;
import org.springframework.expression.spel.ast.OpLT;
import org.springframework.expression.spel.ast.OpMinus;
import org.springframework.expression.spel.ast.OpModulus;
import org.springframework.expression.spel.ast.OpMultiply;
import org.springframework.expression.spel.ast.OpNE;
import org.springframework.expression.spel.ast.OpOr;
import org.springframework.expression.spel.ast.OpPlus;
import org.springframework.expression.spel.ast.OperatorBetween;
import org.springframework.expression.spel.ast.OperatorInstanceof;
import org.springframework.expression.spel.ast.OperatorMatches;
import org.springframework.expression.spel.ast.OperatorNot;
import org.springframework.expression.spel.ast.OperatorPower;
import org.springframework.expression.spel.ast.Projection;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;
import org.springframework.expression.spel.ast.QualifiedIdentifier;
import org.springframework.expression.spel.ast.Selection;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.expression.spel.ast.StringLiteral;
import org.springframework.expression.spel.ast.Ternary;
import org.springframework.expression.spel.ast.TypeReference;
import org.springframework.expression.spel.ast.VariableReference;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Hand-written SpEL parser. Instances are reusable but are not thread-safe.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.0
 */
class InternalSpelExpressionParser extends TemplateAwareExpressionParser {

	private static final Pattern VALID_QUALIFIED_ID_PATTERN = Pattern.compile("[\\p{L}\\p{N}_$]+");


	private final SpelParserConfiguration configuration;

	// 对于构建节点的规则，它们被堆叠在这里以返回
	private final Deque<SpelNodeImpl> constructedNodes = new ArrayDeque<>();

	// 正在解析的表达式
	private String expressionString = "";

	// 由该表达式字符串构造的令牌流
	private List<Token> tokenStream = Collections.emptyList();

	// 填充的令牌流的长度
	private int tokenStreamLength;

	// 处理令牌时令牌流中的当前位置
	private int tokenStreamPointer;


	/**
	 * 创建具有一些配置的行为的解析器。
	 *
	 * @param configuration 自定义配置选项
	 */
	public InternalSpelExpressionParser(SpelParserConfiguration configuration) {
		this.configuration = configuration;
	}


	@Override
	protected SpelExpression doParseExpression(String expressionString, @Nullable ParserContext context)
			throws ParseException {

		try {
			this.expressionString = expressionString;
			Tokenizer tokenizer = new Tokenizer(expressionString);
			this.tokenStream = tokenizer.process();
			this.tokenStreamLength = this.tokenStream.size();
			this.tokenStreamPointer = 0;
			//清除构建的节点
			this.constructedNodes.clear();
			//解析表达式
			SpelNodeImpl ast = eatExpression();
			Assert.state(ast != null, "No node");
			//弹出当前位置的令牌
			Token t = peekToken();
			if (t != null) {
				//该令牌为空，抛出异常
				throw new SpelParseException(t.startPos, SpelMessage.MORE_INPUT, toString(nextToken()));
			}
			Assert.isTrue(this.constructedNodes.isEmpty(), "At least one node expected");
			//返回该Spring EL表达式的实例
			return new SpelExpression(expressionString, ast, this.configuration);
		} catch (InternalParseException ex) {
			throw ex.getCause();
		}
	}

	//	表达式
	//    : 逻辑或表达式
	//      ( (ASSIGN^ logicalOrExpression)
	//	    | (DEFAULT^ logicalOrExpression)
	//	    | (QMARK^ expression COLON! expression)
	//      | (ELVIS^ expression))?;
	@Nullable
	private SpelNodeImpl eatExpression() {
		//处理逻辑或表达式
		SpelNodeImpl expr = eatLogicalOrExpression();
		//获取当前位置的token
		Token t = peekToken();
		if (t == null) {
			return expr;
		}
		if (t.kind == TokenKind.ASSIGN) {
			// 如果是赋值操作，表达式为null，则返回Null字面量
			// a=b
			if (expr == null) {
				expr = new NullLiteral(t.startPos - 1, t.endPos - 1);
			}
			//消费一个token
			nextToken();
			//处理逻辑或表达式
			SpelNodeImpl assignedValue = eatLogicalOrExpression();
			//返回赋值操作
			return new Assign(t.startPos, t.endPos, expr, assignedValue);
		}
		if (t.kind == TokenKind.ELVIS) {
			// a?:b (a如果不是null，否则b)
			if (expr == null) {
				//表达式为null，返回一个Null字面量
				expr = new NullLiteral(t.startPos - 1, t.endPos - 2);
			}
			//消费一个token
			nextToken();
			//解析剩下的表达式
			SpelNodeImpl valueIfNull = eatExpression();
			//如果值为空，应该返回的表达式
			if (valueIfNull == null) {
				//表达式为null，返回一个Null字面量
				valueIfNull = new NullLiteral(t.startPos + 1, t.endPos + 1);
			}
			//返回一个a为null，否则b的表达式节点
			return new Elvis(t.startPos, t.endPos, expr, valueIfNull);
		}
		if (t.kind == TokenKind.QMARK) {
			//处理?表达式，表示if-else关系
			// a?b:c
			if (expr == null) {
				expr = new NullLiteral(t.startPos - 1, t.endPos - 1);
			}
			nextToken();
			//解析表达式
			SpelNodeImpl ifTrueExprValue = eatExpression();
			//消费一个冒号token
			eatToken(TokenKind.COLON);
			//解析if-else表达式
			SpelNodeImpl ifFalseExprValue = eatExpression();
			//返回一个三元运算表达式
			return new Ternary(t.startPos, t.endPos, expr, ifTrueExprValue, ifFalseExprValue);
		}
		return expr;
	}

	//逻辑或表达式: logicalAndExpression (OR^ logicalAndExpression)*;
	@Nullable
	private SpelNodeImpl eatLogicalOrExpression() {
		//处理逻辑和表达式
		SpelNodeImpl expr = eatLogicalAndExpression();
		while (peekIdentifierToken("or") || peekToken(TokenKind.SYMBOLIC_OR)) {
			//如果当前Token是'OR'字面量，或者当前位置的Token类型是OR符号
			//消耗 OR ，先获取当前位置的Token，再将位置+1
			Token t = takeToken();
			//处理逻辑和表达式
			SpelNodeImpl rhExpr = eatLogicalAndExpression();
			//检查操作节点是否符合规范
			checkOperands(t, expr, rhExpr);
			expr = new OpOr(t.startPos, t.endPos, expr, rhExpr);
		}
		return expr;
	}

	// 逻辑和表达式 : relationalExpression (AND^ relationalExpression)*;
	@Nullable
	private SpelNodeImpl eatLogicalAndExpression() {
		//处理关联表达式
		SpelNodeImpl expr = eatRelationalExpression();
		while (peekIdentifierToken("and") || peekToken(TokenKind.SYMBOLIC_AND)) {
			//如果当前Token是'AND'字面量，或者当前位置的Token类型是AND符号
			// 消费 'AND' ，返回当前位置的token，并将位置+1
			Token t = takeToken();
			//处理关联表达式
			SpelNodeImpl rhExpr = eatRelationalExpression();
			checkOperands(t, expr, rhExpr);
			expr = new OpAnd(t.startPos, t.endPos, expr, rhExpr);
		}
		return expr;
	}

	// 关联表达式 : sumExpression (relationalOperator^ sumExpression)?;
	@Nullable
	private SpelNodeImpl eatRelationalExpression() {
		//处理总数表达式
		SpelNodeImpl expr = eatSumExpression();
		Token relationalOperatorToken = maybeEatRelationalOperator();
		if (relationalOperatorToken == null) {
			//没有关联操作符，返回总数的表达式
			return expr;
		}
		// 消耗 相关操作Token
		Token t = takeToken();
		//再次消费一个总数表达式
		SpelNodeImpl rhExpr = eatSumExpression();
		//检查该表达式是否符合规范
		checkOperands(t, expr, rhExpr);
		TokenKind tk = relationalOperatorToken.kind;

		if (relationalOperatorToken.isNumericRelationalOperator()) {
			//如果是数字类型的关联操作符，根据类型，返回不同的令牌
			if (tk == TokenKind.GT) {
				return new OpGT(t.startPos, t.endPos, expr, rhExpr);
			}
			if (tk == TokenKind.LT) {
				return new OpLT(t.startPos, t.endPos, expr, rhExpr);
			}
			if (tk == TokenKind.LE) {
				return new OpLE(t.startPos, t.endPos, expr, rhExpr);
			}
			if (tk == TokenKind.GE) {
				return new OpGE(t.startPos, t.endPos, expr, rhExpr);
			}
			if (tk == TokenKind.EQ) {
				return new OpEQ(t.startPos, t.endPos, expr, rhExpr);
			}
			Assert.isTrue(tk == TokenKind.NE, "Not-equals token expected");
			return new OpNE(t.startPos, t.endPos, expr, rhExpr);
		}

		if (tk == TokenKind.INSTANCEOF) {
			//如果是INSTANCEOF，返回INSTANCEOF操作符
			return new OperatorInstanceof(t.startPos, t.endPos, expr, rhExpr);
		}

		if (tk == TokenKind.MATCHES) {
			//如果是MATCHES，返回MATCHES操作符
			return new OperatorMatches(t.startPos, t.endPos, expr, rhExpr);
		}
		//返回between操作符
		Assert.isTrue(tk == TokenKind.BETWEEN, "Between token expected");
		return new OperatorBetween(t.startPos, t.endPos, expr, rhExpr);
	}

	//总数表达式: productExpression ( (PLUS^ | MINUS^) productExpression)*;
	@Nullable
	private SpelNodeImpl eatSumExpression() {
		SpelNodeImpl expr = eatProductExpression();
		while (peekToken(TokenKind.PLUS, TokenKind.MINUS, TokenKind.INC)) {
			//如果是+或者-或者++
			//消费 PLUS or MINUS or INC
			Token t = takeToken();
			SpelNodeImpl rhExpr = eatProductExpression();
			//检查是否是正确的表达式
			checkRightOperand(t, rhExpr);
			if (t.kind == TokenKind.PLUS) {
				expr = new OpPlus(t.startPos, t.endPos, expr, rhExpr);
			} else if (t.kind == TokenKind.MINUS) {
				expr = new OpMinus(t.startPos, t.endPos, expr, rhExpr);
			}
		}
		return expr;
	}

	// 产品表达式: powerExpr ((STAR^ | DIV^| MOD^) powerExpr)* ;
	@Nullable
	private SpelNodeImpl eatProductExpression() {
		SpelNodeImpl expr = eatPowerIncDecExpression();
		while (peekToken(TokenKind.STAR, TokenKind.DIV, TokenKind.MOD)) {
			//如果是*或者/或者%
			// 消费 STAR/DIV/MOD
			Token t = takeToken();
			SpelNodeImpl rhExpr = eatPowerIncDecExpression();
			checkOperands(t, expr, rhExpr);
			if (t.kind == TokenKind.STAR) {
				//如果是该类型是* ,即返回乘法操作的实例
				expr = new OpMultiply(t.startPos, t.endPos, expr, rhExpr);
			} else if (t.kind == TokenKind.DIV) {
				//如果是该类型是/ ,即返回除法操作的实例
				expr = new OpDivide(t.startPos, t.endPos, expr, rhExpr);
			} else {
				//如果是该类型是% ,即返回取模操作的实例
				Assert.isTrue(t.kind == TokenKind.MOD, "Mod token expected");
				expr = new OpModulus(t.startPos, t.endPos, expr, rhExpr);
			}
		}
		return expr;
	}

	// 指数表达式  : unaryExpression (POWER^ unaryExpression)? (INC || DEC) ;
	@Nullable
	private SpelNodeImpl eatPowerIncDecExpression() {
		//解析一元表达式
		SpelNodeImpl expr = eatUnaryExpression();
		if (peekToken(TokenKind.POWER)) {
			//如果是指数^符号
			//消费 POWER
			Token t = takeToken();
			SpelNodeImpl rhExpr = eatUnaryExpression();
			//检查右操作是否正确
			checkRightOperand(t, rhExpr);
			return new OperatorPower(t.startPos, t.endPos, expr, rhExpr);
		}
		if (expr != null && peekToken(TokenKind.INC, TokenKind.DEC)) {
			//如果是++或者--
			//消费 INC/DEC，获取TOKEN
			Token t = takeToken();
			if (t.getKind() == TokenKind.INC) {
				return new OpInc(t.startPos, t.endPos, true, expr);
			}
			return new OpDec(t.startPos, t.endPos, true, expr);
		}
		return expr;
	}

	// 一元表达式: (PLUS^ | MINUS^ | BANG^ | INC^ | DEC^) unaryExpression | primaryExpression ;
	@Nullable
	private SpelNodeImpl eatUnaryExpression() {
		if (peekToken(TokenKind.PLUS, TokenKind.MINUS, TokenKind.NOT)) {
			//如果是+或者-或者!符号，消费并获取TOKEN
			Token t = takeToken();
			SpelNodeImpl expr = eatUnaryExpression();
			Assert.state(expr != null, "No node");
			if (t.kind == TokenKind.NOT) {
				return new OperatorNot(t.startPos, t.endPos, expr);
			}
			if (t.kind == TokenKind.PLUS) {
				return new OpPlus(t.startPos, t.endPos, expr);
			}
			Assert.isTrue(t.kind == TokenKind.MINUS, "Minus token expected");
			return new OpMinus(t.startPos, t.endPos, expr);
		}
		if (peekToken(TokenKind.INC, TokenKind.DEC)) {
			//如果是++或者--符号，消费并获取TOKEN
			Token t = takeToken();
			SpelNodeImpl expr = eatUnaryExpression();
			if (t.getKind() == TokenKind.INC) {
				return new OpInc(t.startPos, t.endPos, false, expr);
			}
			return new OpDec(t.startPos, t.endPos, false, expr);
		}
		return eatPrimaryExpression();
	}

	// 基础的表达式 : startNode (node)? -> ^(EXPRESSION startNode (node)?);
	@Nullable
	private SpelNodeImpl eatPrimaryExpression() {
		// 总是一个开始节点
		SpelNodeImpl start = eatStartNode();
		List<SpelNodeImpl> nodes = null;
		SpelNodeImpl node = eatNode();
		while (node != null) {
			if (nodes == null) {
				nodes = new ArrayList<>(4);
				nodes.add(start);
			}
			nodes.add(node);
			node = eatNode();
		}
		if (start == null || nodes == null) {
			return start;
		}
		//返回一个复合的表达式
		return new CompoundExpression(start.getStartPosition(), nodes.get(nodes.size() - 1).getEndPosition(),
				nodes.toArray(new SpelNodeImpl[0]));
	}

	// 节点 : ((DOT dottedNode) | (SAFE_NAVI dottedNode) | nonDottedNode)+;
	@Nullable
	private SpelNodeImpl eatNode() {
		//如果是.或者?.符号，返回eatDottedNode，否则返回eatNonDottedNode
		return (peekToken(TokenKind.DOT, TokenKind.SAFE_NAVI) ? eatDottedNode() : eatNonDottedNode());
	}

	// 不是.的节点: indexer;
	@Nullable
	private SpelNodeImpl eatNonDottedNode() {
		if (peekToken(TokenKind.LSQUARE)) {
			//如果是[符号
			if (maybeEatIndexer()) {
				//如果不是[符号，将上面的[令牌出栈，并返回
				return pop();
			}
		}
		return null;
	}

	//dottedNode
	// : ((methodOrProperty
	//	  | functionOrVar
	//    | projection
	//    | selection
	//    | firstSelection
	//    | lastSelection
	//    ))
	//	;
	private SpelNodeImpl eatDottedNode() {
		Token t = takeToken();  // 它是 '.' 还是一个  '?.'
		boolean nullSafeNavigation = (t.kind == TokenKind.SAFE_NAVI);
		if (maybeEatMethodOrProperty(nullSafeNavigation) || maybeEatFunctionOrVar() ||
				maybeEatProjection(nullSafeNavigation) || maybeEatSelection(nullSafeNavigation)) {
			//如果是方法或值、函数、投影、选中的节点，将令牌出栈并返回
			return pop();
		}
		//否则抛出异常
		if (peekToken() == null) {
			// 数据意外用完
			throw internalException(t.startPos, SpelMessage.OOD);
		} else {
			throw internalException(t.startPos, SpelMessage.UNEXPECTED_DATA_AFTER_DOT, toString(peekToken()));
		}
	}

	// 方法或值
	// : (POUND ID LPAREN) => function
	// | var
	//
	// function : POUND id=ID methodArgs -> ^(FUNCTIONREF[$id] methodArgs);
	// var : POUND id=ID -> ^(VARIABLEREF[$id]);
	private boolean maybeEatFunctionOrVar() {
		if (peekToken(TokenKind.HASH)) {
			//如果是#符号，消费并获取TOKEN
			Token t = takeToken();
			//消费下一个令牌，获取方法或者令牌
			Token functionOrVariableName = eatToken(TokenKind.IDENTIFIER);
			SpelNodeImpl[] args = maybeEatMethodArgs();
			if (args == null) {
				push(new VariableReference(functionOrVariableName.stringValue(),
						t.startPos, functionOrVariableName.endPos));
				return true;
			}

			push(new FunctionReference(functionOrVariableName.stringValue(),
					t.startPos, functionOrVariableName.endPos, args));
			return true;
		} else {
			return false;
		}
	}

	// 方法参数 : LPAREN! (argument (COMMA! argument)* (COMMA!)?)? RPAREN!;
	@Nullable
	private SpelNodeImpl[] maybeEatMethodArgs() {
		if (!peekToken(TokenKind.LPAREN)) {
			//如果下一个令牌符号不是(符号，返回null
			return null;
		}
		List<SpelNodeImpl> args = new ArrayList<>();
		//消费参数符号
		consumeArguments(args);
		//消费下一个)符号令牌
		eatToken(TokenKind.RPAREN);
		//转为SpelNodeImpl数组
		return args.toArray(new SpelNodeImpl[0]);
	}

	private void eatConstructorArgs(List<SpelNodeImpl> accumulatedArguments) {
		if (!peekToken(TokenKind.LPAREN)) {
			//如果下一个令牌不是(符号，抛出异常
			throw new InternalParseException(new SpelParseException(this.expressionString,
					positionOf(peekToken()), SpelMessage.MISSING_CONSTRUCTOR_ARGS));
		}
		//消耗参数令牌，获取参数列表，添加到accumulatedArguments中
		consumeArguments(accumulatedArguments);
		//消耗下一个)令牌
		eatToken(TokenKind.RPAREN);
	}

	/**
	 * 用于消耗方法或构造函数调用的参数。
	 */
	private void consumeArguments(List<SpelNodeImpl> accumulatedArguments) {
		Token t = peekToken();
		Assert.state(t != null, "Expected token");
		//获取开始位置
		int pos = t.startPos;
		Token next;
		do {
			//消费 (第一次通过) 或逗号 (随后的次数)
			nextToken();
			//弹出当前令牌
			t = peekToken();
			if (t == null) {
				throw internalException(pos, SpelMessage.RUN_OUT_OF_ARGUMENTS);
			}
			if (t.kind != TokenKind.RPAREN) {
				//如果当前令牌的符号不是)符号，添加表达式到accumulatedArguments中，并将位置+1
				accumulatedArguments.add(eatExpression());
			}
			//重置解析中的令牌
			next = peekToken();
		} while (
			//如果令牌不为空，且令牌的符号是逗号，继续解析
				next != null && next.kind == TokenKind.COMMA
		);
		if (next == null) {
			//如果下一个token为空，抛出异常
			throw internalException(pos, SpelMessage.RUN_OUT_OF_ARGUMENTS);
		}
	}

	private int positionOf(@Nullable Token t) {
		if (t == null) {
			// 如果为null，则认为问题是因为在表达式末尾未找到正确的令牌
			return this.expressionString.length();
		}
		return t.startPos;
	}

	//startNode
	// : parenExpr | literal
	//	    | type
	//	    | methodOrProperty
	//	    | functionOrVar
	//	    | projection
	//	    | selection
	//	    | firstSelection
	//	    | lastSelection
	//	    | indexer
	//	    | constructor
	@Nullable
	private SpelNodeImpl eatStartNode() {
		if (maybeEatLiteral()) {
			//如果是字面量，将刚才推入栈顶的元素弹出来，然后返回该元素
			return pop();
		} else if (maybeEatParenExpression()) {
			//如果有左右括号，返回括号内的表达式节点
			return pop();
		} else if (maybeEatTypeReference() || maybeEatNullReference() || maybeEatConstructorReference() ||
				maybeEatMethodOrProperty(false) || maybeEatFunctionOrVar()) {
			return pop();
		} else if (maybeEatBeanReference()) {
			return pop();
		} else if (maybeEatProjection(false) || maybeEatSelection(false) || maybeEatIndexer()) {
			return pop();
		} else if (maybeEatInlineListOrMap()) {
			return pop();
		} else {
			return null;
		}
	}

	// parse: @beanname @'bean.name'
	// quoted if dotted
	private boolean maybeEatBeanReference() {
		if (peekToken(TokenKind.BEAN_REF) || peekToken(TokenKind.FACTORY_BEAN_REF)) {
			Token beanRefToken = takeToken();
			Token beanNameToken = null;
			String beanName = null;
			if (peekToken(TokenKind.IDENTIFIER)) {
				beanNameToken = eatToken(TokenKind.IDENTIFIER);
				beanName = beanNameToken.stringValue();
			} else if (peekToken(TokenKind.LITERAL_STRING)) {
				beanNameToken = eatToken(TokenKind.LITERAL_STRING);
				beanName = beanNameToken.stringValue();
				beanName = beanName.substring(1, beanName.length() - 1);
			} else {
				throw internalException(beanRefToken.startPos, SpelMessage.INVALID_BEAN_REFERENCE);
			}
			BeanReference beanReference;
			if (beanRefToken.getKind() == TokenKind.FACTORY_BEAN_REF) {
				String beanNameString = String.valueOf(TokenKind.FACTORY_BEAN_REF.tokenChars) + beanName;
				beanReference = new BeanReference(beanRefToken.startPos, beanNameToken.endPos, beanNameString);
			} else {
				beanReference = new BeanReference(beanNameToken.startPos, beanNameToken.endPos, beanName);
			}
			this.constructedNodes.push(beanReference);
			return true;
		}
		return false;
	}

	private boolean maybeEatTypeReference() {
		if (peekToken(TokenKind.IDENTIFIER)) {
			//如果当前位置是标识符，获取类型名称
			Token typeName = peekToken();
			Assert.state(typeName != null, "Expected token");
			if (!"T".equals(typeName.stringValue())) {
				//类型名称为T，返回false
				return false;
			}
			// 它看起来像一个类型引用，但被用作Map的Key吗？
			//获取当前字符，位置+1
			Token t = takeToken();
			if (peekToken(TokenKind.RSQUARE)) {
				//如果当前字符是]
				// 看起来像 'T]' (T是map的key)
				push(new PropertyOrFieldReference(false, t.stringValue(), t.startPos, t.endPos));
				return true;
			}
			//消费一个(符号的令牌，位置+1
			eatToken(TokenKind.LPAREN);
			SpelNodeImpl node = eatPossiblyQualifiedId();
			// 点 合格id 是否有数组维度?
			int dims = 0;
			while (peekToken(TokenKind.LSQUARE, true)) {
				//如果当前令牌是[
				//消费一个 ] 符号，维度数量+1
				eatToken(TokenKind.RSQUARE);
				dims++;
			}
			//消费一个) 符号，位置+1
			eatToken(TokenKind.RPAREN);
			this.constructedNodes.push(new TypeReference(typeName.startPos, typeName.endPos, node, dims));
			return true;
		}
		return false;
	}

	private boolean maybeEatNullReference() {
		if (peekToken(TokenKind.IDENTIFIER)) {
			//当前字符如果是标识符，获取当前令牌
			Token nullToken = peekToken();
			Assert.state(nullToken != null, "Expected token");
			if (!"null".equalsIgnoreCase(nullToken.stringValue())) {
				//该值不是null字符串，返回false
				return false;
			}
			//获取下一个令牌，添加null字面量
			nextToken();
			this.constructedNodes.push(new NullLiteral(nullToken.startPos, nullToken.endPos));
			return true;
		}
		return false;
	}

	//投影: PROJECT^ expression RCURLY!;
	private boolean maybeEatProjection(boolean nullSafeNavigation) {
		Token t = peekToken();
		if (!peekToken(TokenKind.PROJECT, true)) {
			//如果当前符号不是![ ，返回false
			return false;
		}
		Assert.state(t != null, "No token");
		//解析表达式
		SpelNodeImpl expr = eatExpression();
		Assert.state(expr != null, "No node");
		//消费下一个 ] 符号
		eatToken(TokenKind.RSQUARE);
		this.constructedNodes.push(new Projection(nullSafeNavigation, t.startPos, t.endPos, expr));
		return true;
	}

	// list = LCURLY (element (COMMA element)*) RCURLY
	// map  = LCURLY (key ':' value (COMMA key ':' value)*) RCURLY
	private boolean maybeEatInlineListOrMap() {
		Token t = peekToken();
		if (!peekToken(TokenKind.LCURLY, true)) {
			//如果当前符号不是{ ，返回false
			return false;
		}
		Assert.state(t != null, "No token");
		SpelNodeImpl expr = null;
		//消费下一个 } 符号
		Token closingCurly = peekToken();
		if (peekToken(TokenKind.RCURLY, true)) {
			//如果是 } 符号，空的列表
			Assert.state(closingCurly != null, "No token");
			expr = new InlineList(t.startPos, closingCurly.endPos);
		} else if (peekToken(TokenKind.COLON, true)) {
			//如果是 : 符号，消耗下一个}符号
			closingCurly = eatToken(TokenKind.RCURLY);
			// 空的映射 '{:}'
			expr = new InlineMap(t.startPos, closingCurly.endPos);
		} else {
			//解析第一个表达式
			SpelNodeImpl firstExpression = eatExpression();
			// 接下来是:
			// '}' - list的结尾
			// ',' - 此列表中的更多表达式
			// ':' - 这是一个 map!
			if (peekToken(TokenKind.RCURLY)) {
				// 当前符号是 }
				// 列表中包含一个项目
				List<SpelNodeImpl> elements = new ArrayList<>();
				elements.add(firstExpression);
				closingCurly = eatToken(TokenKind.RCURLY);
				expr = new InlineList(t.startPos, closingCurly.endPos, elements.toArray(new SpelNodeImpl[0]));
			} else if (peekToken(TokenKind.COMMA, true)) {
				//当前符号是 ,
				// 多项目列表
				List<SpelNodeImpl> elements = new ArrayList<>();
				elements.add(firstExpression);
				do {
					elements.add(eatExpression());
				}
				while (peekToken(TokenKind.COMMA, true));
				closingCurly = eatToken(TokenKind.RCURLY);
				expr = new InlineList(t.startPos, closingCurly.endPos, elements.toArray(new SpelNodeImpl[0]));

			} else if (peekToken(TokenKind.COLON, true)) {
				//如果当前符号是 :
				// map!
				List<SpelNodeImpl> elements = new ArrayList<>();
				elements.add(firstExpression);
				elements.add(eatExpression());
				while (peekToken(TokenKind.COMMA, true)) {
					elements.add(eatExpression());
					eatToken(TokenKind.COLON);
					elements.add(eatExpression());
				}
				closingCurly = eatToken(TokenKind.RCURLY);
				expr = new InlineMap(t.startPos, closingCurly.endPos, elements.toArray(new SpelNodeImpl[0]));
			} else {
				// 抛出异常
				throw internalException(t.startPos, SpelMessage.OOD);
			}
		}
		this.constructedNodes.push(expr);
		return true;
	}

	private boolean maybeEatIndexer() {
		//获取当前的token
		Token t = peekToken();
		if (peekToken(TokenKind.LSQUARE, true)) {
			//如果下一个位置的令牌符号仍然是[
			Assert.state(t != null, "No token");
			//解析嵌套的表达式
			SpelNodeImpl expr = eatExpression();
			Assert.state(expr != null, "No node");
			//消费一个]符号的令牌
			eatToken(TokenKind.RSQUARE);
			//添加嵌套的表达式
			this.constructedNodes.push(new Indexer(t.startPos, t.endPos, expr));
			return true;
		}

		return false;

	}

	private boolean maybeEatSelection(boolean nullSafeNavigation) {
		Token t = peekToken();
		if (!peekSelectToken()) {
			return false;
		}
		Assert.state(t != null, "No token");
		nextToken();
		SpelNodeImpl expr = eatExpression();
		if (expr == null) {
			throw internalException(t.startPos, SpelMessage.MISSING_SELECTION_EXPRESSION);
		}
		eatToken(TokenKind.RSQUARE);
		if (t.kind == TokenKind.SELECT_FIRST) {
			this.constructedNodes.push(new Selection(nullSafeNavigation, Selection.FIRST, t.startPos, t.endPos, expr));
		} else if (t.kind == TokenKind.SELECT_LAST) {
			this.constructedNodes.push(new Selection(nullSafeNavigation, Selection.LAST, t.startPos, t.endPos, expr));
		} else {
			this.constructedNodes.push(new Selection(nullSafeNavigation, Selection.ALL, t.startPos, t.endPos, expr));
		}
		return true;
	}

	/**
	 * 消费一个标识符，可能是合格的 (意味着它是点状的)。
	 * TODO AndyC可以在这里创建完整的标识符 (a.b.c) 而不是它们的序列? (a, b, c)
	 */
	private SpelNodeImpl eatPossiblyQualifiedId() {
		//创建一个栈
		Deque<SpelNodeImpl> qualifiedIdPieces = new ArrayDeque<>();
		Token node = peekToken();
		while (isValidQualifiedId(node)) {
			//如果是有效的id，获取下一个令牌
			nextToken();
			if (node.kind != TokenKind.DOT) {
				//如果不是.符号，添加标识符
				qualifiedIdPieces.add(new Identifier(node.stringValue(), node.startPos, node.endPos));
			}
			//获取下一个令牌
			node = peekToken();
		}
		if (qualifiedIdPieces.isEmpty()) {
			//栈为空，抛出异常
			if (node == null) {
				throw internalException(this.expressionString.length(), SpelMessage.OOD);
			}
			throw internalException(node.startPos, SpelMessage.NOT_EXPECTED_TOKEN,
					"qualified ID", node.getKind().toString().toLowerCase());
		}
		//构建成合格标识符返回
		return new QualifiedIdentifier(qualifiedIdPieces.getFirst().getStartPosition(),
				qualifiedIdPieces.getLast().getEndPosition(), qualifiedIdPieces.toArray(new SpelNodeImpl[0]));
	}

	/**
	 * 是否是有效的id
	 *
	 * @param node 令牌节点
	 * @return true表示有效
	 */
	private boolean isValidQualifiedId(@Nullable Token node) {
		if (node == null || node.kind == TokenKind.LITERAL_STRING) {
			//节点为空，或者节点是字符串类型，则不是有效的id
			return false;
		}
		if (node.kind == TokenKind.DOT || node.kind == TokenKind.IDENTIFIER) {
			//如果是.或者标识符，则是有效的id
			return true;
		}
		//获取节点值，如果值不为空，且匹配VALID_QUALIFIED_ID_PATTERN，则是有效的id
		String value = node.stringValue();
		return (StringUtils.hasLength(value) && VALID_QUALIFIED_ID_PATTERN.matcher(value).matches());
	}

	// 由于支持标识符中的美元，这很复杂。美元通常是单独的令牌，但我们希望将一系列标识符和美元合并为一个标识符。
	private boolean maybeEatMethodOrProperty(boolean nullSafeNavigation) {
		if (peekToken(TokenKind.IDENTIFIER)) {
			//如果当前位置是一个标识符
			//解析为方法或者属性名称
			Token methodOrPropertyName = takeToken();
			//获取方法参数
			SpelNodeImpl[] args = maybeEatMethodArgs();
			if (args == null) {
				//属性
				push(new PropertyOrFieldReference(nullSafeNavigation, methodOrPropertyName.stringValue(),
						methodOrPropertyName.startPos, methodOrPropertyName.endPos));
				return true;
			}
			// 相关方法
			push(new MethodReference(nullSafeNavigation, methodOrPropertyName.stringValue(),
					methodOrPropertyName.startPos, methodOrPropertyName.endPos, args));
			// TODO 方法参考的结束位置是什么？名字还是最后一个arg？
			return true;
		}
		return false;
	}

	//构造函数
	//:	('new' qualifiedId LPAREN) => 'new' qualifiedId ctorArgs -> ^(CONSTRUCTOR qualifiedId ctorArgs)
	private boolean maybeEatConstructorReference() {
		if (peekIdentifierToken("new")) {
			//如果当前位置是 new 符号
			//获取当前位置的令牌
			Token newToken = takeToken();
			// It looks like a constructor reference but is NEW being used as a map key?
			// 它看起来像一个构造函数引用，但 NEW 被用作 Map 的键吗？
			if (peekToken(TokenKind.RSQUARE)) {
				//如果下一个字符是 ]
				// 看起来像 “ new]” (所以新用作地图键)
				push(new PropertyOrFieldReference(false, newToken.stringValue(), newToken.startPos, newToken.endPos));
				return true;
			}
			//可能是合格的构造函数名称
			SpelNodeImpl possiblyQualifiedConstructorName = eatPossiblyQualifiedId();
			List<SpelNodeImpl> nodes = new ArrayList<>();
			nodes.add(possiblyQualifiedConstructorName);
			if (peekToken(TokenKind.LSQUARE)) {
				//如果当前字符是[
				// 数组初始化器
				List<SpelNodeImpl> dimensions = new ArrayList<>();
				while (peekToken(TokenKind.LSQUARE, true)) {
					//如果下一个仍然是[
					if (peekToken(TokenKind.RSQUARE)) {
						//下一个符号是 ]，添加null值
						dimensions.add(null);
					} else {
						//添加解析后的表达式
						dimensions.add(eatExpression());
					}
					//消费下一个]
					eatToken(TokenKind.RSQUARE);
				}
				if (maybeEatInlineListOrMap()) {
					nodes.add(pop());
				}
				push(new ConstructorReference(newToken.startPos, newToken.endPos,
						dimensions.toArray(new SpelNodeImpl[0]), nodes.toArray(new SpelNodeImpl[0])));
			} else {
				// regular constructor invocation
				eatConstructorArgs(nodes);
				// TODO correct end position?
				push(new ConstructorReference(newToken.startPos, newToken.endPos, nodes.toArray(new SpelNodeImpl[0])));
			}
			return true;
		}
		return false;
	}

	private void push(SpelNodeImpl newNode) {
		this.constructedNodes.push(newNode);
	}

	private SpelNodeImpl pop() {
		return this.constructedNodes.pop();
	}

	//	literal
	//  : INTEGER_LITERAL
	//	| boolLiteral
	//	| STRING_LITERAL
	//  | HEXADECIMAL_INTEGER_LITERAL
	//  | REAL_LITERAL
	//	| DQ_STRING_LITERAL
	//	| NULL_LITERAL
	private boolean maybeEatLiteral() {
		Token t = peekToken();
		if (t == null) {
			return false;
		}
		//如果是对应的类型，解析成对应的字面量后，推入到栈中
		if (t.kind == TokenKind.LITERAL_INT) {
			push(Literal.getIntLiteral(t.stringValue(), t.startPos, t.endPos, 10));
		} else if (t.kind == TokenKind.LITERAL_LONG) {
			push(Literal.getLongLiteral(t.stringValue(), t.startPos, t.endPos, 10));
		} else if (t.kind == TokenKind.LITERAL_HEXINT) {
			push(Literal.getIntLiteral(t.stringValue(), t.startPos, t.endPos, 16));
		} else if (t.kind == TokenKind.LITERAL_HEXLONG) {
			push(Literal.getLongLiteral(t.stringValue(), t.startPos, t.endPos, 16));
		} else if (t.kind == TokenKind.LITERAL_REAL) {
			push(Literal.getRealLiteral(t.stringValue(), t.startPos, t.endPos, false));
		} else if (t.kind == TokenKind.LITERAL_REAL_FLOAT) {
			push(Literal.getRealLiteral(t.stringValue(), t.startPos, t.endPos, true));
		} else if (peekIdentifierToken("true")) {
			push(new BooleanLiteral(t.stringValue(), t.startPos, t.endPos, true));
		} else if (peekIdentifierToken("false")) {
			push(new BooleanLiteral(t.stringValue(), t.startPos, t.endPos, false));
		} else if (t.kind == TokenKind.LITERAL_STRING) {
			push(new StringLiteral(t.stringValue(), t.startPos, t.endPos, t.stringValue()));
		} else {
			return false;
		}
		nextToken();
		return true;
	}

	//父级表达式 : LPAREN! expression RPAREN!;
	private boolean maybeEatParenExpression() {
		if (peekToken(TokenKind.LPAREN)) {
			//如果当前位置的TOKEN是(
			nextToken();
			//解析表达式
			SpelNodeImpl expr = eatExpression();
			Assert.state(expr != null, "No node");
			//消费 ) 符号
			eatToken(TokenKind.RPAREN);
			//将表达式推入栈
			push(expr);
			return true;
		} else {
			return false;
		}
	}

	// 关系运算符
	// : EQUAL | NOT_EQUAL | LESS_THAN | LESS_THAN_OR_EQUAL | GREATER_THAN
	// | GREATER_THAN_OR_EQUAL | INSTANCEOF | BETWEEN | MATCHES
	@Nullable
	private Token maybeEatRelationalOperator() {
		Token t = peekToken();
		if (t == null) {
			return null;
		}
		if (t.isNumericRelationalOperator()) {
			//如果是数字类型的关系运算符，即>、<、>=、<=、==、!= 中的任意一种符号，返回该token
			return t;
		}
		if (t.isIdentifier()) {
			//如果是字面量，获取字符串值
			String idString = t.stringValue();
			if (idString.equalsIgnoreCase("instanceof")) {
				//如果该字符串是instanceof，将其转为INSTANCEOF令牌
				return t.asInstanceOfToken();
			}
			if (idString.equalsIgnoreCase("matches")) {
				//如果该字符串是matches，将其转为MATCHES令牌
				return t.asMatchesToken();
			}
			if (idString.equalsIgnoreCase("between")) {
				//如果该字符串是between，将其转为BETWEEN令牌
				return t.asBetweenToken();
			}
		}
		return null;
	}

	private Token eatToken(TokenKind expectedKind) {
		//返回当前令牌，并当前位置+1
		Token t = nextToken();
		if (t == null) {
			//如果令牌为空，抛出异常
			int pos = this.expressionString.length();
			throw internalException(pos, SpelMessage.OOD);
		}
		if (t.kind != expectedKind) {
			//如果不是期望的类型，抛出异常
			throw internalException(t.startPos, SpelMessage.NOT_EXPECTED_TOKEN,
					expectedKind.toString().toLowerCase(), t.getKind().toString().toLowerCase());
		}
		return t;
	}

	private boolean peekToken(TokenKind desiredTokenKind) {
		return peekToken(desiredTokenKind, false);
	}

	private boolean peekToken(TokenKind desiredTokenKind, boolean consumeIfMatched) {
		Token t = peekToken();
		if (t == null) {
			return false;
		}
		if (t.kind == desiredTokenKind) {
			//如果当前类型和指定类型相同，如果需要消费，位置+1，最后返回true
			if (consumeIfMatched) {
				this.tokenStreamPointer++;
			}
			return true;
		}

		if (desiredTokenKind == TokenKind.IDENTIFIER) {
			//如果期望类型为标识符

			// 可能是运算符的文本形式之一 (例如!=) -在这种情况下，我们可以将其视为标识符。列表在这里表示: Tokenizer。alternativeOperatorNames和那些在TokenKind枚举中是按顺序排列的。
			if (t.kind.ordinal() >= TokenKind.DIV.ordinal() && t.kind.ordinal() <= TokenKind.NOT.ordinal() &&
					t.data != null) {
				// 如果类型的序数大于 DIV 序数，且该类型的序数小于等于NOT类型的序数，且该类型的数据不为空，返回true
				// 如果t.data为空，我们就会知道它不是文本形式，而是符号形式
				return true;
			}
		}
		return false;
	}

	private boolean peekToken(TokenKind possible1, TokenKind possible2) {
		Token t = peekToken();
		if (t == null) {
			return false;
		}
		return (t.kind == possible1 || t.kind == possible2);
	}

	private boolean peekToken(TokenKind possible1, TokenKind possible2, TokenKind possible3) {
		Token t = peekToken();
		if (t == null) {
			return false;
		}
		return (t.kind == possible1 || t.kind == possible2 || t.kind == possible3);
	}

	private boolean peekIdentifierToken(String identifierString) {
		//获取当前位置的Token
		Token t = peekToken();
		if (t == null) {
			return false;
		}
		//如果token类型为标识符且token的值为指定字符串
		return (t.kind == TokenKind.IDENTIFIER && identifierString.equalsIgnoreCase(t.stringValue()));
	}

	/**
	 * 弹出选中的令牌
	 *
	 * @return true表示选中的令牌
	 */
	private boolean peekSelectToken() {
		//获取当前令牌
		Token t = peekToken();
		if (t == null) {
			return false;
		}
		//该令牌类型为 ?[ 或者 ^[ 或者 $[
		return (t.kind == TokenKind.SELECT || t.kind == TokenKind.SELECT_FIRST || t.kind == TokenKind.SELECT_LAST);
	}

	private Token takeToken() {
		if (this.tokenStreamPointer >= this.tokenStreamLength) {
			throw new IllegalStateException("No token");
		}
		return this.tokenStream.get(this.tokenStreamPointer++);
	}

	@Nullable
	private Token nextToken() {
		if (this.tokenStreamPointer >= this.tokenStreamLength) {
			return null;
		}
		return this.tokenStream.get(this.tokenStreamPointer++);
	}

	@Nullable
	private Token peekToken() {
		if (this.tokenStreamPointer >= this.tokenStreamLength) {
			return null;
		}
		return this.tokenStream.get(this.tokenStreamPointer);
	}

	public String toString(@Nullable Token t) {
		if (t == null) {
			return "";
		}
		if (t.getKind().hasPayload()) {
			return t.stringValue();
		}
		return t.kind.toString().toLowerCase();
	}

	private void checkOperands(Token token, @Nullable SpelNodeImpl left, @Nullable SpelNodeImpl right) {
		//检查左右两个表达式
		checkLeftOperand(token, left);
		checkRightOperand(token, right);
	}

	private void checkLeftOperand(Token token, @Nullable SpelNodeImpl operandExpression) {
		if (operandExpression == null) {
			throw internalException(token.startPos, SpelMessage.LEFT_OPERAND_PROBLEM);
		}
	}

	private void checkRightOperand(Token token, @Nullable SpelNodeImpl operandExpression) {
		if (operandExpression == null) {
			throw internalException(token.startPos, SpelMessage.RIGHT_OPERAND_PROBLEM);
		}
	}

	private InternalParseException internalException(int startPos, SpelMessage message, Object... inserts) {
		return new InternalParseException(new SpelParseException(this.expressionString, startPos, message, inserts));
	}

}
