package com.cts.mfrp.oa.dto.request;

public record CartRequest(

        Integer userId,
        Integer menuItemId,
        Integer quantity
) {}