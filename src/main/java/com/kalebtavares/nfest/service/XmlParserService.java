package com.kalebtavares.nfest.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.kalebtavares.nfest.model.NotaFiscal;
import com.kalebtavares.nfest.model.Produto;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XmlParserService {

    private final XmlMapper xmlMapper;

    public XmlParserService() {
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public NotaFiscal lerNotaFiscal(File arquivoXml) throws IOException {
        JsonNode rootNode = xmlMapper.readTree(arquivoXml);

        // Tratamento para diferentes estruturas de XML (nfeProc vs NFe puro)
        JsonNode nfeNode;
        if (rootNode.has("NFe")) {
            nfeNode = rootNode.get("NFe");
        } else if (rootNode.has("infNFe")) {
            nfeNode = rootNode; // Caso pegue direto o nó interno
        } else {
            // Tenta navegar padrão
            nfeNode = rootNode.get("NFe");
        }

        // Se a nota vier envelopada em nfeProc (padrão Receita), o NFe está dentro
        if (nfeNode == null && rootNode.has("protNFe")) {
            // Estrutura atípica, mas vamos tentar garantir
            nfeNode = rootNode;
        }

        JsonNode infNFe = nfeNode.get("infNFe");

        NotaFiscal nota = new NotaFiscal();

        // --- Cabeçalho ---
        if (infNFe.has("Id")) nota.setChaveAcesso(infNFe.get("Id").asText());

        // Emitente
        JsonNode emit = infNFe.get("emit");
        nota.setNomeEmitente(emit.get("xNome").asText());
        nota.setCnpjEmitente(emit.get("CNPJ").asText());

        // Destinatário
        JsonNode dest = infNFe.get("dest");
        nota.setNomeDestinatario(dest.get("xNome").asText());
        if (dest.has("enderDest") && dest.get("enderDest").has("UF")) {
            nota.setUfDestino(dest.get("enderDest").get("UF").asText());
        }

        // Totais
        JsonNode total = infNFe.get("total").get("ICMSTot");
        nota.setValorTotalNota(total.get("vNF").asDouble());
        nota.setValorTotalProdutos(total.get("vProd").asDouble());

        // --- Produtos ---
        List<Produto> produtos = new ArrayList<>();
        JsonNode detNode = infNFe.get("det");

        if (detNode.isArray()) {
            for (JsonNode det : detNode) {
                produtos.add(mapearProduto(det));
            }
        } else {
            produtos.add(mapearProduto(detNode));
        }

        nota.setProdutos(produtos);
        return nota;
    }

    private Produto mapearProduto(JsonNode det) {
        JsonNode prod = det.get("prod");
        Produto p = new Produto();

        p.setCodigo(prod.get("cProd").asText());
        p.setNome(prod.get("xProd").asText());
        p.setNcm(prod.get("NCM").asText());
        p.setCfop(prod.get("CFOP").asText());
        p.setValorProduto(prod.get("vProd").asDouble());

        if (prod.has("CEST")) p.setCest(prod.get("CEST").asText());

        // Frete (Item a Item)
        if (prod.has("vFrete")) {
            p.setValorFrete(prod.get("vFrete").asDouble());
        } else {
            p.setValorFrete(0.0);
        }

        // --- LÓGICA DE IPI (Correção Importante) ---
        // O IPI fica em: det -> imposto -> IPI -> IPITrib -> vIPI
        p.setValorIPI(0.0); // Padrão
        if (det.has("imposto") && det.get("imposto").has("IPI")) {
            JsonNode ipiNode = det.get("imposto").get("IPI");

            // IPI Tributado
            if (ipiNode.has("IPITrib") && ipiNode.get("IPITrib").has("vIPI")) {
                p.setValorIPI(ipiNode.get("IPITrib").get("vIPI").asDouble());
            }
        }

        return p;
    }
}