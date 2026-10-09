package software.consistent.model.util

@IgnorableReturnValue
internal fun StringBuilder.appendIndented(value: Any?, times: Int = 1, char: CharSequence = "\t") =
  appendIndented(value.toString(), times, char)

@IgnorableReturnValue
internal fun StringBuilder.appendIndented(text: String, times: Int = 1, char: CharSequence = "\t"): StringBuilder {
  val indent = char.repeat(times)
  append(indent)
  var lastAppendEnd = 0
  for (i in text.indices) {
    if (text[i] == '\n') {
      val end = i + 1
      appendRange(text, lastAppendEnd, end)
      append(indent)
      lastAppendEnd = end
    }
  }
  return appendRange(text, lastAppendEnd, text.length)
}
