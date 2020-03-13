package com.fel.function.operator.big;

import com.fel.common.NumberUtil;
import com.fel.compile.InterpreterSourceBuilder;
import com.fel.compile.SourceBuilder;
import com.fel.context.FelContext;
import com.fel.function.operator.Equal;
import com.fel.parser.FelNode;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.fel.function.operator.big.BigAdd.hasFloat;
import static com.fel.function.operator.big.BigAdd.isInt;

public class BigEqual extends Equal {


	@Override
	protected boolean compareNumber(Object left, Object right) {
		try {
			// 浮点型，转换成BigDecimal
			if (hasFloat(left, right)) {
				BigDecimal l = NumberUtil.toBigDecimal(left);
				BigDecimal r = NumberUtil.toBigDecimal(right);
				return l.equals(r);
			}

			// 数值弄，转换成BigInteger
			if (isInt(left, right)) {
				BigInteger l = NumberUtil.toBigInteger(left);
				BigInteger r = NumberUtil.toBigInteger(right);
				return l.equals(r);
			}
		} catch (NumberFormatException e) {
			// 忽略
		}
		return left.equals(right);
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
