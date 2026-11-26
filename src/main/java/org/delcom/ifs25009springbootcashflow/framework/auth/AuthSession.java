package org.delcom.ifs25009springbootcashflow.framework.auth;

import org.delcom.ifs25009springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25009springbootcashflow.domain.entity.User;
import org.delcom.ifs25009springbootcashflow.domain.repository.IUserRepository;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import jakarta.servlet.http.Cookie;

/** Sesi login: VaadinSession + HTTP session + cookie bertanda tangan (pulih setelah refresh). */
public final class AuthSession {
    public static final String USER_ID_KEY = "cashflow_uid";
    public static final String LOGGED_OUT_KEY = "cashflow_logged_out";
    private static final String SECRET = System.getProperty("SESSION_SECRET",
            System.getProperty("ROOT_PASSWORD", "cashflow-secret"));

    private AuthSession() { }

    private static String sign(int id) {
        return id + "." + PasswordHasher.hash(id + ":" + SECRET).substring(0, 24);
    }

    public static void login(User user) {
        VaadinSession session = VaadinSession.getCurrent();
        session.setAttribute(USER_ID_KEY, user.getId());
        session.setAttribute(LOGGED_OUT_KEY, Boolean.FALSE);
        VaadinRequest request = VaadinService.getCurrentRequest();
        if (request != null) {
            request.getWrappedSession().setAttribute(LOGGED_OUT_KEY, Boolean.FALSE);
        }
        VaadinResponse response = VaadinService.getCurrentResponse();
        if (response != null) {
            Cookie cookie = new Cookie(USER_ID_KEY, sign(user.getId()));
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(8 * 60 * 60);
            response.addCookie(cookie);
        }
    }

    public static void logout() {
        VaadinSession session = VaadinSession.getCurrent();
        session.setAttribute(USER_ID_KEY, null);
        session.setAttribute(LOGGED_OUT_KEY, Boolean.TRUE);
        VaadinRequest request = VaadinService.getCurrentRequest();
        if (request != null) {
            request.getWrappedSession().setAttribute(LOGGED_OUT_KEY, Boolean.TRUE);
        }
        VaadinResponse response = VaadinService.getCurrentResponse();
        if (response != null) {
            Cookie cookie = new Cookie(USER_ID_KEY, "");
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }
    }

    /** Pengguna yang sedang login, atau null. Memulihkan sesi dari cookie bila perlu. */
    public static User currentUser(IUserRepository users) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null || Boolean.TRUE.equals(session.getAttribute(LOGGED_OUT_KEY))) {
            return null;
        }
        Object id = session.getAttribute(USER_ID_KEY);
        if (id == null) {
            VaadinRequest request = VaadinService.getCurrentRequest();
            if (request == null || request.getCookies() == null) {
                return null;
            }
            for (Cookie c : request.getCookies()) {
                if (USER_ID_KEY.equals(c.getName())) {
                    id = verify(c.getValue());
                }
            }
            if (id == null) {
                return null;
            }
            session.setAttribute(USER_ID_KEY, id);
        }
        try {
            return users.findById((Integer) id);
        } catch (Exception e) {
            session.setAttribute(USER_ID_KEY, null);
            return null;
        }
    }

    private static Integer verify(String value) {
        try {
            int dot = value.indexOf('.');
            int id = Integer.parseInt(value.substring(0, dot));
            return sign(id).equals(value) ? id : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
