package org.facenet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.entity.rbac.Permission;
import org.facenet.entity.rbac.Role;
import org.facenet.entity.rbac.User;
import org.facenet.entity.protocol.Protocol;
import org.facenet.repository.protocol.ProtocolRepository;
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
    private final ProtocolRepository protocolRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        ensureSabusProtocol();
        ensureSerialProtocol();
        if (userRepository.count() > 0) {
            ensureFeatureRbac();
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

        ensureFeatureRbac();

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

    private void ensureFeatureRbac() {
        Permission dashboardView = ensurePermission("DASHBOARD_VIEW", "View operations dashboard");
        Permission reportView = ensurePermission("REPORT_VIEW", "Search and view reports");
        Permission reportExport = ensurePermission("REPORT_EXPORT", "Export report data");
        Permission configManage = ensurePermission("CONFIG_MANAGE", "Manage sensitive device configuration");
        Permission auditView = ensurePermission("AUDIT_VIEW", "View configuration audit history");
        Permission scaleView = ensurePermission("SCALE_VIEW", "View scales");

        grant("ADMIN", dashboardView, reportView, reportExport, configManage, auditView, scaleView);
        grant("MANAGER", dashboardView, reportView, reportExport, auditView, scaleView);
        grant("OPERATOR", dashboardView, reportView, scaleView);

        Role supervisor = ensureRole("SUPERVISOR", "Supervisor");
        supervisor.getPermissions().addAll(Set.of(dashboardView, reportView, reportExport, auditView, scaleView));
        roleRepository.save(supervisor);

        Role guest = ensureRole("GUEST", "Guest");
        guest.getPermissions().addAll(Set.of(dashboardView, reportView, scaleView));
        roleRepository.save(guest);
    }

    private Permission ensurePermission(String code, String description) {
        return permissionRepository.findByCode(code).orElseGet(() -> permissionRepository.save(
                Permission.builder().code(code).description(description).isActive(true).build()));
    }

    private Role ensureRole(String code, String name) {
        return roleRepository.findByCode(code).orElseGet(() -> roleRepository.save(
                Role.builder().code(code).name(name).isActive(true).permissions(new HashSet<>()).build()));
    }

    private void grant(String roleCode, Permission... permissions) {
        roleRepository.findByCode(roleCode).ifPresent(role -> {
            role.getPermissions().addAll(Set.of(permissions));
            roleRepository.save(role);
        });
    }

    private void ensureSabusProtocol() {
        Protocol protocol = protocolRepository.findByCode("SABUS").orElseGet(Protocol::new);
        protocol.setCode("SABUS");
        protocol.setName("SABUS ASCII");
        protocol.setDescription("Serial scale protocol using ![ID]FL[CR] and ![ID]NT[CR] commands");
        protocol.setConnectionType("SERIAL");
        protocol.setDefaultBaudRate(9600);
        protocol.setIsActive(true);
        protocol.setConfigTemplate("""
                {
                  "conn_params": {
                    "com_port": "COM1",
                    "baud_rate": 9600,
                    "data_bits": 8,
                    "stop_bits": 1,
                    "parity": "NONE",
                    "device_id": "01",
                    "device_id_width": 2,
                    "response_timeout_ms": 3000,
                    "reconnect_delay_ms": 5000,
                    "offline_threshold_ms": 60000,
                    "command_prefix": "!",
                    "command_terminator": "\\r"
                  },
                  "data_1": {
                    "name": "Instantaneous Flow",
                    "command": "FL",
                    "is_used": true
                  },
                  "data_2": {
                    "name": "Accumulated Total",
                    "command": "NT",
                    "is_used": true
                  }
                }
                """);
        protocolRepository.save(protocol);
    }

    private void ensureSerialProtocol() {
        Protocol protocol = protocolRepository.findByCode("SERIAL").orElseGet(Protocol::new);
        protocol.setCode("SERIAL");
        protocol.setName("Generic Serial ASCII");
        protocol.setDescription("Configurable serial ASCII protocol for streaming and request/response scales");
        protocol.setConnectionType("SERIAL");
        protocol.setDefaultBaudRate(9600);
        protocol.setIsActive(true);
        protocol.setConfigTemplate("""
                {
                  "conn_params": {
                    "com_port": "COM1",
                    "baud_rate": 9600,
                    "data_bits": 8,
                    "stop_bits": 1,
                    "parity": "NONE",
                    "charset": "US-ASCII",
                    "response_timeout_ms": 3000,
                    "reconnect_delay_ms": 5000,
                    "offline_threshold_ms": 60000,
                    "command_prefix": "",
                    "command_terminator": "\\r",
                    "response_terminator": "\\r",
                    "strip_echo": true,
                    "max_response_bytes": 1024
                  },
                  "data_1": {
                    "name": "Weight",
                    "command": "W",
                    "response_pattern": "([+-]?[0-9]+(?:[.,][0-9]+)?)",
                    "value_type": "NUMBER",
                    "is_used": true
                  }
                }
                """);
        protocolRepository.save(protocol);
    }
}
