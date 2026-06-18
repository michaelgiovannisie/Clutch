package com.zipcode.worldcuptracker.model;

import lombok.Getter;
import lombok.Setter;

/**
 * A computed (not persisted) group-table row: a team's record within its
 * World Cup group, derived on the fly from finished Match rows. Standard
 * football points: win = 3, draw = 1, loss = 0.
 */
@Getter
@Setter
public class TeamStanding {

    private Team team;
    private int played = 0;
    private int won = 0;
    private int drawn = 0;
    private int lost = 0;
    private int goalsFor = 0;
    private int goalsAgainst = 0;

    public TeamStanding(Team team) {
        this.team = team;
    }

    public int getGoalDifference() {
        return goalsFor - goalsAgainst;
    }

    public int getPoints() {
        return won * 3 + drawn;
    }

    public void recordResult(int scored, int conceded) {
        played++;
        goalsFor += scored;
        goalsAgainst += conceded;
        if (scored > conceded) won++;
        else if (scored == conceded) drawn++;
        else lost++;
    }
}
