package de.knospenraucher.mczombies.weapon;

import de.knospenraucher.mczombies.MCZombies;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/** Registriert die Schusswaffen der Mod. */
public final class ModItems {
	public static final List<GunItem> GUNS = new ArrayList<>();

	static {
		for (GunCatalog.Def def : GunCatalog.all()) {
			gun(def.id(), def.category());
		}
	}

	private ModItems() {
	}

	private static GunItem gun(String name, String category) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MCZombies.id(name));
		Item.Properties properties = new Item.Properties()
				.setId(key)
				.stacksTo(1)
				.component(DataComponents.UNBREAKABLE, Unit.INSTANCE);
		GunItem item = Registry.register(BuiltInRegistries.ITEM, key, new GunItem(name, category, properties));
		GUNS.add(item);
		return item;
	}

	/** Lädt die Klasse (registriert damit alle Waffen) und fügt sie dem Kampf-Tab hinzu. */
	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			for (GunItem gun : GUNS) {
				output.accept(gun);
			}
		});
	}
}
