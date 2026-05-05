package com.example.fan_cafe.global.logging;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.type.filter.TypeFilter;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

@Configuration
@Profile("!prod")
public class DomainLoggerPackageConfig {

    private static final String BASE_PACKAGE = "com.example.fan_cafe";


//    {
//        "order" -> "com.example.fan_cafe.order",
//            "user" -> "com.example.fan_cafe.user",
//            "notification" -> "com.example.fan_cafe.notification"
//    }
    @Bean(name = "domainLoggerPackages")
    Map<String, String> domainLoggerPackages() {
        //클래스 스캔
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        //fan_cafe 하위 클래스
        scanner.addIncludeFilter((TypeFilter) (metadataReader, mrf) -> {
            String name = metadataReader.getClassMetadata().getClassName();
            return name.startsWith(BASE_PACKAGE + ".");
        });
        Map<String, String> map = new TreeMap<>();
        for (BeanDefinition bd : scanner.findCandidateComponents(BASE_PACKAGE)) {
            String className = bd.getBeanClassName();
            if (className == null) {
                continue;
            }
            int dollar = className.indexOf('$');
            if (dollar > 0) {
                className = className.substring(0, dollar);
            }
            int lastDot = className.lastIndexOf('.');
            if (lastDot <= 0) {
                continue;
            }
            String pkg = className.substring(0, lastDot);
            if (pkg.length() <= BASE_PACKAGE.length() || !pkg.startsWith(BASE_PACKAGE + ".")) {
                continue;
            }
            String rest = pkg.substring(BASE_PACKAGE.length() + 1);
            int sub = rest.indexOf('.');
            String domain = sub < 0 ? rest : rest.substring(0, sub);
            if (domain.isEmpty()) {
                continue;
            }
            map.putIfAbsent(domain, BASE_PACKAGE + "." + domain);
        }
        return Collections.unmodifiableMap(map);
    }
}
