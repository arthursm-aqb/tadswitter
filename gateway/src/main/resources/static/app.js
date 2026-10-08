const $ = id => document.getElementById(id);
let token = localStorage.getItem('tadswitter_token');
let usuario = JSON.parse(localStorage.getItem('tadswitter_usuario') || 'null');
let cadastro = false;

function aviso(mensagem, sucesso = false) {
  $('aviso').textContent = mensagem;
  $('aviso').className = sucesso ? 'sucesso' : '';
}
function mostrar() {
  const logado = Boolean(token && usuario);
  $('tela-auth').hidden = logado;
  $('tela-board').hidden = !logado;
  $('sair').hidden = !logado;
  $('nome-usuario').textContent = logado ? usuario.nome : '';
  if (logado) carregar();
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
    const mensagens = {400:'Confira os campos preenchidos.', 401:'Login ou senha incorretos.', 404:'Postagem não encontrada.', 409:'Este login já está em uso.', 503:'Uma das APIs internas está fora do ar.'};
    throw new Error(mensagens[resposta.status] || 'Não foi possível completar a operação.');
  }
  return resposta.json();
}
function sair() {
  token = null; usuario = null;
  localStorage.removeItem('tadswitter_token');
  localStorage.removeItem('tadswitter_usuario');
  mostrar();
}
function data(valor) { return new Date(valor).toLocaleString('pt-BR', {dateStyle:'short', timeStyle:'short'}); }
function elemento(tag, classe, texto) {
  const el = document.createElement(tag);
  if (classe) el.className = classe;
  if (texto !== undefined) el.textContent = texto;
  return el;
}
function renderizarPostagem(post) {
  const artigo = elemento('article', 'cartao postagem');
  const topo = elemento('div', 'post-topo');
  topo.append(elemento('span','autor', post.autorNome), elemento('time','data', data(post.criadoEm)));
  artigo.append(topo, elemento('p','texto',post.texto));
  const area = elemento('div','comentarios');
  area.append(elemento('h3','',`Comentários · ${post.comentarios.length}`));
  if (!post.comentarios.length) area.append(elemento('p','sem-comentarios','Seja o primeiro a comentar.'));
  for (const c of post.comentarios) {
    const item = elemento('div','comentario');
    const cabecalho = elemento('div','comentario-topo');
    cabecalho.append(elemento('span','autor',c.autorNome),elemento('time','data',data(c.criadoEm)));
    item.append(cabecalho,elemento('p','texto',c.texto));
    area.append(item);
  }
  const form = elemento('form','form-comentario');
  const input = elemento('input');
  input.required = true; input.maxLength = 5000; input.placeholder = 'Escreva um comentário...';
  input.setAttribute('aria-label','Novo comentário');
  const botao = elemento('button','primario','Comentar'); botao.type = 'submit';
  form.append(input,botao);
  form.addEventListener('submit', async e => {
    e.preventDefault(); botao.disabled = true;
    try { await api(`/api/postagens/${post.id}/comentarios`,'POST',{texto:input.value}); aviso('Comentário publicado.',true); await carregar(); }
    catch (erro) { aviso(erro.message); }
    finally { botao.disabled = false; }
  });
  area.append(form); artigo.append(area);
  return artigo;
}
async function carregar() {
  try {
    const dados = await api('/api/postagens');
    const posts = Object.values(dados._embedded || {})[0] || [];
    $('postagens').replaceChildren(...(posts.length ? posts.map(renderizarPostagem) : [elemento('div','cartao vazio','Ainda não há postagens. Comece a conversa!')]));
    $('total-postagens').textContent = `${posts.length} postagem${posts.length === 1 ? '' : 's'}`;
  } catch (erro) { aviso(erro.message); }
}

$('form-auth').addEventListener('submit', async e => {
  e.preventDefault();
  const botao = $('enviar-auth'); botao.disabled = true;
  try {
    const credenciais = {login:$('login').value, senha:$('senha').value};
    if (cadastro) await api('/api/auth/cadastro','POST',{nome:$('nome').value,...credenciais});
    const resposta = await api('/api/auth/login','POST',credenciais);
    token = resposta.token; usuario = resposta.usuario;
    localStorage.setItem('tadswitter_token',token);
    localStorage.setItem('tadswitter_usuario',JSON.stringify(usuario));
    aviso(''); mostrar();
  } catch (erro) { aviso(erro.message); }
  finally { botao.disabled = false; }
});
$('trocar-auth').addEventListener('click', () => {
  cadastro = !cadastro;
  $('campo-nome').hidden = !cadastro;
  $('nome').required = cadastro;
  $('enviar-auth').textContent = cadastro ? 'Criar conta' : 'Entrar';
  $('trocar-auth').textContent = cadastro ? 'Já tem conta? Entrar' : 'Não tem conta? Cadastre-se';
  $('senha').autocomplete = cadastro ? 'new-password' : 'current-password';
  aviso('');
});
$('form-postagem').addEventListener('submit', async e => {
  e.preventDefault();
  const botao = e.currentTarget.querySelector('button'); botao.disabled = true;
  try { await api('/api/postagens','POST',{texto:$('texto-postagem').value}); $('texto-postagem').value=''; aviso('Postagem publicada.',true); await carregar(); }
  catch (erro) { aviso(erro.message); }
  finally { botao.disabled = false; }
});
$('atualizar').addEventListener('click',carregar);
$('sair').addEventListener('click',() => {sair(); aviso('');});
mostrar();
