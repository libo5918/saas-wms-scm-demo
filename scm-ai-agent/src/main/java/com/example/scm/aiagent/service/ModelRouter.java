package com.example.scm.aiagent.service;

import com.example.scm.aiagent.model.ModelRoute;
import com.example.scm.aiagent.model.ModelRouteRequest;

public interface ModelRouter {

    ModelRoute route(ModelRouteRequest request);
}
