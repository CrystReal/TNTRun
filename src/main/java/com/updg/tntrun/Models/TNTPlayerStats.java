package com.updg.tntrun.Models;

/**
 * Created by Alex
 * Date: 15.12.13  13:40
 */
public class TNTPlayerStats {
    private boolean winner = false;
    private int position = 1;
    private long inGameTime = 0;

    public boolean isWinner() {
        return winner;
    }

    public void setWinner(boolean winner) {
        this.winner = winner;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public long getInGameTime() {
        return inGameTime;
    }

    public void setInGameTime(long inGameTime) {
        this.inGameTime = inGameTime;
    }
}
