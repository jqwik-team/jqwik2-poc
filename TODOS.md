- Arbitrary.filter()

- Reporting
    - run time in result section
    - Report differences between sample values and regenerated values

- Make afterFailure attribute multi value, e.g List.of(SAMPLES, REPLAY |
  RANDOMIZED)

- PropertyAnalyzer: Find as many failing samples as possible and analyze
  their commonalities

- Auto Test-Driving
    - Stop growing when configurable maxDepth is reached
    - Generate test cases beyond just growing, e.g. edge cases, random values,
      provided ones
    - Adapt next-gen-pbt.md#Automated-Test-Driven-Development to actual
      behaviour of jqwik2

- Additional generators and arbitraries
    - StringArbitrary: withChar(char... chars), withChar(CharSet chars),
      withCharRange(from, to)
    - Empty / Null Arbitrary -> When used in combinations, nothing is generated

- Target-based generation and shrinking
    - simulated
      annealing: https://www.baeldung.com/java-simulated-annealing-for-traveling-salesman
    - see http://proper.softlab.ntua.gr/Publications.html

- Allow JqwikDefaults to be changed programmatically
    - Maybe Introduce Jqwik class and then Jqwik.defaults() and Jqwik.defaults(
      new JqwikDefaults())

- Add descriptions to generators
    - Can target type of map / flatMap be inferred for description?
      E.g. by generating a single sample and using its type?

- PropertyRun Execution
    - Fail if ratio checks/tries is too low

- Stateful generators
    - Model-based testing: Pre-create model chain on model, then compare to behaviour in real system
    - Concurrent execution of Chains:
        1. Create n (small n) chains of length l. Make key collisions likely! 
        2. Run chains concurrently. Collect final result.
        3. Expectations:
            - No invariant of single transaction should be violated.
            - One of all combinatorically possible interleaving results should be found.
              For this the system can be its own sequential model.
            According to https://www.youtube.com/watch?v=r5i_OiZw6Sw only 10 repetitions (also during shrinking) are needed to detect most bugs.
        4. Insert sleep operations to simulate real-world timing issues.
        5. Also see https://www.youtube.com/watch?v=zi0rHwfiX1Q (John Hughes "Testing the hard stuff and staying sane")
    - ActionChain?

- Statistics
    - Collect and report statistics

- Shrinking (https://www.drmaciver.com/2019/01/notes-on-test-case-reduction/)
    - Compare shrinkables due to full length if different recording types are
      compared
    - Remove big chunks of elements when shrinking lists
    - Parallelize shrinking
        - https://github.com/DRMacIver/shrinkray/
        - https://dl.acm.org/doi/10.1145/3319619.3322004
    - Allow shrinking to use backup source when recording is exhausted
    - Cache tryable results during shrinking
    - Shrink with a timeout
    - Shrink in passes?
    - Show shrinking progress on console
    - Reduce list by improved binary
      search: https://notebook.drmaciver.com/posts/2019-04-30-13:03.html

- Jupiter Extension

- Misc
    - Loading samples that provide wrong number of gen sources ->
      IllegalArgumentException
      Should be ignored and sample be deleted