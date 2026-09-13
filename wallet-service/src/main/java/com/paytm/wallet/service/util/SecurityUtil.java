package com.paytm.wallet.service.util;

import com.paytm.wallet.common.dto.CreateWalletRequest;
import com.paytm.wallet.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {

    private SecurityUtil() {}

    public static String resolveUserId(CreateWalletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName().trim();
        }
        String userId = request != null ? request.userId() : null;
        if (userId == null || userId.isBlank()) {
            throw new ApiException("Authentication required", HttpStatus.UNAUTHORIZED);
        }
        return userId.trim();
    }
}
