package com.paytm.wallet.service.util;

import com.paytm.wallet.common.dto.CreateWalletRequest;
import com.paytm.wallet.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {

    private SecurityUtil() {}

    public static String resolveUserId(CreateWalletRequest request) {
        String userId = request != null ? request.userId() : null;
        if (userId == null || userId.isBlank()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                throw new ApiException("Authentication required", HttpStatus.UNAUTHORIZED);
            }
            userId = authentication.getName();
        }
        return userId.trim();
    }
}
