package com.bibleapp.badges;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Round badge medallion drawn entirely from JavaFX shapes — no image assets.
 *
 * Tier visual progression:
 *   Tier 1 (or milestone)  flat book/slate color, thin gold rim
 *   Tier 2                 same color, thicker gold rim
 *   Tier 3                 gold gradient outer, book-color inner
 *   Tier 4                 full gold radial + 4 star accents around the rim
 *
 * Locked badges desaturate to grayscale so the user can see what's coming.
 */
public final class BadgeView extends StackPane {

    private static final Color GOLD_DARK   = Color.web("#b8860b");
    private static final Color GOLD        = Color.web("#d4af37");
    private static final Color GOLD_LIGHT  = Color.web("#f5d76e");
    private static final Color LOCKED_BG   = Color.web("#c8ccd1");
    private static final Color LOCKED_RIM  = Color.web("#9aa1ab");
    private static final Color LOCKED_TEXT = Color.web("#5a6068");

    public BadgeView(Badge badge, boolean earned, double size) {
        getStyleClass().add("badge-view");
        if (!earned) getStyleClass().add("badge-view--locked");

        double r = size / 2.0;

        Color base = earned ? Color.web(badge.getColorHex()) : LOCKED_BG;
        Color rim  = earned ? rimForTier(badge.getTier()) : LOCKED_RIM;
        double rimWidth = earned ? rimWidthForTier(badge.getTier()) : 1.5;

        Circle outer = new Circle(r);
        outer.setFill(rim);

        Circle inner = new Circle(r - rimWidth);
        inner.setFill(earned ? fillForTier(badge.getTier(), base) : LOCKED_BG);

        Group ornament = new Group();
        if (earned && badge.getTier() == 4) {
            addStarAccents(ornament, r, base);
        }

        Text label = new Text(shortLabel(badge));
        label.setFill(earned ? Color.WHITE : LOCKED_TEXT);
        label.setFont(Font.font("System", FontWeight.BOLD, fontSizeFor(size, badge)));
        StackPane.setAlignment(label, Pos.CENTER);

        getChildren().addAll(outer, inner, ornament, label);
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
    }

    private static Color rimForTier(int tier) {
        return switch (tier) {
            case 4 -> GOLD_LIGHT;
            case 3 -> GOLD;
            case 2 -> GOLD_DARK;
            default -> GOLD_DARK; // milestones (tier 0) get the same thin gold rim as tier 1
        };
    }

    private static double rimWidthForTier(int tier) {
        return switch (tier) {
            case 4 -> 6;
            case 3 -> 4.5;
            case 2 -> 3;
            default -> 1.8;
        };
    }

    private static javafx.scene.paint.Paint fillForTier(int tier, Color base) {
        return switch (tier) {
            case 4 -> new RadialGradient(0, 0, 0.5, 0.4, 0.7, true, CycleMethod.NO_CYCLE,
                    new Stop(0.0, GOLD_LIGHT),
                    new Stop(0.55, base),
                    new Stop(1.0, base.darker()));
            case 3 -> new RadialGradient(0, 0, 0.5, 0.5, 0.85, true, CycleMethod.NO_CYCLE,
                    new Stop(0.0, base.brighter()),
                    new Stop(1.0, base.darker()));
            default -> base;
        };
    }

    /** Four small gold stars positioned at the cardinal points of the rim. */
    private static void addStarAccents(Group g, double r, Color base) {
        double starR = r * 0.13;
        double inset = r - starR * 1.6;
        double[][] positions = { {0, -inset}, {inset, 0}, {0, inset}, {-inset, 0} };
        for (double[] p : positions) {
            Polygon star = makeStar(starR, starR * 0.45);
            star.setFill(GOLD_LIGHT);
            star.setStroke(GOLD_DARK);
            star.setStrokeWidth(0.6);
            star.setTranslateX(p[0]);
            star.setTranslateY(p[1]);
            g.getChildren().add(star);
        }
    }

    private static Polygon makeStar(double outerR, double innerR) {
        Polygon star = new Polygon();
        int points = 5;
        for (int i = 0; i < points * 2; i++) {
            double radius = (i % 2 == 0) ? outerR : innerR;
            double angle = -Math.PI / 2 + i * Math.PI / points;
            star.getPoints().addAll(radius * Math.cos(angle), radius * Math.sin(angle));
        }
        return star;
    }

    /**
     * Short text drawn on the medallion. Mastery shows the tier roman numeral;
     * milestones show the first letter of each significant word.
     */
    private static String shortLabel(Badge b) {
        if (b.getCategory() == BadgeCategory.MASTERY) {
            return switch (b.getTier()) {
                case 1 -> "I";
                case 2 -> "II";
                case 3 -> "III";
                case 4 -> "IV";
                default -> "";
            };
        }
        StringBuilder sb = new StringBuilder();
        for (String word : b.getTitle().split("\\s+")) {
            if (!word.isEmpty() && Character.isLetterOrDigit(word.charAt(0))) {
                sb.append(Character.toUpperCase(word.charAt(0)));
            }
            if (sb.length() >= 3) break;
        }
        return sb.toString();
    }

    private static double fontSizeFor(double size, Badge b) {
        // Roman numerals look cleaner a bit larger.
        double base = b.getCategory() == BadgeCategory.MASTERY ? 0.34 : 0.28;
        return Math.max(10, size * base);
    }
}
