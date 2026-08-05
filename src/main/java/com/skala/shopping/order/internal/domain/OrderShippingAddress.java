package com.skala.shopping.order.internal.domain;

import com.skala.shopping.order.ShippingAddressCommand;
import com.skala.shopping.order.ShippingAddressView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "order_shipping_addresses", schema = "orders")
public class OrderShippingAddress {
    @Id @Column(name = "order_id") private UUID orderId;
    @Column(name = "recipient_name", nullable = false) private String recipientName;
    @Column(name = "phone_number", nullable = false) private String phoneNumber;
    @Column(name = "postal_code", nullable = false) private String postalCode;
    @Column(name = "address_line1", nullable = false) private String addressLine1;
    @Column(name = "address_line2") private String addressLine2;
    protected OrderShippingAddress() { }
    public OrderShippingAddress(UUID orderId, ShippingAddressCommand address) {
        this.orderId = orderId; this.recipientName = address.getRecipientName();
        this.phoneNumber = address.getPhoneNumber(); this.postalCode = address.getPostalCode();
        this.addressLine1 = address.getAddressLine1(); this.addressLine2 = address.getAddressLine2();
    }
    public UUID orderId(){return orderId;}
    public ShippingAddressView toView(){return new ShippingAddressView(recipientName,phoneNumber,postalCode,addressLine1,addressLine2);}
}
