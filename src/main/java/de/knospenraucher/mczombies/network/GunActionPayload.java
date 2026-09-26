package de.knospenraucher.mczombies.network;

import de.knospenraucher.mczombies.MCZombies;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → Server: der Spieler will schießen, nachladen oder mit dem Messer zustechen.
 * Der Server prüft Feuerrate, Munition usw. selbst.
 *
 * @param action {@link #SHOOT} (Linksklick), {@link #RELOAD} (Taste R), {@link #MELEE} (Taste V) oder Zielen (Rechtsklick)
 */
public record GunActionPayload(int action) implements CustomPacketPayload {
	public static final int SHOOT = 0;
	public static final int RELOAD = 1;
	public static final int MELEE = 2;
	/** Messer mit Ausfallschritt (der Spieler lief auf einen Zombie zu). */
	public static final int MELEE_LUNGE = 3;
	/** Rechtsklick gedrückt bzw. losgelassen: Zielen über Kimme und Korn. */
	public static final int AIM_START = 4;
	public static final int AIM_STOP = 5;

	public static final CustomPacketPayload.Type<GunActionPayload> TYPE =
			new CustomPacketPayload.Type<>(MCZombies.id("gun_action"));

	public static final StreamCodec<ByteBuf, GunActionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, GunActionPayload::action,
			GunActionPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
