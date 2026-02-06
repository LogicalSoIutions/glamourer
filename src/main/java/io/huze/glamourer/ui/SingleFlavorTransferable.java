package io.huze.glamourer.ui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

/**
 * Generic Transferable implementation for drag-and-drop operations
 * that transfer a single data type.
 *
 * @param <T> The type of data being transferred
 */
public class SingleFlavorTransferable<T> implements Transferable
{
	private final DataFlavor flavor;
	private final DataFlavor[] flavors;
	private final T data;

	public SingleFlavorTransferable(DataFlavor flavor, T data)
	{
		this.flavor = flavor;
		this.flavors = new DataFlavor[]{flavor};
		this.data = data;
	}

	@Override
	public DataFlavor[] getTransferDataFlavors()
	{
		return flavors;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor)
	{
		return this.flavor.equals(flavor);
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
	{
		if (!isDataFlavorSupported(flavor))
		{
			throw new UnsupportedFlavorException(flavor);
		}
		return data;
	}
}
