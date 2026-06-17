package com.zipcode.worldcuptracker.standings;

public record RawStandingRow(
    String slug,
    String name,
    String abbr,
    int won,
    int drawn,
    int lost,
    int gf,
    int ga
) {
    public int getPlayed()  { return won + drawn + lost; }
    public int getGd()      { return gf - ga; }
    public int getPoints()  { return won * 3 + drawn; }
}
