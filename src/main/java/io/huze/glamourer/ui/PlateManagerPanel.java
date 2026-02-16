package io.huze.glamourer.ui;

import io.huze.glamourer.Config;
import io.huze.glamourer.glam.Glamour;
import io.huze.glamourer.glam.Glamourer;
import io.huze.glamourer.plate.Plate;
import io.huze.glamourer.plate.PlateManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.SwingUtil;

public class PlateManagerPanel extends JPanel
{
	private final ScrollablePanel platesContainer;
	private final ClientThread clientThread;
	private final PlateManager plateManager;
	private final Glamourer glamourer;
	private final Consumer<Plate> onAddItemRequest;
	private final Config config;

	private final JScrollPane scrollPane;
	private final JButton expandCollapseAllButton;

	public PlateManagerPanel(ClientThread clientThread,
							 PlateManager plateManager, Glamourer glamourer, Config config,
							 Consumer<Plate> onAddItemRequest)
	{
		this.clientThread = clientThread;
		this.plateManager = plateManager;
		this.glamourer = glamourer;
		this.config = config;
		this.onAddItemRequest = onAddItemRequest;

		setLayout(new BorderLayout());

		// Title bar at the top
		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		titlePanel.setBorder(new EmptyBorder(4, 6, 4, 4));

		JLabel titleLabel = new JLabel("Glamourer");
		titleLabel.setForeground(Color.WHITE);
		titlePanel.add(titleLabel, BorderLayout.WEST);

		JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		rightPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JButton importPlateButton = new JButton();
		ImageIcons.setImportIcon(importPlateButton);
		importPlateButton.setToolTipText("Import plate from clipboard");
		importPlateButton.addActionListener(e -> importPlateFromClipboard());
		rightPanel.add(importPlateButton);

		JButton createPlateButton = new JButton();
		ImageIcons.setCreateIcon(createPlateButton);
		createPlateButton.setToolTipText("Create plate");
		rightPanel.add(createPlateButton);

		expandCollapseAllButton = new JButton();
		expandCollapseAllButton.addActionListener(e -> toggleExpandCollapseAll());
		rightPanel.add(expandCollapseAllButton);

		titlePanel.add(rightPanel, BorderLayout.EAST);

		add(titlePanel, BorderLayout.NORTH);

		platesContainer = new ScrollablePanel();
		platesContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		scrollPane = new JScrollPane(platesContainer);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(scrollPane, BorderLayout.CENTER);

		createPlateButton.addActionListener(e -> {
			plateManager.createPlate();
			rebuildPlatesSection();
			SwingUtilities.invokeLater(() -> {
				JScrollBar vertical = scrollPane.getVerticalScrollBar();
				vertical.setValue(vertical.getMaximum());
			});
		});

		// Listen for plate changes
		plateManager.setOnPlatesChanged(v -> SwingUtilities.invokeLater(this::rebuildPlatesSection));

		// Initial build
		rebuildPlatesSection();
	}

	public void rebuildPlatesSection()
	{
		platesContainer.removeAll();

		List<Plate> plates = plateManager.getPlates();
		for (int i = 0; i < plates.size(); i++)
		{
			Plate plate = plates.get(i);
			PlateRowPanel rowPanel = new PlateRowPanel(
				plate, glamourer, clientThread,
				config.iconScale() / 100f, onAddItemRequest,
				p -> clientThread.invokeLater(() -> plateManager.deletePlate(p.getId())),
				this::exportPlateToClipboard,
				() -> {
					updateExpandCollapseButton();
					revalidate();
					repaint();
				},
				this::handleItemMove
			);

			PlateDragDropHandler.setupDragAndDrop(
				rowPanel.getHeaderPanel(),
				rowPanel,
				i,
				plateManager::movePlate,
				this::rebuildPlatesSection
			);

			platesContainer.add(rowPanel);
		}

		updateExpandCollapseButton();

		platesContainer.revalidate();
		platesContainer.repaint();
	}

