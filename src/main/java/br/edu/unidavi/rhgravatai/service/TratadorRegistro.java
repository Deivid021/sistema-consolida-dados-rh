package br.edu.unidavi.rhgravatai.service;

import br.edu.unidavi.rhgravatai.client.RegistroFonte;
import br.edu.unidavi.rhgravatai.model.RegistroTratado;
import br.edu.unidavi.rhgravatai.model.Rejeicao;
import br.edu.unidavi.rhgravatai.model.Situacao;
import br.edu.unidavi.rhgravatai.util.NormalizadorTexto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Converte e valida os registros da fonte (FR-006, FR-007; regras em data-model.md).
 *
 * <p>Nada é corrigido nem inferido: um valor que não converte rejeita o registro inteiro, com
 * o campo e o motivo.
 */
@Component
public class TratadorRegistro {

    // "uuuu" é obrigatório com ResolverStyle.STRICT
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern VALOR = Pattern.compile("^\\d{1,3}(\\.\\d{3})*,\\d{2}$");
    private static final Pattern HORAS = Pattern.compile("^(\\d+):(\\d{2})$");

    public Resultado tratar(RegistroFonte r) {
        String matricula = NormalizadorTexto.limpar(r.matricula());
        try {
            if (matricula == null) {
                throw new CampoInvalido("matricula", r.matricula(), "matrícula ausente");
            }
            String entidade = obrigatorio("entidade", r.entidade());
            Situacao situacao = Situacao.daFonte(r.situacao())
                    .orElseThrow(() -> new CampoInvalido("situacao", r.situacao(), "situação desconhecida"));

            Map<String, String> originais = new LinkedHashMap<>();
            LocalDate admissao = data("admissao", r.admissao(), originais);
            LocalDate desligamento = data("dataRescisao", r.dataRescisao(), originais);
            BigDecimal salario = valor("salarioBase", r.salarioBase(), originais);
            Integer horasMensais = minutos("horasMensais", r.horasMensais(), originais);
            Integer horasSemanais = minutos("horasSemanais", r.horasSemanais(), originais);

            String nome = NormalizadorTexto.limpar(r.nome());
            String cargo = NormalizadorTexto.limpar(r.cargo());
            String textoBusca = NormalizadorTexto.normalizar(Stream.of(nome, cargo, matricula)
                    .filter(Objects::nonNull).collect(Collectors.joining(" ")));

            return new Resultado.Valido(new RegistroTratado(
                    entidade,
                    matricula,
                    nome,
                    situacao,
                    admissao,
                    desligamento,
                    NormalizadorTexto.limpar(r.motivoRescisao()),
                    NormalizadorTexto.limpar(r.regime()),
                    NormalizadorTexto.limpar(r.classificacao()),
                    NormalizadorTexto.limpar(r.formaInvestidura()),
                    NormalizadorTexto.limpar(r.centroCusto()),
                    NormalizadorTexto.limpar(r.localTrabalho()),
                    cargo,
                    NormalizadorTexto.limpar(r.funcao()),
                    NormalizadorTexto.limpar(r.nivel()),
                    salario,
                    NormalizadorTexto.limpar(r.horario()),
                    horasMensais,
                    horasSemanais,
                    originais,
                    textoBusca));
        } catch (CampoInvalido e) {
            return new Resultado.Rejeitado(new Rejeicao(matricula, e.campo, e.valor, e.getMessage()));
        }
    }

    private static String obrigatorio(String campo, String texto) {
        String valor = NormalizadorTexto.limpar(texto);
        if (valor == null) {
            throw new CampoInvalido(campo, texto, campo + " ausente");
        }
        return valor;
    }

    private static LocalDate data(String campo, String texto, Map<String, String> originais) {
        String valor = NormalizadorTexto.limpar(texto);
        if (valor == null) {
            return null;
        }
        try {
            LocalDate data = LocalDate.parse(valor, DATA);
            originais.put(campo, texto);
            return data;
        } catch (DateTimeParseException e) {
            throw new CampoInvalido(campo, texto, "data inválida (esperado dd/mm/aaaa)");
        }
    }

    private static BigDecimal valor(String campo, String texto, Map<String, String> originais) {
        String valor = NormalizadorTexto.limpar(texto);
        if (valor == null) {
            return null;
        }
        if (!VALOR.matcher(valor).matches()) {
            throw new CampoInvalido(campo, texto, "valor inválido (esperado 1.234,56)");
        }
        originais.put(campo, texto);
        return new BigDecimal(valor.replace(".", "").replace(',', '.'));
    }

    private static Integer minutos(String campo, String texto, Map<String, String> originais) {
        String valor = NormalizadorTexto.limpar(texto);
        if (valor == null) {
            return null;
        }
        Matcher m = HORAS.matcher(valor);
        if (!m.matches() || Integer.parseInt(m.group(2)) >= 60) {
            throw new CampoInvalido(campo, texto, "horas inválidas (esperado HHH:MM)");
        }
        originais.put(campo, texto);
        return Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
    }

    /** Resultado do tratamento de um registro. */
    public sealed interface Resultado {

        record Valido(RegistroTratado registro) implements Resultado {
        }

        record Rejeitado(Rejeicao rejeicao) implements Resultado {
        }
    }

    private static final class CampoInvalido extends RuntimeException {

        private final String campo;
        private final String valor;

        CampoInvalido(String campo, String valor, String motivo) {
            super(motivo, null, false, false);
            this.campo = campo;
            this.valor = valor;
        }
    }
}
