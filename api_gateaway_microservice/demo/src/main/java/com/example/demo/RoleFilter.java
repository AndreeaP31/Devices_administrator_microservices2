package com.example.demo;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoleFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        // 🔥 ALWAYS use the cached request wrapper
        CachedBodyHttpServletRequest request =
                req instanceof CachedBodyHttpServletRequest
                        ? (CachedBodyHttpServletRequest) req
                        : new CachedBodyHttpServletRequest((HttpServletRequest) req);

        HttpServletResponse response = (HttpServletResponse) res;

        System.out.println("🛡️ RoleFilter - Processing: " + request.getMethod() + " " + request.getRequestURI());

        /**
         * ✅ FIX CORS: bypass OPTIONS preflight request
         * Aceasta este modificarea necesară!
         */
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            System.out.println("   🟡 OPTIONS preflight → bypass role check");
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        if (path.startsWith("/api")) {
            path = path.substring(4);
        }

        System.out.println("   Path after prefix removal: " + path);

        // PUBLIC endpoints
        if (path.startsWith("/auth")) {
            System.out.println("   ✅ Public auth endpoint - allowing");
            chain.doFilter(request, response);
            return;
        }

        // Internal user creation
        if (path.equals("/users") && "POST".equals(request.getMethod())) {
            System.out.println("   ✅ POST /users - allowing (internal call)");
            chain.doFilter(request, response);
            return;
        }

        // Get role + userId from JwtAuthenticationFilter
        String role = (String) request.getAttribute("role");
        Object userId = request.getAttribute("userId");

        System.out.println("   Role: " + role + ", UserId: " + userId);

        if (role == null) {
            System.out.println("   ⚠️ No role found - passing through");
            chain.doFilter(request, response);
            return;
        }

        // ADMIN rules
        if (path.startsWith("/users")) {
            if (!"ADMIN".equals(role)) {
                System.err.println("   ❌ Access denied - ADMIN role required");
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "ADMIN only");
                return;
            }
            System.out.println("   ✅ ADMIN access granted");
        }

        // DEVICE rules
        if (path.startsWith("/device")) {
            if ("ADMIN".equals(role)) {
                System.out.println("   ✅ ADMIN can access all devices");
                chain.doFilter(request, response);
                return;
            }

            if (path.matches("/device/.+/for-user/devices")) {
                String[] parts = path.split("/");
                String userIdFromPath = parts[2];
                String userIdStr = userId != null ? userId.toString() : null;

                if (userIdStr != null && userIdStr.equals(userIdFromPath)) {
                    System.out.println("   ✅ CLIENT accessing own devices");
                    chain.doFilter(request, response);
                    return;
                }

                System.err.println("   ❌ CLIENT can only access own devices");
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "CLIENT can only see own devices");
                return;
            }


            System.err.println("   ❌ CLIENT forbidden");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "CLIENT forbidden");
            return;
        }

        if (path.startsWith("/monitoring")) {
            // Permitem accesul atât pentru ADMIN cât și pentru CLIENT
            // Deoarece userul trebuie să fie autentificat (trece de JwtFilter), e safe.
            if ("ADMIN".equals(role) || "CLIENT".equals(role)) {
                System.out.println("   ✅ Monitoring access granted for: " + role);
                chain.doFilter(request, response);
                return;
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Monitoring access denied");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
