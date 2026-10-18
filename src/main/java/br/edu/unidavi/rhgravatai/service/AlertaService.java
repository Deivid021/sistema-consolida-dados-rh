package br.edu.unidavi.rhgravatai.service;

import br.edu.unidavi.rhgravatai.config.PropriedadesAlertas;
import br.edu.unidavi.rhgravatai.dto.AlertaResposta;
import br.edu.unidavi.rhgravatai.dto.FiltroVinculos;
import br.edu.unidavi.rhgravatai.dto.PaginaAlertasResposta;
import br.edu.unidavi.rhgravatai.dto.Paginacao;
import br.edu.unidavi.rhgravatai.dto.VinculoResposta;
import br.edu.unidavi.rhgravatai.model.Alerta;
import br.edu.unidavi.rhgravatai.model.RegraAlerta;
import br.edu.unidavi.rhgravatai.repository.AlertaRepository;
import br.edu.unidavi.rhgravatai.repository.CargaRepository;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Listagem dos alertas calculados na última carga (FR-040 a FR-042). */
@Service
public class AlertaService {

    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");

    private final AlertaRepository alertas;
    private final TetoService tetoService;
    private final CargaRepository cargas;
    private final PropriedadesAlertas propriedades;

    public AlertaService(AlertaRepository alertas, TetoService tetoService, CargaRepository cargas,
                         PropriedadesAlertas propriedades) {
        this.alertas = alertas;
        this.tetoService = tetoService;
        this.cargas = cargas;
        this.propriedades = propriedades;
    }

    /** Ordenação fixa: maior diferença percentual primeiro. */
    @Transactional(readOnly = true)
    public PaginaAlertasResposta listar(RegraAlerta regra, FiltroVinculos filtro, Paginacao paginacao) {
        List<AlertaResposta> itens = alertas.buscar(regra, filtro, paginacao).stream()
                .map(AlertaService::paraResposta).toList();
        return new PaginaAlertasResposta(itens, alertas.contar(regra, filtro), paginacao.pagina(),
                paginacao.tamanho(), tetoService.vigente(), propriedades.foraPadrao().percentual(),
                cargas.ultimaAtualizacao());
    }

    public long contar(RegraAlerta regra, FiltroVinculos filtro) {
        return alertas.contar(regra, filtro);
    }

    /** Todos os alertas do filtro, para a exportação. Consumir dentro de uma transação. */
    public Stream<AlertaResposta> fluxo(RegraAlerta regra, FiltroVinculos filtro) {
        return alertas.fluxo(regra, filtro).map(AlertaService::paraResposta);
    }

    static AlertaResposta paraResposta(Alerta a) {
        return new AlertaResposta(a.regra(), VinculoResposta.de(a.vinculo()), a.valorVinculo(), a.valorReferencia(),
                a.diferenca(), a.diferencaPercentual(), a.grupoTamanho(), descrever(a));
    }

    /** Texto do critério, exibido junto ao aviso de que é sinalização para verificação (FR-041). */
    static String descrever(Alerta alerta) {
        NumberFormat moeda = NumberFormat.getCurrencyInstance(BRASIL);
        String texto;
        if (alerta.regra() == RegraAlerta.TETO) {
            texto = "Salário base " + moeda.format(alerta.diferenca()) + " acima do teto municipal (subsídio do prefeito) de "
                    + moeda.format(alerta.valorReferencia()) + ".";
        } else {
            NumberFormat numero = NumberFormat.getNumberInstance(BRASIL);
            numero.setMinimumFractionDigits(1);
            numero.setMaximumFractionDigits(1);
            String direcao = alerta.diferencaPercentual().signum() >= 0 ? "acima" : "abaixo";
            texto = "Salário base " + numero.format(alerta.diferencaPercentual().abs()) + "% " + direcao
                    + " da mediana (" + moeda.format(alerta.valorReferencia()) + ") dos " + alerta.grupoTamanho()
                    + " vínculos ativos com o mesmo cargo, nível e carga horária.";
        }
        // O formato de moeda do Java usa espaço não separável depois de "R$"
        return texto.replace(' ', ' ');
    }
}
