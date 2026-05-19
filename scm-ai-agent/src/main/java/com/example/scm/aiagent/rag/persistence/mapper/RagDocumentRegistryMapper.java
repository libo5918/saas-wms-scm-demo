package com.example.scm.aiagent.rag.persistence.mapper;

import com.example.scm.aiagent.rag.persistence.po.RagDocumentRecordPO;
import com.example.scm.aiagent.rag.persistence.po.RagImportBatchPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * RAG 文档治理元数据 MyBatis Mapper。
 */
@Mapper
public interface RagDocumentRegistryMapper {

    /** 新增或更新文档治理元数据。 */
    int upsertDocument(RagDocumentRecordPO record);

    /** 按租户和知识库查询文档列表。 */
    List<RagDocumentRecordPO> selectDocuments(@Param("tenantId") Long tenantId,
                                              @Param("knowledgeBaseId") String knowledgeBaseId);

    /** 按租户、知识库和文档 ID 查询单个文档。 */
    RagDocumentRecordPO selectDocument(@Param("tenantId") Long tenantId,
                                       @Param("knowledgeBaseId") String knowledgeBaseId,
                                       @Param("documentId") String documentId);

    /** 按租户、知识库和文档 ID 删除文档治理记录。 */
    int deleteDocument(@Param("tenantId") Long tenantId,
                       @Param("knowledgeBaseId") String knowledgeBaseId,
                       @Param("documentId") String documentId);

    /** 新增或更新 docs 导入批次。 */
    int upsertImportBatch(RagImportBatchPO record);

    /** 删除某个批次下的文档关联，用于重复保存批次时先清理旧关联。 */
    int deleteImportDocuments(@Param("tenantId") Long tenantId,
                              @Param("importBatchId") String importBatchId);

    /** 新增批次和文档关联。 */
    int insertImportDocument(@Param("tenantId") Long tenantId,
                             @Param("importBatchId") String importBatchId,
                             @Param("knowledgeBaseId") String knowledgeBaseId,
                             @Param("documentId") String documentId);

    /** 查询租户下的导入批次列表。 */
    List<RagImportBatchPO> selectImportBatches(@Param("tenantId") Long tenantId);

    /** 查询单个导入批次。 */
    RagImportBatchPO selectImportBatch(@Param("tenantId") Long tenantId,
                                       @Param("importBatchId") String importBatchId);

    /** 查询某个导入批次关联的文档 ID。 */
    List<String> selectImportDocumentIds(@Param("tenantId") Long tenantId,
                                         @Param("importBatchId") String importBatchId);
}