	public PlateRowPanel findRowPanelForPlate(Plate plate)
	{
		for (Component comp : platesContainer.getComponents())
		{
			if (comp instanceof PlateRowPanel)
			{
				PlateRowPanel row = (PlateRowPanel) comp;
				if (row.getPlate() == plate)
				{
					return row;
				}
			}
		}
		return null;
	}

	private boolean isAnyPlateExpanded()
	{
		for (Component comp : platesContainer.getComponents())
		{
			if (comp instanceof PlateRowPanel)
			{
				if (((PlateRowPanel) comp).getPlate().isExpanded())
				{
					return true;
				}
			}
		}
		return false;
	}

	private void updateExpandCollapseButton()
	{
		boolean hasExpanded = isAnyPlateExpanded();
		ImageIcons.setExpandCollapseAllIcon(expandCollapseAllButton, hasExpanded);
		expandCollapseAllButton.setToolTipText(hasExpanded ? "Collapse all plates" : "Expand all plates");
	}

	private void toggleExpandCollapseAll()
	{
		boolean shouldExpand = !isAnyPlateExpanded();
		for (Component comp : platesContainer.getComponents())
		{
			if (comp instanceof PlateRowPanel)
			{
				((PlateRowPanel) comp).setExpanded(shouldExpand);
			}
		}
		updateExpandCollapseButton();
	}

	private void importPlateFromClipboard()
	{
		String json = JOptionPane.showInputDialog(
			SwingUtilities.windowForComponent(this),
			"Paste exported plate JSON:",
			"Import Plate",
			JOptionPane.PLAIN_MESSAGE
		);
		if (json == null || json.trim().isEmpty())
		{
			return;
		}
		clientThread.invokeLater(() -> {
			try
			{
				plateManager.importPlateFromJson(json.trim());
				SwingUtilities.invokeLater(() -> {
					rebuildPlatesSection();
					JScrollBar vertical = scrollPane.getVerticalScrollBar();
					vertical.setValue(vertical.getMaximum());
				});
			}
			catch (Exception ex)
			{
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
					SwingUtilities.windowForComponent(this),
					"Invalid plate data.",
					"Import Failed",
					JOptionPane.ERROR_MESSAGE
				));
			}
		});
	}

	private void exportPlateToClipboard(Plate plate)
	{
		try
		{
			String json = plateManager.exportPlateToJson(plate);
			StringSelection selection = new StringSelection(json);
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
			JOptionPane.showMessageDialog(
				SwingUtilities.windowForComponent(this),
				"Plate \"" + plate.getName() + "\" exported to clipboard.",
				"Export Successful",
				JOptionPane.INFORMATION_MESSAGE
			);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(
				SwingUtilities.windowForComponent(this),
				"Failed to export plate: " + e.getMessage(),
				"Export Failed",
				JOptionPane.ERROR_MESSAGE
			);
		}
	}

	private void handleItemMove(Plate sourcePlate, int sourceIndex, Plate targetPlate, int targetIndex)
	{
		clientThread.invokeLater(() -> {
			if (sourcePlate == targetPlate)
			{
				// Same plate - just reorder
				sourcePlate.moveGlamour(sourceIndex, targetIndex);
			}
			else
			{
				// Cross-plate transfer
				Glamour glam = sourcePlate.extractGlamour(glamourer, sourceIndex);
				if (glam != null)
				{
					targetPlate.insertGlamour(glamourer, targetIndex, glam);
				}
			}

			// Rebuild affected row panels on EDT
			SwingUtilities.invokeLater(() -> {
				PlateRowPanel sourceRow = findRowPanelForPlate(sourcePlate);
				if (sourceRow != null)
				{
					sourceRow.rebuildDetailsPanel();
				}
				if (sourcePlate != targetPlate)
				{
					PlateRowPanel targetRow = findRowPanelForPlate(targetPlate);
					if (targetRow != null)
					{
						targetRow.rebuildDetailsPanel();
					}
				}
			});
		});
	}

	public int getScrollPosition()
	{
		return scrollPane.getVerticalScrollBar().getValue();
	}

	public void setScrollPosition(int position)
	{
		SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(position));
	}
}
