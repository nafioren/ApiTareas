# ---------- Etapa 1: compilar con Maven ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Primero solo el pom: si no cambian las dependencias, Docker reutiliza esta capa
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package

# Bajar el driver de MySQL desde Maven Central
ARG MYSQL_CONNECTOR_VERSION=9.1.0
RUN mvn -B -q dependency:copy \
        -Dartifact=com.mysql:mysql-connector-j:${MYSQL_CONNECTOR_VERSION} \
        -DoutputDirectory=/drivers \
    && mv /drivers/mysql-connector-j-*.jar /drivers/mysql-connector-j.jar

# ---------- Etapa 2: imagen final con WildFly ----------
FROM quay.io/wildfly/wildfly:latest-jdk21

COPY --from=build --chown=jboss:jboss /drivers/mysql-connector-j.jar /tmp/
COPY --chown=jboss:jboss docker/configurar-wildfly.cli /tmp/

RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/tmp/configurar-wildfly.cli \
    && rm -rf /tmp/*.cli /tmp/*.jar \
              /opt/jboss/wildfly/standalone/configuration/standalone_xml_history

COPY --from=build --chown=jboss:jboss /app/target/api-tareas.war /opt/jboss/wildfly/standalone/deployments/

# La imagen oficial ya corre con el usuario "jboss", no como root.
EXPOSE 8080
