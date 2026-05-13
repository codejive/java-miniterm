package org.codejive.miniterm.image.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import org.codejive.miniterm.image.ImageEncoder;
import org.codejive.miniterm.image.util.AnsiUtils;
import org.codejive.miniterm.image.util.FontSize;
import org.codejive.miniterm.image.util.ImageUtils;
import org.codejive.miniterm.image.util.Resolution;
import org.jspecify.annotations.NonNull;

/**
 * Implementation of a block-based terminal image encoder using Unicode block characters.
 *
 * <p>This encoder works in any terminal by using Unicode block drawing characters (half-blocks,
 * quadrants, sextants, or octants) to represent sub-pixel resolution within each character cell.
 * Since each cell can only have one foreground and one background color, this implementation uses
 * color clustering to find the best two representative colors for each cell's pixels.
 *
 * <p>This is the most compatible image rendering method as it requires no special terminal support
 * beyond Unicode and ANSI color codes.
 *
 * <p>This encoder is stateful: the image, font size, and block mode are set at construction time
 * and are immutable, while the target size and fit mode can be changed via setters. Expensive
 * transformations like image scaling are performed lazily on the first call to {@link
 * #render(Appendable)} and cached for subsequent calls.
 */
public class BlockEncoder implements ImageEncoder {
    // Immutable state
    private final @NonNull BufferedImage image;
    private final @NonNull BlockMode mode;

    // Mutable state
    private int targetWidth;
    private int targetHeight;
    private boolean fitImage;

    // Cached transformations
    private BufferedImage scaledImage;

    public static BlockEncoder create(
            @NonNull BlockMode mode,
            @NonNull BufferedImage image,
            int targetWidth,
            int targetHeight,
            boolean fitImage) {
        return new BlockEncoder(mode, image, targetWidth, targetHeight, fitImage);
    }

    /**
     * Defines the different block rendering modes for the block-based image encoder.
     *
     * <p>Block modes determine how many sub-pixels are rendered within each terminal character
     * cell, trading off between resolution and compatibility.
     */
    public enum BlockMode {
        /**
         * Full block mode using solid block characters
         *
         * <p>Each cell represents a single pixel (1x1), with no subdivision. This is the simplest,
         * rendering each terminal cell as a solid color. Provides lowest resolution.
         */
        FULL(1, 1),

        /**
         * Half-block mode using upper and lower half block characters
         *
         * <p>Divides each cell into 2 vertical pixels (1x2), providing basic vertical resolution
         * improvement. This is the most compatible mode, supported in virtually all terminals.
         */
        HALF(1, 2),

        /**
         * Quadrant mode using 2x2 block characters
         *
         * <p>Divides each cell into 4 pixels (2x2), providing moderate resolution improvement in
         * both dimensions. Well supported in modern terminals.
         */
        QUADRANT(2, 2),

        /**
         * Sextant mode using 2x3 block characters.
         *
         * <p>Divides each cell into 6 pixels (2x3), providing higher vertical resolution. Requires
         * Unicode support for Symbols for Legacy Computing characters (U+1FB00-U+1FB3B).
         */
        SEXTANT(2, 3),

        /**
         * Octant mode using 2x4 block characters.
         *
         * <p>Divides each cell into 8 pixels (2x4), providing the highest resolution. Requires wide
         * Unicode support.
         */
        OCTANT(2, 4);

        private final int columns;
        private final int rows;

        BlockMode(int columns, int rows) {
            this.columns = columns;
            this.rows = rows;
        }

        /**
         * Gets the number of horizontal sub-pixels per cell.
         *
         * @return the number of horizontal sub-pixels in this block mode (1 or 2)
         */
        public int columns() {
            return columns;
        }

        /**
         * Gets the number of vertical sub-pixels per cell.
         *
         * @return the number of vertical sub-pixels in this block mode (2, 3, or 4)
         */
        public int rows() {
            return rows;
        }

        /**
         * Gets the total number of sub-pixels per cell.
         *
         * @return columns * rows
         */
        public int pixelsPerCell() {
            return columns * rows;
        }
    }

    // Full block characters (1x1)
    private static final String[] FULL_BLOCKS = {
        " ", // 0b0 - U+00A0 NO-BREAK SPACE (EMPTY)
        "█" // 0b1 - U+2588 FULL BLOCK
    };

    // Half-block characters (1x2)
    private static final String[] HALF_BLOCKS = {
        " ", // 0b00 - U+00A0 NO-BREAK SPACE (EMPTY)
        "▀", // 0b01 - U+2580 UPPER HALF BLOCK
        "▄", // 0b10 - U+2584 LOWER HALF BLOCK
        "█" // 0b11 - U+2588 FULL BLOCK
    };

    // Quadrant characters (2x2) - indexed by bit pattern: top-left, top-right, bottom-left,
    // bottom-right
    private static final String[] QUADRANT_BLOCKS = {
        " ", // 0b0000 - U+00A0 NO-BREAK SPACE (EMPTY)
        "▘", // 0b0001 - U+2598 QUADRANT UPPER LEFT
        "▝", // 0b0010 - U+259D QUADRANT UPPER RIGHT
        "▀", // 0b0011 - U+2580 UPPER HALF BLOCK
        "▖", // 0b0100 - U+2596 QUADRANT LOWER LEFT
        "▌", // 0b0101 - U+258C LEFT HALF BLOCK
        "▞", // 0b0110 - U+259E QUADRANT LOWER LEFT AND UPPER RIGHT
        "▛", // 0b0111 - U+259B QUADRANT UPPER LEFT AND UPPER RIGHT AND LOWER LEFT
        "▗", // 0b1000 - U+2597 QUADRANT LOWER RIGHT
        "▚", // 0b1001 - U+259A QUADRANT UPPER LEFT AND LOWER RIGHT
        "▐", // 0b1010 - U+2590 RIGHT HALF BLOCK
        "▜", // 0b1011 - U+259C QUADRANT UPPER LEFT AND UPPER RIGHT AND LOWER RIGHT
        "▄", // 0b1100 - U+2584 LOWER HALF BLOCK
        "▙", // 0b1101 - U+2599 QUADRANT UPPER LEFT AND LOWER LEFT AND LOWER RIGHT
        "▟", // 0b1110 - U+259F QUADRANT UPPER RIGHT AND LOWER LEFT AND LOWER RIGHT
        "█" // 0b1111 - U+2588 FULL BLOCK
    };

