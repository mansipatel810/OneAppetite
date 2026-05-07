package com.cts.mfrp.oa.model;

public enum OrderStatus {
    CART,        // open shopping cart, never visible to vendor
    PLACED,      // employee has paid; appears in vendor kanban
    PREPARING,   // vendor is cooking
    READY,       // vendor has finished; awaiting employee pickup
    PICKED_UP,   // employee collected the order — terminal state
    COMPLETED,   // legacy alias for PICKED_UP (kept for back-compat)
    PENDING      // legacy / unused
}
