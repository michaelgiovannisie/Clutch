package com.zipcode.worldcuptracker.standings;

import java.util.List;

public record GroupStandingView(String id, List<StandingRowView> rows) {
    public String getId()               { return id; }
    public List<StandingRowView> getRows() { return rows; }
}
