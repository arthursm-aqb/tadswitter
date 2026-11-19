const $ = id => document.getElementById(id);
let token = localStorage.getItem('tadswitter_token');
let usuario = JSON.parse(localStorage.getItem('tadswitter_usuario') || 'null');
let cadastro = false;
let postagemAtual = null;
let socket = null;
let reconectarTimer = null;
let pendentes = 0;

function aviso(mensagem, estado = false) {
  $('aviso').textContent = mensagem;
  $('aviso').className = estado === true || estado === 'erro' ? 'erro' : (estado === 'aguardando' ? 'aguardando' : '');
}

function moderacaoPendente(resposta) {
  return !resposta || resposta._pendente || resposta.status === 'PENDENTE' || resposta.status === 'AGUARDANDO_MODERACAO';
}

function conectarWs() {
  if (!token || !usuario) return;
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) return;

  const protocolo = location.protocol === 'https:' ? 'wss:' : 'ws:';
  socket = new WebSocket(`${protocolo}//${location.hostname}:9083/ws?token=${encodeURIComponent(token)}`);

  socket.addEventListener('message', event => {
    try {
      const msg = JSON.parse(event.data);
      tratarEventoWs(msg);
    } catch { /* ignora mensagens inválidas */ }
  });

  socket.addEventListener('close', () => {
    socket = null;
    if (token && usuario) {
      clearTimeout(reconectarTimer);
      reconectarTimer = setTimeout(conectarWs, 3000);
    }
  });
}

function desconectarWs() {
  clearTimeout(reconectarTimer);
  reconectarTimer = null;
  if (socket) {
    socket.close();
    socket = null;
  }
}

async function api(caminho, metodo = 'GET', dados) {
  const resposta = await fetch(caminho, {
    method: metodo,
    headers: {'Content-Type': 'application/json', ...(token ? {Authorization: 'Bearer ' + token} : {})},
    body: dados ? JSON.stringify(dados) : undefined
  });
  if (!resposta.ok) {
    if (resposta.status === 401 && !caminho.includes('/auth/')) {
      sair();
      throw new Error('Sua sessão expirou. Entre novamente.');
    }
    const mensagens = {
      400: 'Confira os campos preenchidos.',
      401: 'Login ou senha incorretos.',
      403: 'Conteúdo bloqueado pela moderação.',
      404: 'Postagem não encontrada.',
      409: 'Este login já está em uso.',
      422: 'Conteúdo ofensivo bloqueado pela moderação.',
      503: 'Uma das APIs internas está fora do ar.'
    };
    throw new Error(mensagens[resposta.status] || 'Não foi possível completar a operação.');
  }
  const texto = await resposta.text();
  const corpo = texto ? JSON.parse(texto) : null;
  if (resposta.status === 202 && corpo && typeof corpo === 'object' && !Array.isArray(corpo)) {
    corpo._pendente = true;
  }
  return corpo;
}

function sair() {
  token = null;
  usuario = null;
  pendentes = 0;
  desconectarWs();
  localStorage.removeItem('tadswitter_token');
  localStorage.removeItem('tadswitter_usuario');
  location.hash = '#/board';
  mostrar();
}

function elemento(tag, classe, texto) {
  const el = document.createElement(tag);
  if (classe) el.className = classe;
  if (texto !== undefined) el.textContent = texto;
  return el;
}

function data(valor) {
  return new Date(valor).toLocaleString('pt-BR', {dateStyle: 'short', timeStyle: 'short'});
}

function titulo(post) {
  return post.titulo?.trim() || 'Postagem sem título';
}

function meta(item, numero) {
  const linha = elemento('div', 'meta');
  linha.append(
    elemento('strong', '', `No. ${numero}`),
    elemento('span', '', item.autorNome),
    elemento('time', '', data(item.criadoEm))
  );
  return linha;
}

