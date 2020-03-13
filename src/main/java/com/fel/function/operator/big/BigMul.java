package com.fel.function.operator.big;

import com.fel.common.NumberUtil;
import com.fel.compile.InterpreterSourceBuilder;
import com.fel.compile.SourceBuilder;
import com.fel.context.FelContext;
import com.fel.exception.EvalException;
import com.fel.function.StableFunction;
import com.fel.function.TolerantFunction;
import com.fel.parser.FelNode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public class BigMul extends StableFunction {

	/*
	 * 大数值加法运算（并保证精度）
	 * 
	 * @see .script.function.Function#call(.script.AstNode,
	 * .script.context.ScriptContext)
	 */
	@Override
	public Object call(FelNode node, FelContext context) {
		List<FelNode> children = node.getChildren();
		if (children == null || children.isEmpty()) {
			return null;
		}
		Object left = TolerantFunction.eval(context, children.get(0));
		Object right = TolerantFunction.eval(context, children.get(1));
		try {
			// 浮点型，转换成BigDecimal
			if (BigAdd.isFloat(left) || BigAdd.isFloat(right)) {
				BigDecimal l = NumberUtil.toBigDecimal(left);
				BigDecimal r = NumberUtil.toBigDecimal(right);
				return calc(l, r);
			}

			// 数值弄，转换成BigInteger
			if (BigAdd.isInt(left) && BigAdd.isInt(right)) {
				BigInteger l = NumberUtil.toBigInteger(left);
				BigInteger r = NumberUtil.toBigInteger(right);
				return calc(l, r);
			}
		} catch (NumberFormatException e) {
			// 忽略
			throw new EvalException("执行乘法失败[" + left + "-" + right + "]", e);
		}
		throw new EvalException("执行乘法失败[" + left + "-" + right + "]");
	}

	@Override
	public String getName() {
		return "*";
	}

	Object calc(BigDecimal left, BigDecimal right) {
		return left.multiply(right);
	}

	Object calc(BigInteger left, BigInteger right) {
		return left.multiply(right);
	}


	/*
	 * 由于java中的”+、-、*、/"等不支持BigInteger和BigDecimal，所以生成的代码效率不高。
	 * @see com.fel.function.Function#toMethod(com.fel.parser.FelNode, com.fel.context.FelContext)
	 */
	@Override
	public SourceBuilder toMethod(FelNode node, FelContext ctx) {
		return InterpreterSourceBuilder.getInstance();
	}

	public static void main(String[] args) {
	}


}
