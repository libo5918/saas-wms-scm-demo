package com.example.scm.aiagent.rag.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.rag.dto.RagDocsImportedDocument;
import com.example.scm.aiagent.rag.dto.RagDocsImportRequest;
import com.example.scm.aiagent.rag.dto.RagDocsImportResponse;
import com.example.scm.aiagent.rag.dto.RagDocumentUpsertRequest;
import com.example.scm.aiagent.rag.dto.RagDocumentUpsertResponse;
import com.example.scm.aiagent.rag.model.RagImportBatchRecord;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * docs Markdown 文档导入服务。
 *
 * <p>该服务只负责扫描、读取、转换项目文档，并复用 RagService 完成切片、Embedding 和向量写入。</p>
 */
@Slf4j
@Service
public class RagDocsImportService {

    private static final String IMPORT_SOURCE = "docs-auto-import";

    private final AiAgentProperties properties;
    private final RagService ragService;

    public RagDocsImportService(AiAgentProperties properties, RagService ragService) {
        this.properties = properties;
        this.ragService = ragService;
    }

    /**
     * 手动触发 docs 目录导入。
     *
     * @param request 本次导入参数，允许为空或部分为空
     * @param context 当前租户和用户上下文
     * @return 导入统计和文档摘要
     */
    public RagDocsImportResponse importDocs(RagDocsImportRequest request, AgentRequestContext context) {
        Instant start = Instant.now();
        RagDocsImportRequest safeRequest = request == null ? new RagDocsImportRequest() : request;
        AiAgentProperties.DocsImportProperties config = properties.getRag().getDocsImport();
        if (!config.isEnabled()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "RAG docs import is disabled");
        }

        Path projectRoot = resolveProjectRoot();
        Path scanRoot = resolveScanRoot(projectRoot, safeRequest.getScanRoot(), config.getRootPath());
        String knowledgeBaseId = resolveKnowledgeBaseId(safeRequest, config);
        String importBatchId = UUID.randomUUID().toString();
        List<String> includeDirectories = resolveList(safeRequest.getIncludeDirectories(), config.getIncludeDirectories());
        Set<String> supportedExtensions = normalizeExtensions(resolveList(safeRequest.getSupportedExtensions(),
                config.getSupportedExtensions()));
        int maxFiles = resolveMaxFiles(safeRequest.getMaxFiles(), config.getMaxFiles());

        List<Path> files = scanMarkdownFiles(scanRoot, includeDirectories, supportedExtensions);
        List<Path> importFiles = files.stream().limit(maxFiles).toList();
        List<RagDocsImportedDocument> importedDocuments = new ArrayList<>();
        int skippedCount = Math.max(0, files.size() - importFiles.size());

        for (Path file : importFiles) {
            try {
                RagDocumentUpsertRequest upsertRequest = toUpsertRequest(projectRoot, scanRoot, file, knowledgeBaseId);
                upsertRequest.setImportBatchId(importBatchId);
                RagDocumentUpsertResponse upsertResponse = ragService.upsertDocument(upsertRequest, context);
                importedDocuments.add(RagDocsImportedDocument.builder()
                        .documentId(upsertResponse.getDocumentId())
                        .title(upsertRequest.getTitle())
                        .source(upsertRequest.getSource())
                        .chunkCount(upsertResponse.getChunkCount())
                        .deletedCount(upsertResponse.getDeletedCount())
                        .importBatchId(importBatchId)
                        .build());
            } catch (IOException ex) {
                skippedCount++;
                log.warn("RAG docs import skipped unreadable file, tenantId={}, userId={}, knowledgeBaseId={}, file={}, errorType={}",
                        context.tenantId(), context.userId(), knowledgeBaseId, file.getFileName(),
                        ex.getClass().getSimpleName());
            }
        }

