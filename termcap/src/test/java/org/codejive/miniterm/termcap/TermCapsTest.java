package org.codejive.miniterm.termcap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codejive.miniterm.termcap.TermCapsTestHelper.detectTerm;
import static org.codejive.miniterm.termcap.TermCapsTestHelper.detectWithEnv;
import static org.codejive.miniterm.termcap.TermCapsTestHelper.detectWithEnvAndVersion;

import org.junit.jupiter.api.Test;

class TermCapsTest {

    // ── builder defaults ──────────────────────────────────────────────────────

    @Test
    void builderDefaultsAreAllOff() {
        TermCaps caps = TermCaps.builder().build();
        assertThat(caps.colors()).isZero();
        assertThat(caps.altScreen()).isFalse();
        assertThat(caps.mouse()).isFalse();
        assertThat(caps.bracketedPaste()).isFalse();
        assertThat(caps.focusTracking()).isFalse();
        assertThat(caps.synchronizedOutput()).isFalse();
        assertThat(caps.hyperlinks()).isFalse();
        assertThat(caps.settableTitle()).isFalse();
        assertThat(caps.unicode()).isFalse();
        assertThat(caps.italic()).isFalse();
        assertThat(caps.strikethrough()).isFalse();
        assertThat(caps.overline()).isFalse();
        assertThat(caps.kittyKeyboard()).isFalse();
        assertThat(caps.undercurl()).isFalse();
    }

    @Test
    void builderSetsEachCapability() {
        TermCaps caps =
                TermCaps.builder()
                        .colors(256)
                        .altScreen(true)
                        .mouse(true)
                        .bracketedPaste(true)
                        .focusTracking(true)
                        .synchronizedOutput(true)
                        .hyperlinks(true)
                        .settableTitle(true)
                        .unicode(true)
                        .italic(true)
                        .strikethrough(true)
                        .overline(true)
                        .kittyKeyboard(true)
                        .undercurl(true)
                        .build();
        assertThat(caps.colors()).isEqualTo(256);
        assertThat(caps.altScreen()).isTrue();
        assertThat(caps.mouse()).isTrue();
        assertThat(caps.bracketedPaste()).isTrue();
        assertThat(caps.focusTracking()).isTrue();
        assertThat(caps.synchronizedOutput()).isTrue();
        assertThat(caps.hyperlinks()).isTrue();
        assertThat(caps.settableTitle()).isTrue();
        assertThat(caps.unicode()).isTrue();
        assertThat(caps.italic()).isTrue();
        assertThat(caps.strikethrough()).isTrue();
        assertThat(caps.overline()).isTrue();
        assertThat(caps.kittyKeyboard()).isTrue();
        assertThat(caps.undercurl()).isTrue();
    }

    @Test
    void builderFromBaseCopiesAllFields() {
        TermCaps base = TermCaps.builder().colors(256).altScreen(true).mouse(true).build();
        TermCaps copy = TermCaps.builder(base).build();
        assertThat(copy.colors()).isEqualTo(256);
        assertThat(copy.altScreen()).isTrue();
        assertThat(copy.mouse()).isTrue();
    }

    @Test
    void builderFromBaseAllowsOverride() {
        TermCaps base = TermCaps.builder().colors(256).mouse(true).build();
        TermCaps override = TermCaps.builder(base).colors(0).mouse(false).build();
        assertThat(override.colors()).isZero();
        assertThat(override.mouse()).isFalse();
        // unrelated field preserved
        assertThat(override.altScreen()).isFalse();
    }

    // ── Layer 1: TERM env var ─────────────────────────────────────────────────

    @Test
    void termDumbGivesNoCapabilities() {
        TermCaps caps = detectTerm("dumb");
        assertThat(caps.colors()).isZero();
        assertThat(caps.altScreen()).isFalse();
    }

    @Test
    void termXtermGivesBaseline() {
        TermCaps caps = detectTerm("xterm");
        assertThat(caps.colors()).isEqualTo(8);
        assertThat(caps.altScreen()).isTrue();
        assertThat(caps.mouse()).isTrue();
        assertThat(caps.settableTitle()).isTrue();
        assertThat(caps.unicode()).isTrue();
    }

    @Test
    void termXterm256colorGives256Colors() {
        TermCaps caps = detectTerm("xterm-256color");
        assertThat(caps.colors()).isEqualTo(256);
        assertThat(caps.bracketedPaste()).isTrue();
        assertThat(caps.italic()).isTrue();
        assertThat(caps.strikethrough()).isTrue();
    }

    @Test
    void termXtermDirectGivesTruecolor() {
        TermCaps caps = detectTerm("xterm-direct");
        assertThat(caps.colors()).isEqualTo(16_777_216);
        assertThat(caps.overline()).isTrue();
    }

    @Test
    void termScreenGivesAltScreen() {
        TermCaps caps = detectTerm("screen");
        assertThat(caps.altScreen()).isTrue();
        assertThat(caps.colors()).isEqualTo(8);
    }

    @Test
    void termScreen256colorGives256Colors() {
        TermCaps caps = detectTerm("screen-256color");
        assertThat(caps.colors()).isEqualTo(256);
        assertThat(caps.bracketedPaste()).isTrue();
    }

