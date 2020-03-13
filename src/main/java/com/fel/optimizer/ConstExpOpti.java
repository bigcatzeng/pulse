package com.fel.optimizer;

import com.fel.common.Null;
import com.fel.common.ReflectUtil;
import com.fel.compile.SourceBuilder;
import com.fel.compile.VarBuffer;
import com.fel.context.FelContext;
import com.fel.parser.ConstNode;
import com.fel.parser.FelNode;

/**
 * 当表达式是常量表达式，对表达式进行优化。
 * 
 * @author yuqingsong
 * 
 */
public class ConstExpOpti implements Optimizer {

	@Override
	public FelNode call(FelContext ctx, FelNode node) {
		if (node instanceof ConstNode) {
			final Object value = node.eval(ctx);

			// 重新构建常量节点的java源码
			node.setSourcebuilder(new SourceBuilder() {

				@Override
				public String source(FelContext ctx, FelNode node) {
					// Class<?> type = returnType(ctx, node);
					return VarBuffer.push(value, Object.class);
				}

				@Override
				public Class<?> returnType(FelContext ctx, FelNode node) {
					if (value != null) {
						Class<?> cls = value.getClass();
						if (cls.isPrimitive()) {
							return ReflectUtil.toWrapperClass(cls);
						}
						return cls;
					}
					return Null.class;
				}
			});
		}
		return node;
	}

}
