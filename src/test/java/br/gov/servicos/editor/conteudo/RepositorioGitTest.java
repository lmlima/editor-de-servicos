package br.gov.servicos.editor.conteudo;


import br.gov.servicos.editor.git.RepositorioConfig;
import br.gov.servicos.editor.git.RepositorioGit;
import lombok.SneakyThrows;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static br.gov.servicos.editor.utils.TestData.PROFILE;
import static br.gov.servicos.editor.utils.Unchecked.Supplier.uncheckedSupplier;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Arrays.asList;
import static org.eclipse.jgit.api.ListBranchCommand.ListMode.ALL;
import static org.eclipse.jgit.lib.Constants.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;

public class RepositorioGitTest {

    static File github;

    static {
        try {
            github = createTempDirectory("RepositorioGitTest-github").toFile();
            github.deleteOnExit();
            System.out.println("github = " + github);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    File upstream;
    File clone1;
    File clone2;

    RepositorioGit repo1;
    RepositorioGit repo2;

    @BeforeClass
    public static void setUpOrigin() {
        cloneBare("https://github.com/lmlima/cartas-de-teste.git", github);
    }

    @Before
    public void setUp() {
        upstream = tempFolder();
        clone1 = tempFolder();
        clone2 = tempFolder();

        cloneBare(github.toPath().toString(), upstream);
        clone(upstream.toPath().toString(), clone1);
        clone(upstream.toPath().toString(), clone2);

        System.out.println("test upstream" + upstream);
        System.out.println("test clone1" + clone1);
        System.out.println("test clone2" + clone2);

        repo1 = new RepositorioGit(new RepositorioConfig("", "", false, true, clone1));
        repo2 = new RepositorioGit(new RepositorioConfig("", "", false, true, clone2));
    }

    @Test
    public void fluxoDeMoverBranch() throws Exception {
        moveBranch(repo1);
        garanteQueBranchFoiMovida(repo1);
    }

    @Test
    public void fluxoDeRemoverServicoNaoPublicado() throws Exception {
        salvaAlteracao(repo1, "Teste");
        garanteQueAlteracaoFoiPara(upstream, "foo");

        repo1.comRepositorioAbertoNoBranch(R_HEADS + MASTER, uncheckedSupplier(() -> {
            repo1.deleteLocalBranch("foo"); //git branch -D foo
            repo1.deleteRemoteBranch(R_HEADS + "foo"); //git push :foo
            return null;
        }));

        assertFalse(repo1.existeBranch("foo"));
    }

    @Test
    public void fluxoDeRemoverServicoPublicado() throws Exception {
        salvaAlteracao(repo1, "teste");
        garanteQueAlteracaoFoiPara(upstream, "foo");

        repo1.comRepositorioAbertoNoBranch(R_HEADS + MASTER, uncheckedSupplier(() -> {
            Path arquivo = Paths.get("LICENSE");
            repo1.deleteLocalBranch("foo"); //git branch -D foo
            repo1.deleteRemoteBranch(R_HEADS + "foo"); //git push :foo
            repo1.remove(arquivo);
            repo1.commit(arquivo, "Apagou", PROFILE);
            repo1.push("master");
            return null;
        }));

        verificaSeBranchExisteLocalERemoto(clone1);

        repo1.comRepositorioAbertoNoBranch(R_HEADS + MASTER, uncheckedSupplier(() -> {
            Path arquivo = repo1.getCaminhoAbsoluto().resolve(Paths.get("LICENSE"));
            assertFalse(Files.exists(arquivo));
            return null;
        }));
    }

    @Test
    public void existeBranchQuandoSalvaAlteracao() throws Exception {
        salvaAlteracao(repo1, "Alteração");
        assertTrue(repo1.existeBranch("foo"));
    }

    @Test
    public void existeBranchQuandoApenasRemoto() throws Exception {
        salvaAlteracao(repo2, "Alteração");
        assertTrue(repo1.existeBranch("foo"));
    }

    @Test
    public void naoExisteBranch() throws Exception {
        assertFalse(repo1.existeBranch("foo2"));
    }

    @SneakyThrows
    private static void verificaSeBranchExisteLocalERemoto(File localRepo) {
        try (Git git = Git.open(localRepo)) {
            List<Ref> branchesList = git.branchList().setListMode(ALL).call();
            branchesList.stream().map(Ref::getName).map(n -> n.replaceAll(R_HEADS + '|' + R_REMOTES + "origin/", "")).forEach(System.out::println);
            Stream<String> branches = branchesList.stream().map(Ref::getName).map(n -> n.replaceAll(R_HEADS + '|' + R_REMOTES + "origin/", ""));
            assertTrue(branches.noneMatch(n -> n.equals("foo")));
        }
    }

    private static void moveBranch(RepositorioGit r) {
        r.comRepositorioAbertoNoBranch("foo-bar", uncheckedSupplier(() -> {
            Path origem = Paths.get("LICENSE");
            Path destino = Paths.get("baz-bar.md");
            r.moveBranchPara("baz-bar");
            Files.move(r.getCaminhoAbsoluto().resolve(origem), r.getCaminhoAbsoluto().resolve(destino));
            r.remove(origem);
            r.add(destino);
            r.commit(origem, "Renomeia \"foo-bar\" para \"baz-bar\"", PROFILE);
            r.commit(destino, "Renomeia \"foo-bar\" para \"baz-bar\"", PROFILE);
            r.push("baz-bar");
            return null;
        }));
    }

    private static void garanteQueBranchFoiMovida(RepositorioGit r) {
        assertTrue(r.branches().noneMatch(n -> n.equals("foo-bar")));
        assertTrue(r.branches().anyMatch(n -> n.equals("baz-bar")));

        r.comRepositorioAbertoNoBranch("baz-bar", uncheckedSupplier(() -> {
            Path antigo = r.getCaminhoAbsoluto().resolve(Paths.get("LICENSE"));
            Path novo = r.getCaminhoAbsoluto().resolve(Paths.get("baz-bar.md"));

            r.pull();

            assertTrue(Files.notExists(antigo));
            assertTrue(Files.exists(novo));
            return null;
        }));
    }

    private static void garanteQueAlteracaoFoiPublicada(File localRepo) throws IOException {
        garanteQueAlteracaoFoiPara(localRepo, MASTER);
    }

    private static void garanteQueAlteracaoFoiRecebidaPor(RepositorioGit r, String alteracao) {
        r.comRepositorioAbertoNoBranch("foo", uncheckedSupplier(() -> {
            Path relativo = Paths.get("LICENSE");
            Path absoluto = r.getCaminhoAbsoluto().resolve(relativo);

            r.pull();

            assertThat(Files.readAllLines(absoluto).get(0), is(alteracao));

            return null;
        }));
    }

    private static void garanteQueAlteracaoFoiPara(File localRepo, String branch) throws IOException {
        try (Git git = Git.open(localRepo)) {
            Ref foo = git.getRepository().getRef(branch);
            assertThat(foo, is(notNullValue()));

            RevCommit commit = new RevWalk(git.getRepository()).parseCommit(foo.getObjectId());
            assertThat(commit.getAuthorIdent().getName(), is("fulano"));
            assertThat(commit.getAuthorIdent().getEmailAddress(), is("servicos@planejamento.gov.br"));
            assertThat(commit.getFullMessage(), is("Alteração de teste"));
        }
    }

    private static void salvaAlteracao(RepositorioGit r, String alteracao) {
        r.comRepositorioAbertoNoBranch("foo", uncheckedSupplier(() -> {
            r.pull();

            Path relativo = Paths.get("LICENSE");
            Path absoluto = r.getCaminhoAbsoluto().resolve(relativo);

            Files.write(absoluto, asList(alteracao, "\n", absoluto.toString()), WRITE);
            r.commit(relativo, "Alteração de teste", PROFILE);

            r.push("foo");

            return null;
        }));
    }

    @SneakyThrows
    private static void clone(String from, File to) {
        Git.cloneRepository()
                .setURI(from)
                .setDirectory(to)
                .setProgressMonitor(new TextProgressMonitor())
                .call();
    }

    @SneakyThrows
    private static void cloneBare(String from, File to) {
        Git.cloneRepository()
                .setURI(from)
                .setDirectory(to)
                .setBare(true)
                .setProgressMonitor(new TextProgressMonitor())
                .call();
    }

    @SneakyThrows
    private static File tempFolder() {
        File t = createTempDirectory("test").toFile();
        t.deleteOnExit();
        return t;
    }

}
