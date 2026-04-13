CREATE DATABASE IF NOT EXISTS scm_mdm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS scm_purchase DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS scm_inventory DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE scm_mdm;
SOURCE E:/ideaProject/saas-wms-scm-demo/scm-mdm/src/main/resources/schema.sql;
SOURCE E:/ideaProject/saas-wms-scm-demo/scm-mdm/src/main/resources/data.sql;

USE scm_purchase;
SOURCE E:/ideaProject/saas-wms-scm-demo/scm-purchase/src/main/resources/schema.sql;
SOURCE E:/ideaProject/saas-wms-scm-demo/scm-purchase/src/main/resources/data.sql;

USE scm_inventory;
SOURCE E:/ideaProject/saas-wms-scm-demo/scm-inventory/src/main/resources/schema.sql;
SOURCE E:/ideaProject/saas-wms-scm-demo/scm-inventory/src/main/resources/data.sql;
