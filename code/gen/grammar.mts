import type { TmLanguage } from "./types.mts"

export default {
  scopeName: "source.ctt",
  patterns: [
    { include: "#term" },
  ],
  repository: {
    comment: {
      patterns: [
        {
          match: "#.*$",
          captures: {
            0: { name: "comment.line.ctt" },
          },
        },
      ],
    },
    term: {
      patterns: [
        { include: "#comment" },
        {
          match: "\\b(?:Type|Func|Closed)\\b",
          name: "keyword.ctt",
        },
        {
          match: "\\b(?:close|open|let)\\b",
          name: "keyword.control.ctt",
        },
      ],
    },
  },
} satisfies TmLanguage<"ctt">
