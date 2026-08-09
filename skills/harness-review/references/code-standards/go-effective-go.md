# Go Code Standards - Effective Go and Code Review Comments

## Overview

This reference document describes Go (Golang) code standards based on "Effective Go" and "Go Code Review Comments" for code review within the harness-review skill.

## Standards Source

**Official Go Documentation**
- **Effective Go**: https://go.dev/doc/effective_go
- **Go Code Review Comments**: https://github.com/golang/go/wiki/CodeReviewComments
- Maintained by: Go Team at Google
- Coverage: Naming, formatting, commenting, error handling, concurrency, and best practices

## Integration Method

The harness-review skill automatically applies Go standards when reviewing Go source files (`.go` extensions).

## Standards Categories

### 1. Naming Conventions

#### Package Names
- **Rule A**: **Required** - Package names should be short, lowercase, single-word
- No underscores or mixed caps
- Don't use plural names
- Use naming that describes what it does

```go
// ❌ Bad
package dataStore
package http_server
package users

// ✅ Good
package store
package http
package user
```

#### Interface Names
- **Rule B**: **Recommended** - Interface names should be descriptive
- One-method interfaces: method name + "-er" suffix
- Multi-method interfaces: descriptive name

```go
// ❌ Bad
type ReadWrite interface {
    Read(p []byte) (n int, err error)
    Write(p []byte) (n int, err error)
}

// ✅ Good
type Reader interface {
    Read(p []byte) (n int, err error)
}

type ReadWriter interface {
    Reader
    Writer
}
```

#### Variable Names
- **Rule C**: **Recommended** - Use short names for local variables
- Longer names for exported or less context
- Acronyms should be consistently cased

```go
// ❌ Bad
var httpRequestHandler HttpRequestHandler
var userId string

// ✅ Good
var handler Handler
var id string
var url string // Not URL
```

#### Constant Names
- **Rule D**: **Required** - Constant names should be CamelCase for exported, camelCase for unexported

```go
// ❌ Bad
const max_retry_count = 3
const HTTP_PORT = 8080

// ✅ Good
const MaxRetryCount = 3
const HTTPPort = 8080
```

### 2. Formatting

#### Indentation and Tabs
- **Rule E**: **Required** - Use tabs for indentation
- Use `gofmt` to format code automatically
- Never manually format without `gofmt`

#### Line Length
- **Rule F**: **Recommended** - Keep lines reasonably short
- No hard limit, but generally under 100 characters
- Break long lines at logical points

#### Comment Spacing
- **Rule G**: **Required** - Use proper comment spacing

```go
// Package comment
// Package http provides HTTP client and server implementations.
package http

// Exported function comment
// Get issues a GET to the specified URL.
func Get(url string) (resp *Response, err error) {
    // Implementation
}
```

### 3. Comments and Documentation

#### Package Comments
- **Rule H**: **Required** - Every package should have a package comment
- Place in a file named `doc.go`
- Present tense, complete sentences

```go
// Package sort provides sorting of slices and user-defined collections.
package sort
```

#### Exported Identifiers
- **Rule I**: **Required** - Every exported function, type, constant should have a comment
- The comment should be a complete sentence starting with the name

```go
// ❌ Bad
// Returns the user by ID
func GetUserByID(id string) (*User, error)

// ✅ Good
// GetUserByID retrieves a user from the database by their ID.
// If the user is not found, it returns ErrUserNotFound.
func GetUserByID(id string) (*User, error) {
    // Implementation
}
```

#### Comments on Unexported Types
- **Rule J**: **Recommended** - Comment unexported types if they're complex
- Focus on "what" and "why", not "how"

```go
// user represents a user account in the system.
// It contains authentication data and profile information.
type user struct {
    id        string
    email     string
    password  string // hashed password
}
```

### 4. Error Handling

#### Error Values
- **Rule K**: **Required** - Always handle errors, never ignore them
- Use error wrapping with `fmt.Errorf`
- Define error variables for common errors

```go
// ❌ Bad
data, _ := readFile("data.txt")

// ✅ Good
data, err := readFile("data.txt")
if err != nil {
    return fmt.Errorf("failed to read file: %w", err)
}
```

#### Error Variables
- **Rule L**: **Recommended** - Define error variables for common errors
- Use `errors.New` or `fmt.Errorf`

```go
var (
    ErrUserNotFound  = errors.New("user not found")
    ErrInvalidInput  = errors.New("invalid input")
    ErrUnauthorized  = errors.New("unauthorized access")
)
```

#### Error Messages
- **Rule M**: **Recommended** - Error messages should not start with capital letters
- Should not end with punctuation
- Should be descriptive but concise

```go
// ❌ Bad
return fmt.Errorf("Failed To Parse The Input.")

// ✅ Good
return fmt.Errorf("failed to parse the input")
```

### 5. Control Structures

#### If Statements
- **Rule N**: **Recommended** - Keep if statements simple
- Avoid nested if statements when possible

