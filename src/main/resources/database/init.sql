-- Criação da Tabela de Regras Fiscais
CREATE TABLE IF NOT EXISTS regras_tributarias (
                                                  id INT AUTO_INCREMENT PRIMARY KEY,
                                                  ncm VARCHAR(8) NOT NULL,
    descricao VARCHAR(200),
    mva DECIMAL(10,2) NOT NULL,        -- Margem de Valor Agregado (ex: 40%)
    aliquota_interna DECIMAL(10,2) NOT NULL, -- Alíquota de MS (ex: 17%)
    ativo BOOLEAN DEFAULT TRUE
    );

-- Inserindo alguns dados de teste (Regras fictícias baseadas em MS)
-- Se o produto tiver NCM 22021000 (Refrigerante), usaremos MVA de 140%
MERGE INTO regras_tributarias (ncm, descricao, mva, aliquota_interna)
    VALUES ('22021000', 'Refrigerantes', 140.00, 17.00);

-- Se for Peça de Moto (exemplo), MVA 50%
MERGE INTO regras_tributarias (ncm, descricao, mva, aliquota_interna)
    VALUES ('87141000', 'Partes e Acessórios Motos', 50.00, 17.00);