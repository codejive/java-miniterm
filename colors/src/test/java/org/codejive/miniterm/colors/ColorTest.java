package org.codejive.miniterm.colors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ColorTest {

    // ── Construction ──────────────────────────────────────────────────────

    @Test
    void of_storesComponents() {
        Color.RgbColor c = Color.rgb(0x1234, 0x5678, 0x9ABC);
        assertThat(c.r()).isEqualTo(0x1234);
        assertThat(c.g()).isEqualTo(0x5678);
        assertThat(c.b()).isEqualTo(0x9ABC);
    }

    @Test
    void of_acceptsBoundaryValues() {
        Color.RgbColor black = Color.rgb(0, 0, 0);
        assertThat(black.r()).isZero();

        Color.RgbColor white = Color.rgb(65535, 65535, 65535);
        assertThat(white.r()).isEqualTo(65535);
    }

    @Test
    void rgb_rejectsOutOfRange() {
        assertThatThrownBy(() -> Color.rgb(-1, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Color.rgb(0, 65536, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Color.rgb(0, 0, 100000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rgb8_expandsToByte16() {
        Color.RgbColor c = Color.rgb8(0xFF, 0x80, 0x00);
        // byte-replication: 0xFF -> 0xFFFF, 0x80 -> 0x8080, 0x00 -> 0x0000
        assertThat(c.r()).isEqualTo(0xFFFF);
        assertThat(c.g()).isEqualTo(0x8080);
        assertThat(c.b()).isEqualTo(0x0000);
    }

    @Test
    void rgb8_rejectsOutOfRange() {
        assertThatThrownBy(() -> Color.rgb8(-1, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Color.rgb8(0, 256, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Downsampling ──────────────────────────────────────────────────────

    @Test
    void r8_g8_b8_downsampleCorrectly() {
        Color.RgbColor c = Color.rgb(0xABCD, 0x1234, 0x0000);
        assertThat(c.r8()).isEqualTo(0xAB);
        assertThat(c.g8()).isEqualTo(0x12);
        assertThat(c.b8()).isZero();
    }

    @Test
    void toRgb8_packsCorrectly() {
        Color.RgbColor c = Color.rgb(0xFF00, 0x8000, 0x0000);
        // r8=0xFF, g8=0x80, b8=0x00  →  0xFF8000
        assertThat(c.toRgb8()).isEqualTo(0xFF8000);
    }

    @Test
    void rgb8_roundTrip_via8bitGetters() {
        Color.RgbColor c = Color.rgb8(200, 100, 50);
        assertThat(c.r8()).isEqualTo(200);
        assertThat(c.g8()).isEqualTo(100);
        assertThat(c.b8()).isEqualTo(50);
    }

    // ── Parsing ───────────────────────────────────────────────────────────

    @Test
    void parse_4digit_roundTrip() {
        Color.RgbColor original = Color.rgb(0xABCD, 0x1234, 0xEF01);
        Color.RgbColor parsed = Color.RgbColor.parse(original.toOscCode());
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void parse_2digit_byteReplicated() {
        // rgb:FF/00/80  →  r=0xFFFF, g=0x0000, b=0x8080
        Color.RgbColor c = Color.RgbColor.parse("rgb:FF/00/80");
        assertThat(c).isNotNull();
        assertThat(c.r()).isEqualTo(0xFFFF);
        assertThat(c.g()).isEqualTo(0x0000);
        assertThat(c.b()).isEqualTo(0x8080);
    }

    @Test
    void parse_1digit_nibbleReplicated() {
        // rgb:F/0/8  →  r=0xFFFF, g=0x0000, b=0x8888
        Color.RgbColor c = Color.RgbColor.parse("rgb:F/0/8");
        assertThat(c).isNotNull();
        assertThat(c.r()).isEqualTo(0xFFFF);
        assertThat(c.g()).isEqualTo(0x0000);
        assertThat(c.b()).isEqualTo(0x8888);
    }

    @Test
    void parse_caseInsensitive() {
        Color.RgbColor lower = Color.RgbColor.parse("rgb:ffff/0000/abcd");
        Color.RgbColor upper = Color.RgbColor.parse("rgb:FFFF/0000/ABCD");
        assertThat(lower).isEqualTo(upper);
    }

    @Test
    void parse_leadingWhitespace_trimmed() {
        Color.RgbColor c = Color.RgbColor.parse("  rgb:FFFF/0000/0000  ");
        assertThat(c).isNotNull();
        assertThat(c.r()).isEqualTo(0xFFFF);
    }

    @Test
    void parse_null_returnsNull() {
        assertThat(Color.RgbColor.parse(null)).isNull();
    }

    @Test
    void parse_missingPrefix_returnsNull() {
        assertThat(Color.RgbColor.parse("FFFF/0000/0000")).isNull();
        assertThat(Color.RgbColor.parse("#FFFFFF")).isNull();
    }

    @Test
    void parse_wrongComponentCount_returnsNull() {
        assertThat(Color.RgbColor.parse("rgb:FFFF/0000")).isNull();
        assertThat(Color.RgbColor.parse("rgb:FFFF/0000/0000/FFFF")).isNull();
    }

    @Test
    void parse_invalidHex_returnsNull() {
        assertThat(Color.RgbColor.parse("rgb:GGGG/0000/0000")).isNull();
        assertThat(Color.RgbColor.parse("rgb:FFFFF/0000/0000")).isNull();
    }

    // ── toString ──────────────────────────────────────────────────────────

    @Test
    void toString_producesCanonicalForm() {
        Color.RgbColor c = Color.rgb(0xABCD, 0x0123, 0xFFFF);
        assertThat(c.toOscCode()).isEqualTo("rgb:ABCD/0123/FFFF");
    }

    @Test
    void toString_padsToFourDigits() {
        Color.RgbColor c = Color.rgb(0x000F, 0x0000, 0x0001);
        assertThat(c.toOscCode()).isEqualTo("rgb:000F/0000/0001");
    }

    // ── Equality ─────────────────────────────────────────────────────────

    @Test
    void equals_sameValues_areEqual() {
        Color.RgbColor a = Color.rgb(0x1234, 0x5678, 0x9ABC);
        Color.RgbColor b = Color.rgb(0x1234, 0x5678, 0x9ABC);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_differentValues_areNotEqual() {
        Color.RgbColor a = Color.rgb(0x1234, 0x5678, 0x9ABC);
        Color.RgbColor b = Color.rgb(0x1234, 0x5678, 0x0000);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_null_returnsFalse() {
        assertThat(Color.rgb(0, 0, 0)).isNotEqualTo(null);
    }
}
