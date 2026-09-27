package de.knospenraucher.mczombies.weapon;

/**
 * Wo ein Treffer einen Zombie erwischt hat (wie die Trefferzonen in Black Ops III).
 * Die Zone bestimmt den Schadensfaktor der Waffe und die Punkte für den Kill.
 */
public enum HitZone {
	HEAD,
	NECK,
	TORSO_UPPER,
	TORSO_LOWER,
	ARM,
	LEG,
	/** Keine Stelle: Explosion, Blitz, Druckwelle, Electric Cherry. */
	NONE
}
