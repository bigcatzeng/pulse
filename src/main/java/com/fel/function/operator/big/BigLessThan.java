package com.fel.function.operator.big;

import com.fel.common.NumberUtil;
import com.fel.compile.InterpreterSourceBuilder;
import com.fel.compile.SourceBuilder;
import com.fel.context.FelContext;
import com.fel.function.operator.LessThen;
import com.fel.parser.FelNode;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.fel.function.operator.big.BigAdd.hasFloat;
import static com.fel.function.operator.big.BigAdd.isInt;

public class BigLessThan extends LessThen {
	

	/**
	 * 大于 >
	 * 
	 * @param left
	 * @param right
	 * @return
	 */
    @SuppressWarnings({
			"rawtypes", "unchecked" })
	@Override
	public boolean compare(Object left, Object right) {
    	if(left == right){
			return equalsReturnValue();
		}
		if(left == null || right == null){
			return nullReturnValue();
		}
		try {
			// 浮点型，转换成BigDecimal
			if (hasFloat(left, right)) {
				BigDecimal l = NumberUtil.toBigDecimal(left);
				BigDecimal r = NumberUtil.toBigDecimal(right);
				int value = l.compareTo(r);
				return compare(value);
			}

			// 数值弄，转换成BigInteger
			if (isInt(left, right)) {
				BigInteger l = NumberUtil.toBigInteger(left);
				BigInteger r = NumberUtil.toBigInteger(right);
				return compare(l.compareTo(r));
			}
			/*		if (left instanceof Number && right instanceof Number) {

						return NumberUtil.toDouble((Number) left) > NumberUtil.toDouble((Number) right);
					}*/

			if (left instanceof Comparable && right instanceof Comparable) {
				Comparable l = (Comparable) left;
				Comparable r = (Comparable) right;
				return compare(l.compareTo(r));
			}

		} catch (NumberFormatException e) {
			throw e;
		}
		throw new IllegalArgumentException("参数[type:" + left.getClass() + ";value:" + left + "]和参数[type:"
				+ right.getClass() + ";value:" + right + "]不能进行比较[" + getName() + "]运算");
    }

	protected boolean compare(int value) {
		return value < 0;
	}


	protected boolean nullReturnValue() {
		return false;
	}

	protected boolean equalsReturnValue() {
		return false;
	}

	@Override
	public SourceBuilder toMethod(FelNode node, FelContext ctx) {
		return InterpreterSourceBuilder.getInstance();
	}


}
