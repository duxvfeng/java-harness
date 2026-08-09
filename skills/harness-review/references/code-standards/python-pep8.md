# Python Code Standards - PEP 8 and Best Practices

## Overview

This reference document describes Python code standards based on PEP 8 (Python Enhancement Proposal 8) and Python best practices for code review within the harness-review skill.

## Standards Source

**PEP 8 -- Style Guide for Python Code**
- Maintained by: Python Software Foundation
- Latest version: PEP 8 (consistent with Python 3.x)
- Coverage: Code layout, naming conventions, programming recommendations, and best practices

## Integration Method

The harness-review skill automatically applies Python standards when reviewing Python code files (`.py`, `.pyi` extensions).

## Standards Categories

### 1. Code Layout

#### Indentation
- Use 4 spaces per indentation level
- No tabs
- Continuation lines should align wrapped elements vertically

#### Line Length
- Limit all lines to a maximum of 79 characters
- For docstrings/comments, limit to 72 characters
- Long lines can be broken using parentheses, braces, and brackets

#### Blank Lines
- Surround top-level function and class definitions with two blank lines
- Method definitions inside a class are surrounded by a single blank line
- Extra blank lines may be used to separate groups of related functions

#### Imports
- Imports should usually be on separate lines
- Group imports in this order:
  1. Standard library imports
  2. Related third-party imports
  3. Local application/library imports
- Put a blank line between each group

### 2. Naming Conventions

#### Variables and Functions
- `lowercase_with_underscores` for variables and functions
- `mixedCase` is allowed only in contexts where that's already the prevailing style

#### Classes
- `CapWords` for class names
- Exception names should end with "Error" (e.g., `ValueError`)

#### Constants
- `ALL_CAPS_WITH_UNDERSCORES` for module-level constants

#### Module Names
- Short, all-lowercase names
- Underscores can be used when improving readability

### 3. Programming Recommendations

#### Code Style
- Avoid comparing boolean values to True or False using `==`
- Use `is` or `is not` for comparing to None
- Use `def` for functions instead of lambda when assigning to a name
- Use explicit exception handling (`except Exception` as opposed to bare `except`)

#### Best Practices
- Use context managers (`with` statement) for resource management
- Prefer list comprehensions over `map()` and `filter()`
- Use generator expressions for large datasets
- Implement `__repr__` for debugging
- Use docstrings for module, class, and function documentation

### 4. Documentation

#### Docstrings
- Use triple double quotes `"""` for docstrings
- Include:
  - Function purpose
  - Parameters (with types)
  - Return values (with types)
  - Raised exceptions
  - Examples (when helpful)

#### Comments
- Comments should be complete sentences
- Block comments generally apply to some (or all) code that follows them
- Inline comments should be separated by at least two spaces

### 5. Type Hints

- Use type hints for function signatures and complex return types
- Import from `typing` module: `List`, `Dict`, `Optional`, `Tuple`, etc.
- Use Python 3.10+ union syntax: `str | int` instead of `Union[str, int]`
- Use `None` for optional types: `Optional[str]` is same as `str | None`

### 6. Error Handling

#### Exception Guidelines
- Catch specific exceptions rather than broad `Exception`
- Use `finally` blocks for cleanup code
- Define custom exceptions inheriting from `Exception`
- Provide meaningful error messages

#### Logging
- Use Python's `logging` module instead of `print()`
- Use appropriate log levels: DEBUG, INFO, WARNING, ERROR, CRITICAL
- Include context in log messages

### 7. Testing

#### Test Structure
- Follow Arrange-Act-Assert pattern
- Use descriptive test names
- Test both normal cases and edge cases
- Use fixtures for setup/teardown

#### Best Practices
- Use `unittest` or `pytest` framework
- Keep tests independent and isolated
- Mock external dependencies
- Aim for high test coverage

### 8. Security

