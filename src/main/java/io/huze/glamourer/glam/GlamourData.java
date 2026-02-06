package io.huze.glamourer.glam;

import io.huze.glamourer.color.ColorReplacement;
import java.io.Serializable;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GlamourData implements Serializable
{
	private int version = 1;
	private String itemKey;
	private ColorReplacement[] colorReplacements;
	private @Nullable Integer replacementModelId;

	public GlamourData(String itemKey, ColorReplacement[] colorReplacements, @Nullable Integer replacementModelId)
	{
		this.itemKey = itemKey;
		this.colorReplacements = colorReplacements;
		this.replacementModelId = replacementModelId;
	}
}
