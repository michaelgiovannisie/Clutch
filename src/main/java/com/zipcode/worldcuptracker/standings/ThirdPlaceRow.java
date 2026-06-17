package com.zipcode.worldcuptracker.standings;

public record ThirdPlaceRow(
    String slug,
    String name,
    String abbr,
    String group,
    int played,
    int won,
    int drawn,
    int lost,
    int gf,
    int ga,
    int gd,
    int points,
    int thirdRank,
    boolean qualified
) {
    // JavaBeans getters for Thymeleaf SpEL compatibility
    public String getSlug()     { return slug; }
    public String getName()     { return name; }
    public String getAbbr()     { return abbr; }
    public String getGroup()    { return group; }
    public int getPlayed()      { return played; }
    public int getWon()         { return won; }
    public int getDrawn()       { return drawn; }
    public int getLost()        { return lost; }
    public int getGf()          { return gf; }
    public int getGa()          { return ga; }
    public int getGd()          { return gd; }
    public int getPoints()      { return points; }
    public int getThirdRank()   { return thirdRank; }
    public boolean isQualified(){ return qualified; }
}