        long latencyMs = Duration.between(start, Instant.now()).toMillis();
        RagDocsImportResponse response = new RagDocsImportResponse();
        response.setTenantId(context.tenantId());
        response.setUserId(context.userId());
        response.setKnowledgeBaseId(knowledgeBaseId);
        response.setImportBatchId(importBatchId);
        response.setScanRoot(toDisplayPath(projectRoot, scanRoot));
        response.setFileCount(files.size());
        response.setImportedCount(importedDocuments.size());
        response.setSkippedCount(skippedCount);
        response.setVectorStoreMode(properties.getRag().getVectorStore().getMode());
        response.setEmbeddingMode(properties.getRag().getEmbedding().getMode());
        response.setEmbeddingModel(properties.getRag().getEmbedding().getModel());
        response.setLatencyMs(latencyMs);
        response.setDocuments(importedDocuments);
        ragService.saveImportBatch(RagImportBatchRecord.builder()
                .tenantId(context.tenantId())
                .userId(context.userId())
                .importBatchId(importBatchId)
                .knowledgeBaseId(knowledgeBaseId)
                .scanRoot(response.getScanRoot())
                .fileCount(files.size())
                .importedCount(importedDocuments.size())
                .skippedCount(skippedCount)
                .vectorStoreMode(response.getVectorStoreMode())
                .embeddingMode(response.getEmbeddingMode())
                .embeddingModel(response.getEmbeddingModel())
                .documentIds(importedDocuments.stream().map(RagDocsImportedDocument::getDocumentId).toList())
                .startedAt(start)
                .finishedAt(Instant.now())
                .latencyMs(latencyMs)
                .build());

