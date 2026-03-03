package com.andreasik.efipoker.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.andreasik.efipoker.shared.event.ProjectCreatedEvent;
import com.andreasik.efipoker.shared.test.BaseModuleTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.modulith.test.AssertablePublishedEvents;

@DisplayName("ProjectModuleTest")
@ApplicationModuleTest(mode = BootstrapMode.ALL_DEPENDENCIES)
class ProjectModuleTest extends BaseModuleTest {

  @Autowired private ProjectService projectService;
  @Autowired private AssertablePublishedEvents events;

  @Test
  void should_create_and_retrieve_project() {
    // Act
    Project created = projectService.createProject("Sprint Planning");

    // Assert
    Project retrieved = projectService.getProjectBySlug(created.slug());
    assertAll(
        () -> assertThat(retrieved.id()).isEqualTo(created.id()),
        () -> assertThat(retrieved.name()).isEqualTo("Sprint Planning"),
        () -> assertThat(retrieved.slug()).hasSize(8));

    assertThat(events)
        .contains(ProjectCreatedEvent.class)
        .matching(e -> e.projectId().equals(created.id()));
  }
}
