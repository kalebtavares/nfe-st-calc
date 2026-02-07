package com.kalebtavares.nfest.controller;

import com.kalebtavares.nfest.model.NotaFiscal;
import com.kalebtavares.nfest.model.Produto;
import com.kalebtavares.nfest.model.RegraTributaria;
import com.kalebtavares.nfest.repository.RegraRepository;
import com.kalebtavares.nfest.service.ReportService;
import com.kalebtavares.nfest.service.TaxCalculatorService;
import com.kalebtavares.nfest.service.XmlParserService;
import com.kalebtavares.nfest.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;

public class MainController {

    // --- Elementos da Aba Calculadora ---
    @FXML private Label lblEmitente, lblDestinatario, lblChave, lblTotalProdutos, lblTotalSt;
    @FXML private Button btnImportar, btnLimpar, btnGerarRelatorio;

    @FXML private TableView<Produto> tabelaProdutos;
    @FXML private TableColumn<Produto, String> colCodigo, colNome, colNcm;
    @FXML private TableColumn<Produto, Double> colValor, colMva, colIcmsSt;

    // --- Elementos da Aba Regras ---
    @FXML private TextField txtRegraNcm, txtRegraDescricao, txtRegraMva, txtRegraAliquota;
    @FXML private TableView<RegraTributaria> tabelaRegras;
    @FXML private TableColumn<RegraTributaria, String> colRegraNcm, colRegraDescricao;
    @FXML private TableColumn<RegraTributaria, Double> colRegraMva, colRegraAliquota;

    // --- Serviços ---
    private final XmlParserService parserService = new XmlParserService();
    private final TaxCalculatorService taxService = new TaxCalculatorService();
    private final RegraRepository regraRepository = new RegraRepository();
    private NotaFiscal notaAtual;

    @FXML
    public void initialize() {
        configurarAbaCalculadora();
        configurarAbaRegras();
    }

    private void configurarAbaCalculadora() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colNcm.setCellValueFactory(new PropertyValueFactory<>("ncm"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valorProduto"));
        colMva.setCellValueFactory(new PropertyValueFactory<>("mvaAplicada"));
        colIcmsSt.setCellValueFactory(new PropertyValueFactory<>("icmsStCalculado"));

        if (btnLimpar != null) btnLimpar.setVisible(false);
        if (btnGerarRelatorio != null) btnGerarRelatorio.setDisable(true);
    }

    private void configurarAbaRegras() {
        colRegraNcm.setCellValueFactory(new PropertyValueFactory<>("ncm"));
        colRegraDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colRegraMva.setCellValueFactory(new PropertyValueFactory<>("mva"));
        colRegraAliquota.setCellValueFactory(new PropertyValueFactory<>("aliquotaInterna"));

        carregarTabelaRegras();
    }

    // --- LÓGICA DA ABA CALCULADORA ---

    @FXML
    public void handleImportarXml() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML", "*.xml"));
        Stage stage = (Stage) lblEmitente.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) processarArquivo(file);
    }

    private void processarArquivo(File file) {
        try {
            NotaFiscal nota = parserService.lerNotaFiscal(file);
            taxService.calcularImpostosNota(nota); // Recalcula com base nas regras atuais do banco
            preencherTela(nota);
            this.notaAtual = nota;
            btnGerarRelatorio.setDisable(false);
            btnLimpar.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.mostrarErro("Erro", e.getMessage());
        }
    }

    @FXML
    public void handleLimpar() {
        this.notaAtual = null;
        lblEmitente.setText("-"); lblDestinatario.setText("-"); lblChave.setText("-");
        lblTotalProdutos.setText("R$ 0,00"); lblTotalSt.setText("R$ 0,00");
        tabelaProdutos.getItems().clear();
        btnGerarRelatorio.setDisable(true);
        btnLimpar.setVisible(false);
    }

    private void preencherTela(NotaFiscal nota) {
        lblEmitente.setText(nota.getNomeEmitente());
        lblDestinatario.setText(nota.getNomeDestinatario());
        lblChave.setText(nota.getChaveAcesso());
        tabelaProdutos.setItems(FXCollections.observableArrayList(nota.getProdutos()));

        double totalSt = nota.getProdutos().stream().mapToDouble(Produto::getIcmsStCalculado).sum();
        NumberFormat brl = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        lblTotalProdutos.setText(brl.format(nota.getValorTotalProdutos()));
        lblTotalSt.setText(brl.format(totalSt));
    }

    @FXML
    public void handleGerarRelatorio() {
        if (this.notaAtual != null) {
            // ... (Mesma lógica de antes)
            FileChooser fc = new FileChooser();
            fc.setInitialFileName("Relatorio.pdf");
            File f = fc.showSaveDialog(lblEmitente.getScene().getWindow());
            if (f != null) {
                try {
                    new ReportService().gerarRelatorioPdf(notaAtual, f);
                    AlertUtil.mostrarInfo("Sucesso", "PDF Gerado!");
                } catch(Exception e) { AlertUtil.mostrarErro("Erro", e.getMessage()); }
            }
        }
    }

    // --- LÓGICA DA ABA REGRAS (NOVO) ---

    private void carregarTabelaRegras() {
        tabelaRegras.setItems(FXCollections.observableArrayList(regraRepository.listarTodas()));
    }

    @FXML
    public void handleSalvarRegra() {
        try {
            RegraTributaria regra = new RegraTributaria();
            regra.setNcm(txtRegraNcm.getText().trim());
            regra.setDescricao(txtRegraDescricao.getText());
            regra.setMva(Double.parseDouble(txtRegraMva.getText().replace(",", ".")));
            regra.setAliquotaInterna(Double.parseDouble(txtRegraAliquota.getText().replace(",", ".")));

            if (regra.getNcm().isEmpty()) throw new Exception("NCM é obrigatório");

            regraRepository.salvar(regra);
            carregarTabelaRegras();
            handleLimparFormRegra();
            AlertUtil.mostrarInfo("Sucesso", "Regra salva com sucesso!");

        } catch (NumberFormatException e) {
            AlertUtil.mostrarErro("Erro", "Verifique se os campos numéricos estão corretos.");
        } catch (Exception e) {
            AlertUtil.mostrarErro("Erro", e.getMessage());
        }
    }

    @FXML
    public void handleExcluirRegra() {
        RegraTributaria selecionada = tabelaRegras.getSelectionModel().getSelectedItem();
        if (selecionada != null) {
            regraRepository.excluir(selecionada.getNcm());
            carregarTabelaRegras();
            handleLimparFormRegra();
        } else {
            AlertUtil.mostrarErro("Atenção", "Selecione uma regra na tabela para excluir.");
        }
    }

    @FXML
    public void handleLimparFormRegra() {
        txtRegraNcm.clear();
        txtRegraDescricao.clear();
        txtRegraMva.clear();
        txtRegraAliquota.setText("17.0");
    }

    @FXML
    public void handleSelecionarRegra() {
        RegraTributaria selecionada = tabelaRegras.getSelectionModel().getSelectedItem();
        if (selecionada != null) {
            txtRegraNcm.setText(selecionada.getNcm());
            txtRegraDescricao.setText(selecionada.getDescricao());
            txtRegraMva.setText(String.valueOf(selecionada.getMva()));
            txtRegraAliquota.setText(String.valueOf(selecionada.getAliquotaInterna()));
        }
    }
}