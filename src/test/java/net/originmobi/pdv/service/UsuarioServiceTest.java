package net.originmobi.pdv.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.originmobi.pdv.model.GrupoUsuario;
import net.originmobi.pdv.model.Pessoa;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @InjectMocks
    private UsuarioService service;

    @Mock
    private UsuarioRepository usuarios;

    @Mock
    private GrupoUsuarioService grupos;

    @Test
    @DisplayName("Deve cadastrar novo usuário com sucesso e criptografar senha")
    void deveCadastrarUsuarioNovoComSucesso() {
        Usuario usuario = new Usuario();
        usuario.setCodigo(null);
        usuario.setUser("admin");
        usuario.setSenha("123");

        Pessoa pessoa = new Pessoa();
        pessoa.setCodigo(1L);
        usuario.setPessoa(pessoa);

        when(usuarios.findByUserEquals("admin")).thenReturn(null);
        when(usuarios.findByPessoaCodigoEquals(1L)).thenReturn(null);

        String resultado = service.cadastrar(usuario);

        assertEquals("Usuário salvo com sucesso", resultado);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());

        Usuario usuarioSalvo = captor.getValue();

        assertNotNull(usuarioSalvo.getData_cadastro());

        assertNotEquals("123", usuarioSalvo.getSenha());
        assertTrue(usuarioSalvo.getSenha().startsWith("$2a$"), "Senha deve estar em formato BCrypt");
    }

    @Test
    @DisplayName("Deve impedir cadastro se usuário já existe")
    void deveImpedirCadastroUsuarioDuplicado() {
        Usuario usuario = new Usuario();
        usuario.setCodigo(null);
        usuario.setUser("admin");
        usuario.setSenha("123");
        Pessoa pessoaMock = new Pessoa();
        pessoaMock.setCodigo(1L);
        usuario.setPessoa(pessoaMock);
        when(usuarios.findByUserEquals("admin")).thenReturn(new Usuario());

        String resultado = service.cadastrar(usuario);

        assertEquals("Usuário já existe", resultado);
        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("Deve impedir cadastro se pessoa já possui usuário")
    void deveImpedirCadastroPessoaVinculada() {
        Usuario usuario = new Usuario();
        usuario.setCodigo(null);
        usuario.setUser("novo_user");
        usuario.setSenha("123");

        Pessoa pessoa = new Pessoa();
        pessoa.setCodigo(50L);
        usuario.setPessoa(pessoa);

        when(usuarios.findByUserEquals("novo_user")).thenReturn(null);
        when(usuarios.findByPessoaCodigoEquals(50L)).thenReturn(new Usuario());

        String resultado = service.cadastrar(usuario);

        assertEquals("Pessoa já vinculada a outro usuário", resultado);
        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar usuário existente com sucesso")
    void deveAtualizarUsuarioComSucesso() {
        Usuario usuario = new Usuario();
        usuario.setCodigo(10L);
        usuario.setSenha("123");

        String resultado = service.cadastrar(usuario);

        assertEquals("Usuário atualizado com sucesso", resultado);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        assertNotEquals("123", captor.getValue().getSenha());
    }

    @Test
    @DisplayName("Deve tratar exceção ao atualizar usuário")
    void deveTratarErroNaAtualizacao() {
        Usuario usuario = new Usuario();
        usuario.setCodigo(10L);
        usuario.setSenha("123");

        doThrow(new RuntimeException("Erro de conexão DB")).when(usuarios).save(any(Usuario.class));

        String resultado = service.cadastrar(usuario);

        assertEquals("Erro de conexão DB", resultado);
    }

    @Test
    @DisplayName("Deve adicionar grupo ao usuário com sucesso")
    void deveAdicionarGrupoComSucesso() {
        Long codUsu = 1L;
        Long codGru = 100L;

        Usuario usuario = new Usuario();
        usuario.setGrupoUsuario(new ArrayList<>());

        GrupoUsuario grupo = new GrupoUsuario();
        grupo.setCodigo(codGru);

        when(usuarios.findByCodigoIn(codUsu)).thenReturn(usuario);
        when(grupos.buscaGrupo(codGru)).thenReturn(grupo);

        String resultado = service.addGrupo(codUsu, codGru);

        assertEquals("ok", resultado);
        verify(usuarios).save(usuario);
        assertTrue(usuario.getGrupoUsuario().contains(grupo));
    }

    @Test
    @DisplayName("Deve impedir adição de grupo duplicado")
    void deveImpedirAdicaoGrupoDuplicado() {
        Long codUsu = 1L;
        Long codGru = 100L;

        GrupoUsuario grupo = new GrupoUsuario();
        grupo.setCodigo(codGru);

        Usuario usuario = new Usuario();
        usuario.setGrupoUsuario(new ArrayList<>());
        usuario.getGrupoUsuario().add(grupo);

        when(usuarios.findByCodigoIn(codUsu)).thenReturn(usuario);
        when(grupos.buscaGrupo(codGru)).thenReturn(grupo);

        String resultado = service.addGrupo(codUsu, codGru);

        assertEquals("ja existe", resultado);
        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("Deve remover grupo do usuário com sucesso")
    void deveRemoverGrupoComSucesso() {
        Long codUsu = 1L;
        Long codGruRemover = 100L;
        Long codGruManter = 200L;

        Usuario usuarioMock = new Usuario();
        usuarioMock.setCodigo(codUsu);

        GrupoUsuario g1 = new GrupoUsuario();
        g1.setCodigo(codGruRemover);

        GrupoUsuario g2 = new GrupoUsuario();
        g2.setCodigo(codGruManter);

        List<GrupoUsuario> listaGrupos = new ArrayList<>(Arrays.asList(g1, g2));

        when(usuarios.findByCodigoIn(codUsu)).thenReturn(usuarioMock);
        when(grupos.buscaGrupo(codGruRemover)).thenReturn(g1);
        when(grupos.buscaGrupos(usuarioMock)).thenReturn(listaGrupos);

        String resultado = service.removeGrupo(codUsu, codGruRemover);

        assertEquals("ok", resultado);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());

        List<GrupoUsuario> gruposSalvos = captor.getValue().getGrupoUsuario();
        assertEquals(1, gruposSalvos.size());
        assertEquals(codGruManter, gruposSalvos.get(0).getCodigo());
    }

    @Test
    @DisplayName("Deve tratar exceção ao remover grupo")
    void deveTratarErroNaRemocao() {
        Long codUsu = 1L;
        Long codGru = 100L;

        Usuario usuario = new Usuario();
        GrupoUsuario grupo = new GrupoUsuario();
        grupo.setCodigo(codGru);

        when(usuarios.findByCodigoIn(codUsu)).thenReturn(usuario);
        when(grupos.buscaGrupo(codGru)).thenReturn(grupo);
        when(grupos.buscaGrupos(usuario)).thenReturn(new ArrayList<>(Arrays.asList(grupo)));

        doThrow(new RuntimeException("Erro ao salvar")).when(usuarios).save(any());

        String resultado = service.removeGrupo(codUsu, codGru);

        assertEquals("ok", resultado);
    }

    @Test
    @DisplayName("Deve buscar usuário por username")
    void deveBuscarUsuarioPorNome() {
        String username = "admin";
        Usuario esperado = new Usuario();
        esperado.setUser(username);

        when(usuarios.findByUserEquals(username)).thenReturn(esperado);

        Usuario retornado = service.buscaUsuario(username);

        assertEquals(esperado, retornado);
    }

    @Test
    @DisplayName("Deve listar todos os usuários")
    void deveListarUsuarios() {
        service.lista();
        verify(usuarios).findAll();
    }
}