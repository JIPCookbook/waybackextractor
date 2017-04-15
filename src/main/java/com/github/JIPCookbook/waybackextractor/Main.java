package com.github.JIPCookbook.waybackextractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


public class Main {

    private final static String ROOT_DIR = "/tmp/web.archive.org/web";
    private final static String OUTPUT_DIR = "/tmp/www.lac.inpe.br/JIPCookbook";

    public static void main(String[] arg) {
        checkDirs();
        System.out.println("start");
        File rootFolder = new File(ROOT_DIR);
        for (File f : rootFolder.listFiles((File pathname) -> pathname.isDirectory() && !pathname.getAbsolutePath().contains("*") && new File(pathname+SUBPATH).exists())) {
            String rootPath = f.getAbsolutePath()+SUBPATH;
            syncDirectory(f, rootPath);
        }  
        System.out.println("done");   
    }
    private static final String SUBPATH = "/http/www.lac.inpe.br/JIPCookbook";

    private static void syncDirectory(File folder, String rootPath) {
        for(File f : folder.listFiles((File pathname) -> !pathname.getName().startsWith("."))) {
            if(f.isDirectory()) {
                syncDirectory(f, rootPath);
            } else {
                String path = f.getAbsolutePath().replace(rootPath, "").trim();
                checkDirectory(new File(OUTPUT_DIR+path));
                System.out.println("cp "+f.getAbsolutePath()+ " "+OUTPUT_DIR+path);                
            }
        }
    }
    
    private static void checkDirectory(File file) {
        File dir = file.getParentFile();
        if(!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    private static void scanDir() {
         Map<String, Long> files = new HashMap<>();
        System.out.println("start");
        File rootFolder = new File(ROOT_DIR);
        for (File f : rootFolder.listFiles((File pathname) -> pathname.isDirectory() && new File(pathname+SUBPATH).exists())) {
            files.put(f.getAbsolutePath(), size(f.toPath()));
        }
        Map<String, Long> sortedMap = sortByValue(files);
        for(String path : sortedMap.keySet()) {
            System.out.println(String.format("%10d %s", sortedMap.get(path), path));
        }
        System.out.println("done");       
    }
    
    private static void checkDirs() {
        File output = new File(OUTPUT_DIR);
        if(!output.exists()) {
            output.mkdirs();
        }
    }
    public static long size(Path path) {

        final AtomicLong size = new AtomicLong(0);

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                    size.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {

                    System.out.println("skipped: " + file + " (" + exc + ")");
                    // Skip folders that can't be traversed
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {

                    if (exc != null) {
                        System.out.println("had trouble traversing: " + dir + " (" + exc + ")");
                    }
                    // Ignore errors traversing a folder
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
        }

        return size.get();
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(/*Collections.reverseOrder()*/))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e2,
                        LinkedHashMap::new
                ));
    }
}
