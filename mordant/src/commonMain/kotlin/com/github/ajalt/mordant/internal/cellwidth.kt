package com.github.ajalt.mordant.internal

import com.github.ajalt.mordant.internal.gen.CELL_WIDTH_TABLE
import com.github.ajalt.mordant.internal.gen.EMOJI_SEQUENCES
import com.github.ajalt.mordant.internal.gen.IntTrie
import com.github.ajalt.mordant.internal.gen.couldStartEmojiSeq


/*
 * This implementation uses a binary search of a lookup table, similar to Markus Kuhn's classic C
 * implementation of wcwidth. This function differs from his in a few ways. We generate the lookup
 * table from the latest unicode standard. wcwidth effectively requires two codespace searches for
 * each codepoint. We perform at most a single binary search, and for ASCII characters, we don't
 * perform any search at all. wcwidth also returns -1 for most control codes, which is wrong for all
 * the use cases you'd use wcwidth for. A BEL character does not suddenly make your line of text
 * shorter. We return 0 for control codes other than DEL and BS.
 */

/**
 * Return the width, in terminal cells, of the given unicode [codepoint].
 */
internal fun cellWidth(codepoint: Int): Int {
    if (codepoint in 0x20..0x7e) return 1 // fast path for printable ASCII
    if (codepoint == 0x08 || codepoint == 0x7f) return -1 // DEL and BS

    val table = CELL_WIDTH_TABLE
    var min = 0
    var mid: Int
    var max = table.lastIndex

    if (codepoint < table[0].low || codepoint > table[max].high) return 1

    while (max >= min) {
        mid = (min + max) / 2
        val entry = table[mid]
        when {
            codepoint > entry.high -> min = mid + 1
            codepoint < entry.low -> max = mid - 1
            else -> return table[mid].width.toInt()
        }
    }

    return 1
}

/** Return the width, in terminal cells, of the given [string]*/
internal fun stringCellWidth(string: String): Int {
    var sum = 0
    var sumSinceZwj = 0
    var zwjSeq: IntTrie? = null
    var prevCodepoint = -1
    for (codepoint in codepointSequence(string)) {
        val width = when {
            // text-presentation selector: keep the default width of the base codepoint
            codepoint == 0xFE0E -> 0
            // emoji-presentation selector: widen text-presentation emoji to two cells
            codepoint == 0xFE0F && isTextPresentationEmoji(prevCodepoint) -> 1
            // some terminals render these as narrow by default unless FE0F is present
            isTextPresentationEmoji(codepoint) -> 1
            else -> cellWidth(codepoint)
        }
        if (zwjSeq != null) {
            sumSinceZwj += width
            if (codepoint in zwjSeq.values) {
                sumSinceZwj = 0
            }
            zwjSeq = zwjSeq.children[codepoint]
            if (zwjSeq == null) {
                // all ZWJ sequences combine to one glyph, which is always an emoji, so add 2 for the width of the
                // emoji, plus the width of any codepoints since the end of the last complete sequence. Unfortunately,
                // some of these emoji are wider than two cells, but given that their size is font-dependant and usually
                // not cell-aligned anyway, there's no perfect solution here. Thanks, unicode.
                sum += sumSinceZwj + 2
                sumSinceZwj = 0
            } else {
                sumSinceZwj += width
            }
        } else {
            // We do a fast range check to skip ZWJ sequence processing for most codepoints
            if (couldStartEmojiSeq(codepoint)) {
                zwjSeq = EMOJI_SEQUENCES.children[codepoint]
            }
            if (zwjSeq == null) {
                sum += width
            }
        }
        if (codepoint != 0xFE0E && codepoint != 0xFE0F) {
            prevCodepoint = codepoint
        }
    }
    // If we were in a zwj sequence at the end of the string, add whatever was left to the sum
    return sum + sumSinceZwj

}

private val TEXT_PRESENTATION_EMOJI: IntArray = intArrayOf(
    0x1F202,
    0x1F237,
    0x1F321,
    0x1F324, 0x1F325, 0x1F326, 0x1F327, 0x1F328, 0x1F329, 0x1F32A, 0x1F32B, 0x1F32C,
    0x1F336,
    0x1F37D,
    0x1F396, 0x1F397,
    0x1F399, 0x1F39A, 0x1F39B,
    0x1F39E, 0x1F39F,
    0x1F3CB, 0x1F3CC, 0x1F3CD, 0x1F3CE,
    0x1F3D4, 0x1F3D5, 0x1F3D6, 0x1F3D7, 0x1F3D8, 0x1F3D9, 0x1F3DA, 0x1F3DB, 0x1F3DC, 0x1F3DD, 0x1F3DE, 0x1F3DF,
    0x1F3F3,
    0x1F3F5,
    0x1F3F7,
    0x1F43F,
    0x1F441,
    0x1F4FD,
    0x1F549, 0x1F54A,
    0x1F56F, 0x1F570,
    0x1F573, 0x1F574, 0x1F575, 0x1F576, 0x1F577, 0x1F578, 0x1F579,
    0x1F587,
    0x1F58A, 0x1F58B, 0x1F58C, 0x1F58D,
    0x1F590,
    0x1F5A5,
    0x1F5A8,
    0x1F5B1, 0x1F5B2,
    0x1F5BC,
    0x1F5C2, 0x1F5C3, 0x1F5C4,
    0x1F5D1, 0x1F5D2, 0x1F5D3,
    0x1F5DC, 0x1F5DD, 0x1F5DE,
    0x1F5E1,
    0x1F5E3,
    0x1F5E8,
    0x1F5EF,
    0x1F5F3,
    0x1F5FA,
    0x1F6CB,
    0x1F6CD, 0x1F6CE, 0x1F6CF,
    0x1F6E0, 0x1F6E1, 0x1F6E2, 0x1F6E3, 0x1F6E4, 0x1F6E5,
    0x1F6E9,
    0x1F6F0,
    0x1F6F3,
)

private fun isTextPresentationEmoji(codepoint: Int): Boolean {
    return TEXT_PRESENTATION_EMOJI.binarySearch(codepoint) >= 0
}
