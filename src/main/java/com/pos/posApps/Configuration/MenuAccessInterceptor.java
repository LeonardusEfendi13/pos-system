package com.pos.posApps.Configuration;

import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Service.AuthService;
import com.pos.posApps.Service.EffectiveAccess;
import com.pos.posApps.Service.MenuAccessService;
import com.pos.posApps.Util.MenuRoutePolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.pos.posApps.Constants.Constant.authSessionKey;

@Component
public class MenuAccessInterceptor implements HandlerInterceptor {
    private final AuthService authService;
    private final MenuAccessService menuAccessService;

    public MenuAccessInterceptor(AuthService authService, MenuAccessService menuAccessService) {
        this.authService = authService;
        this.menuAccessService = menuAccessService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (MenuRoutePolicy.isPublic(path) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        AccountEntity account = accountFrom(request);
        if (account == null) {
            return true;
        }
        if (MenuRoutePolicy.isSession(path) || MenuRoutePolicy.isShellRead(request.getMethod(), path)) {
            return true;
        }

        List<String> required = MenuRoutePolicy.requiredKeys(request.getMethod(), path);
        EffectiveAccess access = menuAccessService.resolve(account);
        boolean allowed = access != null && (required == null
                ? access.superAdmin()
                : access.allowsAny(required));

        if (allowed) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":false,\"message\":\"Anda tidak memiliki akses untuk ini!\"}");
        return false;
    }

    private AccountEntity accountFrom(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object token = session.getAttribute(authSessionKey);
        if (!(token instanceof String value) || value.isBlank()) {
            return null;
        }
        try {
            return authService.validateToken(value);
        } catch (Exception exception) {
            return null;
        }
    }
}
