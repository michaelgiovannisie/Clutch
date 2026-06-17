package com.zipcode.worldcuptracker.service;

import com.zipcode.worldcuptracker.standings.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Builds the projected Round of 32 from group standings.
 * T3 slots show provisional "3rd of X/Y/Z/..." labels — never fabricated.
 */
@Service
public class BracketService {

    private static final String[][] SKELETON = {
        { "73", "2A", "2B" },
        { "74", "1E", "3:A,B,C,D,F" },
        { "75", "1F", "2C" },
        { "76", "1C", "2F" },
        { "77", "1I", "3:C,D,F,G,H" },
        { "78", "2E", "2I" },
        { "79", "1A", "3:C,E,F,H,I" },
        { "80", "1L", "3:E,H,I,J,K" },
        { "81", "1D", "3:B,E,F,I,J" },
        { "82", "1G", "3:A,E,H,I,J" },
        { "83", "2K", "2L" },
        { "84", "1H", "2J" },
        { "85", "1B", "3:E,F,G,I,J" },
        { "86", "1J", "2H" },
        { "87", "1K", "3:D,E,I,J,L" },
        { "88", "2D", "2G" },
    };

    private final StandingsService standingsService;

    public BracketService(StandingsService standingsService) {
        this.standingsService = standingsService;
    }

    public List<ProjectedMatchup> getProjectedR32() {
        List<GroupStandingView> groups = standingsService.getRankedGroups();

        Map<String, StandingRowView> winners   = new HashMap<>();
        Map<String, StandingRowView> runnersUp = new HashMap<>();
        for (GroupStandingView g : groups) {
            winners.put(g.id(), g.rows().get(0));
            runnersUp.put(g.id(), g.rows().get(1));
        }

        List<ProjectedMatchup> result = new ArrayList<>();
        for (String[] entry : SKELETON) {
            int matchNum = Integer.parseInt(entry[0]);
            String homeSlot = entry[1];
            String awaySlot = entry[2];

            String homeSlug = null, homeName = null, homeGroup = null, homeLabel = null;
            String awaySlug = null, awayName = null, awayGroup = null, awayLabel = null;

            if (homeSlot.startsWith("3:")) {
                homeLabel = "3rd of " + homeSlot.substring(2).replace(",", "/");
            } else {
                char p = homeSlot.charAt(0);
                String g = homeSlot.substring(1);
                StandingRowView t = p == '1' ? winners.get(g) : runnersUp.get(g);
                if (t != null) { homeSlug = t.slug(); homeName = t.name(); homeGroup = g; }
                else           { homeLabel = (p == '1' ? "Winner " : "Runner-up ") + g; }
            }

            if (awaySlot.startsWith("3:")) {
                awayLabel = "3rd of " + awaySlot.substring(2).replace(",", "/");
            } else {
                char p = awaySlot.charAt(0);
                String g = awaySlot.substring(1);
                StandingRowView t = p == '1' ? winners.get(g) : runnersUp.get(g);
                if (t != null) { awaySlug = t.slug(); awayName = t.name(); awayGroup = g; }
                else           { awayLabel = (p == '1' ? "Winner " : "Runner-up ") + g; }
            }

            result.add(new ProjectedMatchup(
                matchNum,
                homeSlug, homeName, homeGroup,
                awaySlug, awayName, awayGroup,
                homeLabel, awayLabel
            ));
        }
        return result;
    }
}
