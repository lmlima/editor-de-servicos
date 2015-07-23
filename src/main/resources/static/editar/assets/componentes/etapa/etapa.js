var etapa = etapa || {};

var Etapa = {

  controller: function(args) {
    this.etapa = args.etapa;
  },

  view: function(ctrl) {
    return m('.etapa#' + ctrl.etapa.id, [
      m.component(etapa.Titulo, { titulo: ctrl.etapa.titulo }),
      m.component(etapa.Descricao, { descricao: ctrl.etapa.descricao }),
      m.component(etapa.Documentacao , { documentacao: ctrl.etapa.documentacao }),
    ])
  }
};