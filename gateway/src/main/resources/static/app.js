const $ = id => document.getElementById(id);
let token = localStorage.getItem('tadswitter_token');
let usuario = JSON.parse(localStorage.getItem('tadswitter_usuario') || 'null');
let cadastro = false;
let postagemAtual = null;

function aviso(mensagem, erro = false) {
  $('aviso').textContent = mensagem;
  $('aviso').className = erro ? 'erro' : '';
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
      404: 'Postagem não encontrada.',
      409: 'Este login já está em uso.',
      503: 'Uma das APIs internas está fora do ar.'
    };
    throw new Error(mensagens[resposta.status] || 'Não foi possível completar a operação.');
  }
  return resposta.json();
}

function sair() {
  token = null;
  usuario = null;
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
  const link = elemento('a', 'postagem-resumo');
  link.href = `#/postagem/${post.id}`;
  link.append(
    meta(post, post.id),
    elemento('h2', 'titulo-postagem', titulo(post)),
    elemento('p', 'texto', post.texto),
    elemento('span', 'contagem', `${post.comentarios.length} comentário${post.comentarios.length === 1 ? '' : 's'} · abrir >>`)
  );
  return link;
}

async function carregarBoard() {
  try {
    const dados = await api('/api/postagens');
    const posts = Object.values(dados._embedded || {})[0] || [];
    $('postagens').replaceChildren(...(posts.length ? posts.map(resumo) : [elemento('p', 'vazio', 'Ainda não há postagens.') ]));
    $('total-postagens').textContent = posts.length === 1 ? '1 postagem' : `${posts.length} postagens`;
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
    const comentarios = post.comentarios.map(c => {
      const item = elemento('article', 'comentario');
      item.append(meta(c, c.id), elemento('p', 'texto', c.texto));
      return item;
    });
    $('comentarios').replaceChildren(...(comentarios.length ? comentarios : [elemento('p', 'vazio', 'Nenhum comentário ainda.') ]));
  } catch (e) { aviso(e.message, true); }
}

function mostrar() {
  const logado = Boolean(token && usuario);
  $('tela-auth').hidden = logado;
  $('menu').hidden = !logado;
  $('nome-usuario').textContent = logado ? usuario.nome : '';
  for (const id of ['tela-board', 'tela-nova', 'tela-postagem']) $(id).hidden = true;
  if (!logado) return;

  aviso('');
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
    if (cadastro) await api('/api/auth/cadastro', 'POST', {nome: $('nome').value, ...credenciais});
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
  try {
    const post = await api('/api/postagens', 'POST', {titulo: $('titulo-postagem').value, texto: $('texto-postagem').value});
    $('titulo-postagem').value = '';
    $('texto-postagem').value = '';
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
  try {
    await api(`/api/postagens/${postagemAtual}/comentarios`, 'POST', {texto: $('texto-comentario').value});
    $('texto-comentario').value = '';
    await carregarPostagem(postagemAtual);
    aviso('Comentário publicado.');
  } catch (e) { aviso(e.message, true); }
  finally { botao.disabled = false; }
});

$('atualizar').addEventListener('click', carregarBoard);
$('sair').addEventListener('click', () => { sair(); aviso(''); });
window.addEventListener('hashchange', mostrar);
mostrar();
