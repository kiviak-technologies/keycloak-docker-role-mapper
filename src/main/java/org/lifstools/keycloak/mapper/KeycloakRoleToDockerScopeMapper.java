/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lifstools.keycloak.mapper;

import java.util.Set;
import java.util.stream.Collectors;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.docker.DockerAuthV2Protocol;
import org.keycloak.protocol.docker.mapper.DockerAuthV2AttributeMapper;
import org.keycloak.protocol.docker.mapper.DockerAuthV2ProtocolMapper;
import org.keycloak.representations.docker.DockerAccess;
import org.keycloak.representations.docker.DockerResponseToken;

/**
 * Maps Keycloak user roles docker-pull and docker-push to the respective pull
 * and push scopes for Docker registry authentication.
 *
 * @author nilshoffmann
 */
public class KeycloakRoleToDockerScopeMapper extends DockerAuthV2ProtocolMapper implements DockerAuthV2AttributeMapper {

    public static final String MAPPER_ID = "docker-v2-group-to-scope-mapper";

    private static final String DOCKER_PULL_ROLE = "docker-pull";

    private static final String DOCKER_PUSH_ROLE = "docker-push";

    private static final String REGISTRY_RESOURCE = "registry";

    private static final String RESOURCE_NAME = "https://docker.lifs-tools.org";

    public KeycloakRoleToDockerScopeMapper() {
    }

    @Override
    public String getDisplayType() {
        return "User role to scope mapping";
    }

    @Override
    public String getId() {
        return MAPPER_ID;
    }

    @Override
    public String getHelpText() {
        return "Allows to map between client roles docker-push and docker-pull and docker scopes pull and push on the complete repository.";
    }

    @Override
    public boolean appliesTo(DockerResponseToken drt) {
        return true;
    }

    @Override
    public DockerResponseToken transformDockerResponseToken(DockerResponseToken drt, ProtocolMapperModel pmm, KeycloakSession ks, UserSessionModel usm, AuthenticatedClientSessionModel acsm) {
        // clear any pre-existing permissions
        drt.getAccessItems().clear();
        final String requestedScope = acsm.getNote(DockerAuthV2Protocol.SCOPE_PARAM);

        //If no scope is requested (e.g. login), return empty list of access items.
        if (requestedScope == null) {
            return drt;
        }

        Set<String> userRoleNames = usm.getUser().getRoleMappingsStream()
                .map(role -> role.getName()).collect(Collectors.toSet());

        //Check if user's roles contain at least one of docker-pull or docker-push, deny access otherwise (empty resources)
        if (!userRoleNames.contains(DOCKER_PULL_ROLE) && !userRoleNames.contains(DOCKER_PUSH_ROLE)) {
            return drt;
        }

        // grant access based on user's assigned roles
        final DockerAccess requestedAccess = new DockerAccess(requestedScope);
        if (userRoleNames.contains(DOCKER_PULL_ROLE)) {
            drt.getAccessItems().add(requestedAccess);
        }
        if (userRoleNames.contains(DOCKER_PUSH_ROLE)) {
            drt.getAccessItems().add(requestedAccess);
        }
        return drt;
    }

}
