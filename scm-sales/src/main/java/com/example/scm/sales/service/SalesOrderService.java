package com.example.scm.sales.service;

import com.example.scm.sales.dto.CreateSalesOrderRequest;
import com.example.scm.sales.vo.SalesOrderVO;

import java.util.List;

public interface SalesOrderService {
    SalesOrderVO create(CreateSalesOrderRequest request);
    SalesOrderVO getById(Long id);
    SalesOrderVO getByOrderNo(String orderNo);
    List<SalesOrderVO> list();
    SalesOrderVO retryLock(Long id);
    SalesOrderVO ship(Long id);
    SalesOrderVO retryShip(Long id);
    SalesOrderVO cancel(Long id);
}
