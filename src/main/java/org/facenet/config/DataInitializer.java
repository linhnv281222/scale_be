package org.facenet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.entity.rbac.Permission;
import org.facenet.entity.rbac.Role;
import org.facenet.entity.rbac.User;
import org.facenet.repository.rbac.PermissionRepository;
import org.facenet.repository.rbac.RoleRepository;
import org.facenet.repository.rbac.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Initialize default data on application startup
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            log.info("Data already initialized, skipping...");
            return;
        }

        log.info("Initializing default data...");

        // Create Permissions
        Permission userManage = createPermission("USER_MANAGE", "Manage users");
        Permission roleManage = createPermission("ROLE_MANAGE", "Manage roles");
        Permission scaleManage = createPermission("SCALE_MANAGE", "Manage scales");
        Permission scaleOperate = createPermission("SCALE_OPERATE", "Operate scales");
        Permission scaleView = createPermission("SCALE_VIEW", "View scales");

        // Create Roles
        Role adminRole = new Role();
        adminRole.setCode("ADMIN");
        adminRole.setName("Administrator");
        adminRole.setPermissions(new HashSet<>(Set.of(
            userManage, roleManage, scaleManage, scaleOperate, scaleView
        )));
        adminRole = roleRepository.save(adminRole);

        Role managerRole = new Role();
        managerRole.setCode("MANAGER");
        managerRole.setName("Manager");
        managerRole.setPermissions(new HashSet<>(Set.of(
            scaleManage, scaleOperate, scaleView
        )));
        managerRole = roleRepository.save(managerRole);

        Role operatorRole = new Role();
        operatorRole.setCode("OPERATOR");
        operatorRole.setName("Operator");
        operatorRole.setPermissions(new HashSet<>(Set.of(scaleOperate, scaleView)));
        operatorRole = roleRepository.save(operatorRole);

        // Create Users
        User admin = User.builder()
                .username("admin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("Administrator")
                .status((short) 1)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();
        userRepository.save(admin);

        User manager = User.builder()
                .username("manager")
                .passwordHash(passwordEncoder.encode("manager123"))
                .fullName("Manager User")
                .status((short) 1)
                .roles(new HashSet<>(Set.of(managerRole)))
                .build();
        userRepository.save(manager);

        User operator = User.builder()
                .username("operator")
                .passwordHash(passwordEncoder.encode("operator123"))
                .fullName("Operator User")
                .status((short) 1)
                .roles(new HashSet<>(Set.of(operatorRole)))
                .build();
        userRepository.save(operator);

        log.info("Default data initialized successfully!");
        log.info("Default users created:");
        log.info("  - admin/admin123 (ADMIN role)");
        log.info("  - manager/manager123 (MANAGER role)");
        log.info("  - operator/operator123 (OPERATOR role)");
    }

    private Permission createPermission(String code, String description) {
        Permission permission = Permission.builder()
                .code(code)
                .description(description)
                .build();
        return permissionRepository.save(permission);
    }
}
