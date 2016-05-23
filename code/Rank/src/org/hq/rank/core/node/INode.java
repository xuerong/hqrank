package org.hq.rank.core.node;

import org.hq.rank.core.element.Element;

public interface INode {
	public Element add(Element element);
	public boolean delete(Element element);
	public int getRankValue(Element element);
}
