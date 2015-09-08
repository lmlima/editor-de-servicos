package br.gov.servicos.editor.frontend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

@Slf4j
@Component
public class Siorg {

    public static final Predicate<String> URL_PREDICATE = Pattern.compile("http://estruturaorganizacional\\.dados\\.gov\\.br/id/unidade-organizacional/\\d+").asPredicate();

    private RestTemplate restTemplate;

    @Autowired
    public Siorg(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable(value = "orgaoSiorg", unless = "#result.isPresent()")
    public Optional<String> nomeDoOrgao(String urlOrgao) {
        if (URL_PREDICATE.negate().test(urlOrgao)) {
            log.warn("URL {} não é uma URL para órgão no Siorg", urlOrgao);
            return empty();
        }

        try {
            ResponseEntity<Orgao> entity = restTemplate.getForEntity(urlOrgao, Orgao.class);
            Orgao body = entity.getBody();

            if (body.getServico().getCodigoErro() > 0) {
                log.warn("Erro ao acessar Siorg: {}", body.getServico().getMensagem());
                return empty();
            }

            return ofNullable(String.format("%s (%s)", body.getUnidade().getNome(), body.getUnidade().getSigla()));

        } catch (Exception e) {
            log.warn("Erro ao acessar Siorg", e);
            return empty();
        }
    }

    @Data
    @Wither
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Orgao {
        Servico servico;
        Unidade unidade;
    }

    @Data
    @Wither
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Servico {
        long codigoErro;
        String mensagem;
    }

    @Data
    @Wither
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Unidade {
        String codigoUnidade;
        String codigoUnidadePai;
        String codigoOrgaoEntidade;
        String codigoTipoUnidade;
        String nome;
        String sigla;
        String codigoEsfera;
        String codigoPoder;
        String codigoNaturezaJuridica;
        String codigoSubNaturezaJuridica;
    }
}
