package io.huze.glamourer.ui;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Ordering
{
	ALPHABETICAL("A to Z"),
	REVERSE_ALPHABETICAL("Z to A"),
	ITEM_ID("Item ID");

	final String name;

	@Override
	public String toString()
	{
		return name;
	}
}