import { type ExtensionContext } from "vscode"
import { LanguageClient } from "vscode-languageclient/node"

let client: LanguageClient | undefined = undefined

export function activate(_context: ExtensionContext) {
  client = new LanguageClient("ctt", {
    command: `ctt${process.platform === "win32" ? ".bat" : ""}`,
    args: ["lsp"],
    options: {
      shell: true,
    },
  }, {
    documentSelector: [{ scheme: "file", language: "ctt" }],
  })
  void client.start()
}

export function deactivate() {
  return client?.stop()
}
