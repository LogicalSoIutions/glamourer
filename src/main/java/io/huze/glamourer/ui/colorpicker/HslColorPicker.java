package io.huze.glamourer.ui.colorpicker;

import io.huze.glamourer.color.Colors;
import io.huze.glamourer.ui.ImageIcons;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import net.runelite.api.JagexColor;

public class HslColorPicker extends JPanel
{
	private final JSlider hueSlider;
	private final JSlider satSlider;
	private final JSlider lumSlider;
	private final JTextField hueText;
	private final JTextField satText;
	private final JTextField lumText;
	private final JLabel colorPreview = new JLabel();
	private final JTextField hslField = new JTextField("0", 6);

	public HslColorPicker(final short original, final short start)
	{
		setLayout(new BorderLayout(10, 10));

		hueSlider = createGradientSlider(Colors.MAX_HUE, getHueSpectrum());
		satSlider = createGradientSlider(Colors.MAX_SAT, new Color[0]);
		lumSlider = createGradientSlider(Colors.MAX_LUM, new Color[0]);
		updateGradients();
		hueText = createColorTextField(Colors.MAX_HUE, hueSlider);
		satText = createColorTextField(Colors.MAX_SAT, satSlider);
		lumText = createColorTextField(Colors.MAX_LUM, lumSlider);

		JPanel previewPanel = new JPanel();
		previewPanel.setLayout(new BoxLayout(previewPanel, BoxLayout.Y_AXIS));
		colorPreview.setPreferredSize(new Dimension(100, 50));
		colorPreview.setMaximumSize(new Dimension(100, 50));
		colorPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		colorPreview.setOpaque(true);
		previewPanel.add(colorPreview);

		previewPanel.add(createColorButton(start));
		previewPanel.add(createColorButton(original));
		previewPanel.add(Box.createVerticalGlue());

		JPanel controlsPanel = new JPanel();
		controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
		controlsPanel.add(createSliderRow(hueSlider, hueText));
		controlsPanel.add(createSliderRow(satSlider, satText));
		controlsPanel.add(createSliderRow(lumSlider, lumText));
		controlsPanel.add(Box.createVerticalStrut(5));
		controlsPanel.add(createExportRow(hslField));

		add(previewPanel, BorderLayout.WEST);
		add(controlsPanel, BorderLayout.CENTER);

		hslField.addActionListener(e -> {
			try
			{
				setColor(Short.parseShort(hslField.getText()));
			}
			catch (NumberFormatException ex)
			{
				hslField.setText(String.valueOf(getColor()));
			}
		});

		setColor(start);

		hueSlider.addChangeListener(this::onUpdate);
		satSlider.addChangeListener(this::onUpdate);
		lumSlider.addChangeListener(this::onUpdate);
		onUpdate(null);
	}

	public void setColor(short hsl)
	{
		hueSlider.setValue(JagexColor.unpackHue(hsl));
		satSlider.setValue(JagexColor.unpackSaturation(hsl));
		lumSlider.setValue(JagexColor.unpackLuminance(hsl));
	}

	public short getColor()
	{
		return JagexColor.packHSL(hueSlider.getValue(), satSlider.getValue(), lumSlider.getValue());
	}

	private void onUpdate(ChangeEvent e)
	{
		short hsl = getColor();
		Color c = Colors.hslToColor(hsl);
		colorPreview.setBackground(c);

		if (e == null || e.getSource() != hslField)
		{
			hslField.setText(String.valueOf(hsl));
		}

		if (e == null || e.getSource() instanceof JSlider)
		{
			hueText.setText(String.valueOf(hueSlider.getValue()));
			satText.setText(String.valueOf(satSlider.getValue()));
			lumText.setText(String.valueOf(lumSlider.getValue()));
		}

		updateGradients();
		repaint();
	}

	private static Color[] getHueSpectrum()
	{
		Color[] colors = new Color[Colors.MAX_HUE + 1];
		for (int i = 0; i <= Colors.MAX_HUE; i++)
		{
			colors[i] = Colors.hslToColor(i, Colors.MAX_SAT, Colors.MAX_LUM / 2);
		}
		return colors;
	}

	private static Color[] getSatGradient(int h, int l)
	{
		Color[] colors = new Color[Colors.MAX_SAT + 1];
		for (int i = 0; i <= Colors.MAX_SAT; i++)
		{
			colors[i] = Colors.hslToColor(h, i, l);
		}
		return colors;
	}

	private static Color[] getLumGradient(int h, int s)
	{
		Color[] colors = new Color[Colors.MAX_LUM + 1];
		for (int i = 0; i <= Colors.MAX_LUM; i++)
		{
			colors[i] = Colors.hslToColor(h, s, i);
		}
		return colors;
	}

	private void updateGradients()
	{
		((ColorSliderUI) satSlider.getUI()).setColors(getSatGradient(hueSlider.getValue(), lumSlider.getValue()));
		((ColorSliderUI) lumSlider.getUI()).setColors(getLumGradient(hueSlider.getValue(), satSlider.getValue()));
	}

	private JTextField createColorTextField(int max, JSlider slider)
	{
		JTextField field = new JTextField("0", 3);
		field.setHorizontalAlignment(JTextField.CENTER);
		field.addActionListener(e -> {
			try
			{
				int val = Integer.parseInt(field.getText());
				val = Math.max(0, Math.min(max, val));
				field.setText(String.valueOf(val));
				slider.setValue(val); // This triggers the Slider's ChangeListener
			}
			catch (NumberFormatException ex)
			{
				// Revert to current slider value if input is invalid
				field.setText(String.valueOf(slider.getValue()));
			}
		});
		return field;
	}

	private JSlider createGradientSlider(int max, Color[] colors)
	{
		JSlider slider = new JSlider(0, max);
		slider.setUI(new ColorSliderUI(slider, colors));
		return slider;
	}

	private JPanel createSliderRow(JSlider slider, JTextField field)
	{
		JPanel row = new JPanel(new BorderLayout(5, 0));
		row.add(slider, BorderLayout.CENTER);
		row.add(field, BorderLayout.EAST);
		return row;
	}

	private JPanel createExportRow(JTextField field)
	{
		JPanel panel = new JPanel();
		panel.add(field, BorderLayout.CENTER);

		JButton copyBtn = new JButton();
		ImageIcons.setCopyIcon(copyBtn);
		copyBtn.addActionListener(e -> copyToClipboard(field.getText()));
		panel.add(copyBtn, BorderLayout.EAST);

		return panel;
	}

	private void copyToClipboard(String text)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard()
			.setContents(new StringSelection(text), null);
	}

	private JButton createColorButton(short hsl)
	{
		JButton btn = new JButton();
		btn.setPreferredSize(new Dimension(100, 25));
		btn.setMaximumSize(new Dimension(100, 25));
		btn.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		btn.setOpaque(true);
		btn.setBackground(Colors.hslToColor(hsl));
		btn.addActionListener(e -> setColor(hsl));
		return btn;
	}
}