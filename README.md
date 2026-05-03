# Git Project Score

A Spring Boot-based REST API service that searches GitHub repositories and scores them based on various metrics including recency, popularity (stars), and community engagement (forks).

## 📋 Overview

Git Project Score is a web service built with Spring WebMVC and Java Virtual Threads that:
- Searches GitHub repositories using both GraphQL and REST APIs
- Scores repositories based on multiple factors:
  - **Recency Score**: How recently the repository was updated
  - **Star Score**: Number of stars relative to the maximum in the result set
  - **Fork Score**: Number of forks relative to the maximum in the result set
  - **Total Score**: Average of all individual scores
- Provides paginated search results with cursor-based pagination
- Supports filtering by programming language, creation date, and repository (partial or full) name
- Uses Java Virtual Threads for concurrent GitHub API calls (GraphQL + REST run in parallel)

## 🏗️ Architecture

### Key Components

- **GitProjectScoreController**: Main REST controller exposing the search endpoint
- **GitHubRepoSearchService**: Orchestrates GitHub API calls and scoring using Virtual Threads for concurrent GraphQL + REST requests
- **ScoringStrategy**: Interface for different scoring algorithms
  - `DefaultScoringStrategy`: Linear scoring based on relative metrics
  - `NormalDistributionScoringStrategy`: Normal distribution-based scoring (not yet implemented)
- **Response Mappers**: Convert GitHub API responses to domain objects

## 🚀 Getting Started

### Prerequisites

- Java 25 or higher
- Gradle 8.x
- GitHub Personal Access Token (for API authentication)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/ahmedcevizci/git-project-score.git
   cd git-project-score
   ```

2. **Configure GitHub Token**

   Set the `GITHUB_API_TOKEN` environment variable with your GitHub Personal Access Token:

   ```bash
   export GITHUB_API_TOKEN=your_github_token_here
   ```

   Or pass it directly when running the application:

   ```bash
   GITHUB_API_TOKEN=your_github_token_here ./gradlew bootRun
   ```

   **⚠️ Security Note**: The application reads the token from the `GITHUB_API_TOKEN` environment variable.

3. **Build the project**
   ```bash
   ./gradlew clean build
   ```

4. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

   The application will start on `http://localhost:8080`

## 📖 API Documentation

### Search and Score Repositories

**Endpoint**: `GET /api/search`

Search for GitHub repositories with specific criteria and receive scored results.

#### Request Parameters

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `language` | String | Yes | Programming language filter | `Java`, `Python`, `JavaScript` |
| `creationDate` | Date | Yes | Minimum creation date (format: dd-MM-yyyy) | `01-01-2020` |
| `pageSize` | Integer | Yes | Number of results per page (max: 10) | `5` |
| `cursor` | String | No | Pagination cursor for next page | `Y3Vyc29yOjE=` |
| `searchInNames` | String | No | Search term in repository names (max 20 chars) | `spring` |

#### Example Request

```bash
curl -X GET "http://localhost:8080/api/search?language=Java&creationDate=01-01-2020&pageSize=5&searchInNames=spring"
```

#### Example Response

```json
{
  "totalResultCount": 150,
  "maxStarGazersCount": 50000,
  "maxForkCount": 10000,
  "pageInfo": {
    "hasNextPage": true,
    "hasPreviousPage": false,
    "startCursor": "Y3Vyc29yOjE=",
    "endCursor": "Y3Vyc29yOjU="
  },
  "scoredRepositories": [
    {
      "repository": {
        "name": "spring-boot",
        "description": "Spring Boot makes it easy to create stand-alone applications",
        "htmlUrl": "https://github.com/spring-projects/spring-boot",
        "primaryLanguage": "Java",
        "createdAt": "2020-03-15T10:30:00Z",
        "updatedAt": "2025-10-09T14:20:00Z",
        "pushedAt": "2025-10-09T14:20:00Z",
        "stargazerCount": 50000,
        "watcherCount": 2500,
        "forkCount": 10000,
        "isFork": false,
        "isArchived": false,
        "isDisabled": false,
        "licenseName": "Apache License 2.0"
      },
      "score": {
        "totalScore": 100,
        "recencyScore": 100,
        "starGazersScore": 100,
        "forksScore": 100
      }
    }
  ]
}
```

#### Error Responses

**400 Bad Request** - Invalid parameters
```json
{
  "timestamp": "2025-10-10T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "`pageSize` query parameter cannot exceed 10"
}
```

**500 Internal Server Error** - Server error
```json
{
  "timestamp": "2025-10-10T12:00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred"
}
```

## 🧪 Testing

The project includes comprehensive unit and integration tests, as well as manual API testing via Bruno.

### Automated Tests

#### Run All Tests

```bash
./gradlew test
```

#### Run Specific Test Suites

**Unit Tests Only**
```bash
./gradlew test --tests "*Test" --exclude-tests "*IntegrationTest"
```

**Integration Tests Only**
```bash
./gradlew test --tests "*IntegrationTest"
```

**Test Specific Class**
```bash
./gradlew test --tests "ScoreOverHundredTest"
```

#### Test Coverage

Generate test coverage report:
```bash
./gradlew test jacocoTestReport
```

Report will be available at: `build/reports/jacoco/test/html/index.html`

### Manual API Testing with Bruno

