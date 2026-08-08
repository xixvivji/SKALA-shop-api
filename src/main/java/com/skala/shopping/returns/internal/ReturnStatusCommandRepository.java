package com.skala.shopping.returns.internal;

import com.skala.shopping.returns.internal.domain.ReturnStatusCommand;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReturnStatusCommandRepository extends JpaRepository<ReturnStatusCommand, UUID> {
}
