package de.knospenraucher.mczombies.network;

import de.knospenraucher.mczombies.MCZombies;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → Server: der Spieler will mit der Waffe in der Hand schießen oder nachladen.
 * Der Server prüft Feuerrate, Munition usw. selbst.
 *
 * @param reload true = nachladen (Taste R), false = schießen (Rechtsklick)
 */
public record GunActionPayload(boolean reload) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<GunActionPayload> TYPE =
			new CustomPacketPayload.Type<>(MCZombies.id("gun_action"));

	public static final StreamCodec<ByteBuf, GunActionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, GunActionPayload::reload,
			GunActionPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
