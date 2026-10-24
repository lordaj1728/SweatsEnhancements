package net.minecraft.world;

import net.minecraft.scoreboard.Scoreboard;

/**
 * Stand-in for the world, so the scoreboard's state machine can be run outside the game.
 *
 * It exists for one method: the panel reaches the scoreboard through the world, and the display
 * slot it empties is what has to survive a frame. Nothing else about a world is needed to test that.
 */
public class World {

    private final Scoreboard scoreboard;

    public World(Scoreboard scoreboard) {
        this.scoreboard = scoreboard;
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }
}
