package io.huze.glamourer.ui;

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
import java.util.function.BiConsumer;
import javax.swing.JComponent;

public class PlateDragDropHandler
{
	private static final Color PLATE_DRAG_COLOR = new Color(50, 205, 50);
	static final DataFlavor PLATE_FLAVOR = new DataFlavor(Integer.class, "PlateIndex");

	public static void setupDragAndDrop(JComponent dragDropComponent, JComponent visualComponent, int index,
										BiConsumer<Integer, Integer> onReorder, Runnable onDragEnd)
	{
		final javax.swing.border.Border originalBorder = visualComponent.getBorder();
		final Color originalBackground = visualComponent.getBackground();
		final Insets originalInsets = originalBorder != null ? originalBorder.getBorderInsets(visualComponent) : new Insets(0, 0, 0, 0);

		DragSource dragSource = DragSource.getDefaultDragSource();

		DragGestureListener dragGestureListener = dge -> {
			if (DragDropUtil.isLeftButtonDrag(dge))
			{
				SingleFlavorTransferable<Integer> transferable = new SingleFlavorTransferable<>(PLATE_FLAVOR, index);
				DragDropUtil.startDrag(dge, transferable, visualComponent, originalBackground, onDragEnd);
			}
		};

		dragSource.createDefaultDragGestureRecognizer(dragDropComponent, DnDConstants.ACTION_MOVE, dragGestureListener);

		new DropTarget(dragDropComponent, DnDConstants.ACTION_MOVE, new DropTargetAdapter()
		{
			private boolean insertAfter = false;

			@Override
			public void dragOver(DropTargetDragEvent dtde)
			{
				Integer sourceIndex = DragDropUtil.getTransferData(dtde.getTransferable(), PLATE_FLAVOR, Integer.class);
				if (sourceIndex == null)
				{
					dtde.rejectDrag();
					return;
				}

				dtde.acceptDrag(DnDConstants.ACTION_MOVE);

				if (sourceIndex < index)
				{
					insertAfter = true;
					visualComponent.setBorder(DragDropUtil.createIndicatorBorder(originalInsets, false, PLATE_DRAG_COLOR));
				}
				else if (sourceIndex > index)
				{
					insertAfter = false;
					visualComponent.setBorder(DragDropUtil.createIndicatorBorder(originalInsets, true, PLATE_DRAG_COLOR));
				}
				else
				{
					visualComponent.setBorder(originalBorder);
				}
			}

			@Override
			public void dragExit(DropTargetEvent dte)
			{
				visualComponent.setBorder(originalBorder);
			}

			@Override
			public void drop(DropTargetDropEvent dtde)
			{
				visualComponent.setBorder(originalBorder);

				Integer fromIndex = DragDropUtil.getTransferData(dtde.getTransferable(), PLATE_FLAVOR, Integer.class);
				if (fromIndex == null || fromIndex == index)
				{
					dtde.rejectDrop();
					return;
				}

				dtde.acceptDrop(DnDConstants.ACTION_MOVE);
				int toIndex = DragDropUtil.adjustTargetIndex(fromIndex, index, insertAfter);
				onReorder.accept(fromIndex, toIndex);
				dtde.dropComplete(true);
			}
		}, true);
	}
}
