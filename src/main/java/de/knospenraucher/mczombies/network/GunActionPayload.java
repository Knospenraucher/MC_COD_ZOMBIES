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
 * @param action {@link #SHOOT} (Rechtsklick), {@link #RELOAD} (Taste R) oder {@link #MELEE} (Taste V)
 */
public record GunActionPayload(int action) implements CustomPacketPayload {
	public static final int SHOOT = 0;
	public static final int RELOAD = 1;
	public static final int MELEE = 2;

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