    @Test
    void termTmux256colorGives256Colors() {
        TermCaps caps = detectTerm("tmux-256color");
        assertThat(caps.colors()).isEqualTo(256);
    }

    @Test
    void unknownTermSuffix256colorUpgradesColors() {
        TermCaps caps = detectTerm("someterm-256color");
        assertThat(caps.colors()).isEqualTo(256);
    }

    @Test
    void unknownTermSuffixDirectUpgradesTruecolor() {
        TermCaps caps = detectTerm("someterm-direct");
        assertThat(caps.colors()).isEqualTo(16_777_216);
    }

    // ── Layer 2: COLORTERM env var ────────────────────────────────────────────

    @Test
    void colortermTruecolorUpgradesToTruecolor() {
        TermCaps caps = detectWithEnv("xterm-256color", "truecolor", null, null, null);
        assertThat(caps.colors()).isEqualTo(16_777_216);
    }

    @Test
    void colorterm24bitUpgradesToTruecolor() {
        TermCaps caps = detectWithEnv("xterm-256color", "24bit", null, null, null);
        assertThat(caps.colors()).isEqualTo(16_777_216);
    }

    @Test
    void termProgramWezTermGivesFullCaps() {
        TermCaps caps = detectWithEnv("xterm-256color", null, "WezTerm", null, null);
        assertThat(caps.colors()).isEqualTo(16_777_216);
        assertThat(caps.synchronizedOutput()).isTrue();
        assertThat(caps.focusTracking()).isTrue();
        assertThat(caps.hyperlinks()).isTrue();
        assertThat(caps.overline()).isTrue();
    }

    @Test
    void termProgramITermGivesFullCaps() {
        TermCaps caps = detectWithEnv("xterm-256color", null, "iTerm.app", null, null);
        assertThat(caps.colors()).isEqualTo(16_777_216);
        assertThat(caps.hyperlinks()).isTrue();
        assertThat(caps.overline()).isTrue();
    }

    @Test
    void termProgramKittyGivesFullCaps() {
        TermCaps caps = detectWithEnv("xterm-256color", null, "kitty", null, null);
        assertThat(caps.colors()).isEqualTo(16_777_216);
        assertThat(caps.focusTracking()).isTrue();
    }

    // ── New capabilities: kittyKeyboard and undercurl ─────────────────────────

    @Test
    void builderSetsKittyKeyboard() {
        TermCaps caps = TermCaps.builder().kittyKeyboard(true).build();
        assertThat(caps.kittyKeyboard()).isTrue();
    }

    @Test
    void builderSetsUndercurl() {
        TermCaps caps = TermCaps.builder().undercurl(true).build();
        assertThat(caps.undercurl()).isTrue();
    }

    @Test
    void termProgramITermWithVersionSupportsKittyKeyboard() {
        TermCaps caps =
                detectWithEnvAndVersion("xterm-256color", null, "iTerm.app", "3.5.0", null, null);
        assertThat(caps.kittyKeyboard()).isTrue();
    }

    @Test
    void termProgramITermOldVersionDoesNotSupportKittyKeyboard() {
        TermCaps caps =
                detectWithEnvAndVersion("xterm-256color", null, "iTerm.app", "3.4.0", null, null);
        assertThat(caps.kittyKeyboard()).isFalse();
    }

    @Test
    void termProgramITermWithVersionSupportsUndercurl() {
        TermCaps caps =
                detectWithEnvAndVersion("xterm-256color", null, "iTerm.app", "3.4.0", null, null);
        assertThat(caps.undercurl()).isTrue();
    }

    @Test
    void termProgramKittyWithVersionSupportsKittyKeyboard() {
        TermCaps caps =
                detectWithEnvAndVersion("xterm-256color", null, "kitty", "0.20.0", null, null);
        assertThat(caps.kittyKeyboard()).isTrue();
        assertThat(caps.undercurl()).isTrue();
    }

    @Test
    void termProgramWezTermWithVersionSupportsKittyKeyboard() {
        TermCaps caps =
                detectWithEnvAndVersion("xterm-256color", null, "WezTerm", "20220101", null, null);
        assertThat(caps.kittyKeyboard()).isTrue();
        assertThat(caps.undercurl()).isTrue();
    }

    @Test
    void termProgramGhosttyWithVersionSupportsKittyKeyboard() {
        TermCaps caps =
                detectWithEnvAndVersion("xterm-256color", null, "Ghostty", "1.0.0", null, null);
        assertThat(caps.kittyKeyboard()).isTrue();
        assertThat(caps.undercurl()).isTrue();
    }

    @Test
    void termProgramAppleTerminalWithVersionSupportsUndercurl() {
        TermCaps caps =
                detectWithEnvAndVersion(
                        "xterm-256color", null, "Apple_Terminal", "2.10", null, null);
        assertThat(caps.undercurl()).isTrue();
    }

    @Test
    void termProgramAppleTerminalOldVersionDoesNotSupportUndercurl() {
        TermCaps caps =
                detectWithEnvAndVersion(
                        "xterm-256color", null, "Apple_Terminal", "2.9", null, null);
        assertThat(caps.undercurl()).isFalse();
    }
}
