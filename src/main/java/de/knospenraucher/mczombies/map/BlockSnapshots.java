package de.knospenraucher.mczombies.map;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import de.knospenraucher.mczombies.map.MapData.BlockSnapshot;
import de.knospenraucher.mczombies.map.MapData.Pos;
import de.knospenraucher.mczombies.map.MapData.Region;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Speichert Blöcke eines Bereichs und stellt sie später wieder her (Türen, Fenster). */
public final class BlockSnapshots {
	/** Schutz vor versehentlich riesigen Bereichen. */
	public static final int MAX_BLOCKS = 512;

	private BlockSnapshots() {
	}

	/** Setzt min/max einer Region aus zwei beliebigen Ecken. */
	public static void setCorners(Region region, BlockPos a, BlockPos b) {
		region.min = new Pos(new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ())));
		region.max = new Pos(new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())));
	}

	public static int volume(BlockPos a, BlockPos b) {
		return (Math.abs(a.getX() - b.getX()) + 1) * (Math.abs(a.getY() - b.getY()) + 1) * (Math.abs(a.getZ() - b.getZ()) + 1);
	}

	/** Liest alle Nicht-Luft-Blöcke der Region aus der Welt. */
	public static List<BlockSnapshot> capture(ServerLevel level, Region region) {
		List<BlockSnapshot> result = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(region.min.toBlockPos(), region.max.toBlockPos())) {
			BlockState state = level.getBlockState(pos);
			if (state.isAir()) {
				continue;
			}
			JsonElement json = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, state).result().orElse(null);
			if (json == null) {
				continue;
			}
			BlockSnapshot snapshot = new BlockSnapshot();
			snapshot.pos = new Pos(pos.immutable());
			snapshot.state = json;
			result.add(snapshot);
		}
		return result;
	}

	public static BlockState decode(BlockSnapshot snapshot) {
		return BlockState.CODEC.parse(JsonOps.INSTANCE, snapshot.state).result().orElse(null);
	}

	/** Setzt den gespeicherten Block wieder in die Welt. */
	public static void restore(ServerLevel level, BlockSnapshot snapshot) {
		BlockState state = decode(snapshot);
		if (state != null) {
			level.setBlock(snapshot.pos.toBlockPos(), state, 3);
		}
	}

	/** true, wenn an der Position gerade (irgendein) Block steht. */
	public static boolean isPresent(ServerLevel level, BlockSnapshot snapshot) {
		return !level.getBlockState(snapshot.pos.toBlockPos()).isAir();
	}
}
