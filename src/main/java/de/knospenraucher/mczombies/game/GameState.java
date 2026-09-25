package de.knospenraucher.mczombies.game;

/** Phasen eines Spiels. Die Reihenfolge (ordinal) wird auch an den Client gesendet. */
public enum GameState {
	/** Kein Spiel aktiv. */
	IDLE,
	/** Countdown vor der ersten bzw. zwischen zwei Runden. */
	INTERMISSION,
	/** Runde läuft, Zombies spawnen. */
	ACTIVE,
	/** Alle Spieler sind down; Anzeige bleibt kurz stehen, dann wird zurückgesetzt. */
	GAME_OVER;

	public static GameState byOrdinal(int ordinal) {
		GameState[] values = values();
		return ordinal >= 0 && ordinal < values.length ? values[ordinal] : IDLE;
	}
}
