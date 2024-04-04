# Next Generation Property-Based Testing

The core of PBT is:

- Define a property with constraints and invariants
- Exercise the property by generating inputs that comply with the constraints

The standard way is to (pseudo)-randomly generate inputs, exercise the property,
and shrink the input when the property fails.

There is more ways you can use the core though. Here are some ideas:

- Generate all inputs exhaustively, if possible
- Generate "growing" inputs until the property fails or a threshold is reached
- Generate only typical inputs (e.g. edge cases plus a small number of typical (
  random) cases)
- Generate inputs based on a domain model (e.g. describing relations and
  constraints)
    - Use those inputs to test implementation
    - Use those inputs to test the model itself by displaying generated
      instantiations of the model.
      This could be helpful in the realm of domain-driven design.
- When you have a failing property, analyse it by collect additional information from the
  falsified samples:
     - Collect all falsified and satisfied samples and find common laws, eg: "p1 > 100 always fails"
     - Start from failing sample and vary on parts of the input to find parameters that are not related to the failure
- Use a combination of growing inputs and edge cases to (semi-)automatically test-drive a feature


## Falsified Sample Analysis

_TODO_


## Automated Test-Driven Development

```java
@TestDrive
class FizzBuzzTests {
    @ForAll @IntRange(min = 1, max = 1000) number;
}
```

### Result

Coverage
  [1] NOT COVERED

```java
@TestDrive
class FizzBuzzTests {
    @ForAll @IntRange(min = 1) number;


    @Case
    void one() {
        Assume.that(number == 1);
        var result = fizzBuzz(number);
        assertThat(result).isEqualTo("1");
    }

    String fizzBuzz(int number) {
        return "1";
    }
}
```

### Result

One
  [1] SATISFIED

Coverage
  [2] NOT COVERED


```java
@TestDrive
class FizzBuzzTests {
    @ForAll @IntRange(min = 1) number;


    @Case
    void normal_numbers() {
        var result = fizzBuzz(number);
        assertThat(result).isEqualTo(Integer.toString(number));
    }

    String fizzBuzz(int number) {
        return "1";
    }
}
```

### Result

normal numbers
  [1] SATISFIED
  [2] FAILED: expected 2 but was 1

```java
String fizzBuzz(int number) {
    return "" + number;
}
```

### Result

normal numbers
  [1] SATISFIED
  [2] SATISFIED
  [1000] SATISFIED


```java
@TestDrive
class FizzBuzzTests {
    @ForAll @IntRange(min = 1) number;


    @Case
    void normal_numbers() {
        Assume.that(number % 3 != 0);
        var result = fizzBuzz(number);
        assertThat(result).isEqualTo(Integer.toString(number));
    }

    @Case
    void three() {
        Assume.that(number == 3);
        var result = fizzBuzz(number);
        assertThat(result).isEqualTo("Fizz");
    }

    String fizzBuzz(int number) {
        return "" + number;
    }
}
```

### Result

normal numbers
  [1] SATISFIED
  [2] SATISFIED
  [1000] SATISFIED

three
  [3] FAILED: expected Fizz but was 3


```java
String fizzBuzz(int number) {
    if (number % 3 == 0) {
        return "Fizz";
    }
    return "" + number;
}
```

### Result

normal numbers
  [1] SATISFIED
  [2] SATISFIED
  [6] FAILED: expected Fizz but was 3

three
  [3] SATISFIED


```java
@TestDrive
class FizzBuzzTests {
    @ForAll @IntRange(min = 1) number;


    @Case
    void normal_numbers() {
        Assume.that(number % 3 != 0);
        var result = fizzBuzz(number);
        assertThat(result).isEqualTo(Integer.toString(number));
    }

    @Case
    void divisible_by_3() {
        Assume.that(number % 3 == 0);
        var result = fizzBuzz(number);
        assertThat(result).isEqualTo("Fizz");
    }

    String fizzBuzz(int number) {
        if (number % 3 == 0) {
            return "Fizz";
        }
        return "" + number;
    }
}
```

### Result

normal numbers
  [1] SATISFIED
  [2] SATISFIED
  [1000] SATISFIED

divisible by 3
  [3] SATISFIED
  [6] SATISFIED
  [999] SATISFIED

########################

```java
@TestDrive
void fizzBuss(@ForAll @IntRange(min = 1) number) {
    var result = fizzBuzz(number);
    if (number % 3 == 0) {
        assertThat(result).isEqualTo("Fizz");
    } else if (number % 5 == 0) {
        assertThat(result).isEqualTo("Buzz");
    } else {
        assertThat(result).isEqualTo(Integer.toString(number));
    }
    done();
}
```

Result:
- "Input: 1 -> Success"
- "Input: 2 -> Success"
- "Input: 3 -> Success"
- "Input: 5 -> Failed: expected Buzz but was 5"

```java
@TestDrive
void fizzBuss(@ForAll @IntRange(min = 1) number) {
    var result = fizzBuzz(number);
    if (number % 3 == 0) {
        assertThat(result).isEqualTo("Fizz");
    } else if (number % 5 == 0) {
        assertThat(result).isEqualTo("Buzz");
    } else {
        assertThat(result).isEqualTo(Integer.toString(number));
    }
    done();
}
```

Result:
- "Input: 1 -> Success"
- "Input: 2 -> Success"
- "Input: 3 -> Success"
- "Input: 5 -> Success"
- "Input: 6 -> Success"
- "Rest -> Success"

```java
@TestDrive
void fizzBuss(@ForAll @IntRange(min = 1) number) {
    var result = fizzBuzz(number);
    if (number % 3 == 0) {
        assertThat(result).startsWith("Fizz");
    } 
    if (number % 5 == 0) {
        assertThat(result).endsWith("Buzz");
    }
    if (number % 3 != 0 && number % 5 != 0) {
        assertThat(result).isEqualTo(Integer.toString(number));
    }
    done();
}
```

Result:
- "Input: 1 -> Success"
- "Input: 2 -> Success"
- "Input: 3 -> Success"
- "Input: 5 -> Success"
- "Input: 6 -> Success"
- "Input: 15 -> Failed: expected Fizz to end with Buzz"
