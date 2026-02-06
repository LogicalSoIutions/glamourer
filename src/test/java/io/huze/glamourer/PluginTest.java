package io.huze.glamourer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginTest
{
	private static final Logger log = LoggerFactory.getLogger(PluginTest.class);

	public static void main(String[] args)
	{
		try
		{
			ExternalPluginManager.loadBuiltin(Plugin.class);
			RuneLite.main(args);
		}
		catch (Throwable t)
		{
			log.error(t.toString());
			t.printStackTrace(System.err);
		}
	}
}