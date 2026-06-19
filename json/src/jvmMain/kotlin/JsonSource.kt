package software.consistent.model.json

import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Parser
import io.github.treesitter.language.json.TreeSitterJson
import software.consistent.model.json.JsonSource.parse

public object JsonSource {
  public fun parse() {
    val language = Language(TreeSitterJson.language())
    val parser = Parser(language)
    val tree = parser.parse("fun main() {}")
    val rootNode = tree.rootNode

    assert(rootNode.type == "source_file")
    assert(rootNode.startPoint.column == 0u)
    assert(rootNode.endPoint.column == 13u)
  }
}

public fun main() {
  parse()
}
