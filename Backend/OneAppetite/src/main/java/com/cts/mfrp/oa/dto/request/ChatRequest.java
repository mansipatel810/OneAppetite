package com.cts.mfrp.oa.dto.request;

import java.util.List;
import java.util.Map;

public record ChatRequest(String message, List<Map<String, String>> history) {}
