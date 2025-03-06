import type { LanguageConfiguration } from "./types.mts"

export default {
  comments: {
    lineComment: "#",
  },
  autoClosingPairs: [
    { open: "{", close: "}" },
    { open: "(", close: ")" },
  ],
  brackets: [
    ["{", "}"],
    ["(", ")"],
  ],
  surroundingPairs: [
    ["{", "}"],
    ["(", ")"],
  ],
} satisfies LanguageConfiguration
