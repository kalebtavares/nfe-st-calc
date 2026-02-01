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
        // Configurações para não quebrar se sobrar campo ou faltar tag
        this.xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public NotaFiscal lerNotaFiscal(File arquivoXml) throws IOException {
        // Lemos a árvore completa do XML primeiro para navegar manualmente nas listas
        // Isso evita erros comuns de mapeamento automático em XMLs complexos como NFe
        JsonNode rootNode = xmlMapper.readTree(arquivoXml);

        // Verifica se começa com nfeProc (padrão) ou NFe direto
        JsonNode nfeNode = rootNode.has("NFe") ? rootNode.get("NFe") : rootNode;
        if (rootNode.has("protNFe") && rootNode.has("NFe")) {
            // Estrutura de distribuição
            nfeNode = rootNode.get("NFe");
        }

        JsonNode infNFe = nfeNode.get("infNFe");

        // Criamos o objeto manualmente para garantir precisão
        NotaFiscal nota = new NotaFiscal();

        // Cabeçalho
        nota.setChaveAcesso(infNFe.get("Id").asText());

        // Emitente
        JsonNode emit = infNFe.get("emit");
        nota.setNomeEmitente(emit.get("xNome").asText());
        nota.setCnpjEmitente(emit.get("CNPJ").asText());

        // Destinatário
        JsonNode dest = infNFe.get("dest");
        nota.setNomeDestinatario(dest.get("xNome").asText());
        nota.setUfDestino(dest.get("enderDest").get("UF").asText());

        // Totais
        JsonNode total = infNFe.get("total").get("ICMSTot");
        nota.setValorTotalNota(total.get("vNF").asDouble());
        nota.setValorTotalProdutos(total.get("vProd").asDouble());

        // Produtos (Iterar sobre a lista <det>)
        List<Produto> produtos = new ArrayList<>();
        JsonNode detNode = infNFe.get("det");

        if (detNode.isArray()) {
            for (JsonNode det : detNode) {
                produtos.add(mapearProduto(det));
            }
        } else {
            // Caso só tenha 1 produto, o Jackson as vezes não retorna array
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

        // Verifica se existe CEST (Código Especificador da ST)
        if (prod.has("CEST")) {
            p.setCest(prod.get("CEST").asText());
        }

        // Verifica Frete
        if (prod.has("vFrete")) {
            p.setValorFrete(prod.get("vFrete").asDouble());
        } else {
            p.setValorFrete(0.0);
        }

        // Verifica IPI (está dentro da tag impostos, mas as vezes simplificado)
        // Por hora, deixaremos IPI zerado até implementarmos a regra fina de impostos
        p.setValorIPI(0.0);

        return p;
    }
}