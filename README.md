# Glamourer

The RuneLite plugin for changing appearances of Old School RuneScape items. Customize the colors of nearly any item for
fashion, accessibility, or just because you feel like it.

Unlike similar recoloring plugins that just overlay an image on top of the item's default icon, Glamourer modifies the
item composition at a deeper level which changes items in your inventory, equipment, and everywhere else it is visible.

## Features

- **Item Recoloring**: Change the colors of any item in the game and see it in your inventory, equipment, on the ground, etc.
- **Glamour Plates**: Organize multiple item recolors into glamour plates that can be enabled/disabled together
- **Color Groups**: Similar colors on an item are automatically grouped, allowing batch editing with a single picker
- **Import / Export**: Share glamour plates as JSON via the clipboard, making it easy to back up or share designs

## Usage

1. Open the Glamourer panel from the RuneLite sidebar
1. Click **+** button at the top right to create a new plate
1. Click **+ Add Item** to search for and add items to your plate
1. Click on a color swatch to open the color picker and adjust the HSL values

### Exporting a Plate

1. Hover over a plate row and click the **copy** button, or right-click the plate and select **Export**
2. The plate is serialized to JSON and copied to your clipboard
3. A confirmation dialog appears once the export succeeds

<!-- ![export_hover](PLACEHOLDER) -->

### Importing a Plate

1. Click the **import** button in the panel title bar
2. Paste the exported JSON into the dialog that appears and press OK
3. The imported plate is added to the bottom of your list and scrolled into view

If the JSON is invalid or cannot be parsed, an error dialog is shown and no plate is created. Imported plates receive a new ID so they never conflict with existing ones.

<!-- ![import_hover](PLACEHOLDER) -->
<!-- ![import_box](PLACEHOLDER) -->

## Examples

![dragon_zombie_axec](https://raw.githubusercontent.com/jhughes/glamourer/master/readme_assets/dragon_zombie_axe.gif)

## Contributing

I am not ready for any contributions. If you'd like to see something added, feel free to open a GitHub issue describing it.
