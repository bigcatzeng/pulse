package com.fel.compile;

import com.fel.context.FelContext;
import com.fel.optimizer.Optimizer;
import com.fel.parser.FelNode;

public interface SourceGenerator {
	
	/**
	 * 获取表达式JAVA源代码
	 * @param node TODO
	 * @return 
	 */
	JavaSource getSource(FelContext ctx, FelNode node);
	
	void addOpti(Optimizer opti);
	
	
}
