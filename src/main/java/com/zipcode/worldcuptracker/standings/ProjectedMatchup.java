package com.zipcode.worldcuptracker.standings;

public record ProjectedMatchup(
    int matchNumber,
    String homeSlug,
    String homeName,
    String homeGroup,
    String awaySlug,
    String awayName,
    String awayGroup,
    /** shown when homeSlug is null, e.g. "3rd of A/B/C/D/F" */
    String homeLabel,
    String awayLabel
) {
    // JavaBeans getters for Thymeleaf SpEL compatibility
    public int getMatchNumber()    { return matchNumber; }
    public String getHomeSlug()    { return homeSlug; }
    public String getHomeName()    { return homeName; }
    public String getHomeGroup()   { return homeGroup; }
    public String getAwaySlug()    { return awaySlug; }
    public String getAwayName()    { return awayName; }
    public String getAwayGroup()   { return awayGroup; }
    public String getHomeLabel()   { return homeLabel; }
    public String getAwayLabel()   { return awayLabel; }
}
