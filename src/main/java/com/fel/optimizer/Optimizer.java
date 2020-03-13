package com.fel.optimizer;

import com.fel.context.FelContext;
import com.fel.parser.FelNode;

/**
 * 优化器
 * @author yuqingsong
 *
 */
public interface Optimizer  {
	
	
	FelNode call(FelContext ctx, FelNode node);
}
