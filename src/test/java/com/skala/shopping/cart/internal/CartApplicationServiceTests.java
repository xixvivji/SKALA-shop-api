package com.skala.shopping.cart.internal;

import static org.mockito.Mockito.verify;

import com.skala.shopping.catalog.CatalogApi;
import com.skala.shopping.catalog.ProductDeleted;
import com.skala.shopping.inventory.InventoryApi;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartApplicationServiceTests {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository itemRepository;
    @Mock private CatalogApi catalogApi;
    @Mock private InventoryApi inventoryApi;

    private CartApplicationService service;

    @BeforeEach
    void setUp() {
        service = new CartApplicationService(cartRepository, itemRepository, catalogApi, inventoryApi);
    }

    @Test
    void removesCartRowsMatchingEitherDeletedProductOrDeletedVariant() {
        UUID deletedCatalogItemId = UUID.randomUUID();

        service.removeDeletedProduct(new ProductDeleted(deletedCatalogItemId));

        verify(itemRepository).deleteAllByProductId(deletedCatalogItemId);
        verify(itemRepository).deleteAllByVariantId(deletedCatalogItemId);
    }
}
