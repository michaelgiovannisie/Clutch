package com.zipcode.worldcuptracker.service;

import com.zipcode.worldcuptracker.standings.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StandingsService {

    // ── Public API ────────────────────────────────────────────────────────────

    public List<GroupStandingView> getRankedGroups() {
        Set<String> qualifiedSlugs = getBestThirds(8).stream()
            .map(ThirdPlaceRow::slug)
            .collect(Collectors.toSet());

        return MockStandingsData.GROUP_ORDER.stream()
            .map(id -> new GroupStandingView(id, rankGroup(MockStandingsData.GROUPS.get(id), qualifiedSlugs)))
            .collect(Collectors.toList());
    }

    public List<ThirdPlaceRow> getRankedThirds() {
        List<ThirdPlaceRow> thirds = MockStandingsData.GROUP_ORDER.stream()
            .map(id -> {
                RawStandingRow third = sortByTiebreaker(MockStandingsData.GROUPS.get(id)).get(2);
                return toThirdPlaceRow(third, id, 0, false);
            })
            .collect(Collectors.toCollection(ArrayList::new));

        thirds.sort(thirdsComparator());

        List<ThirdPlaceRow> ranked = new ArrayList<>();
        for (int i = 0; i < thirds.size(); i++) {
            ThirdPlaceRow t = thirds.get(i);
            ranked.add(new ThirdPlaceRow(
                t.slug(), t.name(), t.abbr(), t.group(),
                t.played(), t.won(), t.drawn(), t.lost(),
                t.gf(), t.ga(), t.gd(), t.points(),
                i + 1, i < 8
            ));
        }
        return ranked;
    }

    public List<StandingRowView> getGroupWinners() {
        return MockStandingsData.GROUP_ORDER.stream()
            .map(id -> toView(sortByTiebreaker(MockStandingsData.GROUPS.get(id)).get(0), 1, "advance"))
            .collect(Collectors.toList());
    }

    public List<StandingRowView> getGroupRunnersUp() {
        return MockStandingsData.GROUP_ORDER.stream()
            .map(id -> toView(sortByTiebreaker(MockStandingsData.GROUPS.get(id)).get(1), 2, "advance"))
            .collect(Collectors.toList());
    }

    public List<ThirdPlaceRow> getBestThirds(int cutoff) {
        List<ThirdPlaceRow> thirds = MockStandingsData.GROUP_ORDER.stream()
            .map(id -> toThirdPlaceRow(sortByTiebreaker(MockStandingsData.GROUPS.get(id)).get(2), id, 0, false))
            .collect(Collectors.toCollection(ArrayList::new));

        thirds.sort(thirdsComparator());

        List<ThirdPlaceRow> result = new ArrayList<>();
        for (int i = 0; i < Math.min(cutoff, thirds.size()); i++) {
            ThirdPlaceRow t = thirds.get(i);
            result.add(new ThirdPlaceRow(
                t.slug(), t.name(), t.abbr(), t.group(),
                t.played(), t.won(), t.drawn(), t.lost(),
                t.gf(), t.ga(), t.gd(), t.points(),
                i + 1, true
            ));
        }
        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<StandingRowView> rankGroup(List<RawStandingRow> raw, Set<String> qualifiedSlugs) {
        List<RawStandingRow> sorted = sortByTiebreaker(raw);
        List<StandingRowView> result = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            RawStandingRow r = sorted.get(i);
            int rank = i + 1;
            String state = rank <= 2 ? "advance"
                : (rank == 3 && qualifiedSlugs.contains(r.slug())) ? "wildcard"
                : "out";
            result.add(toView(r, rank, state));
        }
        return result;
    }

    private List<RawStandingRow> sortByTiebreaker(List<RawStandingRow> rows) {
        return rows.stream()
            .sorted(Comparator
                .comparingInt(RawStandingRow::getPoints).reversed()
                .thenComparingInt(RawStandingRow::getGd).reversed()
                .thenComparingInt(RawStandingRow::gf).reversed()
                .thenComparing(RawStandingRow::name))
            .collect(Collectors.toList());
    }

    private Comparator<ThirdPlaceRow> thirdsComparator() {
        return Comparator
            .comparingInt(ThirdPlaceRow::points).reversed()
            .thenComparingInt(ThirdPlaceRow::gd).reversed()
            .thenComparingInt(ThirdPlaceRow::gf).reversed()
            .thenComparing(ThirdPlaceRow::name);
    }

    private StandingRowView toView(RawStandingRow r, int rank, String state) {
        return new StandingRowView(
            r.slug(), r.name(), r.abbr(),
            r.getPlayed(), r.won(), r.drawn(), r.lost(),
            r.gf(), r.ga(), r.getGd(), r.getPoints(),
            rank, state
        );
    }

    private ThirdPlaceRow toThirdPlaceRow(RawStandingRow r, String group, int thirdRank, boolean qualified) {
        return new ThirdPlaceRow(
            r.slug(), r.name(), r.abbr(), group,
            r.getPlayed(), r.won(), r.drawn(), r.lost(),
            r.gf(), r.ga(), r.getGd(), r.getPoints(),
            thirdRank, qualified
        );
    }
}
