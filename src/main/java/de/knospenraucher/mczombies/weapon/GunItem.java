package de.knospenraucher.mczombies.weapon;

import de.knospenraucher.mczombies.config.ZombiesConfig;
import de.knospenraucher.mczombies.config.ZombiesConfig.GunStats;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * Eine Schusswaffe. Das Item selbst ist zustandslos; Munition und Aufrüstung
 * stehen in den Custom-Daten des ItemStacks:
 * <ul>
 *   <li>{@code mag} – Schuss im Magazin</li>
 *   <li>{@code reserve} – Reservemunition</li>
 *   <li>{@code upgraded} – an der Aufrüst-Maschine verbessert</li>
 *   <li>{@code reloading} – lädt gerade nach (nur für die Anzeige)</li>
 * </ul>
 * Fehlen die Werte (z. B. frisch aus dem Kreativinventar), ist die Waffe voll geladen.
 * Geschossen wird über {@link GunManager}; der Client schickt dafür ein Paket.
 */
public class GunItem extends Item {
	private static final String MAG = "mag";
	private static final String RESERVE = "reserve";
	private static final String UPGRADED = "upgraded";
	private static final String RELOADING = "reloading";

	/** Name der Waffe in der Config, z. B. "pistol". */
	private final String key;

	public GunItem(String key, Properties properties) {
		super(properties);
		this.key = key;
	}

	public String key() {
		return key;
	}

	public GunStats stats() {
		return ZombiesConfig.get().gun(key);
	}

	// ---------------------------------------------------------------- Werte mit Aufrüstung

	public double damage(ItemStack stack) {
		double damage = stats().damage;
		return isUpgraded(stack) ? damage * ZombiesConfig.get().upgradeDamageMultiplier : damage;
	}

	public int magazineSize(ItemStack stack) {
		return scaled(stack, stats().magazine);
	}

	public int maxReserve(ItemStack stack) {
		return scaled(stack, stats().reserve);
	}

	private static int scaled(ItemStack stack, int value) {
		return isUpgraded(stack) ? (int) Math.round(value * ZombiesConfig.get().upgradeAmmoMultiplier) : value;
	}

	// ---------------------------------------------------------------- Munition (Custom-Daten)

	private static CompoundTag tag(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null ? data.copyTag() : new CompoundTag();
	}

	public int getMag(ItemStack stack) {
		return tag(stack).getIntOr(MAG, magazineSize(stack));
	}

	public int getReserve(ItemStack stack) {
		return tag(stack).getIntOr(RESERVE, maxReserve(stack));
	}

	public static boolean isUpgraded(ItemStack stack) {
		return tag(stack).getBooleanOr(UPGRADED, false);
	}

	public static boolean isReloading(ItemStack stack) {
		return tag(stack).getBooleanOr(RELOADING, false);
	}

	public void setAmmo(ItemStack stack, int mag, int reserve) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			tag.putInt(MAG, mag);
			tag.putInt(RESERVE, reserve);
		});
	}

	public static void setReloading(ItemStack stack, boolean reloading) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(RELOADING, reloading));
	}

	/** Magazin und Reserve auffüllen (Munition kaufen). */
	public void refill(ItemStack stack) {
		setAmmo(stack, magazineSize(stack), maxReserve(stack));
	}

	/** Markiert die Waffe als aufgerüstet (Munition danach mit {@link #refill} auffüllen). */
	public static void setUpgraded(ItemStack stack) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(UPGRADED, true));
	}

	// ---------------------------------------------------------------- Item-Verhalten

	/** Rechtsklick macht vanilla-seitig nichts; der Schuss läuft über ein eigenes Paket. */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		return InteractionResult.FAIL;
	}

	/** Der Balken unter dem Item zeigt den Füllstand des Magazins. */
	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		int size = Math.max(1, magazineSize(stack));
		return Math.round(13.0F * Math.min(getMag(stack), size) / size);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return isReloading(stack) ? 0xAAAAAA : isUpgraded(stack) ? 0xC060FF : 0xFFD24A;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return isUpgraded(stack) || super.isFoil(stack);
	}

	/**
	 * Fabric-Hook: kein "Neu-Ausrüsten"-Wackeln der Waffe, nur weil sich die Munition ändert.
	 * (Ohne @Override, damit der Code auch ohne den Hook kompiliert.)
	 */
	public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
		return oldStack.getItem() != newStack.getItem();
	}
}
