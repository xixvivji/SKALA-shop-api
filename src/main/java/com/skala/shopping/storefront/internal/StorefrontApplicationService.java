package com.skala.shopping.storefront.internal;

import com.skala.shopping.auth.AuthAccountApi;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.member.MemberApi;
import com.skala.shopping.member.MemberResponse;
import com.skala.shopping.order.CancellationView;
import com.skala.shopping.order.OrderApi;
import com.skala.shopping.order.OrderView;
import com.skala.shopping.wallet.WalletApi;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@EnableConfigurationProperties(MemberRegistrationProperties.class)
public class StorefrontApplicationService {

    private final AuthAccountApi authAccountApi;
    private final MemberApi memberApi;
    private final WalletApi walletApi;
    private final OrderApi orderApi;
    private final MemberRegistrationProperties properties;

    public StorefrontApplicationService(
            AuthAccountApi authAccountApi,
            MemberApi memberApi,
            WalletApi walletApi,
            OrderApi orderApi,
            MemberRegistrationProperties properties
    ) {
        this.authAccountApi = authAccountApi;
        this.memberApi = memberApi;
        this.walletApi = walletApi;
        this.orderApi = orderApi;
        this.properties = properties;
    }

    @Transactional
    public RegistrationView register(String customerId, String password, String name) {
        UUID memberId = UUID.randomUUID();
        authAccountApi.createAccount(memberId, customerId, password);
        var member = memberApi.createMember(memberId, customerId, name);
        var balance = walletApi.openAccount(
                memberId,
                properties.getInitialPoints(),
                memberId,
                UUID.randomUUID()
        );
        return new RegistrationView(
                member.getId(),
                member.getCustomerId(),
                member.getName(),
                balance.getBalance()
        );
    }

    @Transactional(readOnly = true)
    public CustomerDetailView getCustomer(UUID authenticatedMemberId, String requestedCustomerId) {
        var member = memberApi.getMember(authenticatedMemberId);
        if (!member.getCustomerId().equals(requestedCustomerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return customerDetail(member);
    }

    @Transactional(readOnly = true)
    public CustomerDetailView getCurrentCustomer(UUID authenticatedMemberId) {
        return customerDetail(memberApi.getMember(authenticatedMemberId));
    }

    private CustomerDetailView customerDetail(MemberResponse member) {
        return new CustomerDetailView(
                member.getId(),
                member.getCustomerId(),
                member.getName(),
                walletApi.getBalance(member.getId()).getBalance(),
                orderApi.getPurchasedProducts(member.getId())
        );
    }

    @Transactional
    public OrderView placeOrder(
            UUID memberId,
            UUID productId,
            int quantity,
            UUID commandId
    ) {
        return orderApi.placeOrder(memberId, productId, quantity, commandId);
    }

    @Transactional
    public CancellationView cancelOrder(
            UUID memberId,
            UUID productId,
            int quantity,
            UUID commandId
    ) {
        return orderApi.cancelProduct(memberId, productId, quantity, commandId);
    }

    @Transactional
    public void deactivate(UUID memberId) {
        authAccountApi.deactivateAccount(memberId);
        memberApi.deactivateMember(memberId);
    }
}
