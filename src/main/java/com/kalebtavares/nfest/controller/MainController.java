package com.kalebtavares.nfest.controller;

import com.kalebtavares.nfest.model.NotaFiscal;
import com.kalebtavares.nfest.model.Produto;
import com.kalebtavares.nfest.service.ReportService;
import com.kalebtavares.nfest.service.TaxCalculatorService;
import com.kalebtavares.nfest.service.XmlParserService;
import com.kalebtavares.nfest.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;

public class MainController {

    // --- Elementos da Tela (FXML) ---
    @FXML private Label lblEmitente;
    @FXML private Label lblDestinatario;
    @FXML private Label lblChave;
    @FXML private Label lblTotalProdutos;
    @FXML private Label lblTotalSt;

    // Botões
    @FXML private Button btnImportar;
    @FXML private Button btnLimpar; // Botão Novo
    @FXML private Button btnGerarRelatorio;

    // Tabela e Colunas
    @FXML private TableView<Produto> tabelaProdutos;
    @FXML private TableColumn<Produto, String> colCodigo;
    @FXML private TableColumn<Produto, String> colNome;
    @FXML private TableColumn<Produto, String> colNcm;
    @FXML private TableColumn<Produto, Double> colValor;
    @FXML private TableColumn<Produto, Double> colMva;
    @FXML private TableColumn<Produto, Double> colIcmsSt;

    // --- Serviços e Dados ---
    private final XmlParserService parserService = new XmlParserService();
    private final TaxCalculatorService taxService = new TaxCalculatorService();
    private NotaFiscal notaAtual; // Guarda a nota na memória

    @FXML
    public void initialize() {
        // Configura as colunas da tabela
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colNcm.setCellValueFactory(new PropertyValueFactory<>("ncm"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valorProduto"));
        colMva.setCellValueFactory(new PropertyValueFactory<>("mvaAplicada"));
        colIcmsSt.setCellValueFactory(new PropertyValueFactory<>("icmsStCalculado"));

        // Garante estado inicial dos botões
        if (btnLimpar != null) btnLimpar.setVisible(false);
        if (btnGerarRelatorio != null) btnGerarRelatorio.setDisable(true);
    }

    @FXML
    public void handleImportarXml() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecione o XML da NFe");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Arquivos XML", "*.xml"));

        Stage stage = (Stage) lblEmitente.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            processarArquivo(file);
        }
    }

    private void processarArquivo(File file) {
        try {
            // 1. Ler o XML
            NotaFiscal nota = parserService.lerNotaFiscal(file);

            // 2. Calcular Impostos
            taxService.calcularImpostosNota(nota);

            // 3. Atualizar Interface
            preencherTela(nota);

            this.notaAtual = nota;

            // Ativa os botões
            if (btnGerarRelatorio != null) btnGerarRelatorio.setDisable(false);
            if (btnLimpar != null) btnLimpar.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.mostrarErro("Erro ao ler arquivo", e.getMessage());
        }
    }

    @FXML
    public void handleLimpar() {
        // 1. Zera a variável de memória
        this.notaAtual = null;

        // 2. Limpa os Labels
        lblEmitente.setText("-");
        lblDestinatario.setText("-");
        lblChave.setText("-");
        lblTotalProdutos.setText("R$ 0,00");
        lblTotalSt.setText("R$ 0,00");

        // 3. Limpa a Tabela
        if (tabelaProdutos != null) tabelaProdutos.getItems().clear();

        // 4. Reseta o estado dos botões
        if (btnGerarRelatorio != null) btnGerarRelatorio.setDisable(true);
        if (btnLimpar != null) btnLimpar.setVisible(false);
        if (btnImportar != null) btnImportar.setDisable(false);
    }

    private void preencherTela(NotaFiscal nota) {
        lblEmitente.setText(nota.getNomeEmitente() + " / " + nota.getCnpjEmitente());
        lblDestinatario.setText(nota.getNomeDestinatario());
        lblChave.setText(nota.getChaveAcesso());

        // Atualiza tabela
        tabelaProdutos.setItems(FXCollections.observableArrayList(nota.getProdutos()));

        // Calcula totais para exibir no rodapé
        double totalSt = nota.getProdutos().stream()
                .mapToDouble(Produto::getIcmsStCalculado).sum();

        NumberFormat brl = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        lblTotalProdutos.setText(brl.format(nota.getValorTotalProdutos()));
        lblTotalSt.setText(brl.format(totalSt));
    }

    @FXML
    public void handleGerarRelatorio() {
        if (this.notaAtual != null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Salvar Relatório PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

            // Sugere nome padrão
            String nomePadrao = "Relatorio_" + (notaAtual.getChaveAcesso() != null ? notaAtual.getChaveAcesso() : "NFe") + ".pdf";
            fileChooser.setInitialFileName(nomePadrao);

            Stage stage = (Stage) lblEmitente.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                try {
                    ReportService reportService = new ReportService();
                    reportService.gerarRelatorioPdf(notaAtual, file);
                    AlertUtil.mostrarInfo("Sucesso", "Relatório salvo em:\n" + file.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                    AlertUtil.mostrarErro("Erro PDF", "Não foi possível gerar o relatório:\n" + e.getMessage());
                }
            }
        }
    }
}