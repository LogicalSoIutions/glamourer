package io.huze.glamourer.plate;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.huze.glamourer.Config;
import io.huze.glamourer.glam.Glamourer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

@Slf4j
@Singleton
public class PlateManager
{
	private static final String PLATES_KEY = "userPlates";
	private static final Type PLATES_LIST_TYPE = new TypeToken<List<PlateData>>()
	{
	}.getType();

	private final ConfigManager configManager;
	private final Gson gson;
	private final Glamourer glamourer;

	@Getter
	private final List<Plate> plates = new ArrayList<>();
	@Setter
	private Consumer<Void> onPlatesChanged;

	@Inject
	public PlateManager(ConfigManager configManager, Gson gson, Glamourer glamourer)
	{
		this.configManager = configManager;
		this.gson = gson;
		this.glamourer = glamourer;
	}

	public void loadPlates()
	{
		plates.clear();
		String json = configManager.getConfiguration(Config.GROUP, PLATES_KEY);
		if (json != null && !json.isEmpty())
		{
			try
			{
				List<PlateData> dataList = gson.fromJson(json, PLATES_LIST_TYPE);
				if (dataList != null)
				{
					for (PlateData data : dataList)
					{
						Plate plate = Plate.fromData(data, glamourer);
						plate.setOnChange(this::savePlates);
						plates.add(plate);
					}
					log.info("Loaded {} plates", plates.size());
				}
			}
			catch (Throwable e)
			{
				log.error("Failed to load plates", e);
			}
		}
	}

	public void savePlates()
	{
		try
		{
			List<PlateData> dataList = plates.stream()
				.map(Plate::getData)
				.collect(Collectors.toList());
			String json = gson.toJson(dataList, PLATES_LIST_TYPE);
			configManager.setConfiguration(Config.GROUP, PLATES_KEY, json);
			log.debug("Saved {} plates", plates.size());
		}
		catch (Exception e)
		{
			log.error("Failed to save plates", e);
		}
	}

	public void createPlate()
	{
		Plate plate = Plate.newEmptyPlate();
		plate.setOnChange(this::savePlates);
		plates.add(plate);
		savePlates();
		notifyPlatesChanged();
	}

	public void deletePlate(String id)
	{
		plates.stream()
			.filter(plate -> plate.getId().equals(id))
			.findFirst()
			.ifPresent(plate -> {
				plate.revertAll(glamourer);
				plates.remove(plate);
				savePlates();
				notifyPlatesChanged();
			});
	}

	public void movePlate(int fromIndex, int toIndex)
	{
		int size = plates.size();
		if (fromIndex < 0 || fromIndex >= size || toIndex < 0 || toIndex >= size || fromIndex == toIndex)
		{
			return;
		}
		Plate plate = plates.remove(fromIndex);
		plates.add(toIndex, plate);
		savePlates();
		notifyPlatesChanged();
	}

	public void applyAllPlates()
	{
		for (Plate plate : plates)
		{
			if (plate.isEnabled())
			{
				plate.applyAll(glamourer);
			}
		}
	}

	public void revertAllPlates()
	{
		for (Plate plate : plates)
		{
			plate.revertAll(glamourer);
		}
	}

	public Set<Integer> getExistingItemIds()
	{
		Set<Integer> ids = new HashSet<>();
		for (Plate plate : plates)
		{
			for (var glam : plate.getGlamours())
			{
				ids.addAll(glam.getItemIds());
			}
		}
		return ids;
	}

	private void notifyPlatesChanged()
	{
		if (onPlatesChanged != null)
		{
			onPlatesChanged.accept(null);
		}
	}
}
