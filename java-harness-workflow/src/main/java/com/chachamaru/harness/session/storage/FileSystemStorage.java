package com.chachamaru.harness.session.storage;

import com.chachamaru.harness.session.model.SessionMetadata;
import com.chachamaru.harness.session.model.SessionSaveResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 文件系统存储实现
 *
 * <p>将会话数据保存到本地文件系统，支持压缩、并发控制和原子写入。</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
public class FileSystemStorage implements SessionStorage {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemStorage.class);

    private final Path storageRoot;
    private final ObjectMapper objectMapper;
    private final long maxStorageBytes;
    private final Object lock = new Object();

    public FileSystemStorage(Path storageRoot, long maxStorageBytes) {
        this.storageRoot = storageRoot;
        this.maxStorageBytes = maxStorageBytes;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            logger.error("Failed to create storage directory: {}", storageRoot, e);
            throw new RuntimeException("Storage initialization failed", e);
        }
    }

    @Override
    public SessionSaveResult saveSession(String sessionId, String sessionData, SessionMetadata metadata) {
        synchronized (lock) {
            try {
                Path sessionDir = storageRoot.resolve(sessionId);
                Files.createDirectories(sessionDir);

                // 检查存储空间
                if (!hasEnoughSpace(sessionData.length())) {
                    cleanupOldSessions(getMaxToKeep(), getMaxAgeDays());
                    if (!hasEnoughSpace(sessionData.length())) {
                        return SessionSaveResult.failed("Insufficient storage space");
                    }
                }

                // 保存会话数据（压缩）
                Path dataFile = sessionDir.resolve("session.jsonl");
                if (sessionData.length() > 1024 * 1024) { // > 1MB
                    writeCompressedFile(dataFile, sessionData);
                } else {
                    writeAtomicFile(dataFile, sessionData);
                }

                // 保存元数据
                Path metadataFile = sessionDir.resolve("metadata.json");
                String metadataJson = objectMapper.writeValueAsString(metadata);
                writeAtomicFile(metadataFile, metadataJson);

                long totalSize = calculateDirectorySize(sessionDir);

                logger.info("Session saved successfully: {}", sessionId);
                return SessionSaveResult.success(sessionId, "Session saved successfully", totalSize);

            } catch (Exception e) {
                logger.error("Failed to save session: {}", sessionId, e);
                return SessionSaveResult.failed("Save failed: " + e.getMessage());
            }
        }
    }

    @Override
    public Optional<String> loadSessionData(String saveId) {
        Path sessionDir = storageRoot.resolve(saveId);
        if (!Files.exists(sessionDir)) {
            return Optional.empty();
        }

        try {
            Path dataFile = sessionDir.resolve("session.jsonl");
            if (!Files.exists(dataFile)) {
                return Optional.empty();
            }

            // 检查是否是压缩文件
            if (isCompressedFile(dataFile)) {
                return Optional.of(readCompressedFile(dataFile));
            } else {
                return Optional.of(Files.readString(dataFile));
            }
        } catch (Exception e) {
            logger.error("Failed to load session data: {}", saveId, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<SessionMetadata> loadMetadata(String saveId) {
        Path sessionDir = storageRoot.resolve(saveId);
        Path metadataFile = sessionDir.resolve("metadata.json");

        if (!Files.exists(metadataFile)) {
            return Optional.empty();
        }

        try {
            String metadataJson = Files.readString(metadataFile);
            SessionMetadata metadata = objectMapper.readValue(metadataJson, SessionMetadata.class);
            return Optional.of(metadata);
        } catch (Exception e) {
            logger.error("Failed to load metadata: {}", saveId, e);
            return Optional.empty();
        }
    }

    @Override
    public List<SessionMetadata> listSessions() {
        try {
            return Files.list(storageRoot)
                    .filter(Files::isDirectory)
                    .map(this::loadMetadataFromDirectory)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .sorted(Comparator.comparing(SessionMetadata::getTimestamp).reversed())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to list sessions", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<SessionMetadata> listRecentSessions(int limit) {
        List<SessionMetadata> allSessions = listSessions();
        return allSessions.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteSession(String saveId) {
        synchronized (lock) {
            try {
                Path sessionDir = storageRoot.resolve(saveId);
                if (!Files.exists(sessionDir)) {
                    return false;
                }

                deleteDirectory(sessionDir);
                logger.info("Session deleted: {}", saveId);
                return true;
            } catch (Exception e) {
                logger.error("Failed to delete session: {}", saveId, e);
                return false;
            }
        }
    }

    @Override
    public int cleanupOldSessions(int maxToKeep, int maxAgeDays) {
        synchronized (lock) {
            try {
                List<SessionMetadata> sessions = listSessions();
                Instant cutoff = Instant.now().minus(maxAgeDays, ChronoUnit.DAYS);

                int deletedCount = 0;

                // 按数量清理
                for (int i = maxToKeep; i < sessions.size(); i++) {
                    if (deleteSession(sessions.get(i).getSaveId())) {
                        deletedCount++;
                    }
                }

                // 按时间清理
                for (SessionMetadata session : sessions) {
                    if (session.getTimestamp().isBefore(cutoff)) {
                        if (deleteSession(session.getSaveId())) {
                            deletedCount++;
                        }
                    }
                }

                logger.info("Cleaned up {} old sessions", deletedCount);
                return deletedCount;

            } catch (Exception e) {
                logger.error("Failed to cleanup old sessions", e);
                return 0;
            }
        }
    }

    @Override
    public boolean healthCheck() {
        try {
            return Files.exists(storageRoot) &&
                   Files.isWritable(storageRoot) &&
                   calculateTotalStorageSize() < (maxStorageBytes * 0.95); // 使用 95% 阈值
        } catch (Exception e) {
            logger.error("Health check failed", e);
            return false;
        }
    }

    @Override
    public StorageInfo getStorageInfo() {
        long usedSize = calculateTotalStorageSize();
        int sessionCount = (int) listSessions().size();
        boolean healthy = Files.exists(storageRoot) && Files.isWritable(storageRoot);

        return new StorageInfo(maxStorageBytes, usedSize, sessionCount, healthy);
    }

    // Private helper methods

    private void writeAtomicFile(Path file, String content) throws IOException {
        Path tempFile = file.getParent().resolve(file.getFileName().toString() + ".tmp");
        try {
            Files.writeString(tempFile, content);
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            if (Files.exists(tempFile)) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    private void writeCompressedFile(Path file, String content) throws IOException {
        Path tempFile = file.getParent().resolve(file.getFileName().toString() + ".tmp.gz");
        try (OutputStream os = Files.newOutputStream(tempFile);
             GZIPOutputStream gzipOut = new GZIPOutputStream(os)) {
            gzipOut.write(content.getBytes());
        }
        Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private String readCompressedFile(Path file) throws IOException {
        try (InputStream is = Files.newInputStream(file);
             GZIPInputStream gzipIn = new GZIPInputStream(is);
             ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = gzipIn.read(buffer)) > 0) {
                result.write(buffer, 0, len);
            }
            return result.toString();
        }
    }

    private boolean isCompressedFile(Path file) throws IOException {
        try (InputStream is = Files.newInputStream(file)) {
            byte[] magic = new byte[2];
            int read = is.read(magic);
            return read == 2 && magic[0] == (byte) 0x1f && magic[1] == (byte) 0x8b;
        }
    }

    private Optional<SessionMetadata> loadMetadataFromDirectory(Path sessionDir) {
        Path metadataFile = sessionDir.resolve("metadata.json");
        if (!Files.exists(metadataFile)) {
            return Optional.empty();
        }

        try {
            String metadataJson = Files.readString(metadataFile);
            SessionMetadata metadata = objectMapper.readValue(metadataJson, SessionMetadata.class);
            return Optional.of(metadata);
        } catch (Exception e) {
            logger.warn("Failed to load metadata from directory: {}", sessionDir, e);
            return Optional.empty();
        }
    }

    private boolean hasEnoughSpace(int requiredBytes) {
        StorageInfo info = getStorageInfo();
        return (info.getUsedSizeBytes() + requiredBytes) <= info.getTotalSizeBytes();
    }

    private long calculateTotalStorageSize() {
        try {
            return Files.walk(storageRoot)
                    .filter(Files::isRegularFile)
                    .mapToLong(FileSystemStorage::calculateFileSize)
                    .sum();
        } catch (Exception e) {
            logger.error("Failed to calculate total storage size", e);
            return 0;
        }
    }

    private long calculateDirectorySize(Path directory) {
        try {
            return Files.walk(directory)
                    .filter(Files::isRegularFile)
                    .mapToLong(FileSystemStorage::calculateFileSize)
                    .sum();
        } catch (Exception e) {
            logger.error("Failed to calculate directory size: {}", directory, e);
            return 0;
        }
    }

    private static long calculateFileSize(Path file) {
        try {
            return Files.size(file);
        } catch (Exception e) {
            return 0;
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        logger.warn("Failed to delete file: {}", path, e);
                    }
                });
    }

    private int getMaxToKeep() {
        return 10; // 默认保留10个保存
    }

    private int getMaxAgeDays() {
        return 7; // 默认保留7天
    }
}