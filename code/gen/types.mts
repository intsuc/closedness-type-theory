export type LanguageConfiguration = {
  comments?: {
    lineComment?: string,
    blockComment?: [string, string],
  },
  brackets?: [string, string][],
  autoClosingPairs?: (
    | [string, string]
    | { open: string, close: string, notIn?: string[] }
  )[],
  autoCloseBefore?: string,
  surroundingPairs?: [string, string][],
  folding?: {
    markers?: {
      start: string,
      end: string,
    },
    offSide?: boolean,
  },
  wordPattern?: string,
  indentationRules?: {
    increaseIndentPattern?: string,
    decreaseIndentPattern?: string,
    indentNextLinePattern?: string,
    unIndentedLinePattern?: string,
  },
  onEnterRules?: {
    beforeText: string,
    afterText?: string,
    previousLineText?: string,
    action?: {
      indent: "none" | "indent" | "outdent" | "indentOutdent",
      appendText?: string,
      removeText?: number,
    },
  }[],
}

export type TmLanguage<S extends string> = Grammar<S> & {
  name?: string,
  scopeName: `source.${S}`,
  foldingStartMarker?: string,
  foldingStopMarker?: string,
  fileTypes?: string[],
  uuid?: string,
  firstLineMatch?: string,
}

interface Grammar<S extends string> {
  patterns: Pattern<S>[],
  repository?: Record<string, Pattern<S>>,
}

type Capture<S extends string> = Record<`${number}`, {
  name?: Name<S>,
  patterns?: Pattern<S>[],
}>

interface Pattern<S extends string> {
  comment?: string,
  disabled?: 0 | 1,
  include?: string,
  match?: string,
  name?: Name<S>,
  contentName?: Name<S>,
  begin?: string,
  end?: string,
  while?: string,
  captures?: Capture<S>,
  beginCaptures?: Capture<S>,
  endCaptures?: Capture<S>,
  whileCaptures?: Capture<S>,
  patterns?: Pattern<S>[],
  applyEndPatternLast?: 0 | 1,
}

type Name<S extends string> = `${(
  | "comment"
  | "comment.block"
  | "comment.block.documentation"
  | "comment.line"
  | "comment.line.double-dash"
  | "comment.line.double-slash"
  | "comment.line.number-sign"
  | "comment.line.percentage"
  | "constant"
  | "constant.character"
  | "constant.character.escape"
  | "constant.language"
  | "constant.numeric"
  | "constant.other"
  | "constant.regexp"
  | "constant.rgb-value"
  | "constant.sha.git-rebase"
  | "emphasis"
  | "entity"
  | "entity.name"
  | "entity.name.class"
  | "entity.name.function"
  | "entity.name.method"
  | "entity.name.section"
  | "entity.name.selector"
  | "entity.name.tag"
  | "entity.name.type"
  | "entity.other"
  | "entity.other.attribute-name"
  | "entity.other.inherited-class"
  | "header"
  | "invalid"
  | "invalid.deprecated"
  | "invalid.illegal"
  | "keyword"
  | "keyword.control"
  | "keyword.control.less"
  | "keyword.operator"
  | "keyword.operator.new"
  | "keyword.other"
  | "keyword.other.unit"
  | "markup"
  | "markup.bold"
  | "markup.changed"
  | "markup.deleted"
  | "markup.heading"
  | "markup.inline.raw"
  | "markup.inserted"
  | "markup.italic"
  | "markup.list"
  | "markup.list.numbered"
  | "markup.list.unnumbered"
  | "markup.other"
  | "markup.punctuation.list.beginning"
  | "markup.punctuation.quote.beginning"
  | "markup.quote"
  | "markup.raw"
  | "markup.underline"
  | "markup.underline.link"
  | "meta"
  | "meta.cast"
  | "meta.parameter.type.variable"
  | "meta.preprocessor"
  | "meta.preprocessor.numeric"
  | "meta.preprocessor.string"
  | "meta.return-type"
  | "meta.selector"
  | "meta.structure.dictionary.key.python"
  | "meta.tag"
  | "meta.type.annotation"
  | "meta.type.name"
  | "metatag.php"
  | "storage"
  | "storage.modifier"
  | "storage.modifier.import.java"
  | "storage.modifier.package.java"
  | "storage.type"
  | "storage.type.cs"
  | "storage.type.java"
  | "string"
  | "string.html"
  | "string.interpolated"
  | "string.jade"
  | "string.other"
  | "string.quoted"
  | "string.quoted.double"
  | "string.quoted.other"
  | "string.quoted.single"
  | "string.quoted.triple"
  | "string.regexp"
  | "string.unquoted"
  | "string.xml"
  | "string.yaml"
  | "strong"
  | "support"
  | "support.class"
  | "support.constant"
  | "support.function"
  | "support.function.git-rebase"
  | "support.other"
  | "support.property-value"
  | "support.type"
  | "support.type.property-name"
  | "support.type.property-name.css"
  | "support.type.property-name.less"
  | "support.type.property-name.scss"
  | "support.variable"
  | "variable"
  | "variable.language"
  | "variable.name"
  | "variable.other"
  | "variable.parameter"
)}.${S}`
