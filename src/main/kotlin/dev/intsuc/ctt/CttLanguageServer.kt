package dev.intsuc.ctt

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.*
import java.util.concurrent.CompletableFuture

class CttLanguageServer private constructor() : LanguageClientAware, LanguageServer, TextDocumentService, WorkspaceService {
    private lateinit var client: LanguageClient

    override fun connect(client: LanguageClient) {
        this.client = client
    }

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        return CompletableFuture.completedFuture(InitializeResult(ServerCapabilities().apply {
            setTextDocumentSync(TextDocumentSyncKind.Full)
            diagnosticProvider = DiagnosticRegistrationOptions()
        }))
    }

    override fun shutdown(): CompletableFuture<in Any> = CompletableFuture.completedFuture(null)

    override fun exit() = Unit

    override fun getTextDocumentService(): TextDocumentService = this

    override fun getWorkspaceService(): WorkspaceService = this

    override fun didOpen(params: DidOpenTextDocumentParams) {
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
    }

    override fun diagnostic(params: DocumentDiagnosticParams): CompletableFuture<DocumentDiagnosticReport> {
        return CompletableFuture.completedFuture(DocumentDiagnosticReport(RelatedFullDocumentDiagnosticReport()))
    }

    override fun didChangeConfiguration(params: DidChangeConfigurationParams) {
    }

    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {
    }

    companion object {
        fun launch() {
            val server = CttLanguageServer()
            val launcher = LSPLauncher.createServerLauncher(server, System.`in`, System.out)
            server.connect(launcher.remoteProxy)
            launcher.startListening()
        }
    }
}
