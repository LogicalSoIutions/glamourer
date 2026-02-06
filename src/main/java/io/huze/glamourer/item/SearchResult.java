package io.huze.glamourer.item;

import java.awt.image.BufferedImage;
import lombok.Value;
import net.runelite.api.ItemComposition;

@Value
public class SearchResult
{
	ItemComposition itemComposition;
	BufferedImage icon;

	public int getId()
	{
		return itemComposition.getId();
	}

	public String getName()
	{
		return itemComposition.getMembersName();
	}
}