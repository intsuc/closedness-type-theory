import fs from "node:fs"
import path from "node:path"
import grammar from "./grammar.mts"
import languageConfiguration from "./language-configuration.mts"

const dist = "dist"

if (!fs.existsSync(dist)) {
  fs.mkdirSync(dist)
}

await Promise.all([
  fs.promises.writeFile(
    path.join(dist, "grammar.json"),
    JSON.stringify(grammar),
  ),
  fs.promises.writeFile(
    path.join(dist, "language-configuration.json"),
    JSON.stringify(languageConfiguration),
  ),
])