    // Sextant characters (2x3) - Symbols for Legacy Computing block (U+1FB00-U+1FB3B)
    // Lookup table from sextant Unicode range 0x1fb00..=0x1fb3b to sextant pattern:
    // `pattern` is a byte whose bits corresponds to elements on a 2 by 3 grid.
    // The position of a sextant for a bit position (1-indexed) is as follows:
    // ╭───┬───╮
    // │ 1 │ 2 │
    // ├───┼───┤
    // │ 3 │ 4 │
    // ├───┼───┤
    // │ 5 │ 6 │
    // ╰───┴───╯
    private static final String[] SEXTANT_BLOCKS = {
        " ", // 0b000000 (0) - U+00A0 NO-BREAK SPACE (EMPTY)
        "\uD83E\uDF00", // 0b000001 (1)  - U+1FB00 SEXTANT-1
        "\uD83E\uDF01", // 0b000010 (2)  - U+1FB01 SEXTANT-2
        "\uD83E\uDF02", // 0b000011 (3)  - U+1FB02 SEXTANT-12
        "\uD83E\uDF03", // 0b000100 (4)  - U+1FB03 SEXTANT-3
        "\uD83E\uDF04", // 0b000101 (5)  - U+1FB04 SEXTANT-13
        "\uD83E\uDF05", // 0b000110 (6)  - U+1FB05 SEXTANT-23
        "\uD83E\uDF06", // 0b000111 (7)  - U+1FB06 SEXTANT-123
        "\uD83E\uDF07", // 0b001000 (8)  - U+1FB07 SEXTANT-4
        "\uD83E\uDF08", // 0b001001 (9)  - U+1FB08 SEXTANT-14
        "\uD83E\uDF09", // 0b001010 (10) - U+1FB09 SEXTANT-24
        "\uD83E\uDF0A", // 0b001011 (11) - U+1FB0A SEXTANT-124
        "\uD83E\uDF0B", // 0b001100 (12) - U+1FB0B SEXTANT-34
        "\uD83E\uDF0C", // 0b001101 (13) - U+1FB0C SEXTANT-134
        "\uD83E\uDF0D", // 0b001110 (14) - U+1FB0D SEXTANT-234
        "\uD83E\uDF0E", // 0b001111 (15) - U+1FB0E SEXTANT-1234
        "\uD83E\uDF0F", // 0b010000 (16) - U+1FB0F SEXTANT-5
        "\uD83E\uDF10", // 0b010001 (17) - U+1FB10 SEXTANT-15
        "\uD83E\uDF11", // 0b010010 (18) - U+1FB11 SEXTANT-25
        "\uD83E\uDF12", // 0b010011 (19) - U+1FB12 SEXTANT-125
        "\uD83E\uDF13", // 0b010100 (20) - U+1FB13 SEXTANT-35
        "▌", // 0b010101 (21) - U+258C LEFT HALF BLOCK (positions 1,3,5)
        "\uD83E\uDF14", // 0b010110 (22) - U+1FB14 SEXTANT-235
        "\uD83E\uDF15", // 0b010111 (23) - U+1FB15 SEXTANT-1235
        "\uD83E\uDF16", // 0b011000 (24) - U+1FB16 SEXTANT-45
        "\uD83E\uDF17", // 0b011001 (25) - U+1FB17 SEXTANT-145
        "\uD83E\uDF18", // 0b011010 (26) - U+1FB18 SEXTANT-245
        "\uD83E\uDF19", // 0b011011 (27) - U+1FB19 SEXTANT-1245
        "\uD83E\uDF1A", // 0b011100 (28) - U+1FB1A SEXTANT-345
        "\uD83E\uDF1B", // 0b011101 (29) - U+1FB1B SEXTANT-1345
        "\uD83E\uDF1C", // 0b011110 (30) - U+1FB1C SEXTANT-2345
        "\uD83E\uDF1D", // 0b011111 (31) - U+1FB1D SEXTANT-12345
        "\uD83E\uDF1E", // 0b100000 (32) - U+1FB1E SEXTANT-6
        "\uD83E\uDF1F", // 0b100001 (33) - U+1FB1F SEXTANT-16
        "\uD83E\uDF20", // 0b100010 (34) - U+1FB20 SEXTANT-26
        "\uD83E\uDF21", // 0b100011 (35) - U+1FB21 SEXTANT-126
        "\uD83E\uDF22", // 0b100100 (36) - U+1FB22 SEXTANT-36
        "\uD83E\uDF23", // 0b100101 (37) - U+1FB23 SEXTANT-136
        "\uD83E\uDF24", // 0b100110 (38) - U+1FB24 SEXTANT-236
        "\uD83E\uDF25", // 0b100111 (39) - U+1FB25 SEXTANT-1236
        "\uD83E\uDF26", // 0b101000 (40) - U+1FB26 SEXTANT-46
        "\uD83E\uDF27", // 0b101001 (41) - U+1FB27 SEXTANT-146
        "▐", // 0b101010 (42) - U+2590 RIGHT HALF BLOCK (positions 2,4,6)
        "\uD83E\uDF28", // 0b101011 (43) - U+1FB28 SEXTANT-1246
        "\uD83E\uDF29", // 0b101100 (44) - U+1FB29 SEXTANT-346
        "\uD83E\uDF2A", // 0b101101 (45) - U+1FB2A SEXTANT-1346
        "\uD83E\uDF2B", // 0b101110 (46) - U+1FB2B SEXTANT-2346
        "\uD83E\uDF2C", // 0b101111 (47) - U+1FB2C SEXTANT-12346
        "\uD83E\uDF2D", // 0b110000 (48) - U+1FB2D SEXTANT-56
        "\uD83E\uDF2E", // 0b110001 (49) - U+1FB2E SEXTANT-156
        "\uD83E\uDF2F", // 0b110010 (50) - U+1FB2F SEXTANT-256
        "\uD83E\uDF30", // 0b110011 (51) - U+1FB30 SEXTANT-1256
        "\uD83E\uDF31", // 0b110100 (52) - U+1FB31 SEXTANT-356
        "\uD83E\uDF32", // 0b110101 (53) - U+1FB32 SEXTANT-1356
        "\uD83E\uDF33", // 0b110110 (54) - U+1FB33 SEXTANT-2356
        "\uD83E\uDF34", // 0b110111 (55) - U+1FB34 SEXTANT-12356
        "\uD83E\uDF35", // 0b111000 (56) - U+1FB35 SEXTANT-456
        "\uD83E\uDF36", // 0b111001 (57) - U+1FB36 SEXTANT-1456
        "\uD83E\uDF37", // 0b111010 (58) - U+1FB37 SEXTANT-2456
        "\uD83E\uDF38", // 0b111011 (59) - U+1FB38 SEXTANT-12456
        "\uD83E\uDF39", // 0b111100 (60) - U+1FB39 SEXTANT-3456
        "\uD83E\uDF3A", // 0b111101 (61) - U+1FB3A SEXTANT-13456
        "\uD83E\uDF3B", // 0b111110 (62) - U+1FB3B SEXTANT-23456
        "█" // 0b111111 (63) - U+2588 FULL BLOCK
    };

