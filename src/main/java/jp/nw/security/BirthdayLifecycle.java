package jp.nw.security;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public final class BirthdayLifecycle implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        if (System.getenv("BIRTHDAY_KEYSTORE_PATH") == null) {
            event
                .getServletContext()
                .log(
                    "Birthday encryption is unconfigured; birthday registration/view/edit are unavailable."
                );
            return;
        }
        try {
            BirthdayRuntime.start(System.getenv());
        } catch (Exception e) {
            throw new IllegalStateException("Birthday encryption initialization failed", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        try {
            BirthdayRuntime.stop();
        } catch (Exception e) {
            event.getServletContext().log("Birthday encryption shutdown failed");
        }
    }
}