#### Security Considerations
- Validate input from external sources
- Use parameterized queries to prevent SQL injection
- Handle secrets properly (environment variables, not hard-coded)
- Be cautious with `eval()` and `exec()` functions

## Rule Severity Levels

| Level | Meaning | Examples |
|-------|---------|-----------|
| **【强制】** (Mandatory) | Must be followed | 4-space indentation, meaningful names |
| **【推荐】** (Recommended) | Should be followed | Docstrings, type hints, logging |
| **【参考】** (Reference) | Guidelines | Specific naming preferences, formatting nuances |

## Common Patterns and Anti-Patterns

### ✅ Good Practices

```python
# Proper function with docstring and type hints
def calculate_total(prices: List[float], tax_rate: float) -> float:
    """Calculate total price including tax.

    Args:
        prices: List of item prices
        tax_rate: Tax rate as decimal (e.g., 0.1 for 10%)

    Returns:
        Total amount including tax
    """
    subtotal = sum(prices)
    return subtotal * (1 + tax_rate)

# Context manager usage
with open('file.txt', 'r') as f:
    content = f.read()

# Proper exception handling
try:
    result = process_data(data)
except ValueError as e:
    logger.error(f"Invalid data: {e}")
    raise

# List comprehension
squares = [x**2 for x in range(10)]
```

### ❌ Anti-Patterns

```python
# Bad: No docstring, no type hints
def calc(a, b):
    return a + b

# Bad: Not using context manager
f = open('file.txt', 'r')
content = f.read()
f.close()

# Bad: Bare except
try:
    result = process_data(data)
except:
    pass

# Bad: Using print instead of logging
print("Processing complete")
```

## File Structure Standards

```python
"""
Module docstring describing the module purpose.
"""

# Standard library imports
import os
import sys
from typing import List, Optional

# Third-party imports
import requests
from django.db import models

# Local imports
from myapp.utils import helper_function


class MyClass:
    """Class docstring."""

    def method(self) -> None:
        """Method docstring."""
        pass


def helper_function(data: List[str]) -> Optional[str]:
    """Function docstring."""
    pass


if __name__ == "__main__":
    # Main execution
    pass
```

## Performance Considerations

- Use generator expressions for large datasets
- Precompile regular expressions if used repeatedly
- Use `__slots__` for classes with many instances
- Profile before optimizing
- Consider using built-in functions instead of custom implementations

## Framework-Specific Standards

### Django
- Follow Django coding style
- Use models correctly
- Proper template structure
- Middleware usage

### Flask
- Application factory pattern
- Blueprint organization
- Context handling
- Extension integration

## Configuration

Python standards integration can be configured via `.claude/config/code-standards.config.json`:

```json
{
  "languageMapping": {
    "python": {
      "standards": ["pep-8", "python-best-practices"],
      "extensions": [".py", ".pyi"],
      "defaultSeverity": "moderate",
      "reviewScope": "full"
    }
  }
}
```

## Integration with Other Tools

### Static Analysis Tools
- **pylint**: Comprehensive code analysis
- **flake8**: Style guide enforcement
- **black**: Code formatting
- **mypy**: Static type checking
- **isort**: Import sorting

### Development Environment
- IDE extensions for Python
- Virtual environment management
- Package management (pip, poetry)

## References

- [PEP 8 -- Style Guide for Python Code](https://peps.python.org/pep-0008/)
- [PEP 257 -- Docstring Conventions](https://peps.python.org/pep-0257/)
- [Python Type Hints](https://docs.python.org/3/library/typing.html)
- [Python Testing Best Practices](https://docs.pytest.org/)
- Related documents: See `architecture.md` for overall multilingual standards architecture

## Future Enhancements

Potential improvements:
1. Framework-specific standards (Django, Flask, FastAPI)
2. Integration with Python static analysis tools
3. Automatic PEP 8 formatting suggestions
4. Type checking validation
5. Security vulnerability scanning
