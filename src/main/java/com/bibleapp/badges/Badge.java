package com.bibleapp.badges;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.bibleapp.data.MemorizedVerse;

/**
 * One badge definition. Definition-driven: a new badge type is one new entry
 * in {@link BadgeRegistry}; no callsite changes.
 *
 * Earned state is computed from the user's data, not stored — so adding,
 * tweaking, or removing badges later doesn't require a data migration, and
 * eligible-but-unflagged badges are awarded retroactively the next time the
 * registry is evaluated.
 */
public final class Badge {

    private final String id;
    private final String title;
    private final String description;
    private final BadgeCategory category;
    private final String book;        // null for milestones
    private final int    tier;        // 0 for milestones; 1..N for mastery
    private final String colorHex;    // base color (book color for mastery, slate for milestone)
    private final Predicate<List<MemorizedVerse>> earnedPredicate;
    private final Function<List<MemorizedVerse>, Progress> progressFn;

    public Badge(String id,
                 String title,
                 String description,
                 BadgeCategory category,
                 String book,
                 int tier,
                 String colorHex,
                 Predicate<List<MemorizedVerse>> earnedPredicate,
                 Function<List<MemorizedVerse>, Progress> progressFn) {
        this.id              = id;
        this.title           = title;
        this.description     = description;
        this.category        = category;
        this.book            = book;
        this.tier            = tier;
        this.colorHex        = colorHex;
        this.earnedPredicate = earnedPredicate;
        this.progressFn      = progressFn;
    }

    public String        getId()          { return id; }
    public String        getTitle()       { return title; }
    public String        getDescription() { return description; }
    public BadgeCategory getCategory()    { return category; }
    public String        getBook()        { return book; }
    public int           getTier()        { return tier; }
    public String        getColorHex()    { return colorHex; }

    public boolean isEarned(List<MemorizedVerse> verses) {
        return earnedPredicate.test(verses);
    }

    public Progress getProgress(List<MemorizedVerse> verses) {
        return progressFn.apply(verses);
    }

    /** Numerator/denominator pair for "X of Y" progress display. */
    public record Progress(int current, int target) {
        public double ratio() {
            return target == 0 ? 1.0 : Math.min(1.0, (double) current / target);
        }
    }
}