    // Lookup table from octant Unicode range 0x1cd00..=0x1cde5 to octant pattern:
    // `pattern` is a byte whose bits corresponds to elements on a 2 by 4 grid.
    // The position of a octant for a bit position (1-indexed) is as follows:
    // ╭───┬───╮
    // │ 1 │ 2 │
    // ├───┼───┤
    // │ 3 │ 4 │
    // ├───┼───┤
    // │ 5 │ 6 │
    // ├───┼───┤
    // │ 7 │ 8 │
    // ╰───┴───╯
    // Octant characters (2x4) - Indexed completely from 0b00000000 (0) to 0b11111111 (255)
    // Combines Block Elements, Legacy Computing, and the Legacy Computing Supplement blocks.
    private static final String[] OCTANT_BLOCKS = {
        " ", // 0b000000 (0) - U+00A0 NO-BREAK SPACE (EMPTY)
        "\uD833\uDEA8", // 0b00000001 (1) - U+1CEA8 LEFT HALF UPPER ONE QUARTER BLOCK (OCTANT-1)
        "\uD833\uDEAB", // 0b00000010 (2) - U+1CEAB RIGHT HALF UPPER ONE QUARTER BLOCK (OCTANT-2)
        "\uD83E\uDF82", // 0b00000011 (3) - U+1FB82 UPPER ONE QUARTER BLOCK (OCTANT-12)
        "\uD833\uDD00", // 0b00000100 (4) - U+1CD00 BLOCK OCTANT-3
        "▘", // 0b00000101 (5) - U+2598 UPPER LEFT QUADRANT (OCTANT-13)
        "\uD833\uDD01", // 0b00000110 (6) - U+1CD01 BLOCK OCTANT-23
        "\uD833\uDD02", // 0b00000111 (7) - U+1CD02 BLOCK OCTANT-123
        "\uD833\uDD03", // 0b00001000 (8) - U+1CD03 BLOCK OCTANT-4
        "\uD833\uDD04", // 0b00001001 (9) - U+1CD04 BLOCK OCTANT-14
        "▝", // 0b00001010 (10) - U+259D UPPER RIGHT QUADRANT (OCTANT-24)
        "\uD833\uDD05", // 0b00001011 (11) - U+1CD05 BLOCK OCTANT-124
        "\uD833\uDD06", // 0b00001100 (12) - U+1CD06 BLOCK OCTANT-34
        "\uD833\uDD07", // 0b00001101 (13) - U+1CD07 BLOCK OCTANT-134
        "\uD833\uDD08", // 0b00001110 (14) - U+1CD08 BLOCK OCTANT-234
        "▀", // 0b00001111 (15) - U+2580 UPPER HALF BLOCK (OCTANT-1234)
        "\uD833\uDD09", // 0b00010000 (16) - U+1CD09 BLOCK OCTANT-5
        "\uD833\uDD0A", // 0b00010001 (17) - U+1CD0A BLOCK OCTANT-15
        "\uD833\uDD0B", // 0b00010010 (18) - U+1CD0B BLOCK OCTANT-25
        "\uD833\uDD0C", // 0b00010011 (19) - U+1CD0C BLOCK OCTANT-125
        "\uD83E\uDFE6", // 0b00010100 (20) - U+1FBE6 MIDDLE LEFT ONE QUARTER BLOCK (OCTANT-35)
        "\uD833\uDD0D", // 0b00010101 (21) - U+1CD0D BLOCK OCTANT-135
        "\uD833\uDD0E", // 0b00010116 (22) - U+1CD0E BLOCK OCTANT-235
        "\uD833\uDD0F", // 0b00010117 (23) - U+1CD0F BLOCK OCTANT-1235
        "\uD833\uDD10", // 0b00011000 (24) - U+1CD10 BLOCK OCTANT-45
        "\uD833\uDD11", // 0b00011001 (25) - U+1CD11 BLOCK OCTANT-145
        "\uD833\uDD12", // 0b00011010 (26) - U+1CD12 BLOCK OCTANT-245
        "\uD833\uDD13", // 0b00011011 (27) - U+1CD13 BLOCK OCTANT-1245
        "\uD833\uDD14", // 0b00011100 (28) - U+1CD14 BLOCK OCTANT-345
        "\uD833\uDD15", // 0b00011101 (29) - U+1CD15 BLOCK OCTANT-1345
        "\uD833\uDD16", // 0b00011110 (30) - U+1CD16 BLOCK OCTANT-2345
        "\uD833\uDD17", // 0b00011111 (31) - U+1CD17 BLOCK OCTANT-12345
        "\uD833\uDD18", // 0b00100000 (32) - U+1CD18 BLOCK OCTANT-6
        "\uD833\uDD19", // 0b00100001 (33) - U+1CD19 BLOCK OCTANT-16
        "\uD833\uDD1A", // 0b00100010 (34) - U+1CD1A BLOCK OCTANT-26
        "\uD833\uDD1B", // 0b00100011 (35) - U+1CD1B BLOCK OCTANT-126
        "\uD833\uDD1C", // 0b00100100 (36) - U+1CD1C BLOCK OCTANT-36
        "\uD833\uDD1D", // 0b00100101 (37) - U+1CD1D BLOCK OCTANT-136
        "\uD833\uDD1E", // 0b00100110 (38) - U+1CD1E BLOCK OCTANT-236
        "\uD833\uDD1F", // 0b00100111 (39) - U+1CD1F BLOCK OCTANT-1236
        "\uD83E\uDFE7", // 0b00101000 (40) - U+1FBE7 MIDDLE RIGHT ONE QUARTER BLOCK (OCTANT-46)
        "\uD833\uDD20", // 0b00101001 (41) - U+1CD20 BLOCK OCTANT-146
        "\uD833\uDD21", // 0b00101010 (42) - U+1CD21 BLOCK OCTANT-246
        "\uD833\uDD22", // 0b00101011 (43) - U+1CD22 BLOCK OCTANT-1246
        "\uD833\uDD23", // 0b00101100 (44) - U+1CD23 BLOCK OCTANT-346
        "\uD833\uDD24", // 0b00101101 (45) - U+1CD24 BLOCK OCTANT-1346
        "\uD833\uDD25", // 0b00101110 (46) - U+1CD25 BLOCK OCTANT-2346
        "\uD833\uDD26", // 0b00101111 (47) - U+1CD26 BLOCK OCTANT-12346
        "\uD833\uDD27", // 0b00110000 (48) - U+1CD27 BLOCK OCTANT-56
        "\uD833\uDD28", // 0b00110001 (49) - U+1CD28 BLOCK OCTANT-156
        "\uD833\uDD29", // 0b00110010 (50) - U+1CD29 BLOCK OCTANT-256
        "\uD833\uDD2A", // 0b00110011 (51) - U+1CD2A BLOCK OCTANT-1256
        "\uD833\uDD2B", // 0b00110100 (52) - U+1CD2B BLOCK OCTANT-356
        "\uD833\uDD2C", // 0b00110101 (53) - U+1CD2C BLOCK OCTANT-1356
        "\uD833\uDD2D", // 0b00110110 (54) - U+1CD2D BLOCK OCTANT-2356
        "\uD833\uDD2E", // 0b00110111 (55) - U+1CD2E BLOCK OCTANT-12356
        "\uD833\uDD2F", // 0b00111000 (56) - U+1CD2F BLOCK OCTANT-456
        "\uD833\uDD30", // 0b00111001 (57) - U+1CD30 BLOCK OCTANT-1456
        "\uD833\uDD31", // 0b00111010 (58) - U+1CD31 BLOCK OCTANT-2456
        "\uD833\uDD32", // 0b00111011 (59) - U+1CD32 BLOCK OCTANT-12456
        "\uD833\uDD33", // 0b00111100 (60) - U+1CD33 BLOCK OCTANT-3456
        "\uD833\uDD34", // 0b00111101 (61) - U+1CD34 BLOCK OCTANT-13456
        "\uD833\uDD35", // 0b00111110 (62) - U+1CD35 BLOCK OCTANT-23456
        "\uD83E\uDF85", // 0b00111111 (63) - U+1FB85 UPPER THREE QUARTERS BLOCK (OCTANT-123456)
        "\uD833\uDEA3", // 0b01000000 (64) - U+1CEA3 LEFT HALF LOWER ONE QUARTER BLOCK (OCTANT-7)
        "\uD833\uDD36", // 0b01000001 (65) - U+1CD36 BLOCK OCTANT-17
        "\uD833\uDD37", // 0b01000010 (66) - U+1CD37 BLOCK OCTANT-27
        "\uD833\uDD38", // 0b01000011 (67) - U+1CD38 BLOCK OCTANT-127
        "\uD833\uDD39", // 0b01000100 (68) - U+1CD39 BLOCK OCTANT-37
        "\uD833\uDD3A", // 0b01000101 (69) - U+1CD3A BLOCK OCTANT-137
        "\uD833\uDD3B", // 0b01000110 (70) - U+1CD3B BLOCK OCTANT-237
        "\uD833\uDD3C", // 0b01000111 (71) - U+1CD3C BLOCK OCTANT-1237
        "\uD833\uDD3D", // 0b01001000 (72) - U+1CD3D BLOCK OCTANT-47
        "\uD833\uDD3E", // 0b01001001 (73) - U+1CD3E BLOCK OCTANT-147
        "\uD833\uDD3F", // 0b01001010 (74) - U+1CD3F BLOCK OCTANT-247
        "\uD833\uDD40", // 0b01001011 (75) - U+1CD40 BLOCK OCTANT-1247
        "\uD833\uDD41", // 0b01001100 (76) - U+1CD41 BLOCK OCTANT-347
        "\uD833\uDD42", // 0b01001101 (77) - U+1CD42 BLOCK OCTANT-1347
        "\uD833\uDD43", // 0b01001110 (78) - U+1CD43 BLOCK OCTANT-2347
        "\uD833\uDD44", // 0b01001111 (79) - U+1CD44 BLOCK OCTANT-12347
        "▖", // 0b01010000 (80) - U+2596 QUADRANT LOWER LEFT (OCTANT-57)
        "\uD833\uDD45", // 0b01010001 (81) - U+1CD45 BLOCK OCTANT-157
        "\uD833\uDD46", // 0b01010010 (82) - U+1CD46 BLOCK OCTANT-257
        "\uD833\uDD47", // 0b01010011 (83) - U+1CD47 BLOCK OCTANT-1257
        "\uD833\uDD48", // 0b01010100 (84) - U+1CD48 BLOCK OCTANT-357
        "▌", // 0b01010101 (85) - U+258C LEFT HALF BLOCK (OCTANT-1357)
        "\uD833\uDD49", // 0b01010110 (86) - U+1CD49 BLOCK OCTANT-2357
        "\uD833\uDD4A", // 0b01010111 (87) - U+1CD4A BLOCK OCTANT-12357
        "\uD833\uDD4B", // 0b01011000 (88) - U+1CD4B BLOCK OCTANT-457
        "\uD833\uDD4C", // 0b01011001 (89) - U+1CD4C BLOCK OCTANT-1457
        "▞", // 0b01011010 (90) - U+259E QUADRANT UPPER RIGHT AND LOWER LEFT (OCTANT-2457)
        "\uD833\uDD4D", // 0b01011011 (91) - U+1CD4D BLOCK OCTANT-12457
        "\uD833\uDD4E", // 0b01011100 (92) - U+1CD4E BLOCK OCTANT-3457
        "\uD833\uDD4F", // 0b01011101 (93) - U+1CD4F BLOCK OCTANT-13457
        "\uD833\uDD50", // 0b01011110 (94) - U+1CD50 BLOCK OCTANT-23457
        "▛", // 0b01011111 (95) - U+259B QUADRANT UL AND UR AND LL (OCTANT-123457)
        "\uD833\uDD51", // 0b01100000 (96) - U+1CD51 BLOCK OCTANT-67
        "\uD833\uDD52", // 0b01100001 (97) - U+1CD52 BLOCK OCTANT-167
        "\uD833\uDD53", // 0b01100010 (98) - U+1CD53 BLOCK OCTANT-267
        "\uD833\uDD54", // 0b01100011 (99) - U+1CD54 BLOCK OCTANT-1267
        "\uD833\uDD55", // 0b01100100 (100) - U+1CD55 BLOCK OCTANT-367
        "\uD833\uDD56", // 0b01100101 (101) - U+1CD56 BLOCK OCTANT-1367
        "\uD833\uDD57", // 0b01100110 (102) - U+1CD57 BLOCK OCTANT-2367
        "\uD833\uDD58", // 0b01100111 (103) - U+1CD58 BLOCK OCTANT-12367
        "\uD833\uDD59", // 0b01101000 (104) - U+1CD59 BLOCK OCTANT-467
        "\uD833\uDD5A", // 0b01101001 (105) - U+1CD5A BLOCK OCTANT-1467
        "\uD833\uDD5B", // 0b01101010 (106) - U+1CD5B BLOCK OCTANT-2467
        "\uD833\uDD5C", // 0b01101011 (107) - U+1CD5C BLOCK OCTANT-12467
        "\uD833\uDD5D", // 0b01101100 (108) - U+1CD5D BLOCK OCTANT-3467
        "\uD833\uDD5E", // 0b01101101 (109) - U+1CD5E BLOCK OCTANT-13467
        "\uD833\uDD5F", // 0b01101110 (110) - U+1CD5F BLOCK OCTANT-23467
        "\uD833\uDD60", // 0b01101111 (111) - U+1CD60 BLOCK OCTANT-123467
        "\uD833\uDD61", // 0b01110000 (112) - U+1CD61 BLOCK OCTANT-567
        "\uD833\uDD62", // 0b01110001 (113) - U+1CD62 BLOCK OCTANT-1567
        "\uD833\uDD63", // 0b01110010 (114) - U+1CD63 BLOCK OCTANT-2567
        "\uD833\uDD64", // 0b01110011 (115) - U+1CD64 BLOCK OCTANT-12567
        "\uD833\uDD65", // 0b01110100 (116) - U+1CD65 BLOCK OCTANT-3567
        "\uD833\uDD66", // 0b01110101 (117) - U+1CD66 BLOCK OCTANT-13567
        "\uD833\uDD67", // 0b01110110 (118) - U+1CD67 BLOCK OCTANT-23567
        "\uD833\uDD68", // 0b01110111 (119) - U+1CD68 BLOCK OCTANT-123567
        "\uD833\uDD69", // 0b01111000 (120) - U+1CD69 BLOCK OCTANT-4567
        "\uD833\uDD6A", // 0b01111001 (121) - U+1CD6A BLOCK OCTANT-14567
        "\uD833\uDD6B", // 0b01111010 (122) - U+1CD6B BLOCK OCTANT-24567
        "\uD833\uDD6C", // 0b01111011 (123) - U+1CD6C BLOCK OCTANT-124567
        "\uD833\uDD6D", // 0b01111100 (124) - U+1CD6D BLOCK OCTANT-34567
        "\uD833\uDD6E", // 0b01111101 (125) - U+1CD6E BLOCK OCTANT-134567
        "\uD833\uDD6F", // 0b01111110 (126) - U+1CD6F BLOCK OCTANT-234567
        "\uD833\uDD70", // 0b01111111 (127) - U+1CD70 BLOCK OCTANT-1234567
        "\uD833\uDEA0", // 0b10000000 (128) - U+1CEA0 RIGHT HALF LOWER ONE QUARTER BLOCK (OCTANT-8)
        "\uD833\uDD71", // 0b10000001 (129) - U+1CD71 BLOCK OCTANT-18
        "\uD833\uDD72", // 0b10000010 (130) - U+1CD72 BLOCK OCTANT-28
        "\uD833\uDD73", // 0b10000011 (131) - U+1CD73 BLOCK OCTANT-128
        "\uD833\uDD74", // 0b10000100 (132) - U+1CD74 BLOCK OCTANT-38
        "\uD833\uDD75", // 0b10000101 (133) - U+1CD75 BLOCK OCTANT-138
        "\uD833\uDD76", // 0b10000110 (134) - U+1CD76 BLOCK OCTANT-238
        "\uD833\uDD77", // 0b10000111 (135) - U+1CD77 BLOCK OCTANT-1238
        "\uD833\uDD78", // 0b10001000 (136) - U+1CD78 BLOCK OCTANT-48
        "\uD833\uDD79", // 0b10001001 (137) - U+1CD79 BLOCK OCTANT-148
        "\uD833\uDD7A", // 0b10001010 (138) - U+1CD7A BLOCK OCTANT-248
        "\uD833\uDD7B", // 0b10001011 (139) - U+1CD7B BLOCK OCTANT-1248
        "\uD833\uDD7C", // 0b10001100 (140) - U+1CD7C BLOCK OCTANT-348
        "\uD833\uDD7D", // 0b10001101 (141) - U+1CD7D BLOCK OCTANT-1348
        "\uD833\uDD7E", // 0b10001110 (142) - U+1CD7E BLOCK OCTANT-2348
        "\uD833\uDD7F", // 0b10001111 (143) - U+1CD7F BLOCK OCTANT-12348
        "\uD833\uDD80", // 0b10010000 (144) - U+1CD80 BLOCK OCTANT-58
        "\uD833\uDD81", // 0b10010001 (145) - U+1CD81 BLOCK OCTANT-158
        "\uD833\uDD82", // 0b10010010 (146) - U+1CD82 BLOCK OCTANT-258
        "\uD833\uDD83", // 0b10010011 (147) - U+1CD83 BLOCK OCTANT-1258
        "\uD833\uDD84", // 0b10010100 (148) - U+1CD84 BLOCK OCTANT-358
        "\uD833\uDD85", // 0b10010101 (149) - U+1CD85 BLOCK OCTANT-1358
        "\uD833\uDD86", // 0b10010110 (150) - U+1CD86 BLOCK OCTANT-2358
        "\uD833\uDD87", // 0b10010111 (151) - U+1CD87 BLOCK OCTANT-12358
        "\uD833\uDD88", // 0b10011000 (152) - U+1CD88 BLOCK OCTANT-458
        "\uD833\uDD89", // 0b10011001 (153) - U+1CD89 BLOCK OCTANT-1458
        "\uD833\uDD8A", // 0b10011010 (154) - U+1CD8A BLOCK OCTANT-2458
        "\uD833\uDD8B", // 0b10011011 (155) - U+1CD8B BLOCK OCTANT-12458
        "\uD833\uDD8C", // 0b10011100 (156) - U+1CD8C BLOCK OCTANT-3458
        "\uD833\uDD8D", // 0b10011101 (157) - U+1CD8D BLOCK OCTANT-13458
        "\uD833\uDD8E", // 0b10011110 (158) - U+1CD8E BLOCK OCTANT-23458
        "\uD833\uDD8F", // 0b10011111 (159) - U+1CD8F BLOCK OCTANT-123458
        "▗", // 0b10100000 (160) - U+2597 QUADRANT LOWER RIGHT (OCTANT-68)
        "\uD833\uDD90", // 0b10100001 (161) - U+1CD90 BLOCK OCTANT-168
        "\uD833\uDD91", // 0b10100010 (162) - U+1CD91 BLOCK OCTANT-268
        "\uD833\uDD92", // 0b10100011 (163) - U+1CD92 BLOCK OCTANT-1268
        "\uD833\uDD93", // 0b10100100 (164) - U+1CD93 BLOCK OCTANT-368
        "▚", // 0b10100101 (165) - U+259A QUADRANT UPPER LEFT AND LOWER RIGHT (OCTANT-1368)
        "\uD833\uDD94", // 0b10100110 (166) - U+1CD94 BLOCK OCTANT-2368
        "\uD833\uDD95", // 0b10100111 (167) - U+1CD95 BLOCK OCTANT-12368
        "\uD833\uDD96", // 0b10101000 (168) - U+1CD96 BLOCK OCTANT-468
        "\uD833\uDD97", // 0b10101001 (169) - U+1CD97 BLOCK OCTANT-1468
        "▐", // 0b10101010 (170) - U+2590 RIGHT HALF BLOCK (OCTANT-2468)
        "\uD833\uDD98", // 0b10101011 (171) - U+1CD98 BLOCK OCTANT-12468
        "\uD833\uDD99", // 0b10101100 (172) - U+1CD99 BLOCK OCTANT-3468
        "\uD833\uDD9A", // 0b10101101 (173) - U+1CD9A BLOCK OCTANT-13468
        "\uD833\uDD9B", // 0b10101110 (174) - U+1CD9B BLOCK OCTANT-23468
        "▜", // 0b10101111 (175) - U+259C QUADRANT UL AND UR AND LR (OCTANT-123468)
        "\uD833\uDD9C", // 0b10110000 (176) - U+1CD9C BLOCK OCTANT-568
        "\uD833\uDD9D", // 0b10110001 (177) - U+1CD9D BLOCK OCTANT-1568
        "\uD833\uDD9E", // 0b10110010 (178) - U+1CD9E BLOCK OCTANT-2568
        "\uD833\uDD9F", // 0b10110011 (179) - U+1CD9F BLOCK OCTANT-12568
        "\uD833\uDDA0", // 0b10110100 (180) - U+1CDA0 BLOCK OCTANT-3568
        "\uD833\uDDA1", // 0b10110101 (181) - U+1CDA1 BLOCK OCTANT-13568
        "\uD833\uDDA2", // 0b10110110 (182) - U+1CDA2 BLOCK OCTANT-23568
        "\uD833\uDDA3", // 0b10110111 (183) - U+1CDA3 BLOCK OCTANT-123568
        "\uD833\uDDA4", // 0b10111000 (184) - U+1CDA4 BLOCK OCTANT-4568
        "\uD833\uDDA5", // 0b10111001 (185) - U+1CDA5 BLOCK OCTANT-14568
        "\uD833\uDDA6", // 0b10111010 (186) - U+1CDA6 BLOCK OCTANT-24568
        "\uD833\uDDA7", // 0b10111011 (187) - U+1CDA7 BLOCK OCTANT-124568
        "\uD833\uDDA8", // 0b10111100 (188) - U+1CDA8 BLOCK OCTANT-34568
        "\uD833\uDDA9", // 0b10111101 (189) - U+1CDA9 BLOCK OCTANT-134568
        "\uD833\uDDAA", // 0b10111110 (190) - U+1CDAA BLOCK OCTANT-234568
        "\uD833\uDDAB", // 0b10111111 (191) - U+1CDAB BLOCK OCTANT-1234568
        "▂", // 0b11000000 (192) - U+2582 LOWER ONE QUARTER BLOCK (OCTANT-78)
        "\uD833\uDDAC", // 0b11000001 (193) - U+1CDAC BLOCK OCTANT-178
        "\uD833\uDDAD", // 0b11000010 (194) - U+1CDAD BLOCK OCTANT-278
        "\uD833\uDDAE", // 0b11000011 (195) - U+1CDAE BLOCK OCTANT-1278
        "\uD833\uDDAF", // 0b11000100 (196) - U+1CDAF BLOCK OCTANT-378
        "\uD833\uDDB0", // 0b11000101 (197) - U+1CDB0 BLOCK OCTANT-1378
        "\uD833\uDDB1", // 0b11000110 (198) - U+1CDB1 BLOCK OCTANT-2378
        "\uD833\uDDB2", // 0b11000111 (199) - U+1CDB2 BLOCK OCTANT-12378
        "\uD833\uDDB3", // 0b11001000 (200) - U+1CDB3 BLOCK OCTANT-478
        "\uD833\uDDB4", // 0b11001001 (201) - U+1CDB4 BLOCK OCTANT-1478
        "\uD833\uDDB5", // 0b11001010 (202) - U+1CDB5 BLOCK OCTANT-2478
        "\uD833\uDDB6", // 0b11001011 (203) - U+1CDB6 BLOCK OCTANT-12478
        "\uD833\uDDB7", // 0b11001100 (204) - U+1CDB7 BLOCK OCTANT-3478
        "\uD833\uDDB8", // 0b11001101 (205) - U+1CDB8 BLOCK OCTANT-13478
        "\uD833\uDDB9", // 0b11001110 (206) - U+1CDB9 BLOCK OCTANT-23478
        "\uD833\uDDBA", // 0b11001111 (207) - U+1CDBA BLOCK OCTANT-123478
        "\uD833\uDDBB", // 0b11010000 (208) - U+1CDBB BLOCK OCTANT-578
        "\uD833\uDDBC", // 0b11010001 (209) - U+1CDBC BLOCK OCTANT-1578
        "\uD833\uDDBD", // 0b11010010 (210) - U+1CDBD BLOCK OCTANT-2578
        "\uD833\uDDBE", // 0b11010011 (211) - U+1CDBE BLOCK OCTANT-12578
        "\uD833\uDDBF", // 0b11010100 (212) - U+1CDBF BLOCK OCTANT-3578
        "\uD833\uDDC0", // 0b11010101 (213) - U+1CDC0 BLOCK OCTANT-13578
        "\uD833\uDDC1", // 0b11010110 (214) - U+1CDC1 BLOCK OCTANT-23578
        "\uD833\uDDC2", // 0b11010111 (215) - U+1CDC2 BLOCK OCTANT-123578
        "\uD833\uDDC3", // 0b11011000 (216) - U+1CDC3 BLOCK OCTANT-4578
        "\uD833\uDDC4", // 0b11011001 (217) - U+1CDC4 BLOCK OCTANT-14578
        "\uD833\uDDC5", // 0b11011010 (218) - U+1CDC5 BLOCK OCTANT-24578
        "\uD833\uDDC6", // 0b11011011 (219) - U+1CDC6 BLOCK OCTANT-124578
        "\uD833\uDDC7", // 0b11011100 (220) - U+1CDC7 BLOCK OCTANT-34578
        "\uD833\uDDC8", // 0b11011101 (221) - U+1CDC8 BLOCK OCTANT-134578
        "\uD833\uDDC9", // 0b11011110 (222) - U+1CDC9 BLOCK OCTANT-234578
        "\uD833\uDDCA", // 0b11011111 (223) - U+1CDCA BLOCK OCTANT-1234578
        "\uD833\uDDCB", // 0b11100000 (224) - U+1CDCB BLOCK OCTANT-678
        "\uD833\uDDCC", // 0b11100001 (225) - U+1CDCC BLOCK OCTANT-1678
        "\uD833\uDDCD", // 0b11100010 (226) - U+1CDCD BLOCK OCTANT-2678
        "\uD833\uDDCE", // 0b11100011 (227) - U+1CDCE BLOCK OCTANT-12678
        "\uD833\uDDCF", // 0b11100100 (228) - U+1CDCF BLOCK OCTANT-3678
        "\uD833\uDDD0", // 0b11100101 (229) - U+1CDD0 BLOCK OCTANT-13678
        "\uD833\uDDD1", // 0b11100110 (230) - U+1CDD1 BLOCK OCTANT-23678
        "\uD833\uDDD2", // 0b11100111 (231) - U+1CDD2 BLOCK OCTANT-123678
        "\uD833\uDDD3", // 0b11101000 (232) - U+1CDD3 BLOCK OCTANT-4678
        "\uD833\uDDD4", // 0b11101001 (233) - U+1CDD4 BLOCK OCTANT-14678
        "\uD833\uDDD5", // 0b11101010 (234) - U+1CDD5 BLOCK OCTANT-24678
        "\uD833\uDDD6", // 0b11101011 (235) - U+1CDD6 BLOCK OCTANT-124678
        "\uD833\uDDD7", // 0b11101100 (236) - U+1CDD7 BLOCK OCTANT-34678
        "\uD833\uDDD8", // 0b11101101 (237) - U+1CDD8 BLOCK OCTANT-134678
        "\uD833\uDDD9", // 0b11101110 (238) - U+1CDD9 BLOCK OCTANT-234678
        "\uD833\uDDDA", // 0b11101111 (239) - U+1CDDA BLOCK OCTANT-1234678
        "▄", // 0b11110000 (240) - U+2584 LOWER HALF BLOCK (OCTANT-5678)
        "\uD833\uDDDB", // 0b11110001 (241) - U+1CDDB BLOCK OCTANT-15678
        "\uD833\uDDDC", // 0b11110010 (242) - U+1CDDC BLOCK OCTANT-25678
        "\uD833\uDDDD", // 0b11110011 (243) - U+1CDDD BLOCK OCTANT-125678
        "\uD833\uDDDE", // 0b11110100 (244) - U+1CDDE BLOCK OCTANT-35678
        "▙", // 0b11110101 (245) - U+2599 QUADRANT UL AND LL AND LR (OCTANT-135678)
        "\uD833\uDDDF", // 0b11110110 (246) - U+1CDDF BLOCK OCTANT-235678
        "\uD833\uDDE0", // 0b11110111 (247) - U+1CDE0 BLOCK OCTANT-1235678
        "\uD833\uDDE1", // 0b11111000 (248) - U+1CDE1 BLOCK OCTANT-45678
        "\uD833\uDDE2", // 0b11111001 (249) - U+1CDE2 BLOCK OCTANT-145678
        "▟", // 0b11111010 (250) - U+259F QUADRANT UR AND LL AND LR (OCTANT-245678)
        "\uD833\uDDE3", // 0b11111011 (251) - U+1CDE3 BLOCK OCTANT-1245678
        "▆", // 0b11111100 (252) - U+2586 LOWER THREE QUARTERS BLOCK (OCTANT-345678)
        "\uD833\uDDE4", // 0b11111101 (253) - U+1CDE4 BLOCK OCTANT-1345678
        "\uD833\uDDE5", // 0b11111110 (254) - U+1CDE5 BLOCK OCTANT-2345678
        "█" // 0b11111111 (255) - U+2588 FULL BLOCK (OCTANT-12345678)
    };