The project includes [Bruno](https://www.usebruno.com/) API collections for manual testing and exploration.

#### Setup Bruno

1. **Install Bruno**
   - Download [Bruno](https://www.usebruno.com/)

2. **Open the Collection**
   - Launch Bruno
   - Click "Open Collection"
   - Navigate to `src/test/resources/bruno/`
   - Select the folder to load the "Git Project Score API" collection

3. **Start the Application**
   ```bash
   ./gradlew bootRun
   ```

   The application must be running on `http://localhost:8080` for the Bruno requests to work.

#### Available Requests

The Bruno collection includes pre-configured requests:

1. **Search GitHub Repositories**
   - Basic search with language filter
   - Searches for Java repositories created after 2024-01-01
   - Includes optional name search parameter

2. **Search with Pagination**
   - Example of using pagination cursors
   - Demonstrates how to fetch subsequent pages
   - Use `endCursor` from previous response as `cursor` parameter

#### Using Bruno

1. **Select Environment**: Choose "local" environment (configured for `http://localhost:8080`)

2. **Modify Parameters**: Edit query parameters in the request:
   - `language`: Change programming language (Java, Python, JavaScript, etc.)
   - `creationDate`: Adjust date filter (format: dd-MM-yyyy)
   - `pageSize`: Set results per page (max: 10)
   - `searchInNames`: Add keyword to search in repository names
   - `cursor`: Use for pagination (copy from previous response's `endCursor`)

3. **Send Request**: Click "Send" button

4. **View Response**: Response appears in the right panel with:
   - Status code
   - Response time
   - JSON formatted response body


## 🔧 Configuration

### Application Properties

| Property | Default | Description |
|----------|---------|-------------|
| `spring.application.name` | `git-project-score` | Application name |
| `git-project-score.github.api.token` | _(required)_ | GitHub Personal Access Token |
| `git-project-score.max-search-page-size` | `10` | Maximum results per page |

### GitHub Token Scopes

Your GitHub token needs the following scopes:
- `public_repo` or `repo` - To search public/private repositories

To create a token:
1. Go to GitHub Settings → Developer settings → Personal access tokens
2. Generate new token (classic)
3. Select required scopes
4. Copy the token and add to `application.properties` as `git-project-score.github.api.token`

## 📊 Scoring Algorithm

### Default Scoring Strategy

The default strategy calculates scores based on three factors:

#### 1. Recency Score (0-100)
Based on when the repository was last updated:
- 0-1 years ago: **100 points**
- 1-2 years ago: **90 points**
- 2-3 years ago: **80 points**
- ...
- 10+ years ago: **0 points**

#### 2. Star Score (0-100)
Linear scale relative to the maximum stars in the result set:
```
score = (repository_stars / max_stars) × 100
```

#### 3. Fork Score (0-100)
Linear scale relative to the maximum forks in the result set:
```
score = (repository_forks / max_forks) × 100
```

#### Total Score
Average of all three scores:
```
total = (recency + stars + forks) / 3
```

## 🏗️ Building for Production

### Create Executable JAR

```bash
./gradlew bootJar
```

The JAR will be created at: `build/libs/git-project-score-0.0.1-SNAPSHOT.jar`

### Run the JAR

```bash
java -jar build/libs/git-project-score-0.0.1-SNAPSHOT.jar
```

### Build Native Image (GraalVM)

The project includes the GraalVM Native Build Tools plugin. To compile a native executable:

```bash
./gradlew nativeCompile
```

The native binary will be created at: `build/native/nativeCompile/git-project-score`

## 🛠️ Development

### Project Structure

- **Language**: Java 25
- **Framework**: Spring Boot 4.0.6
- **Build Tool**: Gradle (Kotlin DSL)
- **Key Dependencies**:
  - Spring WebMVC (blocking web with Virtual Threads)
  - Spring GraphQL Client (`HttpSyncGraphQlClient`)
  - Apache Commons Math3 (Statistical calculations)
  - Lombok (code generation)
  - JUnit 5 & Mockito (Testing)
  - GraalVM Native Build Tools (native image support)

### Adding New Scoring Strategies

1. Implement the `ScoringStrategy` interface:
```java
public class MyCustomScoringStrategy implements ScoringStrategy {
    @Override
    public boolean isRelativeToOtherProjects() {
        return true; // or false
    }

    @Override
    public ScoreDto calculateScore(
        GithubRepositoryDto githubRepositoryDto,
        Integer totalMatchingRepoCount,
        Integer maxForkCount,
        Integer maxStarGazersCount
    ) {
        // New scoring logic here
    }
}
```

2. Inject it into the controller or service
3. Add unit tests

## 📝 API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/search` | GET | Search and score GitHub repositories |
| `/actuator/health` | GET | Health check endpoint |
| `/actuator/info` | GET | Application info |

## 🐛 Troubleshooting

### Common Issues

**Issue**: `401 Unauthorized` error
- **Solution**: Check your GitHub token is valid and has correct scopes

**Issue**: `403 Rate Limit Exceeded`
- **Solution**: GitHub API has rate limits. Wait or use a token with higher limits

**Issue**: Application fails to start
- **Solution**: Ensure Java 25 is installed: `java -version`

### Enable Debug Logging

Add to `application.properties`:
```properties
logging.level.tech.alaz.git.project.score=DEBUG
logging.level.org.springframework.graphql=DEBUG
```

## 🗺️ Possible Future Improvements

- [ ] Add missing tests and improve test quality.
- [ ] Implement Wiremock for better integration tests.
- [ ] Add a caching mechanism to avoid unnecessary calls to Github API for fetching max fork count and max star count
  during the same search where pagination is being used.
- [ ] Separate Domain, Controller, and Service layers DTOs with proper mappers to avoid cascading changes and cleaner architecture.
- [ ] Refactor DTOs and strategies to have less anemic models, to have DDD style domain objects where behaviors are encapsulated in domain objects.
- [ ] Implement a Normal Distribution scoring strategy.
- [ ] Different scoring scales to present in response via custom configuration.
- [ ] Support for search filter checks like check against language names locally to prevent GitHub calls.
- [ ] Improve error handling by creating more custom exceptions.
- [ ] Add support for GraphQL API.
