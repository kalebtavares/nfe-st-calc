package com.kalebtavares.nfest.controller;

import com.kalebtavares.nfest.service.ReportService;
import com.kalebtavares.nfest.model.NotaFiscal;
import com.kalebtavares.nfest.model.Produto;
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

    @FXML
    private Label lblEmitente;
    @FXML
    private Label lblDestinatario;
    @FXML
    private Label lblChave;
    @FXML
    private Label lblTotalProdutos;
    @FXML
    private Label lblTotalSt;
    @FXML
    private Button btnGerarRelatorio;

    @FXML
    private TableView<Produto> tabelaProdutos;
    @FXML
    private TableColumn<Produto, String> colCodigo;
    @FXML
    private TableColumn<Produto, String> colNome;
    @FXML
    private TableColumn<Produto, String> colNcm;
    @FXML
    private TableColumn<Produto, Double> colValor;
    @FXML
    private TableColumn<Produto, Double> colMva;
    @FXML
    private TableColumn<Produto, Double> colIcmsSt;

    private final XmlParserService parserService = new XmlParserService();
    private final TaxCalculatorService taxService = new TaxCalculatorService();

    // Variável para guardar a nota atual na memória para o relatório
    private NotaFiscal notaAtual;

    @FXML
    public void initialize() {
        // Configura as colunas da tabela para lerem os campos da classe Produto
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colNcm.setCellValueFactory(new PropertyValueFactory<>("ncm"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valorProduto"));
        colMva.setCellValueFactory(new PropertyValueFactory<>("mvaAplicada"));
        colIcmsSt.setCellValueFactory(new PropertyValueFactory<>("icmsStCalculado"));

        // Formatação monetária nas células (Opcional, mas fica mais bonito)
        // Se der erro de compilação por versão do JavaFX, pode remover essa parte de setCellFactory
    }

    @FXML
    public void handleImportarXml() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecione o XML da NFe");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Arquivos XML", "*.xml"));

        // Pega a janela atual para abrir o modal
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
            btnGerarRelatorio.setDisable(false);

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.mostrarErro("Erro ao ler arquivo", e.getMessage());
        }
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
            // Configura o seletor de onde salvar o arquivo
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Salvar Relatório PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

            // Sugere um nome de arquivo padrão
            fileChooser.setInitialFileName("Relatorio_NFe_" + notaAtual.getChaveAcesso() + ".pdf");

            Stage stage = (Stage) lblEmitente.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                try {
                    // Chama o serviço que acabamos de criar
                    ReportService reportService = new ReportService();
                    reportService.gerarRelatorioPdf(notaAtual, file);

                    AlertUtil.mostrarInfo("Sucesso", "Relatório gerado com sucesso em:\n" + file.getAbsolutePath());

                    // Opcional: Tentar abrir o arquivo automaticamente após gerar
                    // java.awt.Desktop.getDesktop().open(file);

                } catch (Exception e) {
                    e.printStackTrace();
                    AlertUtil.mostrarErro("Erro ao gerar PDF", e.getMessage());
                }
            }
        }
    }
}