function resumo(post) {
  const qtd = (post.comentarios || []).length;
  const link = elemento('a', 'postagem-resumo');
  link.href = `#/postagem/${post.id}`;
  link.dataset.postagemId = String(post.id);
  link.dataset.comentarios = String(qtd);
  link.append(
    meta(post, post.id),
    elemento('h2', 'titulo-postagem', titulo(post)),
    elemento('p', 'texto', post.texto),
    elemento('span', 'contagem', `${qtd} comentário${qtd === 1 ? '' : 's'} · abrir >>`)
  );
  return link;
}

function renderComentario(c) {
  const item = elemento('article', 'comentario');
  item.dataset.comentarioId = String(c.id);
  item.append(meta(c, c.id), elemento('p', 'texto', c.texto));
  return item;
}

function atualizarTotalPostagens() {
  const total = $('postagens').querySelectorAll('.postagem-resumo').length;
  $('total-postagens').textContent = total === 1 ? '1 postagem' : `${total} postagens`;
}

function tratarEventoWs(msg) {
  if (msg.evento === 'MODERACAO_RESULTADO') {
    pendentes = Math.max(0, pendentes - 1);
    if (msg.aprovado) {
      if (msg.tipo === 'POSTAGEM' && location.hash === '#/nova' && msg.postagemId) {
        location.hash = `#/postagem/${msg.postagemId}`;
        mostrar();
      }
      aviso(msg.mensagem || 'Conteúdo aprovado e publicado.');
    } else {
      aviso(msg.mensagem || 'Conteúdo ofensivo bloqueado pela moderação.', true);
    }
    return;
  }

  if (msg.evento === 'NOVA_POSTAGEM' && msg.postagem) {
    if ($('tela-board').hidden) return;
    const lista = $('postagens');
    if (lista.querySelector(`[data-postagem-id="${msg.postagem.id}"]`)) return;
    const vazio = lista.querySelector('.vazio');
    if (vazio) vazio.remove();
    lista.prepend(resumo(msg.postagem));
    atualizarTotalPostagens();
    return;
  }

  if (msg.evento === 'NOVO_COMENTARIO' && msg.comentario) {
    if (!$('tela-board').hidden) {
      const card = $('postagens').querySelector(`[data-postagem-id="${msg.postagemId}"]`);
      if (card) {
        const novoTotal = Number(card.dataset.comentarios || 0) + 1;
        card.dataset.comentarios = String(novoTotal);
        const contagem = card.querySelector('.contagem');
        if (contagem) {
          contagem.textContent = `${novoTotal} comentário${novoTotal === 1 ? '' : 's'} · abrir >>`;
        }
      }
    }
    if (!$('tela-postagem').hidden && String(postagemAtual) === String(msg.postagemId)) {
      const lista = $('comentarios');
      if (lista.querySelector(`[data-comentario-id="${msg.comentario.id}"]`)) return;
      const vazio = lista.querySelector('.vazio');
      if (vazio) vazio.remove();
      lista.append(renderComentario(msg.comentario));
      const total = lista.querySelectorAll('.comentario').length;
      $('titulo-comentarios').textContent = `Comentários (${total})`;
    }
  }
}

async function carregarBoard() {
  try {
    const dados = await api('/api/postagens');
    const posts = Object.values(dados._embedded || {})[0] || [];
    $('postagens').replaceChildren(...(posts.length ? posts.map(resumo) : [elemento('p', 'vazio', 'Ainda não há postagens.')]));
    atualizarTotalPostagens();
  } catch (e) { aviso(e.message, true); }
}

async function carregarPostagem(id) {
  try {
    const post = await api(`/api/postagens/${id}`);
    postagemAtual = post.id;
    const caixa = elemento('article', 'postagem-inteira');
    caixa.append(elemento('h1', 'titulo-postagem', titulo(post)), meta(post, post.id), elemento('p', 'texto', post.texto));
    $('postagem-detalhe').replaceChildren(caixa);
    $('titulo-comentarios').textContent = `Comentários (${post.comentarios.length})`;
    const comentarios = post.comentarios.map(renderComentario);
    $('comentarios').replaceChildren(...(comentarios.length ? comentarios : [elemento('p', 'vazio', 'Nenhum comentário ainda.')]));
  } catch (e) { aviso(e.message, true); }
}

