package io.huze.glamourer.ui;

import java.awt.Dimension;
import javax.swing.JToggleButton;
import net.runelite.client.util.SwingUtil;

public class ToggleSwitch extends JToggleButton
{
	public ToggleSwitch(boolean selected)
	{
		super(ImageIcons.OFF_SWITCHER, selected);
		setSelectedIcon(ImageIcons.ON_SWITCHER);
		SwingUtil.removeButtonDecorations(this);
		setPreferredSize(new Dimension(25, 25));
		addItemListener(l -> updateTooltip());
		updateTooltip();
	}

	private void updateTooltip()
	{
		setToolTipText(isSelected() ? "Disable" : "Enable");
	}
}
