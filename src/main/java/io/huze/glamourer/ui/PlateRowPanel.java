package io.huze.glamourer.ui;

import io.huze.glamourer.color.ColorGroup;
import io.huze.glamourer.color.ColorReplacement;
import io.huze.glamourer.glam.Glamour;
import io.huze.glamourer.glam.Glamourer;
import io.huze.glamourer.plate.Plate;
import io.huze.glamourer.ui.colorpicker.GroupColorLabel;
import io.huze.glamourer.ui.colorpicker.SingleColorLabel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.SwingUtil;

@Slf4j
public class PlateRowPanel extends JPanel
{
	@Getter
	private final Plate plate;
	private final Glamourer glamourer;
	private final ClientThread clientThread;
	private final float iconScale;
	private final Consumer<Plate> onAddItemRequest;
	private final Runnable onExpandToggle;
	private final ItemDragDropHandler.ItemMoveCallback onItemMoved;

	private boolean expanded;
	private final Set<String> expandedGroups = new HashSet<>();
	private final JPanel detailsPanel;
	@Getter
	private final JPanel headerPanel;
	private final JButton expandButton;
	private final ToggleSwitch enabledToggle;
	private final JLabel nameLabel;
	private final JTextField nameField;
	private final JPanel nameContainer;
	private final CardLayout nameCardLayout;
	private boolean editingCancelled;

