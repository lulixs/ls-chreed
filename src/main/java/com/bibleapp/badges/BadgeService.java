package com.bibleapp.badges;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.bibleapp.data.DataStore;
import com.bibleapp.data.MemorizedVerse;

/**
 * Evaluates {@link BadgeRegistry} against the current user data.
 *
 * Badges are computed on demand — never stored — so adding new types or
 * changing thresholds is safe and self-healing.
 *
 * To detect newly-earned badges (for toast notifications), call
 * {@link #snapshotEarnedIds()} before the action and {@link #diffEarnedSince}
 * after. The "since" is plain set diff against the snapshot.
 */
public final class BadgeService {

    private BadgeService() {}

    public static List<Badge> earned() {
        List<MemorizedVerse> verses = DataStore.getMemorizationList();
        List<Badge> earned = new ArrayList<>();
        for (Badge b : BadgeRegistry.all()) {
            if (b.isEarned(verses)) earned.add(b);
        }
        return earned;
    }

    public static Set<String> snapshotEarnedIds() {
        Set<String> ids = new LinkedHashSet<>();
        List<MemorizedVerse> verses = DataStore.getMemorizationList();
        for (Badge b : BadgeRegistry.all()) {
            if (b.isEarned(verses)) ids.add(b.getId());
        }
        return ids;
    }

    /** Returns badges earned now but not present in the prior snapshot. */
    public static List<Badge> diffEarnedSince(Set<String> priorIds) {
        List<Badge> newlyEarned = new ArrayList<>();
        List<MemorizedVerse> verses = DataStore.getMemorizationList();
        for (Badge b : BadgeRegistry.all()) {
            if (b.isEarned(verses) && !priorIds.contains(b.getId())) {
                newlyEarned.add(b);
            }
        }
        return newlyEarned;
    }
}
