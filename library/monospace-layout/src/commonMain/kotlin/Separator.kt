package software.consistent.data.formatting.monospacelayout

import software.consistent.data.formatting.monospacelayout.LayoutSettings.Companion.NEWLINE_PATTERN

// TODO consider whether this is necessary
@ConsistentCopyVisibility
public data class Separator private constructor(
  internal val singleLine: CharSequenceWithWidth,
  internal val multiLine: CharSequenceWithWidth,
) {
  init {
    require(!singleLine.contains(NEWLINE_PATTERN)) { "${::singleLine.name} must not contain newlines!" }
    require(!multiLine.contains(NEWLINE_PATTERN)) { "${::multiLine.name} must not contain newlines!" }
  }

  public constructor(
    singleLine: CharSequence = "",
    multiLine: CharSequence = "",
  ) : this(CharSequenceWithCachedWidth(singleLine), CharSequenceWithCachedWidth(multiLine))
}
