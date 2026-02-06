package io.huze.glamourer.ui;

import java.awt.Color;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

public class DragDropUtil
{
	public static final Color DRAGGING_SOURCE_COLOR = new Color(60, 60, 60);
	public static final int INDICATOR_THICKNESS = 2;

	/// Creates a border that shows a drop indicator line on top or bottom, while preserving the original padding/insets.
	public static Border createIndicatorBorder(Insets originalInsets, boolean showOnTop, Color color)
	{
		int top = originalInsets.top;
		int bottom = originalInsets.bottom;
		int left = originalInsets.left;
		int right = originalInsets.right;

		if (showOnTop)
		{
			return BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(INDICATOR_THICKNESS, 0, 0, 0, color),
				BorderFactory.createEmptyBorder(Math.max(0, top - INDICATOR_THICKNESS), left, bottom, right)
			);
		}
		else
		{
			return BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, INDICATOR_THICKNESS, 0, color),
				BorderFactory.createEmptyBorder(top, left, Math.max(0, bottom - INDICATOR_THICKNESS), right)
			);
		}
	}

	 /// Starts a drag operation with standard visual feedback.
	public static void startDrag(DragGestureEvent dge, Transferable transferable,
								 JComponent visualComponent, Color originalBackground,
								 Runnable onDragEnd)
	{
		// Dim the source component
		visualComponent.setBackground(DRAGGING_SOURCE_COLOR);

		// Use transparent drag image to avoid ghost
		BufferedImage transparentImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

		dge.getDragSource().startDrag(
			dge,
			DragSource.DefaultMoveDrop,
			transparentImg,
			new Point(0, 0),
			transferable,
			new DragSourceAdapter()
			{
				@Override
				public void dragDropEnd(DragSourceDropEvent dsde)
				{
					visualComponent.setBackground(originalBackground);
					if (onDragEnd != null)
					{
						SwingUtilities.invokeLater(onDragEnd);
					}
				}
			}
		);
	}

	/// Checks if a drag gesture was initiated with the left mouse button.
	/// Only left-button drags should start drag operations.
	public static boolean isLeftButtonDrag(DragGestureEvent dge)
	{
		if (dge.getTriggerEvent() instanceof MouseEvent)
		{
			MouseEvent me = (MouseEvent) dge.getTriggerEvent();
			return SwingUtilities.isLeftMouseButton(me);
		}
		return true; // Default to allowing if we can't determine
	}

	/// Calculates the target index adjustment when reordering items.
	/// Handles the case where removing an item before the target shifts indices.
	public static int adjustTargetIndex(int sourceIndex, int targetIndex, boolean insertAfter)
	{
		int adjustedTarget = insertAfter ? targetIndex + 1 : targetIndex;

		// If moving within same container and source is before target, adjust for removal
		if (sourceIndex < targetIndex && insertAfter)
		{
			adjustedTarget = targetIndex;
		}

		return adjustedTarget;
	}

	/// Determines whether to show the drop indicator on top or bottom based on drag direction within the same container.
	public static boolean shouldInsertAfter(int sourceIndex, int targetIndex)
	{
		// Dragging down (source above target): insert after (bottom indicator)
		// Dragging up (source below target): insert before (top indicator)
		return sourceIndex < targetIndex;
	}

	/// Extracts transfer data safely, returning null on any error.
	@SuppressWarnings("unchecked")
	public static <T> T getTransferData(Transferable transferable, DataFlavor flavor, Class<T> type)
	{
		try
		{
			if (transferable.isDataFlavorSupported(flavor))
			{
				Object data = transferable.getTransferData(flavor);
				if (type.isInstance(data))
				{
					return (T) data;
				}
			}
		}
		catch (Exception ignored)
		{
		}
		return null;
	}
}
