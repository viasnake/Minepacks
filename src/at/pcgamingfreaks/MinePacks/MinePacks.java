/*
 *   Copyright (C) 2014-2016 GeorgH93
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package at.pcgamingfreaks.MinePacks;

import at.pcgamingfreaks.MinePacks.Database.Config;
import at.pcgamingfreaks.MinePacks.Database.Database;
import at.pcgamingfreaks.MinePacks.Database.Language;

import net.gravitydevelopment.Updater.Bukkit_Updater;
import net.gravitydevelopment.Updater.UpdateType;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.logging.Logger;

public class MinePacks extends JavaPlugin
{
	private static MinePacks instance = null;

	public Logger log;
	public Config config;
	public Language lang;
	public Database DB;

	public HashMap<Player, Long> cooldowns = new HashMap<>();

	public static String BackpackTitle;
	public String Message_InvalidBackpack;

	public static MinePacks getInstance()
	{
		return instance;
	}

	@Override
	public void onEnable()
	{
		log = getLogger();

		String name = Bukkit.getServer().getClass().getPackage().getName();
		String[] version = name.substring(name.lastIndexOf('.') + 2).split("_");
		if((version[0].equals("1") && Integer.valueOf(version[1]) > 9) || Integer.valueOf(version[0]) > 1)
		{
			MinePacks.getInstance().warnOnVersionIncompatibility(version[0] + "." + version[1]);
			this.setEnabled(false);
			return;
		}

		config = new Config(this);
		lang = new Language(this);

		instance = this;

		lang.load(config.getLanguage(), config.getLanguageUpdateMode());
		DB = Database.getDatabase(this);
		getCommand("backpack").setExecutor(new OnCommand(this));
		getServer().getPluginManager().registerEvents(new EventListener(this), this);

		if(config.getFullInvCollect())
		{
			(new ItemsCollector(this)).runTaskTimer(this, config.getFullInvCheckInterval(), config.getFullInvCheckInterval());
		}

		BackpackTitle = config.getBPTitle();
		Message_InvalidBackpack = lang.getTranslated("Ingame.InvalidBackpack");
		getServer().getServicesManager().register(MinePacks.class, this, this, ServicePriority.Normal);
		log.info(lang.get("Console.Enabled"));
	}

	@Override
	public void onDisable()
	{
		getServer().getScheduler().cancelTasks(this);
		DB.close();
		if(config.getAutoUpdate())
		{
			new Bukkit_Updater(this, 83445, this.getFile(), UpdateType.DEFAULT, true);
		}
		log.info(lang.get("Console.Disabled"));
	}

	public void warnOnVersionIncompatibility(String version)
	{
		log.warning(ChatColor.RED + "################################");
		log.warning(ChatColor.RED + String.format(MinePacks.getInstance().lang.getTranslated("Console.MinecraftVersionNotCompatible"), version));
		log.warning(ChatColor.RED + "################################");
		try
		{
			Thread.sleep(5000L);
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	public void openBackpack(final Player opener, OfflinePlayer owner, final boolean editable)
	{
		if(owner == null)
		{
			return;
		}
		DB.getBackpack(owner, new Database.Callback<Backpack>()
		{
			@Override
			public void onResult(Backpack backpack)
			{
				openBackpack(opener, backpack, editable);
			}

			@Override
			public void onFail() {}
		});
	}

	public void openBackpack(Player opener, Backpack backpack, boolean editable)
	{
		if(backpack == null)
		{
			opener.sendMessage(Message_InvalidBackpack);
			return;
		}
		backpack.open(opener, editable);
	}

	public int getBackpackPermSize(Player player)
	{
		for(int i = 9; i > 1; i--)
		{
			if(player.hasPermission("backpack.size." + i))
			{
				return i * 9;
			}
		}
		return 9;
	}
}