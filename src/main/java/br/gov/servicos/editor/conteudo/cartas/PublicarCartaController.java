package br.gov.servicos.editor.conteudo.cartas;

import br.gov.servicos.editor.conteudo.ConteudoVersionado;
import br.gov.servicos.editor.conteudo.paginas.ConteudoVersionadoFactory;
import br.gov.servicos.editor.conteudo.paginas.TipoPagina;
import br.gov.servicos.editor.security.UserProfiles;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import static br.gov.servicos.editor.conteudo.paginas.TipoPagina.*;
import static lombok.AccessLevel.PRIVATE;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@Controller
@FieldDefaults(level = PRIVATE, makeFinal = true)
class PublicarCartaController {

    private ConteudoVersionadoFactory factory;
    UserProfiles userProfiles;

    @Autowired
    public PublicarCartaController(ConteudoVersionadoFactory factory, UserProfiles userProfiles) {
        this.factory = factory;
        this.userProfiles = userProfiles;
    }

    @ResponseStatus(value = HttpStatus.OK)
    @RequestMapping(value = "/editar/api/pagina/servico/{id}", method = PUT)
    void publicar(@PathVariable("id") String id) {
        ConteudoVersionado carta = factory.pagina(id, SERVICO);
        carta.publicar(userProfiles.get());
    }

}
