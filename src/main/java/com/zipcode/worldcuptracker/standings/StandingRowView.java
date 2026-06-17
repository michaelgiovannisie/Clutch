package com.zipcode.worldcuptracker.standings;

/** Immutable view of one team row in a group table. */
public record StandingRowView(
    String slug,
    String name,
    String abbr,
    int played,
    int won,
    int drawn,
    int lost,
    int gf,
    int ga,
    int gd,
    int points,
    int rank,
    /** "advance" | "wildcard" | "out" */
    String state
) {
    // JavaBeans getters for Thymeleaf SpEL compatibility
    public String getSlug()  { return slug; }
    public String getName()  { return name; }
    public String getAbbr()  { return abbr; }
    public int getPlayed()   { return played; }
    public int getWon()      { return won; }
    public int getDrawn()    { return drawn; }
    public int getLost()     { return lost; }
    public int getGf()       { return gf; }
    public int getGa()       { return ga; }
    public int getGd()       { return gd; }
    public int getPoints()   { return points; }
    public int getRank()     { return rank; }
    public String getState() { return state; }
}
