package com.kalebtavares.nfest.service;

import com.kalebtavares.nfest.model.NotaFiscal;
import com.kalebtavares.nfest.model.Produto;
import com.kalebtavares.nfest.model.RegraTributaria;
import com.kalebtavares.nfest.repository.RegraRepository;

import java.util.Optional;

public class TaxCalculatorService {

    private final RegraRepository repository;

    // Constante: Alíquota Interestadual padrão chegando em MS (geralmente 7% do Sul/Sudeste)
    private static final double ALIQ_INTER_PADRAO = 7.0;

    public TaxCalculatorService() {
        this.repository = new RegraRepository();
    }

    public void calcularImpostosNota(NotaFiscal nota) {
        double totalStNota = 0.0;

        for (Produto produto : nota.getProdutos()) {
            // 1. Busca regra no banco pelo NCM
            Optional<RegraTributaria> regraOpt = repository.buscarPorNcm(produto.getNcm());

            if (regraOpt.isPresent()) {
                RegraTributaria regra = regraOpt.get();

                // --- INICIO DO CALCULO ICMS-ST (MVA AJUSTADA) ---

                // Passo A: Formar a base de cálculo (Valor Produto + IPI + Frete + Seguro etc)
                // O MVA é aplicado sobre tudo isso.
                double valorBaseInicial = produto.getValorProduto() + produto.getValorFrete() + produto.getValorIPI();

                // Passo B: Aplicar MVA (Margem de Valor Agregado)
                // Exemplo: Se MVA é 40%, multiplicamos por 1.40
                double mvaMultiplicador = 1 + (regra.getMva() / 100);
                double baseCalculoSt = valorBaseInicial * mvaMultiplicador;

                // Passo C: Calcular o ICMS "Cheio" (Interno MS)
                double icmsInterno = baseCalculoSt * (regra.getAliquotaInterna() / 100);

                // Passo D: Calcular o ICMS de Origem (Crédito)
                // Usamos a alíquota interestadual sobre o valor do produto (sem MVA)
                double icmsOrigem = valorBaseInicial * (ALIQ_INTER_PADRAO / 100);

                // Passo E: O valor a pagar é a diferença (O que o estado quer - O que já foi pago)
                double valorStAPagar = icmsInterno - icmsOrigem;

                // Arredondamento e travas (não pode ser negativo)
                if (valorStAPagar < 0) valorStAPagar = 0.0;

                // Atualiza o objeto produto com os valores
                produto.setDevePagarSt(true);
                produto.setMvaAplicada(regra.getMva());
                produto.setIcmsStCalculado(arredondar(valorStAPagar));

                totalStNota += valorStAPagar;

            } else {
                // Se não tem regra no banco, não calcula ST
                produto.setDevePagarSt(false);
                produto.setIcmsStCalculado(0.0);
            }
        }

        // Podemos adicionar um campo "totalStCalculado" na NotaFiscal se quiser depois
        System.out.println("Cálculo finalizado. Total ST da Nota: R$ " + totalStNota);
    }

    // Função auxiliar para arredondar 2 casas decimais (dinheiro)
    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}