	public PlateRowPanel(Plate plate, Glamourer glamourer, ClientThread clientThread,
						 float iconScale, Consumer<Plate> onAddItemRequest,
						 Consumer<Plate> onDeleteRequest, Runnable onExpandToggle,
						 ItemDragDropHandler.ItemMoveCallback onItemMoved)
	{
		this.plate = plate;
		this.glamourer = glamourer;
		this.clientThread = clientThread;
		this.iconScale = iconScale;
		this.onAddItemRequest = onAddItemRequest;
		this.onExpandToggle = onExpandToggle;
		this.onItemMoved = onItemMoved;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
			BorderFactory.createEmptyBorder(3, 3, 3, 3)
		));

		// Header panel using BorderLayout for compact fit
		headerPanel = new JPanel(new BorderLayout(2, 0));
		headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// Left side: expand button with icon
		expanded = plate.isExpanded();
		expandButton = new JButton();
		ImageIcons.setExpandIcon(expandButton, expanded);
		expandButton.addActionListener(e -> toggleExpanded());
		headerPanel.add(expandButton, BorderLayout.WEST);

		// Center: name container with CardLayout for inline editing
		nameCardLayout = new CardLayout();
		nameContainer = new JPanel(nameCardLayout);
		nameContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		nameContainer.setMinimumSize(new Dimension(0, 0));

		// Name label (default view) - left aligned, clips long text
		nameLabel = new JLabel(plate.getName(), SwingConstants.LEFT);
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setBorder(new EmptyBorder(0, 4, 0, 4));
		nameLabel.setPreferredSize(new Dimension(0, nameLabel.getPreferredSize().height));
		nameLabel.setMinimumSize(new Dimension(0, nameLabel.getPreferredSize().height));
		nameContainer.add(nameLabel, "label");

		// Name text field (edit view)
		nameField = new JTextField(plate.getName());
		nameField.setHorizontalAlignment(JTextField.LEFT);
		nameField.setBorder(new EmptyBorder(0, 4, 0, 4));
		nameField.setPreferredSize(new Dimension(0, nameField.getPreferredSize().height));
		nameField.setMinimumSize(new Dimension(0, nameField.getPreferredSize().height));
		nameField.addActionListener(e -> finishEditing());
		nameField.addFocusListener(new java.awt.event.FocusAdapter()
		{
			@Override
			public void focusLost(java.awt.event.FocusEvent e)
			{
				finishEditing();
			}
		});
		nameField.addKeyListener(new java.awt.event.KeyAdapter()
		{
			@Override
			public void keyPressed(java.awt.event.KeyEvent e)
			{
				if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE)
				{
					cancelEditing();
				}
			}
		});
		nameContainer.add(nameField, "edit");

		nameCardLayout.show(nameContainer, "label");
		headerPanel.add(nameContainer, BorderLayout.CENTER);

		// Right side: edit + toggle in a compact panel
		JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
		rightPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JButton editButton = new JButton();
		ImageIcons.setEditIcon(editButton);
		editButton.setToolTipText("Rename");
		editButton.addActionListener(e -> startEditing());
		rightPanel.add(editButton);

		enabledToggle = new ToggleSwitch(plate.isEnabled());
		enabledToggle.addActionListener(e -> {
			boolean enabled = enabledToggle.isSelected();
			clientThread.invokeLater(() -> plate.setEnabledAndApply(glamourer, enabled));
		});
		rightPanel.add(enabledToggle);

		headerPanel.add(rightPanel, BorderLayout.EAST);

		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem deleteItem = new JMenuItem("Delete");
		deleteItem.addActionListener(e -> {
			if (plate.getGlamours().isEmpty())
			{
				onDeleteRequest.accept(plate);
			}
			else
			{
				int result = DialogUtil.showConfirmDialogNearCursor(
					SwingUtilities.windowForComponent(this),
					"Delete plate \"" + plate.getName() + "\"?",
					"Confirm Delete",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE
				);
				if (result == JOptionPane.YES_OPTION)
				{
					onDeleteRequest.accept(plate);
				}
			}
		});
		popupMenu.add(deleteItem);
		headerPanel.setComponentPopupMenu(popupMenu);

		add(headerPanel, BorderLayout.NORTH);

		// Details panel (visibility based on saved expanded state)
		detailsPanel = new JPanel();
		detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
		detailsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		detailsPanel.setBorder(new EmptyBorder(5, 3, 3, 3));
		detailsPanel.setVisible(expanded);

		add(detailsPanel, BorderLayout.CENTER);

		rebuildDetailsPanel();
	}

	private void toggleExpanded()
	{
		expanded = !expanded;
		ImageIcons.setExpandIcon(expandButton, expanded);
		detailsPanel.setVisible(expanded);
		plate.setExpanded(expanded);
		revalidate();
		if (onExpandToggle != null)
		{
			onExpandToggle.run();
		}
	}

	private void startEditing()
	{
		editingCancelled = false;
		nameField.setText(plate.getName());
		nameCardLayout.show(nameContainer, "edit");
		nameField.requestFocusInWindow();
		nameField.selectAll();
	}

	private void finishEditing()
	{
		if (editingCancelled)
		{
			return;
		}
		String newName = nameField.getText().trim();
		if (!newName.isEmpty() && !newName.equals(plate.getName()))
		{
			plate.setName(newName);
			nameLabel.setText(newName);
		}
		nameCardLayout.show(nameContainer, "label");
	}

	private void cancelEditing()
	{
		editingCancelled = true;
		nameField.setText(plate.getName());
		nameCardLayout.show(nameContainer, "label");
	}

	public void setExpanded(boolean expanded)
	{
		if (this.expanded != expanded)
		{
			toggleExpanded();
		}
	}

	public void rebuildDetailsPanel()
	{
		detailsPanel.removeAll();

		int glamourIndex = 0;
		for (Glamour glam : plate.getGlamours())
		{
			JPanel itemPanel = createGlamourItemPanel(glam, glamourIndex);

			final int i = glamourIndex;
			ItemDragDropHandler.setupItemDragAndDrop(
				itemPanel,
				plate,
				i,
				onItemMoved,
				this::rebuildDetailsPanel
			);

			detailsPanel.add(itemPanel);
			detailsPanel.add(Box.createVerticalStrut(3));
			glamourIndex++;
		}

		JButton addItemButton = new JButton("+ Add Item");
		addItemButton.setMargin(new Insets(2, 6, 2, 6));
		addItemButton.addActionListener(e -> {
			if (onAddItemRequest != null)
			{
				onAddItemRequest.accept(plate);
			}
		});

		// Setup as drop target for items from OTHER plates
		ItemDragDropHandler.setupAddItemButtonDropTarget(addItemButton, plate, onItemMoved, this::rebuildDetailsPanel);

		JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		buttonWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		buttonWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, addItemButton.getPreferredSize().height + 4));
		buttonWrapper.add(addItemButton);
		detailsPanel.add(buttonWrapper);

		detailsPanel.revalidate();
		detailsPanel.repaint();
	}

	private JPanel createGlamourItemPanel(Glamour glam, int glamourIndex)
	{
		JPanel panel = new JPanel(new BorderLayout())
		{
			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(3, 3, 3, 3));

		panel.add(createItemHeaderRow(glam, glamourIndex), BorderLayout.NORTH);

		JPanel bodyPanel = new JPanel(new BorderLayout(5, 0));
		bodyPanel.setOpaque(false);
		bodyPanel.add(createItemIconLabel(glam), BorderLayout.WEST);
		bodyPanel.add(createColorsPanel(glam, glamourIndex), BorderLayout.CENTER);

		panel.add(bodyPanel, BorderLayout.CENTER);
		return panel;
	}

	private JLabel createItemIconLabel(Glamour glam)
	{
		JLabel iconLabel = new JLabel();
		ImageIcons.setScaledIcon(iconLabel, glam.getImage(), iconScale);
		return iconLabel;
	}

	private JPanel createItemHeaderRow(Glamour glam, int glamourIndex)
	{
		JPanel headerRow = new JPanel(new BorderLayout(2, 0));
		headerRow.setOpaque(false);
		headerRow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_HOVER_COLOR));

		JLabel itemNameLabel = new JLabel(glam.getItemName(), SwingConstants.LEFT);
		itemNameLabel.setForeground(Color.WHITE);
		itemNameLabel.setBorder(new EmptyBorder(4, 0, 4, 0));
		headerRow.add(itemNameLabel, BorderLayout.CENTER);

		JButton removeButton = new JButton();
		ImageIcons.setBinIcon(removeButton);
		removeButton.setToolTipText("Remove");
		removeButton.addActionListener(e -> clientThread.invokeLater(() -> {
			plate.removeGlamour(glamourer, glamourIndex);
			SwingUtilities.invokeLater(this::rebuildDetailsPanel);
		}));
		headerRow.add(removeButton, BorderLayout.EAST);

		return headerRow;
	}

	private JPanel createColorsPanel(Glamour glam, int glamourIndex)
	{
		JPanel colorsPanel = new JPanel();
		colorsPanel.setLayout(new BoxLayout(colorsPanel, BoxLayout.Y_AXIS));
		colorsPanel.setOpaque(false);

		List<ColorReplacement> pairs = glam.getColorReplacementsForUI();
		List<ColorGroup> groups = ColorGroup.groupColors(pairs);
		int groupNum = 0;
		int displayNum = 1;

		for (ColorGroup group : groups)
		{
			if (groupNum > 0)
			{
				colorsPanel.add(Box.createVerticalStrut(2));
			}

			boolean isMultiColor = group.getColorIndices().size() > 1;
			boolean isCoherent = group.areReplacementsCoherent(pairs);
			String groupKey = glamourIndex + "_" + groupNum;
			boolean isGroupExpanded = expandedGroups.contains(groupKey);

			if (isMultiColor && isCoherent)
			{
				if (isGroupExpanded)
				{
					displayNum = addExpandedGroupRows(colorsPanel, glam, glamourIndex, groupNum, group, pairs, displayNum);
				}
				else
				{
					addCollapsedGroupRow(colorsPanel, glam, glamourIndex, groupNum, group, pairs);
					displayNum += group.getColorIndices().size();
				}
			}
			else
			{
				displayNum = addSingleColorRows(colorsPanel, glam, glamourIndex, group, pairs, displayNum);
			}

			groupNum++;
		}

		return colorsPanel;
	}

	private int addExpandedGroupRows(JPanel colorsPanel, Glamour glam, int glamourIndex, int groupNum,
									 ColorGroup group, List<ColorReplacement> pairs, int displayNum)
	{
		boolean isFirst = true;
		for (int colorIdx : group.getColorIndices())
		{
			ColorReplacement pair = pairs.get(colorIdx);

			if (!isFirst)
			{
				colorsPanel.add(Box.createVerticalStrut(2));
			}

			JPanel rowPanel = new JPanel(new BorderLayout(3, 0));
			rowPanel.setOpaque(false);
			rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
			rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

			if (isFirst)
			{
				rowPanel.add(createCollapseButton(glamourIndex, groupNum), BorderLayout.WEST);
			}
			else
			{
				rowPanel.setBorder(new EmptyBorder(0, 20, 0, 0));
			}

			SingleColorLabel colorLabel = new SingleColorLabel(glam.getItemName() + " Color " + displayNum, pair);
			colorLabel.setOnColorChange(newColor -> updateSingleColor(glamourIndex, colorIdx, newColor));
			rowPanel.add(colorLabel, BorderLayout.CENTER);

			colorsPanel.add(rowPanel);
			isFirst = false;
			displayNum++;
		}
		return displayNum;
	}

	private void addCollapsedGroupRow(JPanel colorsPanel, Glamour glam, int glamourIndex, int groupNum,
									  ColorGroup group, List<ColorReplacement> pairs)
	{
		JPanel groupHeaderPanel = new JPanel(new BorderLayout(3, 0));
		groupHeaderPanel.setOpaque(false);
		groupHeaderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		groupHeaderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

		groupHeaderPanel.add(createExpandButton(glamourIndex, groupNum), BorderLayout.WEST);

		List<ColorReplacement> groupReplacements = new ArrayList<>();
		for (int idx : group.getColorIndices())
		{
			groupReplacements.add(pairs.get(idx));
		}

		String label = glam.getItemName() + " Group " + (groupNum + 1) + " (" + group.getColorIndices().size() + " colors)";
		GroupColorLabel colorLabel = new GroupColorLabel(label, groupReplacements);
		colorLabel.setOnColorChange(newColor -> updateGroupColors(glamourIndex, group, newColor));
		colorLabel.setOnRevert(() -> revertGroupColors(glamourIndex, group, groupReplacements));
		groupHeaderPanel.add(colorLabel, BorderLayout.CENTER);

		colorsPanel.add(groupHeaderPanel);
	}

	private int addSingleColorRows(JPanel colorsPanel, Glamour glam, int glamourIndex,
								   ColorGroup group, List<ColorReplacement> pairs, int displayNum)
	{
		boolean first = true;
		for (int colorIdx : group.getColorIndices())
		{
			ColorReplacement pair = pairs.get(colorIdx);

			if (!first)
			{
				colorsPanel.add(Box.createVerticalStrut(2));
			}
			first = false;

			SingleColorLabel colorLabel = new SingleColorLabel(glam.getItemName() + " Color " + displayNum, pair);
			colorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			colorLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, colorLabel.getPreferredSize().height));
			colorLabel.setOnColorChange(newColor -> updateSingleColor(glamourIndex, colorIdx, newColor));
			colorsPanel.add(colorLabel);
			displayNum++;
		}
		return displayNum;
	}

	private JButton createExpandButton(int glamourIndex, int groupNum)
	{
		JButton expandButton = new JButton();
		ImageIcons.setExpandIcon(expandButton, false);
		expandButton.addActionListener(e -> {
			expandedGroups.add(glamourIndex + "_" + groupNum);
			rebuildDetailsPanel();
		});
		return expandButton;
	}

	private JButton createCollapseButton(int glamourIndex, int groupNum)
	{
		JButton collapseButton = new JButton();
		ImageIcons.setExpandIcon(collapseButton, true);
		collapseButton.addActionListener(e -> {
			expandedGroups.remove(glamourIndex + "_" + groupNum);
			rebuildDetailsPanel();
		});
		return collapseButton;
	}

	private void updateSingleColor(int glamourIndex, int colorIdx, short newColor)
	{
		clientThread.invokeLater(() -> {
			plate.updateGlamourColor(glamourer, glamourIndex, colorIdx, newColor);
			SwingUtilities.invokeLater(this::rebuildDetailsPanel);
		});
	}

	private void updateGroupColors(int glamourIndex, ColorGroup group, short newColor)
	{
		clientThread.invokeLater(() -> {
			List<int[]> colorUpdates = new ArrayList<>();
			for (int i = 0; i < group.getColorIndices().size(); i++)
			{
				int colorIdx = group.getColorIndices().get(i);
				short adjustedColor = group.calculateNewColor(newColor, i);
				colorUpdates.add(new int[]{colorIdx, adjustedColor});
			}
			plate.updateGlamourColors(glamourer, glamourIndex, colorUpdates);
			SwingUtilities.invokeLater(this::rebuildDetailsPanel);
		});
	}

	private void revertGroupColors(int glamourIndex, ColorGroup group, List<ColorReplacement> groupReplacements)
	{
		clientThread.invokeLater(() -> {
			List<int[]> colorUpdates = new ArrayList<>();
			List<Integer> indices = group.getColorIndices();
			for (int i = 0; i < indices.size(); i++)
			{
				ColorReplacement pair = groupReplacements.get(i);
				colorUpdates.add(new int[]{indices.get(i), pair.getOriginal()});
			}
			plate.updateGlamourColors(glamourer, glamourIndex, colorUpdates);
			SwingUtilities.invokeLater(this::rebuildDetailsPanel);
		});
	}

}
