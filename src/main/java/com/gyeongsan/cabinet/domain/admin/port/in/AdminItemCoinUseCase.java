package com.gyeongsan.cabinet.domain.admin.port.in;

import com.gyeongsan.cabinet.adapter.in.web.admin.dto.CoinProvideRequest;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.CoinRevokeRequest;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.ItemGrantRequest;
import com.gyeongsan.cabinet.adapter.in.web.admin.dto.ItemRevokeRequest;

public interface AdminItemCoinUseCase {
    void provideCoin(String username, CoinProvideRequest request);

    void updateItemPrice(String itemName, Long newPrice);

    void grantItem(String username, ItemGrantRequest request);

    void revokeUserItem(String username, ItemRevokeRequest request);

    void revokeUserCoin(String username, CoinRevokeRequest request);
}
