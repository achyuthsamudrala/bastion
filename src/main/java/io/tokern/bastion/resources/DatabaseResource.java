package io.tokern.bastion.resources;

import io.dropwizard.auth.Auth;
import io.tokern.bastion.api.Database;
import io.tokern.bastion.api.User;
import io.tokern.bastion.db.DatabaseDAO;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/databases")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "DBADMIN"})
public class DatabaseResource {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseResource.class);

  private final Jdbi jdbi;

  public DatabaseResource(final Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @PermitAll
  @GET
  public List<Database> list(@Auth User principal) {
    List<Database> databases = jdbi.withExtension(DatabaseDAO.class, dao -> dao.listByOrgId(principal.orgId));
    logger.debug("Returning list of size: " + databases.size() + " for org: " + principal.orgId);
    return databases;
  }

  @PermitAll
  @GET
  @Path("/{databaseId}")
  public Database getDatabase(@Auth User principal, @PathParam("databaseId") final int databaseId) {
    return jdbi.withExtension(DatabaseDAO.class, dao -> dao.getById(databaseId, principal.orgId));
  }

  @DELETE
  @Path("/{databaseId}")
  public Response deleteDatabase(@Auth User principal, @PathParam("databaseId") final int databaseId) {
    jdbi.useExtension(DatabaseDAO.class, dao -> dao.deleteById(databaseId, principal.orgId));
    return Response.ok("Database '" + databaseId + "' deleted").build();
  }

  @POST
  public Database createDatabase(@Auth User principal, @Valid @NotNull Database database) {
    Long id = jdbi.withExtension(DatabaseDAO.class, dao -> dao.insert(database));
    return jdbi.withExtension(DatabaseDAO.class, dao -> dao.getById(id, principal.orgId));
  }

  @PUT
  @Path("/{databaseId}")
  public Response updateDatabase(@Auth User principal,
                                 @PathParam("databaseId") final int databaseId,
                                 @Valid @NotNull Database.UpdateRequest request) {
    Database inDb = jdbi.withExtension(DatabaseDAO.class, dao -> dao.getById(databaseId, principal.orgId));
    if (inDb != null) {
      Database updated = new Database(
          inDb.id,
          request.getJdbcUrl() != null ? request.getJdbcUrl() : inDb.jdbcUrl,
          request.getUserName() != null ? request.getUserName() : inDb.userName,
          request.getPassword() != null ? request.getPassword() : inDb.password,
          request.getType() != null ? request.getType() : inDb.type,
          inDb.orgId
      );
      jdbi.useExtension(DatabaseDAO.class, dao -> dao.update(updated));
      return Response.ok(updated).build();
    }

    return Response.status(Response.Status.NOT_FOUND).build();
  }
}