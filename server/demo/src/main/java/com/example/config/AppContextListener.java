package com.example.config;

import com.example.config.AppConfig;
import com.example.repository.AddressRepository;
import com.example.repository.UserRepository;
import com.example.service.AddressService;
import com.example.service.AuthService;
import com.example.service.UserService;
import com.example.util.ValidationUtil;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(AppContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Initializing application context...");

        // Initialize the bean validator
        ValidationUtil.initialize();

        // Load application configuration
        AppConfig appConfig = new AppConfig();

        // NOTE: Ensure "login-app-pu" matches the persistence-unit name in your persistence.xml
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("login-app-pu");

        ServletContext context = sce.getServletContext();
        context.setAttribute("entityManagerFactory", emf);

        // Initialize repositories (assuming they now take an EntityManagerFactory)
        UserRepository userRepository = new UserRepository(emf);
        AddressRepository addressRepository = new AddressRepository(emf);

        // Initialize services with dependency injection
        UserService userService = new UserService(userRepository);
        AddressService addressService = new AddressService(addressRepository);
        AuthService authService = new AuthService(userService, appConfig);

        // Store services in servlet context to be used by servlets
        context.setAttribute("userService", userService);
        context.setAttribute("addressService", addressService);
        context.setAttribute("authService", authService);
        context.setAttribute("appConfig", appConfig);

        logger.info("Application context initialized successfully.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        EntityManagerFactory emf = (EntityManagerFactory) sce.getServletContext().getAttribute("entityManagerFactory");
        if (emf != null && emf.isOpen()) {
            emf.close();
            logger.info("EntityManagerFactory closed.");
        }
        logger.info("Application context destroyed.");
    }
}