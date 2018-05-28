package org.redhat.syseng.soleng.rhpam;

import org.eclipse.jgit.api.Git;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

@javax.ws.rs.Path("/")
public class TestPerformance {
    private static Logger logger = Logger.getLogger(TestPerformance.class.getName());

    @GET
    @javax.ws.rs.Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response test(@DefaultValue("true") @QueryParam("multiCommit") boolean multiCommit) {
        try {
            logger.info("Starting with multiCommit " + multiCommit);
            Path gitPath = FileSystems.getDefault().getPath("/deployments/data/jgit");
            createGitDirectory(gitPath);

            long start = System.currentTimeMillis();

            Git git = Git.init().setDirectory(gitPath.toFile()).call();
            git.commit().setMessage("Empty git repo").call();

            createDirectoryStructure(gitPath, "/dirs.txt");
            createFiles(gitPath, multiCommit, "/files.txt");

            if( !multiCommit )
            {
                git.add().addFilepattern(gitPath.resolve("IT-Orders").getFileName().toString()).call();
                git.commit().setMessage("Created samples").call();
            }

            long elapsed = System.currentTimeMillis() - start;
            logger.info("Finished after " + elapsed + " milliseconds!");
            return Response.ok().build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed with exception", e);
            return Response.serverError().build();
        }
    }

    private void createGitDirectory(Path gitPath) throws IOException {
        if (Files.exists(gitPath)) {
            Files.walkFileTree(gitPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return super.visitFile(file, attrs);
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return super.postVisitDirectory(dir, exc);
                }
            });
        }
        try {
            Files.createDirectory(gitPath);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create " + gitPath, e);
        }
    }

    private void createDirectoryStructure(Path gitPath, String dirList) {
        InputStream directoryListStream = getClass().getResourceAsStream(dirList);
        new BufferedReader(new InputStreamReader(directoryListStream)).lines().forEach(dir -> {
            try {
                Files.createDirectory(gitPath.resolve(dir));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to create " + gitPath.resolve(dir), e);
            }
        });
    }

    private void createFiles(Path gitPath, boolean multiCommit, String fileList) throws IOException {
        Git git = Git.open(new File(gitPath.toFile(), ".git"));
        InputStream fileListStream = getClass().getResourceAsStream(fileList);
        new BufferedReader(new InputStreamReader(fileListStream)).lines().forEach(fileName -> {
            try {
                PrintWriter printWriter = new PrintWriter( new FileWriter(gitPath.resolve(fileName).toFile()) );
                InputStream fileContentStream = getClass().getResourceAsStream("/" + fileName);
                new BufferedReader(new InputStreamReader(fileContentStream)).lines().forEach(printWriter::println);
                printWriter.close();
                if( multiCommit )
                {
                    git.add().addFilepattern(gitPath.resolve(fileName).getFileName().toString()).call();
                    git.commit().setMessage("Committed " + fileName).call();
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to create " + gitPath.resolve(fileName), e);
            }
        });
    }
}
