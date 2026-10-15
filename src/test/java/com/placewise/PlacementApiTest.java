package com.placewise;

import com.fasterxml.jackson.databind.*;
import com.placewise.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class PlacementApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired DriveRepository drives;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtEncoder encoder;
    @Autowired Clock clock;

    record Account(String token, String email, String roll) {}
    String unique() { return UUID.randomUUID().toString().substring(0,8); }
    JsonNode response(MockHttpServletRequestBuilder request, int expected) throws Exception {
        String body=mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return body.isEmpty() ? json.nullNode() : json.readTree(body);
    }
    MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request,Object value) throws Exception {
        return request.contentType("application/json").content(json.writeValueAsString(value));
    }
    MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request,String token) { return request.header("Authorization","Bearer "+token); }
    String admin() throws Exception {
        return response(body(post("/api/auth/login"),Map.of("email","admin@placewise.edu","password","Admin@123")),200).get("token").asText();
    }
    Account account(String branch, String cgpa) throws Exception {
        String id=unique(), email="student"+id+"@college.edu", roll="R"+id;
        JsonNode result=response(body(post("/api/auth/register"),Map.of("fullName","Test Student","email",email,
            "password","Student@123","rollNumber",roll,"branch",branch,"year",4,"cgpa",cgpa,"phone","9876543210")),201);
        return new Account(result.get("token").asText(),email,roll.toUpperCase(Locale.ROOT));
    }
    JsonNode profile(Account a) throws Exception { return response(auth(get("/api/students/profile"),a.token()),200); }
    JsonNode updateProfile(Account a,String branch,String cgpa,Set<String> skills) throws Exception {
        return response(body(auth(put("/api/students/profile"),a.token()),Map.of("fullName","Updated Student","rollNumber",a.roll(),
            "branch",branch,"year",4,"cgpa",cgpa,"phone","9876543210","skills",skills)),200);
    }
    long company(String token) throws Exception {
        return response(body(auth(post("/api/companies"),token),Map.of("name","Test Company "+unique(),"description","An engineering company",
            "website","https://example.com","industry","Technology","location","Bengaluru")),201).get("id").asLong();
    }
    Map<String,Object> driveInput(long company) {
        Map<String,Object> input=new LinkedHashMap<>();
        input.put("companyId",company); input.put("jobTitle","Java Developer"); input.put("description","Build reliable software.");
        input.put("salaryPackage",12); input.put("location","Hyderabad"); input.put("driveDate",LocalDate.now(clock).plusDays(8).toString());
        input.put("deadline",LocalDate.now(clock).plusDays(4).toString()); input.put("minCgpa",7.5);
        input.put("eligibleBranches",List.of("CSE","IT")); input.put("requiredSkills",List.of("Java","SQL"));
        input.put("jobType","FULL_TIME"); input.put("active",true); return input;
    }
    long drive(String token,long company) throws Exception {
        return response(body(auth(post("/api/drives"),token),driveInput(company)),201).get("id").asLong();
    }
    JsonNode apply(Account account,long drive,int expected) throws Exception {
        return response(body(auth(post("/api/applications"),account.token()),Map.of("driveId",drive)),expected);
    }

    @Test void registrationReturnsSafeRoleProfileAndHashesPassword() throws Exception {
        Account a=account("cse","8.2");
        JsonNode profile=profile(a);
        assertThat(profile.get("branch").asText()).isEqualTo("CSE");
        assertThat(profile.toString()).doesNotContain("password");
        var user=users.findByEmail(a.email()).orElseThrow();
        assertThat(user.passwordHash).isNotEqualTo("Student@123");
        assertThat(passwords.matches("Student@123",user.passwordHash)).isTrue();
        JsonNode loggedIn=response(body(post("/api/auth/login"),Map.of("email",a.email().toUpperCase(Locale.ROOT),"password","Student@123")),200);
        assertThat(loggedIn.get("role").asText()).isEqualTo("ROLE_STUDENT");
        assertThat(loggedIn.has("fullName")).isTrue();
        assertThat(loggedIn.has("expiresAt")).isTrue();
    }
    @Test void eligibleBranchCollectionHasThePrimaryKeyRequiredByHostedMySql() throws Exception {
        try (var connection = jdbc.getDataSource().getConnection();
             var tables = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            String table = null, schema = null, catalog = null;
            while (tables.next()) {
                if ("drive_branches".equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    table = tables.getString("TABLE_NAME");
                    schema = tables.getString("TABLE_SCHEM");
                    catalog = tables.getString("TABLE_CAT");
                    break;
                }
            }
            assertThat(table).as("eligible-branch table exists").isNotNull();
            Set<String> keyColumns = new HashSet<>();
            try (var keys = connection.getMetaData().getPrimaryKeys(catalog, schema, table)) {
                while (keys.next()) keyColumns.add(keys.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
            assertThat(keyColumns).containsExactlyInAnyOrder("drive_id", "branch");
        }
    }
    @Test void duplicateEmailAndRollAreRejectedCaseInsensitively() throws Exception {
        Account a=account("CSE","8");
        Map<String,Object> input=new HashMap<>(Map.of("fullName","Another Student","email",a.email().toUpperCase(Locale.ROOT),"password","Student@123",
            "rollNumber","R"+unique(),"branch","CSE","year",4,"cgpa",8,"phone","9876543210"));
        response(body(post("/api/auth/register"),input),409);
        input.put("email","another"+unique()+"@college.edu"); input.put("rollNumber",a.roll().toLowerCase(Locale.ROOT));
        response(body(post("/api/auth/register"),input),409);
    }
    @Test void invalidFieldsAndRoleInjectionAreRejected() throws Exception {
        Map<String,Object> input=new HashMap<>(Map.of("fullName","Student","email","invalid","password","short","rollNumber","R123",
            "branch","CSE","year",8,"cgpa",11,"phone","123"));
        JsonNode result=response(body(post("/api/auth/register"),input),400);
        assertThat(result.get("errors").has("email")).isTrue();
        assertThat(result.get("errors").has("cgpa")).isTrue();
        assertThat(result.get("errors").has("phone")).isTrue();
        input.put("role","ROLE_ADMIN"); response(body(post("/api/auth/register"),input),400);
    }
    @Test void authenticationRejectsMissingForgedAndExpiredTokens() throws Exception {
        response(get("/api/companies"),401);
        response(auth(get("/api/drives"),"not.a.valid.token"),401);
        response(body(post("/api/auth/login"),Map.of("email","admin@placewise.edu","password","wrong-password")),401);
        Instant now=Instant.now();
        String expired=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
            JwtClaimsSet.builder().issuer("placewise").subject("1").issuedAt(now.minusSeconds(7200)).expiresAt(now.minusSeconds(3600))
            .claim("roles",List.of("ROLE_ADMIN")).build())).getTokenValue();
        response(auth(get("/api/companies"),expired),401);
    }
    @Test void studentCannotReadAdminDataOrWriteAdminResources() throws Exception {
        Account a=account("CSE","8");
        for (String path:List.of("/api/students","/api/dashboard/admin","/api/applications","/api/applications/drive/1"))
            response(auth(get(path),a.token()),403);
        response(body(auth(post("/api/companies"),a.token()),Map.of("name","Fake","description","Fake","website","https://example.com","industry","Tech","location","Test")),403);
        response(auth(delete("/api/students/1"),a.token()),403);
        response(body(auth(put("/api/applications/1/status"),a.token()),Map.of("status","SELECTED")),403);
        response(auth(get("/api/students/profile"),admin()),403);
    }
    @Test void eligibilityListsAllFailuresAndBackendBlocksApply() throws Exception {
        String admin=admin(); long drive=drive(admin,company(admin)); Account a=account("EEE","5.5");
        JsonNode detail=response(auth(get("/api/drives/"+drive),a.token()),200);
        assertThat(detail.at("/eligibility/eligible").asBoolean()).isFalse();
        assertThat(detail.at("/eligibility/reasons").size()).isEqualTo(3);
        assertThat(detail.at("/eligibility/reasons").toString()).contains("CGPA","branch","skills");
        apply(a,drive,400);
        updateProfile(a,"CSE","8.1",Set.of("JAVA"," sql "));
        detail=response(auth(get("/api/drives/"+drive),a.token()),200);
        assertThat(detail.at("/eligibility/eligible").asBoolean()).isTrue();
        apply(a,drive,201);
        assertThat(response(auth(get("/api/drives/"+drive),a.token()),200).get("applied").asBoolean()).isTrue();
    }
    @Test void duplicateApplicationsAreBlockedByApiAndDatabaseConstraint() throws Exception {
        String admin=admin(); long drive=drive(admin,company(admin)); Account a=account("CSE","8.5");
        updateProfile(a,"CSE","8.5",Set.of("java","sql")); apply(a,drive,201); apply(a,drive,409);
        long studentId=profile(a).get("id").asLong();
        assertThatThrownBy(() -> jdbc.update("insert into applications (student_id,drive_id,applied_at,status) values (?,?,CURRENT_TIMESTAMP,'APPLIED')",
            studentId,drive)).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void expiredDrivesAutomaticallyCloseAndCannotReceiveApplications() throws Exception {
        String admin=admin(); long company=company(admin), driveId=drive(admin,company); Account a=account("CSE","8.5");
        updateProfile(a,"CSE","8.5",Set.of("java","sql"));
        var expired=drives.findById(driveId).orElseThrow(); expired.deadline=LocalDate.now(clock).minusDays(1); drives.saveAndFlush(expired);
        JsonNode detail=response(auth(get("/api/drives/"+driveId),a.token()),200);
        assertThat(detail.get("active").asBoolean()).isFalse();
        assertThat(detail.at("/eligibility/reasons").toString()).contains("deadline");
        apply(a,driveId,400);
        assertThat(response(auth(get("/api/drives?active=true&companyId="+company),a.token()),200).size()).isZero();
        assertThat(response(auth(get("/api/drives?active=false&companyId="+company),a.token()),200).size()).isEqualTo(1);
    }
    @Test void pausedDriveBlocksApplicationsAndCanBeReenabled() throws Exception {
        String admin=admin(); long id=drive(admin,company(admin)); Account a=account("CSE","8");
        updateProfile(a,"CSE","8",Set.of("java","sql"));
        response(body(auth(patch("/api/drives/"+id+"/active"),admin),Map.of("active",false)),200); apply(a,id,400);
        response(body(auth(patch("/api/drives/"+id+"/active"),admin),Map.of("active",true)),200); apply(a,id,201);
    }
    @Test void queryFiltersCombineOnTheServer() throws Exception {
        String admin=admin(); long company=company(admin); drive(admin,company);
        String base="/api/drives?companyId="+company;
        assertThat(response(auth(get(base+"&search=hyderabad&branch=CSE&maxCgpa=8&minPackage=10&jobType=FULL_TIME&active=true"),admin),200).size()).isEqualTo(1);
        for (String filter:List.of("&branch=EEE","&maxCgpa=7","&minPackage=20","&jobType=INTERNSHIP","&search=nonexistent"))
            assertThat(response(auth(get(base+filter),admin),200).size()).isZero();
        response(auth(get(base+"&maxCgpa=11"),admin),400);
    }
    @Test void applicationStatusUpdatesAreVisibleOnlyToTheirOwner() throws Exception {
        String admin=admin(); long drive=drive(admin,company(admin)); Account owner=account("CSE","8.2"), other=account("CSE","8.2");
        updateProfile(owner,"CSE","8.2",Set.of("java","sql"));
        long id=apply(owner,drive,201).get("id").asLong();
        response(body(auth(put("/api/applications/"+id+"/status"),admin),Map.of("status","SELECTED")),200);
        JsonNode mine=response(auth(get("/api/applications/my"),owner.token()),200);
        assertThat(mine.get(0).get("status").asText()).isEqualTo("SELECTED");
        assertThat(profile(owner).get("placed").asBoolean()).isTrue();
        assertThat(response(auth(get("/api/applications/my"),other.token()),200).size()).isZero();
        assertThat(response(auth(get("/api/dashboard/student"),owner.token()),200).get("selected").asLong()).isEqualTo(1);
    }
    @Test void selectedStudentCountDeduplicatesMultipleOffers() throws Exception {
        String admin=admin(); long company=company(admin); Account a=account("CSE","8.5");
        updateProfile(a,"CSE","8.5",Set.of("java","sql"));
        long before=response(auth(get("/api/dashboard/admin"),admin),200).get("selectedStudents").asLong();
        for (int i=0;i<2;i++) {
            long application=apply(a,drive(admin,company),201).get("id").asLong();
            response(body(auth(put("/api/applications/"+application+"/status"),admin),Map.of("status","SELECTED")),200);
        }
        assertThat(response(auth(get("/api/dashboard/admin"),admin),200).get("selectedStudents").asLong()).isEqualTo(before+1);
    }
    @Test void companyDeletionProtectsOpenDrivesAndCascadesArchivedRecords() throws Exception {
        String admin=admin(); long company=company(admin), drive=drive(admin,company); Account a=account("CSE","8.5");
        updateProfile(a,"CSE","8.5",Set.of("java","sql")); apply(a,drive,201);
        response(auth(delete("/api/companies/"+company),admin),409);
        response(body(auth(patch("/api/drives/"+drive+"/active"),admin),Map.of("active",false)),200);
        response(auth(delete("/api/companies/"+company),admin),204);
        response(auth(get("/api/drives/"+drive),admin),404);
        assertThat(response(auth(get("/api/applications/my"),a.token()),200).size()).isZero();
    }
    @Test void invalidDatesAndUnknownResourcesReturnUsefulErrors() throws Exception {
        String admin=admin(); Map<String,Object> input=driveInput(company(admin));
        input.put("deadline",LocalDate.now(clock).plusDays(20).toString());
        assertThat(response(body(auth(post("/api/drives"),admin),input),400).get("message").asText()).contains("deadline");
        input.put("deadline",LocalDate.now(clock).minusDays(1).toString());
        response(body(auth(post("/api/drives"),admin),input),400);
        response(auth(get("/api/drives/99999999"),admin),404);
        response(body(auth(put("/api/applications/99999999/status"),admin),Map.of("status","SELECTED")),404);
    }
    @Test void deletingStudentRemovesAccountAndApplications() throws Exception {
        String admin=admin(); Account a=account("CSE","8.5"); updateProfile(a,"CSE","8.5",Set.of("java","sql"));
        long drive=drive(admin,company(admin)); apply(a,drive,201); long id=profile(a).get("id").asLong();
        response(auth(delete("/api/students/"+id),admin),204);
        response(body(post("/api/auth/login"),Map.of("email",a.email(),"password","Student@123")),401);
        assertThat(response(auth(get("/api/applications/drive/"+drive),admin),200).size()).isZero();
    }
}