```go
// ❌ Bad
if err != nil {
    if err == io.EOF {
        return nil
    } else {
        return err
    }
}

// ✅ Good
if err == nil {
    return nil
}
if err == io.EOF {
    return nil
}
return err
```

#### For Loops
- **Rule O**: **Recommended** - Use range for simple iteration
- Break complex iterations into named functions

```go
// ✅ Good
for _, item := range items {
    process(item)
}

// ✅ Good - Named function for complex logic
for _, item := range items {
    if err := processItem(item); err != nil {
        return fmt.Errorf("processing item: %w", err)
    }
}
```

#### Defer Statements
- **Rule P**: **Required** - Use defer for cleanup operations
- Place defer immediately after resource acquisition

```go
// ✅ Good
file, err := os.Open("data.txt")
if err != nil {
    return err
}
defer file.Close()

// Process file
```

### 6. Functions and Methods

#### Function Signatures
- **Rule Q**: **Recommended** - Keep function signatures simple
- Return errors as the last return value
- Use multiple return values instead of passing pointers

```go
// ❌ Bad
func ReadUser(id string, user *User) error

// ✅ Good
func ReadUser(id string) (*User, error)
```

#### Method Receivers
- **Rule R**: **Recommended** - Use value receivers for immutable operations
- Use pointer receivers for mutable operations or to avoid copying

```go
// ✅ Good - Value receiver for immutable
func (u User) ID() string {
    return u.id
}

// ✅ Good - Pointer receiver for mutable
func (u *User) SetEmail(email string) {
    u.email = email
}
```

#### Function Length
- **Rule S**: **Recommended** - Keep functions focused and short
- Break long functions into smaller, named functions
- Extract complex logic into helper functions

### 7. Structs and Interfaces

#### Struct Field Tags
- **Rule T**: **Recommended** - Use struct tags for serialization
- Follow standard tag formats (json, xml, yaml)

```go
type User struct {
    ID       string    `json:"id"`
    Name     string    `json:"name"`
    Email    string    `json:"email"`
    Created  time.Time `json:"created"`
}
```

#### Interface Design
- **Rule U**: **Recommended** - Define interfaces where they are used
- Small, focused interfaces are preferred
- Don't define interfaces prematurely

```go
// ❌ Bad - Defining interface in producer package
type UserRepository interface {
    GetUser(id string) (*User, error)
}

func (r *userRepository) GetUser(id string) (*User, error) {
    // Implementation
}

// ✅ Good - Define interface where used
type userGetter interface {
    GetUser(id string) (*User, error)
}

func GetUserByID(ug userGetter, id string) (*User, error) {
    return ug.GetUser(id)
}
```

### 8. Concurrency

#### Goroutines
- **Rule V**: **Required** - Always know when goroutines exit
- Use context for cancellation
- Use sync.WaitGroup for waiting

```go
// ✅ Good
func processConcurrently(items []Item) error {
    ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
    defer cancel()

    var wg sync.WaitGroup
    errChan := make(chan error, len(items))

    for _, item := range items {
        wg.Add(1)
        go func(item Item) {
            defer wg.Done()
            if err := processItem(ctx, item); err != nil {
                errChan <- err
            }
        }(item)
    }

    wg.Wait()
    close(errChan)

    for err := range errChan {
        if err != nil {
            return err
        }
    }
    return nil
}
```

#### Channels
- **Rule W**: **Recommended** - Use channels for communication
- Prefer buffered channels for known workloads
- Always close channels when done

```go
// ✅ Good
func processItems(items <-chan Item) <-chan Result {
    results := make(chan Result, 10)
    go func() {
        defer close(results)
        for item := range items {
            results <- processItem(item)
        }
    }()
    return results
}
```

#### Mutex Usage
- **Rule X**: **Required** - Protect shared state with mutex
- Use defer to unlock
- Embed sync.Mutex in structs

```go
type Counter struct {
    mu    sync.Mutex
    value int
}

func (c *Counter) Increment() {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.value++
}
```

### 9. Testing

#### Test Organization
- **Rule Y**: **Required** - Test files should be `*_test.go`
- Test functions should start with `Test`
- Use table-driven tests for multiple cases

```go
// ✅ Good - Table-driven test
func TestAdd(t *testing.T) {
    tests := []struct {
        name     string
        a, b     int
        expected int
    }{
        {"positive numbers", 2, 3, 5},
        {"negative numbers", -2, -3, -5},
        {"mixed numbers", -2, 3, 1},
    }

    for _, tt := range tests {
        t.Run(tt.name, func(t *testing.T) {
            result := Add(tt.a, tt.b)
            if result != tt.expected {
                t.Errorf("Add(%d, %d) = %d; want %d", tt.a, tt.b, result, tt.expected)
            }
        })
    }
}
```

#### Test Helpers
- **Rule Z**: **Recommended** - Use test helpers to reduce duplication
- Helper functions should accept `*testing.T`

```go
func setupTestDB(t *testing.T) *sql.DB {
    db, err := sql.Open("sqlite3", ":memory:")
    if err != nil {
        t.Fatalf("failed to open database: %v", err)
    }
    return db
}
```