    /**
     * Creates a block encoder with the specified mode, image, and font size.
     *
     * @param mode the block rendering mode
     * @param image the image to encode
     * @param targetWidth the initial target width in terminal columns
     * @param targetHeight the initial target height in terminal rows
     * @param fitImage the initial fit mode
     */
    protected BlockEncoder(
            @NonNull BlockMode mode,
            @NonNull BufferedImage image,
            int targetWidth,
            int targetHeight,
            boolean fitImage) {
        if (mode == null) {
            throw new IllegalArgumentException("Mode cannot be null");
        }
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }
        if (targetWidth <= 0) {
            throw new IllegalArgumentException("Target width must be positive");
        }
        if (targetHeight <= 0) {
            throw new IllegalArgumentException("Target height must be positive");
        }
        this.mode = mode;
        this.image = image;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.fitImage = fitImage;
    }

    /**
     * Creates a block encoder with half-block mode (most compatible).
     *
     * @param image the image to encode
     * @param targetWidth the initial target width in terminal columns
     * @param targetHeight the initial target height in terminal rows
     * @param fitImage the initial fit mode
     */
    public BlockEncoder(
            @NonNull BufferedImage image, int targetWidth, int targetHeight, boolean fitImage) {
        this(BlockMode.HALF, image, targetWidth, targetHeight, fitImage);
    }

    /**
     * Gets the block rendering mode used by this encoder.
     *
     * @return the block mode (FULL, HALF, QUADRANT, SEXTANT, or OCTANT)
     */
    public @NonNull BlockMode mode() {
        return mode;
    }

    @Override
    public @NonNull ImageEncoder targetSize(int targetWidth, int targetHeight) {
        if (targetWidth <= 0) {
            throw new IllegalArgumentException("Target width must be positive");
        }
        if (targetHeight <= 0) {
            throw new IllegalArgumentException("Target height must be positive");
        }
        if (this.targetWidth != targetWidth || this.targetHeight != targetHeight) {
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
            this.scaledImage = null; // Invalidate cache
        }
        return this;
    }

    @Override
    public int targetWidth() {
        return targetWidth;
    }

    @Override
    public int targetHeight() {
        return targetHeight;
    }

    @Override
    public @NonNull ImageEncoder fitImage(boolean fitImage) {
        if (this.fitImage != fitImage) {
            this.fitImage = fitImage;
            this.scaledImage = null; // Invalidate cache
        }
        return this;
    }

    @Override
    public boolean fitImage() {
        return fitImage;
    }

    @Override
    public void render(@NonNull Appendable output) throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("Output cannot be null");
        }

        // Lazily compute and cache the scaled image
        if (scaledImage == null) {
            // Calculate the physical pixel dimensions of the terminal area
            // This accounts for the actual font size (e.g., 8x16 pixels per cell)
            Resolution fontSize = FontSize.defaultFontSize();
            int physicalWidth = targetWidth * fontSize.x;
            int physicalHeight = targetHeight * fontSize.y;

            // Scale image to match the physical dimensions (preserving aspect ratio or fitting
            // exactly)
            scaledImage = ImageUtils.scaleImage(image, physicalWidth, physicalHeight, fitImage);
        }

        // Calculate how many cells the scaled image actually fills
        // (aspect ratio preservation may leave the image smaller in one dimension)
        Resolution fontSize = FontSize.defaultFontSize();
        int actualCols =
                Math.min(
                        (int) Math.ceil((double) scaledImage.getWidth() / fontSize.x), targetWidth);
        int actualRows =
                Math.min(
                        (int) Math.ceil((double) scaledImage.getHeight() / fontSize.y),
                        targetHeight);

        // Render using only the cells covered by the image
        renderBlocks(scaledImage, actualCols, actualRows, output);
    }

    /**
     * Renders the scaled image using block characters.
     *
     * @param image the scaled image
     * @param targetWidth the target width in terminal columns
     * @param targetHeight the target height in terminal rows
     * @param output the output to write to
     * @throws IOException if an I/O error occurs
     */
    private void renderBlocks(
            @NonNull BufferedImage image,
            int targetWidth,
            int targetHeight,
            @NonNull Appendable output)
            throws IOException {

        int cols = mode.columns();
        int rows = mode.rows();

        // Calculate how many physical pixels each sub-pixel represents
        Resolution fontSize = FontSize.defaultFontSize();
        double pixelsPerSubPixelX = (double) fontSize.x / cols;
        double pixelsPerSubPixelY = (double) fontSize.y / rows;

        for (int cellRow = 0; cellRow < targetHeight; cellRow++) {
            for (int cellCol = 0; cellCol < targetWidth; cellCol++) {
                // Sample pixels for this cell
                int[] pixels =
                        sampleCell(image, cellCol, cellRow, pixelsPerSubPixelX, pixelsPerSubPixelY);

                // Find the two best representative colors
                ColorPair colors = findBestColorPair(pixels);

                // Determine which pixels belong to foreground vs background
                int pattern = determinePattern(pixels, colors);

                // Get the appropriate block character
                String blockChar = getBlockCharacter(pattern);

                // Output the character with colors
                outputCell(output, blockChar, colors);
            }
            // Reset colors at the end of each line to prevent bleeding
            output.append(AnsiUtils.STYLE_RESET);
            if (cellRow < targetHeight - 1) {
                output.append('\n');
            }
        }
    }

    /**
     * Samples the pixels for a single cell.
     *
     * @param image the image to sample from
     * @param cellCol the cell column
     * @param cellRow the cell row
     * @param pixelsPerSubPixelX physical pixels per sub-pixel in X direction
     * @param pixelsPerSubPixelY physical pixels per sub-pixel in Y direction
     * @return array of RGB pixel values
     */
    private int[] sampleCell(
            @NonNull BufferedImage image,
            int cellCol,
            int cellRow,
            double pixelsPerSubPixelX,
            double pixelsPerSubPixelY) {
        int cols = mode.columns();
        int rows = mode.rows();
        int[] pixels = new int[cols * rows];

        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                // Calculate sub-pixel coordinates
                int subPixelX = cellCol * cols + col;
                int subPixelY = cellRow * rows + row;

                // Map to physical pixel coordinates
                int x = (int) (subPixelX * pixelsPerSubPixelX);
                int y = (int) (subPixelY * pixelsPerSubPixelY);

                // Clamp coordinates to image bounds
                x = Math.min(x, imgWidth - 1);
                y = Math.min(y, imgHeight - 1);

                pixels[row * cols + col] = image.getRGB(x, y);
            }
        }

        return pixels;
    }

    /**
     * Finds the best two representative colors for the given pixels using color clustering.
     *
     * @param pixels array of RGB pixel values
     * @return the foreground and background colors
     */
    private @NonNull ColorPair findBestColorPair(int[] pixels) {
        // Simple k-means clustering with k=2
        // Initialize with darkest and brightest pixels
        int darkest = 0xFFFFFF;
        int brightest = 0x000000;

        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i];
            int brightness = getBrightness(rgb);

            if (brightness < getBrightness(darkest)) {
                darkest = rgb;
            }
            if (brightness > getBrightness(brightest)) {
                brightest = rgb;
            }
        }

        // Perform a few iterations of k-means
        int color1 = darkest;
        int color2 = brightest;

        for (int iter = 0; iter < 3; iter++) {
            long sumR1 = 0, sumG1 = 0, sumB1 = 0, count1 = 0;
            long sumR2 = 0, sumG2 = 0, sumB2 = 0, count2 = 0;

            for (int pixel : pixels) {
                if (colorDistance(pixel, color1) < colorDistance(pixel, color2)) {
                    sumR1 += (pixel >> 16) & 0xFF;
                    sumG1 += (pixel >> 8) & 0xFF;
                    sumB1 += pixel & 0xFF;
                    count1++;
                } else {
                    sumR2 += (pixel >> 16) & 0xFF;
                    sumG2 += (pixel >> 8) & 0xFF;
                    sumB2 += pixel & 0xFF;
                    count2++;
                }
            }

            if (count1 > 0) {
                color1 =
                        ((int) (sumR1 / count1) << 16)
                                | ((int) (sumG1 / count1) << 8)
                                | (int) (sumB1 / count1);
            }
            if (count2 > 0) {
                color2 =
                        ((int) (sumR2 / count2) << 16)
                                | ((int) (sumG2 / count2) << 8)
                                | (int) (sumB2 / count2);
            }
        }

        return new ColorPair(color1, color2);
    }

    /**
     * Determines the bit pattern for which pixels belong to the foreground color.
     *
     * @param pixels array of RGB pixel values
     * @param colors the foreground and background colors
     * @return bit pattern where 1 = foreground, 0 = background
     */
    private int determinePattern(int[] pixels, @NonNull ColorPair colors) {
        int pattern = 0;
        for (int i = 0; i < pixels.length; i++) {
            if (colorDistance(pixels[i], colors.foreground)
                    < colorDistance(pixels[i], colors.background)) {
                pattern |= (1 << i);
            }
        }
        return pattern;
    }

    /**
     * Gets the appropriate block character for the given pattern.
     *
     * @param pattern the bit pattern
     * @return the Unicode block character
     */
    private @NonNull String getBlockCharacter(int pattern) {
        switch (mode) {
            case FULL:
                return FULL_BLOCKS[pattern & 0x1];
            case HALF:
                return HALF_BLOCKS[pattern & 0x3];
            case QUADRANT:
                return QUADRANT_BLOCKS[pattern & 0xF];
            case SEXTANT:
                return SEXTANT_BLOCKS[pattern & 0x3F];
            case OCTANT:
                return OCTANT_BLOCKS[pattern & 0xFF];
            default:
                return " ";
        }
    }

    /**
     * Outputs a cell with the specified character and colors.
     *
     * @param output the output to write to
     * @param blockChar the block character
     * @param colors the foreground and background colors
     * @throws IOException if an I/O error occurs
     */
    private void outputCell(
            @NonNull Appendable output, @NonNull String blockChar, @NonNull ColorPair colors)
            throws IOException {
        // Set foreground color
        int fgR = (colors.foreground >> 16) & 0xFF;
        int fgG = (colors.foreground >> 8) & 0xFF;
        int fgB = colors.foreground & 0xFF;

        // Set background color
        int bgR = (colors.background >> 16) & 0xFF;
        int bgG = (colors.background >> 8) & 0xFF;
        int bgB = colors.background & 0xFF;

        output.append(AnsiUtils.rgbFg(fgR, fgG, fgB));
        output.append(AnsiUtils.rgbBg(bgR, bgG, bgB));
        output.append(blockChar);
    }

    /**
     * Calculates the brightness of an RGB color.
     *
     * @param rgb the RGB value
     * @return the brightness (0-255)
     */
    private int getBrightness(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        // Use perceived brightness formula
        return (int) (0.299 * r + 0.587 * g + 0.114 * b);
    }

    /**
     * Calculates the distance between two RGB colors.
     *
     * @param rgb1 first RGB value
     * @param rgb2 second RGB value
     * @return the color distance
     */
    private int colorDistance(int rgb1, int rgb2) {
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;

        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;

        int dr = r1 - r2;
        int dg = g1 - g2;
        int db = b1 - b2;

        return dr * dr + dg * dg + db * db;
    }

    /** Helper class to hold a pair of colors (foreground and background). */
    private static class ColorPair {
        final int foreground;
        final int background;

        ColorPair(int foreground, int background) {
            this.foreground = foreground;
            this.background = background;
        }
    }

    /** Provider for creating BlockEncoder instances. */
    public static class Provider implements ImageEncoder.Provider {
        private final @NonNull BlockMode mode;

        public Provider(@NonNull BlockMode mode) {
            this.mode = mode;
        }

        @Override
        public @NonNull String name() {
            return "block-" + mode.name().toLowerCase();
        }

        @Override
        public @NonNull Resolution resolution() {
            return new Resolution(mode.columns(), mode.rows());
        }

        @Override
        public @NonNull ImageEncoder create(
                @NonNull BufferedImage image, int targetWidth, int targetHeight, boolean fitImage) {
            return new BlockEncoder(mode, image, targetWidth, targetHeight, fitImage);
        }
    }
}
