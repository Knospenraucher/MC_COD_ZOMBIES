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

	/** Name der Waffe in der Config, z. B. "kn_44". */
	private final String key;
	/** Waffenklasse (pistol, smg, rifle, shotgun, lmg, sniper, launcher, wonder), bestimmt u. a. den Klang. */
	private final String category;

	public GunItem(String key, String category, Properties properties) {
		super(properties);
		this.key = key;
		this.category = category;
	}

	public String key() {
		return key;
	}

	public String category() {
		return category;
	}

	/** Übersetzungsschlüssel des Namens nach Pack-a-Punch. */
	public String upgradedNameKey() {
		return getDescriptionId() + ".upgraded";
	}

	public GunStats stats() {
		return ZombiesConfig.get().gun(key);
	}

	// ---------------------------------------------------------------- Werte mit Aufrüstung

	/** Schaden auf kurze Entfernung (mit Pack-a-Punch den aufgerüsteten Wert). */
	public double damage(ItemStack stack) {
		GunStats s = stats();
		if (!isUpgraded(stack)) {
			return s.damage;
		}
		return s.upgradedDamage > 0 ? s.upgradedDamage : s.damage * ZombiesConfig.get().upgradeDamageMultiplier;
	}

	/** Schaden auf große Entfernung (ab minDamageRange). */
	public double damageMin(ItemStack stack) {
		GunStats s = stats();
		double min = s.damageMin > 0 ? s.damageMin : s.damage;
		if (!isUpgraded(stack)) {
			return min;
		}
		if (s.upgradedDamageMin > 0) {
			return s.upgradedDamageMin;
		}
		// Gleiches Verhältnis wie ohne Pack-a-Punch.
		return s.damage > 0 ? damage(stack) * min / s.damage : damage(stack);
	}

	/** Schaden auf diese Entfernung: bis maxDamageRange voll, ab minDamageRange damageMin, dazwischen gleitend. */
	public double damageAt(ItemStack stack, double distance) {
		GunStats s = stats();
		double max = damage(stack);
		double min = damageMin(stack);
		double near = s.maxDamageRange;
		double far = s.minDamageRange;
		if (far <= 0 || distance <= near) {
			return max;
		}
		if (distance >= far || far <= near) {
			return min;
		}
		return max + (min - max) * (distance - near) / (far - near);
	}

	/** Schadensfaktor der Trefferzone. */
	public double zoneMultiplier(ItemStack stack, HitZone zone) {
		GunStats s = stats();
		return switch (zone) {
			case HEAD -> {
				double head = s.headshotMultiplier > 0 ? s.headshotMultiplier : 1.0;
				yield isUpgraded(stack) && s.upgradedHeadshotMultiplier > 0 ? s.upgradedHeadshotMultiplier : head;
			}
			case NECK -> orOne(s.neckMultiplier);
			case TORSO_UPPER, TORSO_LOWER -> orOne(s.torsoMultiplier);
			case ARM, LEG -> orOne(s.limbMultiplier);
			case NONE -> 1.0;
		};
	}

	/** Wie viele Zombies eine Kugel hintereinander trifft. */
	public int penetration(ItemStack stack) {
		GunStats s = stats();
		int base = Math.max(1, s.penetration);
		return isUpgraded(stack) && s.upgradedPenetration > 0 ? s.upgradedPenetration : base;
	}

	/** Anteil des Schadens, der nach jedem durchschossenen Zombie bleibt. */
	public double penetrationFactor(ItemStack stack) {
		GunStats s = stats();
		double base = s.penetrationDamageFactor > 0 ? s.penetrationDamageFactor : 1.0;
		return isUpgraded(stack) && s.upgradedPenetrationDamageFactor > 0 ? s.upgradedPenetrationDamageFactor : base;
	}

	/** Ticks von Schuss zu Schuss (bei Feuerstößen von Stoß zu Stoß). */
	public double fireInterval(ItemStack stack) {
		GunStats s = stats();
		double base = s.fireRateTicks > 0 ? s.fireRateTicks : 1.0;
		return isUpgraded(stack) && s.upgradedFireRateTicks > 0 ? s.upgradedFireRateTicks : base;
	}

	/** Ticks zwischen den Schüssen eines Feuerstoßes. */
	public double burstInterval() {
		return stats().burstIntervalTicks > 0 ? stats().burstIntervalTicks : 1.0;
	}

	public int reloadTicks(ItemStack stack) {
		GunStats s = stats();
		return isUpgraded(stack) && s.upgradedReloadTicks > 0 ? s.upgradedReloadTicks : s.reloadTicks;
	}

	private static double orOne(double value) {
		return value > 0 ? value : 1.0;
	}

	public int magazineSize(ItemStack stack) {
		return scaled(stack, stats().magazine, stats().upgradedMagazine);
	}

	public int maxReserve(ItemStack stack) {
		return scaled(stack, stats().reserve, stats().upgradedReserve);
	}

	public double explosionRadius(ItemStack stack) {
		GunStats s = stats();
		return isUpgraded(stack) && s.upgradedExplosionRadius > 0 ? s.upgradedExplosionRadius : s.explosionRadius;
	}

	private static int scaled(ItemStack stack, int value, int upgraded) {
		if (!isUpgraded(stack)) {
			return value;
		}
		return upgraded > 0 ? upgraded : (int) Math.round(value * ZombiesConfig.get().upgradeAmmoMultiplier);
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
	 */
	@Override
	public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
		return oldStack.getItem() != newStack.getItem();
	}
}
