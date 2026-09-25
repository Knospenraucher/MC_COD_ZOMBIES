package de.knospenraucher.mczombies.network;

import de.knospenraucher.mczombies.MCZombies;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Server → Client: aktueller Spielzustand für das HUD.
 *
 * @param state        {@link de.knospenraucher.mczombies.game.GameState#ordinal()}
 * @param round        aktuelle Runde (0 = noch nicht gestartet)
 * @param zombiesLeft  noch zu tötende Zombies dieser Runde
 * @param countdown    Sekunden bis zur nächsten Runde (nur in der Pause)
 * @param players      Punktestand aller Teilnehmer, absteigend sortiert
 */
public record HudSyncPayload(int state, int round, int zombiesLeft, int countdown, List<Entry> players)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<HudSyncPayload> TYPE =
			new CustomPacketPayload.Type<>(MCZombies.id("hud_sync"));

	/** Eine Zeile der Punkteliste. */
	public record Entry(String name, int points, boolean down) {
		public static final StreamCodec<ByteBuf, Entry> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Entry::name,
				ByteBufCodecs.VAR_INT, Entry::points,
				ByteBufCodecs.BOOL, Entry::down,
				Entry::new);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, HudSyncPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, HudSyncPayload::state,
			ByteBufCodecs.VAR_INT, HudSyncPayload::round,
			ByteBufCodecs.VAR_INT, HudSyncPayload::zombiesLeft,
			ByteBufCodecs.VAR_INT, HudSyncPayload::countdown,
			Entry.CODEC.apply(ByteBufCodecs.list()), HudSyncPayload::players,
			HudSyncPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
