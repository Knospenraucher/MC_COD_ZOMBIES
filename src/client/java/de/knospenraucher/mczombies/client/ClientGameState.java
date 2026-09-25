package de.knospenraucher.mczombies.client;

import de.knospenraucher.mczombies.game.GameState;
import de.knospenraucher.mczombies.network.HudSyncPayload;

import java.util.List;

/** Letzter vom Server empfangener Spielzustand (nur Anzeige, keine Spiellogik). */
public final class ClientGameState {
	private static GameState state = GameState.IDLE;
	private static int round;
	private static int zombiesLeft;
	private static int countdown;
	private static List<HudSyncPayload.Entry> players = List.of();

	private ClientGameState() {
	}

	public static void update(HudSyncPayload payload) {
		state = GameState.byOrdinal(payload.state());
		round = payload.round();
		zombiesLeft = payload.zombiesLeft();
		countdown = payload.countdown();
		players = List.copyOf(payload.players());
	}

	public static void clear() {
		state = GameState.IDLE;
		round = 0;
		zombiesLeft = 0;
		countdown = 0;
		players = List.of();
	}

	public static GameState state() {
		return state;
	}

	public static int round() {
		return round;
	}

	public static int zombiesLeft() {
		return zombiesLeft;
	}

	public static int countdown() {
		return countdown;
	}

	public static List<HudSyncPayload.Entry> players() {
		return players;
	}
}