#### Subtests
- **Rule AA**: **Recommended** - Use subtests for related test cases
- Provides better test output and isolation

```go
func TestUserValidation(t *testing.T) {
    t.Run("valid user", func(t *testing.T) {
        user := User{ID: "123", Name: "John"}
        if err := user.Validate(); err != nil {
            t.Errorf("expected no error, got %v", err)
        }
    })

    t.Run("invalid user", func(t *testing.T) {
        user := User{ID: "", Name: ""}
        if err := user.Validate(); err == nil {
            t.Error("expected error, got nil")
        }
    })
}
```

### 10. Error Handling Patterns

#### Error Wrapping
- **Rule AB**: **Recommended** - Wrap errors with context
- Use `%w` verb for wrapped errors
- Use `%v` for new errors

```go
// ✅ Good
if err := db.SaveUser(user); err != nil {
    return fmt.Errorf("failed to save user %s: %w", user.ID, err)
}
```

#### Error Checking
- **Rule AC**: **Required** - Always check errors
- Never ignore error returns
- Use blank identifier only when absolutely necessary

```go
// ❌ Bad
data, _ := ioutil.ReadFile("file.txt")

// ✅ Good
data, err := ioutil.ReadFile("file.txt")
if err != nil {
    return fmt.Errorf("failed to read file: %w", err)
}
```

## Common Patterns and Anti-Patterns

### ✅ Good Practices

```go
// Proper error handling
func ProcessUser(id string) (*User, error) {
    user, err := db.GetUser(id)
    if err != nil {
        return nil, fmt.Errorf("getting user: %w", err)
    }

    if err := validateUser(user); err != nil {
        return nil, fmt.Errorf("validating user: %w", err)
    }

    return user, nil
}

// Proper defer usage
func ProcessFile(path string) error {
    file, err := os.Open(path)
    if err != nil {
        return err
    }
    defer file.Close()

    // Process file
    return nil
}

// Proper goroutine management
func ProcessConcurrently(items []Item) error {
    var wg sync.WaitGroup
    errs := make(chan error, len(items))

    for _, item := range items {
        wg.Add(1)
        go func(item Item) {
            defer wg.Done()
            if err := processItem(item); err != nil {
                errs <- err
            }
        }(item)
    }

    wg.Wait()
    close(errs)

    for err := range errs {
        if err != nil {
            return err
        }
    }
    return nil
}
```

### ❌ Anti-Patterns

```go
// Bad: Ignoring errors
data, _ := ioutil.ReadFile("file.txt")

// Bad: Not checking for nil before using
func ProcessUser(u *User) error {
    fmt.Println(u.Name) // Possible panic
    return nil
}

// Bad: Goroutine leak
func processItems(items []Item) {
    for _, item := range items {
        go processItem(item) // No way to wait for completion
    }
}

// Bad: Not using defer for cleanup
func ProcessFile(path string) error {
    file, err := os.Open(path)
    if err != nil {
        return err
    }

    // Process file
    file.Close() // Won't execute if processing fails
    return nil
}

// Bad: Inconsistent error checking
if err != nil {
    return err
}
// Do more work without checking if it succeeded
```

## Configuration

Go standards integration can be configured via `.claude/config/code-standards.config.json`:

```json
{
  "languageMapping": {
    "go": {
      "standards": ["effective-go", "go-code-review-comments"],
      "extensions": [".go"],
      "defaultSeverity": "major",
      "reviewScope": "full"
    }
  }
}
```

## Development Tools

### Essential Tools
- **gofmt**: Code formatting
- **go vet**: Static analysis
- **golint**: Style checker (deprecated, use staticcheck)
- **staticcheck**: Advanced static analysis
- **golangci-lint**: Comprehensive linting tool

### IDE Extensions
- **Go extension for VS Code**: IntelliSense, debugging, testing
- **GoLand**: JetBrains IDE for Go
- **vim-go**: Vim/Neovim plugin

## Performance Considerations

- Use `sync.Pool` for object reuse
- Pre-allocate slices when size is known
- Use buffered channels for performance
- Profile before optimizing
- Consider memory allocation patterns

## Security Considerations

- Validate all input
- Use `crypto/rand` for cryptographic operations
- Be careful with `eval`-like operations
- Handle secrets properly (environment variables)
- Use TLS for network communications

## Testing Best Practices

- Write tests alongside code
- Use table-driven tests
- Mock external dependencies
- Test error paths
- Use benchmarks for performance-critical code
- Test race conditions with `-race` flag

## References

- [Effective Go](https://go.dev/doc/effective_go)
- [Go Code Review Comments](https://github.com/golang/go/wiki/CodeReviewComments)
- [Go Blog](https://go.dev/blog/)
- [Go by Example](https://gobyexample.com/)
- Related documents: See `architecture.md` for overall multilingual standards architecture

## Future Enhancements

Potential improvements:
1. Go modules best practices
2. Database ORM standards (GORM, sqlx)
3. HTTP handler patterns
4. gRPC service standards
5. Deployment and containerization guidelines