function mostrar() {
  const logado = Boolean(token && usuario);
  $('tela-auth').hidden = logado;
  $('menu').hidden = !logado;
  $('nome-usuario').textContent = logado ? usuario.nome : '';
  for (const id of ['tela-board', 'tela-nova', 'tela-postagem']) $(id).hidden = true;
  if (!logado) return;

  conectarWs();
  if (pendentes > 0) {
    aviso('Aguardando aprovação da moderação...', 'aguardando');
  } else {
    aviso('');
  }

  const detalhe = location.hash.match(/^#\/postagem\/(\d+)$/);
  if (detalhe) {
    $('tela-postagem').hidden = false;
    carregarPostagem(detalhe[1]);
  } else if (location.hash === '#/nova') {
    $('tela-nova').hidden = false;
  } else {
    $('tela-board').hidden = false;
    carregarBoard();
  }
}

$('form-auth').addEventListener('submit', async e => {
  e.preventDefault();
  const botao = $('enviar-auth');
  botao.disabled = true;
  try {
    const credenciais = {login: $('login').value, senha: $('senha').value};
    if (cadastro) {
      aviso('Aguardando aprovação da moderação...', 'aguardando');
      await api('/api/auth/cadastro', 'POST', {nome: $('nome').value, ...credenciais});
    }
    const resposta = await api('/api/auth/login', 'POST', credenciais);
    token = resposta.token;
    usuario = resposta.usuario;
    localStorage.setItem('tadswitter_token', token);
    localStorage.setItem('tadswitter_usuario', JSON.stringify(usuario));
    location.hash = '#/board';
    mostrar();
  } catch (e) { aviso(e.message, true); }
  finally { botao.disabled = false; }
});

$('trocar-auth').addEventListener('click', () => {
  cadastro = !cadastro;
  $('campo-nome').hidden = !cadastro;
  $('nome').required = cadastro;
  $('enviar-auth').textContent = cadastro ? 'Criar conta' : 'Entrar';
  $('trocar-auth').textContent = cadastro ? 'Já tem conta? Entrar' : 'Criar conta';
  $('senha').autocomplete = cadastro ? 'new-password' : 'current-password';
  aviso('');
});

$('form-postagem').addEventListener('submit', async e => {
  e.preventDefault();
  const botao = e.currentTarget.querySelector('button');
  botao.disabled = true;
  aviso('Aguardando aprovação da moderação...', 'aguardando');
  try {
    const post = await api('/api/postagens', 'POST', {titulo: $('titulo-postagem').value, texto: $('texto-postagem').value});
    $('titulo-postagem').value = '';
    $('texto-postagem').value = '';
    if (moderacaoPendente(post)) {
      pendentes += 1;
      aviso('Aguardando aprovação da moderação...', 'aguardando');
      return;
    }
    location.hash = `#/postagem/${post.id}`;
    mostrar();
    aviso('Postagem publicada.');
  } catch (e) { aviso(e.message, true); }
  finally { botao.disabled = false; }
});

$('form-comentario').addEventListener('submit', async e => {
  e.preventDefault();
  const botao = e.currentTarget.querySelector('button');
  botao.disabled = true;
  aviso('Aguardando aprovação da moderação...', 'aguardando');
  try {
    const comentario = await api(`/api/postagens/${postagemAtual}/comentarios`, 'POST', {texto: $('texto-comentario').value});
    $('texto-comentario').value = '';
    if (moderacaoPendente(comentario)) {
      pendentes += 1;
      aviso('Aguardando aprovação da moderação...', 'aguardando');
      return;
    }
    await carregarPostagem(postagemAtual);
    aviso('Comentário publicado.');
  } catch (e) { aviso(e.message, true); }
  finally { botao.disabled = false; }
});

$('atualizar').addEventListener('click', carregarBoard);
$('sair').addEventListener('click', () => { sair(); aviso(''); });
window.addEventListener('hashchange', mostrar);
mostrar();
