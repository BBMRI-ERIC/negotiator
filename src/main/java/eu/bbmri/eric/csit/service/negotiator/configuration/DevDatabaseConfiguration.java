package eu.bbmri.eric.csit.service.negotiator.configuration;

import org.springframework.context.annotation.Bean;

import java.sql.SQLException;
import org.h2.tools.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev"})
public class DevDatabaseConfiguration {
    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2Server() throws SQLException {
        return Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092");
    }
}
