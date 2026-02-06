package io.huze.glamourer.ui;

import io.huze.glamourer.plate.Plate;
import java.awt.Color;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ItemDragDropHandler
{

	private static final Color DRAG_INDICATOR_COLOR = new Color(220, 138, 0);
	static final DataFlavor ITEM_FLAVOR = new DataFlavor(ItemData.class, "GlamourItem");

	public interface ItemMoveCallback
	{
		void moveItem(Plate sourcePlate, int sourceIndex, Plate targetPlate, int targetIndex);
	}

	/// Data transferred during item drag operations.
	public static class ItemData
	{
		public final Plate plate;
		public final int index;

		public ItemData(Plate plate, int index)
		{
			this.plate = plate;
			this.index = index;
		}
	}

	public static void setupItemDragAndDrop(JComponent component, Plate plate, int index,
											ItemMoveCallback moveCallback, Runnable onDragEnd)
	{
		setupDragSource(component, plate, index, onDragEnd);
		setupItemDropTarget(component, plate, index, moveCallback);
	}

	private static void setupDragSource(JComponent component, Plate plate, int index, Runnable onDragEnd)
	{
		Color originalBackground = component.getBackground();
		DragSource dragSource = DragSource.getDefaultDragSource();

		DragGestureListener listener = dge -> {
			if (DragDropUtil.isLeftButtonDrag(dge))
			{
				SingleFlavorTransferable<ItemData> transferable =
					new SingleFlavorTransferable<>(ITEM_FLAVOR, new ItemData(plate, index));
				DragDropUtil.startDrag(dge, transferable, component, originalBackground, onDragEnd);
			}
		};

		dragSource.createDefaultDragGestureRecognizer(component, DnDConstants.ACTION_MOVE, listener);
	}

	private static void setupItemDropTarget(JComponent component, Plate plate, int index,
											ItemMoveCallback moveCallback)
	{
		javax.swing.border.Border originalBorder = component.getBorder();
		Insets originalInsets = originalBorder != null ? originalBorder.getBorderInsets(component) : new Insets(0, 0, 0, 0);

		new DropTarget(component, DnDConstants.ACTION_MOVE, new DropTargetAdapter()
		{
			private boolean insertAfter = false;

			@Override
			public void dragOver(DropTargetDragEvent dtde)
			{
				ItemData data = DragDropUtil.getTransferData(dtde.getTransferable(), ITEM_FLAVOR, ItemData.class);
				if (data == null)
				{
					return;
				}

				dtde.acceptDrag(DnDConstants.ACTION_MOVE);
				insertAfter = calculateInsertPosition(dtde, data, plate, index, component);
				updateDragIndicator(component, data, plate, index, originalBorder, originalInsets, insertAfter);
			}

			@Override
			public void dragExit(DropTargetEvent dte)
			{
				component.setBorder(originalBorder);
			}

			@Override
			public void drop(DropTargetDropEvent dtde)
			{
				component.setBorder(originalBorder);
				handleItemDrop(dtde, plate, index, insertAfter, moveCallback);
			}
		}, true);
	}

	private static boolean calculateInsertPosition(DropTargetDragEvent dtde, ItemData data,
												   Plate plate, int index, JComponent component)
	{
		if (data.plate == plate)
		{
			return DragDropUtil.shouldInsertAfter(data.index, index);
		}
		else
		{
			return dtde.getLocation().y >= component.getHeight() / 2;
		}
	}

	private static void updateDragIndicator(JComponent component, ItemData data, Plate plate, int index,
											javax.swing.border.Border originalBorder, Insets originalInsets,
											boolean insertAfter)
	{
		if (data.plate == plate && data.index == index)
		{
			component.setBorder(originalBorder);
		}
		else
		{
			component.setBorder(DragDropUtil.createIndicatorBorder(originalInsets, !insertAfter, DRAG_INDICATOR_COLOR));
		}
	}

	private static void handleItemDrop(DropTargetDropEvent dtde, Plate plate, int index,
									   boolean insertAfter, ItemMoveCallback moveCallback)
	{
		ItemData data = DragDropUtil.getTransferData(dtde.getTransferable(), ITEM_FLAVOR, ItemData.class);
		if (data == null)
		{
			return;
		}

		if (data.plate == plate && data.index == index)
		{
			dtde.dropComplete(false);
			return;
		}

		dtde.acceptDrop(DnDConstants.ACTION_MOVE);
		int targetIndex = (data.plate == plate)
			? DragDropUtil.adjustTargetIndex(data.index, index, insertAfter)
			: (insertAfter ? index + 1 : index);

		moveCallback.moveItem(data.plate, data.index, plate, targetIndex);
		dtde.dropComplete(true);
	}

	public static void setupAddItemButtonDropTarget(JButton button, Plate plate,
													ItemMoveCallback moveCallback, Runnable onDragEnd)
	{
		String originalText = button.getText();

		new DropTarget(button, DnDConstants.ACTION_MOVE, new DropTargetAdapter()
		{
			private boolean isValidDrop = false;

			@Override
			public void dragOver(DropTargetDragEvent dtde)
			{
				ItemData data = DragDropUtil.getTransferData(dtde.getTransferable(), ITEM_FLAVOR, ItemData.class);
				isValidDrop = data != null && data.plate != plate;

				if (isValidDrop)
				{
					dtde.acceptDrag(DnDConstants.ACTION_MOVE);
					showDropZoneAppearance(button);
				}
			}

			@Override
			public void dragExit(DropTargetEvent dte)
			{
				revertButtonAppearance(button, originalText);
			}

			@Override
			public void drop(DropTargetDropEvent dtde)
			{
				revertButtonAppearance(button, originalText);
				handleButtonDrop(dtde, isValidDrop, plate, moveCallback, onDragEnd);
			}
		}, true);
	}

	private static void showDropZoneAppearance(JButton button)
	{
		button.setText("Drop Here");
		button.setBackground(new Color(
			DRAG_INDICATOR_COLOR.getRed(),
			DRAG_INDICATOR_COLOR.getGreen(),
			DRAG_INDICATOR_COLOR.getBlue(), 100));
		button.setBorder(BorderFactory.createLineBorder(DRAG_INDICATOR_COLOR, 2));
		button.setContentAreaFilled(true);
	}

	private static void revertButtonAppearance(JButton button, String originalText)
	{
		button.setText(originalText);
		button.setContentAreaFilled(false);
		button.setBorder(UIManager.getBorder("Button.border"));
	}

	private static void handleButtonDrop(DropTargetDropEvent dtde, boolean isValidDrop,
										 Plate plate, ItemMoveCallback moveCallback, Runnable onDragEnd)
	{
		ItemData data = DragDropUtil.getTransferData(dtde.getTransferable(), ITEM_FLAVOR, ItemData.class);
		if (!isValidDrop || data == null)
		{
			dtde.rejectDrop();
			return;
		}

		dtde.acceptDrop(DnDConstants.ACTION_MOVE);
		moveCallback.moveItem(data.plate, data.index, plate, plate.getGlamours().size());
		dtde.dropComplete(true);

		if (onDragEnd != null)
		{
			SwingUtilities.invokeLater(onDragEnd);
		}
	}
}
