package com.andreasik.efipoker.project;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import liquibase.change.custom.CustomTaskChange;
import liquibase.change.custom.CustomTaskRollback;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.RollbackImpossibleException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * One-shot migration: hash all plaintext admin codes with BCrypt. Plaintext UUIDs (36 chars, no $
 * prefix) are hashed in place. Already-hashed values (starting with $2) are skipped.
 * ProjectService.adminCodeMatches() uses passwordEncoder.matches() for all codes.
 */
public class AdminCodeHashMigration implements CustomTaskChange, CustomTaskRollback {

  @Override
  public void execute(Database database) throws CustomChangeException {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    try {
      JdbcConnection conn = (JdbcConnection) database.getConnection();

      // Find all projects with plaintext admin codes (not starting with $2)
      record Row(UUID id, String adminCode) {}
      List<Row> toHash = new ArrayList<>();

      try (PreparedStatement select =
          conn.prepareStatement(
              "SELECT id, admin_code FROM projects WHERE admin_code NOT LIKE '$2%'")) {
        ResultSet rs = select.executeQuery();
        while (rs.next()) {
          toHash.add(new Row(rs.getObject("id", UUID.class), rs.getString("admin_code")));
        }
      }

      // Hash each plaintext code
      try (PreparedStatement update =
          conn.prepareStatement("UPDATE projects SET admin_code = ? WHERE id = ?")) {
        for (Row row : toHash) {
          update.setString(1, encoder.encode(row.adminCode()));
          update.setObject(2, row.id());
          update.addBatch();
        }
        update.executeBatch();
      }
    } catch (SQLException e) {
      throw new CustomChangeException("Failed to hash admin codes", e);
    } catch (Exception e) {
      throw new CustomChangeException("Unexpected error during admin code migration", e);
    }
  }

  @Override
  public void rollback(Database database) throws RollbackImpossibleException {
    // BCrypt hashing is one-way - cannot recover plaintext admin codes.
    // Rollback requires generating new admin codes for all projects.
    throw new RollbackImpossibleException(
        "Cannot rollback BCrypt hashing. Generate new admin codes manually if needed.");
  }

  @Override
  public String getConfirmationMessage() {
    return "All plaintext admin codes have been hashed with BCrypt";
  }

  @Override
  public void setUp() throws SetupException {}

  @Override
  public void setFileOpener(ResourceAccessor resourceAccessor) {}

  @Override
  public ValidationErrors validate(Database database) {
    return new ValidationErrors();
  }
}
