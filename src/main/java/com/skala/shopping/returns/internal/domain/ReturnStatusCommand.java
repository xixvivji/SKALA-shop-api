package com.skala.shopping.returns.internal.domain;

import com.skala.shopping.returns.ReturnView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "return_status_commands", schema = "returns")
public class ReturnStatusCommand {

    @Id
    @Column(name = "command_id")
    private UUID commandId;

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_status", nullable = false, length = 30)
    private ReturnStatus requestedStatus;

    @Column(name = "requested_admin_note", length = 500)
    private String requestedAdminNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 30)
    private ReturnStatus resultStatus;

    @Column(name = "result_balance_after", precision = 19, scale = 2)
    private BigDecimal resultBalanceAfter;

    @Column(name = "result_admin_note", length = 500)
    private String resultAdminNote;

    @Column(name = "result_updated_at", nullable = false)
    private Instant resultUpdatedAt;

    protected ReturnStatusCommand() {
    }

    public ReturnStatusCommand(
            UUID commandId,
            UUID returnId,
            UUID adminId,
            ReturnStatus requestedStatus,
            String requestedAdminNote,
            ReturnView result
    ) {
        this.commandId = commandId;
        this.returnId = returnId;
        this.adminId = adminId;
        this.requestedStatus = requestedStatus;
        this.requestedAdminNote = requestedAdminNote;
        this.resultStatus = ReturnStatus.valueOf(result.getStatus());
        this.resultBalanceAfter = result.getBalanceAfter();
        this.resultAdminNote = result.getAdminNote();
        this.resultUpdatedAt = result.getUpdatedAt();
    }

    public UUID returnId() {
        return returnId;
    }

    public boolean matches(
            UUID expectedReturnId,
            UUID expectedAdminId,
            ReturnStatus expectedStatus,
            String expectedAdminNote
    ) {
        return returnId.equals(expectedReturnId)
                && adminId.equals(expectedAdminId)
                && requestedStatus == expectedStatus
                && Objects.equals(requestedAdminNote, expectedAdminNote);
    }

    public ReturnView replay(ReturnRequest request) {
        return request.toView(resultStatus, resultBalanceAfter, resultAdminNote, resultUpdatedAt);
    }
}