        log.info("RAG docs import completed, tenantId={}, userId={}, knowledgeBaseId={}, importBatchId={}, registryMode={}, scanRoot={}, fileCount={}, importedCount={}, skippedCount={}, latencyMs={}",
                context.tenantId(), context.userId(), knowledgeBaseId, importBatchId,
                properties.getRag().getRegistry().getMode(), response.getScanRoot(),
                files.size(), importedDocuments.size(), skippedCount, latencyMs);
        return response;
    }

    /**
     * 扫描符合配置的 Markdown 文件，返回稳定排序后的文件列表。
     */
    List<Path> scanMarkdownFiles(Path scanRoot, List<String> includeDirectories, Set<String> supportedExtensions) {
        if (!Files.exists(scanRoot) || !Files.isDirectory(scanRoot)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(), "RAG docs scan root does not exist: " + scanRoot);
        }
        List<Path> roots = CollectionUtils.isEmpty(includeDirectories)
                ? List.of(scanRoot)
                : includeDirectories.stream()
                .filter(StringUtils::hasText)
                .map(scanRoot::resolve)
                .map(Path::normalize)
                .filter(path -> Files.exists(path) && Files.isDirectory(path))
                .toList();

        List<Path> files = new ArrayList<>();
        for (Path root : roots) {
            try (Stream<Path> stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> supportedExtensions.contains(extensionOf(path)))
                        .forEach(files::add);
            } catch (IOException ex) {
                throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "Failed to scan docs directory");
            }
        }
        return files.stream()
                .distinct()
                .sorted(Comparator.comparing(path -> path.toAbsolutePath().normalize().toString()))
                .toList();
    }

    /**
     * 将 Markdown 文件转换为 RagService 可写入的文档请求。
     */
    RagDocumentUpsertRequest toUpsertRequest(Path projectRoot, Path scanRoot, Path file, String knowledgeBaseId)
            throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        String source = toSourcePath(projectRoot, scanRoot, file);
        RagDocumentUpsertRequest request = new RagDocumentUpsertRequest();
        request.setKnowledgeBaseId(knowledgeBaseId);
        request.setDocumentId(stableDocumentId(source));
        request.setTitle(extractTitle(content, file));
        request.setSource(source);
        request.setContent(content);
        request.setMetadata(buildMetadata(projectRoot, scanRoot, file, source));
        return request;
    }

    /**
     * 根据相对 source 生成稳定 documentId，保证同一个文件重复导入不会改变 ID。
     */
    String stableDocumentId(String source) {
        String normalized = source == null ? "document" : source.replace('\\', '/').toLowerCase(Locale.ROOT);
        String slug = normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (!StringUtils.hasText(slug)) {
            slug = "document";
        }
        if (slug.length() > 80) {
            slug = slug.substring(0, 80).replaceAll("-+$", "");
        }
        return "doc-" + slug + "-" + sha256Hex(normalized).substring(0, 12);
    }

    /**
     * 提取 Markdown 一级标题；没有一级标题时使用文件名作为标题。
     */
    String extractTitle(String content, Path file) {
        if (StringUtils.hasText(content)) {
            for (String line : content.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("# ")) {
                    String title = trimmed.substring(2).trim();
                    if (StringUtils.hasText(title)) {
                        return title;
                    }
                }
            }
        }
        String fileName = file.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private Map<String, Object> buildMetadata(Path projectRoot, Path scanRoot, Path file, String source) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filePath", source);
        metadata.put("fileName", file.getFileName().toString());
        metadata.put("directory", toDirectoryPath(projectRoot, scanRoot, file));
        metadata.put("extension", extensionOf(file));
        metadata.put("importSource", IMPORT_SOURCE);
        return metadata;
    }

    private Path resolveProjectRoot() {
        Path userDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (Files.exists(userDir.resolve("docs"))) {
            return userDir;
        }
        Path parent = userDir.getParent();
        if (parent != null && Files.exists(parent.resolve("docs"))) {
            return parent.toAbsolutePath().normalize();
        }
        return userDir;
    }

    private Path resolveScanRoot(Path projectRoot, String requestScanRoot, String configScanRoot) {
        String root = StringUtils.hasText(requestScanRoot) ? requestScanRoot : configScanRoot;
        if (!StringUtils.hasText(root)) {
            root = "docs";
        }
        Path path = Paths.get(root);
        return path.isAbsolute() ? path.toAbsolutePath().normalize() : projectRoot.resolve(path).normalize();
    }

    private String resolveKnowledgeBaseId(RagDocsImportRequest request, AiAgentProperties.DocsImportProperties config) {
        if (StringUtils.hasText(request.getKnowledgeBaseId())) {
            return request.getKnowledgeBaseId();
        }
        return StringUtils.hasText(config.getKnowledgeBaseId()) ? config.getKnowledgeBaseId() : "kb-project-docs";
    }

    private List<String> resolveList(List<String> requestValues, List<String> configValues) {
        if (!CollectionUtils.isEmpty(requestValues)) {
            return requestValues;
        }
        return CollectionUtils.isEmpty(configValues) ? List.of() : configValues;
    }

    private int resolveMaxFiles(Integer requestMaxFiles, int configMaxFiles) {
        int maxFiles = requestMaxFiles != null ? requestMaxFiles : configMaxFiles;
        return Math.max(1, maxFiles);
    }

    private Set<String> normalizeExtensions(List<String> extensions) {
        Set<String> normalized = new HashSet<>();
        List<String> source = CollectionUtils.isEmpty(extensions) ? List.of(".md") : extensions;
        for (String extension : source) {
            if (!StringUtils.hasText(extension)) {
                continue;
            }
            String value = extension.trim().toLowerCase(Locale.ROOT);
            normalized.add(value.startsWith(".") ? value : "." + value);
        }
        return normalized.isEmpty() ? Set.of(".md") : normalized;
    }

    private String toSourcePath(Path projectRoot, Path scanRoot, Path file) {
        Path absoluteFile = file.toAbsolutePath().normalize();
        Path absoluteProjectRoot = projectRoot.toAbsolutePath().normalize();
        if (absoluteFile.startsWith(absoluteProjectRoot)) {
            return normalizePath(absoluteProjectRoot.relativize(absoluteFile));
        }
        return normalizePath(scanRoot.toAbsolutePath().normalize().relativize(absoluteFile));
    }

    private String toDirectoryPath(Path projectRoot, Path scanRoot, Path file) {
        Path parent = file.getParent();
        if (parent == null) {
            return "";
        }
        return toDisplayPath(projectRoot, parent).equals(toDisplayPath(projectRoot, scanRoot))
                ? ""
                : toDisplayPath(projectRoot, parent);
    }

    private String toDisplayPath(Path projectRoot, Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        Path absoluteProjectRoot = projectRoot.toAbsolutePath().normalize();
        if (absolute.startsWith(absoluteProjectRoot)) {
            return normalizePath(absoluteProjectRoot.relativize(absolute));
        }
        return normalizePath(absolute);
    }

    private String normalizePath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private String extensionOf(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(dotIndex).toLowerCase(Locale.ROOT) : "";
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }
}
