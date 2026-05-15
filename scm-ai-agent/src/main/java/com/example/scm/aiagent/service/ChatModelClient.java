package com.example.scm.aiagent.service;

import com.example.scm.aiagent.model.ChatModelInvocation;
import com.example.scm.aiagent.model.ChatModelResult;

public interface ChatModelClient {

    ChatModelResult chat(ChatModelInvocation invocation);
}
