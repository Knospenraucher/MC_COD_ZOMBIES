package de.knospenraucher.mczombies.game;

import net.minecraft.world.level.GameType;

/** Punktestand und Statistik eines teilnehmenden Spielers. */
public class PlayerData {
	public String name;
	public int points;
	public int kills;
	public int headshots;
	public int downs;
	/** true, solange der Spieler in der aktuellen Runde ausgeschaltet ist. */
	public boolean down;
	/** Spielmodus vor Spielbeginn, wird bei Spielende wiederhergestellt. */
	public final GameType previousGameType;

	public PlayerData(String name, int startingPoints, GameType previousGameType) {
		this.name = name;
		this.points = startingPoints;
		this.previousGameType = previousGameType;
	}
}
