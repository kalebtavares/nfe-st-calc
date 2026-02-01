package com.kalebtavares.nfest.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.kalebtavares.nfest.model.NotaFiscal;
import com.kalebtavares.nfest.model.Produto;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ReportService {

    public void gerarRelatorioPdf(NotaFiscal nota, File destino) throws IOException {
        // Inicializa o escritor de PDF
        PdfWriter writer = new PdfWriter(destino);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // --- 1. Cabeçalho ---
        document.add(new Paragraph("RELATÓRIO DE CÁLCULO ICMS-ST (MS)")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(18));

        document.add(new Paragraph("Data de Geração: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:"))));
        document.add(new Paragraph("Chave de Acesso: " + nota.getChaveAcesso()).setFontSize(10));
        document.add(new Paragraph("\n")); // Espaço

        // Dados dos Envolvidos
        document.add(new Paragraph("EMITENTE: " + nota.getNomeEmitente() + " (CNPJ: " + nota.getCnpjEmitente() + ")"));
        document.add(new Paragraph("DESTINATÁRIO: " + nota.getNomeDestinatario()));
        document.add(new Paragraph("\n"));

        // --- 2. Tabela de Produtos ---
        // Cria tabela com 5 colunas (larguras relativas)
        Table table = new Table(UnitValue.createPercentArray(new float[]{10, 40, 15, 15, 20}));
        table.setWidth(UnitValue.createPercentValue(100));

        // Cabeçalhos da Tabela
        table.addHeaderCell(new Cell().add(new Paragraph("Cód.")));
        table.addHeaderCell(new Cell().add(new Paragraph("Produto")));
        table.addHeaderCell(new Cell().add(new Paragraph("NCM")));
        table.addHeaderCell(new Cell().add(new Paragraph("Valor")));
        table.addHeaderCell(new Cell().add(new Paragraph("ICMS-ST")));

        NumberFormat brl = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        double totalSt = 0.0;

        for (Produto p : nota.getProdutos()) {
            table.addCell(new Paragraph(p.getCodigo()).setFontSize(9));
            table.addCell(new Paragraph(p.getNome()).setFontSize(9));
            table.addCell(new Paragraph(p.getNcm()).setFontSize(9));
            table.addCell(new Paragraph(brl.format(p.getValorProduto())).setFontSize(9));

            String stTexto = p.getIcmsStCalculado() > 0
                    ? brl.format(p.getIcmsStCalculado())
                    : "-";

            table.addCell(new Paragraph(stTexto).setFontSize(9).setTextAlignment(TextAlignment.RIGHT));

            totalSt += p.getIcmsStCalculado();
        }

        document.add(table);

        // --- 3. Totais Finais ---
        document.add(new Paragraph("\nRESUMO DO CÁLCULO").setBold());
        document.add(new Paragraph("Valor Total dos Produtos: " + brl.format(nota.getValorTotalProdutos())));
        document.add(new Paragraph("TOTAL ICMS-ST A RECOLHER: " + brl.format(totalSt))
                .setBold()
                .setFontSize(14));

        document.close();
    }
}