package net.minecraft.client.multiplayer;

import net.minecraft.scoreboard.Scoreboard;

/** Stand-in for the client world, carrying the scoreboard the panel reads. */
public class WorldClient extends net.minecraft.world.World {

    public WorldClient(Scoreboard scoreboard) {
        super(scoreboard);
    }
}
