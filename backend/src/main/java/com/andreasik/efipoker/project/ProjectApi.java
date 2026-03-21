package com.andreasik.efipoker.project;

import java.util.UUID;

/// Module API for project operations. Used by other modules to avoid direct repository access.
public interface ProjectApi {

  void validateProjectExists(UUID projectId);

  void validateAdminCodeForProject(UUID projectId, String adminCode);

  void validateAdminCodeBySlug(String slug, String adminCode);
}
