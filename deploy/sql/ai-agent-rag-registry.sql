-- AI Agent Phase 3.7: RAG 文档治理元数据 MySQL 表结构
-- 说明：当前脚本只持久化 Document Registry / Import Batch 元数据，不保存向量；向量仍由 Milvus 或其它 VectorStore 承载。
use scm_ai_agent;
CREATE TABLE IF NOT EXISTS rag_document_registry (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    tenant_id BIGINT NOT NULL COMMENT '租户 ID',
    knowledge_base_id VARCHAR(128) NOT NULL COMMENT '知识库 ID',
    document_id VARCHAR(256) NOT NULL COMMENT '文档 ID',
    title VARCHAR(512) NOT NULL COMMENT '文档标题',
    source VARCHAR(1024) NULL COMMENT '文档来源',
    file_path VARCHAR(1024) NULL COMMENT '文件路径',
    file_name VARCHAR(255) NULL COMMENT '文件名',
    directory VARCHAR(512) NULL COMMENT '所在目录',
    import_source VARCHAR(128) NULL COMMENT '导入来源',
    chunk_count INT NOT NULL DEFAULT 0 COMMENT '当前文档 chunk 数量',
    deleted_count BIGINT NOT NULL DEFAULT 0 COMMENT '最近一次写入前删除的旧 chunk 数量',
    embedding_mode VARCHAR(64) NULL COMMENT 'Embedding 模式',
    embedding_model VARCHAR(128) NULL COMMENT 'Embedding 模型',
    vector_store_mode VARCHAR(64) NULL COMMENT '向量存储模式',
    import_batch_id VARCHAR(64) NULL COMMENT '最近一次导入批次 ID',
    metadata_json JSON NULL COMMENT '扩展元数据 JSON',
    imported_at DATETIME(3) NULL COMMENT '首次导入时间',
    updated_at DATETIME(3) NULL COMMENT '最近更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识，0=有效，1=已删除',
    deleted_at DATETIME(3) NULL COMMENT '逻辑删除时间',
    deleted_by BIGINT NULL COMMENT '逻辑删除操作人',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '记录创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rag_doc_tenant_kb_doc (tenant_id, knowledge_base_id, document_id),
    KEY idx_rag_doc_tenant_kb_updated (tenant_id, knowledge_base_id, deleted, updated_at),
    KEY idx_rag_doc_import_batch (tenant_id, import_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RAG 文档治理元数据表';

CREATE TABLE IF NOT EXISTS rag_import_batch (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    tenant_id BIGINT NOT NULL COMMENT '租户 ID',
    import_batch_id VARCHAR(64) NOT NULL COMMENT '导入批次 ID',
    user_id BIGINT NOT NULL COMMENT '触发导入的用户 ID',
    knowledge_base_id VARCHAR(128) NOT NULL COMMENT '知识库 ID',
    scan_root VARCHAR(1024) NULL COMMENT '扫描根目录',
    file_count INT NOT NULL DEFAULT 0 COMMENT '扫描命中文件数量',
    imported_count INT NOT NULL DEFAULT 0 COMMENT '成功导入数量',
    skipped_count INT NOT NULL DEFAULT 0 COMMENT '跳过数量',
    vector_store_mode VARCHAR(64) NULL COMMENT '向量存储模式',
    embedding_mode VARCHAR(64) NULL COMMENT 'Embedding 模式',
    embedding_model VARCHAR(128) NULL COMMENT 'Embedding 模型',
    started_at DATETIME(3) NULL COMMENT '导入开始时间',
    finished_at DATETIME(3) NULL COMMENT '导入完成时间',
    latency_ms BIGINT NOT NULL DEFAULT 0 COMMENT '导入耗时毫秒',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '记录创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rag_batch_tenant_batch (tenant_id, import_batch_id),
    KEY idx_rag_batch_tenant_started (tenant_id, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RAG docs 导入批次表';

CREATE TABLE IF NOT EXISTS rag_import_document (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    tenant_id BIGINT NOT NULL COMMENT '租户 ID',
    import_batch_id VARCHAR(64) NOT NULL COMMENT '导入批次 ID',
    knowledge_base_id VARCHAR(128) NOT NULL COMMENT '知识库 ID',
    document_id VARCHAR(256) NOT NULL COMMENT '文档 ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '记录创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rag_import_doc (tenant_id, import_batch_id, knowledge_base_id, document_id),
    KEY idx_rag_import_doc_batch (tenant_id, import_batch_id),
    KEY idx_rag_import_doc_document (tenant_id, knowledge_base_id, document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='RAG 导入批次文档关联表';
