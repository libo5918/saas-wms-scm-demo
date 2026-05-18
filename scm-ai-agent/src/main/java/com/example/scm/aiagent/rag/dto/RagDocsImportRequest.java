package com.example.scm.aiagent.rag.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * docs 目录导入 RAG 知识库的手动触发请求。
 *
 * <p>字段都允许为空；为空时使用 ai.agent.rag.docs-import 下的默认配置。</p>
 */
@Getter
@Setter
public class RagDocsImportRequest {

    /** 本次导入使用的 docs 根目录，支持相对项目根目录路径，例如 docs。 */
    private String scanRoot;

    /** 本次导入写入的知识库 ID，不传时使用配置中的默认知识库。 */
    private String knowledgeBaseId;

    /** 需要扫描的子目录列表，例如 architecture、business、operations、database。 */
    private List<String> includeDirectories = new ArrayList<>();

    /** 支持导入的文件后缀，默认只导入 .md。 */
    private List<String> supportedExtensions = new ArrayList<>();

    /** 单次最大导入文件数，用于避免误扫大目录。 */
    private Integer maxFiles;
}
